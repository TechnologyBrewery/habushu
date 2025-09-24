package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.util.TomlLocationBlock;
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
import java.util.HashMap;

/**
 * Provides functionality to migrate configuration entries from the [tool.poetry] section to the [project] section of a TOML file.
 * Migrates any entry included in entriesToMigrate that does not already exist.
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryToProjectMigration extends AbstractPoetryMigration {
    private static final Logger logger = LoggerFactory.getLogger(PoetryToProjectMigration.class);
    public static final String PROJECT_SECTION_WITH_BRACKETS = "[" + TomlUtils.PROJECT + "]";
    public static final String NO_SECTION_HEADER = "no_section_header";
    private final List<String> entriesToMigrate = Arrays.asList("name", "description", "version", "license");
    private final Map<String, List<String>> tomlMap = new LinkedHashMap<>();

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        boolean shouldExecute = false;
        Config.setInsertionOrderPreserved(true);

        if (super.shouldExecuteOnFile(file)) {
            try (FileConfig tomlFileConfig = FileConfig.of(file)) {
                tomlFileConfig.load();
                Optional<Config> poetryGroupOpt = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY);

                if (poetryGroupOpt.isPresent()){
                    Config poetryGroup = poetryGroupOpt.get();
                    for (Config.Entry groupEntry : poetryGroup.entrySet()) {

                        // check if any of the entries under [tool.poetry] match one of "name", "description", or "version"
                        String groupEntryName = groupEntry.getKey();

                        if (entriesToMigrate.stream().anyMatch(e -> e.equalsIgnoreCase(groupEntryName))) {
                            shouldExecute = true;
                        }
                    }
                }
            }
        }
        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        Map<String, TomlLocationBlock> multiLineProperties = findMultilineProperties(pyProjectTomlFile);

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            loadTomlFileToMap(reader, multiLineProperties);

            StringBuilder fileContent = generateNewTomlFileContentsFromUpdatedStructure();

            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent.toString());
        } catch (IOException e) {
            throw new BatonException("Problem reading pyproject.toml for migration!", e);
        }

        return true;
    }

    public Map<String, TomlLocationBlock> findMultilineProperties(File pyProjectTomlFile) {
        /*
         * Parses the Toml file and identifies where items are represented on multiple lines.
         * If an item is multi-line, the start and end locations are determined and added
         * to a list.
         */
        Map<String, TomlLocationBlock> multiLineProperties = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            boolean inArray = false;
            String currentSection = null;
            String currentName = null;
            int startLine = -1;

            String line = reader.readLine();
            int lineCount = 1;
            while (line != null) {
                String trimmedLine = line.strip();

                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    currentSection = trimmedLine.replaceAll(" ", "");
                }

                if (!inArray) {
                    if (trimmedLine.matches("^[a-zA-Z0-9_.-]+\\s*=\\s*\\[$")) {
                        // matches lines like the following:
                        // packages = [
                        inArray = true;
                        currentName = getKeyFromLine(trimmedLine);
                        startLine = lineCount;
                    }
                } else {
                    if (trimmedLine.equals("]")) {
                        inArray = false;

                        // We only want to create a location object for items under [tool.poetry]
                        // and in the list of items we are migrating
                        if(currentSection.equals("[" + TomlUtils.TOOL_POETRY + "]") && entriesToMigrate.contains(currentName)) {
                            multiLineProperties.put(currentName, new TomlLocationBlock(currentName, startLine, lineCount));
                        }
                    }
                }

                lineCount++;

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new BatonException("Problem reading pyproject.toml for migration!", e);
        }

        return multiLineProperties;
    }

    /**
     * Processes the Toml file and performs the migration.
     * @param reader the toml file being processed
     * @param multiLineProperties a list of all the items that are multi-line
     * @throws IOException
     */
    private void loadTomlFileToMap(BufferedReader reader, Map<String, TomlLocationBlock> multiLineProperties) throws IOException {
        String currentSection = new String();
        String line = reader.readLine();
        String currentKeyName = null;
        int lineCount = 1;
        while (line != null) {
            String trimmedLine = line.strip();

            if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                // Process Section Header
                currentSection = trimmedLine.replaceAll(" ", "");
                if(!tomlMap.containsKey(currentSection)) {
                    tomlMap.put(currentSection, new ArrayList<>());
                } else if(!currentSection.equals(PROJECT_SECTION_WITH_BRACKETS)) {
                    // If there is a match on the tomlMap key then we have a
                    // duplicate section header.  We will just add the duplicate
                    // section header to the group.
                    tomlMap.get(trimmedLine).add(line);
                }
            } else {
                if(trimmedLine.contains(TomlUtils.EQUALS)) {
                    // Section items
                    currentKeyName = getKeyFromLine(trimmedLine);;

                    if(itemNeedsToBeMigrated(currentKeyName, currentSection)){
                        ensureProjectSectionExistsInTomlMap();
                        // check for duplicate
                        if(!checkIfItemAlreadyExistsInProjectSection(tomlMap.get(PROJECT_SECTION_WITH_BRACKETS), trimmedLine)) {
                            tomlMap.get(PROJECT_SECTION_WITH_BRACKETS).add(trimmedLine);

                            List<String> additionaLines = checkPropertyForMultilineAdditions(
                                    reader,
                                    multiLineProperties,
                                    currentKeyName,
                                    lineCount
                            );

                            tomlMap.get(PROJECT_SECTION_WITH_BRACKETS).addAll(additionaLines);
                            lineCount += additionaLines.size();
                        }
                    } else {
                        if(currentSection.equals(PROJECT_SECTION_WITH_BRACKETS)) {
                            // check for duplicate
                            if (!checkIfItemAlreadyExistsInProjectSection(tomlMap.get(PROJECT_SECTION_WITH_BRACKETS), trimmedLine)) {
                                tomlMap.get(PROJECT_SECTION_WITH_BRACKETS).add(trimmedLine);
                                List<String> additionaLines = checkPropertyForMultilineAdditions(
                                        reader,
                                        multiLineProperties,
                                        currentKeyName,
                                        lineCount
                                );
                                tomlMap.get(PROJECT_SECTION_WITH_BRACKETS).addAll(additionaLines);
                                lineCount += additionaLines.size();
                            }
                        } else {
                            // Non-migrate items
                            tomlMap.get(currentSection).add(trimmedLine);
                            List<String> additionaLines = checkPropertyForMultilineAdditions(
                                    reader,
                                    multiLineProperties,
                                    currentKeyName,
                                    lineCount
                            );
                            tomlMap.get(currentSection).addAll(additionaLines);
                            lineCount += additionaLines.size();
                        }
                    }
                } else {
                    // Comments and blank lines
                    if(currentSection.isEmpty()) {
                        // comments at the top of the file that don't reside under
                        // a section header
                        if(!tomlMap.containsKey(NO_SECTION_HEADER)) {
                            tomlMap.put(NO_SECTION_HEADER, new ArrayList<>());
                        }
                        tomlMap.get(NO_SECTION_HEADER).add(line);
                    } else {
                        tomlMap.get(currentSection).add(line);
                    }
                }
            }

            line = reader.readLine();
            lineCount++;
        }
    }

    /**
     * If a migratable item is encountered before the project section is created
     * then it will be added to the Map
     */
    private void ensureProjectSectionExistsInTomlMap() {
        if(!tomlMap.containsKey(PROJECT_SECTION_WITH_BRACKETS)) {
            tomlMap.put(PROJECT_SECTION_WITH_BRACKETS, new ArrayList<>());
        }
    }

    /**
     * If the current section is not already [project] and the item is in the list of migratable
     * items then we need to migrate.
     * @param currentKeyName the item being processed
     * @return If the item needs to be migrated
     */
    private boolean itemNeedsToBeMigrated(String currentKeyName, String currentSection) {
        return currentSection.equals("[" + TomlUtils.TOOL_POETRY + "]") && entriesToMigrate.contains(currentKeyName);
    }

    /**
     * Checks if the current item is multi-line (formatted).  If it is we need to
     * process the next few lines until the whole item is processed.
     * @param reader the file being processed
     * @param multiLineProperties a list of items that are multi-line
     * @param currentKeyName the current item being processed
     * @param lineCount the current position within the Toml file
     * @return the last line that was processed
     * @throws IOException
     */
    private List<String> checkPropertyForMultilineAdditions(BufferedReader reader, Map<String, TomlLocationBlock> multiLineProperties, String currentKeyName, int lineCount) throws IOException {
        List<String> appendChangeSet = new ArrayList<>();
        if(multiLineProperties.containsKey(currentKeyName)) {
            TomlLocationBlock multilineBlock = multiLineProperties.get(currentKeyName);
            if(multilineBlock != null) {
                while (lineCount < multilineBlock.getEndLine()) {
                    appendChangeSet.add(reader.readLine());
                    lineCount++;
                }
            }
        }
        return appendChangeSet;
    }

    /**
     * Extracts the key from the line
     * i.e. version = "2.9.0.dev"
     * @param trimmedLine the current line being processed
     * @return the extracted key
     */
    private static String getKeyFromLine(String trimmedLine) {
        return trimmedLine.substring(0, trimmedLine.indexOf(TomlUtils.EQUALS)).strip();
    }

    /**
     * Check if the item already exists before adding.
     * @param existingProjectItems a list of items that already exists in the [project] section
     * @param migratingItem the current item being processed
     * @return if the item already exists
     */
    private boolean checkIfItemAlreadyExistsInProjectSection(List<String> existingProjectItems, String migratingItem) {
        String movingItemKey = migratingItem.substring(0, migratingItem.indexOf(TomlUtils.EQUALS) + 1);

        boolean itemAlreadyExists = false;
        for(String existingItem: existingProjectItems) {
            if(existingItem.contains(movingItemKey)){
                itemAlreadyExists = true;
            }
        }

        return itemAlreadyExists;
    }

    /**
     * Takes the mapped toml file and outputs it line by line to a StringBuilder
     * @return the StringBuilder of the processed file
     * @throws IOException
     */
    private StringBuilder generateNewTomlFileContentsFromUpdatedStructure() throws IOException {
        StringBuilder fileContent = new StringBuilder();

        // Process items that were not part of a section first
        if(tomlMap.containsKey(NO_SECTION_HEADER)) {
            List<String> commentsWithoutASection = tomlMap.get(NO_SECTION_HEADER);
            for (String line : commentsWithoutASection) {
                fileContent.append(line).append("\n");
            }
            fileContent.append("").append("\n");
        }

        // Process the [project] section next so it's the first section of the toml file
        List<String> projectItems = tomlMap.get(PROJECT_SECTION_WITH_BRACKETS);
        fileContent.append(PROJECT_SECTION_WITH_BRACKETS).append("\n");

        for (String line : projectItems) {
            fileContent.append(line).append("\n");
        }
        fileContent.append("").append("\n");

        for (Map.Entry<String, List<String>> entry : tomlMap.entrySet()) {
            if(!entry.getKey().contains(TomlUtils.PROJECT) && !entry.getKey().contains(NO_SECTION_HEADER)) {
                fileContent.append(entry.getKey()).append("\n");

                for (String line : entry.getValue()) {
                    fileContent.append(line).append("\n");
                }
            }
        }

        return fileContent;
    }

}
