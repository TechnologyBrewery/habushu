package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvAuthenticationCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class InitializeHabushuUv extends AbstractInitializeHabushu{
    /**
     * Instance of InitializeHabushuMojo
     */
    protected InitializeHabushuMojo initializeHabushuMojo;

    public InitializeHabushuUv(File baseDir, Log log, boolean overridePackageVersion, String expectedPythonPackageVersion, InitializeHabushuMojo mojo) {
        super(baseDir, log, overridePackageVersion, expectedPythonPackageVersion);
        this.initializeHabushuMojo = mojo;
    }

    @Override
    public void doExecute() throws MojoExecutionException {
        log.info("Validating UV-based project structure...");
        UvAuthenticationCommandHelper uvAuthenticationHelper = createUvAuthenticationCommandHelper();
        try {
            uvAuthenticationHelper.executeLockCommand(true, false);

        } catch (HabushuException e) {
            log.debug("Failure  encountered while running 'uv lock --check'!", e);
            log.warn("uv lock --check failed (debug contains more details) - this is likely due to a "
                    + "mismatch between your pyproject.toml and uv.lock file or missing uv.lock file. - attempting to correct...");
            uvAuthenticationHelper.executeLockCommand(true, true);
            log.warn("Corrected - pyproject.toml and uv.lock now synced");
        }
        UvCommandHelper uvHelper = createUvCommandHelper();
        List<String> getPythonProjectVersion = Arrays.asList("--from=toml-cli", "toml", "get", "--toml-path=pyproject.toml", "project.version");
        List<String> getPythonProjectVersionCommamd = uvHelper.createToolRunCommand(getPythonProjectVersion);
        String currentPythonPackageVersion = uvHelper.execute(getPythonProjectVersionCommamd);
        if (!StringUtils.equals(currentPythonPackageVersion, expectedPythonPackageVersion)) {
            if (overridePackageVersion) {
                log.info(String.format("Setting uv package version to %s", expectedPythonPackageVersion));
                log.info(
                        "If you do *not* want the uv package version to be automatically synced with the POM version, set <overridePackageVersion>false</overridePackageVersion> in the plugin's <configuration>");
                
                List<String> setPythonProjectVersion = Arrays.asList("--from=toml-cli", "toml", "set", "--toml-path=pyproject.toml", "project.version", expectedPythonPackageVersion);
                List<String> setPythonProjectVersionCommamd = uvHelper.createToolRunCommand(setPythonProjectVersion);
                uvHelper.executeAndLogOutput(setPythonProjectVersionCommamd);
            } else {
                log.debug(String.format(
                        "uv package version set to %s in pyproject.toml does not align with expected POM-derived version of %s",
                        currentPythonPackageVersion, expectedPythonPackageVersion));
            }
        }

    }

    /**
     * Creates a {@link UvCommandHelper} that may be used to invoke uv
     * commands from the project's working directory.
     *
     * @return command helper
     */
    protected UvCommandHelper createUvCommandHelper() {
        return new UvCommandHelper(baseDir);
    }

    /**
     * Creates a {@link org.technologybrewery.habushu.exec.UvAuthenticationCommandHelper} that may be used to invoke uv
     * commands that could require authentication from the project's working directory.
     *
     * @return command helper
     */
    protected UvAuthenticationCommandHelper createUvAuthenticationCommandHelper() {
        return new UvAuthenticationCommandHelper(baseDir, initializeHabushuMojo, null);
    }
}
