package org.technologybrewery.habushu;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.PyenvCommandHelper;
import org.technologybrewery.habushu.exec.PythonVersionHelper;
import org.technologybrewery.habushu.util.PoetryUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy {@link PoetryUtil#POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 */
public class PyenvAndPoetrySetup {

    /**
     * Specifies the semver compliant requirement for the default version of Python that
     * must be installed and available for Habushu to use.
     */
    final String pythonDefaultVersionRequirement;

    public final String validatedInPriorBuildPhase;

    /**
     * The desired version of Python to use.
     */
    protected String pythonVersion;

    /**
     * Should Habushu use pyenv to manage the utilized version of Python?
     */
    protected boolean usePyenv;

    /**
     * Base directory from which to write poetry files.
     */
    protected File baseDir;

    /**
     * Indicates whether Habushu should leverage the
     * {@code poetry-monorepo-dependency-plugin} to rewrite any local path
     * dependencies (to other Poetry projects) as versioned packaged dependencies in
     * generated wheel/sdist archives. If {@code true}, Habushu will replace
     * invocations of Poetry's {@code build} and {@code publish} commands in the
     * {@link BuildDeploymentArtifactsMojo} and {@link PublishToPyPiRepoMojo} with
     * the extensions of those commands exposed by the
     * {@code poetry monorepo-dependency-plugin}, which are
     * {@code build-rewrite-path-deps} and {@code publish-rewrite-path-deps}
     * respectively.
     * <p>
     * Typically, this flag will only be {@code true} when deploying/releasing
     * Habushu modules within a CI environment that are part of a monorepo project
     * structure which multiple Poetry projects depend on one another.
     */
    protected boolean rewriteLocalPathDepsInArchives;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param pythonDefaultVersionRequirement   Specifies the semver compliant requirement for the default version of Python that
     * must be installed and available for Habushu to use.
     * @param validatedInPriorBuildPhase     string value indicating validation in prior build phase
     * @param pythonVersion                  version of python to leverage
     * @param usePyenv                       whether we are using pyenv to instance and activate python versions
     * @param baseDir                        base directory from which to operate for this module
     * @param rewriteLocalPathDepsInArchives see member variable for details
     * @param log                            the logger to use for output
     */
    public PyenvAndPoetrySetup(String pythonDefaultVersionRequirement, 
        String validatedInPriorBuildPhase, String pythonVersion, boolean usePyenv,
        File baseDir, boolean rewriteLocalPathDepsInArchives, Log log) {
        this.pythonDefaultVersionRequirement = pythonDefaultVersionRequirement;
        this.validatedInPriorBuildPhase = validatedInPriorBuildPhase;
        this.pythonVersion = pythonVersion;
        this.usePyenv = usePyenv;
        this.baseDir = baseDir;
        this.rewriteLocalPathDepsInArchives = rewriteLocalPathDepsInArchives;
        this.log = log;
    }

    public String configurePyenvOrStraightPython(boolean usePyenv, List<String> missingRequiredToolMsgs, File patchInstallScript) throws MojoExecutionException {
        String currentPythonVersion = "";
        if (usePyenv) {
            currentPythonVersion = validateAndConfigurePyenv(missingRequiredToolMsgs, currentPythonVersion, patchInstallScript);
        } else {
            currentPythonVersion = validateAndConfigureStraightPython();
        }
        return currentPythonVersion;
    } 

    private String validateAndConfigurePyenv(List<String> missingRequiredToolMsgs, String currentPythonVersion, File patchInstallScript) throws MojoExecutionException {
        PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();
        log.debug("Checking if pyenv is installed...");
        if (!pyenvHelper.isPyenvInstalled()) {
            missingRequiredToolMsgs.add(
                    "'pyenv' is not currently installed! Please install pyenv and try again. Visit https://github.com/pyenv/pyenv for more information.");
        } else {
            currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
            if (!pythonVersion.equals(currentPythonVersion)) {
                pyenvHelper.updatePythonVersion(pythonVersion, patchInstallScript);
                currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
            }

            // Check for misconfigured pyenv that looks right, but is actually not "taking" due to missing PATH setup:
            PythonVersionHelper pythonVersionHelper = new PythonVersionHelper(baseDir, pythonVersion);
            String postPyenvActivatedPythonVersion = pythonVersionHelper.getCurrentPythonVersion();
            if (!pythonVersion.equals(postPyenvActivatedPythonVersion)) {
                missingRequiredToolMsgs.add(String.format("Expected 'pyenv' to set Python to %s but instead found %s!",
                        pythonVersion, postPyenvActivatedPythonVersion));
                missingRequiredToolMsgs.add("'pyenv' is installed, but not configured correctly.  " +
                        "Ensure your PATH includes 'pyenv init -' expected content " +
                        "OR do not configure habushu to use 'pyenv' to manage the Python version!");
            }

            log.debug("pyenv already installed");
        }
        return currentPythonVersion;
    }

    /**
     * Creates a {@link PyenvCommandHelper} that may be used to invoke Pyenv
     * commands from the project's working directory.
     *
     * @return command helper
     */
    protected PyenvCommandHelper createPyenvCommandHelper() {
        return new PyenvCommandHelper(baseDir);
    }

    private String validateAndConfigureStraightPython() throws MojoExecutionException {
        String currentPythonVersion;
        PythonVersionHelper pythonVersionHelper = new PythonVersionHelper(baseDir, pythonVersion);
        try {
            currentPythonVersion = pythonVersionHelper.getCurrentPythonVersion();
        } catch (MojoExecutionException mojoExecutionException) {
            throw new MojoExecutionException(
                    "Expected Python version " + pythonVersion + ", but it was not installed");
        }
        return currentPythonVersion;
    }

    public List<String> validatePoetryInstallationAndVersion(ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) throws MojoExecutionException {
        String alreadyValidatedPoetryVersion = validationTracker.getAlreadyValidatedPoetryVersion();
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        if (!validationTracker.isAlreadyValidatedPoetryInstallation()) {
            log.debug("Checking if Poetry is installed...");
            Pair<Boolean, String> poetryInstallStatusAndVersion = poetryHelper.getIsPoetryInstalledAndVersion();

            if (!poetryInstallStatusAndVersion.getLeft()) {
                missingRequiredToolMsgs.add(
                        "'poetry' is not currently installed! Execute 'curl -sSL https://install.python-poetry.org | python -' to install or visit https://python-poetry.org/ for more information and installation options");
            } else {

                Semver poetryVersionSemver = new Semver(poetryInstallStatusAndVersion.getRight(), SemverType.NPM);
                if (!poetryVersionSemver.satisfies(PoetryUtil.POETRY_VERSION_REQUIREMENT)) {
                    missingRequiredToolMsgs.add(String.format(
                            "Poetry version %s was installed - Habushu requires that installed version of Poetry satisfies %s.  Please update Poetry by executing 'poetry self update' or visit https://python-poetry.org/docs/#installation for more information",
                            poetryInstallStatusAndVersion.getRight(), PoetryUtil.POETRY_VERSION_REQUIREMENT));
                } else {
                    alreadyValidatedPoetryVersion = poetryInstallStatusAndVersion.getRight();
                    validationTracker.setAlreadyValidatedPoetryVersion(alreadyValidatedPoetryVersion);
                    validationTracker.setAlreadyValidatedPoetryInstallation(true);
                }
            }
        } else {
            alreadyValidatedPoetryVersion += validatedInPriorBuildPhase;
        }

        log.info("Found Poetry " + alreadyValidatedPoetryVersion);

        return missingRequiredToolMsgs;
    }

    public void finalizePoetryConfiguration() throws MojoExecutionException {
        checkForPoetryToml();
        configurePoetryToUsePyenv();
        installPoetryMonorepoDependencyPlugin();
    }

    private void checkForPoetryToml() throws MojoExecutionException {
        // check for existing poetry.toml, warn that this file should be tracked in version control if one does not exist
        String poetryTomlPath = baseDir.getAbsolutePath() + "/poetry.toml";
        File poetryToml = new File(poetryTomlPath);
        if (!poetryToml.exists()) {
            log.warn("Did not find a poetry.toml within the current project. It is recommended to always include this file in version control to ensure consistent builds.");
        }
    }

    private void configurePoetryToUsePyenv() throws MojoExecutionException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        if (usePyenv) {
            log.info("Configuring Poetry to use the pyenv-activated Python binary...");
            poetryHelper.executeAndLogOutput(Arrays.asList("config", "--local", "virtualenvs.prefer-active-python", "true"));
        }
    }
  
    void installPoetryMonorepoDependencyPlugin() throws MojoExecutionException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        log.info("Checking for updates to poetry-monorepo-dependency-plugin...");
        poetryHelper.installPoetryPlugin("poetry-monorepo-dependency-plugin@latest");
    }

    public void registerRepositoryToSupportAuthenticatedDependencyResolutionForPoetry(String repoId, String username, String password) throws MojoExecutionException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            log.info(String.format("Did not find username and password for the server with <id> %s. Will use existing configuration.", repoId));
        } else {
            String configKey = String.format("http-basic.%s", repoId);
            log.info(String.format("Adding username and password configuration for %s", repoId));

            List<Pair<String, Boolean>> credentialConfigurationArgs = new ArrayList<>();
            credentialConfigurationArgs.add(new ImmutablePair<>("config", false));
            credentialConfigurationArgs.add(new ImmutablePair<>(configKey, false));
            credentialConfigurationArgs.add(new ImmutablePair<>(username, false));
            credentialConfigurationArgs.add(new ImmutablePair<>(password, true));

            poetryHelper.executeWithSensitiveArgsAndLogOutput(credentialConfigurationArgs);
        }
    }
    
    /**
     * Creates a {@link PoetryCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     *
     * @return command helper
     */
    protected PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(baseDir);
    }



}
