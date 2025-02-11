package org.technologybrewery.habushu.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.HabushuException;


/**
 * Facilitates the execution of uv commands.
 */
public class UvCommandHelper {

    private static final String UV_COMMAND = "uv";
    private static final Logger logger = LoggerFactory.getLogger(UvCommandHelper.class);

    private static final String extractUvVersionRegex = "^(?:uv\\s)\\b((\\d+.?)(\\d+.?){1,2})\\b";
    private static final String extractPythonVersionRegex = "^(?:[Pp]ython\\s)\\b((\\d+.?)(\\d+.?){1,2})\\b";

    protected File workingDirectory;

    protected String pythonVersionFile; 


    public UvCommandHelper(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.pythonVersionFile = workingDirectory.getAbsolutePath() + "/.python-version";
    }

    /**
     * Returns a {@link Boolean} and {@link String} {@link Pair} indicating whether
     * Poetry is installed and if so, the version of Poetry that is installed. If
     * Poetry is not installed, the returned {@link String} part of the {@link Pair}
     * will be {@code null}.
     *
     * @return
     */
    public Pair<Boolean, String> getIsUvInstalledAndVersion() {
        try {
            ProcessExecutor executor = createUvExecutor(Arrays.asList("--version"));
            String versionResult = executor.executeAndGetResult(logger);

            // Extracts version number from output, given the following format "uv 0.5.20 (1c17662b3 2025-01-15)"
            Pattern versionPattern = Pattern.compile(extractUvVersionRegex);
            Matcher matchedPattern = versionPattern.matcher(versionResult); 
            if (matchedPattern.find()){
                String version = matchedPattern.group(1);
                return new ImmutablePair<Boolean, String>(true, version); 
            } else {
                throw new HabushuException("uv version pattern not found.");
            }
            
        } catch (Throwable e) {
            return new ImmutablePair<Boolean, String>(false, null);
        }
    }

    /**
     * Retrieves the version of Python that is set for the configured working
     * directory.
     *
     * @return
     */
    public String getCurrentPythonVersion() throws MojoExecutionException {
        String currentPythonVersion = pythonVersionFileContents();

        if (!currentPythonVersion.isEmpty()) {
            return currentPythonVersion;
        } else throw new MojoExecutionException("A .python-version file is required to have a reproducible build. To create this file, from the module level directory, run `uv python pin <python-version>`.");
    }

    /**
     * Updates the virtual environment to use the desired Python version.
     *
     * @return
     */
    public void updatePythonVersion(String targetVersion) throws MojoExecutionException {
        List<String> pythonPinCommand = createPythonPinCommand(targetVersion);
        execute(pythonPinCommand);
    }

    /**
     * Returns whether the specified dependency package is installed within this
     * uv project's virtual environment (and pyproject.toml).
     *
     * @param packageName
     * @return
     */
    public boolean isDependencyInstalled(String packageName) {
        try {
            execute(Arrays.asList("pip", "show", packageName));
        } catch (Throwable e) {
            return false;
        }
        return true;

    }

    /**
     * Installs the specified package as a development dependency to this uv
     * project's virtual environment and pyproject.toml specification.
     *
     * @param packageName
     */
    public void installDevelopmentDependency(String packageName) throws MojoExecutionException {
        execute(Arrays.asList("add", packageName, "--group", "dev"));
    }

    /**
     * Executes a uv command with the given arguments, logs the executed
     * command, and returns the resultant process output as a string. This method
     * should be utilized when performing downstream logic based on the output of a
     * uv command, or it is desirable to not show the command's generated
     * stdout.
     *
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    public String execute(List<String> arguments) throws MojoExecutionException {
        if (logger.isInfoEnabled()) {
            logger.info("Executing uv command: {} {}", UV_COMMAND, StringUtils.join(arguments, " "));
        }
        ProcessExecutor executor = createUvExecutor(arguments);
        return executor.executeAndGetResult(logger);
    }

    /**
     * Executes a uv command with the given arguments, logs the executed
     * command, logs the stdout/stderr generated by the process, and returns the
     * process exit code. This method should be utilized when it is desirable to
     * immediately show all of the stdout/stderr produced by a uv command for
     * diagnostic purposes.
     *
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    public int executeAndLogOutput(List<String> arguments) throws MojoExecutionException {
        if (logger.isInfoEnabled()) {
            logger.info("Executing uv command: {} {}", UV_COMMAND, StringUtils.join(arguments, " "));
        }
        ProcessExecutor executor = createUvExecutor(arguments);
        return executor.executeAndRedirectOutput(logger);
    }

    /**
     * Executes a uv command with the given arguments and environment variables,
     * logs the executed command and logs the stdout/stderr generated by the process.
     * This method should be utilized when environment variables are needed for the uv command,
     * and it is desirable to immediately show all the stdout/stderr produced by a uv command for
     * diagnostic purposes.
     *
     * @param arguments
     * @param environmentVariables
     */
    public void executeAndLogOutput(List<String> arguments, Map<String, String> environmentVariables) {
        if (logger.isInfoEnabled()) {
            logger.info("Executing uv command: {} {}", UV_COMMAND, StringUtils.join(arguments, " "));
        }
        ProcessExecutor executor = createUvExecutor(arguments, environmentVariables);
        executor.executeAndRedirectOutput(logger);
    }

    /**
     * Similar to {@link #executeAndLogOutput(List)}, except the executed uv
     * command that is logged obfuscates/masks any given command arguments that are
     * marked as sensitive. This method should be utilized if any uv command
     * line arguments contain sensitive values that are not desirable to log, such
     * as passwords.
     *
     * @param argAndIsSensitivePairs
     * @return
     * @throws MojoExecutionException
     */
    public int executeWithSensitiveArgsAndLogOutput(List<Pair<String, Boolean>> argAndIsSensitivePairs)
            throws MojoExecutionException {
        if (logger.isInfoEnabled()) {
            List<String> argsWithSensitiveArgsMasked = argAndIsSensitivePairs.stream()
                    .map(pair -> pair.getRight() ? "XXXX" : pair.getLeft()).collect(Collectors.toList());
            logger.info("Executing uv command: {} {}", UV_COMMAND,
                    StringUtils.join(argsWithSensitiveArgsMasked, " "));
        }
        ProcessExecutor executor = createUvExecutor(
                argAndIsSensitivePairs.stream().map(Pair::getLeft).collect(Collectors.toList()));
        return executor.executeAndRedirectOutput(logger);
    }

    /**
     * Executes a uv command with the given arguments and logs a warning message
     * if the command has not yet completed after the specified timeout period. This
     * may be useful for providing input to developers when certain uv commands
     * are running for longer than expected and may need to be manually halted due
     * to cache-related issues.<br>
     * <b>NOTE:</b>The executed uv command will *not* be halted nor terminated
     * when the timeout expires. After the timeout expires, this method will
     * continue to wait until underlying uv command completes.
     *
     * @param arguments
     * @param timeout
     * @param timeUnit
     * @return
     */
    public Integer executeUvCommandAndLogAfterTimeout(List<String> arguments, int timeout, TimeUnit timeUnit) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(() -> this.executeAndLogOutput(arguments));
        try {
            return future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            logger.warn("uv " + String.join(" ", arguments)
                    + " has been running for quite some time, you may want to quit the mvn process (Ctrl+c) and run \"uv cache clean\" and restart your build.");
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e1) {
                throw new RuntimeException("Error occurred while waiting for uv command to complete", e1);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error occurred while performing uv command: uv %s",
                    StringUtils.join(arguments, " ")), e);
        } finally {
            executor.shutdown();
        }
    }

    protected ProcessExecutor createUvExecutor(List<String> arguments) {
        List<String> fullCommandArgs = new ArrayList<>();
        fullCommandArgs.add(UV_COMMAND);
        fullCommandArgs.addAll(arguments);
        return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), null);
    }

    protected ProcessExecutor createUvExecutor(List<String> arguments, Map<String, String> environmentVariables) {
        List<String> fullCommandArgs = new ArrayList<>();
        fullCommandArgs.add(UV_COMMAND);
        fullCommandArgs.addAll(arguments);
        return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), environmentVariables);
    }

    public List<String> createLockCommand() {
        return createLockCommand(true);
    }

    public List<String> createLockCommand(boolean skipUvLockRefresh) {
        List<String> arguments = new ArrayList<>();
        arguments.add("lock");

        if (!skipUvLockRefresh) {
            arguments.add("--refresh");
        }
        
        return arguments;
    }

    public List<String> createSyncCommand() {
        List<String> arguments = new ArrayList<>();

        arguments.add("sync");
 
        return arguments;
    }

    public List<String> createRunCommand(List<String> additional_arguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add("run");
        arguments.addAll(additional_arguments);
        return arguments;
    }

    public List<String> createPythonPinCommand(String targetVersion) {
        List<String> arguments = new ArrayList<>();
        arguments.add("python");
        arguments.add("pin");
        arguments.addAll(Arrays.asList(targetVersion));
        return arguments;
    }

    private String pythonVersionFileContents() throws MojoExecutionException {
        String pythonVersion = StringUtils.EMPTY;
        if (checkFileExistance(pythonVersionFile)) {
            try {
                Path pythonVersionFilePath = Paths.get(pythonVersionFile);
                pythonVersion = Files.readString(pythonVersionFilePath, StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                throw new MojoExecutionException(String.format("Could not read file contents for %s.",
                pythonVersionFile), e);
            }
        }
        return pythonVersion;
    }

    private Boolean checkFileExistance(String stringFilePath) {
        File filePath = new File(stringFilePath);
        return filePath.isFile();
    }
}