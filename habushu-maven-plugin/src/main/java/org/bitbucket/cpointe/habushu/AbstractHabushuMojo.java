package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Contains logic common across the various Habushu mojos.
 */
public abstract class AbstractHabushuMojo extends AbstractMojo {

    /**
     * Default name of the Conda configuration file to use.
     */
    static final String DEFAULT_CONDA_CONFIGURATION_FILE_NAME = "conda.yaml";
    
    /**
     * Default name of the directory in the output target folder to use to stage content for archiving.
     */
    static final String DEFAULT_STAGING_FOLDER = "staging";    

    /**
     * The path to conda that will be used for this build.
     */
    @Parameter(property = "condaInstallPath", required = false)
    protected File condaInstallPath;

    /**
     * The conda configuration file (e.g., yaml file) for this module. Each module can have EXACTLY ONE conda
     * configuration file.
     */
    @Parameter(property = "condaConfigurationFile", required = true, defaultValue = "${project.basedir}/"
            + DEFAULT_CONDA_CONFIGURATION_FILE_NAME)
    protected File condaConfigurationFile;

    /**
     * The base directory for running all Conda commands. (Usually the directory that contains the environment yaml
     * file)
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "workingDirectory", required = false)
    protected File workingDirectory;

    /**
     * Represents the set of environments within Conda.
     */
    protected String currentEnvironments;

    /**
     * Represents the environment's yaml file.
     */
    protected Map<String, Object> condaEnvironment;

    /**
     * Represents the environment we want to use for this build.
     */
    protected String environmentName;

    /**
     * Handles basic set up used across steps so that the current environments and environment name are available.
     * 
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createWorkingDirectoryIfNeeded();
        establishCondaInstallPath();

        Logger logger = getLogger();

        String condaVersionResponse = getCondaVersion();
        logger.debug("Version: {}", condaVersionResponse);

        String condaConfigurationFileMessage = "Configuring module based on the Conda configuration at {}";
        String condaConfigurationFileCanonicalPath = getCanonicalPathForFile(condaConfigurationFile);
        if (!condaConfigurationFileCanonicalPath.endsWith("/" + DEFAULT_CONDA_CONFIGURATION_FILE_NAME)) {
            logger.info(condaConfigurationFileMessage, condaConfigurationFileCanonicalPath);

        } else {
            logger.debug(condaConfigurationFileMessage, condaConfigurationFileCanonicalPath);

        }

        Yaml condaYaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(condaConfigurationFile)) {
            condaEnvironment = condaYaml.load(inputStream);

        } catch (IOException e) {
            throw new HabushuException("Problem reading conda yaml file!", e);
        }

        if (!condaConfigurationFile.exists()) {
            throw new HabushuException("Specified configuration file '" + condaConfigurationFile + "' does not exist!");

        }

        currentEnvironments = getCurrentEnvironments();
        logger.debug("Current Conda Environments: \n{}", currentEnvironments);

        environmentName = (String) condaEnvironment.get("name");

    }

    /**
     * Returns the logger for the concrete class to make it more clear how to control logging.
     */
    protected abstract Logger getLogger();

    /**
     * Gets the canonical path for a file without having to deal w/ checked exceptions.
     * 
     * @param file
     *            file for which to get the canonical format
     * @return canonical format
     */
    protected String getCanonicalPathForFile(File file) {
        try {
            return file.getCanonicalPath();

        } catch (IOException ioe) {
            throw new HabushuException("Could not access file: " + file.getName(), ioe);
        }

    }

    /**
     * Invokes a Conda command and returns the system output back as a String. This is useful when you want to parse the
     * response and do something with that information. This could include, but probably shouldn't, output for the user.
     * When the output is intended for the user, it is typically better to use
     * {@link invokeCondaCommandAndRedirectOutput}.
     * 
     * @param command
     *            command to invoke
     * @return system output
     */
    protected String invokeCondaCommand(String command) {
        CondaExecutor executor = createExecutor(command);
        return executor.executeAndGetResult(getLogger());
    }

    /**
     * Invoked a Conda command and return the exit code. System output is logged to the console as it happens while
     * system errors are queued up and logged upon exiting from the command.
     * 
     * @param command
     *            command to invoke
     * @return return code
     */
    protected int invokeCondaCommandAndRedirectOutput(String command) {
        CondaExecutor executor = createExecutor(command);
        return executor.executeAndRedirectOutput(getLogger());
    }

    private CondaExecutor createExecutor(String command) {
        List<String> commands = new ArrayList<>();
        commands.add("/bin/sh");
        commands.add("-c");
        commands.add(getCanonicalPathForFile(condaInstallPath) + " " + command);
        CondaExecutor executor = new CondaExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
        return executor;
    }

    private void establishCondaInstallPath() {
        String condaExe = System.getenv("CONDA_EXE");
        condaInstallPath = new File(condaExe);
        getLogger().debug("Using the following conda path for this plugin: {}", condaExe);

    }

    /**
     * Runs a command and return the exit code within a specific Conda environment.
     * 
     * @param environmentName
     *            environment to run within
     * @param command
     *            the command to invoke
     * @return return code
     */
    protected String runInCondaEnvironment(String environmentName, String command) {
        return invokeCondaCommand("run -n " + environmentName + " " + command);
    }

    /**
     * Runs a command and returns the system out back as a String within a specific Conda environment.
     * 
     * @param environmentName
     * @param command
     * @return
     */
    protected int runInCondaEnvironmentAndRedirectOutput(String environmentName, String command) {
        return invokeCondaCommandAndRedirectOutput("run -n " + environmentName + " " + command);
    }

    private void createWorkingDirectoryIfNeeded() {
        if (!workingDirectory.exists()) {
            getLogger().debug("Working directory did not exist - creating {}",
                    getCanonicalPathForFile(workingDirectory));
            workingDirectory.mkdirs();

            if (!workingDirectory.exists()) {
                throw new HabushuException("Working directory STILL does not exist after trying to create it!");
            }

        }
    }

    /**
     * Returns the set of environments on this machine within a single string.
     * 
     * @return current environments
     */
    private String getCurrentEnvironments() {
        // TODO: this can be slow when you have a lot of environments - might be better to parse the directory set in
        // Java and use that to determine the available environments
        return invokeCondaCommand("env list");
    }

    /**
     * Returns the current Conda version being used.
     * 
     * @return conda version
     */
    private String getCondaVersion() {
        return invokeCondaCommand("-V");
    }

}
