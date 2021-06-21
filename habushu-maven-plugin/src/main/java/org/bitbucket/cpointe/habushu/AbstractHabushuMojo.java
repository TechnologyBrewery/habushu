package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.bitbucket.cpointe.habushu.util.HabushuUtil;
import org.bitbucket.cpointe.habushu.util.Platform;
import org.bitbucket.cpointe.habushu.util.PyenvUtil;
import org.bitbucket.cpointe.habushu.util.VenvExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains logic common across the various Habushu mojos.
 */
public abstract class AbstractHabushuMojo extends AbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(AbstractHabushuMojo.class);

	private static final Pattern pattern = Pattern.compile("3\\.7.*");

	/**
	 * Default name of the directory in the output target folder to use to stage
	 * content for archiving.
	 */
	static final String DEFAULT_STAGING_FOLDER = "staging";

	/**
	 * Default name of the directory in the output target folder to use to stage
	 * test content for running behave tests.
	 */
	static final String DEFAULT_TEST_STAGING_FOLDER = "test-staging";

	/**
	 * Default name of the file for dependencies for the virtual environment.
	 */
	static final String VENV_DEPENDENCY_FILE_NAME = "dependencies.txt";

	/**
	 * The command used to run python.
	 */
	static final String[] pythonCommands = new String[] { "pyenv", "exec", "python" };

	/**
	 * The version of python to pull down to your .habushu directory and use for
	 * running scripts
	 */
	@Parameter(defaultValue = "3.7.10", property = "pythonVersion", required = false)
	protected String pythonVersion;

	/**
	 * The working directory for the build results. Usually the target directory.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "workingDirectory", required = false)
	protected File workingDirectory;

	/**
	 * The directory containing the virtual environments that have been created.
	 * Located under the working directory.
	 */
	@Parameter(defaultValue = "${project.build.directory}/virtualenvs", property = "venvDirectory", required = false)
	protected File venvDirectory;

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
	 * The current Maven user's settings, pulled dynamically from their settings.xml
	 * file.
	 */
	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	protected Settings settings;

	/**
	 * Represents the path to the activation script for the virtual environment.
	 */
	@Parameter(defaultValue = "${project.build.directory}/virtualenvs/${project.artifactId}/bin/activate", property = "pathToActivationScript", required = false)
	protected String pathToActivationScript = pathToVirtualEnvironment + "/bin/activate";

	/**
	 * The generated shell script that updates the python version if a normal
	 * install fails file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/change-python-version.sh", property = "pythonVersionScript", required = false)
	private File changeVersionScript;

	/**
	 * Handles basic set up used across steps so that the current environments and
	 * environment name are available.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		PyenvUtil.checkPyEnvInstall(workingDirectory);
		checkPythonVersion();

		createWorkingDirectoryIfNeeded();
		createVirtualEnvironmentIfNeeded();

		HabushuUtil.giveFullFilePermissions(pathToActivationScript);
	}

	/**
	 * Returns the logger for the concrete class to make it more clear how to
	 * control logging.
	 */
	protected abstract Logger getLogger();

	/**
	 * Gets the canonical path for a file without having to deal w/ checked
	 * exceptions.
	 * 
	 * @param file file for which to get the canonical format
	 * @return canonical format
	 */
	protected String getCanonicalPathForFile(File file) {
		try {
			return file.getCanonicalPath();

		} catch (IOException ioe) {
			throw new HabushuException("Could not access file: " + file.getName(), ioe);
		}
	}

	protected void checkPythonVersion() {
		List<String> commands = new ArrayList<>();
		commands.addAll(Arrays.asList(pythonCommands));
		commands.add("--version");

		VenvExecutor executor = new VenvExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
		String foundPythonVersion = executor.executeAndGetResult(logger);

		// Remove the leading "Python "
		String foundPythonVersionShort = foundPythonVersion.substring(7, foundPythonVersion.length());

		if (!checkPythonVersion(foundPythonVersionShort)) {
			throw new HabushuException(
					"Currently, only Python versions of 3.7.X are supported.  You are running version " + foundPythonVersionShort
							+ ".");
		}

		if (!StringUtils.equals(pythonVersion, foundPythonVersionShort)) {
			getLogger().warn(
					"Specified python version does NOT match found pyenv version! Expected: {}, but found: {}! Attempting python version change",
					pythonVersion, foundPythonVersionShort);
			PyenvUtil.updatePythonVersion(pythonVersion, changeVersionScript, workingDirectory);
		}
	}

	public static boolean checkPythonVersion(String pythonVersion) {
		Matcher matcher = pattern.matcher(pythonVersion);
		return matcher.matches();
	}

	/**
	 * Creates the Python virtual environment at the specified path if needed.
	 * 
	 * @return system output
	 */
	private void createVirtualEnvironmentIfNeeded() {
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
		commands.addAll(Arrays.asList(pythonCommands));
		commands.add("-m");
		commands.add("venv");
		commands.add(pathToVirtualEnvironment);

		VenvExecutor executor = new VenvExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
		executor.executeAndGetResult(getLogger());
	}

	/**
	 * Invoked a Venv command and return the exit code. System output is logged to
	 * the console as it happens while system errors are queued up and logged upon
	 * exiting from the command.
	 * 
	 * @param directoryForVenv the directory for the virtual environment
	 * @param command          command to invoke
	 * @return return code
	 */
	protected int invokeVenvCommandAndRedirectOutput(File directoryForVenv, String command) {
		VenvExecutor executor = createExecutorWithDirectory(directoryForVenv, command);
		return executor.executeAndRedirectOutput(getLogger());
	}

	/**
	 * Invoked multiple Venv commands and return the exit code. System output is
	 * logged to the console as it happens while system errors are queued up and
	 * logged upon exiting from the command.
	 * 
	 * @param directoryForVenv the directory for the virtual environment
	 * @param commands         commands to invoke
	 * @return return code
	 */
	protected int runMultipleVenvCommandsAndRedirectOutput(File directoryForVenv, List<String> commands) {
		VenvExecutor executor = new VenvExecutor(directoryForVenv, commands, Platform.guess(), new HashMap<>());
		return executor.executeAndRedirectOutput(getLogger());
	}

	/**
	 * Creates and returns an executor for Venv tied to a specified directory.
	 * 
	 * @param directoryForVenv the directory for the virtual environment
	 * @param command          the command to invoke
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
				throw new HabushuException(
						"Virtual environment directory STILL does not exist after trying to create it!");
			}
		}
	}

}
