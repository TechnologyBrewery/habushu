package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Provides functionality to migrate configuration entries from the [tool.poetry] section to the [project] section of a TOML file.
 * Migrates any entry included in entriesToMigrate that does not already exist.
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryToProjectMigration extends AbstractPoetryMigration {
    private static final Logger logger = LoggerFactory.getLogger(PoetryToProjectMigration.class);
    private boolean hasProjectGroup = false;
    private final List<String> entriesToMigrate = Arrays.asList("name", "description", "version", "authors", "maintainers", "license");
    private final List<String> existingProjectKeys = new ArrayList<>();
    private final Map<String, TomlReplacementTuple> replacements = new LinkedHashMap<>();

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        replacements.clear();
        boolean shouldExecute = false;
        Config.setInsertionOrderPreserved(true);

        if (!isPoetryVersionAtLeast2) {
            return false;
        }

        try (FileConfig tomlFileConfig = FileConfig.of(file)) {
            tomlFileConfig.load();
            Optional<Config> poetryGroupOpt = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY);

            if (poetryGroupOpt.isPresent()){
                Config poetryGroup = poetryGroupOpt.get();
                for (Config.Entry groupEntry : poetryGroup.entrySet()) {

                    // check if any of the entries under [tool.poetry] match one of "name", "description", or "version"
                    String groupEntryName = groupEntry.getKey();
                    Object groupEntryRhs = groupEntry.getValue();
                    String groupEntryRhsAsString = null;

                    if (entriesToMigrate.stream().anyMatch(e -> e.equalsIgnoreCase(groupEntryName))) {
                        if (groupEntryRhs instanceof List<?>){
                            List<String> listValues = poetryGroup.get(groupEntryName);
                            List<String> listOfStrings = new ArrayList<>(listValues);
                            groupEntryRhsAsString = convertListOfStringsToString(listOfStrings);
                        } else {
                            groupEntryRhsAsString = (String) groupEntryRhs;
                        }

                        logger.info("Found [{}] group entry to migrate! ({} = {})", TomlUtils.TOOL_POETRY, groupEntryName, groupEntryRhsAsString);
                        TomlReplacementTuple replacementTuple = new TomlReplacementTuple(groupEntryName, groupEntryRhsAsString, "");
                        replacements.put(groupEntryName, replacementTuple);
                        shouldExecute = true;
                    }
                }
            }
            // process the [project] section and record existing keys
            Optional<Config> projectGroupEntries = tomlFileConfig.getOptional(TomlUtils.PROJECT);
            if (projectGroupEntries.isPresent()){
                hasProjectGroup = true;
                Config projectConfig = projectGroupEntries.get();
                // iterate over the entries and add each key to the set
                for (Config.Entry entry : projectConfig.entrySet()) {
                    existingProjectKeys.add(entry.getKey());
                }
            }
        }
        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        boolean injectAfterNextEmptyLine = false;
        boolean inToolPoetrySection = false;
        boolean inProjectSection = false;
        boolean dependenciesInjected = false;
        StringBuilder fileContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();

            while (line != null) {
                boolean addLine = true;
                boolean isEmptyLine = line.isBlank();
                String trimmedLine = line.strip();

                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    if (trimmedLine.equals("[" + TomlUtils.TOOL_POETRY + "]")) {
                        inToolPoetrySection = true;
                        inProjectSection = false;
                    } else if (trimmedLine.equals(("[" + TomlUtils.PROJECT + "]"))) {
                        inToolPoetrySection = false;
                        inProjectSection = true;
                    } else {
                        inToolPoetrySection = false;
                        inProjectSection = false;
                    }
                }
                if (trimmedLine.contains(TomlUtils.EQUALS)) {
                    if (inToolPoetrySection) {
                        String key = line.substring(0, line.indexOf(TomlUtils.EQUALS)).strip();
                        TomlReplacementTuple matchedTuple = replacements.get(key);
                        if (matchedTuple != null) {
                            // skip this line, we will add it back to [project] later
                            addLine = false;
                        }
                    }
                    if (!hasProjectGroup || (inProjectSection && !replacements.isEmpty())) {
                        injectAfterNextEmptyLine = true;
                    }
                }
                if (isEmptyLine && injectAfterNextEmptyLine){
                    if (hasProjectGroup){
                        fileContent = injectDependencies(fileContent, existingProjectKeys, replacements);
                        dependenciesInjected = true;
                        injectAfterNextEmptyLine = false;
                    } else if (!dependenciesInjected){
                        fileContent.append("\n[").append(TomlUtils.PROJECT).append("]\n");
                        fileContent = injectDependencies(fileContent, existingProjectKeys, replacements);
                        injectAfterNextEmptyLine = false;
                        hasProjectGroup = true;
                        dependenciesInjected = true;
                    }
                }
                if (addLine) {
                    fileContent.append(line).append("\n");
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
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
