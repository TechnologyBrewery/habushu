package org.technologybrewery.habushu.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps determine the version of Python that is available on the user's
 * {@code PATH}.
 */
public class PythonVersionHelper {

    private static final Logger logger = LoggerFactory.getLogger(PythonVersionHelper.class);

    private static final String PYTHON_COMMAND = "python";
    private static final String PYTHON_3_COMMAND = "python3";
    private static final String pythonVersion3Regex = "^3.*";
    private static final String extractVersionRegex = "^.*?(?=([0-9]))";

    private final String desiredPythonVersion;
    private final File workingDirectory;

    public PythonVersionHelper(File workingDirectory, String desiredPythonVersion) {
	Validate.notNull(desiredPythonVersion);

	this.workingDirectory = workingDirectory;
	this.desiredPythonVersion = desiredPythonVersion;
    }

    /**
     * Retrieves the version of Python that is set for the configured working
     * directory.
     *
     * @return
     */
    public String getCurrentPythonVersion() throws MojoExecutionException {
	String version = execute(Collections.singletonList("--version"));
	return version.replaceAll(extractVersionRegex, "");
    }

    /**
     * Executes a python command with the given arguments, logs the executed
     * command, and returns the resultant process output as a string.
     *
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    protected String execute(List<String> arguments) throws MojoExecutionException {
	ProcessExecutor executor;

	if (desiredPythonVersion.matches(pythonVersion3Regex) && isPython3Installed()) {
	    executor = createPythonExecutor(PYTHON_3_COMMAND, arguments);
	    if (logger.isInfoEnabled()) {
		logger.info("Executing python3 command: {} {}", PYTHON_3_COMMAND, StringUtils.join(arguments, " "));
	    }
	} else {
	    executor = createPythonExecutor(PYTHON_COMMAND, arguments);
	    if (logger.isInfoEnabled()) {
		logger.info("Executing python command: {} {}", PYTHON_COMMAND, StringUtils.join(arguments, " "));
	    }
	}

	return executor.executeAndGetResult(logger);
    }

    private ProcessExecutor createPythonExecutor(String pythonCommand, List<String> arguments) {
	List<String> fullCommandArgs = new ArrayList<>();
	fullCommandArgs.add(pythonCommand);
	fullCommandArgs.addAll(arguments);
	return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), null);
    }

    private boolean isPython3Installed() {
	try {
	    ProcessExecutor executor = createPythonExecutor(PYTHON_3_COMMAND, Collections.singletonList("--version"));
	    executor.executeAndGetResult(logger);
	} catch (Throwable e) {
	    return false;
	}
	return true;
    }
}
