package org.technologybrewery.habushu;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.util.PackageManager;

public class PythonPackageAndDependencyManagerFactory {

    private PythonPackageAndDependencyManagerFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static AbstractPythonPackageAndDependencyManagerSetup createPythonPackageAndDependencyManagerSetup(
        String pythonVersion, boolean isPythonVersionConfigurationSet, String defaultPythonStrategy, File baseDir,
        boolean rewriteLocalPathDepsInArchives, Log log, PackageManager pythonPackageAndDependencyManager,
        Boolean usePyenv, File patchInstallScript) throws MojoExecutionException {

        if (pythonPackageAndDependencyManager == PackageManager.POETRY){
            if ((usePyenv == null) || (patchInstallScript == null)) {
                throw new MojoExecutionException("PyenvAndPoetrySetup requires usePyenv and patchInstallScript.");
            }
            return new PyenvAndPoetrySetup(pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir,
                    rewriteLocalPathDepsInArchives, log, usePyenv, patchInstallScript);
        } else {
            return new UvSetup(pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir,
                    rewriteLocalPathDepsInArchives, log);
        }
    }
}
