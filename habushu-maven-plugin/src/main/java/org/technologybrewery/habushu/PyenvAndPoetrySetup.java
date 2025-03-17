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
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subclass of PythonPackageAndDependencyManagerSetup. Ensures Poetry-related pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy {@link PoetryUtil#POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 */
public class PyenvAndPoetrySetup extends AbstractPythonPackageAndDependencyManagerSetup {
    /**
     * Should Habushu use pyenv to manage the utilized version of Python for Poetry projects?
     */
    protected boolean usePyenv;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    private final File patchInstallScript;

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *

     * @param usePyenv              whether we are using pyenv to instance and activate python versions
     * @param patchInstallScript    patch install script path
     */
    public PyenvAndPoetrySetup(String pythonVersion, boolean isPythonVersionConfigurationSet, String defaultPythonStrategy,
                               File baseDir, boolean rewriteLocalPathDepsInArchives, Log log, boolean usePyenv,
                               File patchInstallScript) {
        super(pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir, rewriteLocalPathDepsInArchives, log);
        this.usePyenv = usePyenv;
        this.patchInstallScript = patchInstallScript;
    }

    private List<String> validatePyenvInstallation(List<String> missingRequiredToolMsgs) {
        PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();
        log.debug("Checking if pyenv is installed...");
        if (!pyenvHelper.isPyenvInstalled()) {
            missingRequiredToolMsgs.add(
                    "'pyenv' is not currently installed! Please install pyenv and try again. Visit https://github.com/pyenv/pyenv for more information.");
        } else {
            log.debug("pyenv already installed");
        }
        return missingRequiredToolMsgs;
    }

    private String validateAndConfigurePythonViaPyenv(File patchInstallScript) throws MojoExecutionException {
        String currentPythonVersion = StringUtils.EMPTY;
        PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();
        try {
            currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
        } catch (Exception e) {
            log.info("Failed to find current python version.");
        }
        if (useCurrentPythonVersion(currentPythonVersion)) {
            pythonVersion = currentPythonVersion;
        }
        if (!pythonVersion.equals(currentPythonVersion)) {
            pyenvHelper.updatePythonVersion(pythonVersion, patchInstallScript);
            currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
        }
        return currentPythonVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String configurePythonUsingPackageAndDependencyManager() throws MojoExecutionException {
        String currentPythonVersion;
        if (usePyenv) {
            currentPythonVersion = validateAndConfigurePythonViaPyenv(patchInstallScript);
        } else {
            currentPythonVersion = validateAndConfigureStraightPython();
        }
        return currentPythonVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> validatePackageAndDependencyManagerInstallationAndVersion(
            ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) {
            missingRequiredToolMsgs = validatePoetryInstallationAndVersion(validationTracker, missingRequiredToolMsgs);
            if (usePyenv) {
                missingRequiredToolMsgs = validatePyenvInstallation(missingRequiredToolMsgs);
                missingRequiredToolMsgs = validatePyenvConfiguration(missingRequiredToolMsgs);
            }
            return missingRequiredToolMsgs;
    }

    private List<String> validatePyenvConfiguration(List<String> missingRequiredToolMsgs) {
        File shellConfigFile = HabushuUtil.getShellConfigFile();
        if (shellConfigFile == null) {
            missingRequiredToolMsgs.add("Could not determine shell configuration file. Pyenv might not be installed correctly. " +
            "Visit https://github.com/pyenv/pyenv?tab=readme-ov-file#b-set-up-your-shell-environment-for-pyenv to properly configure Pyenv for use with Habushu.");
        }

        if (shellConfigFile != null && !shellConfigFile.exists()) {
            missingRequiredToolMsgs.add("Configuration file, " + shellConfigFile + " not found. Pyenv might not be installed correctly. " +
            "Visit https://github.com/pyenv/pyenv?tab=readme-ov-file#b-set-up-your-shell-environment-for-pyenv to properly configure Pyenv for use with Habushu.");
        }
        String pathEnvironmentVariable = HabushuUtil.getEnvironmentVariable("PATH");
        String shimsPath = HabushuUtil.getHomeDirectory() + "/.pyenv/shims" ;
        if (!pathEnvironmentVariable.contains(shimsPath)) {
            missingRequiredToolMsgs.add("'pyenv' is installed, but not configured correctly. " +
            "Visit https://github.com/pyenv/pyenv?tab=readme-ov-file#b-set-up-your-shell-environment-for-pyenv to properly configure Pyenv for use with Habushu.");
        }
        return missingRequiredToolMsgs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizePythonPackageAndDependencyManagerConfiguration() {
        checkForPoetryToml();
        configurePoetryToUsePyenv();
        installPoetryMonorepoDependencyPlugin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String pythonSourceMessage() {
        return usePyenv ? "(managed by pyenv)" : "(managed by the operating system)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password) {
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
        if (useCurrentPythonVersion(currentPythonVersion)) {
            pythonVersion = currentPythonVersion;
        }
        return currentPythonVersion;
    }

    private List<String> validatePoetryInstallationAndVersion(ValidationTrackingStatus validationTracker,
                                                        List<String> missingRequiredToolMsgs) {
        String alreadyValidatedVersion = validationTracker.getAlreadyValidatedVersion();
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        if (!validationTracker.isAlreadyValidatedInstallation()) {
            log.debug("Checking if Poetry is installed...");
            Pair<Boolean, String> poetryInstallStatusAndVersion = poetryHelper.getIsPoetryInstalledAndVersion();

            if (Boolean.FALSE.equals(poetryInstallStatusAndVersion.getLeft())) {
                missingRequiredToolMsgs.add(
                        "'poetry' is not currently installed! Execute 'curl -sSL https://install.python-poetry.org | python -' to install or visit https://python-poetry.org/ for more information and installation options");
            } else {

                Semver poetryVersionSemver = new Semver(poetryInstallStatusAndVersion.getRight(), SemverType.NPM);
                if (!poetryVersionSemver.satisfies(PoetryUtil.POETRY_VERSION_REQUIREMENT)) {
                    missingRequiredToolMsgs.add(String.format(
                            "Poetry version %s was installed - Habushu requires that installed version of Poetry satisfies %s.  Please update Poetry by executing 'poetry self update' or visit https://python-poetry.org/docs/#installation for more information",
                            poetryInstallStatusAndVersion.getRight(), PoetryUtil.POETRY_VERSION_REQUIREMENT));
                } else {
                    alreadyValidatedVersion = poetryInstallStatusAndVersion.getRight();
                    validationTracker.setAlreadyValidatedVersion(alreadyValidatedVersion);
                    validationTracker.setAlreadyValidatedInstallation(true);
                }
            }
        } else {
            alreadyValidatedVersion += VALIDATED_IN_PRIOR_BUILD_PHASE;
        }

        log.info("Found Poetry " + alreadyValidatedVersion);

        return missingRequiredToolMsgs;
    }

    private void checkForPoetryToml() {
        // check for existing poetry.toml, warn that this file should be tracked in version control if one does not exist
        String poetryTomlPath = baseDir.getAbsolutePath() + "/poetry.toml";
        File poetryToml = new File(poetryTomlPath);
        if (!poetryToml.exists()) {
            log.warn("Did not find a poetry.toml within the current project. It is recommended to always include this file in version control to ensure consistent builds.");
        }
    }

    private void configurePoetryToUsePyenv() {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        if (usePyenv) {
            log.info("Configuring Poetry to use the pyenv-activated Python binary...");
            poetryHelper.executeAndLogOutput(poetryHelper.createUsePyenvCommand());
        }
    }
  
    private void installPoetryMonorepoDependencyPlugin() {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        log.info("Checking for updates to poetry-monorepo-dependency-plugin...");
        poetryHelper.installPoetryPlugin("poetry-monorepo-dependency-plugin@latest");
    }
    
    /**
     * Creates a {@link PoetryCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     *
     * @return command helper
     */
    private PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(baseDir);
    }

}
