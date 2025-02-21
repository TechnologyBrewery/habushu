package org.technologybrewery.habushu;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;
import org.technologybrewery.habushu.util.VersionExtraTuple;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
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

    private static final String SNAPSHOT = "-SNAPSHOT";
    protected static final String PUBLIC_PYPI_REPO_ID = "pypi";
    protected static final String PUBLIC_PYPI_REPO_URL = "https://pypi.org/simple/";
    private static final String UV_CLEAN_CACHE_COMMAND = "uv cache clean";
    private static final char[] RELATIONAL_OPERATORS = {'=', '<', '>', '^'};
    private static final String ENVIRONMENT_MARKER_START = "[";
    private static final String ENVIRONMENT_MARKER_END = "]";


    /**
     * Path within a UV project's pyproject.toml configuration at which private
     * PyPi repositories may be specified as sources from which dependencies may be
     * resolved and installed.
     */
    protected static final String PYPROJECT_PACKAGE_INDEX_PATH = "tool.uv.index";

    public InstallDependenciesUv(File baseDir, Log log, InstallDependenciesConfigurations installDependenciesConfigurations)  {
        super(baseDir, log, installDependenciesConfigurations);
    }


    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        UvCommandHelper uvCommandHelper = new UvCommandHelper(baseDir);

        processManagedDependencyMismatches();

        if (installDependenciesConfigurations.getPypiRepoUrl() == null || installDependenciesConfigurations.getPypiRepoUrl().isEmpty()) {
            prepareDefaultRepositoryForInstallation();
        }

        prepareRepositoryForInstallation(installDependenciesConfigurations.getPypiRepoId(), installDependenciesConfigurations.getPypiRepoUrl());
        if (installDependenciesConfigurations.useDevRepository()) {
            prepareRepositoryForInstallation(installDependenciesConfigurations.getDevRepositoryId(), installDependenciesConfigurations.getDevRepositoryUrl());
        }

        if (!installDependenciesConfigurations.skipPoetryLockUpdate()) {
            log.info("Locking dependencies specified in pyproject.toml...");
            uvCommandHelper.executePackageManagerCommandAndLogAfterTimeout(
                    uvCommandHelper.createLockCommand(installDependenciesConfigurations.skipPoetryLockUpdate(), true),
                    2,
                    TimeUnit.MINUTES,
                    UV_CLEAN_CACHE_COMMAND
            );

        }

        List<String> syncCommand = uvCommandHelper.createSyncCommand();

        for (String groupName : installDependenciesConfigurations.getWithGroups()) {
            syncCommand.add("--group");
            syncCommand.add(groupName);
        }
        for (String groupName : installDependenciesConfigurations.getWithoutGroups()) {
            syncCommand.add("--no-group");
            syncCommand.add(groupName);
        }

        log.info("Installing dependencies...");
        uvCommandHelper.executePackageManagerCommandAndLogAfterTimeout(syncCommand, 2, TimeUnit.MINUTES, UV_CLEAN_CACHE_COMMAND);
    }


    private void prepareRepositoryForInstallation(String repoId, String repoUrl) {
        if (StringUtils.isNotEmpty(repoUrl) && installDependenciesConfigurations.addPypiRepoAsPackageSources()) {
            String pypiRepoSimpleIndexUrl;
            try {
                pypiRepoSimpleIndexUrl = getPyPiRepoSimpleIndexUrl(repoUrl);
            } catch (URISyntaxException e) {
                throw new HabushuException(
                        String.format("Could not parse configured repoUrl %s", repoUrl), e);
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
                            repoUrl, PYPROJECT_PACKAGE_INDEX_PATH, matchingPypiRepoIndexConfig));
                }
            } else {
                List<String> newPypiRepoSourceConfig = Arrays.asList(System.lineSeparator(), String.format(
                                "# Added by habushu-maven-plugin at %s to use %s as source PyPi repository for installing dependencies",
                                LocalDateTime.now(), pypiRepoSimpleIndexUrl),
                        String.format("[[%s]]", PYPROJECT_PACKAGE_INDEX_PATH),
                        String.format("name = \"%s\"",
                                StringUtils.isNotEmpty(repoId) && !PUBLIC_PYPI_REPO_ID.equals(repoId)
                                        ? repoId
                                        : "private-pypi-repo"),
                        String.format("url = \"%s\"", pypiRepoSimpleIndexUrl));
                log.info(String.format("Private PyPi repository entry for %s not found in pyproject.toml",
                        repoUrl));
                log.info(String.format(
                        "Adding %s to pyproject.toml as supplemental repository from which dependencies may be installed",
                        pypiRepoSimpleIndexUrl));
                try {
                    Files.write(getPyProjectTomlFile().toPath(), newPypiRepoSourceConfig,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new HabushuException(String.format(
                            "Could not write new [[%s]] element to pyproject.toml", PYPROJECT_PACKAGE_INDEX_PATH), e);
                }
            }
        }
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
            // search supplemental source. UV only search for specified index if available ( if there's no tool. uv. index, it would fall back to default public pypi.).
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

    /**
     * Attempts to infer the PEP-503 compliant PyPI simple repository index URL
     * associated with the provided PyPI repository URL. In order to configure
     * UV to use a private PyPi repository as a source for installing package
     * dependencies, the simple index URL of the repository <b>*must*</b> be
     * utilized. For example, if a private PyPI repository is hosted at
     * https://my-company-sonatype-nexus/repository/internal-pypi and provided to
     * Habushu via the {@literal <pypiRepoUrl>} configuration, the simple index URL
     * returned by this method will be
     * https://my-company-sonatype-nexus/repository/internal-pypi/simple/ (the
     * trailing slash is required!).
     *
     * @param pypiRepoUrl URL of the private PyPi repository for which to generate
     *                    the simple index API URL.
     * @return simple index API URL associated with the given PyPi repository URL.
     * @throws URISyntaxException
     */
    protected String getPyPiRepoSimpleIndexUrl(String pypiRepoUrl) throws URISyntaxException {
        URIBuilder pypiRepoUriBuilder = new URIBuilder(StringUtils.removeEnd(pypiRepoUrl, "/"));
        List<String> repoUriPathSegments = pypiRepoUriBuilder.getPathSegments();
        String lastPathSegment = CollectionUtils.isNotEmpty(repoUriPathSegments)
                ? repoUriPathSegments.get(repoUriPathSegments.size() - 1)
                : null;
        if (!installDependenciesConfigurations.getPypiSimpleSuffix().equals(lastPathSegment)) {
            // If the URL has no path, an unmodifiable Collections.emptyList() is returned,
            // so wrap in an ArrayList to enable later modifications
            repoUriPathSegments = new ArrayList<>(repoUriPathSegments);
            repoUriPathSegments.add(installDependenciesConfigurations.getPypiSimpleSuffix());
            pypiRepoUriBuilder.setPathSegments(repoUriPathSegments);
        }

        return StringUtils.appendIfMissing(pypiRepoUriBuilder.build().toString(), "/");
    }

    protected void processManagedDependencyMismatches() {
        if (!installDependenciesConfigurations.getManagedDependencies().isEmpty()) {
            Map<String, TomlReplacementTuple> replacements = new HashMap<>();
            try (FileConfig pyProjectConfig = FileConfig.of(getPyProjectTomlFile())) {
                pyProjectConfig.load();
                // Look for the Project UV dependencies:
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, TomlUtils.TOOL_UV_PROJECT);
                // Look for the UV Dependency Groups
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, TomlUtils.TOOL_UV_DEPENDENCY_GROUPS);

                // Log replacements, if appropriate:
                if (installDependenciesConfigurations.failOnManagedDependenciesMismatches() || !installDependenciesConfigurations.updateManagedDependenciesWhenFound()) {
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
                    if (indexOfAny(dependency, RELATIONAL_OPERATORS) == -1) {
                        log.info(String.format("%s does not have a specific version to manage - skipping", dependency));
                    } else {
                        //strip out any whitespace first.
                        dependency = dependency.replaceAll("\\s", "");
                        String packageName = dependency.substring(0, indexOfAny(dependency, RELATIONAL_OPERATORS));
                        String extra = "";
                        if (dependency.contains(ENVIRONMENT_MARKER_START)) {
                            packageName = dependency.substring(0, dependency.indexOf(ENVIRONMENT_MARKER_START));
                            extra = dependency.substring(dependency.indexOf(ENVIRONMENT_MARKER_START), dependency.indexOf(ENVIRONMENT_MARKER_END ) + 1);
                        }
                        String versions = dependency.substring(indexOfAny(dependency, RELATIONAL_OPERATORS));
                        dependencyMap.put(packageName, new VersionExtraTuple(versions, extra));

                    }
                }
            }

            for (PackageDefinition def : installDependenciesConfigurations.getManagedDependencies()) {
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
        String operator = originalOperatorAndVersion.substring(0,lastIndexOfAny(originalOperatorAndVersion,RELATIONAL_OPERATORS)+1);
        String packageName = def.getPackageName();

        if (installDependenciesConfigurations.overridePackageVersion() && updatedOperatorAndVersion.contains(SNAPSHOT)) {
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
            if (installDependenciesConfigurations.failOnManagedDependenciesMismatches()) {
                if (installDependenciesConfigurations.updateManagedDependenciesWhenFound()) {
                    log.warn("updateManagedDependenciesWhenFound=true will never be processed when failOnManagedDependenciesMismatches also equals true!");
                }
                throw new HabushuException("Found managed dependencies - please fix before proceeding!  "
                        + "(see 'Package abc is not up to date with common project package definition guidance!` log messages above!");
            }


            if (installDependenciesConfigurations.updateManagedDependenciesWhenFound()) {
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

    protected static String replaceSnapshotWithWildcard(String pomVersion) {
        return pomVersion.substring(0, pomVersion.indexOf(SNAPSHOT)) + ".*";
    }


    public static int indexOfAny(String s, char[] chars) {
        int ind = -1;
        for (char c : chars) {
            int pos = s.indexOf(c);
            if (pos >= 0 && (pos < ind || ind < 0)) {
                ind = pos;
            }
        }
        return ind;
    }

    public static int lastIndexOfAny(String s, char[] chars) {
        int ind = -1;
        for (char c : chars) {
            int pos = s.lastIndexOf(c);
            if (pos >= 0 && (pos < ind || ind < 0)) {
                ind = pos;
            }
        }
        return ind;
    }

}
