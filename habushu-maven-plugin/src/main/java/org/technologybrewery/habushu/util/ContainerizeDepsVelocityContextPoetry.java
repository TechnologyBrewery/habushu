package org.technologybrewery.habushu.util;

public class ContainerizeDepsVelocityContextPoetry extends AbstractContainerizeDepsVelocityContext {
    private static final String POETRY_VERSION = "poetryVersion";
    private static final String POETRY_PLUGIN_BUNDLE_VERSION = "poetryPluginBundleVersion";

    public void setPluginBundleVersion(String poetryPluginBundleVersion) {
        put(POETRY_PLUGIN_BUNDLE_VERSION, poetryPluginBundleVersion);
    }

    @Override
    public void setVersion(String poetryVersion) {
        put(POETRY_VERSION, poetryVersion);
    }
}
