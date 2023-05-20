package org.technologybrewery.habushu;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DependencyManagementSteps {

    private DependencyManagementTestMojo mojo;
    private File originalPyProjectToml = new File("target/orig.pyproject.toml");
    private File finalPyProjectToml = new File("target/final.pyproject.toml");
    private HabushuException encounteredException;

    @Before
    public void cleanUp() throws IOException {
        originalPyProjectToml.delete();
        finalPyProjectToml.delete();
        encounteredException = null;

        createPyProjectTomlFiles();
    }

    @Given("a Habushu configuration with no dependency management entries")
    public void a_habushu_configuration_with_no_dependency_management_entries() throws Exception {
        mojo = new DependencyManagementTestMojo(finalPyProjectToml);
        List<PackageDefinition> managedDependencies = new ArrayList<>();
        mojo.setManagedDependencies(managedDependencies);
    }

    @Given("a Habushu configuration with dependency management entries")
    public void a_habushu_configuration_with_dependency_management_entries() {
        mojo = new DependencyManagementTestMojo(finalPyProjectToml);

        List<PackageDefinition> managedDependencies = new ArrayList<>();
        PackageDefinition blackUpdateDefinition = getBlackUpdate();
        managedDependencies.add(blackUpdateDefinition);

        mojo.setManagedDependencies(managedDependencies);
    }

    @Given("a Habushu configuration with a managed dependency of {string} and {string}")
    public void a_habushu_configuration_with_a_managed_dependency_of_and(String packageName, String operatorAndVersion) {
        createMojoWithManagedDependency(packageName, operatorAndVersion, true);
    }

    @Given("a Habushu configuration with an inactive managed dependency of {string} and {string}")
    public void a_habushu_configuration_with_an_inactive_managed_dependency_of_and(String packageName, String operatorAndVersion) {
        createMojoWithManagedDependency(packageName, operatorAndVersion, false);
    }

    protected void createMojoWithManagedDependency(String packageName, String operatorAndVersion, boolean isActive) {
        mojo = new DependencyManagementTestMojo(finalPyProjectToml);

        List<PackageDefinition> managedDependencies = new ArrayList<>();
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setPackageName(packageName);
        packageDefinition.setOperatorAndVersion(operatorAndVersion);
        packageDefinition.setActive(isActive);
        managedDependencies.add(packageDefinition);

        mojo.setManagedDependencies(managedDependencies);
    }


    @Given("update managed dependencies when found is disabled")
    public void update_managed_dependencies_when_found_is_disabled() {
        mojo.setUpdateManagedDependenciesWhenFound(false);
    }

    @Given("fail on managed dependency mismatches is enabled")
    public void fail_on_managed_dependency_mismatches_is_enabled() {
        mojo.setFailOnManagedDependenciesMismatches(true);
    }


    @When("Habushu executes")
    public void habushu_executes() throws Exception {
        try {
            mojo.processManagedDependencyMismatches();
        } catch (HabushuException e) {
            encounteredException = e;
        }
    }

    @Then("the pyproject.toml file has no updates")
    public void the_pyproject_toml_file_has_no_updates() throws IOException {
        Assertions.assertTrue(FileUtils.contentEquals(originalPyProjectToml, finalPyProjectToml), "Unexpected pyproject.toml changes found!");
    }

    @Then("the pyproject.toml file has updates")
    public void the_pyproject_toml_file_has_updates() throws IOException {
        Assertions.assertFalse(FileUtils.contentEquals(originalPyProjectToml, finalPyProjectToml), "Unexpected pyproject.toml changes found!");
    }

    @Then("the build process is halted")
    public void the_build_process_is_halted() {
        Assertions.assertNotNull(encounteredException, "An exception should have been thrown to stop the build!");
    }

    private void createPyProjectTomlFiles() throws IOException {
        File baseFile = new File("src/test/resources/base-test-pyproject.toml");
        FileUtils.copyFile(baseFile, originalPyProjectToml);
        FileUtils.copyFile(baseFile, finalPyProjectToml);
    }

    private PackageDefinition getBlackUpdate() {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setPackageName("black");
        packageDefinition.setOperatorAndVersion("^23.3.0");
        return packageDefinition;
    }

}
