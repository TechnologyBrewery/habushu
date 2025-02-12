package org.technologybrewery.habushu;

import java.io.File;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.util.HabushuUtil;

public class PythonPackageAndDependencyManagerFactory {
    public static AbstractPythonPackageAndDependencyManagerSetup createPythonPackageAndDependencyManagerSetup(
        String pythonVersion, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log, HabushuUtil.PackageManager pythonPackageAndDependencyManager,
        Boolean usePyenv, File patchInstallScript) throws MojoExecutionException {
        if (pythonPackageAndDependencyManager == HabushuUtil.PackageManager.POETRY){
            if ((usePyenv == null) || (patchInstallScript == null)) {
                throw new MojoExecutionException("PyenvAndPoetrySetup requires usePyenv and patchInstallScript.");
            }
            return new PyenvAndPoetrySetup(pythonVersion, baseDir, rewriteLocalPathDepsInArchives, log, usePyenv, patchInstallScript);
        } else {
            // TODO: Implement UV SETUP
            throw new NotImplementedException("uv not yet implemented (targeted for 3.0.0 release)");
        }
    }
}
