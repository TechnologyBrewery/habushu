package org.technologybrewery.habushu.util;

import org.technologybrewery.habushu.ContainerizeDepsMojo;

public class UvContainerizeDepsDockerfileHelper extends ContainerizeDepsDockerfileHelper {

    public UvContainerizeDepsDockerfileHelper(ContainerizeDepsMojo containerizeDepsMojo) {
        super(containerizeDepsMojo);
    }

    private void setUvVelocityTemplateContext(ContainerizeDepsVelocityContextUv context) {
        context.setVersion(containerizeDepsMojo.getDockerUvVersion());
    }

    @Override
    public String getBuilderStageContent() {
        ContainerizeDepsVelocityContextUv context = new ContainerizeDepsVelocityContextUv(containerizeDepsMojo);
        setBuilderSharedContext(context);
        setUvVelocityTemplateContext(context);
        return createContainerStageContentFrom(context, containerizeDepsMojo.getDockerUvBuilderStageTemplatePath());
    }

    @Override
    public String getFinalStageContent() {
        ContainerizeDepsVelocityContextUv context = new ContainerizeDepsVelocityContextUv(containerizeDepsMojo);
        setFinalSharedContext(context);
        setUvVelocityTemplateContext(context);
        return createContainerStageContentFrom(context, containerizeDepsMojo.getDockerUvFinalStageTemplatePath());
    }
}
