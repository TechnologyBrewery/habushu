package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.technologybrewery.habushu.util.HabushuUtil;

public class DeployPhaseSteps {

    protected TestPublishToPyPiRepoMojo deployMojo = new TestPublishToPyPiRepoMojo();

    @Given("use development repositories are enabled")
    public void use_development_repositories_are_enabled() {
        deployMojo.useDevRepository = true;
    }

    @Given("a custom dev repository is configured to {string}")
    public void a_custom_dev_repository_is_configured_to(String customDevRepositoryUrl) {
        deployMojo.devRepositoryUrl = customDevRepositoryUrl;
    }

    @Given("a custom push dev repository url suffix of {string}")
    public void a_custom_push_dev_repository_url_path_of(String customPushDevRepositoryUrlPath) {
        deployMojo.devRepositoryUrlUploadSuffix = customPushDevRepositoryUrlPath;
    }

    @Given("a null URL")
    public void a_null_url() {
        deployMojo.devRepositoryUrl = null;
    }

    @Given("an empty URL")
    public void an_empty_url() {
        deployMojo.devRepositoryUrl = "";
    }

    @When("the Habushu deploy phase executes")
    public void the_habushu_deploy_phase_executes() throws MojoExecutionException, MojoFailureException {
        // do nothing
    }

    @Then("the dev repository url is {string}")
    public void the_dev_repository_url_is(String expectedDevRepository) {
        Assertions.assertEquals(expectedDevRepository, deployMojo.getRepositoryUrl(true), "Unexpected default dev repository url!");
    }

    @Then("the repository url of {string} contains a trailing slash")
    public void the_repository_url_of_contains_a_trailing_slash(String expectedRepositoryUrl) {
        String processedRepositoryUrl = HabushuUtil.addTrailingSlash(deployMojo.devRepositoryUrl);
        Assertions.assertEquals(expectedRepositoryUrl, processedRepositoryUrl, "Unexpected repository url!");
        Assertions.assertTrue(processedRepositoryUrl.endsWith("/"), "Missing trailing slash!");
    }


    @Then("the repository url is null")
    public void the_repository_url_is_null() {
        Assertions.assertNull(HabushuUtil.addTrailingSlash(deployMojo.devRepositoryUrl), "Expected null repository url!");
    }

    @Then("the repository url is empty")
    public void the_repository_url_is_empty() {
        Assertions.assertEquals("", HabushuUtil.addTrailingSlash(deployMojo.devRepositoryUrl), "Expected null repository url!");
    }

}
