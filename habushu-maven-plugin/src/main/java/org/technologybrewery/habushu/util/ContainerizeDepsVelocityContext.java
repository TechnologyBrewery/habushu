package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.technologybrewery.habushu.ContainerizeDepsMojo;

import java.util.List;

public class ContainerizeDepsVelocityContext extends VelocityContext {
    private static final String STAGING_DIRECTORY = "stagingDirectory";
    private static final String PROJECT_WHEELS = "projectWheels";
    private static final String EXTRA_WHEELS = "extraWheels";
    private static final String BUILDER_BASE_IMAGE = "builderBaseImage";
    private static final String FINAL_BASE_IMAGE = "finalBaseImage";
    private static final String CHOWN = "chownPlaceholder";
    private static final String CHMOD = "chmodPlaceholder";

    private final ContainerizeDepsMojo containerizeDepsMojo;

    public ContainerizeDepsVelocityContext(ContainerizeDepsMojo containerizeDepsMojo){
        this.containerizeDepsMojo = containerizeDepsMojo;
    }

    public void setBuilderBaseImage() {
        put(BUILDER_BASE_IMAGE, containerizeDepsMojo.getDockerBuilderBase());
    }

    public void setFinalBaseImage() {
        put(FINAL_BASE_IMAGE, containerizeDepsMojo.getDockerFinalBase());
    }

    public void setStagingDirectory() {
        put(STAGING_DIRECTORY, containerizeDepsMojo.getStagingDirectoryRelativeToContext());
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

    public void setProjectWheels(List<String> orderedProjectWheels) {
        put(PROJECT_WHEELS, orderedProjectWheels);
    }

    public void setExtraWheels() {
        put(EXTRA_WHEELS, containerizeDepsMojo.getExtraWheels());
    }
}
