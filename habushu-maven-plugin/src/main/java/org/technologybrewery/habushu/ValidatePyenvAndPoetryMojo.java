package org.technologybrewery.habushu;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Attaches to the {@link LifecyclePhase#VALIDATE} phase to ensure that the all
 * pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy
 * {@link PyenvAndPoetrySetup#POETRY_VERSION_REQUIREMENT poetryVersion})</li>
 * <li>Required Poetry plugins (currently only
 * {@code poetry-monorepo-dependency-plugin})</li>
 * </ul>
 */
@Mojo(name = "validate-pyenv-and-poetry", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidatePyenvAndPoetryMojo extends AbstractHabushuMojo {

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = PyenvAndPoetrySetup.PYTHON_DEFAULT_VERSION_REQUIREMENT, property = "habushu.pythonVersion")
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

        PyenvAndPoetrySetup configureTools = new PyenvAndPoetrySetup(pythonVersion, usePyenv,
                patchInstallScript, getPoetryProjectBaseDir(), rewriteLocalPathDepsInArchives,
                getLog());

        configureTools.execute();

        configurePrivatePyPiRepositoryCredentials(configureTools);
        configurePrivateDevPyPiRepositoryCredentials(configureTools);

        configureTools.installPoetryMonorepoDependencyPlugin();
    }

    private void configurePrivateDevPyPiRepositoryCredentials(PyenvAndPoetrySetup configureTools) throws MojoExecutionException {
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

    private void configurePrivatePyPiRepositoryCredentials(PyenvAndPoetrySetup configureTools) throws MojoExecutionException {
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
