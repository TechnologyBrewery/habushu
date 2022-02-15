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
	@Parameter(property = "previousDependencyDirectory", required = true, defaultValue = "${project.build.directory}/build-accelerator/")
	protected File previousDependencyDirectory;

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

    private File previousDependencyFile;
    private File previousSetupPyFile;
    private File sourceSetupPyFile;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        
        previousDependencyFile = new File(previousDependencyDirectory.getAbsolutePath() + File.separatorChar
                + VENV_DEPENDENCY_FILE_NAME);
        previousSetupPyFile = new File(previousDependencyDirectory.getAbsolutePath() + File.separatorChar + SETUP_PY_FILE_NAME);
        sourceSetupPyFile = new File(pythonSourceDirectory.getAbsolutePath() + File.separatorChar + SETUP_PY_FILE_NAME);
		    
        boolean updateRequired = haveDependencyFilesChanged();
        if (updateRequired) {
            logger.debug("Change detected in venv dependency file. Updating configuration.");
            upgradeVirtualEnvironmentPip();
            installVenvDependencies();
            installUnpackedPythonDependencies();
            overwritePriorDependencyFiles();
        } else {
            logger.info("Skipped python setup because no python environment changes detected. Can run with -Dhabushu.force.clean to force an update.");
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
			File setupPyFile = new File(dependency, SETUP_PY_FILE_NAME);
			if (setupPyFile.exists()) {
				commandList.append("cd " + dependency.getAbsolutePath() + "\n");
				for (String command : installCommands) {
					commandList.append(command + " ");
	            }
				commandList.append(" install ." + "\n");
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
	 * Compares the current and previous dependency files.
	 * 
	 * @return true if the hashes/files are different, false if they're equal
	 */
	private boolean haveDependencyFilesChanged() {
	    boolean hasSetupPyFileChanged = true;
	    boolean hasRequirementsTxtFileChanged = true;
		
        if (previousDependencyFile.exists() && previousSetupPyFile.exists()) {
            try {
                hasRequirementsTxtFileChanged = !FileUtils.contentEquals(previousDependencyFile, venvDependencyFile);
                hasSetupPyFileChanged = !FileUtils.contentEquals(previousSetupPyFile, sourceSetupPyFile);
            } catch (IOException e) {
                throw new HabushuException("Error when attempting to create file hashes for comparison!", e);
            }
		} 

		return hasSetupPyFileChanged || hasRequirementsTxtFileChanged;
	}

	/**
	 * Overwrite the previous hash of the venv dependency file, updating it to the
	 * current one to reflect any changes.
	 */
	private void overwritePriorDependencyFiles() {
		logger.debug("Saves a copy of the current requirements.txt file so we can see if it changes in a future build.");

		try {
			FileUtils.copyFile(venvDependencyFile, previousDependencyFile);
			FileUtils.copyFile(sourceSetupPyFile, previousSetupPyFile);
		} catch (IOException e) {
			throw new HabushuException("Error when trying to overwrite previous dependency file hash with current!", e);
		}
	}

	


	@Override
	protected Logger getLogger() {
		return logger;
	}
}
