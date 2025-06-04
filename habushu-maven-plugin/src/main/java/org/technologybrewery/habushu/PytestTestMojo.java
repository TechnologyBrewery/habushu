package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;

import java.util.Map;

/**
 * Leverages the pytest framework for small tests defined in the configured {@link #testDirectory}.
 * Developers may specify additional command line options via {@link #pytestOptions} or set
 * environment variables via {@link #pytestTestEnvironmentVariables} to apply when running pytest.
 * If {@link #pytestOptions} are provided, {@link #enableVerbose} is effectively overridden and ignored.
 */
@Mojo(name = "pytest-test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class PytestTestMojo extends AbstractHabushuMojo {

    /**
     * Options that should be passed to the pytest command.<b>NOTE:</b> If this
     * value is provided, then {@link #enableVerbose} is ignored.
     */
    @Parameter(property = "habushu.pytestOptions")
    protected String pytestOptions;

    /**
     * Set enableVerbose to true to enable verbose output for more details
     * about which tests have passed, failed or been skipped.
     */
    @Parameter(property = "habushu.enableVerbose", defaultValue = "false")
    protected boolean enableVerbose;
    
    /**
     * Set this to "true" to skip running tests. Its use is NOT RECOMMENDED, but
     * quite convenient on occasion.
     */
    @Parameter(property = "habushu.skipTests", defaultValue = "false")
    protected boolean skipTests;

    /**
     * Environment variables utilized in the pytest tests.
     */
    @Parameter(property = "habushu.pytestTestEnvironmentVariables")
    protected Map<String, String> pytestTestEnvironmentVariables = null;

    public String getPytestOptions() {
        return pytestOptions;
    }

    public boolean enableVerbose() {
        return enableVerbose;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public Map<String, String> getPytestTestEnvironmentVariables() {
        return pytestTestEnvironmentVariables;
    }

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (getTestPackage().equals("pytest")) {
            if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == PackageManager.POETRY) {
                getLog().info("Pytest test is not yet supported for Poetry projects");
            } else {
                PytestTestUv pytestTestUv = new PytestTestUv(getLog(), this,
                        createUvCommandHelper());
                pytestTestUv.doExecute();
            }
        } else {
            getLog().info(String.format("This Mojo is not used for %s test.", getTestPackage()));
        }
    }
}
