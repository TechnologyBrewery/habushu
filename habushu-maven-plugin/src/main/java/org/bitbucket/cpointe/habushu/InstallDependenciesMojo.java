package org.bitbucket.cpointe.habushu;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;

/**
 * Installs dependencies defined in the project's pyproject.toml configuration,
 * specifically by running "poetry lock" followed by "poetry install". If a
 * private PyPi repository is defined via
 * {@link AbstractHabushuMojo#pypiRepoUrl} (and
 * {@link AbstractHabushuMojo#pypiRepoId}), it will be automatically added to
 * the module's pyproject.toml configuration as a secondary source of
 * dependencies, if it is not already configured in the pyproject.toml
 */
@Mojo(name = "install-dependencies", defaultPhase = LifecyclePhase.COMPILE)
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
     * Path within a Poetry project's pyproject.toml configuration at which private
     * PyPi repositories may be specified as sources from which dependencies may be
     * resolved and installed.
     */
    protected final String PYPROJECT_PACKAGE_SOURCES_PATH = "tool.poetry.source";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

	if (StringUtils.isNotEmpty(this.pypiRepoUrl) && this.addPypiRepoAsPackageSources) {
	    String pypiRepoSimpleIndexUrl;
	    try {
		pypiRepoSimpleIndexUrl = getPyPiRepoSimpleIndexUrl(pypiRepoUrl);
	    } catch (URISyntaxException e) {
		throw new MojoExecutionException(
			String.format("Could not parse configured pypiRepoUrl %s", this.pypiRepoUrl), e);
	    }

	    // NB later version of Poetry will support retrieving and configuring package
	    // source repositories via the "poetry source" command in future releases, but
	    // for now we need to manually inspect and modify the package's pyproject.toml
	    Config matchingPypiRepoSourceConfig;
	    try (FileConfig pyProjectConfig = FileConfig.of(getPoetryPyProjectTomlFile())) {
		pyProjectConfig.load();

		Optional<List<Config>> packageSources = pyProjectConfig.getOptional(PYPROJECT_PACKAGE_SOURCES_PATH);
		matchingPypiRepoSourceConfig = packageSources.orElse(Collections.emptyList()).stream()
			.filter(packageSource -> pypiRepoSimpleIndexUrl.equals(packageSource.get("url"))).findFirst()
			.orElse(Config.inMemory());
	    }

	    if (!matchingPypiRepoSourceConfig.isEmpty()) {
		if (getLog().isDebugEnabled()) {
		    getLog().debug(String.format(
			    "Configured PyPi repository %s found in the following pyproject.toml [[%s]] array element: %s",
			    this.pypiRepoUrl, PYPROJECT_PACKAGE_SOURCES_PATH, matchingPypiRepoSourceConfig));
		}
	    } else {
		// NB NightConfig's TOML serializer generates TOML in a manner that makes it
		// difficult to append an array element of tables to an existing TOML
		// configuration, so manually write out the desired new repository TOML
		// configuration with human-readable formatting
		List<String> newPypiRepoSourceConfig = Arrays.asList(System.lineSeparator(), String.format(
			"# Added by habushu-maven-plugin at %s to use %s as source PyPi repository for installing dependencies",
			LocalDateTime.now(), pypiRepoSimpleIndexUrl),
			String.format("[[%s]]", PYPROJECT_PACKAGE_SOURCES_PATH),
			String.format("name = \"%s\"",
				StringUtils.isNotEmpty(this.pypiRepoId) ? this.pypiRepoId : "private-pypi-repo"),
			String.format("url = \"%s\"", pypiRepoSimpleIndexUrl), "secondary = true");
		getLog().info(String.format("Private PyPi repository entry for %s not found in pyproject.toml",
			this.pypiRepoUrl));
		getLog().info(String.format(
			"Adding %s to pyproject.toml as secondary repository from which dependencies may be installed",
			pypiRepoSimpleIndexUrl));
		try {
		    Files.write(getPoetryPyProjectTomlFile().toPath(), newPypiRepoSourceConfig,
			    StandardOpenOption.APPEND);
		} catch (IOException e) {
		    throw new MojoExecutionException(String.format(
			    "Could not write new [[%s]] element to pyproject.toml", PYPROJECT_PACKAGE_SOURCES_PATH), e);
		}
	    }

	}

	getLog().info("Locking dependencies specified in pyproject.toml...");
	poetryHelper.executeAndLogOutput(Arrays.asList("lock"));

	getLog().info("Installing dependencies...");
	poetryHelper.executeAndLogOutput(Arrays.asList("install"));
    }

    /**
     * Attempts to infer the PEP-503 compliant PyPI simple repository index URL
     * associated with the provided PyPI repository URL. In order to configure
     * Poetry to use a private PyPi repository as a source for installing package
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
	if (!"simple".equals(lastPathSegment)) {
	    repoUriPathSegments.add("simple");
	    pypiRepoUriBuilder.setPathSegments(repoUriPathSegments);
	}

	return StringUtils.appendIfMissing(pypiRepoUriBuilder.build().toString(), "/");
    }

}
