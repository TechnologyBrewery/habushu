package org.technologybrewery.habushu;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
 * the module's pyproject.toml configuration as a supplemental source of
 * dependencies, if it is not already configured in the pyproject.toml
 */
public class InstallDependenciesPoetry extends AbstractInstallDependencies {

    private static final String EQUALS = "=";
    private static final String POETRY_CLEAN_CACHE_COMMAND = "poetry cache clear . --all";

    /**
     * Path within a Poetry project's pyproject.toml configuration at which private
     * PyPi repositories may be specified as sources from which dependencies may be
     * resolved and installed.
     */
    protected static final String PYPROJECT_PACKAGE_SOURCES_PATH = "tool.poetry.source";

    public InstallDependenciesPoetry(File baseDir, Log log, InstallDependenciesMojo mojo)  {
        super(baseDir, log, mojo);
    }


    @Override
    public void doExecute() throws MojoFailureException {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);

        setUpInProjectVirtualEnvironment(poetryHelper);

        processManagedDependencyMismatches();

        prepareRepositoryForInstallation(installDependenciesMojo.getPypiRepoId(), installDependenciesMojo.getPypiRepoUrl(), PYPROJECT_PACKAGE_SOURCES_PATH);
        if (installDependenciesMojo.useDevRepository()) {
            prepareRepositoryForInstallation(installDependenciesMojo.getDevRepositoryId(), installDependenciesMojo.getDevRepositoryUrl(), PYPROJECT_PACKAGE_SOURCES_PATH);
        }

        if (!installDependenciesMojo.skipPoetryLockUpdate()) {
            log.info("Locking dependencies specified in pyproject.toml...");
            poetryHelper.executePackageManagerCommandAndLogAfterTimeout(
                    poetryHelper.createLockCommand(installDependenciesMojo.skipPoetryLockUpdate()),
                    2,
                    TimeUnit.MINUTES,
                    POETRY_CLEAN_CACHE_COMMAND
            );

        }

        List<String> installCommand = poetryHelper.createInstallCommand(installDependenciesMojo.forceSync());

        for (String groupName : installDependenciesMojo.getWithGroups()) {
            installCommand.add("--with");
            installCommand.add(groupName);
        }
        for (String groupName : installDependenciesMojo.getWithoutGroups()) {
            installCommand.add("--without");
            installCommand.add(groupName);
        }

        log.info("Installing dependencies...");
        poetryHelper.executePackageManagerCommandAndLogAfterTimeout(installCommand, 2, TimeUnit.MINUTES, POETRY_CLEAN_CACHE_COMMAND);
    }

    private void setUpInProjectVirtualEnvironment(PoetryCommandHelper poetryHelper) {
        List<String> arguments = new ArrayList<>();
        arguments.add("config");
        arguments.add("virtualenvs.in-project");
        arguments.add("--local");
        String currentInProjectSetting = poetryHelper.execute(arguments);
        
        // update the poetry config to match the useInProjectVirtualEnvironment boolean
        if (installDependenciesMojo.useInProjectVirtualEnvironment() && Boolean.FALSE.equals(Boolean.valueOf(currentInProjectSetting))) {
            log.info("Configuring Poetry to use an in-project virtual environment...");
            configureVirtualEnvironmentsInProject(true);
        } else if (!installDependenciesMojo.useInProjectVirtualEnvironment() && Boolean.TRUE.equals(Boolean.valueOf(currentInProjectSetting))) {
            configureVirtualEnvironmentsInProject(false);
        }
    }

    private void configureVirtualEnvironmentsInProject(boolean enable) {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);
        List<String> arguments = new ArrayList<>();
        arguments.add("config");
        arguments.add("virtualenvs.in-project");
        arguments.add(Boolean.toString(enable));
        arguments.add("--local");
        poetryHelper.executeAndLogOutput(arguments);
    }

    protected void processManagedDependencyMismatches() {
        if (!installDependenciesMojo.getManagedDependencies().isEmpty()) {
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
                if (installDependenciesMojo.failOnManagedDependenciesMismatches() || !installDependenciesMojo.updateManagedDependenciesWhenFound()) {
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
            for (PackageDefinition def : installDependenciesMojo.getManagedDependencies()) {
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

        if (installDependenciesMojo.overridePackageVersion() && updatedOperatorAndVersion.contains(SNAPSHOT)) {
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
            if (installDependenciesMojo.failOnManagedDependenciesMismatches()) {
                if (installDependenciesMojo.updateManagedDependenciesWhenFound()) {
                    log.warn("updateManagedDependenciesWhenFound=true will never be processed when failOnManagedDependenciesMismatches also equals true!");
                }

                throw new HabushuException("Found managed dependencies - please fix before proceeding!  "
                        + "(see 'Package abc is not up to date with common project package definition guidance!` log messages above!");
            }

            if (installDependenciesMojo.updateManagedDependenciesWhenFound()) {
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
