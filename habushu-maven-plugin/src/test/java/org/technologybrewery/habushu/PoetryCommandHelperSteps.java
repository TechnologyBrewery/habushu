package org.technologybrewery.habushu;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;


public class PoetryCommandHelperSteps {
    private PoetryCommandHelperTestWrapper poetryHelper;
    boolean isPoetryVersionAtLeast2;
    List<String> arguments;
    boolean testSkipUpdate;
    boolean testForceSync;

    @Given("the Poetry version is {string}")
    public void the_poetry_version_is(String version) {
        poetryHelper = new PoetryCommandHelperTestWrapper(new File("."), version);
    }

    @When("the version is checked against \"2.0.0\"")
    public void the_version_is_checked_against_2_0_0() {
        isPoetryVersionAtLeast2 = poetryHelper.isPoetryVersionAtLeastMinimumVersion();
    }

    @Then("the result should be {}")
    public void the_result_should_be_true(boolean expectedResult) {
        assertEquals(expectedResult, isPoetryVersionAtLeast2, "Unexpected Poetry version found.");
    }

    @Given("skipLockUpdate is {}")
    public void skip_poetry_lock_update_is(boolean skipLockUpdate) {
        testSkipUpdate = skipLockUpdate;
    }

    @When("the lock command is created")
    public void the_lock_command_is_created() {
        arguments = poetryHelper.createLockCommand(testSkipUpdate);
    }

    @Given("forceSync is {}")
    public void force_sync_is_true(boolean forceSync) {
        testForceSync = forceSync;
    }

    @When("the install command is created")
    public void the_install_command_is_created() {
        arguments = poetryHelper.createInstallCommand(testForceSync);
    }

    @When("the use pyenv command is created")
    public void the_use_pyenv_command_is_created() {
        arguments = poetryHelper.createUsePyenvCommand();
    }

    @Then("the returned arguments should be:")
    public void the_returned_arguments_should_be(DataTable dataTable) {
        List<String> expectedArgs = dataTable.asList(String.class);
        // Filter out any empty strings from list of given arguments
        List<String> filteredExpected = expectedArgs.stream()
                .filter(item -> item != null && !item.trim().isEmpty())
                .collect(Collectors.toList());
        assertEquals(filteredExpected, arguments, "Mismatch in poetry command arguments");
    }

}
