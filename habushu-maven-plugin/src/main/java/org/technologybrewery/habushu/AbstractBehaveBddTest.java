package org.technologybrewery.habushu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.CommandHelper;

public abstract class AbstractBehaveBddTest {

    protected static final String BEHAVE_PACKAGE = "behave";
    protected static final String BEHAVE_CUCUMBER_FORMATTER = "kappa-maki";
    protected static final String KAPPA_FORMAT = "kappa_maki.kappa_maki_formatter:PrettyCucumberJSONFormatter";
    protected Log log;
    protected BehaveBddTestMojo behaveBddTestMojo;
    protected CommandHelper commandHelper;

    protected AbstractBehaveBddTest(Log log, BehaveBddTestMojo behaveBddTestMojo, CommandHelper commandHelper) {
        this.log = log;
        this.behaveBddTestMojo = behaveBddTestMojo;
        this.commandHelper = commandHelper;
    }

    public void doExecute() throws MojoExecutionException, MojoFailureException {

        if (this.behaveBddTestMojo.isSkipTests()) {
            this.log.warn("Tests are skipped (-DskipTests=true)");
            return;
        }

        File behaveDirectory = new File(this.behaveBddTestMojo.getTestDirectory(), "features");
        String canonicalPathForFile = this.behaveBddTestMojo.getCanonicalPathForFile(behaveDirectory);
        if (hasTests(behaveDirectory)) {
            checkAndInstallBehave();

            // The package managers Habushu currently support run this behave command in the exact same way
            List<String> executeBehaveTestArgs = new ArrayList<>(Arrays.asList("run", BEHAVE_PACKAGE,
                    canonicalPathForFile));

            appendBehaveOptions(executeBehaveTestArgs);

            this.log.info(String.format("Executing behave tests in %s...", canonicalPathForFile));
            this.log.info("-------------------------------------------------------");
            this.log.info("T E S T S");
            this.log.info("-------------------------------------------------------");
            this.commandHelper.executeAndLogOutput(executeBehaveTestArgs,
                    this.behaveBddTestMojo.getBehaveTestEnvironmentVariables());
        } else {
            this.log.warn(String.format("No tests found in %s", canonicalPathForFile));
        }
    }

    protected void installBehave() {
        this.log.info(String.format("%s dependency not specified in pyproject.toml - installing now...",
                    BEHAVE_PACKAGE));
        this.commandHelper.installDevelopmentDependency(BEHAVE_PACKAGE);
    }

    protected void checkAndInstallBehave() {
        if (!behaveBddTestMojo.isBehaveInstalled()) {
            installBehave();
        }
    }

    protected boolean hasTests(File behaveDirectory) throws MojoExecutionException {
        boolean hasTests = false;
        if (behaveDirectory.exists()) {
            try (Stream<Path> path = Files.list(behaveDirectory.toPath())) {
                hasTests = path.findAny().isPresent();
            } catch (IOException e) {
                throw new MojoExecutionException("Could not load behave features directory", e);
            }
        }
        return hasTests;
    }

    protected void appendBehaveOptions(List<String> executeBehaveTestArgs) {
        if (this.behaveBddTestMojo.isOutputCucumberStyleTestReports()) {
            this.commandHelper.installDevelopmentDependency(BEHAVE_CUCUMBER_FORMATTER);
            executeBehaveTestArgs.add("--format=" + KAPPA_FORMAT);
            executeBehaveTestArgs.add("--outfile=target/cucumber-reports/cucumber.json");
            executeBehaveTestArgs.add("--format=progress2");
        }

        if (this.behaveBddTestMojo.isOmitSkippedTests()) {
            executeBehaveTestArgs.add("--no-skipped");
        }

        if (this.behaveBddTestMojo.isDisableOutputCapture()) {
            executeBehaveTestArgs.add("--no-capture");
            executeBehaveTestArgs.add("--no-capture-stderr");
            executeBehaveTestArgs.add("--no-logcapture");
        }

        if (StringUtils.isNotEmpty(this.behaveBddTestMojo.getBehaveOptions())) {
            executeBehaveTestArgs.addAll(Arrays.asList(StringUtils.split(this.behaveBddTestMojo.getBehaveOptions())));
        } else if (this.behaveBddTestMojo.isBehaveExcludeManualTag()) {
            executeBehaveTestArgs.add("--tags=-manual");
        }
    }

}
