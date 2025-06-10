package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

public class BehaveBddTestSteps {
    private final String testPackageSet = "behave";
    private final TestBehaveBddTestMojo behaveBddTestMojo = new TestBehaveBddTestMojo();
    private String testPackageToUse = null;

    @Given("a Habushu configuration with testPackage is set to behave")
    public void aHabushuConfigurationWithTestPackageIsSetToBehave() {
        this.behaveBddTestMojo.testPackage = testPackageSet;
    }

    @Given("no test package is installed")
    public void noTestPackageIsInstalled() {
        // set mock CommandHelper in the test mojo
    }

    @When("the behave-bdd-test goal detects the test package to use")
    public void theBehaveBddTestGoalDetectsTheTestPackageToUse() {
        this.testPackageToUse = this.behaveBddTestMojo.getTestPackage(behaveBddTestMojo.getCommandHelper());
    }

    @Then("behave is used for test")
    public void behaveIsUsedForTest() {
        Assertions.assertEquals(testPackageSet, testPackageToUse, "behave is not used for test.");
    }
}
