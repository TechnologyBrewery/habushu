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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * A plugin to help include Conda-based projects in Maven builds. This helps keep a single build command that can build
 * the entire system with common lifecycle needs like testings and packaging artifacts that are commonly skipped in
 * Python- and R-projects.
 */
@Mojo(name = "habushu-maven-plugin", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class HabushuMojo extends AbstractMojo {

    private static final Logger logger = LoggerFactory.getLogger(HabushuMojo.class);

    /**
     * The base directory for running all Conda commands. (Usually the directory that contains the environment yaml
     * file)
     */
    @Parameter(defaultValue = "${basedir}/target", property = "workingDirectory", required = false)
    protected File workingDirectory;

    /**
     * The base directory for installing conda.
     */
    @Parameter(property = "installDirectory", required = false)
    protected File installDirectory;

    /**
     * The conda configuration file (e.g., yaml file) for this module. Each module can have EXACTLY ONE conda
     * configuration file.
     */
    @Parameter(property = "condaConfigurationFile", required = true, defaultValue = "${project.basedir}/conda.yaml")
    protected File condaConfigurationFile;

    /**
     * The conda configuration file checksum storage file.
     */
    @Parameter(property = "condaConfigurationChecksumFile", required = true, defaultValue = "${project.basedir}/target/config-checksum.md5")
    protected File condaConfigurationChecksumFile;

    /**
     * Folder in which python source files are located.
     */
    @Parameter(property = "pythonSourceDirectory", required = true, defaultValue = "${project.basedir}/src/main/python")
    protected File pythonSourceDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createWorkingDirectoryIfNeeded();

        String condaVersionResponse = getCondaVersion();
        logger.info("Version: {}", condaVersionResponse);

        String condaConfigurationFileCanonicalPath = getCanonicalPathForFile(condaConfigurationFile);
        logger.info("Configuring module based on the Conda configuration at {}", condaConfigurationFileCanonicalPath);

        Yaml condaYaml = new Yaml();
        Map<String, Object> condaEnvironment;
        try (InputStream inputStream = new FileInputStream(condaConfigurationFile)) {
            condaEnvironment = condaYaml.load(inputStream);

        } catch (IOException e) {
            throw new HabushuException("Problem reading conda yaml file!", e);
        }

        if (!condaConfigurationFile.exists()) {
            throw new HabushuException("Specified configuration file '" + condaConfigurationFile + "' does not exist!");

        }
                
        String currentEnvironments = getCurrentEnvironments();
        logger.debug("Current Conda Environments: \n{}", currentEnvironments);

        String environmentName = (String) condaEnvironment.get("name");
        boolean exists = currentEnvironments.contains("\n" + environmentName + " ");

        handleCondaEnvironmentSetup(condaConfigurationFileCanonicalPath, environmentName, exists);
        
        // temp, just to demonstrate that a python file can be run in the Conda environment that was created.  Will be replaced
        // with a unit and integration phase of the lifecycle in subsequent tickets:
        // logger.info("test output: {}", runPythonTest(environmentName));

    }

    private void createWorkingDirectoryIfNeeded() {
        if (!workingDirectory.exists()) {
            logger.debug("Working directory did not exist - creating {}", getCanonicalPathForFile(workingDirectory));
            workingDirectory.mkdirs();

            if (!workingDirectory.exists()) {
                throw new HabushuException("Working directory STILL does not exist after trying to create it!");
            }

        }
    }

    private void handleCondaEnvironmentSetup(String condaConfigurationPath, String environmentName, boolean exists) {
        // TODO: if we have an update but no change in the Conda configuration, it would be nice to skip this step.
        // It's easy enough to do w/ a file hash, but we need to figure out where we want to store that hash since
        // target will be blown away by default during builds.  Likely we will override clean to exclude the hash file
        // along with adding a "force clean" option
        if (exists) {
            logger.info("Updating existing Conda Environment: {}", environmentName);

        } else {
            logger.info("Creating new Conda Environment: {}", environmentName);

        }

        String action = (exists) ? "update" : "create";
        String environmentCreateResponse = invokeCondaCommand("env " + action + " --file " + condaConfigurationPath);

        logger.debug(environmentCreateResponse);

    }

    private String getCurrentEnvironments() {
        return invokeCondaCommand("env list");
    }

    private String runPythonTest(String environmentName) {
        File pythonFile = new File(pythonSourceDirectory, "helloworld.py");
        return invokeCondaCommand("run -n " + environmentName + " python " + getCanonicalPathForFile(pythonFile));
    }

    private String getCondaVersion() {
        return invokeCondaCommand("-V");
    }

    private String getCanonicalPathForFile(File file) {
        try {
            return file.getCanonicalPath();

        } catch (IOException ioe) {
            throw new HabushuException("Could not create working directory!", ioe);
        }

    }
       
    private String invokeCondaCommand(String command) {
        // TODO: once we get the lifecycle working end to end, we'll circle back and make it work on Windows 10+ as well:
        List<String> commands = new ArrayList<>();
        commands.add("/bin/sh");
        commands.add("-c");
        commands.add("/usr/local/anaconda3/bin/conda " + command);
        CondaExecutor executor = new CondaExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
        return executor.executeAndGetResult(logger);
    }

}
