package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PyenvCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PythonVersionHelper;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

/**
 * Attaches to the {@link LifecyclePhase#VALIDATE} phase to ensure that the all
 * pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy
 * {@link #POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only
 * {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 */
@Mojo(name = "validate-pyenv-and-poetry", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidatePyenvAndPoetryMojo extends AbstractHabushuMojo {

    /**
     * Specifies the semver compliant requirement for the version of Poetry that
     * must be installed and available for Habushu to use.
     */
    protected static final String POETRY_VERSION_REQUIREMENT = "~1.2.0";

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = "3.9.13", property = "habushu.pythonVersion")
    protected String pythonVersion;

    /**
     * Should Habushu use pyenv to manage the utilized version of Python?
     */
    @Parameter(defaultValue = "true", property = "habushu.usePyenv")
    protected boolean usePyenv;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "${project.build.directory}/pyenv-patch-install-python-version.sh", readonly = true)
    private File patchInstallScript;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

	List<String> missingRequiredToolMsgs = new ArrayList<>();
	String currentPythonVersion = "";

	if (usePyenv) {
	    PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();
	    getLog().info("Checking if pyenv is installed...");
	    if (!pyenvHelper.isPyenvInstalled()) {
		missingRequiredToolMsgs.add(
			"'pyenv' is not currently installed! Please install pyenv and try again. Visit https://github.com/pyenv/pyenv for more information.");
	    } else {
		currentPythonVersion = pyenvHelper.getCurrentPythonVersion();

		if (!pythonVersion.equals(currentPythonVersion)) {
		    pyenvHelper.updatePythonVersion(pythonVersion, patchInstallScript);
		    currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
		}
	    }
	} else {
	    PythonVersionHelper pythonVersionHelper = new PythonVersionHelper(getPoetryProjectBaseDir(), pythonVersion);
	    try {
		currentPythonVersion = pythonVersionHelper.getCurrentPythonVersion();
	    } catch (MojoExecutionException mojoExecutionException) {
		throw new MojoExecutionException(
			"Expected Python version " + pythonVersion + ", but it was not installed");
	    }
	}

	// If a version of python is installed, verify that it matches the desired
	// version
	if (StringUtils.isNotBlank(currentPythonVersion)) {
	    if (!currentPythonVersion.equals(pythonVersion)) {
		throw new MojoExecutionException(String.format("Expected Python version %s, but found version %s",
			pythonVersion, currentPythonVersion));
	    }
	    getLog().info(String.format("Using Python %s", currentPythonVersion));
	}

	getLog().info("Checking if Poetry is installed...");
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
	Pair<Boolean, String> poetryInstallStatusAndVersion = poetryHelper.getIsPoetryInstalledAndVersion();

	if (!poetryInstallStatusAndVersion.getLeft()) {
	    missingRequiredToolMsgs.add(
		    "'poetry' is not currently installed! Execute 'curl -sSL https://install.python-poetry.org | python -' to install or visit https://python-poetry.org/ for more information and installation options");
	} else {

	    Semver poetryVersionSemver = new Semver(poetryInstallStatusAndVersion.getRight(), SemverType.NPM);
	    if (!poetryVersionSemver.satisfies(POETRY_VERSION_REQUIREMENT)) {
		missingRequiredToolMsgs.add(String.format(
			"Poetry version %s was installed - Habushu requires that installed version of Poetry satisfies %s.  Please update Poetry by executing 'poetry self update' or visit https://python-poetry.org/docs/#installation for more information",
			poetryInstallStatusAndVersion.getRight(), POETRY_VERSION_REQUIREMENT));
	    }
	}

	if (!missingRequiredToolMsgs.isEmpty()) {
	    throw new MojoExecutionException(StringUtils.join(missingRequiredToolMsgs, System.lineSeparator()));

	}

	if (usePyenv) {
	    getLog().info("Configuring Poetry to use the pyenv-activated Python binary...");
	    poetryHelper.executeAndLogOutput(Arrays.asList("config", "--local", "virtualenvs.prefer-active-python", "true"));
	}
	
	if (this.rewriteLocalPathDepsInArchives) {
	    getLog().info("Checking for updates to poetry-monorepo-dependency-plugin...");
	    poetryHelper.installPoetryPlugin("poetry-monorepo-dependency-plugin");
	}
    }

}
