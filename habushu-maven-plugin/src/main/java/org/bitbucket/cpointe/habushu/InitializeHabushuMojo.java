package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.Arrays;

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
 * setuptools/pip based configurations to Poetry's myproject.toml configuration.
 */
@Mojo(name = "initialize-habushu", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeHabushuMojo extends AbstractHabushuMojo {

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = "3.9.9", property = "pythonVersion", required = false)
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
    }

}
