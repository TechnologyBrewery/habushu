package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PyenvCommandHelper;

/**
 * Initializes and configures the usage of the specified version of Python for
 * the execution of all downstream Python-based DevOps operations. Later
 * iterations will also attempt to migration Habushu 1.x projects from their
 * setuptools/pip based configurations to Poetry's pyproject.toml configuration.
 */
@Mojo(name = "initialize-habushu", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeHabushuMojo extends AbstractHabushuMojo {

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = "3.9.13", property = "habushu.pythonVersion")
    protected String pythonVersion;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "${project.build.directory}/pyenv-patch-install-python-version.sh", readonly = true)
    private File patchInstallScript;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();
	String currentPythonVersion = pyenvHelper.getCurrentPythonVersion();

	if (!pythonVersion.equals(currentPythonVersion)) {

	    pyenvHelper.updatePythonVersion(pythonVersion, patchInstallScript);

	    // sanity check that the version update was completed successfully:
	    currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
	    if (!pythonVersion.equals(currentPythonVersion)) {
		throw new MojoExecutionException(String.format("Expected Python version %s, but found version %s",
			pythonVersion, currentPythonVersion));
	    }
	}

	getLog().info(String.format("Using Python %s", currentPythonVersion));

	getLog().info("Validating Poetry-based project structure...");
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
	poetryHelper.execute(Arrays.asList("check"));

	String currentPythonPackageVersion = poetryHelper.execute(Arrays.asList("version", "-s"));
	String pomVersion = project.getVersion();
	String expectedPythonPackageVersion = getPythonPackageVersion(pomVersion, false, null);

	if (!StringUtils.equals(currentPythonPackageVersion, expectedPythonPackageVersion)) {
	    if (overridePackageVersion) {
		getLog().info(String.format("Setting Poetry package version to %s", expectedPythonPackageVersion));
		getLog().info(
			"If you do *not* want the Poetry package version to be automatically synced with the POM version, set <overridePackageVersion>false</overridePackageVersion> in the plugin's <configuration>");
		poetryHelper.executeAndLogOutput(Arrays.asList("version", expectedPythonPackageVersion));
	    } else {
		getLog().debug(String.format(
			"Poetry package version set to %s in pyproject.toml does not align with expected POM-derived version of %s",
			currentPythonPackageVersion, expectedPythonPackageVersion));
	    }

	}

    }

}
