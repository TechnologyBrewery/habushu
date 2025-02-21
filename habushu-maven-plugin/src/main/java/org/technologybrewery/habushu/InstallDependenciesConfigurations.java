package org.technologybrewery.habushu;

import java.util.List;

public class InstallDependenciesConfigurations {

    /**
     * Configures whether a private PyPi repository, if specified via
     * {@link AbstractHabushuMojo#pypiRepoUrl}, is automatically added as a package
     * source from which dependencies may be installed. This value is <b>*only*</b>
     * utilized if a private PyPi repository is specified via
     * {@link AbstractHabushuMojo#pypiRepoUrl}.
     */
    private final boolean addPypiRepoAsPackageSources;

    /**
     * Configures the path for the simple index on a private pypi repository.
     * Certain private repository solutions (ie: devpi) use different names for the
     * simple index. devpi, for instance, uses "+simple".
     */
    private final String pypiSimpleSuffix;

    /**
     * Configures whether the lock file will be updated before install.
     */
    private final boolean skipPoetryLockUpdate;

    /**
     * Specifies groups to include in the installation.
     */
    private final String[] withGroups;

    /**
     * Specifies groups to exclude from the installation.
     */
    private final String[] withoutGroups;

    /**
     * Configuration option to include the --sync option on poetry install
     */
    private final boolean forceSync;

    /**
     * The set of managed dependencies to monitor for conformance.  These can result in:
     * * direct changes to your pyproject.toml file (default behavior)
     * * log statements warning of mismatches (if habushu.updateManagedDependenciesWhenFound = false)
     * * stopping the build for manual intervention (if habushu.failOnManagedDependenciesMismatches = true)
     */
    protected final List<PackageDefinition> managedDependencies;

    /**
     * Determines if managed dependency mismatches are automatically updated when encountered.
     */
    protected final boolean updateManagedDependenciesWhenFound;

    /**
     * Determines if the build should be failed when managed dependency mismatches are found.
     */
    protected final boolean failOnManagedDependenciesMismatches;

    /**
     * Whether to configure Poetry's {@code virtualenvs.in-project} value for this project.
     * If configured, virtual environments will be migrated to this approach during the clean phase of the build.
     */
    protected final boolean useInProjectVirtualEnvironment;

    /**
     * Repository ID for public pypi repository.
     */
    protected final String pypiRepoId;

    /**
     * Repository Url for public pypi repository.
     */
    protected final String pypiRepoUrl;

    /**
     * Whether to use Dev Repository for pulling dependencies.
     */
    protected final boolean useDevRepository;

    /**
     * Repository ID for dev repository.
     */
    protected final String devRepositoryId;

    /**
     * Repository url for dev repository.
     */
    protected final String devRepositoryUrl;

    /**
     * specifies whether the version of the encapsulated Poetry package should be automatically
     * managed and overridden where necessary by Habushu.
     */
    protected boolean overridePackageVersion;


    public InstallDependenciesConfigurations(boolean addPypiRepoAsPackageSources, String pypiSimpleSuffix, boolean skipPoetryLockUpdate,
                                             String[] withGroups, String[] withoutGroups, boolean forceSync,
                                             List<PackageDefinition> managedDependencies, boolean updateManagedDependenciesWhenFound,
                                             boolean failOnManagedDependenciesMismatches, boolean useInProjectVirtualEnvironment, String pypiRepoId,
                                             String pypiRepoUrl, boolean useDevRepository, String devRepositoryId, String devRepositoryUrl, boolean overridePackageVersion) {
        this.addPypiRepoAsPackageSources = addPypiRepoAsPackageSources;
        this.pypiSimpleSuffix = pypiSimpleSuffix;
        this.skipPoetryLockUpdate = skipPoetryLockUpdate;
        this.withGroups = withGroups;
        this.withoutGroups = withoutGroups;
        this.forceSync = forceSync;
        this.managedDependencies = managedDependencies;
        this.updateManagedDependenciesWhenFound = updateManagedDependenciesWhenFound;
        this.failOnManagedDependenciesMismatches = failOnManagedDependenciesMismatches;
        this.useInProjectVirtualEnvironment = useInProjectVirtualEnvironment;
        this.pypiRepoId = pypiRepoId;
        this.pypiRepoUrl = pypiRepoUrl;
        this.useDevRepository = useDevRepository;
        this.devRepositoryId = devRepositoryId;
        this.devRepositoryUrl = devRepositoryUrl;
        this.overridePackageVersion = overridePackageVersion;

    }

    public boolean addPypiRepoAsPackageSources() {
        return addPypiRepoAsPackageSources;
    }

    public String getPypiSimpleSuffix() {
        return pypiSimpleSuffix;
    }

    public boolean skipPoetryLockUpdate() {
        return skipPoetryLockUpdate;
    }

    public String[] getWithGroups() {
        return withGroups;
    }

    public String[] getWithoutGroups() {
        return withoutGroups;
    }

    public boolean isForceSync() {
        return forceSync;
    }

    public List<PackageDefinition> getManagedDependencies() {
        return managedDependencies;
    }

    public boolean updateManagedDependenciesWhenFound() {
        return updateManagedDependenciesWhenFound;
    }

    public boolean failOnManagedDependenciesMismatches() {
        return failOnManagedDependenciesMismatches;
    }

    public boolean useInProjectVirtualEnvironment() {
        return useInProjectVirtualEnvironment;
    }

    public String getPypiRepoId() {
        return pypiRepoId;
    }

    public String getPypiRepoUrl() {
        return pypiRepoUrl;
    }

    public boolean useDevRepository() {
        return useDevRepository;
    }

    public String getDevRepositoryId() {
        return devRepositoryId;
    }

    public String getDevRepositoryUrl() {
        return devRepositoryUrl;
    }

    public boolean overridePackageVersion() {
        return overridePackageVersion;
    }
}
