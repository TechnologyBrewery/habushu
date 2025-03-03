package org.technologybrewery.habushu;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.Before;
import org.apache.commons.io.FileUtils;
import org.technologybrewery.habushu.util.PackageManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormatterSteps {

    // Working Directory Files
    private static final File target = new File("target/test-classes/test-formatter/workdir");
    private static final File targetPyFile = new File("target/test-classes/test-formatter/workdir/test_ruff_formatter.py");
    private static final File targetTomlFile = new File("target/test-classes/test-formatter/workdir/pyproject.toml");

    // Test files
    private static final File badlyFormattedFileTemplate = new File("target/test-classes/test-formatter/test_ruff_formatter.py");
    private static final File tomlPoetryWithRuffTemplate = new File("target/test-classes/test-formatter/formatter-test-pyproject.toml");
    private static final File tomlUvWithRuffTemplate = new File("target/test-classes/test-formatter/formatter-uv-test-pyproject.toml");
    private static final File tomlPoetryWithRuffButNoConfigsTemplate = new File("target/test-classes/test-formatter/formatter-no-config-test-pyproject.toml");
    private static final File tomlUvWithRuffButNoConfigsTemplate = new File("target/test-classes/test-formatter/formatter-uv-no-config-test-pyproject.toml");
    private static final File tomlPoetryWithoutRuffTemplate = new File("target/test-classes/test-formatter/formatter-missing-test-pyproject.toml");
    private static final File tomlUvWithoutRuffTemplate = new File("target/test-classes/test-formatter/formatter-uv-missing-test-pyproject.toml");
    private static final File expectedFormattedFile = new File("target/test-classes/test-formatter/test_ruff_formatter_expected.py");
    private static final File defaultRuffConfigs = new File("target/test-classes/test-formatter/ruff_formatter_default_settings.toml");
    private final FormatPythonMojoTestWrapper mojo = new FormatPythonMojoTestWrapper();

    @Before
    public void cleanup() throws IOException {
        if (!target.exists()) {
            target.mkdirs();
        }
        FileUtils.cleanDirectory(target);
    }

    @Given("a {string} Habushu configuration with formatting rules")
    public void aHabushuConfigurationWithFormattingRules(String packageManager) throws IOException {
        if (PackageManager.POETRY.toString().toLowerCase().equals(packageManager)) {
            initializeTestDirectory(tomlPoetryWithRuffTemplate);
        } else { // uv
            initializeTestDirectory(tomlUvWithRuffTemplate);
        }
    }

    @Given("a {string} Habushu configuration without formatting rules")
    public void aHabushuConfigurationWithoutFormattingRules(String packageManager) throws IOException {
        if (PackageManager.POETRY.toString().toLowerCase().equals(packageManager)){
            initializeTestDirectory(tomlPoetryWithRuffButNoConfigsTemplate);
        } else { // uv
            initializeTestDirectory(tomlUvWithRuffButNoConfigsTemplate);
        }
    }

    @Given("a {string} Habushu configuration without a formatter installed")
    public void aHabushuConfigurationWithoutAFormatterInstalled(String packageManager) throws IOException {
        if (PackageManager.POETRY.toString().toLowerCase().equals(packageManager)) {
            initializeTestDirectory(tomlPoetryWithoutRuffTemplate);
        } else { // uv
            initializeTestDirectory(tomlUvWithoutRuffTemplate);
        }
    }

    @Given("a badly formatted .py file")
    public void aBadlyFormattedPyFile() throws IOException {
        FileUtils.copyFile(badlyFormattedFileTemplate, targetPyFile);
    }

    @Given("the formatter is disabled")
    public void theFormatterIsDisabled() {
        mojo.enableFormatter(false);
    }

    @When("habushu executes during the process-classes lifecycle hook")
    public void habushuExecutesDuringTheProcessClassesLifecycleHook() {
        mojo.doExecute();
    }

    @Then("the validation should automatically format the file")
    public void theValidationShouldAutomaticallyFormatTheFile() throws IOException {
        String expected = Files.readString(expectedFormattedFile.toPath());
        String real = Files.readString(targetPyFile.toPath());
        assertEquals("Expected file contents for formatter does not match", expected, real);
    }

    @Then("the build should automatically install the formatter")
    public void theBuildShouldAutomaticallyInstallTheFormatter() {
        assertTrue("Missing formatter was not downloaded as expected", mojo.wasPackageInstallAttempted());
    }

    @Then("the formatter should not run")
    public void theFormatterShouldNotRun() throws IOException {
        String expected = Files.readString(badlyFormattedFileTemplate.toPath());
        String real = Files.readString(targetPyFile.toPath());
        assertEquals("Expected file contents for formatter does not match", expected, real);
    }

    @Then("default formatting rules should be automatically added to the configuration if not already present")
    public void defaultFormattingRulesShouldBeAutomaticallyAddedToTheConfigurationIfNotAlreadyPresent() throws IOException {
        String pyprojectContents = new String(Files.readAllBytes(targetTomlFile.toPath()));
        String ruffConfigs = new String(Files.readAllBytes(defaultRuffConfigs.toPath()));

        assertTrue("Expected default ruff configs in pyproject.toml but found none",
                pyprojectContents.contains(ruffConfigs)
        );
    }

    private void initializeTestDirectory(File baseToml) throws IOException {
        mojo.setSourceDirectory(targetPyFile.getParentFile());
        mojo.setTestDirectory(new File("this/directory/does/not/exist"));
        FileUtils.copyFile(baseToml, targetTomlFile);
    }
}
