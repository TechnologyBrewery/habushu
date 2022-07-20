package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;

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

    /**
     * Options that should be passed to the behave command. <b>NOTE:</b> If this
     * value is provided, then {@link #behaveExcludeManualTag} is ignored.
     */
    @Parameter(property = "habushu.behaveOptions", required = false)
    protected String behaveOptions;

    /**
     * List of environment variables that will be set in the virtual environment in
     * which tests are executed. <b>NOTE:</b> Support for this parameter has not yet
     * been implemented in the Poetry-based implementation of Habushu
     */
    @Parameter
    private Map<String, String> environmentVariables;

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
    @Parameter(property = "skipTests", defaultValue = "false")
    protected boolean skipTests;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

	if (skipTests) {
	    getLog().warn("Tests are skipped (-DskipTests=true)");
	    return;
	}

	File behaveDirectory = new File(testDirectory, "features");

	boolean hasTests;
	try {
	    hasTests = Files.list(behaveDirectory.toPath()).findAny().isPresent();
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
	}

	else {
	    getLog().warn(String.format("No tests found in %s", getCanonicalPathForFile(behaveDirectory)));
	}

    }

    /**
     * TODO: Applies the provided {@link #environmentVariables} to the virtual
     * environment in which behave tests are executed. This method has not yet been
     * adapted to support Poetry-based Habushu implementation and is not yet used!
     * 
     * @param set
     * @return
     */
    private String applyEnvVarsToVirtualEnv(boolean set) {
	String command = "unset ";
	if (set) {
	    command = "export ";
	}

	StringBuilder stringBuilder = new StringBuilder();
	if (environmentVariables != null && !environmentVariables.isEmpty()) {
	    Set<String> keys = environmentVariables.keySet();
	    Iterator<String> iter = keys.iterator();
	    while (iter.hasNext()) {

		// Get the system property
		String systemProperty = iter.next().trim();
		String systemPropertyValue = environmentVariables.get(systemProperty).trim();
		stringBuilder.append(command + systemProperty);

		if (set) {

		    // Add the commands to the system property
		    stringBuilder.append("=" + systemPropertyValue);
		}
		stringBuilder.append("\n");
	    }
	}
	return stringBuilder.toString();
    }

}
