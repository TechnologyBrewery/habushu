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
import org.technologybrewery.shell.exec.CommandHelper;
import org.technologybrewery.shell.exec.ProcessExecutor;

/**
 * Helps determine the version of Python that is available on the user's
 * {@code PATH}.
 */
public class PythonVersionHelper extends CommandHelper {

    private static final Logger logger = LoggerFactory.getLogger(PythonVersionHelper.class);

    private static final String PYTHON_COMMAND = "python";
    private static final String PYTHON_3_COMMAND = "python3";
    private static final String PYTHON_VERSION_3_REGEX = "^3.*";
    private static final String EXTRACT_VERSION_REGEX = "^.*?(?=(\\d))";


    public PythonVersionHelper(File workingDirectory, String desiredPythonVersion) {
        super(workingDirectory, PYTHON_COMMAND);
        Validate.notNull(desiredPythonVersion);

        if (desiredPythonVersion.matches(PYTHON_VERSION_3_REGEX) && isPython3Installed()) {
            setBaseCommand(PYTHON_3_COMMAND);
        }
    }

    /**
     * Retrieves the version of Python that is set for the configured working
     * directory.
     *
     * @return
     */
    public String getCurrentPythonVersion() throws MojoExecutionException {
        String version = executeWithDebugLogging(Collections.singletonList("--version")).getStdout();
        return version.replaceAll(EXTRACT_VERSION_REGEX, "");
    }

    private boolean isPython3Installed() {
        try {
            execute(List.of("--version"));
        } catch (Throwable e) {
            return false;
        }
        return true;
    }
}
