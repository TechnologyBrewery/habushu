package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.util.PoetryUtil;
import org.technologybrewery.habushu.util.UvUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Common class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy {@link PoetryUtil#POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 * or 
 * <ul>
 * <li>uv (installed version must satisfy {@link UvUtil#UV_VERSION_REQUIREMENT})</li>
 * </ul>
 */
public class PythonPackageAndDependencyManagerSetup {

    /**
     * Specifies the semver compliant requirement for the default version of Python that
     * must be installed and available for Habushu to use.
     */
    static final String PYTHON_DEFAULT_VERSION_REQUIREMENT = "3.11.4";

    private static final ThreadLocal<ValidationTrackingStatus> validationStatusContainer = ThreadLocal.withInitial(ValidationTrackingStatus::new);
    public static final String VALIDATED_IN_PRIOR_BUILD_PHASE = " (validated in prior build phase)";


    /**
     * The desired version of Python to use.
     */
    protected String pythonVersion;

    /**
     * Indicates the desired Python package and dependency manager. Currently, the available options are Poetry or uv.
     */
    protected String pythonPackageAndDependencyManager;

    /**
     * Should Habushu use pyenv to manage the utilized version of Python for Poetry projects?
     */
    protected boolean usePyenv;

     /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    private File patchInstallScript;

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

    private PyenvAndPoetrySetup poetryConfigureTools;

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param pythonVersion                  version of python to leverage
     * @param pythonPackageAndDependencyManager python package and dependency manager to leverage for this module
     * @param usePyenv                       whether we are using pyenv to instance and activate python versions for Poetry projects
     * @param baseDir                        base directory from which to operate for this module
     * @param rewriteLocalPathDepsInArchives see member variable for details
     * @param log                            the logger to use for output
     * @param patchInstallScript             patch install script path for Poetry
     */
    public PythonPackageAndDependencyManagerSetup(String pythonVersion, String pythonPackageAndDependencyManager, 
            boolean usePyenv, File patchInstallScript, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log) {
        this.pythonVersion = pythonVersion;
        this.pythonPackageAndDependencyManager = pythonPackageAndDependencyManager;
        this.usePyenv = usePyenv;
        this.patchInstallScript = patchInstallScript;
        this.baseDir = baseDir;
        this.rewriteLocalPathDepsInArchives = rewriteLocalPathDepsInArchives;
        this.log = log;
        this.poetryConfigureTools = new PyenvAndPoetrySetup(PYTHON_DEFAULT_VERSION_REQUIREMENT, 
        VALIDATED_IN_PRIOR_BUILD_PHASE, pythonVersion, usePyenv,
            baseDir, rewriteLocalPathDepsInArchives, log);
    }

    public void execute() throws MojoExecutionException {
        List<String> missingRequiredToolMsgs = new ArrayList<>();
        String currentPythonVersion;

        ValidationTrackingStatus validationTracker = validationStatusContainer.get();
        String priorActivePythonVersionActivated = validationTracker.getPriorActivePythonVersionActivated();

        if (pythonVersion.equals(priorActivePythonVersionActivated)) {
            log.info("Using Python version: " + priorActivePythonVersionActivated + VALIDATED_IN_PRIOR_BUILD_PHASE);

        } else {
            if (pythonPackageAndDependencyManager.equals("poetry")) {
                currentPythonVersion = poetryConfigureTools.configurePyenvOrStraightPython(usePyenv, missingRequiredToolMsgs, patchInstallScript);
            } else {
                // todo: call corresponding uv functionality 
                currentPythonVersion = "";
            }

            // If a version of python is installed, verify that it matches the desired
            // version
            validatePythonVersion(currentPythonVersion);

            validationTracker.setPriorActivePythonVersionActivated(currentPythonVersion);
        }

        missingRequiredToolMsgs = poetryConfigureTools.validatePoetryInstallationAndVersion(validationTracker, missingRequiredToolMsgs);

        if (!missingRequiredToolMsgs.isEmpty()) {
            throw new MojoExecutionException(StringUtils.join(System.lineSeparator(), missingRequiredToolMsgs, System.lineSeparator()));
        }

        if (pythonPackageAndDependencyManager.equals("poetry")) {
            poetryConfigureTools.finalizePoetryConfiguration();
        } else {
            // todo: call corresponding uv functionality 
        }
    }

    private void validatePythonVersion(String currentPythonVersion) throws MojoExecutionException {
        if (StringUtils.isNotBlank(currentPythonVersion)) {
            if (!currentPythonVersion.equals(pythonVersion)) {
                throw new MojoExecutionException(String.format("Expected Python version %s, but found version %s",
                        pythonVersion, currentPythonVersion));
            }

            String sourceMessage;
            if (pythonPackageAndDependencyManager.equals("poetry")) {
                sourceMessage = usePyenv ? "(managed by pyenv)" : "(managed by the operating system)";
            } else {
                sourceMessage = "(managed by uv)";
            }

            log.info(String.format("Using Python %s %s", currentPythonVersion, sourceMessage));
        }
    }

    public void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password) throws MojoExecutionException {
        if (pythonPackageAndDependencyManager.equals("poetry")) {
            poetryConfigureTools.registerRepositoryToSupportAuthenticatedDependencyResolutionForPoetry(repoId, username, password);
        } else {
            // todo: call corresponding uv functionality 
        }
    }

}
