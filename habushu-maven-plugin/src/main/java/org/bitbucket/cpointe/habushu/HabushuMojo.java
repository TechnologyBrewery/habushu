package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * A plugin to help include Venv-based projects in Maven builds. This helps keep
 * a single build command that can build the entire system with common lifecycle
 * needs like testings and packaging artifacts that are commonly skipped in
 * Python- and R-projects.
 */
@Mojo(name = "configure-environment", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class HabushuMojo extends AbstractHabushuMojo {

	private static final Logger logger = LoggerFactory.getLogger(HabushuMojo.class);

	/**
	 * Folder where the previous file hash of the venv dependency file is stored.
	 */
	@Parameter(property = "previousDependencyHashDirectory", required = true, defaultValue = "${project.build.directory}/build-accelerator/")
	protected File previousDependencyHashDirectory;

	/**
	 * The name of the .txt file storing the previous hash of the venv dependency
	 * file.
	 */
	@Parameter(property = "previousVenvDependencyFileHash", required = true, defaultValue = "previousDependencyFileHash.txt")
	protected String previousVenvDependencyFileHash;

	/**
	 * The configuration file for the instance of pip in the virtual environment.
	 */
	@Parameter(property = "pathToPipConfigFile", required = true, defaultValue = "${project.build.directory}/virtualenvs/${project.artifactId}/pip.conf")
	protected String pathToPipConfigFile;

	/**
	 * The URL pointing to the distribution management server (a private PyPi
	 * repository hosted in Nexus).
	 */
	@Parameter(defaultValue = "https://nexus.aws.cpointe-inc.com/repository/habushu-pypi-repo/", property = "repositoryUrl", required = true)
	protected String repositoryUrl;

	/**
	 * The ID of the distribution management server (a private PyPi repository
	 * hosted in Nexus).
	 */
	@Parameter(defaultValue = "nexus.aws.cpointe-inc.com", property = "repositoryId", required = true)
	protected String repositoryId;

	/**
	 * The generated shell script that uses pip-login to log in to the remote
	 * repository.
	 */
	@Parameter(defaultValue = "${project.build.directory}/pip-login.sh", property = "pipLoginScript", required = false)
	private File pipLoginScript;

	/**
	 * The generated shell script that installs Maven-unpackaged Python
	 * depdendencies.
	 */
	@Parameter(defaultValue = "${project.build.directory}/python-setup-install-local-dependencies.sh", property = "pythonSetupInstallScript", required = false)
	private File pythonSetupInstallScript;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		upgradeVirtualEnvironmentPip();
		installPipLogin();
		HabushuUtil.createFileAndGivePermissions(pipLoginScript);
		writeCommandsToPipLoginScript();
		HabushuUtil.runBashScript(pipLoginScript.getAbsolutePath(), constructParametersForPipLoginScript());

		installUnpackedPythonDependencies();
		
		boolean updateRequired = compareCurrentAndPreviousDependencyFileHashes();
		if (updateRequired) {
			logger.debug("Change detected in venv dependency file. Updating configuration.");
			
			installVenvDependencies();
			overwritePreviousDependencyHash();
		} else {
			logger.debug("No change detected in venv dependency file.");
		}
	}
	
	/**
	 * Upgrades the pip instance for the virtual environment to the latest version.
	 */
	private void upgradeVirtualEnvironmentPip() {
		logger.debug("Upgrading pip for virtual environment to latest version.");
		String pathToPip = pathToVirtualEnvironment + "/bin/pip";

		VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pathToPip + " install --upgrade pip");
		executor.executeAndGetResult(logger);
	}

	/**
	 * Compares the current and previous hashes of the venv dependency file.
	 * 
	 * @return true if the hashes/files are different, false if they're equal
	 */
	private boolean compareCurrentAndPreviousDependencyFileHashes() {
		createPreviousDependencyFileHashDirectoryIfNeeded();

		String currentDependencyFileHash = null;
		String previousDependencyFileHash = null;
		String previousDependencyFilePath = previousDependencyHashDirectory.getAbsolutePath() + "/"
				+ previousVenvDependencyFileHash;

		File previousDependencyHashFile = new File(previousDependencyFilePath);
		if (previousDependencyHashFile.exists()) {
			try {
				HashFunction hashAlgorithm = Hashing.sha256();

				currentDependencyFileHash = Files.asByteSource(previousDependencyHashFile).hash(hashAlgorithm)
						.toString();
				previousDependencyFileHash = Files.asByteSource(venvDependencyFile).hash(hashAlgorithm).toString();
			} catch (IOException e) {
				throw new HabushuException("Error when attempting to create file hashes for comparison!", e);
			}
		} else {
			logger.debug("No previous venv dependency file hash found. Update required.");
			return true;
		}

		return !StringUtils.equals(currentDependencyFileHash, previousDependencyFileHash);
	}

	/**
	 * Overwrite the previous hash of the venv dependency file, updating it to the
	 * current one to reflect any changes.
	 */
	private void overwritePreviousDependencyHash() {
		logger.debug("Overwriting previous file hash with current one.");

		File previousDependencyHash = new File(
				previousDependencyHashDirectory.getAbsolutePath() + "/" + previousVenvDependencyFileHash);
		File currentDependencyHash = new File(venvDependencyFile.getAbsolutePath());

		try {
			FileUtils.copyFile(currentDependencyHash, previousDependencyHash);
		} catch (IOException e) {
			throw new HabushuException("Error when trying to overwrite previous dependency file hash with current!", e);
		}
	}

	/**
	 * Installs pip-login, which will allow authentication from the command line for
	 * PyPi hosted repositories.
	 */
	private void installPipLogin() {
		logger.debug("Installing pip-login.");
		String pathToPip = pathToVirtualEnvironment + "/bin/pip";

		VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pathToPip + " install pip-login");
		executor.executeAndGetResult(logger);
	}

	/**
	 * Creates a bash script to run pip-login, targeted at the repositoryUrl. Will
	 * use the current Maven user's username and password for that repository.
	 */
	private void writeCommandsToPipLoginScript() {
		String pathToActivationScript = pathToVirtualEnvironment + "/bin/activate";

		StringBuilder commandList = new StringBuilder();
		commandList.append("#!/bin/bash" + "\n");
		commandList.append("source " + pathToActivationScript + "\n");
		commandList.append("cd " + venvDirectory + "\n");
		commandList.append("pip-login -u $1 -p $2 " + repositoryUrl + "simple/");

		HabushuUtil.writeLinesToFile(commandList.toString(), pipLoginScript.getAbsolutePath());
	}

	/**
	 * Creates the list of parameters for the pip-login shell script, which includes
	 * the current Maven user's username and password.
	 * 
	 * Also allows the current Maven user to change their settings file location if
	 * necessary.
	 * 
	 * @return parameters the parameters for pip-login
	 */
	private String[] constructParametersForPipLoginScript() {
		logger.debug("Constructing parameters for pip-login shell script.");

		if (settings != null) {
			HabushuUtil.setMavenSettings(settings);
		}

		String[] parameters = new String[2];
		parameters[0] = HabushuUtil.findUsernameForServer(repositoryId);
		parameters[1] = HabushuUtil.decryptServerPassword(repositoryId);

		if (StringUtils.isBlank(parameters[0]) || StringUtils.isBlank(parameters[1])) {
			throw new HabushuException(
					"Incorrectly configured credentials for PyPi hosted repository.  Please check your Maven settings.xml and build again.");
		}

		return parameters;
	}

	private void installUnpackedPythonDependencies() {
		HabushuUtil.createFileAndGivePermissions(pythonSetupInstallScript);
		writeCommandsToPythonSetupInstallScript();
		HabushuUtil.runBashScript(pythonSetupInstallScript.getAbsolutePath());
	}

	/**
	 * Creates a bash script to install Python dependencies that have been
	 * unpackaged by Maven.
	 */
	private void writeCommandsToPythonSetupInstallScript() {
		String pathToActivationScript = pathToVirtualEnvironment + "/bin/activate";
		List<File> dependencies = getDependencies();

		StringBuilder commandList = new StringBuilder();
		commandList.append("#!/bin/bash" + "\n");
		commandList.append("source " + pathToActivationScript + "\n");

		for (File dependency : dependencies) {
			File setupPyFile = new File(dependency, "setup.py");
			if (setupPyFile.exists()) {
				commandList.append("cd " + dependency.getAbsolutePath() + "\n");
				commandList.append(PYTHON_COMMAND + " setup.py install" + "\n");
			}
		}

		HabushuUtil.writeLinesToFile(commandList.toString(), pythonSetupInstallScript.getAbsolutePath());
	}

	private List<File> getDependencies() {
		List<File> dependencies = new ArrayList<>();

		File dependencyDirectory = new File(workingDirectory, "dependency");
		if (dependencyDirectory.exists()) {
			dependencies = Arrays.asList(dependencyDirectory.listFiles());
		}

		return dependencies;
	}

	private void installVenvDependencies() {
		String pathToPip = pathToVirtualEnvironment + "/bin/pip";
		VirtualEnvFileHelper venvFileHelper = new VirtualEnvFileHelper(venvDependencyFile);
		List<String> dependencies = venvFileHelper.readDependencyListFromFile();

		for (String dependency : dependencies) {
			logger.debug("Installing dependency listed in dependency file: {}", dependency);

			VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pathToPip + " install " + dependency);
			executor.executeAndGetResult(logger);
		}
	}

	/**
	 * Create the directory that will contain the previous hash of the venv
	 * dependency file.
	 */
	private void createPreviousDependencyFileHashDirectoryIfNeeded() {
		if (!previousDependencyHashDirectory.exists()) {
			logger.debug("Previous dependency file hash directory did not exist - creating {}",
					getCanonicalPathForFile(previousDependencyHashDirectory));
			previousDependencyHashDirectory.mkdirs();
		} else {
			logger.debug("Previous dependency file hash directory already exists.");
		}
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
}
