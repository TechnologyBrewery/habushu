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

public class UvDependencyManagementSteps {

    private DependencyManagementTestMojo mojo;
    private File originalPyProjectToml = new File("target/orig.pyproject.toml");
    private File finalPyProjectToml = new File("target/pyproject.toml");
    private HabushuException encounteredException;

    @Before("@dependencyManagementUv")
    public void cleanUp() throws IOException {
        originalPyProjectToml.delete();
        finalPyProjectToml.delete();
        encounteredException = null;

        createPyProjectTomlFiles();
    }

    @Given("a Habushu configuration with no UV dependency management entries")
    public void a_habushu_configuration_with_no_dependency_management_entries() throws Exception {
        mojo = new DependencyManagementTestMojo();
        List<PackageDefinition> managedDependencies = new ArrayList<>();
        mojo.setManagedDependencies(managedDependencies);
    }

    @Given("a Habushu configuration with UV dependency management entries")
    public void a_habushu_configuration_with_dependency_management_entries() {
        mojo = new DependencyManagementTestMojo();

        List<PackageDefinition> managedDependencies = new ArrayList<>();
        PackageDefinition blackUpdateDefinition = getBlackUpdate();
        managedDependencies.add(blackUpdateDefinition);

        mojo.setManagedDependencies(managedDependencies);
    }

    @Given("a Habushu configuration with a UV managed dependency of {string} and {string}")
    public void a_habushu_configuration_with_a_managed_dependency_of_and(String packageName, String operatorAndVersion) {
        createMojoWithManagedDependency(packageName, operatorAndVersion, true);
    }

    @Given("a Habushu configuration with an inactive managed dependency of {string} and {string}")
    public void a_habushu_configuration_with_an_inactive_managed_dependency_of_and(String packageName, String operatorAndVersion) {
        createMojoWithManagedDependency(packageName, operatorAndVersion, false);
    }

    protected void createMojoWithManagedDependency(String packageName, String operatorAndVersion, boolean isActive) {
        mojo = new DependencyManagementTestMojo();

        List<PackageDefinition> managedDependencies = new ArrayList<>();
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setPackageName(packageName);
        packageDefinition.setOperatorAndVersion(StringEscapeUtils.unescapeJava(operatorAndVersion));
        packageDefinition.setActive(isActive);
        managedDependencies.add(packageDefinition);

        mojo.setManagedDependencies(managedDependencies);
    }


    @Given("update UV managed dependencies when found is disabled")
    public void update_managed_dependencies_when_found_is_disabled() {
        mojo.setUpdateManagedDependenciesWhenFound(false);
    }

    @Given("fail on UV managed dependency mismatches is enabled")
    public void fail_on_managed_dependency_mismatches_is_enabled() {
        mojo.setFailOnManagedDependenciesMismatches(true);
    }

    @Given("replace development version is disabled")
    public void replace_development_version_is_disabled() {
        mojo.overridePackageVersion = false;
    }

    @When("Habushu executes with uv")
    public void habushu_executes_using_uv() throws Exception {
        try {
            mojo.processManagedDependencyMismatchesUv();
        } catch (HabushuException e) {
            encounteredException = e;
        }
    }

    @Then("the UV pyproject.toml file has no updates")
    public void the_pyproject_toml_file_has_no_updates() throws IOException {
        Assertions.assertTrue(FileUtils.contentEquals(originalPyProjectToml, finalPyProjectToml), "Unexpected pyproject.toml changes found!");
    }

    @Then("the UV pyproject.toml file has updates")
    public void the_pyproject_toml_file_has_updates() throws IOException {
        Assertions.assertFalse(FileUtils.contentEquals(originalPyProjectToml, finalPyProjectToml), "Expected pyproject.toml changes, but found none!");
    }

    @Then("the UV build process is halted")
    public void the_build_process_is_halted() {
        Assertions.assertNotNull(encounteredException, "An exception should have been thrown to stop the build!");
    }

    @Then("the UV pyproject.toml file is updated to contain {string} and {string}")
    public void the_pyproject_toml_file_is_updated_to_contain_and(String packageName, String updatedOperatorAndVersion) throws Exception {
        boolean foundMatch = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(finalPyProjectToml))) {
            String line = reader.readLine();

            while (line != null) {
                System.out.println(line);
                if (line.contains(packageName) && line.contains(updatedOperatorAndVersion)) {
                    foundMatch = true;
                }

                line = reader.readLine();
            }
        }

        Assertions.assertTrue(foundMatch, "Expected to find the update");

    }

    private void createPyProjectTomlFiles() throws IOException {
        File baseFile = new File("src/test/resources/base-uv-test-pyproject.toml");
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
