package org.technologybrewery.habushu.util;

public class VersionExtraTuple {
    private String version;

    private String extra;

    public VersionExtraTuple(String version, String extra) {
        this.version = version;
        this.extra = extra;

    }

    public String getVersion() {
        return version;
    }

    public String getExtra() {
        return extra;
    }
}
