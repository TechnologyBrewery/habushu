package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;

import java.util.List;

/**
 * Installs dependencies defined in the project's pyproject.toml configuration,
 *  If a private PyPi repository is defined via
 * {@link AbstractHabushuMojo#pypiRepoUrl} (and
 * {@link AbstractHabushuMojo#pypiRepoId}), it will be automatically added to
 * the module's pyproject.toml configuration as a supplemental source of
 * dependencies, if it is not already configured in the pyproject.toml
 */
@Mojo(name = "install-dependencies", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class InstallDependenciesMojo extends AbstractHabushuMojo {

    /**
     * Configures whether a private PyPi repository, if specified via
     * {@link AbstractHabushuMojo#pypiRepoUrl}, is automatically added as a package
     * source from which dependencies may be installed. This value is <b>*only*</b>
     * utilized if a private PyPi repository is specified via
     * {@link AbstractHabushuMojo#pypiRepoUrl}.
     */
    @Parameter(defaultValue = "true", property = "habushu.addPypiRepoAsPackageSources")
    protected boolean addPypiRepoAsPackageSources;

    /**
     * Determines if pypiSimpleSuffix is enabled or not.
     * Setting enablePypiSimpleSuffix to false will set pypiSimpleSuffix to empty value regardless
     * of pypiSimpleSuffix property value.
     * Setting enablePypiSimpleSuffix to true will honor pypiSimpleSuffix property value.
     */
    @Parameter(property = "habushu.enablePypiSimpleSuffix", defaultValue = "true")
    protected boolean enablePypiSimpleSuffix;

    /**
     * Configures the path for the simple index on a private pypi repository.
     * Certain private repository solutions (ie: devpi) use different names for the
     * simple index. devpi, for instance, uses "+simple".
     */
    @Parameter(property = "habushu.pypiSimpleSuffix", defaultValue = "simple")
    protected String pypiSimpleSuffix;

    /**
     * Configures whether the lock file will be updated before install.
     */
    @Parameter(defaultValue = "false", property = "habushu.skipLockUpdate")
    protected boolean skipLockUpdate;

    /**
     * Specifies groups to include in the installation.
     */
    @Parameter(property = "habushu.withGroups")
    protected String[] withGroups;

    /**
     * Specifies groups to exclude from the installation.
     */
    @Parameter(property = "habushu.withoutGroups")
    protected String[] withoutGroups;

    /**
     * Configuration option to include the --sync option on poetry install
     */
    @Parameter(defaultValue = "false", property = "habushu.forceSync")
    protected boolean forceSync;

    /**
     * The set of managed dependencies to monitor for conformance.  These can result in:
     * * direct changes to your pyproject.toml file (default behavior)
     * * log statements warning of mismatches (if habushu.updateManagedDependenciesWhenFound = false)
     * * stopping the build for manual intervention (if habushu.failOnManagedDependenciesMismatches = true)
     */
    @Parameter(property = "habushu.managedDependencies")
    protected List<PackageDefinition> managedDependencies;

    /**
     * Determines if managed dependency mismatches are automatically updated when encountered.
     */
    @Parameter(defaultValue = "true", property = "habushu.updateManagedDependenciesWhenFound")
    protected boolean updateManagedDependenciesWhenFound;

    /**
     * Determines if the build should be failed when managed dependency mismatches are found.
     */
    @Parameter(defaultValue = "false", property = "habushu.failOnManagedDependenciesMismatches")
    protected boolean failOnManagedDependenciesMismatches;

    /**
     * Whether to configure Poetry's {@code virtualenvs.in-project} value for this project.
     * If configured, virtual environments will be migrated to this approach during the clean phase of the build.
     *
     * While generally easier to find and use for tasks like debugging, having your virtual environment co-located in
     * your project may be less useful for executions like CI builds where you may want to centrally caches virtual
     * environments from a central location.
     */
    @Parameter(defaultValue = "true", property = "habushu.useInProjectVirtualEnvironment")
    protected boolean useInProjectVirtualEnvironment;


    /**
     * Allows tailoring of the path used for pushing to a PyPI repository for deployment.  Some repositories, like
     * Nexus or Artifactory, do not require an url path on top of the base repository url.  Others, do (often using
     * "legacy/").  This variable allows customization in a manner that does not impact the installation API for the
     * same repository.  Defaults to empty as the most common scenario when overriding the repository URL is to leverage
     * one of the repositories mentioned above.
     * Note: The property must be set equal to "" in order for Maven to not set the default value to null
     */
    @Parameter(property = "habushu.pypiUploadSuffix", defaultValue = "")
    protected String pypiUploadSuffix = "";

    /**
     * Determines whether we enable devRepositoryUrlUploadSuffix.
     * Setting enableDevRepositoryUrlUploadSuffix to false will set devRepositoryUrlUploadSuffix to empty value regardless
     * of devRepositoryUrlUploadSuffix property value.
     * Setting enableDevRepositoryUrlUploadSuffix to true will honor devRepositoryUrlUploadSuffix property values.
     */
    @Parameter(property = "habushu.enableDevRepositoryUrlUploadSuffix", defaultValue = "true")
    protected boolean enableDevRepositoryUrlUploadSuffix;

    /**
     * {{@link #pypiUploadSuffix repositoryUploadSuffix} contains critical information.  The main difference is that
     * this dev repository url path defaults to "legacy/" as the most common scenario when overriding the dev
     * repository URL is to leverage test.pypi.org, which needs this configuration.
     */
    @Parameter(property = "habushu.devRepositoryUrlUploadSuffix", defaultValue = "legacy/")
    protected String devRepositoryUrlUploadSuffix;

    /**
     * Get whether a private PyPi repository, is automatically added as a package source
     * @return addPypiRepoAsPackageSources
     */
    public boolean addPypiRepoAsPackageSources() {
        return addPypiRepoAsPackageSources;
    }

    /**
     * Whether to enable path for the simple index on a pypi repository.
     * @return enablePypiSimpleSuffix
     */
    public boolean enablePypiSimpleSuffix() {
        return enablePypiSimpleSuffix;
    }

    /**
     * Get configuration for the path for the simple index on a private pypi repository.
     * @return pypiSimpleSuffix
     */
    public String getPypiSimpleSuffix() {
        return pypiSimpleSuffix;
    }

    /**
     * Get configuration for the path for the simple index on a private pypi repository.
     * @return pypiSimpleSuffix
     */
    public boolean skipLockUpdate() {
        return skipLockUpdate;
    }

    /**
     * Specifies groups to include in the installation.
     * @return withGroups
     */
    public String[] getWithGroups() {
        return withGroups;
    }

    /**
     * Specifies groups to exclude in the installation.
     * @return withoutGroups
     */
    public String[] getWithoutGroups() {
        return withoutGroups;
    }

    /**
     * Whether to include the --sync option on poetry install
     * @return forceSync
     */
    public boolean forceSync() {
        return forceSync;
    }

    /**
     * The set of managed dependencies to monitor for conformance
     * @return managedDependencies
     */
    public List<PackageDefinition> getManagedDependencies() {
        return managedDependencies;
    }

    /**
     * whether to update managed dependencies when found.
     * @return updateManagedDependenciesWhenFound
     */
    public boolean updateManagedDependenciesWhenFound() {
        return updateManagedDependenciesWhenFound;
    }

    /**
     * Whether to fail if managed dependencies mismatch.
     * @return failOnManagedDependenciesMismatches
     */
    public boolean failOnManagedDependenciesMismatches() {
        return failOnManagedDependenciesMismatches;
    }

    /**
     * Whether to configure Poetry's {@code virtualenvs.in-project} value for this project.
     * @return useInProjectVirtualEnvironment
     */
    public boolean useInProjectVirtualEnvironment() {
        return useInProjectVirtualEnvironment;
    }

    /**
     * Get Pypi upload suffix for publishing url
     * @return pypiUploadSuffix
     */
    public String getPypiUploadSuffix() {
        return pypiUploadSuffix;
    }

    /**
     * Get Dev Repository upload suffix for publishing url
     * @return devRepositoryUrlUploadSuffix
     */
    public String getDevRepositoryUrlUploadSuffix() {
        return devRepositoryUrlUploadSuffix;
    }

    /**
     * whether to enable Dev Repository upload suffix for publishing url
     * @return enableDevRepositoryUrlUploadSuffix
     */
    public boolean enableDevRepositoryUrlUploadSuffix() {
        return enableDevRepositoryUrlUploadSuffix;
    }


    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {

        if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == PackageManager.POETRY) {
            //Passing "this" in the param is intentional so that we can carry over all @param variables to this class using the instance
            //of InstallDependenciesMOJO instead of passing giant list of parameters above.
            InstallDependenciesPoetry installDependenciesPoetry = new InstallDependenciesPoetry(getPythonProjectBaseDir(), getLog(), this);
            installDependenciesPoetry.doExecute();
        } else {
            InstallDependenciesUv installDependenciesUv = new InstallDependenciesUv(getPythonProjectBaseDir(), getLog(), this);
            installDependenciesUv.doExecute();
        }
    }
}
