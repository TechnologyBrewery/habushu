package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

public abstract class AbstractContainerizeDepsVelocityContext extends VelocityContext {
    private static final String SINGLE_REPO_PROJECT_DIR = "singleRepoProjectDir";
    private static final String BASE_IMAGE = "baseImage";
    private static final String ANCHOR_DIRECTORY = "anchorDirectory";
    private static final String CHOWN = "chownPlaceholder";

    public void setSingleRepoProjectDir(String singleRepoProjectDir) {
        put(SINGLE_REPO_PROJECT_DIR, singleRepoProjectDir);
    }

    public void setBaseImage(String baseImage) {
        put(BASE_IMAGE, baseImage);
    }

    public void setAnchorDirectory(String anchorDirectory) {
        put(ANCHOR_DIRECTORY, anchorDirectory);
    }

    public void setOwner(String owner) {
        if (StringUtils.isNotEmpty(owner)) {
            put(CHOWN, "--chown=" + owner);
        } else {
            put(CHOWN, StringUtils.EMPTY);
        }
    }

    public abstract void setVersion(String version);

}
