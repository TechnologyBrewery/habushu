package org.technologybrewery.habushu.exec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.technologybrewery.habushu.AbstractHabushuMojo;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.InstallDependenciesMojo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
        ensurePythonInstalled(targetVersion);
        executePythonPinCommand(targetVersion);
    }

    public void installTool(String toolName) {
        executeToolInstallCommand(toolName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDependencyInstalled(String packageName) {
        try {
            // if the package is not installed, log as info
            executeWithCustomExceptionLogLevel(Arrays.asList("pip", "show", packageName), Level.INFO);
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


    /**
     * {@inheritDoc}
     */
    @Override
    public String getProjectVersion() {
        List<String> getPythonProjectVersion = Arrays.asList(
                "--from=toml-cli", "toml",
                "get", "--toml-path=pyproject.toml",
                "project.version");
        List<String> getPythonProjectVersionCommamd = createToolRunCommand(getPythonProjectVersion);
        return execute(getPythonProjectVersionCommamd);
    }

    public List<String> createToolRunCommand(List<String> additionalArguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add("tool");
        arguments.add("run");
        arguments.addAll(additionalArguments);
        return arguments;
    }

    public void executeLockCommand(AbstractHabushuMojo mojo, boolean skipUvLockRefresh, boolean skipUvCheck) {
        List<String> arguments = createLockCommand(skipUvLockRefresh, skipUvCheck);

        if (executeCommandWithCredentials(mojo)) {
            Map<String, String> privateRepoCredentials = getPrivateRepoCredentials(mojo, getRepoId(mojo));
            execute(arguments, privateRepoCredentials);
        } else {
            execute(arguments);
        }
    }

    public void executeLockCommandAndLogAfterTimeout(InstallDependenciesMojo mojo, boolean skipUvCheck, int timeout, TimeUnit timeUnit, String messageToDisplay) {
        List<String> arguments = createLockCommand(mojo.skipLockUpdate(), skipUvCheck);

        if (executeCommandWithCredentials(mojo)) {
            Map<String, String> privateRepoCredentials = getPrivateRepoCredentials(mojo, getRepoId(mojo));
            executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay, privateRepoCredentials);
        } else {
            executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay);
        }
    }

    public void executeSyncCommand(InstallDependenciesMojo mojo, int timeout, TimeUnit timeUnit, String messageToDisplay) {
        List<String> arguments = new ArrayList<>();

        arguments.add("sync");

        for (String groupName : mojo.getWithGroups()) {
            arguments.add("--group");
            arguments.add(groupName);
        }
        for (String groupName : mojo.getWithoutGroups()) {
            logger.warn("While Habushu does support this configuration, `uv sync --no-group` is underdeveloped and does not possess strong useful functionality at this time.");
            arguments.add("--no-group");
            arguments.add(groupName);
        }

        if (executeCommandWithCredentials(mojo)) {
            Map<String, String> privateRepoCredentials = getPrivateRepoCredentials(mojo, getRepoId(mojo));
            executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay, privateRepoCredentials);
        } else {
            executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay);
        }
    }

    private List<String> createLockCommand(boolean skipLockUpdate, boolean skipUvCheck) {
        List<String> arguments = new ArrayList<>();
        arguments.add("lock");

        if (!skipLockUpdate) {
            arguments.add("--refresh");
        }

        if (!skipUvCheck) {
            arguments.add("--check");
        }
        return arguments;
    }

    private String getRepoId(AbstractHabushuMojo mojo) {
        String repoId = StringUtils.EMPTY;
        if (mojo.isUseDevRepository()) {
            if (!mojo.getTestPyPiRepositoryUrl().equals(mojo.getDevRepositoryUrl())){
                repoId = mojo.getDevRepositoryId();
            }
        } else {
            if (!org.codehaus.plexus.util.StringUtils.isEmpty(mojo.getPypiRepoUrl())) {
                if (!"https://pypi.org".equals(mojo.getPypiRepoUrl())) {
                    repoId = mojo.getPypiRepoId();
                }
            }
        }
        return repoId;
    }

    private Map<String, String> getPrivateRepoCredentials(AbstractHabushuMojo mojo, String repoId) {
        String username = mojo.findUsernameForServer(repoId);
        String password = mojo.findPasswordForServer(repoId);
        Map<String, String> credentials = new HashMap<>(System.getenv());
        String uvIndexUsernameEnvironmentVariable = getUvIndexUsernameEnvironmentVariable(repoId);
        String uvIndexPasswordEnvironmentVariable = getUvIndexPasswordEnvironmentVariable(repoId);

        if (username==null && password==null) {
            throw new HabushuException("Your credentials for " + repoId + " must be set in your ~/.m2/settings.xml");
        } else {
            credentials.put(uvIndexUsernameEnvironmentVariable, username);
            credentials.put(uvIndexPasswordEnvironmentVariable, password);
            logAuthenticationInformation(repoId);
        }
        return credentials;
    }

    private String getExportParameter(String repoId){
        return repoId.toUpperCase().replace("-", "_");
    }

    private String getUvIndexUsernameEnvironmentVariable(String repoId){
        return String.format("UV_INDEX_%s_USERNAME", getExportParameter(repoId));
    }

    private String getUvIndexPasswordEnvironmentVariable(String repoId){
        return String.format("UV_INDEX_%s_PASSWORD", getExportParameter(repoId));
    }

    private Boolean isUvIndexUsernameEnvironmentVariableSet(String repoId){
        String uvIndexUsernameEnvironmentVariable = getUvIndexUsernameEnvironmentVariable(repoId);
        return !StringUtils.isEmpty(System.getenv(uvIndexUsernameEnvironmentVariable));
    }

    private Boolean isUvIndexPasswordEnvironmentVariableSet(String repoId){
        String uvIndexPasswordEnvironmentVariable = getUvIndexPasswordEnvironmentVariable(repoId);
        return !StringUtils.isEmpty(System.getenv(uvIndexPasswordEnvironmentVariable));
    }

    private Boolean executeCommandWithCredentials(AbstractHabushuMojo mojo) {
        String repoId = getRepoId(mojo);
        if (!getRepoId(mojo).isEmpty()) {
            return (!isUvIndexUsernameEnvironmentVariableSet(repoId)) && (!isUvIndexPasswordEnvironmentVariableSet(repoId));
        }
        return false;
    }

    private void logAuthenticationInformation(String repoId) {
        String uvIndexUsernameEnvironmentVariable = getUvIndexUsernameEnvironmentVariable(repoId);
        String uvIndexPasswordEnvironmentVariable = getUvIndexPasswordEnvironmentVariable(repoId);
        logger.info("Temporarily setting {} and {} for use in uv command.", uvIndexUsernameEnvironmentVariable, uvIndexPasswordEnvironmentVariable);
    }


   private void executeToolInstallCommand(String argument) {
        List<String> arguments = new ArrayList<>();
        arguments.add("tool");
        arguments.add("install");
        arguments.add(argument);
        executeAndLogOutput(arguments);
    }

    private String executePythonPinCommand() {
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

    private void executePythonPinCommand(String targetVersion) {
        List<String> arguments = new ArrayList<>();
        arguments.add("python");
        arguments.add("pin");
        arguments.add(targetVersion);
        execute(arguments);
    }

    /**
     * Ensures the target version of Python is installed by calling uv venv
     * @param targetVersion the requested version of Python to install
     */
    private void ensurePythonInstalled(String targetVersion) {
        try {
            execute(Arrays.asList("venv", "-p", targetVersion));
        } catch (Throwable e) {
            throw new HabushuException("UV could not install the desired version of python", e);
        }
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
