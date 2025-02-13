package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UVCommandHelper;

import java.io.File;
import java.util.Arrays;

public class InitializeHabushuUV extends AbstractInitializeHabushu{

    public InitializeHabushuUV( File baseDir, Log log, boolean overridePackageVersion, String expectedPythonPackageVersion) {
        super(baseDir, log, overridePackageVersion, expectedPythonPackageVersion);
    }

    @Override
    public void doExecute() throws MojoExecutionException {
        log.info("Validating UV-based project structure...");
        UVCommandHelper uvHelper = new UVCommandHelper(baseDir);
        try {
            uvHelper.execute(Arrays.asList("lock", "--check"));
        } catch (HabushuException e) {
            log.debug("Failure  encountered while running 'uv lock --check'!", e);
            log.warn("uv lock --check failed (debug contains more details) - this is likely due to a "
                    + "mismatch between your pyproject.toml and uv.lock file or missing uv.lock file. - attempting to correct...");
            uvHelper.execute(uvHelper.createLockCommand());
            log.warn("Corrected - pyproject.toml and uv.lock now synced");
        }

        String currentPythonPackageVersion = uvHelper.executeUVTool(Arrays.asList("--from=toml-cli", "toml", "get", "--toml-path=pyproject.toml", "project.version"));
        if (!StringUtils.equals(currentPythonPackageVersion, expectedPythonPackageVersion)) {
            if (overridePackageVersion) {
                log.info(String.format("Setting UV package version to %s", expectedPythonPackageVersion));
                log.info(
                        "If you do *not* want the UV package version to be automatically synced with the POM version, set <overridePackageVersion>false</overridePackageVersion> in the plugin's <configuration>");
                uvHelper.executeUVToolAndLogOutput(Arrays.asList("--from=toml-cli", "toml", "get", "--toml-path=pyproject.toml", "project.version", expectedPythonPackageVersion));
            } else {
                log.debug(String.format(
                        "UV package version set to %s in pyproject.toml does not align with expected POM-derived version of %s",
                        currentPythonPackageVersion, expectedPythonPackageVersion));
            }
        }

    }
}
