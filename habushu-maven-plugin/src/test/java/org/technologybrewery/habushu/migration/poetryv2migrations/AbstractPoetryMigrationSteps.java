package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Pure helper methods for loading TOML groups and making assertions.
 * All methods take explicit File + section/key arguments—no static fields.
 */
public class AbstractPoetryMigrationSteps{
    protected static File testTomlFileDirectory = new File("./target/test-classes/migration/poetry-v2");
    protected static File pyProjectToml;
    protected static File pythonFile;

    protected void verifyExecutionOccurred(boolean shouldExecute, boolean executionSucceeded) {
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
    protected void assertKeyExists(Config groupEntries, String key, boolean shouldExist) {
        if (shouldExist) {
            assertTrue(groupEntries.contains(key), key + " should exist in group");
        } else {
            assertFalse(groupEntries.contains(key), key + " should NOT exist in group");
        }
    }

    /**
     * Helper methods to load a toml group and/or assert its presence
     */
    protected Config loadAndAssertGroupExists(String groupName){
        Optional<Config> groupOpt = getOptionalConfig(pyProjectToml, groupName);
        assertTrue(groupOpt.isPresent(), groupName + " missing from toml file");
        return groupOpt.get();
    }

    protected void assertGroupExists(String groupName){
        Optional<Config> groupOpt = getOptionalConfig(pyProjectToml, groupName);
        assertTrue(groupOpt.isPresent(), groupName + " missing from toml file");
    }

    protected void assertGroupDoesNotExist(String groupName){
        Optional<Config> groupOpt = getOptionalConfig(pyProjectToml, groupName);
        assertFalse(groupOpt.isPresent(), groupName + " should not be in toml file");
    }

    protected void assertNumOfEntries(Config groupName, Integer expectedExperimentalEntries ){
        int actualNumEntries = getNumberOfEntries(groupName);
        assertEquals(expectedExperimentalEntries, actualNumEntries, expectedExperimentalEntries + " entries should remain in the group!");
    }

    /**
     * Helper method to check if header (as "[sectionName]" string) exists in a toml file
     */
    protected boolean isSectionHeaderMissing(List<String> allHeaders, String sectionName) {
        return allHeaders.stream()
                .noneMatch(header -> header.equals(sectionName));
    }
}
