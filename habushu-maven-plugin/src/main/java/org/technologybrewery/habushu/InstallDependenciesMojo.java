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
    private boolean addPypiRepoAsPackageSources;

    /**
     * Configures the path for the simple index on a private pypi repository.
     * Certain private repository solutions (ie: devpi) use different names for the
     * simple index. devpi, for instance, uses "+simple".
     */
    @Parameter(property = "habushu.pypiSimpleSuffix", defaultValue = "simple")
    private String pypiSimpleSuffix;

    /**
     * Configures whether the lock file will be updated before install.
     */
    @Parameter(defaultValue = "false", property = "habushu.skipPoetryLockUpdate")
    private boolean skipPoetryLockUpdate;

    /**
     * Specifies groups to include in the installation.
     */
    @Parameter(property = "habushu.withGroups")
    private String[] withGroups;

    /**
     * Specifies groups to exclude from the installation.
     */
    @Parameter(property = "habushu.withoutGroups")
    private String[] withoutGroups;

    /**
     * Configuration option to include the --sync option on poetry install
     */
    @Parameter(defaultValue = "false", property = "habushu.forceSync")
    private boolean forceSync;

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

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        InstallDependenciesConfigurations installDependenciesConfigurations = new InstallDependenciesConfigurations(addPypiRepoAsPackageSources, pypiSimpleSuffix, skipPoetryLockUpdate, withGroups,
                withoutGroups, forceSync, managedDependencies, updateManagedDependenciesWhenFound, failOnManagedDependenciesMismatches, useInProjectVirtualEnvironment, pypiRepoId, pypiRepoUrl,
                useDevRepository, devRepositoryId, devRepositoryUrl, overridePackageVersion);

        if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == PackageManager.POETRY) {
            InstallDependenciesPoetry installDependenciesPoetry = new InstallDependenciesPoetry(getPythonProjectBaseDir(), getLog(), installDependenciesConfigurations);
            installDependenciesPoetry.doExecute();
        } else {
            InstallDependenciesUv installDependenciesUv = new InstallDependenciesUv(getPythonProjectBaseDir(), getLog(), installDependenciesConfigurations);
            installDependenciesUv.doExecute();
        }
    }
}
