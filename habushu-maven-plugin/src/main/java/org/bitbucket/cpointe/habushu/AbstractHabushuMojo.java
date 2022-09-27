package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PyenvCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PythonVersionHelper;

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
     * Configures whether to bypass use of poetry-lock-groups.  Setting this to true
     * may cause certain dependency group operations to fail.
     */
    @Parameter(defaultValue = "false", property = "habushu.useLockWithGroups")
    protected boolean useLockWithGroups;

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
     * to which this project's archives will be published and/or used as a secondary
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
     * archives will be published and/or used as a secondary repository from which
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
     * <li>validate ({@link ValidatePyenvAndPoetryMojo}): Automatically sets the
     * Poetry package version to the version specified in the POM. If the POM is a
     * SNAPSHOT, the Poetry package version will be set to the corresponding
     * developmental release version without a numeric component (i.e. POM version
     * of {@code 1.2.3-SNAPSHOT} will result in the Poetry package version being set
     * to {@code 1.2.3.dev}).</li>
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
     * Should habushu use pyenv to manage python versioning.
     */
    @Parameter(defaultValue = "true", property = "habushu.usePyenv")
    protected boolean usePyenv;

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
     * Creates a {@link PythonVersionHelper} that may be used to invoke Pyenv
     * commands from the project's working directory.
     *
     * @param pythonVersion The desired version of Python to use
     * @return
     */
    protected PythonVersionHelper createPythonVersionHelper(String pythonVersion) {
        return new PythonVersionHelper(getPoetryProjectBaseDir(), pythonVersion);
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
     *         formatted by the given parameters.
     */
    protected String getPythonPackageVersion(String pomVersion, boolean addSnapshotNumber,
	    String snapshotNumberDateFormatPattern) {
	String pythonPackageVersion = pomVersion;

	if (isPomVersionSnapshot(pomVersion)) {
	    pythonPackageVersion = pomVersion.substring(0, pomVersion.indexOf("-SNAPSHOT")) + ".dev";

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

    /**
     * Returns whether the given POM version is a SNAPSHOT version.
     * 
     * @param pomVersion
     * @return
     */
    protected boolean isPomVersionSnapshot(String pomVersion) {
	return pomVersion.endsWith("-SNAPSHOT");
    }
}
