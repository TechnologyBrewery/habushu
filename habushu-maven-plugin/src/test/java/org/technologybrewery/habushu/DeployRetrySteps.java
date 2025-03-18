package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import java.io.File;
import java.util.Collections;

public class DeployRetrySteps {

    protected TestRetryPublishToPyPiRepoMojo mojo;
    protected int retryCount;

    @Given("Habushu is using its default configuration")
    public void habushu_is_using_its_default_configuration() {
        this.retryCount = -1;
    }

    @Given("Habushu is configured to retry {int}")
    public void habushu_is_configured_to_retry(Integer retryCount) {
        this.retryCount = retryCount;
    }

    @When("{int} failures are encountered")
    public void failures_are_encountered(Integer finalRetryNumber) {
        mojo = new TestRetryPublishToPyPiRepoMojo(retryCount, finalRetryNumber);
    }

    @Then("Habushu fails to perform the push using Poetry")
    public void habushu_fails_to_perform_the_push_using_poetry() {
        try {
            PoetryCommandHelper poetryHelper = new PoetryCommandHelper(new File("./"));
            mojo.invokePublishUsingPoetry(poetryHelper, Collections.emptyList());
            Assertions.fail("Should have encountered a retry exception!");
        } catch (Exception e) {
            Assertions.assertTrue(true, "Expected a MojoExecutionException to be throw to signify a retry failure!");
        }
    }

    @Then("Habushu successfully performs the push using Poetry")
    public void habushu_successfully_performs_the_push_using_poetry() throws Exception {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(new File("./"));
        mojo.invokePublishUsingPoetry(poetryHelper, Collections.emptyList());
    }

    @Then("Habushu fails to perform the push using UV")
    public void habushu_fails_to_perform_the_push_using_uv() {
        try {
            UvCommandHelper uvCommandHelper = new UvCommandHelper(new File("./"));
            mojo.invokePublishUsingUv(uvCommandHelper, Collections.emptyList());
            Assertions.fail("Should have encountered a retry exception!");
        } catch (Exception e) {
            Assertions.assertTrue(true, "Expected a MojoExecutionException to be throw to signify a retry failure!");
        }
    }

    @Then("Habushu successfully performs the push using UV")
    public void habushu_successfully_performs_the_push_using_uv() throws Exception {
        UvCommandHelper uvCommandHelper = new UvCommandHelper(new File("./"));
        mojo.invokePublishUsingUv(uvCommandHelper, Collections.emptyList());
    }


}
