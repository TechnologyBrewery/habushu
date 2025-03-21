package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

public class ContainerizeDepsVelocityContextPoetry extends AbstractContainerizeDepsVelocityContext {
    private static final String POETRY_VERSION = "poetryVersion";
    private static final String POETRY_PLUGIN_BUNDLE_VERSION = "poetryPluginBundleVersion";
    private static final String POETRY_MONOREPO_DEPENDENCY_PLUGIN_VERSION = "poetryMonorepoDependencyPluginVersion";


    @Override
    public void setMonorepoDependencyPluginVersion(String poetryMonorepoDependencyPluginVersion) {
        put(POETRY_MONOREPO_DEPENDENCY_PLUGIN_VERSION, poetryMonorepoDependencyPluginVersion);
    }

    @Override
    public void setPluginBundleVersion(String poetryPluginBundleVersion) {
        put(POETRY_PLUGIN_BUNDLE_VERSION, poetryPluginBundleVersion);
    }

    @Override
    public void setVersion(String poetryVersion) {
        put(POETRY_VERSION, poetryVersion);
    }
}
