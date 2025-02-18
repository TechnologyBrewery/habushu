package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class InitializeHabushuUv extends AbstractInitializeHabushu{

    public InitializeHabushuUv( File baseDir, Log log, boolean overridePackageVersion, String expectedPythonPackageVersion) {
        super(baseDir, log, overridePackageVersion, expectedPythonPackageVersion);
    }

    @Override
    public void doExecute() throws MojoExecutionException {
        log.info("Validating UV-based project structure...");
        UvCommandHelper uvHelper = createUvCommandHelper();
        try {
            List<String> createLockCheckCommand = uvHelper.createLockCommand(true, false);
            uvHelper.execute(createLockCheckCommand);
        } catch (HabushuException e) {
            log.debug("Failure  encountered while running 'uv lock --check'!", e);
            log.warn("uv lock --check failed (debug contains more details) - this is likely due to a "
                    + "mismatch between your pyproject.toml and uv.lock file or missing uv.lock file. - attempting to correct...");
            List<String> createLockCommand = uvHelper.createLockCommand(true, true);
            uvHelper.execute(createLockCommand);
            log.warn("Corrected - pyproject.toml and uv.lock now synced");
        }
        List<String> getPythonProjectVersion = Arrays.asList("--from=toml-cli", "toml", "get", "--toml-path=pyproject.toml", "project.version");
        List<String> createUvToolRunCommand = uvHelper.createToolRunCommand(getPythonProjectVersion);
        String currentPythonPackageVersion = uvHelper.execute(createUvToolRunCommand);
        if (!StringUtils.equals(currentPythonPackageVersion, expectedPythonPackageVersion)) {
            if (overridePackageVersion) {
                log.info(String.format("Setting uv package version to %s", expectedPythonPackageVersion));
                log.info(
                        "If you do *not* want the uv package version to be automatically synced with the POM version, set <overridePackageVersion>false</overridePackageVersion> in the plugin's <configuration>");
                
                List<String> setPythonProjectVersion = Arrays.asList("--from=toml-cli", "toml", "set", "--toml-path=pyproject.toml", "project.version");
                uvHelper.executeAndLogOutput(setPythonProjectVersion);
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
}
