package org.technologybrewery.habushu.util;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.PythonPackageAndDependencyManagerSetup;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Common utility methods for handling TOML Poetry.
 */
public final class PoetryUtil {
    /**
     * Specifies the semver compliant requirement for the version of Poetry that
     * must be installed and available for Habushu to use.
     */
    public static final String POETRY_VERSION_REQUIREMENT = "^1.5.0";

    /**
     * Specifies the semver compliant requirement for the version of Poetry-core that
     * must be installed and available for Habushu to use.
     */
    public static final String POETRY_CORE_VERSION_REQUIREMENT = "^1.6.0";


    public static String findCurrentVirtualEnvironmentFullPathForPoetry(String pythonVersion, String pythonPackageAndDependencyManager,
        boolean usePyenv, File patchInstallScript, File workingDirectory, boolean rewriteLocalPathDepsInArchives, Log log)
            throws MojoExecutionException { 
		String virtualEnvFullPath = null;
		PythonPackageAndDependencyManagerSetup configureTools = new PythonPackageAndDependencyManagerSetup(pythonVersion, pythonPackageAndDependencyManager, 
                usePyenv, patchInstallScript, workingDirectory, rewriteLocalPathDepsInArchives, log);
		configureTools.execute();

		try {
			PoetryCommandHelper poetryHelper = new PoetryCommandHelper(workingDirectory);
			virtualEnvFullPath = poetryHelper.execute(Arrays.asList("env", "list", "--full-path"));
		} catch (RuntimeException e) {
			log.debug("Could not retrieve Poetry-managed virtual environment path - it likely does not exist",
					e);
		}

		return virtualEnvFullPath;
	}
}