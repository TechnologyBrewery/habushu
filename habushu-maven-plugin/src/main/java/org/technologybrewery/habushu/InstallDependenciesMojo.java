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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Installs dependencies defined in the project's pyproject.toml configuration,
 * specifically by running "poetry lock" followed by "poetry install". If a
 * private PyPi repository is defined via
 * {@link AbstractHabushuMojo#pypiRepoUrl} (and
 * {@link AbstractHabushuMojo#pypiRepoId}), it will be automatically added to
 * the module's pyproject.toml configuration as a secondary source of
 * dependencies, if it is not already configured in the pyproject.toml
 */
@Mojo(name = "install-dependencies", defaultPhase = LifecyclePhase.COMPILE)
public class InstallDependenciesMojo extends AbstractHabushuMojo {

    private static final String EQUALS = "=";
    private static final String DOUBLE_QUOTE = "\"";

    /**
     * Configures whether a private PyPi repository, if specified via
     * {@link AbstractHabushuMojo#pypiRepoUrl}, is automatically added as a package
     * source from which dependencies may be installed. This value is <b>*only*</b>
     * utilized if a private PyPi repository is specified via
     * {@link AbstractHabushuMojo#pypiRepoUrl}.
     */
    @Parameter(defaultValue = "true", property = "habushu.addPypiRepoAsPackageSources")
    private boolean addPypiRepoAsPackageSources;

    /**
     * Configures the path for the simple index on a private pypi repository.
     * Certain private repository solutions (ie: devpi) use different names for the
     * simple index. devpi, for instance, uses "+simple".
     */
    @Parameter(property = "habushu.pypiSimpleSuffix", defaultValue = "simple")
    private String pypiSimpleSuffix;

    /**
     * Configures whether the poetry lock file will be updated before poetry
     * install.
     */
    @Parameter(defaultValue = "false", property = "habushu.skipPoetryLockUpdate")
    private boolean skipPoetryLockUpdate;

    /**
     * Path within a Poetry project's pyproject.toml configuration at which private
     * PyPi repositories may be specified as sources from which dependencies may be
     * resolved and installed.
     */
    protected static final String PYPROJECT_PACKAGE_SOURCES_PATH = "tool.poetry.source";

    /**
     * Specifies Poetry groups to include in the installation.
     */
    @Parameter(property = "habushu.withGroups")
    private String[] withGroups;

    /**
     * Specifies Poetry groups to exclude from the installation.
     */
    @Parameter(property = "habushu.withoutGroups")
    private String[] withoutGroups;

    /**
     * Configuration option to include the --sync option on poetry install
     */
    @Parameter(defaultValue = "false", property = "habushu.forceSync")
    private boolean forceSync;

    /**
     * The set of managed dependencies to monitor for conformance.  These can result in:
     * * direct changes to your pyproject.toml file (default behavior)
     * * log statements warning of mismatches (if habushu.updateManagedDependenciesWhenFound = false)
     * * stopping the build for manual intervention (if habushu.failOnManagedDependenciesMismatches = true)
     */
    @Parameter(property = "habushu.managedDependencies")
    protected List<PackageDefinition> managedDependencies;

    /**
     * Determines if managed dependency mismatches are automatically updated when encountered.
     */
    @Parameter(defaultValue = "true", property = "habushu.updateManagedDependenciesWhenFound")
    protected boolean updateManagedDependenciesWhenFound;

    /**
     * Determines if the build should be failed when managed dependency mismatches are found.
     */
    @Parameter(defaultValue = "false", property = "habushu.failOnManagedDependenciesMismatches")
    protected boolean failOnManagedDependenciesMismatches;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

        processManagedDependencyMismatches();

        if (StringUtils.isNotEmpty(this.pypiRepoUrl) && this.addPypiRepoAsPackageSources) {
            String pypiRepoSimpleIndexUrl;
            try {
                pypiRepoSimpleIndexUrl = getPyPiRepoSimpleIndexUrl(pypiRepoUrl);
            } catch (URISyntaxException e) {
                throw new MojoExecutionException(
                        String.format("Could not parse configured pypiRepoUrl %s", this.pypiRepoUrl), e);
            }

            // NB later version of Poetry will support retrieving and configuring package
            // source repositories via the "poetry source" command in future releases, but
            // for now we need to manually inspect and modify the package's pyproject.toml
            Config matchingPypiRepoSourceConfig;
            try (FileConfig pyProjectConfig = FileConfig.of(getPoetryPyProjectTomlFile())) {
                pyProjectConfig.load();

                Optional<List<Config>> packageSources = pyProjectConfig.getOptional(PYPROJECT_PACKAGE_SOURCES_PATH);
                matchingPypiRepoSourceConfig = packageSources.orElse(Collections.emptyList()).stream()
                        .filter(packageSource -> pypiRepoSimpleIndexUrl.equals(packageSource.get("url"))).findFirst()
                        .orElse(Config.inMemory());
            }

            if (!matchingPypiRepoSourceConfig.isEmpty()) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug(String.format(
                            "Configured PyPi repository %s found in the following pyproject.toml [[%s]] array element: %s",
                            this.pypiRepoUrl, PYPROJECT_PACKAGE_SOURCES_PATH, matchingPypiRepoSourceConfig));
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
                                StringUtils.isNotEmpty(this.pypiRepoId) && !PUBLIC_PYPI_REPO_ID.equals(this.pypiRepoId)
                                        ? this.pypiRepoId
                                        : "private-pypi-repo"),
                        String.format("url = \"%s\"", pypiRepoSimpleIndexUrl), "secondary = true");
                getLog().info(String.format("Private PyPi repository entry for %s not found in pyproject.toml",
                        this.pypiRepoUrl));
                getLog().info(String.format(
                        "Adding %s to pyproject.toml as secondary repository from which dependencies may be installed",
                        pypiRepoSimpleIndexUrl));
                try {
                    Files.write(getPoetryPyProjectTomlFile().toPath(), newPypiRepoSourceConfig,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new MojoExecutionException(String.format(
                            "Could not write new [[%s]] element to pyproject.toml", PYPROJECT_PACKAGE_SOURCES_PATH), e);
                }
            }

        }

        if (!this.skipPoetryLockUpdate) {
            getLog().info("Locking dependencies specified in pyproject.toml...");
            poetryHelper.executePoetryCommandAndLogAfterTimeout(Arrays.asList("lock"), 2, TimeUnit.MINUTES);
        }

        List<String> installCommand = new ArrayList<>();

        installCommand.add("install");
        for (String groupName : this.withGroups) {
            installCommand.add("--with");
            installCommand.add(groupName);
        }
        for (String groupName : this.withoutGroups) {
            installCommand.add("--without");
            installCommand.add(groupName);
        }
        if (this.forceSync) {
            installCommand.add("--sync");
        }

        getLog().info("Installing dependencies...");
        poetryHelper.executePoetryCommandAndLogAfterTimeout(installCommand, 2, TimeUnit.MINUTES);
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
        if (!this.pypiSimpleSuffix.equals(lastPathSegment)) {
            // If the URL has no path, an unmodifiable Collections.emptyList() is returned,
            // so wrap in an ArrayList to enable later modifications
            repoUriPathSegments = new ArrayList<>(repoUriPathSegments);
            repoUriPathSegments.add(this.pypiSimpleSuffix);
            pypiRepoUriBuilder.setPathSegments(repoUriPathSegments);
        }

        return StringUtils.appendIfMissing(pypiRepoUriBuilder.build().toString(), "/");
    }

    protected void processManagedDependencyMismatches() {
        if (!managedDependencies.isEmpty()) {
            Map<String, TomlReplacementTuple> replacements = new HashMap<>();

            try (FileConfig pyProjectConfig = FileConfig.of(getPoetryPyProjectTomlFile())) {
                pyProjectConfig.load();

                // Look for the standard Poetry dependency groups:
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, "tool.poetry.dependencies");
                executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, "tool.poetry.dev-dependencies");

                // Search for custom Poetry dependency groups:
                List<String> toolPoetryGroupSections = findCustomToolPoetryGroups();
                for (String toolPoetryGroupSection : toolPoetryGroupSections) {
                    executeDetailedManagedDependencyMismatchActions(replacements, pyProjectConfig, toolPoetryGroupSection);
                }

                // Log replacements, if appropriate:
                if (failOnManagedDependenciesMismatches || !updateManagedDependenciesWhenFound) {
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

            for (PackageDefinition def : managedDependencies) {
                String packageName = def.getPackageName();
                if (dependencyMap.containsKey(packageName)) {
                    Object packageRhs = dependencyMap.get(packageName);

                    if (representsLocalDevelopmentVersion(packageRhs)) {
                        getLog().info(String.format("%s does not have a specific version to manage - skipping", packageName));
                        getLog().debug(String.format("\t %s", packageRhs.toString()));
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

        if (overridePackageVersion && updatedOperatorAndVersion.contains(SNAPSHOT)) {
            updatedOperatorAndVersion = replaceSnapshotWithDev(updatedOperatorAndVersion);
        }

        boolean mismatch = !originalOperatorAndVersion.equals(updatedOperatorAndVersion);

        if (mismatch) {
            if (def.isActive()) {
                TomlReplacementTuple tuple = new TomlReplacementTuple(packageName, originalOperatorAndVersion, updatedOperatorAndVersion);
                replacements.put(packageName, tuple);
            } else {
                getLog().info(String.format("Package %s is not up to date with common project package definition guidance, "
                        + "but the check has been inactivated", packageName));
            }
        }
    }

    private void logPackageMismatch(String packageName, String originalOperatorAndVersion, String updatedOperatorAndVersion) {
        getLog().warn(String.format("Package %s is not up to date with common project package definition guidance! "
                + "Currently %s, but should be %s!", packageName, originalOperatorAndVersion, updatedOperatorAndVersion));
    }

    protected List<String> findCustomToolPoetryGroups() {
        List<String> toolPoetryGroupSections = new ArrayList<>();

        File pyProjectTomlFile = getPoetryPyProjectTomlFile();

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

    protected void performPendingDependencyReplacements(Map<String, TomlReplacementTuple> replacements) {
        if (MapUtils.isNotEmpty(replacements)) {
            if (failOnManagedDependenciesMismatches) {
                if (updateManagedDependenciesWhenFound) {
                    getLog().warn("updateManagedDependenciesWhenFound=true will never be processed when failOnManagedDependenciesMismatches also equals true!");
                }

                throw new HabushuException("Found managed dependencies - please fix before proceeding!  "
                        + "(see 'Package abc is not up to date with common project package definition guidance!` log messages above!");
            }

            if (updateManagedDependenciesWhenFound) {
                File pyProjectTomlFile = getPoetryPyProjectTomlFile();
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
                                    String original = escapeTomlRightHandSide(matchedTuple.getOriginalOperatorAndVersion());
                                    String updated = escapeTomlRightHandSide(matchedTuple.getUpdatedOperatorAndVersion());

                                    if (line.endsWith(original)) {
                                        line = line.replace(original, updated);
                                        getLog().info(String.format("Updated %s: %s --> %s", matchedTuple.getPackageName(),
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

                writeTomlFile(pyProjectTomlFile, fileContent);
            }
        }
    }

    /**
     * Handles escaping with double quotes only if the value is not an inline table.
     *
     * @param valueToEscape value to potentially escape
     * @return value ready to write to toml file
     */
    protected static String escapeTomlRightHandSide(String valueToEscape) {
        return (!valueToEscape.contains("{")) ? DOUBLE_QUOTE + valueToEscape + DOUBLE_QUOTE : valueToEscape;
    }

    private static void writeTomlFile(File pyProjectTomlFile, String fileContent) {
        if (fileContent != null) {
            try (Writer writer = new FileWriter(pyProjectTomlFile)) {
                writer.write(fileContent);
            } catch (IOException e) {
                throw new HabushuException("Problem writing pyproject.toml with managed dependency updates!", e);
            }
        }
    }

    protected boolean representsLocalDevelopmentVersion(Object rawData) {
        boolean localDevelopmentVersion = false;

        if (rawData instanceof CommentedConfig) {
            CommentedConfig config = (CommentedConfig) rawData;
            if (!config.contains("version")) {
                localDevelopmentVersion = true;
            }

        }

        return localDevelopmentVersion;
    }

    protected String getOperatorAndVersion(Object rawData) {
        String operatorAndVersion = null;
        if (rawData instanceof String) {
            operatorAndVersion = (String) rawData;

        } else if (rawData instanceof CommentedConfig) {
            operatorAndVersion = convertCommentedConfigToToml((CommentedConfig) rawData);

        } else {
            getLog().warn(String.format("Could not process type %s - attempting to use toString() value!", rawData.getClass()));
            operatorAndVersion = rawData.toString();
        }

        return operatorAndVersion;

    }

    protected static String convertCommentedConfigToToml(CommentedConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("version = \"").append(config.get("version").toString()).append("\"");
        List<String> extras = config.get("extras");
        if (CollectionUtils.isNotEmpty(extras)) {
            sb.append(", extras = [");
            // NB: if we expect more complex values, such as multiple extras, more work would need to be done for
            // both consistent formatting and comparison of these values.  However, at the time of initially writing
            // this method, there isn't a clear demand signal, so we are going to KISS for now:

            for (int i = 0; i < extras.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(extras.get(i)).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");

        return sb.toString();
    }

    private class TomlReplacementTuple {
        private String packageName;

        private String originalOperatorAndVersion;

        private String updatedOperatorAndVersion;

        public TomlReplacementTuple(String packageName, String originalOperatorAndVersion, String updatedOperatorAndVersion) {
            this.packageName = packageName;
            this.originalOperatorAndVersion = originalOperatorAndVersion;
            this.updatedOperatorAndVersion = updatedOperatorAndVersion;

        }

        public String getPackageName() {
            return packageName;
        }

        public String getOriginalOperatorAndVersion() {
            return originalOperatorAndVersion;
        }

        public String getUpdatedOperatorAndVersion() {
            return updatedOperatorAndVersion;
        }
    }

}