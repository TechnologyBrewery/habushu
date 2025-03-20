package org.technologybrewery.habushu;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.PyenvCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.MavenPasswordDecoder;
import org.technologybrewery.habushu.util.PackageManager;

/**
 * Contains logic common across the various Habushu mojos.
 */
public abstract class AbstractHabushuMojo extends AbstractMojo {

    /**
     * The current Maven user's settings, pulled dynamically from their settings.xml
     * file.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    /**
     * Toggle for whether the server password should be decrypted or retrieved as
     * plain text.
     * <p>
     * true (default) -> decrypt false -> plain text
     */
    @Parameter(property = "habushu.decryptPassword", defaultValue = "true")
    protected boolean decryptPassword;

    /**
     * The packaging type of the current Maven project. If it is not "habushu", then habushu packaging-related mojos
     * will skip execution.
     */
    @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
    protected String packaging;

    /**
     * Base directory in which Python projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected File workingDirectory;

    /**
     * Folder in which Python source files are located - should align with Poetry's
     * project structure conventions.
     */
    @Parameter(property = "habushu.sourceDirectory", required = true, defaultValue = "${project.basedir}/src")
    protected File sourceDirectory;

    /**
     * Folder in which Python test files are located - should align with Poetry's
     * project structure conventions.
     */
    @Parameter(property = "habushu.testDirectory", required = true, defaultValue = "${project.basedir}/tests")
    protected File testDirectory;

    /**
     * Specifies the {@code <id>} of the {@code <server>} element declared within
     * the utilized settings.xml configuration that represents the desired
     * credentials to use when publishing the package to a dev PyPI repository.
     */
    public static final String DEV_PYPI_REPO_ID = "dev-pypi";

    /**
     * Specifies the default dev pypi url to leverage.
     */
    public static final String TEST_PYPI_REPOSITORY_URL = "https://test.pypi.org/";

    /**
     * Specifies the {@code <id>} of the {@code <server>} element declared within
     * the utilized settings.xml configuration that represents the PyPI repository
     * to which this project's archives will be published and/or used as a supplemental
     * repository from which dependencies may be installed. This property is
     * <b>REQUIRED</b> if publishing to or consuming dependencies from a private
     * PyPI repository that requires authentication - it is expected that the
     * relevant {@code <server>} element provides the needed authentication details.
     * If this property is *not* specified, this property will default to
     * PUBLIC_PYPI_REPO_ID and the execution of the {@code deploy}
     * lifecycle phase will publish this package to the official public PyPI
     * repository. Downstream package publishing functionality (i.e.
     * {@link PublishToPyPiRepoMojo}) will use the relevant settings.xml
     * {@code <server>} declaration with a matching {@code <id>} as credentials for
     * publishing the package to the official public PyPI repository.
     */
    @Parameter(property = "habushu.pypiRepoId", defaultValue = HabushuUtil.PUBLIC_PYPI_REPO_ID)
    protected String pypiRepoId;

    /**
     * Specifies the URL of the private PyPI repository to which this project's
     * archives will be published and/or used as a supplemental repository from which
     * dependencies may be installed. This property is <b>REQUIRED</b> if publishing
     * to or consuming dependencies from a private PyPI repository.
     */
    @Parameter(property = "habushu.pypiRepoUrl")
    protected String pypiRepoUrl;

    /**
     * Instructs deployment to use a development repository rather than a release repository. This is conceptually
     * similar to Maven's release vs. snapshot repositories, allowing the release repository to only have formal
     * releases with a separate repository for all 'dev' releases.  Works in conjunction with the
     * {@code devRepositoryId} and {@code devRepositoryUrl>} properties.
     */
    @Parameter(property = "habushu.useDevRepository", defaultValue = "false")
    protected boolean useDevRepository;

    /**
     * Specifies the {@code <id>} of the {@code <server>} element declared within
     * the utilized settings.xml configuration that represents the PyPI dev repository
     * to which this project's archives will be published and/or used as a supplemental
     * repository from which dependencies may be installed when {@code useDevRepository}
     * is enabled. This property is <b>REQUIRED</b> if publishing to or consuming
     * dependencies from a devPyPI repository that requires authentication - it is
     * expected that the relevant {@code <server>} element provides the needed
     * authentication details. If this property is *not* specified, this property will
     * default to {@link #DEV_PYPI_REPO_ID} and the execution of the {@code deploy}
     * lifecycle phase will publish this package to the official public Test PyPI
     * repository. Downstream package publishing functionality (i.e.
     * {@link PublishToPyPiRepoMojo}) will use the relevant settings.xml
     * {@code <server>} declaration with a matching {@code <id>} as credentials for
     * publishing the package to the official public test PyPI repository.
     */
    @Parameter(property = "habushu.devRepositoryId", defaultValue = DEV_PYPI_REPO_ID)
    protected String devRepositoryId;

    /**
     * Specifies the URL of the PyPI repository to which this project's dev
     * archives will be published and/or used as a supplemental repository from which
     * dependencies may be installed. This property is <b>REQUIRED</b> if publishing
     * to or consuming dependencies from a private PyPI repository.  Should end with a
     * trailing "/".
     */
    @Parameter(property = "habushu.devRepositoryUrl", defaultValue = TEST_PYPI_REPOSITORY_URL)
    protected String devRepositoryUrl;

    /**
     * Specifies whether the version of the encapsulated Poetry package should be
     * automatically managed and overridden where necessary by Habushu. If this
     * property is true, Habushu may override the pyproject.toml defined version in
     * the following build phases/mojos:
     * <ul>
     * <li>initialize ({@link InitializeHabushuMojo}): Automatically sets the
     * Poetry package version to the version specified in the POM. If the POM is a
     * SNAPSHOT, the Poetry package version will be set to the corresponding
     * developmental release version without a numeric component (i.e. POM version
     * of {@code 1.2.3-SNAPSHOT} will result in the Poetry package version being set
     * to {@code 1.2.3.dev}). If the version is a release candidate (`rc`), `alpha`,
     * or `beta` version in SemVer 2.0 format then it is translated to the equivalent
     * PEP 440 format.</li>
     * <li>deploy ({@link PublishToPyPiRepoMojo}): Automatically sets the version of
     * published Poetry packages that are SNAPSHOT modules to timestamped
     * developmental release versions (i.e. POM version of {@code 1.2.3-SNAPSHOT}
     * will result in the published Poetry package version to to
     * {@code 1.2.3.dev1658238063}). After the package is published, the version of
     * the SNAPSHOT module is reverted to its previous value (i.e.
     * {@code 1.2.3.dev}).</li>
     * </ul>
     * If {@link #overridePackageVersion} is set to false, none of the above
     * automated version management operations will be performed.
     */
    @Parameter(defaultValue = "true", property = "habushu.overridePackageVersion")
    protected boolean overridePackageVersion;

    /**
     * Enables access to the runtime properties associated with the project's POM
     * configuration against which Habushu is being executed.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Indicates whether Habushu should leverage the
     * {@code poetry-monorepo-dependency-plugin} or the
     * {@code uv-monorepo-dependency-plugin} (<- todo) to rewrite any local path
     * dependencies (to other Poetry/uv projects) as versioned packaged dependencies in
     * generated wheel/sdist archives. If {@code true}, Habushu will replace
     * invocations of Poetry/uv's {@code build} and {@code publish} commands in the
     * {@link BuildDeploymentArtifactsMojo} and {@link PublishToPyPiRepoMojo} with
     * the extensions of those commands exposed by the
     * {@code poetry-monorepo-dependency-plugin}/{@code uv-monorepo-dependency-plugin}, which are
     * {@code build-rewrite-path-deps} and {@code publish-rewrite-path-deps}
     * respectively.
     * <p>
     * Typically, this flag will only be {@code true} when deploying/releasing
     * Habushu modules within a CI environment that are part of a monorepo project
     * structure in which multiple Poetry/uv projects depend on one another.
     */
    @Parameter(defaultValue = "false", property = "habushu.rewriteLocalPathDepsInArchives")
    protected boolean rewriteLocalPathDepsInArchives;

    public Settings getSettings() {
        return settings;
    }

    public boolean isDecryptPassword() {
        return decryptPassword;
    }

    public String getPackaging() {
        return packaging;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public File getTestDirectory() {
        return testDirectory;
    }

    public boolean isUseDevRepository() {
        return useDevRepository;
    }

    public boolean isOverridePackageVersion() {
        return overridePackageVersion;
    }

    public MavenProject getProject() {
        return project;
    }

    public boolean isRewriteLocalPathDepsInArchives() {
        return rewriteLocalPathDepsInArchives;
    }

    /**
     * Find the username for a given server in Maven's user settings.
     *
     * @return the username for the server specified in Maven's settings.xml
     */
    public String findUsernameForServer() {
        return findUsernameForServer(this.pypiRepoId);
    }

    /**
     * Find the username for a given server in Maven's user settings.
     *
     * @param repoId the id of the repository for which to find the username
     * @return the username for the server specified in Maven's settings.xml
     */
    public String findUsernameForServer(String repoId) {
        Server server = this.settings.getServer(repoId);
        return server != null ? server.getUsername() : null;
    }

    /**
     * Find the password for a given server in Maven's user settings, decrypting password if needed.
     *
     * @return the password for the server specified in Maven's settings.xml
     */
    public String findPasswordForServer() {
        return findPasswordForServer(this.pypiRepoId);
    }

    /**
     * Find the password for a given server in Maven's user settings, decrypting password if needed.
     *
     * @param repoId the id of the repository for which to find the password
     * @return the password for the server specified in Maven's settings.xml
     */
    public String findPasswordForServer(String repoId) {
        String password = "";
        if (this.decryptPassword) {
            password = decryptServerPassword(repoId);
        } else {
            getLog().warn(
                    "Detected use of plain-text password!  This is a security risk!  Please consider using an encrypted password!");
            password = findPlaintextPasswordForServer(repoId);
        }
        return password;
    }

    /**
     * Simple utility method to decrypt a stored password for a server.
     *
     * @param repoId the ide of the repository for which to find the password
     * @return the decrypted password for the server specified in Maven's settings.xml
     */
    public String decryptServerPassword(String repoId) {
        String decryptedPassword = null;

        try {
            decryptedPassword = MavenPasswordDecoder.decryptPasswordForServer(this.settings, repoId);
        } catch (PlexusCipherException | SecDispatcherException e) {
            throw new HabushuException("Unable to decrypt stored passwords.", e);
        }

        return decryptedPassword;
    }

    protected String findPlaintextPasswordForServer(String repoId) {
        Server server = this.settings.getServer(repoId);
        return server != null ? server.getPassword() : null;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if ("habushu".equals(packaging)) {
            doExecute();
        } else {
            getLog().info("Skipping execution - packaging type is not 'habushu'");
        }
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    /**
     * Gets the canonical path for a file without having to deal w/ checked
     * exceptions.
     *
     * @param file file for which to get the canonical format
     * @return canonical format
     */
    protected String getCanonicalPathForFile(File file) {
        try {
            return file.getCanonicalPath();

        } catch (IOException ioe) {
            throw new HabushuException("Could not access file: " + file.getName(), ioe);
        }
    }

    /**
     * Creates a {@link PyenvCommandHelper} that may be used to invoke Pyenv
     * commands from the project's working directory.
     *
     * @return
     */
    protected PyenvCommandHelper createPyenvCommandHelper() {
        return new PyenvCommandHelper(getPythonProjectBaseDir());
    }

    /**
     * Creates a {@link PoetryCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     *
     * @return PoetryCommandHelper
     */
    protected PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(getPythonProjectBaseDir());
    }

    /**
     * Creates a {@link UvCommandHelper} that may be used to invoke uv
     * commands from the project's working directory.
     *
     * @return UvCommandHelper
     */
    protected UvCommandHelper createUvCommandHelper() {
        return new UvCommandHelper(getPythonProjectBaseDir());
    }

    /**
     Creates a {@link PoetryCommandHelper} or {@link UvCommandHelper} that may be used to invoke Poetry or uv
     * commands from the project's working directory.
     *
     * @return helper
     */
    protected CommandHelper getCommandHelper() {
        CommandHelper helper;

        PackageManager packageManagerType = HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile());
        if (PackageManager.POETRY.equals(packageManagerType)) {
            helper = new PoetryCommandHelper(getPythonProjectBaseDir());
        } else {
            helper = new UvCommandHelper(getPythonProjectBaseDir());
        }
        return helper;
    }

    /**
     * Base directory in which Poetry projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    protected File getPythonProjectBaseDir() {
        return this.project.getBasedir();
    }

    /**
     * Artifact Id for Package Manager projects
     */
    protected String getProjectArtifactId() {
        return this.project.getArtifactId();
    }

    /**
     * Artifact for Package Manager projects
     */
    protected Artifact getProjectArtifact() {
        return this.project.getArtifact();
    }


    /**
     * Fetches pypi Repository Id
     * @return pypi repository id.
     */
    public String getPypiRepoId() {
        return pypiRepoId;
    }

    /**
     * Fetches pypi Repository url
     * @return pypi repository url.
     */
    public String getPypiRepoUrl() {
        return pypiRepoUrl;
    }

    /**
     *  Check whether to use dev Repository
     * @return boolean useDevRepository
     */
    public boolean useDevRepository() {
        return useDevRepository;
    }

    /**
     * Fetches dev Repository Id
     * @return dev repository id.
     */
    public String getDevRepositoryId() {
        return devRepositoryId;
    }

    /**
     * Fetches dev Repository url
     * @return dev repository url.
     */
    public String getDevRepositoryUrl() {
        return devRepositoryUrl;
    }

    /**
     *  Check whether to override package version
     * @return boolean overridePackageVersion
     */
    public boolean overridePackageVersion() {
        return overridePackageVersion;
    }

    /**
     *  Check whether to rewriteLocalPathDepsInArchives
     * @return boolean rewriteLocalPathDepsInArchives
     */
    public boolean rewriteLocalPathDepsInArchives() {
        return rewriteLocalPathDepsInArchives;
    }

    /**
     * Returns a {@link File} representing this project's pyproject.toml
     * configuration.
     *
     * @return
     */
    protected File getPyProjectTomlFile() {
        return new File(getPythonProjectBaseDir(), "pyproject.toml");
    }


}
