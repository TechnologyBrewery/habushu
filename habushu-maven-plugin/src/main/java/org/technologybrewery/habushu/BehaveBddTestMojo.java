package org.technologybrewery.habushu;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;

/**
 * Leverages the behave package to execute BDD scenarios that are defined in the
 * "features" sub-directory of the configured {@link #testDirectory}. By
 * default, as per {@link #behaveExcludeManualTag}, features/scenarios tagged
 * with {@literal @manual} are skipped. Developers may specify additional
 * command line options via {@link #behaveOptions} to apply when running behave.
 * If {@link #behaveOptions} are provided, {@link #behaveExcludeManualTag} is
 * effectively overridden and ignored.
 */
@Mojo(name = "behave-bdd-test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class BehaveBddTestMojo extends AbstractHabushuMojo {

    /**
     * Options that should be passed to the behave command. <b>NOTE:</b> If this
     * value is provided, then {@link #behaveExcludeManualTag} is ignored.
     */
    @Parameter(property = "habushu.behaveOptions")
    protected String behaveOptions;

    /**
     * By default, format Behave test results in a JSON compatible with Cucumber Reports plugin
     */
    @Parameter(property = "habushu.outputCucumberStyleTestReports", defaultValue = "true")
    protected boolean outputCucumberStyleTestReports;

    /**
     * By default, Behave marks Scenarios and Features with skipped steps as failures in its reports even if the
     * other test steps themselves pass. To match Cucumber's default logic, this setting will prevent capturing skipped
     * tests if there are no failures.
     */
    @Parameter(property = "habushu.omitSkippedTests", defaultValue = "true")
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

    /**
     * Environment variables utilized in the behave bdd tests.
     */
    @Parameter(property = "habushu.behaveTestEnvironmentVariables")
    protected Map<String, String> behaveTestEnvironmentVariables = null;

    public String getBehaveOptions() {
        return behaveOptions;
    }

    public boolean isOutputCucumberStyleTestReports() {
        return outputCucumberStyleTestReports;
    }

    public boolean isOmitSkippedTests() {
        return omitSkippedTests;
    }

    public boolean isBehaveExcludeManualTag() {
        return behaveExcludeManualTag;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public boolean isDisableOutputCapture() {
        return disableOutputCapture;
    }

    public Map<String, String> getBehaveTestEnvironmentVariables() {
        return behaveTestEnvironmentVariables;
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (getTestPackage().equals("behave")) {
            if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == PackageManager.POETRY) {
                BehaveBddTestPoetry behaveBddTestPoetry = new BehaveBddTestPoetry(getLog(), this,
                        createPoetryCommandHelper());
                behaveBddTestPoetry.doExecute();
            } else {
                BehaveBddTestUv behaveBddTestUv = new BehaveBddTestUv(getLog(), this,
                        createUvCommandHelper());
                behaveBddTestUv.doExecute();
            }
        } else {
            getLog().info(String.format("This Mojo is not used for %s test.", getTestPackage()));
        }
    }
}
