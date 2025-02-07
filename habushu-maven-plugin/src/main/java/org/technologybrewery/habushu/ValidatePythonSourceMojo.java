package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Leverages the lint package to validate both source and test Python
 * directories using Poetry's run command.
 */
@Mojo(name = "validate-python-source", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ValidatePythonSourceMojo extends AbstractValidateMojo {

    /**
     * By default, linting will be enabled on the source module. Can be configured to false so that linting is
     * not triggered during build.
     */
    @Parameter(property = "habushu.lintSource", required = false, defaultValue = "true")
    private boolean lintSource;

    /**
     * Specifies disabled checkers for lint on source module.
     */
    @Parameter(property = "habushu.sourceLintDisabledChecker", required = false, defaultValue = "C,R,W")
    private String sourceLintDisabledChecker;

    /**
     * Specifies enabled checkers for lint on source module.
     */
    @Parameter(property = "habushu.sourceLintEnabledChecker", required = false)
    private String sourceLintEnabledChecker;

    /**
     * By default, build will stop if lint errors are found in source module. Can be configured to true so that
     * build will continue. 
     */
    @Parameter(property = "habushu.sourceFailOnLintErrors", required = false, defaultValue = "true")
    private boolean sourceFailOnLintErrors;

    @Override
    public void doExecute() throws MojoExecutionException {
        if (lintSource) {
            runLinter(this.sourceDirectory, sourceLintDisabledChecker, sourceLintEnabledChecker, sourceFailOnLintErrors, null);
        }
    }
}
