package org.technologybrewery.habushu;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.File;

public class PoetryCommandHelperTestWrapper extends PoetryCommandHelper {
    private final String mockPoetryVersion;

    public PoetryCommandHelperTestWrapper(File workingDirectory, String mockPoetryVersion) {
        super(workingDirectory);
        this.mockPoetryVersion = mockPoetryVersion;
    }

    @Override
    public Pair<Boolean, String> getIsPoetryInstalledAndVersion() {
        return new ImmutablePair<>(true, mockPoetryVersion);
    }
}
