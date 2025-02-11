package org.technologybrewery.habushu;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.UvUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Subclass of PythonPackageAndDependencyManagerSetup. Ensures uv-related pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations. These include:
 * <ul>
 * <li>uv (installed version must satisfy {@link UvUtil#UV_VERSION_REQUIREMENT})</li>
 * </ul>
 */
public class UvSetup extends AbstractPythonPackageAndDependencyManagerSetup {
   
    public UvSetup(String pythonVersion, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log) throws MojoExecutionException {
        super(pythonVersion, baseDir, rewriteLocalPathDepsInArchives, log); 
    }

    /**
     * Checks if the .python-version file exists. If it exists, it checks the listed python 
     * version and updates the listed version to the pythonVersion, if necessary. If the .python-version
     * file does not exisit, it throws a MojoExecutionException.
     */

    @Override
    protected String configurePythonUsingPackageAndDependencyManager(List<String> missingRequiredToolMsgs) throws MojoExecutionException {
        UvCommandHelper uvHelper = createUvCommandHelper();
        String currentPythonVersion = uvHelper.getCurrentPythonVersion();
            if (!pythonVersion.equals(currentPythonVersion)) {
                uvHelper.updatePythonVersion(pythonVersion);
                currentPythonVersion = uvHelper.getCurrentPythonVersion();
            }
        return currentPythonVersion;
    }

    @Override
    protected List<String> validatePackageAndDependencyManagerInstallationAndVersion(ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs) 
        throws MojoExecutionException {
            String alreadyValidatedUvVersion = validationTracker.getAlreadyValidatedUvVersion();
            UvCommandHelper uvHelper = createUvCommandHelper();
            if (!validationTracker.isAlreadyValidatedUvInstallation()) {
                log.debug("Checking if uv is installed...");
                Pair<Boolean, String> uvInstallStatusAndVersion = uvHelper.getIsUvInstalledAndVersion();
    
                if (!uvInstallStatusAndVersion.getLeft()) {
                    missingRequiredToolMsgs.add(
                            "'uv' is not currently installed! Visit https://docs.astral.sh/uv/getting-started/installation/ for more information and installation options");
                } else {
    
                    Semver uvVersionSemver = new Semver(uvInstallStatusAndVersion.getRight(), SemverType.NPM);
                    if (!uvVersionSemver.satisfies(UvUtil.UV_VERSION_REQUIREMENT)) {
                        missingRequiredToolMsgs.add(String.format(
                                "uv version %s was installed - Habushu requires that installed version of uv satisfies %s.  Please update uv by executing 'uv self update' or visit https://docs.astral.sh/uv/getting-started/installation/ for more information",
                                uvInstallStatusAndVersion.getRight(), UvUtil.UV_VERSION_REQUIREMENT));
                    } else {
                        alreadyValidatedUvVersion = uvInstallStatusAndVersion.getRight();
                        validationTracker.setAlreadyValidatedUvVersion(alreadyValidatedUvVersion);
                        validationTracker.setAlreadyValidatedUvInstallation(true);
                    }
                }
            } else {
                alreadyValidatedUvVersion += VALIDATED_IN_PRIOR_BUILD_PHASE;
            }
    
            log.info("Found uv " + alreadyValidatedUvVersion);
    
            return missingRequiredToolMsgs;
    }

    @Override
    protected void finalizePythonPackageAndDependencyManagerConfiguration() throws MojoExecutionException {
        // No additional configuration is necessary for uv at this time.
    };

    @Override
    protected String pythonSourceMessage() throws MojoExecutionException {
        String sourceMessage = "(managed by uv)";
        return sourceMessage;
    };

    @Override
    public void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password) throws MojoExecutionException, NotImplementedException {
        // TODO: move this method out of the abstract methods as this is specific to Poetry.
        // uv does not support a config command to store username and password credentials. Users must enter this information via the publish command.
    }

    /**
     * Creates a {@link UvCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     *
     * @return command helper
     */
    protected UvCommandHelper createUvCommandHelper() {
        return new UvCommandHelper(baseDir);
    }

    @Override
    public String findCurrentVirtualEnvironmentFullPath() throws MojoExecutionException {
        // TODO: move this method out of the abstract method as this method is specfic to poetry
        return StringUtils.EMPTY;
    };


}
