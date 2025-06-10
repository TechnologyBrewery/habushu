package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.CommandHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPytestTest {

    protected static final String PYTEST_PACKAGE = "pytest";
    protected Log log;
    protected PytestTestMojo pytestTestMojo;
    protected CommandHelper commandHelper;

    protected AbstractPytestTest(Log log, PytestTestMojo pytestTestMojo, CommandHelper commandHelper) {
        this.log = log;
        this.pytestTestMojo = pytestTestMojo;
        this.commandHelper = commandHelper;
    }

    public void doExecute() throws MojoExecutionException, MojoFailureException {

        if (this.pytestTestMojo.isSkipTests()) {
            this.log.warn("Tests are skipped (-DskipTests=true)");
            return;
        }

        File testDirectory = this.pytestTestMojo.getTestDirectory();
        if (hasTests(testDirectory)) {
            this.log.info("================================================================================");
            this.log.info("T E S T S");
            this.log.info("================================================================================");

            checkAndInstallPytest();
            // The package managers Habushu currently supports run this pytest command in the exact same way
            List<String> executePytestTestArgs = new ArrayList<>(Arrays.asList("run", PYTEST_PACKAGE));

            appendPytestOptions(executePytestTestArgs);
            this.log.info(String.format("Executing pytest tests in %s...", testDirectory));
            this.commandHelper.executeAndLogOutput(executePytestTestArgs,
                    this.pytestTestMojo.getPytestTestEnvironmentVariables());

        } else {
            this.log.warn(String.format("A pytest test file must be named as test_*.py or *_test.py. No pytest test files found under %s or sub directories", testDirectory.getAbsolutePath()));
        }
    }

    protected void appendPytestOptions(List<String> executePytestTestArgs) {

        if (StringUtils.isNotEmpty(this.pytestTestMojo.getPytestOptions())) {
            executePytestTestArgs.addAll(Arrays.asList(StringUtils.split(this.pytestTestMojo.getPytestOptions())));
        } else if (this.pytestTestMojo.enableVerbose) {
            executePytestTestArgs.add("-v");
        }
    }

    protected void installPytest() {
        this.log.info(String.format("%s dependency not specified in pyproject.toml - installing now...",
                    PYTEST_PACKAGE));
        this.commandHelper.installDevelopmentDependency(PYTEST_PACKAGE);
    }

    protected void checkAndInstallPytest() {
        if (!pytestTestMojo.isPytestInstalled()) {
            installPytest();
        }
    }

    protected boolean hasTests(File directory) throws MojoExecutionException {
        boolean hasTests = false;
        if (directory.exists()) {
            try (Stream<Path> pathStream = Files.list(directory.toPath())){
                List<Path> files = pathStream.collect(Collectors.toList());
                for (Path file : files) {
                    if (file.toFile().isDirectory()) {
                        hasTests = hasTests(file.toFile());
                    } else if (isPytestTestFile(file)) {
                        hasTests = true;
                    }
                    if (hasTests) {
                        break;
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Could not find pytest test files", e);
            }
        }
        return hasTests;
    }

    private boolean isPytestTestFile(Path path) {
        String fileName = path.getFileName().toString();
        // for pytest to detect the test files, they must be named test_*.py or *_test.py
        return  (fileName.startsWith("test_") && fileName.endsWith(".py")) || fileName.endsWith("_test.py");
    }

}
