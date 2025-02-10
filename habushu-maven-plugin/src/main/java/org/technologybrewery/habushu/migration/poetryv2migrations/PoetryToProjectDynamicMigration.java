package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.util.TomlUtils;
import org.technologybrewery.baton.BatonException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Provides functionality to add/update the dynamic field in the [project] section of a TOML file and migrate readme fields, if present.
 * Adds any entries listed in dynamicEntriesExpected to [project.dynamic], unless already present.
 * If a single readme value (as a string) is specified under [tool.poetry], it is migrated from [tool.poetry] to [project.readme].
 * If multiple readmes are specified, the readme entry is added into [project.dynamic].
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryToProjectDynamicMigration extends AbstractPoetryMigration{
    private static final Logger logger = LoggerFactory.getLogger(PoetryToProjectDynamicMigration.class);
    private final List<String> dynamicEntriesExpected = new ArrayList<>(Arrays.asList(TomlUtils.VERSION, TomlUtils.DEPENDENCIES));
    private final List<String> dynamicEntriesToAdd = new ArrayList<>();
    private final List<String> dynamicEntriesActual = new ArrayList<>();
    private boolean addDynamicEntryToProject;
    private boolean appendEntriesToDynamic;
    private boolean addReadMeEntryToProject;
    private String readMeAsString;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        boolean shouldExecute = false;

        if (!isPoetryVersionAtLeast2) {
            return false;
        }

        try (FileConfig tomlFileConfig = FileConfig.of(file)) {
            tomlFileConfig.load();

            // check if [tool.poetry] has a readme field
            Optional<Config> toolPoetryOpt = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY);
            if(toolPoetryOpt.isPresent()){
                Config toolPoetryGroup = toolPoetryOpt.get();
                if (toolPoetryGroup.contains(TomlUtils.README)){
                    Object readMeEntry = toolPoetryGroup.get(TomlUtils.README);
                    // if [tool.poetry.readme] contains a table, then append "readme" to the expected list of dynamic entries
                    if (readMeEntry instanceof List<?>){
                        dynamicEntriesExpected.add(TomlUtils.README);
                    } else {
                        // if [tool.poetry.readme] contains a string, then migrate "readme" entry to [project]
                        addReadMeEntryToProject = true;
                        readMeAsString = (String) readMeEntry;
                    }
                }
            }

            Optional<Config> projectGroupOpt = tomlFileConfig.getOptional(TomlUtils.PROJECT);
            if (projectGroupOpt.isPresent()) {
                Config projectGroupEntry = projectGroupOpt.get();
                if (!projectGroupEntry.contains(TomlUtils.DYNAMIC)) {
                    shouldExecute = true;
                    addDynamicEntryToProject = true;
                    logger.info("Adding to [{}] group entry! ({})", TomlUtils.PROJECT, TomlUtils.DYNAMIC);
                } else {
                    // if [project] has dynamic field, but it's missing one of the expected fields from the list
                    Object dynamicEntry = projectGroupEntry.get(TomlUtils.DYNAMIC);
                    // convert existing dynamic field to a List<String> so we can compare against expected values
                    if (dynamicEntry instanceof List<?>) {
                        for (Object entry : (List<?>) dynamicEntry) {
                            if (entry instanceof String) {
                                dynamicEntriesActual.add((String) entry);
                            }
                        }
                    }
                    // loop through expected entries and add missing ones
                    for (String expectedEntry : dynamicEntriesExpected) {
                        if (!dynamicEntriesActual.contains(expectedEntry)) {
                            dynamicEntriesToAdd.add(expectedEntry);
                        }
                    }

                    if (!dynamicEntriesToAdd.isEmpty()) {
                        shouldExecute = true;
                        appendEntriesToDynamic = true;
                        logger.info("Adding additional fields to [{}] group entry! ({})", TomlUtils.DYNAMIC, dynamicEntriesToAdd);
                    }
                }
            }
        }
        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        boolean inProjectSection = false;
        boolean inPoetrySection = false;
        boolean injectedDynamicString = false;
        boolean injectAfterNextEmptyLine = false;
        boolean injectedDynamicList = false;
        boolean injectedReadMeString = false;
        String readMeString = TomlUtils.README + " " + TomlUtils.EQUALS + " " + TomlUtils.DOUBLE_QUOTE + readMeAsString + TomlUtils.DOUBLE_QUOTE;
        StringBuilder fileContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();

            while (line != null) {
                boolean isEmptyLine = line.isBlank();
                boolean addLine = true;
                String trimmedLine = line.strip();

                // set flags to keep track of which header we're currently under
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    if (trimmedLine.equals("[" + TomlUtils.TOOL_POETRY + "]")){
                        inPoetrySection = true;
                        inProjectSection = false;
                    } else if (trimmedLine.equals(("[" + TomlUtils.PROJECT + "]"))){
                        inPoetrySection = false;
                        inProjectSection = true;
                    }
                }

                if (trimmedLine.contains(TomlUtils.EQUALS)){
                    if(inProjectSection){
                        if(addDynamicEntryToProject && !injectedDynamicString){
                            injectAfterNextEmptyLine = true;
                        } else if (addReadMeEntryToProject && !injectedReadMeString){
                            injectAfterNextEmptyLine = true;
                        } else if (appendEntriesToDynamic && !injectedDynamicList && trimmedLine.startsWith(TomlUtils.DYNAMIC)){
                                addLine = false;
                                fileContent.append(buildDynamicListString()).append("\n");
                                injectedDynamicList = true;
                            }
                    } else if (inPoetrySection && addReadMeEntryToProject && trimmedLine.startsWith(TomlUtils.README)){
                        // skip [tool.poetry.readme] line since it will be migrated to [project]
                            addLine = false;
                        }
                }

                if (isEmptyLine && injectAfterNextEmptyLine){
                    if(addDynamicEntryToProject){
                        fileContent.append(buildDynamicListString()).append("\n");
                        injectAfterNextEmptyLine = false;
                        injectedDynamicString = true;
                    }
                    if(addReadMeEntryToProject){
                        fileContent.append(readMeString).append("\n");
                        injectAfterNextEmptyLine = false;
                        injectedReadMeString = true;
                    }
                }
                if(addLine){
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

    protected String buildDynamicListString() {
        List<String> listToUse;
        if (addDynamicEntryToProject) {
            listToUse = dynamicEntriesExpected;
        } else {
            listToUse = new ArrayList<>(dynamicEntriesActual);
            listToUse.addAll(dynamicEntriesToAdd);
        }
        return TomlUtils.DYNAMIC + " " + TomlUtils.EQUALS + " [" + joinQuoted(listToUse) + "]";
    }

}
