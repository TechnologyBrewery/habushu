package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.technologybrewery.habushu.ContainerizeDepsMojo;

public abstract class AbstractContainerizeDepsVelocityContext extends VelocityContext {
    private final ContainerizeDepsMojo containerizeDepsMojo;
    private static final String SINGLE_REPO_PROJECT_DIR = "singleRepoProjectDir";
    private static final String BUILDER_BASE_IMAGE = "builderBaseImage";
    private static final String FINAL_BASE_IMAGE = "finalBaseImage";
    private static final String ANCHOR_DIRECTORY = "anchorDirectory";
    private static final String CHOWN = "chownPlaceholder";
    private static final String CHMOD = "chmodPlaceholder";

    public AbstractContainerizeDepsVelocityContext(ContainerizeDepsMojo containerizeDepsMojo){
        this.containerizeDepsMojo = containerizeDepsMojo;
    }

    public void setSingleRepoProjectDir() {
        put(SINGLE_REPO_PROJECT_DIR, containerizeDepsMojo.moduleBaseDir);
    }

    public void setBuilderBaseImage() {
        put(BUILDER_BASE_IMAGE, containerizeDepsMojo.getDockerBuilderBase());
    }

    public void setFinalBaseImage() {
        put(FINAL_BASE_IMAGE, containerizeDepsMojo.getDockerFinalBase());
    }

    public void setAnchorDirectory() {
        put(ANCHOR_DIRECTORY, containerizeDepsMojo.anchorDirectory);
    }

    public void setOwner() {
        String owner = containerizeDepsMojo.getDockerUser();
        if (StringUtils.isNotEmpty(owner)) {
            put(CHOWN, "--chown=" + owner);
        } else {
            put(CHOWN, StringUtils.EMPTY);
        }
    }

    public void setVenvDirectoryPermissions() {
        String permissions = containerizeDepsMojo.getDockerVenvDirectoryPermissions();
        if (StringUtils.isNotEmpty(permissions)) {
            put(CHMOD, "--chmod=" + permissions);
        } else {
            put(CHMOD, StringUtils.EMPTY);
        }
    }

    public abstract void setVersion(String version);

}
