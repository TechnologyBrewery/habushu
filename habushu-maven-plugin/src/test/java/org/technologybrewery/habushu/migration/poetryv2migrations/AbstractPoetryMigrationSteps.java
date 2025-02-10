package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AbstractPoetryMigrationSteps{
    protected static File testTomlFileDirectory = new File("./target/test-classes/migration/poetry-v2");
    protected static File pyProjectToml;
    protected Optional<Config> projectOpt;
    protected Optional<Config> toolPoetryOpt;
    protected Optional<Config> toolPoetryDependenciesOpt;
    protected Config projectEntries;
    protected Config toolPoetryEntries;
    protected Config toolPoetryDependencyEntries;
    protected boolean shouldExecute;
    protected boolean executionSucceeded;

    protected void verifyExecutionOccurred() {
        assertTrue(shouldExecute, "Migration should have been selected to execute!");
        assertTrue(executionSucceeded, "Migration should have executed successfully!");
    }

    protected int getNumberOfEntries(Config groupName) {
        return groupName.entrySet().size();
    }

    protected static Optional<Config> getOptionalConfig(File file, String key){
        try (FileConfig tomlFileConfig = FileConfig.of(file)) {
            tomlFileConfig.load();
            return tomlFileConfig.getOptional(key);
        }
    }

    /**
     * Helper method to check if entry exists within specified toml group
     */
    protected void assertKeyExists(String groupName, String key, boolean shouldExist) {
        loadAndAssertGroupExists(groupName);

        // Retrieve the appropriate entries based on groupName
        Config entries;
        if (groupName.equals(TomlUtils.PROJECT)) {
            entries = projectEntries;
        } else if (groupName.equals(TomlUtils.TOOL_POETRY)) {
            entries = toolPoetryEntries;
        } else if (groupName.equals(TomlUtils.TOOL_POETRY_DEPENDENCIES)) {
            entries = toolPoetryDependencyEntries;
        } else {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        if (shouldExist) {
            assertTrue(entries.contains(key), key + " should exist under " + groupName);
        } else {
            assertFalse(entries.contains(key), key + " should NOT exist under " + groupName);
        }
    }

    /**
     * Helper methods to load a toml group and assert its presence
     */
    protected void loadAndAssertGroupExists(String groupName) {
        Optional<Config> groupOpt = getOptionalConfig(pyProjectToml, groupName);
        assertTrue(groupOpt.isPresent(), groupName + " missing from toml file");
        Config entries = groupOpt.get();

        if (groupName.equals(TomlUtils.PROJECT)) {
            projectOpt = groupOpt;
            projectEntries = entries;
        } else if (groupName.equals(TomlUtils.TOOL_POETRY)) {
            toolPoetryOpt = groupOpt;
            toolPoetryEntries = entries;
        } else if (groupName.equals(TomlUtils.TOOL_POETRY_DEPENDENCIES)) {
            toolPoetryDependenciesOpt = groupOpt;
            toolPoetryDependencyEntries = entries;
        } else {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }
    }
    protected void loadAndAssertDoesNotExist(String groupName) {
        Optional<Config> groupOpt = getOptionalConfig(pyProjectToml, groupName);
        assertFalse(groupOpt.isPresent(), groupName + " missing from toml file");
    }

}
