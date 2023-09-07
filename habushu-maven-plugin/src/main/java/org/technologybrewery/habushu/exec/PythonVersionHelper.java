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
import org.slf4j.event.Level;

/**
 * Helps determine the version of Python that is available on the user's
 * {@code PATH}.
 */
public class PythonVersionHelper {

    private static final Logger logger = LoggerFactory.getLogger(PythonVersionHelper.class);

    private static final String PYTHON_COMMAND = "python";
    private static final String PYTHON_3_COMMAND = "python3";
    private static final String PYTHON_VERSION_3_REGEX = "^3.*";
    private static final String EXTRACT_VERSION_REGEX = "^.*?(?=(\\d))";

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
        String version = quietlyExecute(Collections.singletonList("--version"));
        return version.replaceAll(EXTRACT_VERSION_REGEX, "");
    }

    /**
     * Executes a python command with the given arguments, logs the executed command
     * at DEBUG level, and returns the resultant process output as a string.
     *
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    protected String quietlyExecute(List<String> arguments) throws MojoExecutionException {
        return execute(arguments, Level.DEBUG);
    }

    /**
     * Executes a python command with the given arguments, logs the executed command
     * at INFO level, and returns the resultant process output as a string.
     *
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    protected String execute(List<String> arguments) throws MojoExecutionException {
        return execute(arguments, Level.INFO);
    }

    /**
     * Executes a python command with the given arguments, logs the executed
     * command, and returns the resultant process output as a string.
     *
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    private String execute(List<String> arguments, Level logLevel) throws MojoExecutionException {
        ProcessExecutor executor;
        String pythonCommand;

        if (desiredPythonVersion.matches(PYTHON_VERSION_3_REGEX) && isPython3Installed()) {
            executor = createPythonExecutor(PYTHON_3_COMMAND, arguments);
            pythonCommand = PYTHON_3_COMMAND;

        } else {
            executor = createPythonExecutor(PYTHON_COMMAND, arguments);
            pythonCommand = PYTHON_COMMAND;
        }

        if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
            String logStatement = String.format("Executing command: %s %s", pythonCommand, StringUtils.join(arguments, " "));
            if (Level.INFO.equals(logLevel)) {
                logger.info(logStatement);
            } else if (Level.DEBUG.equals(logLevel)) {
                logger.debug(logStatement);
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
