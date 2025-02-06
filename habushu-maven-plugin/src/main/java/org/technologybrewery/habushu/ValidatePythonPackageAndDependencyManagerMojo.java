package org.technologybrewery.habushu;

import java.io.File;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PoetryUtil;

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
 * <li>uv (installed version must satisfy TODO: ADD CONSTANT HERE WHEN AVAILABLE)</li>
 * </ul>
 */
@Mojo(name = "validate-python-package-and-dependency-manager", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidatePythonPackageAndDependencyManagerMojo extends AbstractHabushuMojo {

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = HabushuUtil.PYTHON_DEFAULT_VERSION_REQUIREMENT, property = "habushu.pythonVersion")
    protected String pythonVersion;

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

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        //TODO: Failing since UV impl is no op. Remove once UV Impl is done.
        // Note this doesn't imply we will implement UV solely on this check, implementation will differ based on investigation on further ticket.
        // (whether we abstract out or just use simple check)
        if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == HabushuUtil.PackageManager.UV) {
            throw new NotImplementedException(" UV not implemented yet ");
        }

        HabushuUtil.PackageManager packageManager = HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile());
        AbstractPythonPackageAndDependencyManagerSetup configureTools = HabushuUtil.getPythonPackageAndDependencyManager(packageManager,
        pythonVersion, getPythonProjectBaseDir(), rewriteLocalPathDepsInArchives, getLog(), usePyenv, patchInstallScript);

        configureTools.execute();

        configurePrivatePyPiRepositoryCredentials(configureTools);
        configurePrivateDevPyPiRepositoryCredentials(configureTools);
    }

    private void configurePrivateDevPyPiRepositoryCredentials(AbstractPythonPackageAndDependencyManagerSetup configureTools) throws MojoExecutionException {
        if (useDevRepository) {
            if (!TEST_PYPI_REPOSITORY_URL.equals(devRepositoryUrl)){
                String pypiDevRepoIdUsername = findUsernameForServer(devRepositoryId);
                String pypiDevRepoIdPassword = findPasswordForServer(devRepositoryId);
                configureTools.registerRepositoryToSupportAuthenticatedDependencyResolution(devRepositoryId,
                        pypiDevRepoIdUsername, pypiDevRepoIdPassword);
            } else {
                logSkipRationale(devRepositoryUrl);
            }
        }
    }

    private void configurePrivatePyPiRepositoryCredentials(AbstractPythonPackageAndDependencyManagerSetup configureTools) throws MojoExecutionException {
        if (StringUtils.isNotEmpty(pypiRepoUrl) && !"https://pypi.org".equals(pypiRepoUrl)) {
            String pypiRepoIdUsername = findUsernameForServer(pypiRepoId);
            String pypiRepoIdPassword = findPasswordForServer(pypiRepoId);
            configureTools.registerRepositoryToSupportAuthenticatedDependencyResolution(pypiRepoId, pypiRepoIdUsername,
                    pypiRepoIdPassword);
        } else {
            logSkipRationale(pypiRepoUrl);
        }
    }

    private void logSkipRationale(String repositoryUrl) {
        getLog().debug("Skipping configuration for pulling from public readable repo: " + repositoryUrl);
    }

}
