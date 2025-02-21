package org.technologybrewery.habushu;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Installs dependencies defined in the project's pyproject.toml configuration,
 * specifically by running "poetry lock" followed by "poetry install". If a
 * private PyPi repository is defined via
 * {@link AbstractHabushuMojo#pypiRepoUrl} (and
 * {@link AbstractHabushuMojo#pypiRepoId}), it will be automatically added to
 * the module's pyproject.toml configuration as a supplemental source of
 * dependencies, if it is not already configured in the pyproject.toml
 */
public class InstallDependenciesPoetry extends AbstractInstallDependencies {

    private static final String EQUALS = "=";
    private static final String SNAPSHOT = "-SNAPSHOT";
    protected static final String PUBLIC_PYPI_REPO_ID = "pypi";
    private static final String POETRY_CLEAN_CACHE_COMMAND = "poetry cache clear . --all";

    /**
     * Path within a Poetry project's pyproject.toml configuration at which private
     * PyPi repositories may be specified as sources from which dependencies may be
     * resolved and installed.
     */
    protected static final String PYPROJECT_PACKAGE_SOURCES_PATH = "tool.poetry.source";

    public InstallDependenciesPoetry(File baseDir, Log log, InstallDependenciesConfigurations installDependenciesConfigurations)  {
        super(baseDir, log, installDependenciesConfigurations);
    }


    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);

        setUpInProjectVirtualEnvironment(poetryHelper);

        processManagedDependencyMismatches();

        prepareRepositoryForInstallation(installDependenciesConfigurations.getPypiRepoId(), installDependenciesConfigurations.getPypiRepoUrl());
        if (installDependenciesConfigurations.useDevRepository()) {
            prepareRepositoryForInstallation(installDependenciesConfigurations.getDevRepositoryId(), installDependenciesConfigurations.getDevRepositoryUrl());
        }

        if (!installDependenciesConfigurations.skipPoetryLockUpdate()) {
            log.info("Locking dependencies specified in pyproject.toml...");
            poetryHelper.executePackageManagerCommandAndLogAfterTimeout(
                    poetryHelper.createLockCommand(installDependenciesConfigurations.skipPoetryLockUpdate()),
                    2,
                    TimeUnit.MINUTES,
                    POETRY_CLEAN_CACHE_COMMAND
            );

        }

        List<String> installCommand = poetryHelper.createInstallCommand(installDependenciesConfigurations.isForceSync());

        for (String groupName : installDependenciesConfigurations.getWithGroups()) {
            installCommand.add("--with");
            installCommand.add(groupName);
        }
        for (String groupName : installDependenciesConfigurations.getWithoutGroups()) {
            installCommand.add("--without");
            installCommand.add(groupName);
        }

        log.info("Installing dependencies...");
        poetryHelper.executePackageManagerCommandAndLogAfterTimeout(installCommand, 2, TimeUnit.MINUTES, POETRY_CLEAN_CACHE_COMMAND);
    }

    private void setUpInProjectVirtualEnvironment(PoetryCommandHelper poetryHelper) throws MojoExecutionException {
        List<String> arguments = new ArrayList<>();
        arguments.add("config");
        arguments.add("virtualenvs.in-project");
        arguments.add("--local");
        String currentInProjectSetting = poetryHelper.execute(arguments);
        
        // update the poetry config to match the useInProjectVirtualEnvironment boolean
        if (installDependenciesConfigurations.useInProjectVirtualEnvironment() && Boolean.FALSE.equals(Boolean.valueOf(currentInProjectSetting))) {
            log.info("Configuring Poetry to use an in-project virtual environment...");
            configureVirtualEnvironmentsInProject(true);
        } else if (!installDependenciesConfigurations.useInProjectVirtualEnvironment() && Boolean.TRUE.equals(Boolean.valueOf(currentInProjectSetting))) {
            configureVirtualEnvironmentsInProject(false);
        }
    }

    private void configureVirtualEnvironmentsInProject(boolean enable) throws MojoExecutionException {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);
        List<String> arguments = new ArrayList<>();
        arguments.add("config");
        arguments.add("virtualenvs.in-project");
        arguments.add(Boolean.toString(enable));
        arguments.add("--local");
        poetryHelper.executeAndLogOutput(arguments);
    }

    private void prepareRepositoryForInstallation(String repoId, String repoUrl) throws MojoExecutionException {
        if (StringUtils.isNotEmpty(repoUrl) && installDependenciesConfigurations.addPypiRepoAsPackageSources()) {
            String pypiRepoSimpleIndexUrl;
            try {
                pypiRepoSimpleIndexUrl = getPyPiRepoSimpleIndexUrl(repoUrl);
            } catch (URISyntaxException e) {
                throw new MojoExecutionException(
                        String.format("Could not parse configured repoUrl %s", repoUrl), e);
            }

            // NB later version of Poetry will support retrieving and configuring package
            // source repositories via the "poetry source" command in future releases, but
            // for now we need to manually inspect and modify the package's pyproject.toml
            Config matchingPypiRepoSourceConfig;
            try (FileConfig pyProjectConfig = FileConfig.of(getPyProjectTomlFile())) {
                pyProjectConfig.load();

                Optional<List<Config>> packageSources = pyProjectConfig.getOptional(PYPROJECT_PACKAGE_SOURCES_PATH);
                matchingPypiRepoSourceConfig = packageSources.orElse(Collections.emptyList()).stream()
                        .filter(packageSource -> pypiRepoSimpleIndexUrl.equals(packageSource.get("url"))).findFirst()
                        .orElse(Config.inMemory());
            }

            if (!matchingPypiRepoSourceConfig.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Configured PyPi repository %s found in the following pyproject.toml [[%s]] array element: %s",
                            repoUrl, PYPROJECT_PACKAGE_SOURCES_PATH, matchingPypiRepoSourceConfig));
                }
            } else {
                // NB NightConfig's TOML serializer generates TOML in a manner that makes it
                // difficult to append an array element of tables to an existing TOML
                // configuration, so manually write out the desired new repository TOML
                // configuration with human-readable formatting
                List<String> newPypiRepoSourceConfig = Arrays.asList(System.lineSeparator(), String.format(
                                "# Added by habushu-maven-plugin at %s to use %s as source PyPi repository for installing dependencies",
                                LocalDateTime.now(), pypiRepoSimpleIndexUrl),
                        String.format("[[%s]]", PYPROJECT_PACKAGE_SOURCES_PATH),
                        String.format("name = \"%s\"",
                                StringUtils.isNotEmpty(repoId) && !PUBLIC_PYPI_REPO_ID.equals(repoId)
                                        ? repoId
                                        : "private-pypi-repo"),
                        String.format("url = \"%s\"", pypiRepoSimpleIndexUrl), "priority = \"supplemental\"");
                log.info(String.format("Private PyPi repository entry for %s not found in pyproject.toml",
                        repoUrl));
                log.info(String.format(
                        "Adding %s to pyproject.toml as supplemental repository from which dependencies may be installed",
                        pypiRepoSimpleIndexUrl));
                try {
                    Files.write(getPyProjectTomlFile().toPath(), newPypiRepoSourceConfig,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new MojoExecutionException(String.format(
                            "Could not write new [[%s]] element to pyproject.toml", PYPROJECT_PACKAGE_SOURCES_PATH), e);
                }
            }

        }
    }

    /**
     * Attempts to infer the PEP-503 compliant PyPI simple repository index URL
     * associated with the provided PyPI repository URL. In order to configure
     * Poetry to use a private PyPi repository as a source for installing package
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

                // Look for the standard Poetry dependency groups:
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, TomlUtils.TOOL_POETRY_DEPENDENCIES);
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, TomlUtils.TOOL_POETRY_DEV_DEPENDENCIES);

                // Search for custom Poetry dependency groups:
                List<String> toolPoetryGroupSections = findCustomToolPoetryGroups();
                for (String toolPoetryGroupSection : toolPoetryGroupSections) {
                    executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, toolPoetryGroupSection);
                }

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

        Optional<Config> toolPoetryDependencies = pyProjectConfig.getOptional(tomlSection);
        if (toolPoetryDependencies.isPresent()) {
            Config foundDependencies = toolPoetryDependencies.get();
            Map<String, Object> dependencyMap = foundDependencies.valueMap();
            for (PackageDefinition def : installDependenciesConfigurations.getManagedDependencies()) {
                String packageName = def.getPackageName();
                if (dependencyMap.containsKey(packageName)) {
                    Object packageRhs = dependencyMap.get(packageName);

                    if (TomlUtils.representsLocalDevelopmentVersion(packageRhs)) {
                        log.info(String.format("%s does not have a specific version to manage - skipping", packageName));
                        log.debug(String.format("\t %s", packageRhs.toString()));
                        continue;
                    }

                    performComparisonAndStageNeededChanges(replacements, def, packageRhs);
                }
            }
        }
    }

    private void performComparisonAndStageNeededChanges(Map<String, TomlReplacementTuple> replacements, PackageDefinition def, Object packageRhs) {
        String originalOperatorAndVersion = getOperatorAndVersion(packageRhs);
        String updatedOperatorAndVersion = def.getOperatorAndVersion();

        String packageName = def.getPackageName();

        if (installDependenciesConfigurations.overridePackageVersion() && updatedOperatorAndVersion.contains(SNAPSHOT)) {
            updatedOperatorAndVersion = replaceSnapshotWithWildcard(updatedOperatorAndVersion);
        }

        boolean mismatch = !originalOperatorAndVersion.equals(updatedOperatorAndVersion);

        if (mismatch) {
            if (def.isActive()) {
                TomlReplacementTuple tuple = new TomlReplacementTuple(packageName, originalOperatorAndVersion, updatedOperatorAndVersion);
                replacements.put(packageName, tuple);
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
                String fileContent = StringUtils.EMPTY;

                try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
                    String line = reader.readLine();

                    while (line != null) {
                        if (line.contains(StringUtils.SPACE) || line.contains(EQUALS)) {
                            String key = line.substring(0, line.indexOf(StringUtils.SPACE));

                            if (key == null) {
                                key = line.substring(0, line.indexOf(EQUALS));
                            }

                            if (key != null) {
                                key = key.strip();

                                TomlReplacementTuple matchedTuple = replacements.get(key);
                                if (matchedTuple != null) {
                                    String original = TomlUtils.escapeTomlRightHandSide(matchedTuple.getOriginalOperatorAndVersion());
                                    String updated = TomlUtils.escapeTomlRightHandSide(matchedTuple.getUpdatedOperatorAndVersion());

                                    if (line.endsWith(original)) {
                                        line = line.replace(original, updated);
                                        log.info(String.format("Updated %s: %s --> %s", matchedTuple.getPackageName(),
                                                original, updated));
                                    }
                                }
                            }
                        }

                        fileContent += line + "\n";

                        line = reader.readLine();
                    }

                } catch (IOException e) {
                    throw new HabushuException("Problem reading pyproject.toml to update with managed dependencies!", e);
                }

                try {
                    TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent);

                } catch (IOException e) {
                    throw new HabushuException("Problem writing pyproject.toml with managed dependency updates!", e);
                }

            }
        }
    }

    protected String getOperatorAndVersion(Object rawData) {
        String operatorAndVersion = null;
        if (rawData instanceof String) {
            operatorAndVersion = (String) rawData;

        } else if (rawData instanceof CommentedConfig) {
            operatorAndVersion = TomlUtils.convertCommentedConfigToToml((CommentedConfig) rawData);

        } else {
            log.warn(String.format("Could not process type %s - attempting to use toString() value!", rawData.getClass()));
            operatorAndVersion = rawData.toString();
        }

        return operatorAndVersion;

    }

    protected static String replaceSnapshotWithWildcard(String pomVersion) {
        return pomVersion.substring(0, pomVersion.indexOf(SNAPSHOT)) + ".*";
    }

    /**
     * Finds and returns all custom tool poetry groups.
     *
     * @return list of custom tool poetry groups.
     */
    private List<String> findCustomToolPoetryGroups() {
        List<String> toolPoetryGroupSections = new ArrayList<>();

        File pyProjectTomlFile = getPyProjectTomlFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();

            while (line != null) {
                line = line.strip();

                if (line.startsWith("[tool.poetry.group")) {
                    toolPoetryGroupSections.add(line.replace("[", StringUtils.EMPTY).replace("]", StringUtils.EMPTY));
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new HabushuException("Problem reading pyproject.toml to search for custom dependency groups!", e);
        }

        return toolPoetryGroupSections;
    }

}
