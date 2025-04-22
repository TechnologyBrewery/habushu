package org.technologybrewery.habushu;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.util.PackageManager;

public class PythonPackageAndDependencyManagerFactory {

    private PythonPackageAndDependencyManagerFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static PythonPackageAndDependencyManagerSetup createPythonPackageAndDependencyManagerSetup(
        String pythonVersion, boolean isPythonVersionConfigurationSet, String defaultPythonStrategy, File baseDir,
        boolean rewriteLocalPathDepsInArchives, Log log, PackageManager pythonPackageAndDependencyManager,
        Boolean usePyenv, File patchInstallScript, String poetryMonorepoDependencyPluginVersion) {

        if (pythonPackageAndDependencyManager == PackageManager.POETRY){
            if ((usePyenv == null) || (patchInstallScript == null)) {
                throw new HabushuException("PyenvAndPoetrySetup requires usePyenv and patchInstallScript.");
            }
            return new PyenvAndPoetrySetup(pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir,
                    rewriteLocalPathDepsInArchives, log, usePyenv, patchInstallScript, poetryMonorepoDependencyPluginVersion);
        } else {
            return new UvSetup(pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir,
                    rewriteLocalPathDepsInArchives, log);
        }
    }
}
