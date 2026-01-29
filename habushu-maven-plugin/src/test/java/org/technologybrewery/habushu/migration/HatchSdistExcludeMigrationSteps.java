package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.file.FileConfig;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HatchSdistExcludeMigrationSteps {

    private static final String TEST_DIR = "./target/test-classes/migration/hatch-sdist";
    private static final String TOOL_HATCH_BUILD_TARGETS_SDIST_EXCLUDE = "tool.hatch.build.targets.sdist.exclude";

    private File pyProjectToml;
    private boolean shouldExecute;
    private boolean executionSucceeded;

    @Given("a UV project without any hatch sdist configuration")
    public void a_uv_project_without_any_hatch_sdist_configuration() {
        pyProjectToml = new File(TEST_DIR, "uv-without-hatch-sdist.toml");
    }

    @Given("a UV project with hatch sdist exclude but without target directory")
    public void a_uv_project_with_hatch_sdist_exclude_but_without_target_directory() {
        pyProjectToml = new File(TEST_DIR, "uv-with-partial-hatch-sdist.toml");
    }

    @Given("a UV project with multi-line hatch sdist exclude without target directory")
    public void a_uv_project_with_multi_line_hatch_sdist_exclude_without_target_directory() {
        pyProjectToml = new File(TEST_DIR, "uv-with-multiline-hatch-sdist.toml");
    }

    @Given("a UV project with hatch sdist exclude already containing target directory")
    public void a_uv_project_with_hatch_sdist_exclude_already_containing_target_directory() {
        pyProjectToml = new File(TEST_DIR, "uv-with-complete-hatch-sdist.toml");
    }

    @Given("a Poetry project")
    public void a_poetry_project() {
        pyProjectToml = new File(TEST_DIR, "poetry-project.toml");
    }

    @When("the hatch sdist exclude migration executes")
    public void the_hatch_sdist_exclude_migration_executes() {
        HatchSdistExcludeMigration migration = new HatchSdistExcludeMigration();
        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = shouldExecute && migration.performMigration(pyProjectToml);
    }

    @Then("the pyproject.toml should have target directory in hatch sdist exclude list")
    public void the_pyproject_toml_should_have_target_directory_in_hatch_sdist_exclude_list() {
        assertTrue(shouldExecute, "Migration should have been selected to execute");
        assertTrue(executionSucceeded, "Migration should have executed successfully");

        try (FileConfig toml = FileConfig.of(pyProjectToml)) {
            toml.load();
            List<String> excludeList = toml.get(TOOL_HATCH_BUILD_TARGETS_SDIST_EXCLUDE);
            assertNotNull(excludeList, "Exclude list should exist");
            assertTrue(excludeList.contains("target/"), "Exclude list should contain target/");
        }
    }

    @Then("the existing exclude entries should be preserved")
    public void the_existing_exclude_entries_should_be_preserved() {
        try (FileConfig toml = FileConfig.of(pyProjectToml)) {
            toml.load();
            List<String> excludeList = toml.get(TOOL_HATCH_BUILD_TARGETS_SDIST_EXCLUDE);
            assertNotNull(excludeList, "Exclude list should exist");
            assertTrue(excludeList.contains(".git/"), "Exclude list should still contain .git/");
            assertTrue(excludeList.contains("docs/"), "Exclude list should still contain docs/");
        }
    }

    @Then("the migration should not execute")
    public void the_migration_should_not_execute() {
        assertFalse(shouldExecute, "Migration should NOT have been selected to execute");
    }

    @Then("the exclude list should have {int} entries")
    public void the_exclude_list_should_have_entries(Integer expectedCount) {
        try (FileConfig toml = FileConfig.of(pyProjectToml)) {
            toml.load();
            List<String> excludeList = toml.get(TOOL_HATCH_BUILD_TARGETS_SDIST_EXCLUDE);
            assertNotNull(excludeList, "Exclude list should exist");
            assertEquals(expectedCount, excludeList.size(), "Exclude list should have " + expectedCount + " entries");
        }
    }
}
