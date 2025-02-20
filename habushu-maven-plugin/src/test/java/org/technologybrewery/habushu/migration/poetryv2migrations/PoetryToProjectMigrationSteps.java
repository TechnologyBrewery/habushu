package org.technologybrewery.habushu.migration.poetryv2migrations;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.technologybrewery.habushu.PoetryCommandHelperTestWrapper;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PoetryToProjectMigrationSteps extends AbstractPoetryMigrationSteps {
    protected PoetryCommandHelperTestWrapper poetryHelper;
    protected boolean mockIsPoetryVersionAtLeast2;

    @Given("the Poetry version is at least \"2.0.0\"")
    public void the_poetry_version_is_at_least_2_0_0(){
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), "2.0.0");
        mockIsPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeastMinimumVersion();
        assertTrue(mockIsPoetryVersionAtLeast2, "The Poetry version found was less than 2.0.0");
    }

    @Given("the Poetry version is less than \"2.0.0\"")
    public void poetry_version_is_less_than_2_0_0(){
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), "1.6.1");
        mockIsPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeastMinimumVersion();
        assertFalse(mockIsPoetryVersionAtLeast2, "The Poetry version found greater than 2.0.0");
    }

    @Given("an existing pyproject.toml file with entries in the tool.poetry group")
    public void an_existing_pyproject_toml_file_with_entries_in_the_tool_poetry_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-fields-in-tool-poetry.toml");
        loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);
    }

    @When("the Habushu poetry-to-project migration executes")
    public void the_habushu_poetry_to_project_migration_executes() {
        PoetryToProjectMigration migration = new PoetryToProjectMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(mockIsPoetryVersionAtLeast2);

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("{int} entries exist in the tool.poetry group")
    public void entries_exist_in_the_tool_poetry_group(Integer expectedToolPoetryEntries) {
        verifyExecutionOccurred();
        loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);

        int actualEntries = getNumberOfEntries(toolPoetryEntries);
        assertEquals(expectedToolPoetryEntries, actualEntries,
                expectedToolPoetryEntries + " entries should remain in the tool.poetry group!");
    }

    @Then("{int} entries exist in the project group")
    public void entries_exist_in_the_project_group(Integer expectedProjectEntries) {
        loadAndAssertGroupExists(TomlUtils.PROJECT);

        int actualEntries = getNumberOfEntries(projectEntries);
        assertEquals(expectedProjectEntries, actualEntries,
                expectedProjectEntries + " entries should remain in the project group!");
    }

    @Given("an existing pyproject.toml file with overlapping entries between the tool.poetry and project groups")
    public void an_existing_pyproject_toml_file_with_overlapping_entries_between_groups() {
        pyProjectToml = new File(testTomlFileDirectory, "with-overlapping-fields-in-tool-poetry-and-project.toml");
        loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);
    }


    @Given("an existing pyproject.toml file with no project group")
    public void an_existing_pyproject_toml_file_with_no_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-to-project-migration.toml");
        loadAndAssertDoesNotExist(TomlUtils.PROJECT);
    }

    @Then("the poetry to project migration did not execute")
    public void the_poetry_to_project_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }

}

