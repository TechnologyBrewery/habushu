package org.technologybrewery.habushu;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.technologybrewery.habushu.util.DefaultPythonStrategy;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;
import org.technologybrewery.habushu.util.PoetryUtil;
import org.technologybrewery.habushu.util.UvUtil;


/**
 * Attaches to the {@link LifecyclePhase#VALIDATE} phase to ensure that the all
 * pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy {@link PoetryUtil#POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only
 * {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 * or 
 * <ul>
 * <li>uv (installed version must satisfy {@link UvUtil#UV_VERSION_REQUIREMENT})</li>
 * </ul>
 */
@Mojo(name = "validate-python-package-and-dependency-manager", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class ValidatePythonPackageAndDependencyManagerMojo extends AbstractHabushuMojo {

    /**
     * The desired version of Python to use.
     */
    @Parameter(property = "habushu.pythonVersion")
    protected String pythonVersion;

    /**
     * The default python version strategy.
     */
    @Parameter(defaultValue = "POM", property = "habushu.defaultPythonStrategy")
    protected String defaultPythonStrategy;

    /**
     * Should Habushu use pyenv to manage the utilized version of Python?
     */
    @Parameter(defaultValue = "true", property = "habushu.usePyenv")
    protected boolean usePyenv;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "${project.build.directory}/pyenv-patch-install-python-version.sh", readonly = true)
    private File patchInstallScript;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "latest", property = "habushu.poetryMonorepoDependencyPluginVersion")
    protected String poetryMonorepoDependencyPluginVersion;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        PackageManager packageManager = HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile());
        boolean isPythonVersionConfigurationSet = true;
        boolean isPythonVersionFilePresent = HabushuUtil.validatePythonVersionFile(getPythonProjectBaseDir());
        
        // If the defaultPythonStrategy is set to PYTHONVERSION, then we need to check if the .python-version file is present
        if (DefaultPythonStrategy.PYTHONVERSION.name().equalsIgnoreCase(defaultPythonStrategy) &&
            !isPythonVersionFilePresent) {
            throw new MojoExecutionException("defaultPythonStrategy is set to PYTHONVERSION, but no .python-version file was found in the project directory.");
        }
        
         // If pythonVersion was not given then update isPythonVersionConfigurationSet and set it to the default
        if (StringUtils.isEmpty(pythonVersion)) {
            isPythonVersionConfigurationSet = false;
            pythonVersion = HabushuUtil.PYTHON_DEFAULT_VERSION_REQUIREMENT;
        }

        PythonPackageAndDependencyManagerSetup pythonPackageAndDependencyManagerSetup =
                HabushuUtil.getPythonPackageAndDependencyManagerSetup(packageManager, pythonVersion, isPythonVersionConfigurationSet,
                        defaultPythonStrategy, getPythonProjectBaseDir(), rewriteLocalPathDepsInArchives, getLog(),
                        usePyenv, patchInstallScript, poetryMonorepoDependencyPluginVersion);

        pythonPackageAndDependencyManagerSetup.execute();

        configurePrivatePyPiRepositoryCredentials(pythonPackageAndDependencyManagerSetup);
        configurePrivateDevPyPiRepositoryCredentials(pythonPackageAndDependencyManagerSetup);
    }

    private void configurePrivateDevPyPiRepositoryCredentials(PythonPackageAndDependencyManagerSetup pythonPackageAndDependencyManagerSetup) {
        if (useDevRepository) {
            if (!TEST_PYPI_REPOSITORY_URL.equals(devRepositoryUrl)){
                String pypiDevRepoIdUsername = findUsernameForServer(devRepositoryId);
                String pypiDevRepoIdPassword = findPasswordForServer(devRepositoryId);
                pythonPackageAndDependencyManagerSetup.registerRepositoryToSupportAuthenticatedDependencyResolution(devRepositoryId,
                        pypiDevRepoIdUsername, pypiDevRepoIdPassword);
            } else {
                logSkipRationale(devRepositoryUrl);
            }
        }
    }

    private void configurePrivatePyPiRepositoryCredentials(PythonPackageAndDependencyManagerSetup pythonPackageAndDependencyManagerSetup) {
        if (StringUtils.isNotEmpty(pypiRepoUrl) && !"https://pypi.org".equals(pypiRepoUrl)) {
            String pypiRepoIdUsername = findUsernameForServer(pypiRepoId);
            String pypiRepoIdPassword = findPasswordForServer(pypiRepoId);
            pythonPackageAndDependencyManagerSetup.registerRepositoryToSupportAuthenticatedDependencyResolution(pypiRepoId, pypiRepoIdUsername,
                    pypiRepoIdPassword);
        } else {
            logSkipRationale(pypiRepoUrl);
        }
    }

    private void logSkipRationale(String repositoryUrl) {
        getLog().debug("Skipping configuration for pulling from public readable repo: " + repositoryUrl);
    }

}
