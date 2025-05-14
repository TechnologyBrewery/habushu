package org.technologybrewery.habushu.util;

import org.technologybrewery.habushu.ContainerizeDepsMojo;

public class PoetryContainerizeDepsDockerfileHelper extends ContainerizeDepsDockerfileHelper {

    public PoetryContainerizeDepsDockerfileHelper(ContainerizeDepsMojo containerizeDepsMojo) {
        super(containerizeDepsMojo);
    }

    private void setPoetryVelocityTemplateContext(ContainerizeDepsVelocityContextPoetry context) {
        context.setPluginBundleVersion(containerizeDepsMojo.getDockerPoetryPluginBundleVersion());
        context.setVersion(containerizeDepsMojo.getDockerPoetryVersion());
    }

    @Override
    public String getBuilderStageContent() {
        ContainerizeDepsVelocityContextPoetry context = new ContainerizeDepsVelocityContextPoetry(containerizeDepsMojo);
        setBuilderSharedContext(context);
        setPoetryVelocityTemplateContext(context);
        return createContainerStageContentFrom(context, containerizeDepsMojo.getDockerPoetryBuilderStageTemplatePath());
    }

    @Override
    public String getFinalStageContent() {
        ContainerizeDepsVelocityContextPoetry context = new ContainerizeDepsVelocityContextPoetry(containerizeDepsMojo);
        setFinalSharedContext(context);
        setPoetryVelocityTemplateContext(context);
        return createContainerStageContentFrom(context, containerizeDepsMojo.getDockerPoetryFinalStageTemplatePath());
    }


}
