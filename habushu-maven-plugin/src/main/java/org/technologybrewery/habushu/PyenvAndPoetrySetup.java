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
    private File patchInstallScript;

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *

     * @param usePyenv              whether we are using pyenv to instance and activate python versions
     * @param patchInstallScript    patch install script path
     */
    public PyenvAndPoetrySetup(String pythonVersion, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log, boolean usePyenv, File patchInstallScript) throws MojoExecutionException {
        super(pythonVersion, baseDir, rewriteLocalPathDepsInArchives, log); 
        this.usePyenv = usePyenv;
        this.patchInstallScript = patchInstallScript;
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
            try {
                currentPythonVersion = pyenvHelper.getCurrentPythonVersion();
            } catch (Exception e) {
                log.info("Failed to find current python version. Attempting to install it now.");
            }
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

    @Override
    protected String configurePythonUsingPackageAndDependencyManager(List<String> missingRequiredToolMsgs) throws MojoExecutionException {
        String currentPythonVersion = configurePyenvOrStraightPython(usePyenv, missingRequiredToolMsgs, patchInstallScript);
        return currentPythonVersion;
    }

    @Override
    protected List<String> validatePackageAndDependencyManagerInstallationAndVersion(ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) 
        throws MojoExecutionException {
            missingRequiredToolMsgs = validatePoetryInstallationAndVersion(validationTracker, missingRequiredToolMsgs);
            return missingRequiredToolMsgs;
    }

    @Override
    protected void finalizePythonPackageAndDependencyManagerConfiguration() throws MojoExecutionException {
        checkForPoetryToml();
        configurePoetryToUsePyenv();
        installPoetryMonorepoDependencyPlugin();
    }

    @Override
    protected String pythonSourceMessage() throws MojoExecutionException {
        String sourceMessage = usePyenv ? "(managed by pyenv)" : "(managed by the operating system)";
        return sourceMessage;
    };

    @Override
    public void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password) throws MojoExecutionException {
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
        return currentPythonVersion;
    }

    public List<String> validatePoetryInstallationAndVersion(ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) throws MojoExecutionException {
        String alreadyValidatedVersion = validationTracker.getalreadyValidatedVersion();
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        if (!validationTracker.isalreadyValidatedInstallation()) {
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
                    alreadyValidatedVersion = poetryInstallStatusAndVersion.getRight();
                    validationTracker.setalreadyValidatedVersion(alreadyValidatedVersion);
                    validationTracker.setalreadyValidatedInstallation(true);
                }
            }
        } else {
            alreadyValidatedVersion += VALIDATED_IN_PRIOR_BUILD_PHASE;
        }

        log.info("Found Poetry " + alreadyValidatedVersion);

        return missingRequiredToolMsgs;
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
            poetryHelper.executeAndLogOutput(poetryHelper.createUsePyenvCommand());
        }
    }
  
    void installPoetryMonorepoDependencyPlugin() throws MojoExecutionException {
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
    protected PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(baseDir);
    }

    @Override
    public String findCurrentVirtualEnvironmentFullPath() throws MojoExecutionException {
        String virtualEnvFullPath = null;
        try {
            PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);
            virtualEnvFullPath = poetryHelper.execute(Arrays.asList("env", "list", "--full-path"));
        } catch (RuntimeException e) {
            log.debug("Could not retrieve Poetry-managed virtual environment path - it likely does not exist", e);
        }
        
        return virtualEnvFullPath;
    }


}
