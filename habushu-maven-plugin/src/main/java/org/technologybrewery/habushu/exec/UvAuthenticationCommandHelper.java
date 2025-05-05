package org.technologybrewery.habushu.exec;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.InitializeHabushuMojo;
import org.technologybrewery.habushu.InstallDependenciesMojo;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facilitates the execution of uv commands.
 */
public class UvAuthenticationCommandHelper extends AbstractCommandHelper {
    /**
     * Instance of InitializeHabushuMojo
     */
    protected InitializeHabushuMojo initializeHabushuMojo;
    protected InstallDependenciesMojo installDependenciesMojo;

    private static final String UV_COMMAND = "uv";
    private static final Logger logger = LoggerFactory.getLogger(UvAuthenticationCommandHelper.class);

    public UvAuthenticationCommandHelper(File workingDirectory, InitializeHabushuMojo initializeHabushu, InstallDependenciesMojo installMojo) {
        super(workingDirectory, UV_COMMAND);

        this.initializeHabushuMojo = initializeHabushu;
        this.installDependenciesMojo = installMojo;
    }

    public void executeLockCommand(boolean skipUvLockRefresh, boolean skipUvCheck) {
        List<String> arguments = new ArrayList<>();
        arguments.add("lock");

        if (!skipUvLockRefresh) {
            arguments.add("--refresh");
        }

        if (!skipUvCheck) {
            arguments.add("--check");
        }

        if (executeCommandWithCredentials()) {
            Map<String, String> privateRepoCredentials = getPrivateRepoCredentials(getRepoId());
            execute(arguments, privateRepoCredentials);
        } else {
            execute(arguments);
        }
    }

    public void executeLockCommandAndLogAfterTimeout(boolean skipUvLockRefresh, boolean skipUvCheck, int timeout, TimeUnit timeUnit, String messageToDisplay) {
        List<String> arguments = new ArrayList<>();
        arguments.add("lock");

        if (!skipUvLockRefresh) {
            arguments.add("--refresh");
        }

        if (!skipUvCheck) {
            arguments.add("--check");
        }

        if (executeCommandWithCredentials()) {
            Map<String, String> privateRepoCredentials = getPrivateRepoCredentials(getRepoId());
                executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay, privateRepoCredentials);
            } else {
                executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay);
        }
    }

    public void executeSyncCommand(String[] withGroups, String[] withoutGroups, int timeout, TimeUnit timeUnit, String messageToDisplay) {
        List<String> arguments = new ArrayList<>();

        arguments.add("sync");

        for (String groupName : withGroups) {
            arguments.add("--group");
            arguments.add(groupName);
        }
        for (String groupName : withoutGroups) {
            logger.warn("While Habushu does support this configuration, `uv sync --no-group` is underdeveloped and does not possess strong useful functionality at this time.");
            arguments.add("--no-group");
            arguments.add(groupName);
        }

        if (executeCommandWithCredentials()) {
            Map<String, String> privateRepoCredentials = getPrivateRepoCredentials(getRepoId());
            executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay, privateRepoCredentials);
        } else {
            executeAndLogAfterTimeout(arguments, timeout, timeUnit, messageToDisplay);
        }
    }

    public boolean isUseDevRepository() {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.isUseDevRepository();
        } else {
            return installDependenciesMojo.isUseDevRepository();
        }
    }

    public String getTestPyPiRepositoryUrl() {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.getTestPyPiRepositoryUrl();
        } else {
            return installDependenciesMojo.getTestPyPiRepositoryUrl();
        }
    }

    public String getDevRepositoryUrl() {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.getDevRepositoryUrl();
        } else {
            return installDependenciesMojo.getDevRepositoryUrl();
        }
    }

    public String getDevRepositoryId() {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.getDevRepositoryId();
        } else {
            return installDependenciesMojo.getDevRepositoryId();
        }
    }

    public String getPypiRepoUrl() {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.getPypiRepoUrl();
        } else {
            return installDependenciesMojo.getPypiRepoUrl();
        }
    }

    public String getPypiRepoId() {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.getPypiRepoId();
        } else {
            return installDependenciesMojo.getPypiRepoId();
        }
    }

    private String getRepoId() {
        String repoId = StringUtils.EMPTY;
        if (isUseDevRepository()) {
            if (!getTestPyPiRepositoryUrl().equals(getDevRepositoryUrl())){
                repoId = getDevRepositoryId();
            }
        } else if (!org.codehaus.plexus.util.StringUtils.isEmpty(getPypiRepoUrl()) && !"https://pypi.org".equals(getPypiRepoUrl())) {
            repoId = getPypiRepoId();
        }
        return repoId;
    }

    public String findUsernameForServer(String repoId) {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.findUsernameForServer(repoId);
        } else {
            return installDependenciesMojo.findUsernameForServer(repoId);
        }
    }

    public String findPasswordForServer(String repoId) {
        if (initializeHabushuMojo != null){
            return initializeHabushuMojo.findPasswordForServer(repoId);
        } else {
            return installDependenciesMojo.findPasswordForServer(repoId);
        }
    }

    private Map<String, String> getPrivateRepoCredentials(String repoId) {
        String username = findUsernameForServer(repoId);
        String password = findPasswordForServer(repoId);
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

    private Boolean IsUvIndexUsernameEnvironmentVariableSet(String repoId){
        String uvIndexUsernameEnvironmentVariable = getUvIndexUsernameEnvironmentVariable(repoId);
         return !StringUtils.isEmpty(System.getenv(uvIndexUsernameEnvironmentVariable));
    }

    private Boolean IsUvIndexPasswordEnvironmentVariableSet(String repoId){
        String uvIndexPasswordEnvironmentVariable = getUvIndexPasswordEnvironmentVariable(repoId);
        return !StringUtils.isEmpty(System.getenv(uvIndexPasswordEnvironmentVariable));
    }

    private Boolean executeCommandWithCredentials() {
        String repoId = getRepoId();
        if (!getRepoId().isEmpty()) {
            return (!IsUvIndexUsernameEnvironmentVariableSet(repoId)) && (!IsUvIndexPasswordEnvironmentVariableSet(repoId));
        }
        return false;
    }

    private void logAuthenticationInformation(String repoId)  {
        String uvIndexUsernameEnvironmentVariable = getUvIndexUsernameEnvironmentVariable(repoId);
        String uvIndexPasswordEnvironmentVariable = getUvIndexPasswordEnvironmentVariable(repoId);
        logger.info("Temporarily setting {} and {} for use in uv command.", uvIndexUsernameEnvironmentVariable, uvIndexPasswordEnvironmentVariable);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDependencyInstalled(String packageName) {
        // Required by CommandHelper Interface, but not needed here
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installDevelopmentDependency(String packageName) {
        // Required by CommandHelper Interface, but not needed here
    }
}
