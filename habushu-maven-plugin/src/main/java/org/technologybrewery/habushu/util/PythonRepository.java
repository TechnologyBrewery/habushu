package org.technologybrewery.habushu.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.http.client.utils.URIBuilder;
import org.technologybrewery.habushu.HabushuException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Python package index, or repository.
 */
public class PythonRepository {
    public static final String SIMPLE_API_PATH = "simple";
    public static final String PUBLIC_PYPI_REPO_ID = "pypi";
    public static final String PUBLIC_PYPI_REPO_URL = "https://pypi.org/";
    public static final PythonRepository PUBLIC_PYPI_REPO = new PythonRepository(PUBLIC_PYPI_REPO_ID, PUBLIC_PYPI_REPO_URL);
    public static final String TEST_PYPI_REPO_ID = "dev-pypi";
    public static final String TEST_PYPI_REPO_URL = "https://test.pypi.org/";
    public static final PythonRepository TEST_PYPI_REPO = new PythonRepository(TEST_PYPI_REPO_ID, TEST_PYPI_REPO_URL, SIMPLE_API_PATH, "legacy");

    private String id;
    private URI baseUrl;
    private String indexPath;
    private String publishPath;

    /**
     * Constructs a repository with the default paths to adhere to the Simple Repository API. See the
     * <a href="https://packaging.python.org/en/latest/specifications/simple-repository-api">specification</a> for more
     * details.
     * @param id the Maven server ID for this repository
     * @param baseUrl the base URL of the repository on which the API paths are built
     */
    public PythonRepository(String id, String baseUrl) {
        this(id, baseUrl, SIMPLE_API_PATH, SIMPLE_API_PATH);
    }

    /**
     * Constructs a repository with custom paths for package look-up ({@code indexPath}) and package publishing
     * ({@code publishPath}). Typically, for a PEP-503-compliant repository that serves the Simple Repository API, the
     * path is "simple/" for both.  However, many repositories use the
     * <a href="https://docs.pypi.org/api/upload">"Legacy" upload API</a> for package publishing (e.g. test.pypi.org).
     * For these repositories, the publish path is "legacy/".
     * @param id the Maven server ID for this repository
     * @param baseUrl the base URL of the repository on which the API paths are built
     * @param indexPath the URL path for package look-up
     * @param publishPath the URL path for package publishing
     */
    public PythonRepository(String id, String baseUrl, String indexPath, String publishPath) {
        try {
            this.id = id;
            this.baseUrl = new URI(Strings.CS.removeEnd(baseUrl, "/"));
            setIndexPath(indexPath);
            setPublishPath(publishPath);
        } catch (URISyntaxException e) {
            throw new HabushuException("Invalid URL for Python repository: " + baseUrl, e);
        }
    }

    public String getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl.toString();
    }

    public void setPublishPath(String publishPath) {
        this.publishPath = normalizePathPart(publishPath);
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = normalizePathPart(indexPath);
    }


    /**
     * Returns the base URL joined with the index path for this repository, accounting for different input formats for
     * both (i.e. use or lack of leading/trailing forward slashes). If the base URL already ends with the index path, it
     * is simply returned after ensuring the slashes are correctly accounted for.
     *
     * @return package index URL
     */
    public String getIndexUrl() {
        String indexUrl = appendPathPart(baseUrl, indexPath);
        return HabushuUtil.addTrailingSlash(indexUrl);
    }

    /**
     * Returns the base URL joined with the publish path for this repository, accounting for different input formats for
     * both (i.e. use or lack of leading/trailing forward slashes). If the base URL already ends with the publish path,
     * it is simply returned after ensuring the slashes are correctly accounted for.
     *
     * @return package index URL
     */
    public String getPublishUrl() {
        String publishUrl = appendPathPart(baseUrl, publishPath);
        return HabushuUtil.addTrailingSlash(publishUrl);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PythonRepository)) {
            return false;
        }

        PythonRepository that = (PythonRepository) o;
        return baseUrl.equals(that.baseUrl);
    }

    @Override
    public int hashCode() {
        return baseUrl.hashCode();
    }

    private static String normalizePathPart(String pathPart) {
        pathPart = Strings.CS.removeStart(pathPart, "/");
        pathPart = Strings.CS.removeEnd(pathPart, "/");
        return pathPart;
    }

    private static String appendPathPart(URI base, String pathPart) {
        URIBuilder pypiRepoUriBuilder = new URIBuilder(base);
        List<String> repoUriPathSegments = pypiRepoUriBuilder.getPathSegments();
        String lastPathSegment = CollectionUtils.isNotEmpty(repoUriPathSegments)
                ? repoUriPathSegments.get(repoUriPathSegments.size() - 1)
                : null;
        if (StringUtils.isNotEmpty(pathPart) && !pathPart.equals(lastPathSegment)) {
            // If the URL has no path, an unmodifiable Collections.emptyList() is returned,
            // so wrap in an ArrayList to enable later modifications
            repoUriPathSegments = new ArrayList<>(repoUriPathSegments);
            repoUriPathSegments.add(pathPart);
            pypiRepoUriBuilder.setPathSegments(repoUriPathSegments);
        }
        return pypiRepoUriBuilder.toString();
    }
}
