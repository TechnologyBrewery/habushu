package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import java.io.File;

public class PytestTestSteps {
    private final String testPackageSet = "pytest";
    private final TestPytestTestMojo pytestTestMojo = new TestPytestTestMojo();
    private String testPackageToUse = null;

    @Given("a Habushu configuration with testPackage is set to pytest")
    public void aHabushuConfigurationWithTestPackageIsSetToPytest() {
        this.pytestTestMojo.testPackage = testPackageSet;
    }

    @When("the pytest-test goal detects the test package to use")
    public void thePytestTestGoalDetectsTheTestPackageToUse() {
        this.testPackageToUse = this.pytestTestMojo.getTestPackage(pytestTestMojo.getCommandHelper());
    }

    @Then("pytest is used for test")
    public void pytestIsUsedForTest() {
        Assertions.assertEquals(testPackageSet, testPackageToUse , "pytest is not used for test.");
    }
}
