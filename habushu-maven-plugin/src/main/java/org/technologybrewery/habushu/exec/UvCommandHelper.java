package org.technologybrewery.habushu.exec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.HabushuException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facilitates the execution of uv commands.
 */
public class UvCommandHelper extends AbstractCommandHelper {

    private static final String UV_COMMAND = "uv";
    private static final Logger logger = LoggerFactory.getLogger(UvCommandHelper.class);

    private static final String EXTRACT_UV_VERSION_REGEX = "^(?:uv\\s)\\b((\\d+.?)(\\d+.?){1,2})\\b";

    private static final Pattern UV_VERSION_PATTERN = Pattern.compile(EXTRACT_UV_VERSION_REGEX);

    protected String pythonVersionFile; 

    protected String virtualEnvironmentDirectory;

    public UvCommandHelper(File workingDirectory) {
        super(workingDirectory, UV_COMMAND);
    
        this.pythonVersionFile = workingDirectory.getAbsolutePath() + "/.python-version";
        this.virtualEnvironmentDirectory = workingDirectory.getAbsolutePath() + "/.venv";
    }

    /**
     * Returns a {@link Boolean} and {@link String} {@link Pair} indicating whether
     * uv is installed and if so, the version of uv that is installed. If
     * uv is not installed, the returned {@link String} part of the {@link Pair}
     * will be {@code null}.
     *
     * @return
     */
    public Pair<Boolean, String> getIsUvInstalledAndVersion() {
        try {
            ProcessExecutor executor = createPackageManagerExecutor(List.of("--version"));
            String versionResult = executor.executeAndGetResult(logger);

            // Extracts version number from output, given the following format "uv 0.5.20 (1c17662b3 2025-01-15)"
            String version = getMatchedPattern(UV_VERSION_PATTERN, versionResult);
            if (!version.isEmpty()){
                return new ImmutablePair<>(true, version);
            } else {
                throw new HabushuException("uv version pattern not found.");
            }
            
        } catch (Throwable e) {
            return new ImmutablePair<>(false, null);
        }
    }

    /**
     * Retrieves the version of Python that is set for the configured working
     * directory via the .python-version file, if it exists, or the virtual environment, 
     * if it exists.
     *
     * @return
     */
    public String getCurrentPythonVersion() {
        return executePythonPinCommand();
    }

    /**
     * Updates the .python-version file to reflect the desired Python version.
     *
     * @return
     */
    public void updatePythonVersion(String targetVersion) {
        executePythonPinCommand(targetVersion);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDependencyInstalled(String packageName) {
        try {
            execute(Arrays.asList("pip", "show", packageName));
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installDevelopmentDependency(String packageName) {
        execute(Arrays.asList("add", packageName, "--group", "dev"));
    }

    public List<String> createLockCommand(boolean skipUvLockRefresh, boolean skipUvCheck) {
        List<String> arguments = new ArrayList<>();
        arguments.add("lock");

        if (!skipUvLockRefresh) {
            arguments.add("--refresh");
        }

        if (!skipUvCheck) {
            arguments.add("--check");
        }
        
        return arguments;
    }

    public List<String> createSyncCommand() {
        List<String> arguments = new ArrayList<>();

        arguments.add("sync");
 
        return arguments;
    }

    public List<String> createToolRunCommand(List<String> additionalArguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add("tool");
        arguments.add("run");
        arguments.addAll(additionalArguments);
        return arguments;
    }

    public String executePythonPinCommand() {
        String pythonVersion = StringUtils.EMPTY;
        List<String> arguments = new ArrayList<>();
        arguments.add("python");
        arguments.add("pin");
        try {
            pythonVersion = execute(arguments);
            return pythonVersion;
        } catch (Throwable e) {
            return pythonVersion;
        }
    }

    public String executePythonPinCommand(String targetVersion) {
        List<String> arguments = new ArrayList<>();
        arguments.add("python");
        arguments.add("pin");
        arguments.add(targetVersion);
        return execute(arguments);
    }

    private String getMatchedPattern(Pattern pattern, String stringToSearch) {
        String stringMatchingPattern = StringUtils.EMPTY;
        Matcher matchedPattern = pattern.matcher(stringToSearch); 
        if (matchedPattern.find()){
            stringMatchingPattern = matchedPattern.group(1);
        }
        return stringMatchingPattern;
    }

}
