package org.technologybrewery.habushu.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
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
public class UvCommandHelper extends AbstractCommandHelper {

    private static final String UV_COMMAND = "uv";
    private static final Logger logger = LoggerFactory.getLogger(UvCommandHelper.class);

    private static final String extractUvVersionRegex = "^(?:uv\\s)\\b((\\d+.?)(\\d+.?){1,2})\\b";

    private static final Pattern uvVersionPattern = Pattern.compile(extractUvVersionRegex);

    private static final String extractPythonVersionRegex = "^(?:[Pp]ython\\s)\\b((\\d+.?)(\\d+.?){1,2})\\b";

    private static final Pattern pythonVersionPattern = Pattern.compile(extractPythonVersionRegex);

    protected File workingDirectory;

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
            ProcessExecutor executor = createPackageManagerExecutor(Arrays.asList("--version"));
            String versionResult = executor.executeAndGetResult(logger);

            // Extracts version number from output, given the following format "uv 0.5.20 (1c17662b3 2025-01-15)"
            String version = getMatchedPattern(uvVersionPattern, versionResult);
            if (!version.isEmpty()){
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
     * directory via the .python-version file, if it exists, or the virtual environment, 
     * if it exists.
     *
     * @return
     */
    public String getCurrentPythonVersion() throws MojoExecutionException {
        String currentPythonVersion = pythonVersionFileContents();
        if (!currentPythonVersion.isEmpty()) {
            return currentPythonVersion;
        } else {
            currentPythonVersion = pythonVersionFromVirtualEnvironment();
            if (!currentPythonVersion.isEmpty()){
                logger.info("A .python-version does not currently exist for this package. Creating it now with the Python version being used in the current virutal environment (in .venv).");
                updatePythonVersion(currentPythonVersion);
            }
            return currentPythonVersion;
        }
    }

    /**
     * Updates the .python-version file to reflect the desired Python version.
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

    public List<String> createRunCommand(List<String> additional_arguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add("run");
        arguments.addAll(additional_arguments);
        return arguments;
    }

    public List<String> createToolRunCommand(List<String> additional_arguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add("tool");
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

    private String getMatchedPattern(Pattern pattern, String stringToSearch) {
        String stringMatchingPattern = StringUtils.EMPTY;
        Matcher matchedPattern = pattern.matcher(stringToSearch); 
        if (matchedPattern.find()){
            stringMatchingPattern = matchedPattern.group(1);
        }
        return stringMatchingPattern;
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

    private String pythonVersionFromVirtualEnvironment() throws MojoExecutionException {
        String pythonVersion = StringUtils.EMPTY;
        if (checkDirectoryExistance(virtualEnvironmentDirectory)) {
            List<String> getPythonVersionFromRunCommand = createRunCommand(Arrays.asList("python", "--version"));
            String rawCommandResult = execute(getPythonVersionFromRunCommand);
            pythonVersion = getMatchedPattern(pythonVersionPattern, rawCommandResult);
        }
        return pythonVersion;
    }

    private Boolean checkDirectoryExistance(String stringDirectoryPath) {
        File directoryPath = new File(stringDirectoryPath);
        return (directoryPath.exists() && directoryPath.isDirectory());
    }

}