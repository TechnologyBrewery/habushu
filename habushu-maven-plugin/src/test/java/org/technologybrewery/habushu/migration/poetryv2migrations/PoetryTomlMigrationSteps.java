package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class PoetryTomlMigrationSteps extends AbstractPoetryMigrationSteps {
    private boolean shouldExecute;
    private boolean executionSucceeded;

    @Given("an existing poetry.toml file with deprecated configurations in [virtualenvs] and [experimental]")
    public void an_existing_poetry_toml_file_with_deprecated_configurations_in_virtualenvs_and_experimental() {
        pyProjectToml = new File(testTomlFileDirectory, "with-deprecated-virtualenvs-and-experimental-configs.toml");
        assertGroupExists(TomlUtils.VIRTUAL_ENVS);
        assertGroupExists(TomlUtils.EXPERIMENTAL);
    }

    @Given("an existing poetry.toml file with deprecated configurations in [virtualenvs]")
    public void an_existing_poetry_toml_file_with_deprecated_configs_in_virtualenvs() {
        pyProjectToml = new File(testTomlFileDirectory, "with-deprecated-virtualenvs-config.toml");
        assertGroupExists(TomlUtils.VIRTUAL_ENVS);
    }

    @Given("an existing poetry.toml file with one deprecated configuration in [experimental] out of multiple configurations")
    public void an_existing_poetry_toml_file_with_one_deprecated_config_in_experimental_out_of_multiple_configs() {
        pyProjectToml = new File(testTomlFileDirectory, "with-one-deprecated-experimental-out-of-multiple.toml");
        assertGroupExists(TomlUtils.EXPERIMENTAL);
    }

    @Given("an existing poetry.toml file with one deprecated configuration in [experimental]")
    public void an_existing_poetry_toml_file_with_one_deprecated_configs_in_experimental() {
        pyProjectToml = new File(testTomlFileDirectory, "with-one-deprecated-experimental-system-git-config.toml");
        assertGroupExists(TomlUtils.EXPERIMENTAL);
    }

    @When("the Habushu poetry-toml migration executes")
    public void the_habushu_poetry_toml_migration_executes() {
        PoetryTomlMigration migration = new PoetryTomlMigration();
        migration.setWorkingDirectory(new File("."));
        migration.setIsPoetryVersionAtLeast2(PoetryV2MigrationContext.getIsPoetryAtLeast2());

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("{int} entries exist in the virtualenvs group")
    public void entries_exist_in_the_virtualenvs_group(Integer expectedVirtualEnvsEntries) {
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        Config virtualEnvsEntries = loadAndAssertGroupExists(TomlUtils.VIRTUAL_ENVS);
        assertNumOfEntries(virtualEnvsEntries, expectedVirtualEnvsEntries);
    }

    @Then("{int} entries exist in the experimental group")
    public void entries_exist_in_the_experimental_group(Integer expectedExperimentalEntries) {
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        Config experimentalEntries = loadAndAssertGroupExists(TomlUtils.EXPERIMENTAL);
        assertNumOfEntries(experimentalEntries, expectedExperimentalEntries);
    }

    @Then("no entry exists in the experimental group")
    public void no_entry_exists_in_the_experimental_group() {
        verifyExecutionOccurred(shouldExecute, executionSucceeded);
        assertGroupDoesNotExist(TomlUtils.EXPERIMENTAL);
    }

    @Given("an existing poetry.toml file with no deprecated configurations in [virtualenvs]")
    public void an_existing_poetry_toml_file_with_no_deprecated_configs_in_virtualenvs() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-toml-migration.toml");
        assertGroupExists(TomlUtils.VIRTUAL_ENVS);
    }

    @Given("an existing poetry.toml file with no deprecated configurations in [experimental]")
    public void an_existing_poetry_toml_file_with_no_deprecated_configs_in_experimental() {
        pyProjectToml = new File(testTomlFileDirectory, "no-poetry-toml-migration.toml");
        assertGroupExists(TomlUtils.EXPERIMENTAL);
    }

    @Then("the poetry-toml migration did not execute")
    public void the_poetry_toml_migration_did_not_execute() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }

}

