package org.technologybrewery.habushu.util;

public class TomlReplacementTuple {
    private String packageName;

    private String originalOperatorAndVersion;

    private String updatedOperatorAndVersion;

    public TomlReplacementTuple(String packageName, String originalOperatorAndVersion, String updatedOperatorAndVersion) {
        this.packageName = packageName;
        this.originalOperatorAndVersion = originalOperatorAndVersion;
        this.updatedOperatorAndVersion = updatedOperatorAndVersion;

    }

    public String getPackageName() {
        return packageName;
    }

    public String getOriginalOperatorAndVersion() {
        return originalOperatorAndVersion;
    }

    public String getUpdatedOperatorAndVersion() {
        return updatedOperatorAndVersion;
    }
}
