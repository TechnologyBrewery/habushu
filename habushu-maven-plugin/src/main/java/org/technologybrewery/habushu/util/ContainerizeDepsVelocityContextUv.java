package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

public class ContainerizeDepsVelocityContextUv extends AbstractContainerizeDepsVelocityContext {
    private static final String UV_VERSION = "uvVersion";
    private static final String UV_PLUGIN_BUNDLE_VERSION = "uvPluginBundleVersion";
    private static final String UV_MONOREPO_DEPENDENCY_PLUGIN_VERSION = "uvMonorepoDependencyPluginVersion";

    @Override
    public void setVersion(String uvVersion) {
        put(UV_VERSION, uvVersion);
    }

    @Override
    public void setMonorepoDependencyPluginVersion(String dockerMonorepoDependencyPluginVersion) {
        put(UV_MONOREPO_DEPENDENCY_PLUGIN_VERSION, dockerMonorepoDependencyPluginVersion);
    }

    @Override
    public void setPluginBundleVersion(String dockerPluginBundleVersion) {
        put(UV_PLUGIN_BUNDLE_VERSION, dockerPluginBundleVersion);
    }
}
