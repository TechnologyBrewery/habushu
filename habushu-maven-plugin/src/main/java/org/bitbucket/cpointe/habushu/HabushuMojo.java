package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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
import org.bitbucket.cpointe.habushu.util.HabushuUtil;
import org.bitbucket.cpointe.habushu.util.VenvExecutor;
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
	 * The URL pointing to the distribution management server (a private PyPi
	 * repository hosted in Nexus).
	 */
	@Parameter(property = "repositoryUrl", required = false)
	protected String repositoryUrl;

	/**
	 * The ID of the distribution management server (a private PyPi repository
	 * hosted in Nexus).
	 */
	@Parameter(property = "repositoryId", required = false)
	protected String repositoryId;

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

		boolean updateRequired = compareCurrentAndPreviousDependencyFileHashes();
		if (updateRequired) {
			logger.debug("Change detected in venv dependency file. Updating configuration.");

			installVenvDependencies();
			overwritePreviousDependencyHash();
		} else {
			logger.debug("No change detected in venv dependency file.");
		}

		installUnpackedPythonDependencies();
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
	 * Installs local, Maven-managed dependencies that have been copied from other
	 * modules.
	 */
	private void installUnpackedPythonDependencies() {
		logger.info("Installing local, unpacked Python dependencies.");

		HabushuUtil.createFileAndGivePermissions(pythonSetupInstallScript);
		writeCommandsToPythonSetupInstallScript();
		HabushuUtil.runBashScript(pythonSetupInstallScript.getAbsolutePath());
	}

	/**
	 * Creates a bash script to install Python dependencies that have been
	 * unpackaged by Maven.
	 */
	private void writeCommandsToPythonSetupInstallScript() {
		List<File> dependencies = getDependencies();

		StringBuilder commandList = new StringBuilder();
		commandList.append("#!/bin/bash" + "\n");
		commandList.append("source " + pathToActivationScript + "\n");

		for (File dependency : dependencies) {
			File setupPyFile = new File(dependency, "setup.py");
			if (setupPyFile.exists()) {
				commandList.append("cd " + dependency.getAbsolutePath() + "\n");
				for (String command : installCommands) {
					commandList.append(command + " ");
	            }
				commandList.append(" setup.py install" + "\n");
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

	/**
	 * Installs dependencies from the dependencies.txt file.
	 * 
	 * Options on this installation include: --prefer-binary: pip will prefer wheel
	 * files. --default-timeout=10: default timeout of ten seconds.
	 * --extra-index-url: if configured, pip will check the specified remote
	 * repository as part of its standard package search.
	 */
	private void installVenvDependencies() {
		logger.info("Installing virtual environment dependencies from pip; process may take a few minutes.");

		String pathToPip = pathToVirtualEnvironment + "/bin/pip";

		StringBuilder pipInstallCommand = new StringBuilder();
		pipInstallCommand.append(pathToPip);
		pipInstallCommand.append(" install --prefer-binary");
		pipInstallCommand.append(" --default-timeout=10");
		pipInstallCommand.append(" -r " + venvDependencyFile.getAbsolutePath());

		if (StringUtils.isNotBlank(repositoryId) && StringUtils.isNotBlank(repositoryUrl)) {
			String[] credentials = findMavenCredentialsForRemoteRepo();
			String urlEncodedUsername = null;
			String urlEncodedPassword = null;

			try {
				urlEncodedUsername = URLEncoder.encode(credentials[0], "UTF-8");
				urlEncodedPassword = URLEncoder.encode(credentials[1], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new HabushuException("Unable to encode Maven credentials as HTML.");
			}

			URL repository = null;
			try {
				repository = new URL(repositoryUrl);

				if (StringUtils.isBlank(repository.getAuthority()) || StringUtils.isBlank(repository.getFile())) {
					throw new MalformedURLException();
				}
			} catch (MalformedURLException e) {
				throw new HabushuException("repositoryUrl is malformed.  Please check your pom.xml.");
			}

			pipInstallCommand.append(" --extra-index-url=");
			pipInstallCommand.append(urlEncodedUsername + ":" + urlEncodedPassword);
			pipInstallCommand.append("@" + repository.getAuthority() + repository.getFile());
			logger.debug("Connecting to remote repository for package search.");
		} else if (StringUtils.isNotBlank(repositoryId) && StringUtils.isBlank(repositoryUrl)) {
			throw new HabushuException("repositoryUrl is missing.  Please check your pom.xml.");
		} else if (StringUtils.isBlank(repositoryId) && StringUtils.isNotBlank(repositoryUrl)) {
			throw new HabushuException("repositoryId is missing.  Please check your pom.xml.");
		} else {
			logger.debug("Not attempting any connection to a remote repository.");
		}

		VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pipInstallCommand.toString());
		executor.executeAndGetResult(logger);
	}

	/**
	 * Finds the current user's Maven credentials for the listed remote repository.
	 * 
	 * @return credentials the credentials for the listed remote repository
	 */
	private String[] findMavenCredentialsForRemoteRepo() {
		logger.debug("Finding credentials for PyPi hosted repository.");

		if (settings != null) {
			HabushuUtil.setMavenSettings(settings);
		}

		String[] credentials = new String[2];
		credentials[0] = HabushuUtil.findUsernameForServer(repositoryId);
		credentials[1] = HabushuUtil.decryptServerPassword(repositoryId);

		if (StringUtils.isBlank(credentials[0]) || StringUtils.isBlank(credentials[1])) {
			throw new HabushuException(
					"Incorrectly configured credentials for PyPi hosted repository.  Please check your Maven settings.xml and build again.");
		}

		return credentials;
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
