package org.technologybrewery.habushu;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ReportFormattingSteps {
    @When("the Habushu project is built")
    public void the_habushu_project_is_built() {
        // Manual - test is dependent on files generated after Behave runs and cannot be automated
    }

    @Then("a Cucumber report file {string} exists")
    public void a_cucumber_report_file_exists(String string) {
        // Manual - test is dependent on files generated after Behave runs and cannot be automated
    }
}
