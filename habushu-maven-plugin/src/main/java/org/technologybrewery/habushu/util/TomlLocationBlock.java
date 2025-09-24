package org.technologybrewery.habushu.util;

public class TomlLocationBlock {
    private String keyName;
    private int startLine;
    private int endLine;

    public TomlLocationBlock(String keyName, int startLine, int endLine) {
        this.keyName = keyName;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getKeyName() {
        return keyName;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

}

