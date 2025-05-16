package org.technologybrewery.habushu.util;

import org.technologybrewery.habushu.ContainerizeDepsMojo;

public class ContainerizeDepsVelocityContextUv extends AbstractContainerizeDepsVelocityContext {
    private static final String UV_VERSION = "uvVersion";

    public ContainerizeDepsVelocityContextUv(ContainerizeDepsMojo containerizeDepsMojo){
        super(containerizeDepsMojo);
    }

    @Override
    public void setVersion(String uvVersion) {
        put(UV_VERSION, uvVersion);
    }
}
