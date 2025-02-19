package org.technologybrewery.habushu;

/**
 * Used to track the status of checks for Python and its package and dependency management tools.
 */
public class ValidationTrackingStatus {

    private boolean alreadyValidatedInstallation;
    private String alreadyValidatedVersion;
    private String activePythonVersion;

    public ValidationTrackingStatus() {
    }

    public ValidationTrackingStatus(boolean alreadyValidatedInstallation, String alreadyValidatedVersion, String activePythonVersion) {
        this.alreadyValidatedInstallation = alreadyValidatedInstallation;
        this.alreadyValidatedVersion = alreadyValidatedVersion;
        this.activePythonVersion = activePythonVersion;
    }

    public boolean isAlreadyValidatedInstallation() {
        return alreadyValidatedInstallation;
    }

    public void setAlreadyValidatedInstallation(boolean alreadyValidatedInstallation) {
        this.alreadyValidatedInstallation = alreadyValidatedInstallation;
    }

    public String getAlreadyValidatedVersion() {
        return alreadyValidatedVersion;
    }

    public void setAlreadyValidatedVersion(String alreadyValidatedVersion) {
        this.alreadyValidatedVersion = alreadyValidatedVersion;
    }

    public String getActivePythonVersion() {
        return activePythonVersion;
    }

    public void setActivePythonVersion(String activePythonVersion) {
        this.activePythonVersion = activePythonVersion;
    }
}
