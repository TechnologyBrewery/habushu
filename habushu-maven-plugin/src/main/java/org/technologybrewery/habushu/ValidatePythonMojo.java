package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Leverages the lint package to validate both source and test Python
 * directories using the package manager's run command.
 */
@Mojo(name = "validate-python", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ValidatePythonMojo extends AbstractValidateMojo {

    /**
     * By default, linting will be enabled on the source module. Can be configured to false so that linting is
     * not triggered during build.
     */
    @Parameter(property = "habushu.lint", required = false, defaultValue = "true")
    private boolean lint;

    @Override
    public void doExecute() throws MojoExecutionException {
        if (lint) {
            runLinter(this.workingDirectory);
        }
    }
}
