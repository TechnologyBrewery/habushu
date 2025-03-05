package org.technologybrewery.habushu;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.collections4.MapUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;
import org.technologybrewery.habushu.util.VersionExtraTuple;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Installs dependencies defined in the project's pyproject.toml configuration,
 * specifically by running "uv lock" followed by "uv sync". If a
 * private PyPi repository is defined via
 * {@link AbstractHabushuMojo#pypiRepoUrl} (and
 * {@link AbstractHabushuMojo#pypiRepoId}), it will be automatically added to
 * the module's pyproject.toml configuration as a supplemental source of
 * dependencies, if it is not already configured in the pyproject.toml
 */
public class InstallDependenciesUv extends AbstractInstallDependencies {

    protected static final String PUBLIC_PYPI_REPO_URL = "https://pypi.org/simple/";
    private static final String UV_CLEAN_CACHE_COMMAND = "uv cache clean";
    private static final String[] RELATIONAL_OPERATORS = {"=", "<", ">", "^"};
    private static final String ENVIRONMENT_MARKER_START = "[";
    private static final String ENVIRONMENT_MARKER_END = "]";


    /**
     * Path within a UV project's pyproject.toml configuration at which private
     * PyPi repositories may be specified as sources from which dependencies may be
     * resolved and installed.
     */
    protected static final String PYPROJECT_PACKAGE_INDEX_PATH = "tool.uv.index";

    public InstallDependenciesUv(File baseDir, Log log, InstallDependenciesMojo mojo)  {
        super(baseDir, log, mojo);
    }


    @Override
    public void doExecute() throws MojoFailureException {
        UvCommandHelper uvCommandHelper = new UvCommandHelper(baseDir);

        processManagedDependencyMismatches();

        if (installDependenciesMojo.getPypiRepoUrl() == null || installDependenciesMojo.getPypiRepoUrl().isEmpty()) {
            prepareDefaultRepositoryForInstallation();
        }

        prepareRepositoryForInstallation(installDependenciesMojo.getPypiRepoId(), installDependenciesMojo.getPypiRepoUrl(), PYPROJECT_PACKAGE_INDEX_PATH);
        if (installDependenciesMojo.useDevRepository()) {
            prepareRepositoryForInstallation(installDependenciesMojo.getDevRepositoryId(), installDependenciesMojo.getDevRepositoryUrl(), PYPROJECT_PACKAGE_INDEX_PATH);
        }

        if (!installDependenciesMojo.skipPoetryLockUpdate()) {
            log.info("Locking dependencies specified in pyproject.toml...");
            uvCommandHelper.executePackageManagerCommandAndLogAfterTimeout(
                    uvCommandHelper.createLockCommand(installDependenciesMojo.skipPoetryLockUpdate, true),
                    2,
                    TimeUnit.MINUTES,
                    UV_CLEAN_CACHE_COMMAND
            );

        }

        List<String> syncCommand = uvCommandHelper.createSyncCommand();

        for (String groupName : installDependenciesMojo.withGroups) {
            syncCommand.add("--group");
            syncCommand.add(groupName);
        }
        for (String groupName : installDependenciesMojo.withoutGroups) {
            syncCommand.add("--no-group");
            syncCommand.add(groupName);
        }

        log.info("Installing dependencies...");
        uvCommandHelper.executePackageManagerCommandAndLogAfterTimeout(syncCommand, 2, TimeUnit.MINUTES, UV_CLEAN_CACHE_COMMAND);
    }

    private void prepareDefaultRepositoryForInstallation() {
        String pypiRepoSimpleIndexUrl;
        try {
            pypiRepoSimpleIndexUrl = getPyPiRepoSimpleIndexUrl(PUBLIC_PYPI_REPO_URL);
        } catch (URISyntaxException e) {
            throw new HabushuException(
                    String.format("Could not parse configured repoUrl %s", PUBLIC_PYPI_REPO_URL), e);
        }

        Config matchingPypiRepoIndexConfig;
        try (FileConfig pyProjectConfig = FileConfig.of(getPyProjectTomlFile())) {
            pyProjectConfig.load();

            Optional<List<Config>> packageIndex = pyProjectConfig.getOptional(PYPROJECT_PACKAGE_INDEX_PATH);
            matchingPypiRepoIndexConfig = packageIndex.orElse(Collections.emptyList()).stream()
                    .filter(packageIdx -> pypiRepoSimpleIndexUrl.equals(packageIdx.get("url"))).findFirst()
                    .orElse(Config.inMemory());
        }


        if (!matchingPypiRepoIndexConfig.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Configured PyPi repository %s found in the following pyproject.toml [[%s]] array element: %s",
                        PUBLIC_PYPI_REPO_URL, PYPROJECT_PACKAGE_INDEX_PATH, matchingPypiRepoIndexConfig));
            }
        } else {
            // Unlike Poetry, UV Doesn't have supplemental priority where it tries to find default source first and
            // search supplemental source. UV only search for specified index if available (if there's no tool.uv.index, it would fall back to default public pypi.).
            // Therefore, if we need to specify supplemental sources, we need to populate default ( or explicit) source first.
            List<String> defaultPypiRepoIndexConfig = Arrays.asList(System.lineSeparator(), String.format(
                            "# Added by habushu-maven-plugin at %s to use %s as source PyPi repository for installing dependencies",
                            LocalDateTime.now(), pypiRepoSimpleIndexUrl),
                    String.format("[[%s]]", PYPROJECT_PACKAGE_INDEX_PATH),
                    String.format("name = \"%s\"",PUBLIC_PYPI_REPO_ID),
                    String.format("url = \"%s\"", PUBLIC_PYPI_REPO_URL));

            log.info("Adding default public PyPi repository entry in pyproject.toml");

            try {
                Files.write(getPyProjectTomlFile().toPath(), defaultPypiRepoIndexConfig,
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new HabushuException(String.format(
                        "Could not write new [[%s]] element to pyproject.toml", PYPROJECT_PACKAGE_INDEX_PATH), e);
            }
        }
    }

    protected void processManagedDependencyMismatches() {
        if (!installDependenciesMojo.managedDependencies.isEmpty()) {
            Map<String, TomlReplacementTuple> replacements = new HashMap<>();
            try (FileConfig pyProjectConfig = FileConfig.of(getPyProjectTomlFile())) {
                pyProjectConfig.load();
                // Look for the Project UV dependencies:
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, TomlUtils.TOOL_UV_PROJECT);
                // Look for the UV Dependency Groups
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, TomlUtils.TOOL_UV_DEPENDENCY_GROUPS);

                // Log replacements, if appropriate:
                if (installDependenciesMojo.failOnManagedDependenciesMismatches || !installDependenciesMojo.updateManagedDependenciesWhenFound) {
                    for (TomlReplacementTuple replacement : replacements.values()) {
                        logPackageMismatch(replacement.getPackageName(), replacement.getOriginalOperatorAndVersion(),
                                replacement.getUpdatedOperatorAndVersion());
                    }
                }
                performPendingDependencyReplacements(replacements);
            }
        }
    }

    private void executeDetailedManagedDependencyMismatchActions(Map<String, TomlReplacementTuple> replacements,
                                                                 FileConfig pyProjectConfig, String tomlSection) {
        Optional<Config> uVDependencies = pyProjectConfig.getOptional(tomlSection);
        if (uVDependencies.isPresent()) {
            Map<String, List<String>> foundDependenciesMap = new HashMap<>();
            if (TomlUtils.TOOL_UV_PROJECT.equals(tomlSection)) {
                //This is to retrieve project dependencies
                foundDependenciesMap.put("dependencies", uVDependencies.get().get("dependencies"));
            } else if (TomlUtils.TOOL_UV_DEPENDENCY_GROUPS.equals(tomlSection)) {
                //This is to retrieve dev or custom dependencies
                for(Config.Entry uvDependency : uVDependencies.get().entrySet())
                {
                    foundDependenciesMap.put(uvDependency.getKey(), uvDependency.getValue());
                }
            }
            Map<String, VersionExtraTuple> dependencyMap = new HashMap<>();

            for (List<String> dependencies : foundDependenciesMap.values()) {
                for (String dependency: dependencies) {
                    if (StringUtils.indexOfAny(dependency, RELATIONAL_OPERATORS) == -1) {
                        log.info(String.format("%s does not have a specific version to manage - skipping", dependency));
                    } else {
                        //strip out any whitespace first.
                        dependency = dependency.replaceAll("\\s", "");
                        String packageName = dependency.substring(0, StringUtils.indexOfAny(dependency, RELATIONAL_OPERATORS));
                        String extra = "";
                        if (dependency.contains(ENVIRONMENT_MARKER_START)) {
                            packageName = dependency.substring(0, dependency.indexOf(ENVIRONMENT_MARKER_START));
                            extra = dependency.substring(dependency.indexOf(ENVIRONMENT_MARKER_START), dependency.indexOf(ENVIRONMENT_MARKER_END ) + 1);
                        }
                        String versions = dependency.substring(StringUtils.indexOfAny(dependency, RELATIONAL_OPERATORS));
                        dependencyMap.put(packageName, new VersionExtraTuple(versions, extra));

                    }
                }
            }

            for (PackageDefinition def : installDependenciesMojo.managedDependencies) {
                String packageName = def.getPackageName();
                if (dependencyMap.containsKey(packageName)) {
                    VersionExtraTuple packageRhs = dependencyMap.get(packageName);
                    performComparisonAndStageNeededChanges(replacements, def, packageRhs);
                }
            }
        }
    }

    private void performComparisonAndStageNeededChanges(Map<String, TomlReplacementTuple> replacements, PackageDefinition def, VersionExtraTuple packageRhs) {
        String originalOperatorAndVersion = packageRhs.getVersion();
        String extra = packageRhs.getExtra();
        String updatedOperatorAndVersion = def.getOperatorAndVersion();
        String operator = originalOperatorAndVersion.substring(0,StringUtils.lastIndexOfAny(originalOperatorAndVersion,RELATIONAL_OPERATORS)+1);
        String packageName = def.getPackageName();

        if (installDependenciesMojo.overridePackageVersion && updatedOperatorAndVersion.contains(SNAPSHOT)) {
            updatedOperatorAndVersion = operator + replaceSnapshotWithWildcard(updatedOperatorAndVersion);
        }
        boolean mismatch = !originalOperatorAndVersion.equals(updatedOperatorAndVersion);

        if (mismatch) {
            if (def.isActive()) {
                TomlReplacementTuple tuple = new TomlReplacementTuple(packageName, originalOperatorAndVersion, updatedOperatorAndVersion);
                replacements.put(packageName+extra, tuple);
            } else {
                log.info(String.format("Package %s is not up to date with common project package definition guidance, "
                        + "but the check has been inactivated", packageName));
            }
        }
    }


    protected void performPendingDependencyReplacements(Map<String, TomlReplacementTuple> replacements) {
        if (MapUtils.isNotEmpty(replacements)) {
            if (installDependenciesMojo.failOnManagedDependenciesMismatches) {
                if (installDependenciesMojo.updateManagedDependenciesWhenFound) {
                    log.warn("updateManagedDependenciesWhenFound=true will never be processed when failOnManagedDependenciesMismatches also equals true!");
                }
                throw new HabushuException("Found managed dependencies - please fix before proceeding!  "
                        + "(see 'Package abc is not up to date with common project package definition guidance!` log messages above!");
            }


            if (installDependenciesMojo.updateManagedDependenciesWhenFound) {
                File pyProjectTomlFile = getPyProjectTomlFile();
                StringBuilder fileContent = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
                    String line = reader.readLine();

                    while (line != null) {

                        for (Map.Entry<String, TomlReplacementTuple> replacement : replacements.entrySet())
                        {
                            String packageNameWithExtra = replacement.getKey();
                            String original = replacement.getValue().getOriginalOperatorAndVersion();
                            String updated = replacement.getValue().getUpdatedOperatorAndVersion();
                            String packageNameWithOriginal = packageNameWithExtra + original;
                            if (line.contains(packageNameWithOriginal)) {
                                line = line.replace(original, updated);
                                log.info(String.format("Updated %s: %s --> %s", replacement.getValue().getPackageName(),
                                        original, updated));
                            }
                        }
                        fileContent.append(line + "\n");
                        line = reader.readLine();
                    }

                } catch (IOException e) {
                    throw new HabushuException("Problem reading pyproject.toml to update with managed dependencies!", e);
                }

                try {
                    TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent.toString());

                } catch (IOException e) {
                    throw new HabushuException("Problem writing pyproject.toml with managed dependency updates!", e);
                }

            }
        }
    }
}
