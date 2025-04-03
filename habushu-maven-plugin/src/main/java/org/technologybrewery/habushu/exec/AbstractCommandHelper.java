package org.technologybrewery.habushu.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facilitates the execution of package manager commands.
 */
public abstract class AbstractCommandHelper implements CommandHelper {
    private static final Logger logger = LoggerFactory.getLogger(AbstractCommandHelper.class);

    protected File workingDirectory;

    protected String packageManagerCommand;


    protected AbstractCommandHelper(File workingDirectory, String packageManagerCommand) {
        this.workingDirectory = workingDirectory;
        this.packageManagerCommand = packageManagerCommand;
    }

    /**
     * {@inheritDoc}
     */
    public String execute(List<String> arguments) {
        if (logger.isInfoEnabled()) {
            logExecutionInformation(arguments);
        }
        ProcessExecutor executor = createPackageManagerExecutor(arguments);
        return executor.executeAndGetResult(logger);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(List<String> arguments, Map<String, String> environmentVariables) {
        if (logger.isInfoEnabled()) {
            logExecutionInformation(arguments);
        }
        ProcessExecutor executor = createPackageManagerExecutor(arguments, environmentVariables);
        executor.executeAndGetResult(logger);
    }

    /**
     * {@inheritDoc}
     */
    public int executeAndLogOutput(List<String> arguments) {
        if (logger.isInfoEnabled()) {
            logExecutionInformation(arguments);
        }
        ProcessExecutor executor = createPackageManagerExecutor(arguments);
        return executor.executeAndRedirectOutput(logger);
    }

    /**
     * {@inheritDoc}
     */
    public int executeAndLogOutput(List<String> arguments, Map<String, String> environmentVariables) {
        if (logger.isInfoEnabled()) {
            logExecutionInformation(arguments);
        }
        ProcessExecutor executor = createPackageManagerExecutor(arguments, environmentVariables);
        return executor.executeAndRedirectOutput(logger);
    }

    /**
     * {@inheritDoc}
     */
    public int executeWithSensitiveArgsAndLogOutput(List<Pair<String, Boolean>> argAndIsSensitivePairs) {
        if (logger.isInfoEnabled()) {
            List<String> argsWithSensitiveArgsMasked = argAndIsSensitivePairs.stream()
                    .map(pair -> Boolean.TRUE.equals(pair.getRight()) ? "XXXX" : pair.getLeft()).collect(Collectors.toList());
            logExecutionInformation(argsWithSensitiveArgsMasked);
        }
        ProcessExecutor executor = createPackageManagerExecutor(
                argAndIsSensitivePairs.stream().map(Pair::getLeft).collect(Collectors.toList()));
        return executor.executeAndRedirectOutput(logger);
    }

    private void logExecutionInformation(List<String> arguments) {
        logger.info("Executing {} command: {} {}", packageManagerCommand, packageManagerCommand,
                StringUtils.join(arguments, " "));
    }

    /**
     * {@inheritDoc}
     */
    public Integer executeAndLogAfterTimeout(List<String> arguments, int timeout, TimeUnit timeUnit, String messageToDisplay) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(() -> this.executeAndLogOutput(arguments));
        try {
            return future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            logger.warn("{} {} has been running for quite some time, you may want to quit the mvn process (Ctrl+c) and " +
                    "run \"{}\" and restart your build.", packageManagerCommand, String.join(" ", arguments),
                    messageToDisplay);
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e1) {
                throw new RuntimeException("Error occurred while waiting for " + packageManagerCommand + " command to complete", e1);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error occurred while performing %s command: %s %s",
                    packageManagerCommand, packageManagerCommand, StringUtils.join(arguments, " ")), e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void executeAndLogAfterTimeout(List<String> arguments, int timeout, TimeUnit timeUnit, String messageToDisplay, Map<String, String> environmentVariables) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(() -> this.executeAndLogOutput(arguments, environmentVariables));
        try {
            future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            logger.warn("{} {} has been running for quite some time, you may want to quit the mvn process (Ctrl+c) and " +
                            "run \"{}\" and restart your build.", packageManagerCommand, String.join(" ", arguments),
                    messageToDisplay);
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e1) {
                throw new RuntimeException("Error occurred while waiting for " + packageManagerCommand + " command to complete", e1);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error occurred while performing %s command: %s %s",
                    packageManagerCommand, packageManagerCommand, StringUtils.join(arguments, " ")), e);
        } finally {
            executor.shutdown();
        }
    }

    protected ProcessExecutor createPackageManagerExecutor(List<String> arguments) {
        List<String> fullCommandArgs = new ArrayList<>();
        fullCommandArgs.add(packageManagerCommand);
        fullCommandArgs.addAll(arguments);
        return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), null);
    }

    protected ProcessExecutor createPackageManagerExecutor(List<String> arguments, Map<String, String> environmentVariables) {
        List<String> fullCommandArgs = new ArrayList<>();
        fullCommandArgs.add(packageManagerCommand);
        fullCommandArgs.addAll(arguments);
        return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), environmentVariables);
    }
}
