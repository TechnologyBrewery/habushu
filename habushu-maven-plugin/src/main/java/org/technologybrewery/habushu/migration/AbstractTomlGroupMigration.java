package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.AbstractMigration;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Common logic to migrate TOML file entries from one group to another.
 */
public abstract class AbstractTomlGroupMigration extends AbstractHabushuMigration {

    public static final Logger logger = LoggerFactory.getLogger(AbstractTomlGroupMigration.class);

    protected Map<String, TomlReplacementTuple> replacements = new HashMap<>();

    private boolean hasExistingNewGroup = false;

    protected abstract String getLegacyGroupName();

    protected abstract String getNewGroupName();

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        replacements.clear();
        boolean shouldExecute = false;
        if (isPoetryProject(file)) {
            try (FileConfig tomlFileConfig = FileConfig.of(file)) {
                tomlFileConfig.load();

                String legacyGroup = getLegacyGroupName();
                Optional<Config> legacyGroupEntries = tomlFileConfig.getOptional(legacyGroup);
                if (legacyGroupEntries.isPresent()) {
                    Config foundGroupEntries = legacyGroupEntries.get();
                    Map<String, Object> groupEntryMap = foundGroupEntries.valueMap();

                    for (Map.Entry<String, Object> groupEntry : groupEntryMap.entrySet()) {
                        String groupEntryName = groupEntry.getKey();
                        Object groupEntryRhs = groupEntry.getValue();
                        String groupEntryRshAsString = null;
                        if (groupEntryRhs instanceof CommentedConfig) {
                            groupEntryRshAsString = TomlUtils.convertCommentedConfigToToml((CommentedConfig) groupEntryRhs);
                        } else {
                            groupEntryRshAsString = (String) groupEntryRhs;
                        }
                        logger.info("Found [{}] group entry to migrate! ({} = {})", legacyGroup, groupEntryName, groupEntryRshAsString);
                        TomlReplacementTuple replacementTuple = new TomlReplacementTuple(groupEntryName, groupEntryRshAsString, "");
                        replacements.put(groupEntryName, replacementTuple);
                    }

                    String newGroupName = getNewGroupName();
                    Optional<Config> newGroupEntries = tomlFileConfig.getOptional(newGroupName);
                    hasExistingNewGroup = newGroupEntries.isPresent();
                    shouldExecute = true;
                }
            }
        }

        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        String fileContent = StringUtils.EMPTY;
        String newGroupName = getNewGroupName();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();
            boolean injectAfterNextEmptyLine = false;

            while (line != null) {
                boolean addLine = true;
                boolean isEmptyLine = line.isBlank();

                if (line.contains(StringUtils.SPACE) && line.contains(TomlUtils.EQUALS)) {
                    String key = line.substring(0, line.indexOf(StringUtils.SPACE));

                    if (key == null) {
                        key = line.substring(0, line.indexOf(TomlUtils.EQUALS));
                    }

                    if (key != null) {
                        key = key.strip();

                        TomlReplacementTuple matchedTuple = replacements.get(key);
                        if (matchedTuple != null) {
                            // skip this line, we will add it back to [tool.poetry.dependencies] later
                            addLine = false;
                        }
                    }

                } else if (line.contains("[") && line.contains("]")) {
                    String key = line.strip();

                    if (hasExistingNewGroup && (key.equals("[" + newGroupName + "]"))) {
                        // skip this line as we are overriding with the line plus dependencies here:
                        addLine = false;
                        fileContent += line + "\n";
                        fileContent = injectDependencies(fileContent);
                    } else if (!hasExistingNewGroup) {
                        injectAfterNextEmptyLine = true;
                    }

                    String legacyGroupName = getLegacyGroupName();
                    if ((key.equals("[" + legacyGroupName + "]"))){
                        addLine = false;
                    }
                }

                if (isEmptyLine && injectAfterNextEmptyLine) {
                    fileContent += "\n[" + newGroupName + "]" + "\n";
                    fileContent = injectDependencies(fileContent);
                    injectAfterNextEmptyLine = false;
                }

                if (addLine) {
                    fileContent += line + "\n";
                }

                line = reader.readLine();
            }

        } catch (IOException e) {
            throw new HabushuException("Problem reading pyproject.toml to migrate groups!", e);
        }

        try {
            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent);
        } catch (IOException e) {
            throw new BatonException("Problem moving monorepo dependencies to [" + newGroupName + "]!", e);
        }

        return true;

    }

    private String injectDependencies(String fileContent) {
        for (Map.Entry<String, TomlReplacementTuple> entry : replacements.entrySet()) {
            fileContent += entry.getKey() + " = " + TomlUtils.escapeTomlRightHandSide(entry.getValue().getOriginalOperatorAndVersion()) + "\n";
        }
        return fileContent;
    }

}
