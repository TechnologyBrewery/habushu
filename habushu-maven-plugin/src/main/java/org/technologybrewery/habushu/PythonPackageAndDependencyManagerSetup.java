package org.technologybrewery.habushu;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations.
 */
public interface PythonPackageAndDependencyManagerSetup {

    void execute() throws MojoExecutionException;

    /**
     * Gets the current python version. It compares this with the desired python version and if it is different it
     * will pin project to that version. Desired python version is determined by the habushu config pythonVersion. If
     * that is not set then it will default to the current projects python version or the default habushu version
     * depending on the config defaultPythonStrategy
     *
     * @return the current Python version
     */
    String configurePythonUsingPackageAndDependencyManager() throws MojoExecutionException;

    /**
     * Validated weather required packages are installed and correctly versioned.
     * @param validationTracker tracks the current validation
     * @param missingRequiredToolMsgs contains the list of messages to display
     * @return updated missingRequiredToolMsgs list
     * @throws MojoExecutionException
     */
    List<String> validatePackageAndDependencyManagerInstallationAndVersion(ValidationTrackingStatus validationTracker, List<String> missingRequiredToolMsgs)
            throws MojoExecutionException;

    /**
     * Performs any required finalization steps
     */
    void finalizePythonPackageAndDependencyManagerConfiguration();

    /**
     * Gets the source of the message
     * @return the source of the message
     * @throws MojoExecutionException
     */
    String pythonSourceMessage() throws MojoExecutionException;

    /**
     * Registers repositories for dependency resolution
     * @param repoId the repository id
     * @param username the username for the repo
     * @param password the password for the repo
     */
    void registerRepositoryToSupportAuthenticatedDependencyResolution(String repoId, String username, String password);

    /**
     * When no explicit pythonVersion config is set, directs weather to use the existing projects python version or
     * the pom configs default value.
     * @param currentPythonVersion the projects current python version
     * @return true if the current projects python version should be used
     */
    boolean useCurrentPythonVersion(String currentPythonVersion);
}
