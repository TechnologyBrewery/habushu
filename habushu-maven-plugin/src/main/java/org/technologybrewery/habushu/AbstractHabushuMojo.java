package org.technologybrewery.habushu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.PyenvCommandHelper;
import org.technologybrewery.habushu.util.MavenPasswordDecoder;

/**
 * Contains logic common across the various Habushu mojos.
 */
public abstract class AbstractHabushuMojo extends AbstractMojo {

    protected static final String SNAPSHOT = "-SNAPSHOT";
    protected static final Pattern SEMVER2_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+-(rc|alpha|beta)\\.\\d+$",
                                                                     Pattern.CASE_INSENSITIVE);

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
     * credentials to use when publishing the package to the official public PyPI
     * repository.
     */
    protected static final String PUBLIC_PYPI_REPO_ID = "pypi";

    /**
     * Specifies the {@code <id>} of the {@code <server>} element declared within
     * the utilized settings.xml configuration that represents the PyPI repository
     * to which this project's archives will be published and/or used as a supplemental
     * repository from which dependencies may be installed. This property is
     * <b>REQUIRED</b> if publishing to or consuming dependencies from a private
     * PyPI repository that requires authentication - it is expected that the
     * relevant {@code <server>} element provides the needed authentication details.
     * If this property is *not* specified, this property will default to
     * {@link #PUBLIC_PYPI_REPO_ID} and the execution of the {@code deploy}
     * lifecycle phase will publish this package to the official public PyPI
     * repository. Downstream package publishing functionality (i.e.
     * {@link PublishToPyPiRepoMojo}) will use the relevant settings.xml
     * {@code <server>} declaration with a matching {@code <id>} as credentials for
     * publishing the package to the official public PyPI repository.
     */
    @Parameter(property = "habushu.pypiRepoId", defaultValue = PUBLIC_PYPI_REPO_ID)
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
    @Parameter(defaultValue = "false", property = "habushu.rewriteLocalPathDepsInArchives")
    protected boolean rewriteLocalPathDepsInArchives;

    /**
     * Find the username for a given server in Maven's user settings.
     *
     * @return the username for the server specified in Maven's settings.xml
     */
    public String findUsernameForServer() {
        Server server = this.settings.getServer(this.pypiRepoId);
        return server != null ? server.getUsername() : null;
    }

    /**
     * Find the password for a given server in Maven's user settings, decrypting password if needed.
     *
     * @return the password for the server specified in Maven's settings.xml
     */
    public String findPasswordForServer() {
        String password = "";
        if (this.decryptPassword) {
            password = decryptServerPassword();
        } else {
            getLog().warn(
                    "Detected use of plain-text password!  This is a security risk!  Please consider using an encrypted password!");
            password = findPlaintextPasswordForServer();
        }
        return password;
    }

    /**
     * Simple utility method to decrypt a stored password for a server.
     */
    public String decryptServerPassword() {
        String decryptedPassword = null;

        try {
            decryptedPassword = MavenPasswordDecoder.decryptPasswordForServer(this.settings, this.pypiRepoId);
        } catch (PlexusCipherException | SecDispatcherException e) {
            throw new HabushuException("Unable to decrypt stored passwords.", e);
        }

        return decryptedPassword;
    }

    /**
     * Find the plain-text server password, without decryption steps, extracted from Maven's user settings.
     *
     * @return the password for the specified server from Maven's settings.xml
     */
    public String findPlaintextPasswordForServer() {
        Server server = this.settings.getServer(this.pypiRepoId);
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
        return new PyenvCommandHelper(getPoetryProjectBaseDir());
    }

    /**
     * Creates a {@link PoetryCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     *
     * @return
     */
    protected PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(getPoetryProjectBaseDir());
    }

    /**
     * Base directory in which Poetry projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    protected File getPoetryProjectBaseDir() {
        return this.project.getBasedir();
    }

    /**
     * Returns a {@link File} representing this project's Poetry pyproject.toml
     * configuration.
     *
     * @return
     */
    protected File getPoetryPyProjectTomlFile() {
        return new File(getPoetryProjectBaseDir(), "pyproject.toml");
    }

    /**
     * Gets the PEP-440 compliant Python package version associated with the given
     * POM version.
     * <p>
     * If the provided POM version is a SNAPSHOT, the version is converted into its
     * corresponding developmental release version, with its numeric component
     * optionally included based on the given {@code addSnapshotNumber} and
     * {@code snapshotNumberDateFormatPattern} parameters. For example, given the
     * POM version of {@code 1.2.3-SNAPSHOT}, a Python package version of
     * {@code 1.2.3.dev} will be returned if {@code addSnapshotNumber} is false. If
     * {@code addSnapshotNumber} is true, the numeric component will be added and
     * defaults to the number of seconds from the epoch (i.e.
     * {@code 1.2.3.dev1658238063}). The format of the snapshot number may be
     * modified by providing a date format pattern (i.e. "YYYYMMddHHmm" would yield
     * {@code 1.2.3.dev202207191002})
     * <p>
     * If the provided POM version is a release version, it is expected to align
     * with a valid PEP-440 final release version and is returned unmodified.
     *
     * @param pomVersion POM version of the encapsulating module in which Habushu is
     *                   being executed.
     * @return version number of the encapsulated Python package, appropriately
     * formatted by the given parameters.
     */
    protected static String getPythonPackageVersion(String pomVersion, boolean addSnapshotNumber,
                                             String snapshotNumberDateFormatPattern) {
        Matcher matcher = SEMVER2_PATTERN.matcher(pomVersion);
        if(matcher.matches()) {
            String qualifier = matcher.group(1);
            pomVersion = pomVersion.replace("-" + qualifier + ".", qualifier);
        }
        String pythonPackageVersion = pomVersion;

        if (isPomVersionSnapshot(pomVersion)) {
            pythonPackageVersion = replaceSnapshotWithDev(pomVersion);

            if (addSnapshotNumber) {
                String snapshotNumber;
                LocalDateTime currentTime = LocalDateTime.now();

                if (StringUtils.isNotEmpty(snapshotNumberDateFormatPattern)) {
                    snapshotNumber = currentTime.format(DateTimeFormatter.ofPattern(snapshotNumberDateFormatPattern));
                } else {
                    snapshotNumber = String.valueOf(currentTime.toEpochSecond(ZoneOffset.UTC));
                }
                pythonPackageVersion += snapshotNumber;
            }
        }

        return pythonPackageVersion;
    }

    protected static String replaceSnapshotWithDev(String pomVersion) {
        return pomVersion.substring(0, pomVersion.indexOf(SNAPSHOT)) + ".dev";
    }

    /**
     * Returns whether the given POM version is a SNAPSHOT version.
     *
     * @param pomVersion
     * @return
     */
    protected static boolean isPomVersionSnapshot(String pomVersion) {
        return pomVersion.endsWith(SNAPSHOT);
    }

    /**
     * Finds and returns all custom tool poetry groups.
     *
     * @return list of custom tool poetry groups.
     */
    protected List<String> findCustomToolPoetryGroups() {
        List<String> toolPoetryGroupSections = new ArrayList<>();

        File pyProjectTomlFile = getPoetryPyProjectTomlFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();

            while (line != null) {
                line = line.strip();

                if (line.startsWith("[tool.poetry.group")) {
                    toolPoetryGroupSections.add(line.replace("[", StringUtils.EMPTY).replace("]", StringUtils.EMPTY));
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new HabushuException("Problem reading pyproject.toml to search for custom dependency groups!", e);
        }

        return toolPoetryGroupSections;
    }
}
