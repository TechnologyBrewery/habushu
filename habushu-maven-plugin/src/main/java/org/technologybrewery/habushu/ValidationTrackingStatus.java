package org.technologybrewery.habushu;

/**
 * Used to track the status of checks for Python and its package and dependency management tools.
 */
class ValidationTrackingStatus {

    private boolean alreadyValidatedInstallation;
    private String alreadyValidatedVersion;
    private String ActivePythonVersion;

    public boolean isalreadyValidatedInstallation() {
        return alreadyValidatedInstallation;
    }

    public void setalreadyValidatedInstallation(boolean alreadyValidatedInstallation) {
        this.alreadyValidatedInstallation = alreadyValidatedInstallation;
    }

    public String getalreadyValidatedVersion() {
        return alreadyValidatedVersion;
    }

    public void setalreadyValidatedVersion(String alreadyValidatedVersion) {
        this.alreadyValidatedVersion = alreadyValidatedVersion;
    }

    public String getActivePythonVersion() {
        return ActivePythonVersion;
    }

    public void setActivePythonVersion(String ActivePythonVersion) {
        this.ActivePythonVersion = ActivePythonVersion;
    }
}
