package org.technologybrewery.habushu;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.UvUtil;

import java.io.File;
import java.util.List;

/**
 * Ensures uv-related pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations. These include:
 * <ul>
 * <li>uv (installed version must satisfy {@link UvUtil#UV_VERSION_REQUIREMENT})</li>
 * </ul>
 */
public class UvSetup extends AbstractPythonPackageAndDependencyManagerSetup {
   
    public UvSetup(String pythonVersion, boolean isPythonVersionConfigurationSet, String defaultPythonStrategy, File baseDir,
                   boolean rewriteLocalPathDepsInArchives, Log log) {
        super(pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir, rewriteLocalPathDepsInArchives, log);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String configurePythonUsingPackageAndDependencyManager() {
        UvCommandHelper uvHelper = createUvCommandHelper();
        String currentPythonVersion = uvHelper.getCurrentPythonVersion();

        if (useCurrentPythonVersion(currentPythonVersion)) {
            pythonVersion = currentPythonVersion;
        }

        // If the current python version does not match the desired version, update it to the desired version
        if (!StringUtils.equals(currentPythonVersion, pythonVersion)) {
            logger.info("The Python version is not currently set to the desired version. Setting the version...");
            uvHelper.updatePythonVersion(pythonVersion);
            currentPythonVersion = uvHelper.getCurrentPythonVersion();
        }
        return currentPythonVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> validatePackageAndDependencyManagerInstallationAndVersion(
        ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) {
        String alreadyValidatedVersion = validationTracker.getAlreadyValidatedVersion();
        if (!validationTracker.isAlreadyValidatedInstallation()) {
            log.debug("Checking if uv is installed...");
            UvCommandHelper uvHelper = createUvCommandHelper();
            Pair<Boolean, String> uvInstallStatusAndVersion = uvHelper.getIsUvInstalledAndVersion();

            if (Boolean.FALSE.equals(uvInstallStatusAndVersion.getLeft())) {
                missingRequiredToolMsgs.add(
                        "'uv' is not currently installed! Visit https://docs.astral.sh/uv/getting-started/installation/ for more information and installation options");
                return missingRequiredToolMsgs;
            } else {
                Semver uvVersionSemver = new Semver(uvInstallStatusAndVersion.getRight(), SemverType.NPM);
                if (!uvVersionSemver.satisfies(UvUtil.UV_VERSION_REQUIREMENT)) {
                    missingRequiredToolMsgs.add(String.format(
                            "uv version %s was installed - Habushu requires that installed version of uv satisfies %s.  Please update uv by executing 'uv self update' or visit https://docs.astral.sh/uv/getting-started/installation/ for more information",
                            uvInstallStatusAndVersion.getRight(), UvUtil.UV_VERSION_REQUIREMENT));
                    return missingRequiredToolMsgs;
                } else {
                    alreadyValidatedVersion = uvInstallStatusAndVersion.getRight();
                    validationTracker.setAlreadyValidatedVersion(alreadyValidatedVersion);
                    validationTracker.setAlreadyValidatedInstallation(true);
                    log.info("Found uv " + alreadyValidatedVersion);
                }
            }
        } else {
            alreadyValidatedVersion += VALIDATED_IN_PRIOR_BUILD_PHASE;
            log.info("Found uv " + alreadyValidatedVersion);
        }
        return missingRequiredToolMsgs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizePythonPackageAndDependencyManagerConfiguration() {
        // Currently no monorepo dependency plugin to install
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String pythonSourceMessage() {
        return "(managed by uv)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password) {
        // TODO update to include UV configuration with repos
    }

    /**
     * Creates a {@link UvCommandHelper} that may be used to invoke uv
     * commands from the project's working directory.
     *
     * @return command helper
     */
    protected UvCommandHelper createUvCommandHelper() {
        return new UvCommandHelper(baseDir);
    }
}
