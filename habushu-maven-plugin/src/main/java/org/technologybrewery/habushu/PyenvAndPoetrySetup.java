package org.technologybrewery.habushu;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.PyenvCommandHelper;
import org.technologybrewery.habushu.exec.PythonVersionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy {@link #POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 */
class PyenvAndPoetrySetup {

    /**
     * Specifies the semver compliant requirement for the default version of Python that
     * must be installed and available for Habushu to use.
     */
    static final String PYTHON_DEFAULT_VERSION_REQUIREMENT = "3.9.16";

    /**
     * Specifies the semver compliant requirement for the version of Poetry that
     * must be installed and available for Habushu to use.
     */
    static final String POETRY_VERSION_REQUIREMENT = "^1.2.0";

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
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    private File patchInstallScript;

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param pythonVersion                  version of python to leverage
     * @param usePyenv                       whether or not we are using pyenv to instance and activate python versions
     * @param patchInstallScript             patch install script path
     * @param baseDir                        base directory from which to operate for this module
     * @param rewriteLocalPathDepsInArchives see memeber variable for details
     * @param log                            the logger to use for output
     */
    public PyenvAndPoetrySetup(String pythonVersion, boolean usePyenv, File patchInstallScript,
                               File baseDir, boolean rewriteLocalPathDepsInArchives, Log log) {
        this.pythonVersion = pythonVersion;
        this.usePyenv = usePyenv;
        this.patchInstallScript = patchInstallScript;
        this.baseDir = baseDir;
        this.rewriteLocalPathDepsInArchives = rewriteLocalPathDepsInArchives;
        this.log = log;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> missingRequiredToolMsgs = new ArrayList<>();
        String currentPythonVersion = "";

        if (usePyenv) {
            currentPythonVersion = validatateAndConfigurePyenv(missingRequiredToolMsgs, currentPythonVersion);
        } else {
            currentPythonVersion = validateAndConfigureStraightPython();
        }

        // If a version of python is installed, verify that it matches the desired
        // version
        validatePythonVersion(currentPythonVersion);

        log.debug("Checking if Poetry is installed...");
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        Pair<Boolean, String> poetryInstallStatusAndVersion = poetryHelper.getIsPoetryInstalledAndVersion();

        if (!poetryInstallStatusAndVersion.getLeft()) {
            missingRequiredToolMsgs.add(
                    "'poetry' is not currently installed! Execute 'curl -sSL https://install.python-poetry.org | python -' to install or visit https://python-poetry.org/ for more information and installation options");
        } else {

            Semver poetryVersionSemver = new Semver(poetryInstallStatusAndVersion.getRight(), SemverType.NPM);
            if (!poetryVersionSemver.satisfies(POETRY_VERSION_REQUIREMENT)) {
                missingRequiredToolMsgs.add(String.format(
                        "Poetry version %s was installed - Habushu requires that installed version of Poetry satisfies %s.  Please update Poetry by executing 'poetry self update' or visit https://python-poetry.org/docs/#installation for more information",
                        poetryInstallStatusAndVersion.getRight(), POETRY_VERSION_REQUIREMENT));
            } else {
                log.info("Found Poetry " + poetryInstallStatusAndVersion.getRight());
            }
        }

        if (!missingRequiredToolMsgs.isEmpty()) {
            throw new MojoExecutionException(StringUtils.join(missingRequiredToolMsgs, System.lineSeparator()));

        }

        if (usePyenv) {
            log.info("Configuring Poetry to use the pyenv-activated Python binary...");
            poetryHelper.executeAndLogOutput(Arrays.asList("config", "--local", "virtualenvs.prefer-active-python", "true"));
        }

        if (rewriteLocalPathDepsInArchives) {
            log.info("Checking for updates to poetry-monorepo-dependency-plugin...");
            poetryHelper.installPoetryPlugin("poetry-monorepo-dependency-plugin");
        }
    }

    private void validatePythonVersion(String currentPythonVersion) throws MojoExecutionException {
        if (StringUtils.isNotBlank(currentPythonVersion)) {
            if (!currentPythonVersion.equals(pythonVersion)) {
                throw new MojoExecutionException(String.format("Expected Python version %s, but found version %s",
                        pythonVersion, currentPythonVersion));
            }

            String sourceMessage = usePyenv ? "(managed by pyenv)" : "(managed by the operating system)";
            log.info(String.format("Using Python %s %s", currentPythonVersion, sourceMessage));
        }
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

    private String validatateAndConfigurePyenv(List<String> missingRequiredToolMsgs, String currentPythonVersion) throws MojoExecutionException {
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

            log.debug("pyenv already installed");
        }
        return currentPythonVersion;
    }

    /**
     * Creates a {@link PyenvCommandHelper} that may be used to invoke Pyenv
     * commands from the project's working directory.
     *
     * @return
     */
    protected PyenvCommandHelper createPyenvCommandHelper() {
        return new PyenvCommandHelper(baseDir);
    }

    /**
     * Creates a {@link PoetryCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     *
     * @return
     */
    protected PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(baseDir);
    }

}
