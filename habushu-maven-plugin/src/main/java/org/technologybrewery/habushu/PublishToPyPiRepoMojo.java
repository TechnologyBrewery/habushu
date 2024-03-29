package org.technologybrewery.habushu;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Publishes the distribution archives generated by
 * {@link BuildDeploymentArtifactsMojo} to the configured PyPI repository.
 * {@link PublishToPyPiRepoMojo} leverages Poetry to support publishing to
 * private PyPI repositories as well as the official PyPI repository.
 * <p>
 * If publishing to a private PyPI repository, both {@link #pypiRepoId} and
 * {@link #pypiRepoUrl} <b>MUST</b> be specified, and it is expected that the
 * relevant username/password credentials are configured in a settings.xml
 * {@literal <server>} entry that has an {@literal <id>} that aligns with the
 * provided {@link #pypiRepoId}.
 * <p>
 * If neither {@link #pypiRepoId} nor {@link #pypiRepoUrl} are provided, *OR*
 * {@link #pypiRepoId} is set to {@code pypi} and {@link #pypiRepoUrl} is not
 * provided, it is assumed that the archives will be published to the official
 * PyPI repository. As described above, developers are expected to provide their
 * PyPI credentials via a username/password entry in their settings.xml with an
 * {@code <id>} that matches {@code pypi}, or use the appropriate Poetry command
 * to configure their PyPI credentials in an adhoc fashion (i.e.
 * {@code poetry config pypi-token.pypi my-token}).
 * <p>
 * If the POM version of the module being published is a SNAPSHOT, the Poetry
 * package will be published to the configured PyPI repository as a Python
 * developmental release. Developers may use
 * {@link #snapshotNumberDateFormatPattern} to adjust the formatting of the
 * numeric component of the published version.
 */
@Mojo(name = "publish-to-pypi-repo", defaultPhase = LifecyclePhase.DEPLOY)
public class PublishToPyPiRepoMojo extends AbstractHabushuMojo {

    /**
     * {@link DateTimeFormatter} compliant pattern that configures the numeric
     * portion of SNAPSHOT Poetry package versions that are published to the
     * configured PyPI repository. By default, the version of SNAPSHOT published
     * packages align with PEP-440 developmental releases and use a numeric
     * component that corresponds to the number of seconds since the epoch. For
     * example, if the POM version is {@code 1.2.3-SNAPSHOT}, the package may be
     * published by default as {@code 1.2.3.dev1658238063}. If
     * {@link #snapshotNumberDateFormatPattern} is provided, the numeric component
     * will reflect the given date format pattern applied to the current build time.
     * For example, if "YYYYMMddHHmm" is provided, {@code 1.2.3.dev202207191002} may
     * be published.
     */
    @Parameter(property = "habushu.snapshotNumberDateFormatPattern")
    protected String snapshotNumberDateFormatPattern;

    /**
     * Skips the entire execution of the deploy phase and does *not* publish the
     * Poetry package to the configured PyPI repository. This configuration may be
     * useful when individual Habushu modules within a larger multi-module project
     * hierarchy should *not* be published to PyPI, but it is still desirable to
     * automate the project's release via the {@code maven-release-plugin}.
     */
    @Parameter(property = "habushu.skipDeploy", defaultValue = "false")
    protected boolean skipDeploy;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (this.skipDeploy) {
            getLog().info(String.format(
                    "Skipping deploy phase - package for %s will not be published to the configured PyPI repository",
                    this.project.getId()));
            return;
        }

        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

        String pomVersion = project.getVersion();
        if (this.overridePackageVersion && isPomVersionSnapshot(pomVersion)) {
            String currentPythonPackageVersion = poetryHelper.execute(Arrays.asList("version", "-s"));

            String snapshotVersionToPublish = getPythonPackageVersion(pomVersion, true,
                    snapshotNumberDateFormatPattern);
            try {
                getLog().info(
                        String.format("Setting version of Poetry package to publish to %s", snapshotVersionToPublish));
                poetryHelper.executeAndLogOutput(Arrays.asList("version", snapshotVersionToPublish));
                publishPackage(poetryHelper, true);
            } finally {
                getLog().info(
                        String.format("Resetting Poetry package version back to %s", currentPythonPackageVersion));
                poetryHelper.executeAndLogOutput(Arrays.asList("version", currentPythonPackageVersion));
            }

        } else {
            publishPackage(poetryHelper, false);
        }

    }

    /**
     * Helper method that encapsulates publishing the Poetry package to the
     * configured PyPI repository.
     *
     * @param poetryHelper   Poetry command helper that delegates publishing
     *                       commands to Poetry.
     * @param rebuildPackage whether to rebuild the package prior to publishing it.
     *                       This is typically only required for SNAPSHOT packages
     *                       where the version of the Poetry package may be
     *                       dynamically set in
     *                       {@link PublishToPyPiRepoMojo#execute()} and as a
     *                       result, the artifacts that were built by previously
     *                       executed build phase (i.e.
     *                       {@link BuildDeploymentArtifactsMojo}) do not reference
     *                       a version that aligns with the target version to be
     *                       published.
     * @throws MojoExecutionException
     */
    protected void publishPackage(PoetryCommandHelper poetryHelper, boolean rebuildPackage)
            throws MojoExecutionException {
        List<Pair<String, Boolean>> publishToRepoWithCredsArgs = Collections.emptyList();

        String username = null;
        String password = null;
        if (StringUtils.isNotEmpty(pypiRepoId)) {
            username = findUsernameForServer();
            password = findPasswordForServer();
        }

        if (StringUtils.isNotEmpty(pypiRepoUrl)) {
            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                throw new MojoExecutionException(String.format(
                        "Please ensure that both <username> and <password> are provided for the <server> with <id> %s in your settings.xml configuration!",
                        pypiRepoId));
            }

            getLog().info(String.format("Adding repository configuration to poetry.toml for %s at %s", pypiRepoId,
                    pypiRepoUrl));
            poetryHelper.execute(
                    Arrays.asList("config", "--local", String.format("repositories.%s", pypiRepoId), pypiRepoUrl));
        }

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            publishToRepoWithCredsArgs = new ArrayList<Pair<String, Boolean>>();

            if (!PUBLIC_PYPI_REPO_ID.equals(this.pypiRepoId)) {
                publishToRepoWithCredsArgs.add(new ImmutablePair<String, Boolean>("--repository", false));
                publishToRepoWithCredsArgs.add(new ImmutablePair<String, Boolean>(pypiRepoId, false));
            }
            publishToRepoWithCredsArgs.add(new ImmutablePair<String, Boolean>("--username", false));
            publishToRepoWithCredsArgs.add(new ImmutablePair<String, Boolean>(username, false));
            publishToRepoWithCredsArgs.add(new ImmutablePair<String, Boolean>("--password", false));
            publishToRepoWithCredsArgs.add(new ImmutablePair<String, Boolean>(password, true));
        }

        String publishCommand = rewriteLocalPathDepsInArchives ? "publish-rewrite-path-deps" : "publish";

        getLog().info(String.format("Publishing archives to %s %s",
                StringUtils.isNotEmpty(pypiRepoUrl) ? pypiRepoUrl : "official PyPI repository",
                rewriteLocalPathDepsInArchives ? "with poetry-monorepo-dependency-plugin" : ""));

        if (!publishToRepoWithCredsArgs.isEmpty()) {
            publishToRepoWithCredsArgs.add(0, new ImmutablePair<String, Boolean>(publishCommand, false));
            if (rebuildPackage) {
                publishToRepoWithCredsArgs.add(1, new ImmutablePair<String, Boolean>("--build", false));
            }

            poetryHelper.executeWithSensitiveArgsAndLogOutput(publishToRepoWithCredsArgs);
        } else {
            getLog().warn(String.format(
                    "PyPI repository credentials not specified in <server> element in settings.xml with <id> of %s",
                    PUBLIC_PYPI_REPO_ID));
            getLog().warn(
                    "Please populate settings.xml with PyPI credentials or ensure that Poetry is manually configured with the correct PyPI credentials (i.e. poetry config pypi-token.pypi my-token)");
            List<String> publishToOfficialPypiRepoArgs = new ArrayList<>();
            publishToOfficialPypiRepoArgs.add(publishCommand);
            if (rebuildPackage) {
                publishToOfficialPypiRepoArgs.add("--build");
            }
            poetryHelper.executeAndLogOutput(publishToOfficialPypiRepoArgs);
        }
    }
}
