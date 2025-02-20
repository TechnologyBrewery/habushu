package org.technologybrewery.habushu.migration.poetryv2migrations;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.technologybrewery.habushu.PoetryCommandHelperTestWrapper;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class PoetryToProjectDynamicMigrationSteps extends AbstractPoetryMigrationSteps {
    private PoetryCommandHelperTestWrapper poetryHelper;
    private boolean mockIsPoetryVersionAtLeast2;

    @Given("Poetry version at least \"2.0.0\"")
    public void poetry_version_at_least_2_0_0(){
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), "2.0.0");
        mockIsPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeast2();
        assertTrue(mockIsPoetryVersionAtLeast2, "The Poetry version found was less than 2.0.0");
    }

    @Given("Poetry version less than \"2.0.0\"")
    public void poetry_version_less_than_2_0_0(){
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), "1.6.1");
        mockIsPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeast2();
        assertFalse(mockIsPoetryVersionAtLeast2, "The Poetry version found greater than 2.0.0");
    }

    @Given("an existing pyproject.toml file with no dynamic in project and no readme in tool.poetry")
    public void an_existing_pyproject_toml_file_with_no_dynamic_in_project_and_no_readme_in_tool_poetry() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-dynamic-in-project-and-no-readme-in-tool-poetry.toml");

        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, false);
        assertKeyExists(TomlUtils.TOOL_POETRY, TomlUtils.README, false);
    }

    @When("the Habushu poetry-to-project-dynamic migration executes")
    public void the_habushu_poetry_to_project_dynamic_migration_executes() {
        PoetryToProjectDynamicMigration migration = new PoetryToProjectDynamicMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(mockIsPoetryVersionAtLeast2);

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("the dynamic entry is added to the project group and contains")
    public void the_dynamic_entry_with_readme_is_added_in_the_project_group_and_contains(DataTable dataTable) {
        verifyExecutionOccurred();

        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, true);

        List<String> expectedEntries = dataTable.asList(String.class);
        Object actualEntries = projectEntries.get(TomlUtils.DYNAMIC);
        assertEquals(expectedEntries.toString(), actualEntries.toString(), "Mismatch in expected readme values");
    }

    @Given("an existing pyproject.toml file with no dynamic in project and with a single readme in tool.poetry")
    public void an_existing_pyproject_toml_file_with_no_dynamic_in_project_and_with_a_single_readme_in_tool_poetry() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-dynamic-in-project-and-with-single-readme-in-tool-poetry.toml");

        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, false);
        assertKeyExists(TomlUtils.TOOL_POETRY, TomlUtils.README, true);

        Object readMeEntry = toolPoetryEntries.get(TomlUtils.README);
        assertInstanceOf(String.class, readMeEntry, "[tool.poetry] readme does not contain single string value as expected");
    }

    @Then("the readme entry is added to the project group as {string}")
    public void the_readme_entry_is_added_in_the_project_group(String expectedReadMeEntry) {
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.README, true);
        assertEquals(expectedReadMeEntry, projectEntries.get(TomlUtils.README).toString(), "readme was set incorrectly in [project]");
    }

    @Then("the readme entry is removed from the tool.poetry group")
    public void the_readme_entry_is_removed_from_the_tool_poetry_group() {
        assertKeyExists(TomlUtils.TOOL_POETRY, TomlUtils.README, false);
    }

    @Given("an existing pyproject.toml file with no dynamic in project and with multiple readmes in tool.poetry")
    public void an_existing_pyproject_toml_file_with_no_dynamic_in_project_and_multiple_readmes_in_tool_poetry() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-dynamic-in-project-and-with-multiple-readmes-in-tool-poetry.toml");

        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, false);
        assertKeyExists(TomlUtils.TOOL_POETRY, TomlUtils.README, true);

        Object readMeEntries = toolPoetryEntries.get(TomlUtils.README);
        assertInstanceOf(ArrayList.class, readMeEntries, "[tool.poetry] readme does not contain multiple values as expected");
    }

    @Then("the readme entry remains in the tool.poetry group and contains")
    public void the_readme_entry_remains_in_the_tool_poetry_group_and_contains(DataTable dataTable) {
        assertKeyExists(TomlUtils.TOOL_POETRY, TomlUtils.README, true);

        List<String> expectedEntries = dataTable.asList(String.class);
        Object actualEntries = toolPoetryEntries.get(TomlUtils.README);
        assertEquals(expectedEntries.toString(), actualEntries.toString(), "Mismatch in expected readme values");
    }

    @Given("an existing pyproject.toml file with a dynamic entry but missing required field in project")
    public void an_existing_pyproject_toml_file_with_a_dynamic_entry_but_missing_required_field_in_project(DataTable dataTable) {
        pyProjectToml = new File(testTomlFileDirectory, "with-dynamic-in-project-but-missing-required-field.toml");
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, true);

        List<String> expectedEntries = dataTable.asList(String.class);
        Object actualEntries = projectEntries.get(TomlUtils.DYNAMIC);
        assertEquals(expectedEntries.toString(), actualEntries.toString(), "Mismatch in expected dynamic values");
    }

    @Then("the missing field is appended to the dynamic entry and now contains")
    public void the_missing_field_is_appended_to_the_dynamic_entry_and_contains(DataTable dataTable) {
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, true);

        List<String> expectedEntries = dataTable.asList(String.class);
        Object actualEntries = projectEntries.get(TomlUtils.DYNAMIC);
        assertEquals(expectedEntries.toString(), actualEntries.toString());
    }

    @Given("an existing pyproject.toml file with no dynamic in project")
    public void an_existing_pyproject_toml_file_with_no_dynamic_in_project() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-to-project-dynamic-migration.toml");
        assertKeyExists(TomlUtils.PROJECT, TomlUtils.DYNAMIC, false);
    }
    @Then("the poetry to project dynamic migration did not execute")
    public void the_poetry_to_project_requires_python_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }

}
