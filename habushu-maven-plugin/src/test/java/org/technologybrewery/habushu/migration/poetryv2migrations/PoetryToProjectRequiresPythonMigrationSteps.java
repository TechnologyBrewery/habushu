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

public class PoetryToProjectRequiresPythonMigrationSteps extends AbstractPoetryMigrationSteps {
    protected PoetryCommandHelperTestWrapper poetryHelper;
    protected boolean mockIsPoetryVersionAtLeast2;


    @Given("Poetry version is at least \"2.0.0\"")
    public void poetry_version_is_at_least_2_0_0(){
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), "2.0.0");
        mockIsPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeastMinimumVersion();
        assertTrue(mockIsPoetryVersionAtLeast2, "The Poetry version found was less than 2.0.0");
    }

    @Given("Poetry version is less than \"2.0.0\"")
    public void poetry_version_is_less_than_2_0_0(){
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), "1.6.1");
        mockIsPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeastMinimumVersion();
        assertFalse(mockIsPoetryVersionAtLeast2, "The Poetry version found greater than 2.0.0");
    }

    @Given("an existing pyproject.toml file with no requires-python entry in the project group")
    public void an_existing_pyproject_toml_file_with_no_requires_python_entry_in_the_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-python-requirement-tag-in-project.toml");
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON, false);
    }

    @Given("an existing pyproject.toml file with a requires-python entry in the project group")
    public void an_existing_pyproject_toml_file_with_a_requires_python_entry_in_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-python-requirement-tag-in-project.toml");
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON, true);
    }

    @Given("an existing pyproject.toml file with a badly formatted requires-python entry in the project group")
    public void an_existing_pyproject_toml_file_with_a_badly_formatted_requires_python_entry_in_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-python-requirement-tag-in-project-badly-formatted.toml");
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON, true);
    }

    @Given("an existing pyproject.toml file with no requires-python")
    public void an_existing_pyproject_toml_file_without_a_requires_python_entry_in_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-to-project-python-requirement-tag-migration.toml");
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON, false);
    }

    @When("the Habushu poetry-to-project-requires-python migration executes")
    public void the_habushu_poetry_to_project_requires_python_migration_executes() {
        PoetryToProjectRequiresPythonMigration migration = new PoetryToProjectRequiresPythonMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(mockIsPoetryVersionAtLeast2);

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("the requires-python entry is added to the project group and set to {string}")
    public void the_requires_python_entry_is_added_to_the_project_group_and_set_to(String expectedPythonVersion) {
        verifyExecutionOccurred();

        assertKeyExists(TomlUtils.PROJECT, TomlUtils.REQUIRES_PYTHON, true);
        assertEquals(expectedPythonVersion, projectEntries.get(TomlUtils.REQUIRES_PYTHON).toString(),"requires-python was set incorrectly in [project]");
    }

    @Then("the python entry no longer exists in the tool.poetry.dependencies group")
    public void the_python_entry_no_longer_exists_in_the_tool_poetry_group() {
        assertKeyExists(TomlUtils.TOOL_POETRY_DEPENDENCIES, TomlUtils.PYTHON, false);
    }

    @Then("the poetry to project requires python migration did not execute")
    public void the_poetry_to_project_requires_python_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }
}
