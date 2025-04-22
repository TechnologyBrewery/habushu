package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PoetryToProjectRequiresPythonMigrationSteps extends AbstractPoetryMigrationSteps {
    private boolean shouldExecute;
    private boolean executionSucceeded;

    @Given("an existing pyproject.toml file with no requires-python entry in the project group")
    public void an_existing_pyproject_toml_file_with_no_requires_python_entry_in_the_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-python-requirement-tag-in-project.toml");
        Config projectEntries = loadAndAssertGroupExists(TomlUtils.PROJECT);
        assertKeyExists(projectEntries, TomlUtils.REQUIRES_PYTHON, false);
    }

    @Given("an existing pyproject.toml file with a requires-python entry in the project group")
    public void an_existing_pyproject_toml_file_with_a_requires_python_entry_in_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-python-requirement-tag-in-project.toml");
        Config projectEntries = loadAndAssertGroupExists(TomlUtils.PROJECT);
        assertKeyExists(projectEntries, TomlUtils.REQUIRES_PYTHON, true);
    }

    @Given("an existing pyproject.toml file with a badly formatted requires-python entry in the project group")
    public void an_existing_pyproject_toml_file_with_a_badly_formatted_requires_python_entry_in_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-python-requirement-tag-in-project-badly-formatted.toml");
        Config projectEntries = loadAndAssertGroupExists(TomlUtils.PROJECT);
        assertKeyExists(projectEntries, TomlUtils.REQUIRES_PYTHON, true);
    }

    @Given("an existing pyproject.toml file with no requires-python")
    public void an_existing_pyproject_toml_file_without_a_requires_python_entry_in_project_group() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-to-project-python-requirement-tag-migration.toml");
        Config projectEntries = loadAndAssertGroupExists(TomlUtils.PROJECT);
        assertKeyExists(projectEntries, TomlUtils.REQUIRES_PYTHON, false);
    }

    @When("the Habushu poetry-to-project-requires-python migration executes")
    public void the_habushu_poetry_to_project_requires_python_migration_executes() {
        PoetryToProjectRequiresPythonMigration migration = new PoetryToProjectRequiresPythonMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(PoetryV2MigrationContext.getIsPoetryAtLeast2());

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("the requires-python entry is added to the project group and set to {string}")
    public void the_requires_python_entry_is_added_to_the_project_group_and_set_to(String expectedPythonVersion) {
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        Config projectEntries = loadAndAssertGroupExists(TomlUtils.PROJECT);

        assertKeyExists(projectEntries, TomlUtils.REQUIRES_PYTHON, true);
        assertEquals(expectedPythonVersion, projectEntries.get(TomlUtils.REQUIRES_PYTHON).toString(),"requires-python was set incorrectly in [project]");
    }

    @Then("the python entry no longer exists in the tool.poetry.dependencies group")
    public void the_python_entry_no_longer_exists_in_the_tool_poetry_group() {
        Config toolPoetryDepsEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY_DEPENDENCIES);
        assertKeyExists(toolPoetryDepsEntries, TomlUtils.PYTHON, false);
    }

    @Then("the poetry to project requires python migration did not execute")
    public void the_poetry_to_project_requires_python_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }
}
