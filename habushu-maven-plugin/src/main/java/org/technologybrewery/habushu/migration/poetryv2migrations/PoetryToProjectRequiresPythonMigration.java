package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Provides functionality to migrate python dependency from the [tool.poetry.dependencies] section to the [project] section of a TOML file.
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryToProjectRequiresPythonMigration extends AbstractPoetryMigration{
    private static final Logger logger = LoggerFactory.getLogger(PoetryToProjectRequiresPythonMigration.class);
    private String pythonDependencyVersion;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        boolean shouldExecute = false;

        if (!isPoetryVersionAtLeast2){
            return false;
        }

        try (FileConfig tomlFileConfig = FileConfig.of(file)){
            tomlFileConfig.load();
            Optional<Config> projectGroup = tomlFileConfig.getOptional(TomlUtils.PROJECT);

            if (projectGroup.isPresent()){
                Config projectGroupEntry = projectGroup.get();

                if (!projectGroupEntry.contains(TomlUtils.REQUIRES_PYTHON)) {
                    shouldExecute = true;
                    logger.info("Adding to [{}] group entry! ({})", TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON);
                }

                // grab original python dependency version from [tool.poetry.dependencies]
                Optional<Config> poetryDependenciesGroup = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_DEPENDENCIES);
                if (poetryDependenciesGroup.isPresent()) {
                    Config poetryDependenciesGroupEntry = poetryDependenciesGroup.get();
                    if (poetryDependenciesGroupEntry.contains(TomlUtils.PYTHON)) {
                        pythonDependencyVersion = poetryDependenciesGroupEntry.get(TomlUtils.PYTHON);
                    }
                }
            }
        }

        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        boolean inProjectSection = false;
        boolean inPoetryDependenciesSection = false;
        boolean injectedRequiresPython = false;
        boolean injectAfterNextEmptyLine = false;
        String requiresPythonLine = TomlUtils.REQUIRES_PYTHON + " " + TomlUtils.EQUALS + " " + TomlUtils.DOUBLE_QUOTE + pythonDependencyVersion + TomlUtils.DOUBLE_QUOTE;
        StringBuilder fileContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))){
            String line = reader.readLine();

            while (line != null){
                boolean isEmptyLine = line.isBlank();
                boolean addLine = true;
                String trimmedLine = line.strip();

                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    if(trimmedLine.equals("[" + TomlUtils.PROJECT + "]")){
                        inProjectSection = true;
                        inPoetryDependenciesSection = false;
                    } else if (trimmedLine.equals("[" + TomlUtils.TOOL_POETRY_DEPENDENCIES + "]")){
                        inProjectSection = false;
                        inPoetryDependenciesSection = true;
                    } else {
                        inProjectSection = false;
                        inPoetryDependenciesSection = false;
                    }
                }
                if (trimmedLine.contains(TomlUtils.EQUALS)) {
                    if (inPoetryDependenciesSection) {
                        // If in the [tool.poetry.dependencies] section, skip the line that defines the python dependency
                        // considers cases where there are other dependencies whose name starts with 'python'
                        int equalIndex = trimmedLine.indexOf(TomlUtils.EQUALS);
                        String key = trimmedLine.substring(0, equalIndex).strip();

                        // Only skip the line if the key exactly matches "python"
                        if (key.equalsIgnoreCase(TomlUtils.PYTHON)) {
                            addLine = false;
                        }
                    } else if (inProjectSection && !injectedRequiresPython){
                        injectAfterNextEmptyLine = true;
                    }
                }
                if (isEmptyLine && injectAfterNextEmptyLine ) {
                    fileContent.append(requiresPythonLine).append("\n");
                    injectedRequiresPython = true;
                    injectAfterNextEmptyLine = false;
                }

                if (addLine) {
                    fileContent.append(line).append("\n");
                }

                line = reader.readLine();
            }

        }catch (IOException e) {
            throw new BatonException("Problem reading pyproject.toml for migration!", e);
        }

        try {
            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent.toString());
        } catch (IOException e) {
            throw new BatonException("Problem writing updated pyproject.toml!", e);
        }

        return true;
    }

}
