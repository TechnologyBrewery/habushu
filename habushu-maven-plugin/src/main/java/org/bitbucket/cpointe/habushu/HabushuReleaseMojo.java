package org.bitbucket.cpointe.habushu;

import java.io.File;

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
 * This release-focused plugin allows for the packaging and deployment of
 * Python-based artifacts to a PyPi hosted server staged in Nexus.
 */
@Mojo(name = "package-and-release-python", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class HabushuReleaseMojo extends AbstractHabushuMojo {

	private static final Logger logger = LoggerFactory.getLogger(HabushuReleaseMojo.class);

	/**
	 * The ID of the distribution management server (a private PyPi repository
	 * hosted in Nexus).
	 */
	@Parameter(property = "repositoryId", required = false)
	protected String repositoryId;

	/**
	 * The URL pointing to the distribution management server (a private PyPi
	 * repository hosted in Nexus).
	 */
	@Parameter(property = "repositoryUrl", required = false)
	protected String repositoryUrl;

	/**
	 * The staging directory into which to generate the wheel file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/"
			+ AbstractHabushuMojo.DEFAULT_STAGING_FOLDER, required = true)
	private File stagingDirectory;

	/**
	 * The directory where the generated wheel file will be automatically placed.
	 */
	@Parameter(defaultValue = "${project.build.directory}/" + AbstractHabushuMojo.DEFAULT_STAGING_FOLDER
			+ "/dist", required = true)
	private File distDirectory;

	/**
	 * The generated shell script that packages the Python project into a wheel
	 * file.
	 */
	@Parameter(defaultValue = "${project.build.directory}/package-python-to-wheel.sh", property = "wheelPackagingScript", required = false)
	private File packageWheelScript;

	/**
	 * The generated shell script that uploads the wheel file to the remote
	 * repository.
	 */
	@Parameter(defaultValue = "${project.build.directory}/upload-wheel-to-remote-repository.sh", property = "uploadWheelScript", required = false)
	private File uploadWheelScript;

	/**
	 * Whether the build should release the created wheel file to the remote
	 * repository.
	 */
	@Parameter(property = "habushu.perform.release", required = true, defaultValue = "false")
	protected boolean habushuPerformRelease;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
	    checkPythonInstall();
		createRequirementsList();

		HabushuUtil.createFileAndGivePermissions(packageWheelScript);
		writeCommandsToPackageScript();
		HabushuUtil.runBashScript(packageWheelScript.getAbsolutePath());

		if (habushuPerformRelease) {
			checkRemoteRepositoryConfiguration();

			if (settings != null) {
				HabushuUtil.setMavenSettings(settings);
			}

			HabushuUtil.createFileAndGivePermissions(uploadWheelScript);
			writeCommandsToUploadScript();

			String parameter = HabushuUtil.decryptServerPassword(repositoryId);
			String[] parameters = new String[1];
			parameters[0] = parameter;
			HabushuUtil.runBashScript(uploadWheelScript.getAbsolutePath(), parameters, true);
		}
	}

	/**
	 * Use pip to create a listing of the exact dependencies and their versions
	 * installed on the current environment, to be used for easy collection by other
	 * environments seeking to install the same dependency list.
	 */
	private void createRequirementsList() {
		logger.debug("Creating list of requirements for virtual environment dependencies.");
		String pathToPip = pathToVirtualEnvironment + "/bin/pip";

		VenvExecutor executor = createExecutorWithDirectory(stagingDirectory, pathToPip + " freeze > requirements.txt");
		executor.executeAndRedirectOutput(logger);
	}

	/**
	 * Write a series of commands to the wheel packaging bash script.
	 */
	private void writeCommandsToPackageScript() {
		String pathToActivationScript = pathToVirtualEnvironment + "/bin/activate";

		StringBuilder commandList = new StringBuilder();
		commandList.append("#!/bin/bash" + "\n");
		commandList.append("source " + pathToActivationScript + "\n");
		commandList.append("cd " + stagingDirectory + "\n");
		commandList.append("python setup.py bdist_wheel --universal");

		HabushuUtil.writeLinesToFile(commandList.toString(), packageWheelScript.getAbsolutePath());
	}

	/**
	 * Write a series of commands to the wheel upload bash script.
	 */
	private void writeCommandsToUploadScript() {
		String pathToActivationScript = pathToVirtualEnvironment + "/bin/activate";
		String artifactName = findGeneratedArtifactName();
		String username = HabushuUtil.findUsernameForServer(repositoryId);

		StringBuilder commandList = new StringBuilder();
		commandList.append("#!/bin/bash" + "\n");
		commandList.append("source " + pathToActivationScript + "\n");
		commandList.append("cd " + distDirectory + "\n");
		commandList.append("twine upload --repository-url " + repositoryUrl);
		commandList.append(" " + artifactName);
		commandList.append(" --username " + username);
		commandList.append(" --password $1");

		HabushuUtil.writeLinesToFile(commandList.toString(), uploadWheelScript.getAbsolutePath());
	}

	/**
	 * Ensures that the remote repository URL, ID, and backing Maven credentials are
	 * all non-null.  All of these items are required for a release to occur.
	 */
	private void checkRemoteRepositoryConfiguration() {
		if (StringUtils.isBlank(repositoryId)) {
			throw new HabushuException("repositoryId is missing.  Please check your pom.xml.");
		}

		if (StringUtils.isBlank(repositoryUrl)) {
			throw new HabushuException("repositoryUrl is missing.  Please check your pom.xml.");
		}

		if (StringUtils.isBlank(HabushuUtil.findUsernameForServer(repositoryId))) {
			throw new HabushuException("Maven username for server is missing.  Please check your settings.xml.");
		}

		if (StringUtils.isBlank(HabushuUtil.decryptServerPassword(repositoryId))) {
			throw new HabushuException("Maven password for server is missing.  Please check your settings.xml.");
		}
	}

	/**
	 * Find the name of the wheel file generated. Searches the distribution
	 * directory and returns the name of the most recently-modified file, which will
	 * be the created .whl file.
	 * 
	 * @return the name of the wheel file
	 */
	private String findGeneratedArtifactName() {
		String lsCommand = "ls -Art | tail -n 1";

		VenvExecutor executor = createExecutorWithDirectory(distDirectory, lsCommand);
		return executor.executeAndGetResult(logger);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
}
