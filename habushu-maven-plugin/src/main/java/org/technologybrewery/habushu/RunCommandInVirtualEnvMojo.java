package org.technologybrewery.habushu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Leverages {@code poetry run} to execute a Python command or script within
 * this Poetry project's virtual environment. For example, this goal might be
 * bound to the {@code compile} phase to facilitate the generation of
 * gRPC/protobuf bindings as an automated part of the build following dependency
 * installation.
 */
@Mojo(name = "run-command-in-virtual-env")
public class RunCommandInVirtualEnvMojo extends AbstractHabushuMojo {

    /**
     * Whitespace-delimited command arguments that will be provided to
     * {@code poetry run} to execute. For example, if {@code python -V} is provided
     * to this parameter, {@code poetry run python -V} will be executed within the
     * Poetry package's virtual environment.
     */
    @Parameter(property = "habushu.runCommandArgs")
    protected String runCommandArgs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
	List<String> poetryRunCommandArgs = new ArrayList<>(Arrays.asList(StringUtils.split(runCommandArgs)));
	poetryRunCommandArgs.add(0, "run");

	getLog().info("Executing command in virtual environment via 'poetry run'...");
	poetryHelper.executeAndLogOutput(poetryRunCommandArgs);
    }

}
