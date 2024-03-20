package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Leverages the behave package to execute BDD scenarios that are defined in the
 * "features" sub-directory of the configured {@link #testDirectory}. By
 * default, as per {@link #behaveExcludeManualTag}, features/scenarios tagged
 * with {@literal @manual} are skipped. Developers may specify additional
 * command line options via {@link #behaveOptions} to apply when running behave.
 * If {@link #behaveOptions} are provided, {@link #behaveExcludeManualTag} is
 * effectively overridden and ignored.
 */
@Mojo(name = "behave-bdd-test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class BehaveBddTestMojo extends AbstractHabushuMojo {

    protected static final String BEHAVE_PACKAGE = "behave";
    protected static final String BEHAVE_CUCUMBER_FORMATTER = "behave-cucumber-formatter";

    /**
     * Options that should be passed to the behave command. <b>NOTE:</b> If this
     * value is provided, then {@link #behaveExcludeManualTag} is ignored.
     */
    @Parameter(property = "habushu.behaveOptions", required = false)
    protected String behaveOptions;

    /**
     * By default, format Behave test results in a JSON compatible with Cucumber Reports plugin
     */
    @Parameter(property = "habushu.outputCucumberStyleTestReports", required = false, defaultValue = "true")
    protected boolean outputCucumberStyleTestReports;

    /**
     * By default, Behave marks Scenarios and Features with skipped steps as failures in its reports even if the
     * other test steps themselves pass. To match Cucumber's default logic, this setting will prevent capturing skipped
     * tests if there are no failures.
     */
    @Parameter(property = "habushu.omitSkippedTests", required = false, defaultValue = "true")
    protected boolean omitSkippedTests;

    /**
     * By default, exclude any scenario or feature file tagged with '@manual'.
     * <b>NOTE:</b> If {@link #behaveOptions} are provided, this property is
     * ignored.
     */
    @Parameter(property = "habushu.behaveExcludeManualTag", required = true, defaultValue = "true")
    protected boolean behaveExcludeManualTag;

    /**
     * Set this to "true" to skip running tests. Its use is NOT RECOMMENDED, but
     * quite convenient on occasion.
     */
    @Parameter(property = "habushu.skipTests", defaultValue = "false")
    protected boolean skipTests;

    /**
     * By default, Behave captures all logging, stdout and stderr. This setting outputs logging, stdout and stderr to
     * the console.
     */
    @Parameter(property = "habushu.disableOutputCapture", defaultValue = "true")
    protected boolean disableOutputCapture;


    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {

        if (skipTests) {
            getLog().warn("Tests are skipped (-DskipTests=true)");
            return;
        }

        File behaveDirectory = new File(testDirectory, "features");

        boolean hasTests;
        try {
            hasTests = behaveDirectory.exists() && Files.list(behaveDirectory.toPath()).findAny().isPresent();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not load behave features directory", e);
        }

        if (hasTests) {
            PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

            if (!poetryHelper.isDependencyInstalled(BEHAVE_PACKAGE)) {
                getLog().info(String.format("%s dependency not specified in pyproject.toml - installing now...",
                        BEHAVE_PACKAGE));
                poetryHelper.installDevelopmentDependency(BEHAVE_PACKAGE);
            }

            List<String> executeBehaveTestArgs = new ArrayList<>();
            executeBehaveTestArgs
                    .addAll(Arrays.asList("run", BEHAVE_PACKAGE, getCanonicalPathForFile(behaveDirectory)));

            if (outputCucumberStyleTestReports) {
                poetryHelper.installDevelopmentDependency(BEHAVE_CUCUMBER_FORMATTER);
                executeBehaveTestArgs.add("--format=behave_cucumber_formatter:PrettyCucumberJSONFormatter");
                executeBehaveTestArgs.add("--outfile=target/cucumber-reports/cucumber.json");
            }

            if (omitSkippedTests) {
                executeBehaveTestArgs.add("--no-skipped");
            }

            if (disableOutputCapture) {
                executeBehaveTestArgs.add("--no-capture");
                executeBehaveTestArgs.add("--no-capture-stderr");
                executeBehaveTestArgs.add("--no-logcapture");
            }

            if (StringUtils.isNotEmpty(behaveOptions)) {
                executeBehaveTestArgs.addAll(Arrays.asList(StringUtils.split(behaveOptions)));
            } else {
                if (behaveExcludeManualTag) {
                    executeBehaveTestArgs.add("--tags=-manual");
                }
            }

            getLog().info(String.format("Executing behave tests in %s...", getCanonicalPathForFile(behaveDirectory)));
            getLog().info("-------------------------------------------------------");
            getLog().info("T E S T S");
            getLog().info("-------------------------------------------------------");
            poetryHelper.executeAndLogOutput(executeBehaveTestArgs);
        } else {
            getLog().warn(String.format("No tests found in %s", getCanonicalPathForFile(behaveDirectory)));
        }

    }

}
