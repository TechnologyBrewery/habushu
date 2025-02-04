package org.technologybrewery.habushu;

/**
 * Used to track the status of checks for Python and its package and dependency management tools.
 */
class ValidationTrackingStatus {

    private boolean alreadyValidatedPoetryInstallation;
    private String alreadyValidatedPoetryVersion;
    private boolean alreadyValidatedUvInstallation;
    private String alreadyValidatedUvVersion;
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

    public boolean isAlreadyValidatedUvInstallation() {
        return alreadyValidatedUvInstallation;
    }

    public void setAlreadyValidatedUvInstallation(boolean alreadyValidatedUvInstallation) {
        this.alreadyValidatedUvInstallation = alreadyValidatedUvInstallation;
    }

    public String getAlreadyValidatedUvVersion() {
        return alreadyValidatedUvVersion;
    }

    public void setAlreadyValidatedUvVersion(String alreadyValidatedUvVersion) {
        this.alreadyValidatedUvVersion = alreadyValidatedUvVersion;
    }

    public String getPriorActivePythonVersionActivated() {
        return priorActivePythonVersionActivated;
    }

    public void setPriorActivePythonVersionActivated(String priorActivePythonVersionActivated) {
        this.priorActivePythonVersionActivated = priorActivePythonVersionActivated;
    }
}
