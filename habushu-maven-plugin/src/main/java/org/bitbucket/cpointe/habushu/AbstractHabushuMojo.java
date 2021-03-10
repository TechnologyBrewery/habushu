package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

/**
 * Contains logic common across the various Habushu mojos.
 */
public abstract class AbstractHabushuMojo extends AbstractMojo {
    
    /**
     * Default name of the directory in the output target folder to use to stage content for archiving.
     */
    static final String DEFAULT_STAGING_FOLDER = "staging";  
    
    /**
     * Default name of the directory in the output target folder to use to stage test content for running behave tests.
     */
    static final String DEFAULT_TEST_STAGING_FOLDER = "test-staging";
    
    /**
     * Default name of the file for dependencies for the virtual environment.
     */
    static final String VENV_DEPENDENCY_FILE_NAME = "dependencies.txt";
    
    /**
     * The default command used to summon Python.
     */
    static final String PYTHON_COMMAND = "python";

    /**
     * The base directory for running all Venv commands. Usually the target directory.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "workingDirectory", required = false)
    protected File workingDirectory;
    
    /**
     * The directory containing the virtual environments that have been created.  Located under the working directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/virtualenvs", property = "venvDirectory", required = false)
    protected File venvDirectory;

    /**
     * Represents the environment we want to use for this build.
     */
    @Parameter(defaultValue = "${project.artifactId}", property = "environmentName", required = false)
    protected String environmentName;
    
    /**
     * Represents the path to the environment we want to use for this build.
     */
    @Parameter(defaultValue = "${project.build.directory}/virtualenvs/${project.artifactId}", property = "pathToVirtualEnvironment", required = false)
    protected String pathToVirtualEnvironment;
    
    /**
     * The file containing the dependencies for the Python virtual environment.
     */
    @Parameter(property = "venvDependencyFile", required = true, defaultValue = "${project.basedir}/"
            + VENV_DEPENDENCY_FILE_NAME)
    protected File venvDependencyFile;

    /**
     * Handles basic set up used across steps so that the current environments and environment name are available.
     * 
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createWorkingDirectoryIfNeeded();
        createVirtualEnvironmentIfNeeded();
        
        String pathToActivationScript = pathToVirtualEnvironment + "/bin/activate";
        
        giveFullFilePermissions(pathToActivationScript);
        activateVirtualEnvironment(pathToActivationScript);
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
     * Invokes a Venv command and returns the system output back as a String. This is useful when you want to parse the
     * response and do something with that information. This could include, but probably shouldn't, output for the user.
     * When the output is intended for the user, it is typically better to use
     * {@link invokeVenvCommandAndRedirectOutput}.
     * 
     * @param command
     *            command to invoke
     * @return system output
     */
    protected String invokeVenvCommand(String command) {
        VenvExecutor executor = createExecutor(command);
        return executor.executeAndGetResult(getLogger());
    }
    
	/**
	 * Activates the Python virtual environment using the activation script located
	 * at the given file path.
	 * 
	 * @param pathToActivateScript
	 * @return system output
	 */
	protected String activateVirtualEnvironment(String pathToActivateScript) {
        VenvExecutor executor = createExecutorWithDirectory(workingDirectory, "source " + pathToActivateScript);
        return executor.executeAndGetResult(getLogger());
	}
    
	/**
	 * Creates the Python virtual environment at the specified path if needed.
	 * 
	 * @return system output
	 */
	protected void createVirtualEnvironmentIfNeeded() {
		File virtualEnvDirectory = new File(pathToVirtualEnvironment);
		if (virtualEnvDirectory.exists()) {
			if (getLogger().isDebugEnabled()) {
				getLogger().debug("Virtual environment already created at {}.", venvDirectory.getAbsolutePath());
			} else {
				getLogger().info("Virtual environment already created.");
			}
			
			return;
		}
		
		List<String> commands = new ArrayList<>();
		commands.add(PYTHON_COMMAND);
		commands.add("-m");
		commands.add("venv");
		commands.add(pathToVirtualEnvironment);
				
		VenvExecutor executor = new VenvExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
		executor.executeAndGetResult(getLogger());
	}
	
    /**
     * Invoked a Venv command and return the exit code. System output is logged to the console as it happens while
     * system errors are queued up and logged upon exiting from the command.
     * 
     * @param command
     *            command to invoke
     * @return return code
     */
    protected int invokeVenvCommandAndRedirectOutput(String command) {
        VenvExecutor executor = createExecutorWithDirectory(workingDirectory, command);
        return executor.executeAndRedirectOutput(getLogger());
    }

    /**
     * Invoked a Venv command and return the exit code. System output is logged to the console as it happens while
     * system errors are queued up and logged upon exiting from the command.
     * 
     * @param directoryForVenv the directory for the virtual environment
     * @param command
     *            command to invoke
     * @return return code
     */
    protected int invokeVenvCommandAndRedirectOutput(File directoryForVenv, String command) {
        VenvExecutor executor = createExecutorWithDirectory(directoryForVenv, command);
        return executor.executeAndRedirectOutput(getLogger());
    }

    /**
     * Invoked multiple Venv commands and return the exit code. System output is logged to the console as it happens while
     * system errors are queued up and logged upon exiting from the command.
     * 
     * @param directoryForVenv the directory for the virtual environment
     * @param commands
     *            commands to invoke
     * @return return code
     */
    protected int runMultipleVenvCommandsAndRedirectOutput(File directoryForVenv, List<String> commands) {
        VenvExecutor executor = new VenvExecutor(directoryForVenv, commands, Platform.guess(), new HashMap<>());
        return executor.executeAndRedirectOutput(getLogger());
    }


    private VenvExecutor createExecutor(String command) {
        VenvExecutor executor = createExecutorWithDirectory(workingDirectory, command);
        return executor;
    }

    /**
     * Creates and returns an executor for Venv tied to a specified directory.
     * @param directoryForVenv the directory for the virtual environment
     * @param command the command to invoke
     * @return Venv Executor
     */
    protected VenvExecutor createExecutorWithDirectory(File directoryForVenv, String command) {
        List<String> commands = new ArrayList<>();
        commands.add("/bin/bash");
        commands.add("-c");
        
        commands.add(command);
        VenvExecutor executor = new VenvExecutor(directoryForVenv, commands, Platform.guess(), new HashMap<>());
        return executor;
    }

    /**
     * Runs a command within a specific Venv environment and returns the integer return code.
     * 
     * @param directory
     * @param environmentName
     * @param command
     * @return return code
     */
    protected int runInVenvEnvironmentAndRedirectOutput(File directory, String environmentName, String command) {
        return invokeVenvCommandAndRedirectOutput(directory, environmentName + " " + command);
    }
    
    /**
     * Create the working directory and virtual environment container directories.
     */
    private void createWorkingDirectoryIfNeeded() {
        if (!workingDirectory.exists()) {
            getLogger().debug("Working directory did not exist - creating {}",
                    getCanonicalPathForFile(workingDirectory));
            workingDirectory.mkdirs();

            if (!workingDirectory.exists()) {
                throw new HabushuException("Working directory STILL does not exist after trying to create it!");
            }
        }
        
        if (!venvDirectory.exists()) {
            getLogger().debug("Virtual environment directory did not exist - creating {}",
                    getCanonicalPathForFile(venvDirectory));
            venvDirectory.mkdirs();

            if (!venvDirectory.exists()) {
                throw new HabushuException("Virtual environment directory STILL does not exist after trying to create it!");
            }
        }
    }
    
    /**
     * Gives full read, write, and execute permissions to a file.
     * Needed to run "source" to activate the Python environment.
     * 
     * @param filePath the path to the environment activation script
     */
    private void giveFullFilePermissions(String filePath) {
    	File file = new File(filePath);
    	
    	if (file.exists()) {
    		file.setExecutable(true, false);
        	file.setReadable(true, false);
        	file.setWritable(true, false);
    	}
    }
}
