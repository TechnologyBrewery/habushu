package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class PoetryToProjectMigrationSteps extends AbstractPoetryMigrationSteps {
    private boolean shouldExecute;
    private boolean executionSucceeded;

    @Given("an existing pyproject.toml file with entries in the tool.poetry group")
    public void an_existing_pyproject_toml_file_with_entries_in_the_tool_poetry_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-fields-in-tool-poetry.toml");
        assertGroupExists(TomlUtils.TOOL_POETRY);
    }

    @When("the Habushu poetry-to-project migration executes")
    public void the_habushu_poetry_to_project_migration_executes() {
        PoetryToProjectMigration migration = new PoetryToProjectMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(PoetryV2MigrationContext.getIsPoetryAtLeast2());

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("{int} entries exist in the tool.poetry group")
    public void entries_exist_in_the_tool_poetry_group(Integer expectedToolPoetryEntries) {
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        Config toolPoetryEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);
        assertNumOfEntries(toolPoetryEntries, expectedToolPoetryEntries);
    }

    @Then("{int} entries exist in the project group")
    public void entries_exist_in_the_project_group(Integer expectedProjectEntries) {
        Config projectEntries = loadAndAssertGroupExists(TomlUtils.PROJECT);
        assertNumOfEntries(projectEntries, expectedProjectEntries);
    }

    @Given("an existing pyproject.toml file with overlapping entries between the tool.poetry and project groups")
    public void an_existing_pyproject_toml_file_with_overlapping_entries_between_groups() {
        pyProjectToml = new File(testTomlFileDirectory, "with-overlapping-fields-in-tool-poetry-and-project.toml");
        assertGroupExists(TomlUtils.TOOL_POETRY);
        assertGroupExists(TomlUtils.PROJECT);
    }

    @Given("an existing pyproject.toml file with no project group")
    public void an_existing_pyproject_toml_file_with_no_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-to-project-migration.toml");
        assertGroupDoesNotExist(TomlUtils.PROJECT);
    }

    @Then("the poetry to project migration did not execute")
    public void the_poetry_to_project_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }

}

