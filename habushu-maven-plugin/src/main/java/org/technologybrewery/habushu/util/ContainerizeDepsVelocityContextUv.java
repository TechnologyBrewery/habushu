package org.technologybrewery.habushu.util;

public class ContainerizeDepsVelocityContextUv extends AbstractContainerizeDepsVelocityContext {
    private static final String UV_VERSION = "uvVersion";

    @Override
    public void setVersion(String uvVersion) {
        put(UV_VERSION, uvVersion);
    }
}
