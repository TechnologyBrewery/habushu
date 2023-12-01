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

import org.technologybrewery.habushu.util.PoetryUtil;

public class PoetrycoreVersionMigrationSteps {

    private File testTomlFileDirectory = new File("./target/test-classes/migration/poetrycore");
    private File pyProjectToml;

    private boolean shouldExecute;

    private boolean executionSucceeded;

    @Given("an existing pyproject.toml file with poetry-core version of 1.6.0 in the build-system group")
    public void an_existing_pyproject_toml_file_with_poetry_core_version_of_1_6_0_in_the_build_system_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-poetry-core-version-1-6-0.toml");
    }

    @Given("an existing pyproject.toml file with poetry-core version less than 1.6.0 in the build-system group")
    public void an_existing_pyproject_toml_file_with_poetry_core_version_less_than_1_6_0_in_the_build_system_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-poetry-core-version-1-0-0.toml");
    }

    @Given("an existing pyproject.toml file with poetry-core version of 1.7.0 in the build-system group")
    public void an_existing_pyproject_toml_file_with_poetry_core_version_of_1_7_0_in_the_build_system_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-poetry-core-version-1-7-0.toml");
    }

    @Given("an existing pyproject.toml file with poetry-core version of 2.0.0 in the build-system group")
    public void an_existing_pyproject_toml_file_with_poetry_core_version_of_2_0_0_in_the_build_system_group() {
        pyProjectToml = new File(testTomlFileDirectory, "with-poetry-core-version-2-0-0.toml");
    }

    @When("Habushu poetry core migration executes")
    public void habushu_migrations_execute() {
        CustomPoetrycoreVersionMigration migration = new CustomPoetrycoreVersionMigration();
        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = (shouldExecute) ? migration.performMigration(pyProjectToml) : false;
    }

    @Then("the poetry-core version is updated to 1.6.0 in the build-system group")
    public void the_poetry_core_version_is_updated_to_1_6_0_in_the_build_system_group() {
        verifyExeuctionOccurred();
        try (FileConfig tomlFileConfig = FileConfig.of(pyProjectToml)) {
            tomlFileConfig.load();

            Optional<Config> toolBuildSystem = tomlFileConfig.getOptional(TomlUtils.BUILD_SYSTEM);
            String poetrycoreVer = "";
            if (toolBuildSystem.isPresent()) {
                Config buildSystem = toolBuildSystem.get();
                Map<String, Object> dependencyMap = buildSystem.valueMap();
                for (Map.Entry<String, Object> dependency : dependencyMap.entrySet()) {

                    String packageName = dependency.getKey();
                    if (packageName.equals(TomlUtils.REQUIRES)) {
                        String dependencyVal = dependency.getValue().toString();
                        if (dependencyVal.contains(TomlUtils.POETRY_CORE) && dependencyVal.contains(TomlUtils.EQUALS)) {
                            poetrycoreVer = dependencyVal.substring(TomlUtils.getIndexOfFirstDigit(dependencyVal), TomlUtils.getIndexOfLastDigit(dependencyVal));
                            break;
                        }
                    }
                }
            }
            int indexStart = TomlUtils.getIndexOfFirstDigit(PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);
            String poetryCoreVersionRequired = PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT.substring(indexStart,PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT.length() );
            assertEquals(poetryCoreVersionRequired, poetrycoreVer,"poetry-core version is not matching with the required poetry-core version and should be updated");
        }
    }

    @Then("no update was performed for poetry-core version")
    public void no_update_was_performed_for_poetry_core_version() {
        assertFalse(shouldExecute, "no update was performed for poetry-core version!");
    }

    private void verifyExeuctionOccurred() {
        assertTrue(shouldExecute, "Migration should have been selected to execute!");
        assertTrue(executionSucceeded, "Migration should have executed successfully!");
    }
}