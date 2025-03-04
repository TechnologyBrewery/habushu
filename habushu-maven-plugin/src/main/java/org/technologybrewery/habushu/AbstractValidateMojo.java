package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.technologybrewery.habushu.exec.AbstractCommandHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractValidateMojo extends AbstractHabushuMojo {
    protected static final String LINT_PACKAGE = "ruff";

    /**
     * Runs the linter on the specified directory with the given checkers and arguments.
     *
     * @param lintDirectory directory to lint
     *
     * @throws MojoExecutionException if an error occurs during linting or the linter cannot be installed
     */
    protected void runLinter(File lintDirectory) throws MojoExecutionException {

        AbstractCommandHelper helper = getCommandHelper();

        if (lintDirectory.exists()) {
            List<String> executeLintArgs = new ArrayList<>(Arrays.asList("run", LINT_PACKAGE, "check"));
            executeLintArgs.add(getCanonicalPathForFile(lintDirectory));
            if (!helper.isDependencyInstalled(LINT_PACKAGE)) {
                getLog().info(String.format("%s dependency not specified in pyproject.toml - installing now...",
                        LINT_PACKAGE));
                helper.installDevelopmentDependency(LINT_PACKAGE);
            }

            getLog().info("Validating code using Ruff...");
            helper.executeAndLogOutput(executeLintArgs);
        } else {
            getLog().warn(String.format("Configured linting directory (%s) does not exist - skipping...",
                    lintDirectory));
        }
    }
}
