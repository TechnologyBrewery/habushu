package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations.
 */
public abstract class AbstractPythonPackageAndDependencyManagerSetup {

    public static final Logger logger = LoggerFactory.getLogger(AbstractPythonPackageAndDependencyManagerSetup.class);

    private static final ThreadLocal<ValidationTrackingStatus> validationStatusContainer = ThreadLocal.withInitial(ValidationTrackingStatus::new);
    public static final String VALIDATED_IN_PRIOR_BUILD_PHASE = " (validated in prior build phase)";

    /**
     * The desired version of Python to use.
     */
    protected String pythonVersion;

    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Indicates whether Habushu should leverage the
     * {@code poetry-monorepo-dependency-plugin} or the
     * {@code uv-monorepo-dependency-plugin} (<- todo) to rewrite any local path
     * dependencies (to other Poetry/uv projects) as versioned packaged dependencies in
     * generated wheel/sdist archives. If {@code true}, Habushu will replace
     * invocations of Poetry/uv's {@code build} and {@code publish} commands in the
     * {@link BuildDeploymentArtifactsMojo} and {@link PublishToPyPiRepoMojo} with
     * the extensions of those commands exposed by the
     * {@code poetry-monorepo-dependency-plugin}/{@code uv-monorepo-dependency-plugin}, which are
     * {@code build-rewrite-path-deps} and {@code publish-rewrite-path-deps}
     * respectively.
     * <p>
     * Typically, this flag will only be {@code true} when deploying/releasing
     * Habushu modules within a CI environment that are part of a monorepo project
     * structure in which multiple Poetry/uv projects depend on one another.
     */
    protected boolean rewriteLocalPathDepsInArchives;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param pythonVersion                  version of python to leverage
     * @param baseDir                        base directory from which to operate for this module
     * @param rewriteLocalPathDepsInArchives see member variable for details
     * @param log                            the logger to use for output
     */
    public AbstractPythonPackageAndDependencyManagerSetup(String pythonVersion, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log) {
        this.pythonVersion = pythonVersion;
        this.baseDir = baseDir;
        this.rewriteLocalPathDepsInArchives = rewriteLocalPathDepsInArchives;
        this.log = log;
    }

    public void execute() throws MojoExecutionException {
        List<String> missingRequiredToolMsgs = new ArrayList<>();
        String currentPythonVersion;

        ValidationTrackingStatus validationTracker = validationStatusContainer.get();
        String ActivePythonVersion = validationTracker.getActivePythonVersion();

        missingRequiredToolMsgs = validatePackageAndDependencyManagerInstallationAndVersion(validationTracker, missingRequiredToolMsgs);

        if (pythonVersion.equals(ActivePythonVersion)) {
            log.info("Using Python version: " + ActivePythonVersion + VALIDATED_IN_PRIOR_BUILD_PHASE);

        } else {
            currentPythonVersion = configurePythonUsingPackageAndDependencyManager(missingRequiredToolMsgs);

            // If a version of python is installed, verify that it matches the desired version
            validatePythonVersion(currentPythonVersion);

            validationTracker.setActivePythonVersion(currentPythonVersion);
        }

        if (!missingRequiredToolMsgs.isEmpty()) {
            throw new MojoExecutionException(StringUtils.join(System.lineSeparator(), missingRequiredToolMsgs, System.lineSeparator()));
        }

        finalizePythonPackageAndDependencyManagerConfiguration();

        // reset validationStatusContainer
        validationStatusContainer.remove();
    }

    protected abstract String configurePythonUsingPackageAndDependencyManager(List<String> missingRequiredToolMsgs)throws MojoExecutionException;

    protected abstract List<String> validatePackageAndDependencyManagerInstallationAndVersion(ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) 
        throws MojoExecutionException;

    protected abstract void finalizePythonPackageAndDependencyManagerConfiguration() throws MojoExecutionException;

    private void validatePythonVersion(String currentPythonVersion) throws MojoExecutionException {
        if (StringUtils.isNotBlank(currentPythonVersion)) {
            if (!currentPythonVersion.equals(pythonVersion)) {
                throw new MojoExecutionException(String.format("Expected Python version %s, but found version %s",
                        pythonVersion, currentPythonVersion));
            }

            String sourceMessage = pythonSourceMessage(); 

            log.info(String.format("Using Python %s %s", currentPythonVersion, sourceMessage));
        }
    }

    protected abstract String pythonSourceMessage() throws MojoExecutionException;

    public abstract void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password) throws MojoExecutionException;

    public abstract String findCurrentVirtualEnvironmentFullPath() throws MojoExecutionException;

}
