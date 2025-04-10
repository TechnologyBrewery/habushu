package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;

public class InstallPhaseSteps {
    protected TestInstallDependenciesMojo installMojo = new TestInstallDependenciesMojo();

    @Given("the PyPi simple suffix is enabled")
    public void the_pypi_simple_suffix_is_enabled(){
        installMojo.enablePypiSimpleSuffix = true;
    }

    @Given("the PyPi simple suffix is disabled")
    public void the_pypi_simple_suffix_is_disabled(){
        installMojo.enablePypiSimpleSuffix = false;
    }

    @Given("a custom PyPi repository URL is set to {string}")
    public void a_custom_pypi_repository_url_is_set_to(String customRepositoryUrl){
        installMojo.pypiRepoUrl = customRepositoryUrl;
    }

    @Given("the simple suffix is set to {string}")
    public void the_simple_suffix_is_set_to(String pypiSimpleSuffix){
        installMojo.pypiSimpleSuffix = pypiSimpleSuffix;
    }

    @When("the install phase executes")
    public void the_install_phase_executes(){
        // do nothing
    }

    @Then("the PyPi source URL added to the Poetry pyproject.toml is {string}")
    public void the_pypi_source_url_added_to_poetry_pyproject_toml_is(String expectedPypiSourceUrl) throws URISyntaxException {
        String actualPypiSourceUrl = installMojo.getPypiSimpleRepoUrl(installMojo.pypiRepoUrl, true);
        assertEquals(expectedPypiSourceUrl, actualPypiSourceUrl, "Unexpected PyPi source repository URL!");
    }

    @Given("a custom dev PyPi repository URL is set to {string}")
    public void a_custom_dev_pypi_repository_url_is_set_to(String customDevRepositoryUrl){
        installMojo.useDevRepository = true;
        installMojo.devRepositoryUrl = customDevRepositoryUrl;
    }

    @Then("the dev PyPi source URL added to the Poetry pyproject.toml is {string}")
    public void the_dev_pypi_source_url_added_to_poetry_pyproject_toml_is(String expectedPypiSourceUrl) throws URISyntaxException {
        String actualPypiSourceUrl = installMojo.getPypiSimpleRepoUrl(installMojo.devRepositoryUrl, true);
        assertEquals(expectedPypiSourceUrl, actualPypiSourceUrl, "Unexpected dev PyPi source repository URL!");
    }

    @Then("the PyPi source URL added to the UV pyproject.toml is {string}")
    public void the_pypi_source_url_added_to_uv_pyproject_toml_is(String expectedPypiSourceUrl) throws URISyntaxException {
        String actualPypiSourceUrl = installMojo.getPypiSimpleRepoUrl(installMojo.pypiRepoUrl, false);
        assertEquals(expectedPypiSourceUrl, actualPypiSourceUrl, "Unexpected PyPi source repository URL!");
    }

    @Then("the dev PyPi source URL added to the UV pyproject.toml is {string}")
    public void the_dev_pypi_source_url_added_to_uv_pyproject_toml_is(String expectedPypiSourceUrl) throws URISyntaxException {
        String actualPypiSourceUrl = installMojo.getPypiSimpleRepoUrl(installMojo.devRepositoryUrl, false);
        assertEquals(expectedPypiSourceUrl, actualPypiSourceUrl, "Unexpected dev PyPi source repository URL!");
    }

    @Then("the PyPi source added to the Poetry pyproject.toml is noted as a \"supplemental\" priority")
    public void the_pypi_source_added_to_poetry_pyproject_toml_is_noted_as_supplemental(){
        assertTrue(installMojo.getShouldAddPriority(true));
    }

    @Then("the PyPi source added to the UV pyproject.toml is not noted as a \"supplemental\" priority")
    public void the_pypi_source_added_to_uv_pyproject_toml_is_not_noted_as_supplemental(){
        assertFalse(installMojo.getShouldAddPriority(false));
    }

}
