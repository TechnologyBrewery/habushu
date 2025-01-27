package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;

@Mojo(name = "validate-python-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ValidatePythonTestMojo extends AbstractValidateMojo {

    /**
     * By default, linting will be enabled on the test module. Can be configured to false so that linting is
     * not triggered during build.
     */
    @Parameter(property = "habushu.lintTest", required = false, defaultValue = "true")
    private boolean lintTest;

    /**
     * Specifies disabled checkers for lint on test module.
     */
    @Parameter(property = "habushu.testLintDisabledChecker", required = false, defaultValue = "C,R,W,E0102")
    private String testLintDisabledChecker;

    /**
     * Specifies enabled checkers for lint on test module.
     */
    @Parameter(property = "habushu.testLintEnabledChecker", required = false)
    private String testLintEnabledChecker;

    /**
     * By default, build will stop if lint errors are found in test module. Can be configured to true so that
     * pylint does interrupt build. 
     */
    @Parameter(property = "habushu.testFailOnLintErrors", required = false, defaultValue = "true")
    private boolean testFailOnLintErrors;

    @Override
    public void doExecute() throws MojoExecutionException {
        if (lintTest) {
            List<String> extraArgs = List.of("--recursive=true");
            runLinter(this.testDirectory, testLintDisabledChecker, testLintEnabledChecker, testFailOnLintErrors, extraArgs);
        }
    }

}
