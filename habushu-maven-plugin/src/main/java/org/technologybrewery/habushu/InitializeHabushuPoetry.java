package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.File;
import java.util.Arrays;

public class InitializeHabushuPoetry extends AbstractInitializeHabushu{

    public InitializeHabushuPoetry( File baseDir, Log log, boolean overridePackageVersion, String expectedPythonPackageVersion)  {
        super(baseDir, log, overridePackageVersion, expectedPythonPackageVersion);
    }


    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        log.info("Validating Poetry-based project structure...");
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);
        try {
            poetryHelper.execute(Arrays.asList("check"));
        } catch (HabushuException e) {
            log.debug("Failure  encountered while running 'poetry check'!", e);
            log.warn("poetry check failed (debug contains more details) - this is likely due to a "
                    + "mismatch between your pyproject.toml and poetry.lock file - attempting to correct...");
            poetryHelper.execute(poetryHelper.createLockCommand());
            log.warn("Corrected - pyproject.toml and poetry.lock now synced");
        }

        String currentPythonPackageVersion = poetryHelper.getProjectVersion();

        if (!StringUtils.equals(currentPythonPackageVersion, expectedPythonPackageVersion)) {
            if (overridePackageVersion) {
                log.info(String.format("Setting Poetry package version to %s", expectedPythonPackageVersion));
                log.info(
                        "If you do *not* want the Poetry package version to be automatically synced with the POM version, set <overridePackageVersion>false</overridePackageVersion> in the plugin's <configuration>");
                poetryHelper.executeAndLogOutput(Arrays.asList("version", expectedPythonPackageVersion));
            } else {
                log.debug(String.format(
                        "Poetry package version set to %s in pyproject.toml does not align with expected POM-derived version of %s",
                        currentPythonPackageVersion, expectedPythonPackageVersion));
            }

        }
    }
}
