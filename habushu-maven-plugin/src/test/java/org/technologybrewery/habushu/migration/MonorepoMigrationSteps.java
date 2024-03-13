package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MonorepoMigrationSteps {

    private File testTomlFileDirectory = new File("./target/test-classes/migration/monorepo");
    private File pyProjectToml;

    private boolean shouldExecute;

    private boolean executionSucceeded;

    @Given("an existing pyproject.toml file with two monorepo dependencies each in the tool.poetry.dependencies and tool.poetry.group.monorepo.dependencies groups")
    public void an_existing_pyproject_toml_file_with_two_monorepo_dependencies_each_in_the_tool_poetry_group_dependencies_groups() {
        pyProjectToml = new File(testTomlFileDirectory, "with-dependencies-in-both-groups.toml");
    }

    @Given("an existing pyproject.toml without any monorepo dependencies")
    public void an_existing_pyproject_toml_without_any_monorepo_dependencies() {
        pyProjectToml = new File(testTomlFileDirectory, "no-monorepo-dependencies-present.toml");
    }


    @Given("an existing pyproject.toml file with three monorepo dependencies already in the tool.poetry.group.monorepo.dependencies group")
    public void an_existing_pyproject_toml_file_with_three_monorepo_dependencies_already_in_the_tool_poetry_group_monorepo_dependencies_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-dependencies-already-in-monorepo-group.toml");
    }

    @When("Habushu migrations execute")
    public void habushu_migrations_execute() {
        RemoveMonorepoGroupMigration migration = new RemoveMonorepoGroupMigration();
        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("{int} pyproject.toml monorepo dependencies exist in the tool.poetry.group.monorepo.dependencies group")
    public void pyproject_toml_monorepo_dependencies_exist_in_the_tool_poetry_group_monorepo_dependencies_group(Integer expectedMonorepoGroupDependencies) {
        verifyExecutionOccurred();
        try (FileConfig tomlFileConfig = FileConfig.of(pyProjectToml)) {
            tomlFileConfig.load();

            Optional<Config> toolPoetryMonorepoDependencies = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_GROUP_MONOREPO_DEPENDENCIES);
            int numberOfMonorepoDependenciesInMonorepoGroup = getNumberOfMonorepoChildren(toolPoetryMonorepoDependencies);
            assertEquals(expectedMonorepoGroupDependencies, numberOfMonorepoDependenciesInMonorepoGroup,
                    expectedMonorepoGroupDependencies + " monorepo  monorepo group should not exist!");


        }
    }
    @Then("{int} pyproject.toml monorepo dependencies exist in the tool.poetry.dependencies group")
    public void pyproject_toml_monorepo_dependencies_exist_in_the_tool_poetry_dependencies_group(Integer expectedMainGroupDependencies) {
        verifyExecutionOccurred();
        try (FileConfig tomlFileConfig = FileConfig.of(pyProjectToml)) {
            tomlFileConfig.load();

            Optional<Config> toolPoetryDependencies = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_DEPENDENCIES);
            int numberOfMonorepoDependenciesInMainGroup = getNumberOfMonorepoChildren(toolPoetryDependencies);
            assertEquals(expectedMainGroupDependencies, numberOfMonorepoDependenciesInMainGroup,
                    expectedMainGroupDependencies + " monorepo dependencies should remain in main group!");
        }
    }

    @Then("no migration was performed")
    public void no_migration_was_performed() {
        assertFalse(shouldExecute, "Migration execution should have been skipped!");
    }


    private static int getNumberOfMonorepoChildren(Optional<Config> tomlElement) {
        int numberOfMonorepoDependencies = 0;
        if (tomlElement.isPresent()) {
            Config foundDependencies = tomlElement.get();
            Map<String, Object> dependencyMap = foundDependencies.valueMap();

            for (Map.Entry<String, Object> dependency : dependencyMap.entrySet()) {
                String packageName = dependency.getKey();
                Object packageRhs = dependency.getValue();
                if (TomlUtils.representsLocalDevelopmentVersion(packageRhs)) {
                    numberOfMonorepoDependencies++;
                }
            }
        }

        return numberOfMonorepoDependencies;
    }


    private void verifyExecutionOccurred() {
        assertTrue(shouldExecute, "Migration should have been selected to execute!");
        assertTrue(executionSucceeded, "Migration should have executed successfully!");
    }

}
