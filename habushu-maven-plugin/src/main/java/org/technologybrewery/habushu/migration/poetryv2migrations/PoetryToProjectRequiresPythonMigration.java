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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides functionality to migrate python dependency from the tool.poetry.dependencies section to the [project] section of a TOML file.
 * If python dependency exists in the tool.poetry.dependencies and the project section,
 * then the duplicate constraint is removed from tool.poetry.dependencies.
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryToProjectRequiresPythonMigration extends AbstractPoetryMigration{
    private static final Logger logger = LoggerFactory.getLogger(PoetryToProjectRequiresPythonMigration.class);
    private String pythonDependencyVersion;
    private boolean updateFaultyFormat;
    private boolean missingRequiresPython;
    private boolean hasInlineDependenciesTable;
    private boolean hasPoetryPython;
    private List<String> poetryDependenciesKeys = new ArrayList<>();

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        if (super.shouldExecuteOnFile(file)) {
            try (FileConfig tomlFileConfig = FileConfig.of(file)){
                tomlFileConfig.load();

                Optional<Config> poetryDependenciesGroup = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_DEPENDENCIES);
                if (poetryDependenciesGroup.isPresent()) {
                    Config poetryDependenciesGroupEntry = poetryDependenciesGroup.get();

                    if (poetryDependenciesGroupEntry.contains(TomlUtils.PYTHON)){
                        hasPoetryPython = true;
                        poetryDependenciesKeys = poetryDependenciesGroupEntry.entrySet().stream().map(Config.Entry::getKey).collect(Collectors.toList());
                        pythonDependencyVersion = poetryDependenciesGroupEntry.get(TomlUtils.PYTHON);

                        if (pythonDependencyVersion.contains(TomlUtils.CARET)) {
                            pythonDependencyVersion = TomlUtils.refactorCaretIntoGreaterThanLessThan(pythonDependencyVersion);
                        }

                        List<String> allTomlHeaders = TomlUtils.extractTomlSectionHeaders(file);
                        hasInlineDependenciesTable = !allTomlHeaders.contains(TomlUtils.TOOL_POETRY_DEPENDENCIES);
                    }
                }

                Optional<Config> projectGroup = tomlFileConfig.getOptional(TomlUtils.PROJECT);
                if (projectGroup.isPresent()){
                    Config projectGroupEntry = projectGroup.get();

                    if (!projectGroupEntry.contains(TomlUtils.REQUIRES_PYTHON)) {
                        missingRequiresPython = true;
                        logger.info("Adding to [{}] group entry! ({})", TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON);
                    } else {
                        String requiresPythonSemver = projectGroupEntry.get(TomlUtils.REQUIRES_PYTHON);
                        if (requiresPythonSemver.contains(TomlUtils.CARET)) {
                            updateFaultyFormat = true;
                            pythonDependencyVersion = TomlUtils.refactorCaretIntoGreaterThanLessThan(requiresPythonSemver);
                            logger.info("Reformatting ({}) in group entry [{}] to use >=,< notation!",
                                    TomlUtils.REQUIRES_PYTHON,
                                    TomlUtils.PROJECT
                            );
                        }
                    }
                }
            }
        }
        return missingRequiresPython || updateFaultyFormat || (!missingRequiresPython && hasPoetryPython);
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        boolean injectAfterNextEmptyLine = false;
        String requiresPythonLine = TomlUtils.REQUIRES_PYTHON + " " + TomlUtils.EQUALS + " " + TomlUtils.DOUBLE_QUOTE + pythonDependencyVersion + TomlUtils.DOUBLE_QUOTE;
        StringBuilder fileContent = new StringBuilder();
        String currentSection = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))){
            String line = reader.readLine();

            while (line != null){
                boolean isEmptyLine = line.isBlank();
                boolean addLine = true;
                String trimmedLine = line.strip();

                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    currentSection = trimmedLine.substring(1, trimmedLine.length()-1);
                }
                if (trimmedLine.contains(TomlUtils.EQUALS)) {
                    if (TomlUtils.TOOL_POETRY_DEPENDENCIES.equals(currentSection) && hasPoetryPython) {
                        // If in the [tool.poetry.dependencies] section, skip the line that defines the python dependency
                        int equalIndex = trimmedLine.indexOf(TomlUtils.EQUALS);
                        String key = trimmedLine.substring(0, equalIndex).strip();

                        // Only skip the line if the key exactly matches "python"
                        if (key.equalsIgnoreCase(TomlUtils.PYTHON)) {
                            addLine = false;
                            hasPoetryPython = false;
                        }

                    } else if (TomlUtils.PROJECT.equals(currentSection)) {
                        if (updateFaultyFormat && trimmedLine.contains(TomlUtils.REQUIRES_PYTHON)){
                            // Update the faulty formatted "requires-python" line to use greater-than or less-than notation
                            addLine = false;
                            fileContent.append(requiresPythonLine).append("\n");
                            updateFaultyFormat = false;
                        } else if (missingRequiresPython){
                            // Otherwise inject the missing "requires-python" line
                            injectAfterNextEmptyLine = true;
                            missingRequiresPython = false;
                        }
                    } else if (TomlUtils.TOOL_POETRY.equals(currentSection) && hasInlineDependenciesTable && trimmedLine.contains(TomlUtils.DEPENDENCIES)){
                        addLine = false;

                        // Only need to remove python if other dependencies exist, otherwise we can just skip the line
                        if (poetryDependenciesKeys.size() > 1){
                            String regex;
                            if(poetryDependenciesKeys.indexOf(TomlUtils.PYTHON) == poetryDependenciesKeys.size()-1){
                                // if python is the last element, we drop the preceding comma + python entry
                                regex = TomlUtils.COMMA_PATTERN.pattern() + TomlUtils.PYTHON_DEPENDENCIES_PATTERN.pattern();
                            } else {
                                // otherwise, we drop the python entry + trailing comma
                                regex = TomlUtils.PYTHON_DEPENDENCIES_PATTERN.pattern() + TomlUtils.COMMA_PATTERN.pattern();
                            }
                            fileContent.append(trimmedLine.replaceAll(regex, "")).append("\n");
                        }
                        hasInlineDependenciesTable = false;
                    }
                }
                if (isEmptyLine && injectAfterNextEmptyLine ) {
                    fileContent.append(requiresPythonLine).append("\n");
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
