package org.technologybrewery.habushu.util;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.PythonPackageAndDependencyManagerSetup;

/**
 * Common utility methods for handling TOML uv.
 */
public final class UvUtil {
    /**
     * Specifies the semver compliant requirement for the version of uv that
     * must be installed and available for Habushu to use.
     */
    public static final String UV_VERSION_REQUIREMENT = ">=0.5.0";

    public static String findCurrentVirtualEnvironmentFullPathForUv(String pythonVersion, String pythonPackageAndDependencyManager,
        boolean usePyenv, File patchInstallScript, File workingDirectory, boolean rewriteLocalPathDepsInArchives, Log log)
            throws MojoExecutionException { 
		String virtualEnvFullPath = null;
		PythonPackageAndDependencyManagerSetup configureTools = new PythonPackageAndDependencyManagerSetup(pythonVersion, pythonPackageAndDependencyManager, 
                usePyenv, patchInstallScript, workingDirectory, rewriteLocalPathDepsInArchives, log);
		configureTools.execute();

		try {
			; // todo
		} catch (RuntimeException e) {
			log.debug("Could not retrieve uv-managed virtual environment path - it likely does not exist",
					e);
		}

		return virtualEnvFullPath;
	}

}