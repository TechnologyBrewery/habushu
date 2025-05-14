package org.technologybrewery.habushu.util;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.ContainerizeDepsMojo;
import org.technologybrewery.habushu.HabushuException;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class ContainerizeDepsDockerfileHelper {
    public final ContainerizeDepsMojo containerizeDepsMojo;
    private final VelocityEngine engine;
    public static final String HABUSHU_FINAL_STAGE = "#HABUSHU_FINAL_STAGE";
    public static final String HABUSHU_BUILDER_STAGE = "#HABUSHU_BUILDER_STAGE";
    public static final String HABUSHU_COMMENT_START = " - HABUSHU GENERATED CODE (DO NOT MODIFY)";
    public static final String HABUSHU_COMMENT_END = " - HABUSHU GENERATED CODE (END)";
    private static final Logger log = LoggerFactory.getLogger(ContainerizeDepsDockerfileHelper.class);

    /**
     * This class can be initialized to generate and write a dockerfile for venv use
     */
    public ContainerizeDepsDockerfileHelper(ContainerizeDepsMojo containerizeDepsMojo) {
        this.containerizeDepsMojo = containerizeDepsMojo;
        this.engine = new VelocityEngine();

        File dockerTemplatePath = containerizeDepsMojo.getDockerTemplatePath();
        if (dockerTemplatePath != null) {
            setVelocityPropertiesForFilePath(dockerTemplatePath);
        } else {
            setVelocityPropertiesForClassPath();
        }
    }

    public void setVelocityPropertiesForClassPath() {
        engine.setProperty("resource.loader", "classpath");
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        engine.init();
    }

    public void setVelocityPropertiesForFilePath(File templateDir) {
        engine.setProperty("resource.loader", "file");
        engine.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        engine.setProperty("file.resource.loader.path", templateDir.getAbsolutePath());
        engine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        engine.init();
    }

    public void setSharedVelocityTemplateContext(AbstractContainerizeDepsVelocityContext context) {
        context.setAnchorDirectory();
        context.setSingleRepoProjectDir();
        context.setOwner();
        context.setVenvDirectoryPermissions();
    }

    public void setBuilderSharedContext(AbstractContainerizeDepsVelocityContext context) {
        setSharedVelocityTemplateContext(context);
        context.setBuilderBaseImage();
    }

    public void setFinalSharedContext(AbstractContainerizeDepsVelocityContext context) {
        setSharedVelocityTemplateContext(context);
        context.setFinalBaseImage();
    }

    public abstract String getBuilderStageContent();

    public abstract String getFinalStageContent();

    /**
     * Generate the dockerfile contents based on a given template
     *
     * @param context  VelocityContext
     * @param template the Poetry or uv template
     */
    public String createContainerStageContentFrom(VelocityContext context, String template) {
        Template vmTemplate = engine.getTemplate(template);
        StringWriter writer = new StringWriter();
        vmTemplate.merge(context, writer);
        return writer.toString();
    }

    private Boolean builderContentPresent(String dockerfileContent) {
        return dockerfileContent.contains(getBuilderStageStartComment());
    }

    private Boolean finalContentPresent(String dockerfileContent) {
        return dockerfileContent.contains(getFinalStageStartComment());
    }

    private Boolean addBuilderContentCommentPresent(String dockerfileContent) {
        return dockerfileContent.contains(HABUSHU_BUILDER_STAGE);
    }

    private Boolean addFinalContentCommentPresent(String dockerfileContent) {
        return dockerfileContent.contains(HABUSHU_FINAL_STAGE);
    }

    private String getBuilderStageStartComment() {
        return HABUSHU_BUILDER_STAGE + HABUSHU_COMMENT_START;
    }

    private String getBuilderStageEndComment() {
        return HABUSHU_BUILDER_STAGE + HABUSHU_COMMENT_END;
    }

    private String getFinalStageStartComment() {
        return HABUSHU_FINAL_STAGE + HABUSHU_COMMENT_START;
    }

    private String getFinalStageEndComment() {
        return HABUSHU_FINAL_STAGE + HABUSHU_COMMENT_END;
    }

    private String getWrappedBuilderStageContent(){
        return getWrappedBuilderStageContent(false);
    }

    private String getWrappedBuilderStageContent(Boolean includeNewLineAtEnd) {
        String builderStageContent = getBuilderStageContent();
        String wrappedBuilderStageContent = getBuilderStageStartComment() + "\n" + builderStageContent + "\n" + getBuilderStageEndComment();
        if (includeNewLineAtEnd) {
            return wrappedBuilderStageContent + "\n";
        }
        return wrappedBuilderStageContent;
    }

    private String getWrappedFinalStageContent(){
        return getWrappedFinalStageContent(false);
    }

    private String getWrappedFinalStageContent(Boolean includeNewLineAtEnd) {
        String finalStageContent = getFinalStageContent();
        String wrappedFinalStageContent = getFinalStageStartComment() + "\n" + finalStageContent + "\n" + getFinalStageEndComment();
        if (includeNewLineAtEnd) {
            return wrappedFinalStageContent + "\n";
        }
        return wrappedFinalStageContent;
    }

    private String updateDockerfileWithBuilderLogic() {
        try {
            String dockerfileContent = Files.readString(Paths.get(containerizeDepsMojo.getDockerfile().toString()));
            if (!builderContentPresent(dockerfileContent)) {
                if (!addBuilderContentCommentPresent(dockerfileContent)) {
                    log.warn("Adding builder stage Habushu-logic to beginning of the Dockerfile. We advise you review the updated Dockerfile.");
                    return getWrappedBuilderStageContent(true) + "\n" + dockerfileContent;
                } else {
                    return dockerfileContent.replace(HABUSHU_BUILDER_STAGE, getWrappedBuilderStageContent(true));
                }
            }
            return dockerfileContent;
        } catch (IOException e) {
            throw new HabushuException("Could not update Dockerfile with builder stage logic.", e);
        }
    }

    private String updateDockerfileWithFinalLogic(String dockerfileContent) {
        if (!finalContentPresent(dockerfileContent)) {
            if (!addFinalContentCommentPresent(dockerfileContent)) {
                log.warn("Adding final stage Habushu-logic to the end of the Dockerfile. We advise you review the updated Dockerfile.");
                return dockerfileContent + "\n\n" + getWrappedFinalStageContent();
            } else {
                return dockerfileContent.replace(HABUSHU_FINAL_STAGE, getWrappedFinalStageContent(true));
            }
        }
        return dockerfileContent;
    }

    /**
     * Update the Dockerfile with container stage logic
     *
     * @return updated Dockerfile content
     */
    public String updateDockerfileWithContainerStageLogic() {
        String dockerfileContent = updateDockerfileWithBuilderLogic();
        return updateDockerfileWithFinalLogic(dockerfileContent);
    }
}