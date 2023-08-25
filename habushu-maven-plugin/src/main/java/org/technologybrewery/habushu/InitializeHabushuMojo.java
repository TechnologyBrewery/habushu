package org.technologybrewery.habushu;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Ensures that the current project is a valid Poetry project and initializes
 * Habushu versioning conventions, specifically aligning the version specified
 * in the {@code pom.xml} with the version in the project's
 * {@code pyproject.toml}.
 */
@Mojo(name = "initialize-habushu", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeHabushuMojo extends AbstractHabushuMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Validating Poetry-based project structure...");
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        try {
            poetryHelper.execute(Arrays.asList("check"));
        } catch (HabushuException e) {
            getLog().debug("Failure encountered while running 'poetry check'!", e);
            getLog().warn("poetry check failed (debug contains more details) - this is likely due to a "
                    + "mismatch between your pyproject.toml and poetry.lock file - attempting to correct...");
            poetryHelper.execute(Arrays.asList("lock", "--no-update"));
            getLog().warn("Corrected - pyproject.toml and poetry.lock now synced");
        }

        String currentPythonPackageVersion = poetryHelper.execute(Arrays.asList("version", "-s"));
        String pomVersion = project.getVersion();
        String expectedPythonPackageVersion = getPythonPackageVersion(pomVersion, false, null);

        if (!StringUtils.equals(currentPythonPackageVersion, expectedPythonPackageVersion)) {
            if (overridePackageVersion) {
                getLog().info(String.format("Setting Poetry package version to %s", expectedPythonPackageVersion));
                getLog().info(
                        "If you do *not* want the Poetry package version to be automatically synced with the POM version, set <overridePackageVersion>false</overridePackageVersion> in the plugin's <configuration>");
                poetryHelper.executeAndLogOutput(Arrays.asList("version", expectedPythonPackageVersion));
            } else {
                getLog().debug(String.format(
                        "Poetry package version set to %s in pyproject.toml does not align with expected POM-derived version of %s",
                        currentPythonPackageVersion, expectedPythonPackageVersion));
            }

        }

    }

}
