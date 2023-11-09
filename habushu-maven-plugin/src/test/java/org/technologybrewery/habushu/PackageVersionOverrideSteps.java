package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PackageVersionOverrideSteps {
    private String pomVersion;
    private String tomlVersion;

    @Given("a Habushu module with a POM version of {string}")
    public void aHabushuModuleWithAPomVersionOfSemver(String pomVersion) {
        this.pomVersion = pomVersion;
    }

    @When("the POM version is translated to a PEP 440 version")
    public void thePomVersionIsTranslatedToAPep440Version() {
        tomlVersion = AbstractHabushuMojo.getPythonPackageVersion(pomVersion, false, null);
    }

    @Then("the pyproject.toml file is updated with the version {string}")
    public void thePyprojectTomlFileIsUpdatedWithTheVersionPep440(String pep440Version) {
        assertEquals(pep440Version, tomlVersion, "POM version translated to PEP 440 format incorrectly");
    }
}
