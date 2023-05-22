package org.technologybrewery.habushu;

public class PackageDefinition {
    private String packageName;
    private String operatorAndVersion;

    private boolean active = true;

    public PackageDefinition() {
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getOperatorAndVersion() {
        return operatorAndVersion;
    }

    public void setOperatorAndVersion(String operatorAndVersion) {
        this.operatorAndVersion = operatorAndVersion;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}