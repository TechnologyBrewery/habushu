package org.technologybrewery.habushu;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.File;

public class PoetryCommandHelperTestWrapper extends PoetryCommandHelper {
    private final String mockPoetryVersion;
    private static final String BREAKING_POETRY_VERSION = "2.0.0";

    public PoetryCommandHelperTestWrapper(File file, String mockPoetryVersion) {
        super(file);
        this.mockPoetryVersion = mockPoetryVersion;
    }

    @Override
    public Pair<Boolean, String> getIsPoetryInstalledAndVersion() {
        return new ImmutablePair<>(true, mockPoetryVersion);
    }

    @Override
    public boolean isPoetryVersionAtLeast2(){
        DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(mockPoetryVersion);
        DefaultArtifactVersion minimumVersion = new DefaultArtifactVersion(BREAKING_POETRY_VERSION);
        return currentVersion.compareTo(minimumVersion) >= 0;
    }
}
