package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PoetryRemoveEmptyTomlMigrationSteps extends AbstractPoetryMigrationSteps {
    private boolean shouldExecute;
    private boolean executionSucceeded;
    private List<String> allTomlSectionHeaders = new ArrayList<>();

    @Given("an existing pyproject.toml file with no direct entries under the tool.poetry header")
    public void an_existing_pyproject_toml_file_with_no_entries_under_the_tool_poetry_header() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-entries-under-tool-poetry.toml");
        Config toolPoetryEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);
        assertNumOfEntries(toolPoetryEntries, 2);
    }

    @Given("an existing pyproject.toml file with no direct entries under the tool.poetry.dependencies header")
    public void an_existing_pyproject_toml_file_with_no_entries_under_the_tool_poetry_dependencies_header() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-entries-under-tool-poetry-dependencies.toml");
        Config toolPoetryDepsEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY_DEPENDENCIES);
        assertNumOfEntries(toolPoetryDepsEntries, 0);
    }

    @Given("an existing pyproject.toml file with no direct entries under the tool.poetry and tool.poetry.dependencies headers")
    public void an_existing_pyproject_toml_file_with_no_entries_under_the_tool_poetry_and_tool_poetry_dependencies_header() {
        pyProjectToml = new File(testTomlFileDirectory, "with-no-entries-under-tool-poetry-and-tool-poetry-dependencies.toml");
        Config toolPoetryEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);
        Config toolPoetryDepsEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY_DEPENDENCIES);
        assertNumOfEntries(toolPoetryEntries, 2);
        assertNumOfEntries(toolPoetryDepsEntries, 0);
    }

    @Given("an existing pyproject.toml file with an inline dependencies table under the tool.poetry header")
    public void an_existing_pyproject_toml_file_with_inline_deps_table_under_tool_poetry_header(){
        pyProjectToml = new File(testTomlFileDirectory, "with-inline-deps-table-under-tool-poetry.toml");
        Config toolPoetryEntries = loadAndAssertGroupExists(TomlUtils.TOOL_POETRY);
        assertNumOfEntries(toolPoetryEntries, 2);
    }

    @When("the Habushu poetry-remove-empty-toml migration executes")
    public void the_habushu_poetry_remove_empty_toml_migration_executes() {
        PoetryRemoveEmptyTomlMigration migration = new PoetryRemoveEmptyTomlMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(PoetryV2MigrationContext.getIsPoetryAtLeast2());

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("the tool.poetry header is removed")
    public void the_tool_poetry_header_is_removed(){
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        allTomlSectionHeaders = TomlUtils.extractTomlSectionHeaders(pyProjectToml);
        assertTrue(isSectionHeaderMissing(allTomlSectionHeaders, TomlUtils.TOOL_POETRY));
    }

    @Then("the tool.poetry.dependencies header is removed")
    public void the_tool_poetry_deps_header_is_removed(){
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        allTomlSectionHeaders = TomlUtils.extractTomlSectionHeaders(pyProjectToml);
        assertTrue(isSectionHeaderMissing(allTomlSectionHeaders, TomlUtils.TOOL_POETRY_DEPENDENCIES));
    }

    @Then("the tool.poetry and tool.poetry.dependencies headers are removed")
    public void the_tool_poetry_header_and_tool_poetry_deps__header_are_removed(){
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        allTomlSectionHeaders = TomlUtils.extractTomlSectionHeaders(pyProjectToml);
        assertTrue(isSectionHeaderMissing(allTomlSectionHeaders, TomlUtils.TOOL_POETRY));
        assertTrue(isSectionHeaderMissing(allTomlSectionHeaders, TomlUtils.TOOL_POETRY_DEPENDENCIES));
    }

    @Then("the tool.poetry header is not removed")
    public void the_tool_poetry_header_is_not_removed(){
        assertFalse(shouldExecute, "Migration execution should have been skipped!");        allTomlSectionHeaders = TomlUtils.extractTomlSectionHeaders(pyProjectToml);
        assertFalse(isSectionHeaderMissing(allTomlSectionHeaders, TomlUtils.TOOL_POETRY));
    }

    @Then("the poetry-remove-empty-toml migration did not execute")
    public void the_poetry_remove_empty_toml_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }

}

