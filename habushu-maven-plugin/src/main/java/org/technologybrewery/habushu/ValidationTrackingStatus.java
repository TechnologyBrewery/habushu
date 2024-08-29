package org.technologybrewery.habushu;

/**
 * Used to track the status of checks for Poetry and Python.
 */
class ValidationTrackingStatus {

    private boolean alreadyValidatedPoetryInstallation;
    private String alreadyValidatedPoetryVersion;
    private String priorActivePythonVersionActivated;

    public boolean isAlreadyValidatedPoetryInstallation() {
        return alreadyValidatedPoetryInstallation;
    }

    public void setAlreadyValidatedPoetryInstallation(boolean alreadyValidatedPoetryInstallation) {
        this.alreadyValidatedPoetryInstallation = alreadyValidatedPoetryInstallation;
    }

    public String getAlreadyValidatedPoetryVersion() {
        return alreadyValidatedPoetryVersion;
    }

    public void setAlreadyValidatedPoetryVersion(String alreadyValidatedPoetryVersion) {
        this.alreadyValidatedPoetryVersion = alreadyValidatedPoetryVersion;
    }

    public String getPriorActivePythonVersionActivated() {
        return priorActivePythonVersionActivated;
    }

    public void setPriorActivePythonVersionActivated(String priorActivePythonVersionActivated) {
        this.priorActivePythonVersionActivated = priorActivePythonVersionActivated;
    }
}
