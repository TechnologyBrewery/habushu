package org.technologybrewery.habushu;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PoetryDependencyManagementSteps {

    private DependencyManagementTestMojo mojo;
    private File originalPyProjectToml = new File("target/orig.pyproject.toml");
    private File finalPyProjectToml = new File("target/pyproject.toml");
    private HabushuException encounteredException;
    private String baseFilePath = "src/test/resources/base-test-pyproject.toml";

    @Before("@dependencyManagementPoetry")
    public void cleanUp() throws IOException {
        originalPyProjectToml.delete();
        finalPyProjectToml.delete();
        encounteredException = null;

        CommonDependencyManagementSteps.createPyProjectTomlFiles(baseFilePath, originalPyProjectToml, finalPyProjectToml);
    }

    @Given("a Habushu configuration with no poetry dependency management entries")
    public void a_habushu_configuration_with_no_poetry_dependency_management_entries() throws Exception {
        mojo = new DependencyManagementTestMojo();
        List<PackageDefinition> managedDependencies = new ArrayList<>();
        mojo.setManagedDependencies(managedDependencies);
    }

    @Given("a Habushu configuration with poetry dependency management entries")
    public void a_habushu_configuration_with_poetry_dependency_management_entries() {
        mojo = new DependencyManagementTestMojo();

        List<PackageDefinition> managedDependencies = new ArrayList<>();
        PackageDefinition blackUpdateDefinition = CommonDependencyManagementSteps.getPackageDefinition("black","^23.3.0");
        managedDependencies.add(blackUpdateDefinition);

        mojo.setManagedDependencies(managedDependencies);
    }

    @Given("a Habushu configuration with a poetry managed dependency of {string} and {string}")
    public void a_habushu_configuration_with_a_poetry_managed_dependency_of_and(String packageName, String operatorAndVersion) {
        mojo = CommonDependencyManagementSteps.createMojoWithManagedDependency(packageName, operatorAndVersion, true);
    }

    @Given("a Habushu configuration with a poetry inactive managed dependency of {string} and {string}")
    public void a_habushu_configuration_with_an_inactive_managed_dependency_of_and(String packageName, String operatorAndVersion) {
        mojo = CommonDependencyManagementSteps.createMojoWithManagedDependency(packageName, operatorAndVersion, false);
    }


    @Given("update managed dependencies when found is disabled for poetry")
    public void update_managed_dependencies_when_found_is_disabled() {
        mojo.setUpdateManagedDependenciesWhenFound(false);
    }

    @Given("fail on poetry managed dependency mismatches is enabled")
    public void fail_on_managed_dependency_mismatches_is_enabled() {
        mojo.setFailOnManagedDependenciesMismatches(true);
    }

    @Given("poetry replace development version is disabled")
    public void replace_development_version_is_disabled() {
        mojo.overridePackageVersion = false;
    }

    @When("Habushu executes with poetry")
    public void habushu_executes_using_poetry() throws Exception {
        try {
            mojo.processManagedDependencyMismatchesPoetry();
        } catch (HabushuException e) {
            encounteredException = e;
        }
    }

    @Then("the poetry pyproject.toml file has no updates")
    public void the_pyproject_toml_file_has_no_updates() throws IOException {
        Assertions.assertTrue(FileUtils.contentEquals(originalPyProjectToml, finalPyProjectToml), "Unexpected pyproject.toml changes found!");
    }

    @Then("the poetry pyproject.toml file has updates")
    public void the_pyproject_toml_file_has_updates() throws IOException {
        Assertions.assertFalse(FileUtils.contentEquals(originalPyProjectToml, finalPyProjectToml), "Expected pyproject.toml changes, but found none!");
    }

    @Then("the build process with poetry is halted")
    public void the_build_process_is_halted() {
        Assertions.assertNotNull(encounteredException, "An exception should have been thrown to stop the build!");
    }

    @Then("the poetry pyproject.toml file is updated to contain {string} and {string}")
    public void the_pyproject_toml_file_is_updated_to_contain_and(String packageName, String updatedOperatorAndVersion) throws Exception {
        String expectedTomlUpdate = packageName + " = \"" + updatedOperatorAndVersion + "\"";

        boolean foundMatch = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(finalPyProjectToml))) {
            String line = reader.readLine();

            while (line != null) {
                if (line.equals(expectedTomlUpdate)) {
                    foundMatch = true;
                }

                line = reader.readLine();
            }
        }

        Assertions.assertTrue(foundMatch, "Expected to find the following update: " + expectedTomlUpdate);

    }

}
