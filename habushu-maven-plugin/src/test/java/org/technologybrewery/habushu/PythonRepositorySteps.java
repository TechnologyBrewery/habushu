package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.technologybrewery.habushu.util.PythonRepository;

public class PythonRepositorySteps {
    private PythonRepository repository;
    private PythonRepository anotherRepository;

    @Given("a repository URL of {string}")
    public void aRepositoryURLOf(String baseUrl) {
        repository = new PythonRepository("test-repo", baseUrl);
    }

    @Given("a search index path of {string}")
    public void aSearchIndexPathOf(String indexPath) {
        if (!"null".equals(indexPath)) {
            repository.setIndexPath(indexPath);
        } else {
            repository.setIndexPath(null);
        }
    }

    @Given("a publish path of {string}")
    public void aPublishPathOf(String publishPath) {
        if (!"null".equals(publishPath)) {
            repository.setPublishPath(publishPath);
        } else {
            repository.setPublishPath(null);
        }
    }

    @Given("another repository with {string}, {string}, and {string}")
    public void anotherRepositoryWithAnd(String baseUrl, String indexPath, String publishPath) {
        anotherRepository = new PythonRepository("another-test-repo", baseUrl, indexPath, publishPath);
    }

    @When("a repository is created")
    public void aRepositoryIsCreated() {
        // nothing to do
    }

    @When("the repositories are compared")
    public void theRepositoriesAreCompared() {
        // nothing to do
    }

    @Then("the search index URL is {string}")
    public void theSearchIndexUrlIs(String expectedUrl) {
        String actualUrl = repository.getIndexUrl();
        Assertions.assertEquals(expectedUrl, actualUrl, "Search Index URL constructed incorrectly");
    }

    @Then("the publish URL is {string}")
    public void thePublishUrlIs(String expectedUrl) {
        String actualUrl = repository.getPublishUrl();
        Assertions.assertEquals(expectedUrl, actualUrl, "Publishing URL constructed incorrectly");
    }

    @Then("they are equal")
    public void theyAreEqual() {
        Assertions.assertEquals(repository, anotherRepository, "Repositories are not equal");
        Assertions.assertEquals(repository.hashCode(), anotherRepository.hashCode(), "Repositories have different hash codes");
    }
}
