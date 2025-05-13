package org.technologybrewery.habushu.migration;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.technologybrewery.habushu.migration.poetryv2migrations.AbstractPoetryMigrationSteps;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class RemovePylintBlackMigrationSteps extends AbstractPoetryMigrationSteps {
    public static final String PYTHON_FILE_PATH = testTomlFileDirectory + "/../dev-dependencies/file-with-pylint-comments.py";
    public static final String NO_PYLINT_COMMENTS_FILE_PATH = testTomlFileDirectory + "/../dev-dependencies/no-pylint-comments-for-comparison-in-test-step.py";
    private boolean shouldExecute;
    private boolean executionSucceeded;
    private List<String> allTomlSectionHeaders = new ArrayList<>();

    @Given("a project exists that referrences pylint and black")
    public void a_project_exists_that_referrences_pylint_and_black() {
        String sourceYaml = testTomlFileDirectory + "/../dev-dependencies/with-pylint-and-black-dependencies.toml";
        String targetYaml = testTomlFileDirectory + "/../dev-dependencies/pyproject.toml";
        // In order for the migration to work the test file has to be named pyproject.toml
        HabushuUtil.copyFile(sourceYaml, targetYaml);
        pyProjectToml = new File(testTomlFileDirectory, "../dev-dependencies/pyproject.toml");
    }

    @When("the remove-pylint-black-migration migration runs")
    public void the_remove_pylint_black_migration_migration_runs() {
        RemovePylintBlackMigration migration = new RemovePylintBlackMigration();
        migration.setWorkingDirectory(new File("."));

        shouldExecute = migration.shouldExecuteOnFile(pyProjectToml);
        executionSucceeded = shouldExecute && migration.performMigration(pyProjectToml);
        assertTrue("Migration did not complete successfully.", executionSucceeded);
    }

    @Then("pylint and black referrences are removed from pyproject.toml")
    public void pylint_and_black_referrences_are_removed_from_pyproject_toml() throws IOException {
        String content = Files.readString(pyProjectToml.toPath());
        Pattern pattern = Pattern.compile(RemovePylintBlackMigration.BLACK_PYLINT_REGEX);
        Matcher matcher = pattern.matcher(content);

        assertFalse("Found occurrence of behave or pylint after migration.", matcher.find());
    }

    @Given("a python file exists that has pylint comments")
    public void a_python_file_exists_that_has_pylint_comments() {
        pythonFile = new File(PYTHON_FILE_PATH);
    }

    @When("the remove-pylint-comments-migration migration runs")
    public void the_remove_pylint_comments_migration_migration_runs() {
        RemovePylintCommentsMigration migration = new RemovePylintCommentsMigration();
        migration.setWorkingDirectory(new File("."));

        shouldExecute = migration.shouldExecuteOnFile(pythonFile);
        executionSucceeded = shouldExecute && migration.performMigration(pythonFile);
    }

    @Then("pylint comments are removed from python files")
    public void pylint_comments_are_removed_from_python_files() throws IOException {
        File noPylintCommentsFile = new File(NO_PYLINT_COMMENTS_FILE_PATH);
        assertTrue("Pylint comments were not correctly removed from file",
                executionSucceeded && FileUtils.contentEquals(pythonFile, noPylintCommentsFile));
    }
}