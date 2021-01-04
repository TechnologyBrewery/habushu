package org.bitbucket.cpointe.habushu;

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
import org.slf4j.Logger;

/**
 * This class is copied over from the frontend-maven-plugin with just minimal changes around exeception class names. As
 * such, we are not making any other changes as it's tried and true.
 * 
 * We are not importing the plugin via dependency because of how much front end code it has baked into every nook and
 * cranny. Bits of that convey with copying over files, but we felt this is the best balance we could achieve.
 */
public class CondaExecutor {
    private static final String PATH_ENV_VAR = "PATH";

    private Map<String, String> environment;
    private CommandLine commandLine;
    private Executor executor;

    public CondaExecutor(File workingDirectory, List<String> command, Platform platform,
            Map<String, String> additionalEnvironment) {
        this(workingDirectory, new ArrayList<>(), command, platform, additionalEnvironment, 0);
    }

    public CondaExecutor(File workingDirectory, List<String> paths, List<String> command, Platform platform,
            Map<String, String> additionalEnvironment) {
        this(workingDirectory, paths, command, platform, additionalEnvironment, 0);
    }

    public CondaExecutor(File workingDirectory, List<String> paths, List<String> command, Platform platform,
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
            System.out.print(stderr.toString());
            throw new HabushuException("Could not invoke command! See output above.", e);
        }
        if (exitValue == 0) {
            return stdout.toString().trim();
        } else {
            throw new HabushuException(stdout + " " + stderr);
        }
    }

    @SuppressWarnings("deprecation")
    public int executeAndRedirectOutput(final Logger logger) {
        OutputStream stdout = new LoggerOutputStream(logger, 0);
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        
        try {                  
            return execute(logger, stdout, stderr);
            
        } catch (Throwable e) {
            System.out.print(stderr.toString());
            throw new HabushuException("Could not invoke command! See output above.", e);
            
        } finally {           
            IOUtils.closeQuietly(stdout);
            IOUtils.closeQuietly(stderr);
            
        }

    }

    private int execute(final Logger logger, final OutputStream stdout, final OutputStream stderr) {
        logger.debug("Executing command line {}", commandLine);
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
            logger.info(line);
        }
    }
}
