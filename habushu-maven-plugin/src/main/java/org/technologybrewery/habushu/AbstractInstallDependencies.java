package org.technologybrewery.habushu;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.util.HabushuUtil;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations.
 */
public abstract class AbstractInstallDependencies {
    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * Instance of InstallDependenciesMOJO
     */
    protected InstallDependenciesMojo installDependenciesMojo;

    protected static final String PUBLIC_PYPI_REPO_ID = "pypi";

    protected static final String SNAPSHOT = "-SNAPSHOT";


    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir    base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param mojo Configurations for installing Dependencies
     */
    protected AbstractInstallDependencies(File baseDir, Log log, InstallDependenciesMojo mojo) {
        this.baseDir = baseDir;
        this.log = log;
        this.installDependenciesMojo = mojo;
    }


    public abstract void doExecute() throws MojoFailureException, IOException;

    /**
     * Returns a {@link File} representing this project's pyproject.toml
     * configuration.
     *
     * @return
     */
    protected File getPyProjectTomlFile() {
        return new File(baseDir, "pyproject.toml");
    }

    /**
     * log that package is not up to date with managed dependencies.
     */
    protected void logPackageMismatch(String packageName, String originalOperatorAndVersion, String updatedOperatorAndVersion) {
        log.warn(String.format("Package %s is not up to date with common project package definition guidance! "
                + "Currently %s, but should be %s!", packageName, originalOperatorAndVersion, updatedOperatorAndVersion));
    }

    protected void prepareRepositoryForInstallation(String repoId, String repoUrl, String packageIndexPath) {
        if (StringUtils.isNotEmpty(repoUrl) && installDependenciesMojo.addPypiRepoAsPackageSources) {
            String pypiRepoSimpleIndexUrl;
            try {
                pypiRepoSimpleIndexUrl = getPyPiRepoSimpleIndexUrl(repoUrl);
            } catch (URISyntaxException e) {
                throw new HabushuException(
                        String.format("Could not parse configured repoUrl %s", repoUrl), e);
            }
            Config matchingPypiRepoSourceConfig = getMatchingPypiRepoIndexConfig(packageIndexPath, pypiRepoSimpleIndexUrl);

            if (!matchingPypiRepoSourceConfig.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Configured PyPi repository %s found in the following pyproject.toml [[%s]] array element: %s",
                            repoUrl, packageIndexPath, matchingPypiRepoSourceConfig));
                }
            } else {
                // NB NightConfig's TOML serializer generates TOML in a manner that makes it
                // difficult to append an array element of tables to an existing TOML
                // configuration, so manually write out the desired new repository TOML
                // configuration with human-readable formatting
                ArrayList<String> newPypiRepoSourceConfig = new ArrayList<>(Arrays.asList(System.lineSeparator(), String.format(
                                "# Added by habushu-maven-plugin at %s to use %s as source PyPi repository for installing dependencies",
                                LocalDateTime.now(), pypiRepoSimpleIndexUrl),
                        String.format("[[%s]]", packageIndexPath),
                        String.format("name = \"%s\"",
                                StringUtils.isNotEmpty(repoId) && !PUBLIC_PYPI_REPO_ID.equals(repoId)
                                        ? repoId
                                        : "private-pypi-repo"),
                        String.format("url = \"%s\"", pypiRepoSimpleIndexUrl)));
                if (HabushuUtil.isCurrentPackageManagerUv(getPyProjectTomlFile()) ) {
                    newPypiRepoSourceConfig.add(String.format("publish-url = \"%s\"", getPublishUrl(repoUrl, !PUBLIC_PYPI_REPO_ID.equals(repoId))));
                }
                newPypiRepoSourceConfig.add("priority = \"supplemental\"");


                log.info(String.format("Private PyPi repository entry for %s not found in pyproject.toml",
                        repoUrl));
                log.info(String.format(
                        "Adding %s to pyproject.toml as supplemental repository from which dependencies may be installed",
                        pypiRepoSimpleIndexUrl));
                try {
                    Files.write(getPyProjectTomlFile().toPath(), newPypiRepoSourceConfig,
                            StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new HabushuException(String.format(
                            "Could not write new [[%s]] element to pyproject.toml", packageIndexPath), e);
                }
            }

        }
    }

    /**
     * Attempts to infer the PEP-503 compliant PyPI simple repository index URL
     * associated with the provided PyPI repository URL. In order to configure
     * UV or Poetry to use a private PyPi repository as a source for installing package
     * dependencies, the simple index URL of the repository <b>*must*</b> be
     * utilized. For example, if a private PyPI repository is hosted at
     * https://my-company-sonatype-nexus/repository/internal-pypi and provided to
     * Habushu via the {@literal <pypiRepoUrl>} configuration, the simple index URL
     * returned by this method will be
     * https://my-company-sonatype-nexus/repository/internal-pypi/simple/ (the
     * trailing slash is required!).
     *
     * @param pypiRepoUrl URL of the private PyPi repository for which to generate
     *                    the simple index API URL.
     * @return simple index API URL associated with the given PyPi repository URL.
     * @throws URISyntaxException
     */
    protected String getPyPiRepoSimpleIndexUrl(String pypiRepoUrl) throws URISyntaxException {
        URIBuilder pypiRepoUriBuilder = new URIBuilder(StringUtils.removeEnd(pypiRepoUrl, "/"));
        List<String> repoUriPathSegments = pypiRepoUriBuilder.getPathSegments();
        String lastPathSegment = CollectionUtils.isNotEmpty(repoUriPathSegments)
                ? repoUriPathSegments.get(repoUriPathSegments.size() - 1)
                : null;
        if (!installDependenciesMojo.pypiSimpleSuffix.equals(lastPathSegment)) {
            // If the URL has no path, an unmodifiable Collections.emptyList() is returned,
            // so wrap in an ArrayList to enable later modifications
            repoUriPathSegments = new ArrayList<>(repoUriPathSegments);
            repoUriPathSegments.add(installDependenciesMojo.pypiSimpleSuffix);
            pypiRepoUriBuilder.setPathSegments(repoUriPathSegments);
        }

        return StringUtils.appendIfMissing(pypiRepoUriBuilder.build().toString(), "/");
    }

    protected static String replaceSnapshotWithWildcard(String pomVersion) {
        return pomVersion.substring(0, pomVersion.indexOf(SNAPSHOT)) + ".*";
    }

    protected String getPublishUrl(String repoUrl, boolean isDevRepository) {
        String repositoryUrl = addTrailingSlash(repoUrl);
        if (isDevRepository) {
            repositoryUrl += addTrailingSlash(installDependenciesMojo.getDevRepositoryUrlUploadSuffix());
        } else if(!StringUtils.isEmpty(installDependenciesMojo.getPypiUploadSuffix())) {
            repositoryUrl += addTrailingSlash(installDependenciesMojo.getPypiUploadSuffix());
        }

        return repositoryUrl;
    }


    protected static String addTrailingSlash(String inputUrl) {
        if (StringUtils.isNotBlank(inputUrl) && !StringUtils.endsWith(inputUrl, "/")) {
            // PEP-0694 likes a trailing slash:
            inputUrl += "/";
        }

        return inputUrl;
    }

    protected Config getMatchingPypiRepoIndexConfig(String packageIndexPath, String pypiRepoSimpleIndexUrl) {
        Config matchingPypiRepoIndexConfig;
        try (FileConfig pyProjectConfig = FileConfig.of(getPyProjectTomlFile())) {
            pyProjectConfig.load();

            Optional<List<Config>> packageIndex = pyProjectConfig.getOptional(packageIndexPath);
            matchingPypiRepoIndexConfig = packageIndex.orElse(Collections.emptyList()).stream()
                    .filter(packageIdx -> pypiRepoSimpleIndexUrl.equals(packageIdx.get("url"))).findFirst()
                    .orElse(Config.inMemory());
        }

        return matchingPypiRepoIndexConfig;
    }
}