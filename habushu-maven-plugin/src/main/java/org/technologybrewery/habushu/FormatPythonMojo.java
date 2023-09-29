package org.technologybrewery.habushu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Leverages the black formatter package to format both source and test Python
 * directories using Poetry's run command.
 */
@Mojo(name = "format-python", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class FormatPythonMojo extends AbstractHabushuMojo {

    protected static final String BLACK_PACKAGE = "black";

    @Override
    public void doExecute() throws MojoExecutionException {

	List<String> directoriesToFormat = new ArrayList<>();
	if (this.sourceDirectory.exists()) {
	    directoriesToFormat.add(getCanonicalPathForFile(sourceDirectory));
	}
	if (this.testDirectory.exists()) {
	    directoriesToFormat.add(getCanonicalPathForFile(testDirectory));
	}

	if (directoriesToFormat.isEmpty()) {
	    getLog().warn(String.format("Neither configured source (%s) nor test (%s) directories exist - skipping...",
		    sourceDirectory, testDirectory));
	}

	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

	if (!poetryHelper.isDependencyInstalled(BLACK_PACKAGE)) {
	    getLog().info(
		    String.format("%s dependency not specified in pyproject.toml - installing now...", BLACK_PACKAGE));
	    poetryHelper.installDevelopmentDependency(BLACK_PACKAGE);
	}

	List<String> executeBlackFormatterArgs = new ArrayList<>();
	executeBlackFormatterArgs.addAll(Arrays.asList("run", BLACK_PACKAGE));
	executeBlackFormatterArgs.addAll(directoriesToFormat);

	getLog().info("Formatting configured source and test directories using black...");
	poetryHelper.executeAndLogOutput(executeBlackFormatterArgs);
    }
}