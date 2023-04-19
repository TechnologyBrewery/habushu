package org.technologybrewery.habushu.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facilitates the execution of pyenv commands related to managing, selecting,
 * and installation desired Python versions.
 */
public class PyenvCommandHelper {

    private static final String PYENV_COMMAND = "pyenv";
    private static final Logger logger = LoggerFactory.getLogger(PyenvCommandHelper.class);

    private File workingDirectory;

    public PyenvCommandHelper(File workingDirectory) {
	this.workingDirectory = workingDirectory;
    }

    /**
     * Returns a boolean value indicating whether pyenv is installed.
     */
    public boolean isPyenvInstalled() {
	try {
	    execute(Arrays.asList("--version"));
	} catch (Throwable e) {
	    return false;
	}
	return true;
    }

    /**
     * Retrieves the version of Python that is set for the configured working
     * directory.
     * 
     * @return
     */
    public String getCurrentPythonVersion() throws MojoExecutionException {
	return execute(Arrays.asList("version-name"));
    }

    /**
     * Updates Python processes launched at the configured working directory to use
     * the specified version of Python by executing the following steps:
     * <ol>
     * <li>Checks pyenv for the currently installed Python versions</li>
     * <li>If the specified version of Python isn't installed, attempt to install it
     * using "pyenv install {@literal <version>}"</li>
     * <li>If installing the target Python version using "pyenv install
     * {@literal <version>}" fails, try to install the target version via "pyenv
     * install --patch" through {@link #installPythonVersionViaPatch(String)}</li>
     * <li>Finally, set the locally used version of Python (relative to the
     * configured working directory) to use the target version</li>
     * </ol>
     * it.
     * 
     * @param targetVersion      desired version of Python to use
     * @param patchInstallScript if installing the specified Python version via
     *                           "pyenv install {@literal <version>}" fails, path to
     *                           the {@link File} that will be
     */
    public void updatePythonVersion(String targetVersion, File patchInstallScript) throws MojoExecutionException {
	List<String> installedPythonVersions = getInstalledPythonVersions();
	if (!installedPythonVersions.contains(targetVersion)) {
	    logger.info(
		    "Could not find Python version {} in following versions [{}] that are installed via pyenv. Installing version {} now...",
		    targetVersion, StringUtils.join(installedPythonVersions, ", "), targetVersion);
	    installPythonVersion(targetVersion, patchInstallScript);
	}

	execute(Arrays.asList("local", targetVersion));
    }

    /**
     * Retrieves a list of the locally installed versions of Python that are managed
     * by pyenv.
     * 
     * @return
     */
    private List<String> getInstalledPythonVersions() throws MojoExecutionException {
	String versionsResult = execute(Arrays.asList("versions", "--bare"));

	if (StringUtils.isNotEmpty(versionsResult)) {
	    return Arrays.asList(StringUtils.split(versionsResult));
	} else {
	    return Collections.emptyList();
	}
    }

    /**
     * Installs Python using "pyenv install {@literal <version>}". If this fails, we
     * try to patch what may be the issue.
     * 
     * @param targetVersion      desired Python version to install
     * @param patchInstallScript if the initially executed "pyenv install
     *                           {@literal <version>}" command fails, write a shell
     *                           script to this file which will attempt to patch
     *                           what may be the issue.
     */
    private void installPythonVersion(String targetVersion, File patchInstallScript) {
	try {
	    execute(Arrays.asList("install", targetVersion));

	} catch (Throwable t) {
	    logger.warn("Could not install Python {} via normal install, attempting install with patch...",
		    targetVersion);
	    installPythonVersionViaPatch(targetVersion, patchInstallScript);
	}
    }

    /**
     * Writes the necessary bash script commands to the given {@link File} to
     * install Python through pyenv with a patch that attempts to fix a compilation
     * error. After the bash script is written and its permissions appropriately
     * updated, the script will be executed.
     * 
     * @param pythonVersion      desired Python version to install
     * @param patchInstallScript target file to which the script will be written and
     *                           executed from
     */
    private void installPythonVersionViaPatch(String pythonVersion, File patchInstallScript) {
	HabushuUtil.createFileAndGivePermissions(patchInstallScript);

	StringBuilder commandList = new StringBuilder();
	commandList.append("#!/bin/bash" + "\n");
	commandList.append("pyenv install --patch ");
	commandList.append(pythonVersion);
	commandList.append(
		" < <(curl -sSL https://github.com/python/cpython/commit/8ea6353.patch\\?full_index\\=1) " + "\n");

	HabushuUtil.writeLinesToFile(commandList.toString(), patchInstallScript.getAbsolutePath());

	try {
	    HabushuUtil.runBashScript(patchInstallScript.getAbsolutePath());
	} catch (Throwable t) {
	    throw new HabushuException(String.format("Failed to install Python version %s with patch", pythonVersion),
		    t);
	}

    }

    /**
     * Executes a pyenv command with the given arguments, logs the executed command,
     * and returns the resultant process output as a string.
     * 
     * @param arguments
     * @return
     * @throws MojoExecutionException
     */
    protected String execute(List<String> arguments) throws MojoExecutionException {
	ProcessExecutor executor = createPyenvExecutor(arguments);
	if (logger.isInfoEnabled()) {
	    logger.info("Executing pyenv command: {} {}", PYENV_COMMAND, StringUtils.join(arguments, " "));
	}
	return executor.executeAndGetResult(logger);
    }

    protected ProcessExecutor createPyenvExecutor(List<String> arguments) {
	List<String> fullCommandArgs = new ArrayList<>();
	fullCommandArgs.add(PYENV_COMMAND);
	fullCommandArgs.addAll(arguments);
	return new ProcessExecutor(workingDirectory, fullCommandArgs, Platform.guess(), null);
    }
}
