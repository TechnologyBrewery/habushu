package org.technologybrewery.habushu.exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.technologybrewery.habushu.HabushuException;
import org.slf4j.Logger;

/**
 * Largely pulled from the com.github.eirslett:frontend-maven-plugin and then
 * updated to better propagate diagnostic error output to the developers. The
 * visibility of this class within the frontend-maven-plugin does not easily
 * facilitate reuse in other modules, so a similar version is maintained here.
 */
public class ProcessExecutor {
    private static final String PATH_ENV_VAR = "PATH";

    private Map<String, String> environment;
    private CommandLine commandLine;
    private Executor executor;

    public ProcessExecutor(File workingDirectory, List<String> command, Platform platform,
                           Map<String, String> additionalEnvironment) {
        this(workingDirectory, new ArrayList<>(), command, platform, additionalEnvironment, 0);
    }

    public ProcessExecutor(File workingDirectory, List<String> paths, List<String> command, Platform platform,
                           Map<String, String> additionalEnvironment) {
        this(workingDirectory, paths, command, platform, additionalEnvironment, 0);
    }

    public ProcessExecutor(File workingDirectory, List<String> paths, List<String> command, Platform platform,
                           Map<String, String> additionalEnvironment, long timeoutInSeconds) {
        this.environment = createEnvironment(paths, platform, additionalEnvironment);
        this.commandLine = createCommandLine(command);
        this.executor = createExecutor(workingDirectory, timeoutInSeconds);
    }

    public String executeAndGetResult(final Logger logger) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitValue = -1;
        try {
            exitValue = execute(logger, stdout, stderr);
        } catch (Throwable e) {
            displayProcessOutputForException(stdout, logger);
            displayProcessOutputForException(stderr, logger);
            throw new HabushuException("Could not invoke command! See output above.", e);
        }
        if (exitValue == 0) {
            String result = stdout.toString().trim();
            if (StringUtils.isBlank(result)) {
                result = stderr.toString().trim();
            }
            return result;
        } else {
            throw new HabushuException(stdout + " " + stderr);
        }
    }

    public int executeAndRedirectOutput(final Logger logger) {
        OutputStream stdout = new LoggerOutputStream(logger, 0);
        OutputStream stderr = new LoggerOutputStream(logger, 0);

        try {
            return execute(logger, stdout, stderr);

        } catch (Throwable e) {
            throw new HabushuException("Could not invoke command! See output above.", e);

        } finally {
            IOUtils.closeQuietly(stdout);
            IOUtils.closeQuietly(stderr);
        }
    }

    private int execute(final Logger logger, final OutputStream stdout, final OutputStream stderr) {
        logger.debug("Executing command line {}", commandLine);
        logger.debug("Active PATH: {}", environment.get(PATH_ENV_VAR));
        try {
            ExecuteStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr);
            executor.setStreamHandler(streamHandler);

            int exitValue = executor.execute(commandLine, environment);
            logger.debug("Exit value {}", exitValue);

            return exitValue;
        } catch (ExecuteException e) {
            if (executor.getWatchdog() != null && executor.getWatchdog().killedProcess()) {
                throw new HabushuException("Process killed after timeout");
            }
            throw new HabushuException(e);
        } catch (IOException e) {
            throw new HabushuException(e);
        }
    }

    private CommandLine createCommandLine(List<String> command) {
        CommandLine commmandLine = new CommandLine(command.get(0));

        for (int i = 1; i < command.size(); i++) {
            String argument = command.get(i);
            commmandLine.addArgument(argument, false);
        }

        return commmandLine;
    }

    private Map<String, String> createEnvironment(final List<String> paths, final Platform platform,
                                                  final Map<String, String> additionalEnvironment) {
        final Map<String, String> environment = new HashMap<>(System.getenv());

        if (additionalEnvironment != null) {
            environment.putAll(additionalEnvironment);
        }

        if (platform.isWindows()) {
            for (final Map.Entry<String, String> entry : environment.entrySet()) {
                final String pathName = entry.getKey();
                if (PATH_ENV_VAR.equalsIgnoreCase(pathName)) {
                    final String pathValue = entry.getValue();
                    environment.put(pathName, extendPathVariable(pathValue, paths));
                }
            }
        } else {
            final String pathValue = environment.get(PATH_ENV_VAR);
            environment.put(PATH_ENV_VAR, extendPathVariable(pathValue, paths));
        }

        return environment;
    }

    private String extendPathVariable(final String existingValue, final List<String> paths) {
        final StringBuilder pathBuilder = new StringBuilder();
        for (final String path : paths) {
            pathBuilder.append(path).append(File.pathSeparator);
        }
        if (existingValue != null) {
            pathBuilder.append(existingValue).append(File.pathSeparator);
        }
        return pathBuilder.toString();
    }

    private Executor createExecutor(File workingDirectory, long timeoutInSeconds) {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDirectory);
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer()); // Fixes #41

        if (timeoutInSeconds > 0) {
            executor.setWatchdog(new ExecuteWatchdog(timeoutInSeconds * 1000));
        }

        return executor;
    }

    private static class LoggerOutputStream extends LogOutputStream {
        private final Logger logger;
        private static final String DEBUG_LOG = "DEBUG";
        private static final String WARNING_LOG = "WARNING";
        private static final String ERROR_LOG = "ERROR";
        private static final String CRITICAL_LOG = "CRITICAL";

        LoggerOutputStream(Logger logger, int logLevel) {
            super(logLevel);
            this.logger = logger;
        }

        @Override
        public final void flush() {
            // buffer processing on close() only
        }

        @Override
        protected void processLine(final String line, final int logLevel) {
            if (line.toUpperCase().contains(ERROR_LOG) || line.toUpperCase().contains(CRITICAL_LOG)) {
                logger.error(line);
            } else if (line.toUpperCase().contains(WARNING_LOG)) {
                logger.warn(line);
            } else if (line.toUpperCase().contains(DEBUG_LOG)) {
                logger.debug(line);
            } else {
                logger.info(line);
            }
        }
    }

    /**
     * Helper method that logs the given process output at the error level if its
     * content is not blank.
     *
     * @param output
     * @param logger
     */
    protected void displayProcessOutputForException(ByteArrayOutputStream output, Logger logger) {
        String outputAsStr = output.toString();
        if (StringUtils.isNotBlank(outputAsStr)) {
            logger.error(outputAsStr);
        }
    }
}
