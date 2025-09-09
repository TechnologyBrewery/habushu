package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerizeDepsDockerfileHelper {
    public final ContainerizeDepsMojo containerizeDepsMojo;
    private final List<String> orderedProjectWheels;
    private final VelocityEngine engine;
    public static final String HABUSHU_FINAL_STAGE = "#HABUSHU_FINAL_STAGE";
    public static final String HABUSHU_BUILDER_STAGE = "#HABUSHU_BUILDER_STAGE";
    public static final String HABUSHU_COMMENT_START = " - HABUSHU GENERATED CODE (DO NOT MODIFY)";
    public static final String HABUSHU_COMMENT_END = " - HABUSHU GENERATED CODE (END)";
    private static final Logger log = LoggerFactory.getLogger(ContainerizeDepsDockerfileHelper.class);

    /**
     * This class can be initialized to generate and write a dockerfile for venv use
     */
    public ContainerizeDepsDockerfileHelper(ContainerizeDepsMojo containerizeDepsMojo, List<String> orderedProjectWheels) {
        this.containerizeDepsMojo = containerizeDepsMojo;
        this.orderedProjectWheels = orderedProjectWheels;
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

    public void setSharedVelocityTemplateContext(ContainerizeDepsVelocityContext context) {
        context.setStagingDirectory();
        context.setOwner();
        context.setVenvDirectoryPermissions();
        context.setRepositoryUrls();
        context.setProjectWheels(orderedProjectWheels);
        context.setExtraWheels();
    }

    public void setBuilderSharedContext(ContainerizeDepsVelocityContext context) {
        setSharedVelocityTemplateContext(context);
        context.setBuilderBaseImage();
    }

    public void setFinalSharedContext(ContainerizeDepsVelocityContext context) {
        setSharedVelocityTemplateContext(context);
        context.setFinalBaseImage();
    }

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

    private String getBuilderStageContent() {
        ContainerizeDepsVelocityContext context = new ContainerizeDepsVelocityContext(containerizeDepsMojo);
        setBuilderSharedContext(context);
        String builderStageContent = createContainerStageContentFrom(context, containerizeDepsMojo.getDockerBuilderStageTemplate());
        String wrappedBuilderStageContent = getBuilderStageStartComment() + "\n" + builderStageContent + "\n" + getBuilderStageEndComment();
        return wrappedBuilderStageContent;
    }

    private String getFinalStageContent(){
        ContainerizeDepsVelocityContext context = new ContainerizeDepsVelocityContext(containerizeDepsMojo);
        setFinalSharedContext(context);
        String finalStageContent = createContainerStageContentFrom(context, containerizeDepsMojo.getDockerFinalStageTemplate());
        return getFinalStageStartComment() + "\n" + finalStageContent + "\n" + getFinalStageEndComment();
    }

    private String updateDockerfileWithBuilderLogic(List<String> content) {
        int insertionPointGuess = 0;
        return updateDockerfileWithStage(content, HABUSHU_BUILDER_STAGE, getBuilderStageContent(), insertionPointGuess);
    }

    private String updateDockerfileWithFinalLogic(List<String> content) {
        int insertionPointGuess = content.size();
        return updateDockerfileWithStage(content, HABUSHU_FINAL_STAGE, getFinalStageContent(), insertionPointGuess);
    }

    private String updateDockerfileWithStage(List<String> content, String stageTag, String stageContent, int insertionPointIfNoTag) {
        if (content.contains(stageTag + HABUSHU_COMMENT_START)) {
            return replaceExistingLogic(content, stageTag, stageContent);
        } else if (content.contains(stageTag)) {
            return replaceInjectionTag(content, stageTag, stageContent);
        } else {
            log.warn("Guessing injection location for Habushu-logic in Dockerfile. We advise you review the updated Dockerfile.");
            return replaceContents(content, stageContent, insertionPointIfNoTag, insertionPointIfNoTag);
        }
    }

    private static String replaceExistingLogic(List<String> content, String stageTag, String stageContent) {
        int replaceStart = content.indexOf(stageTag + HABUSHU_COMMENT_START);
        int replaceEnd = content.indexOf(stageTag + HABUSHU_COMMENT_END) + 1;
        return replaceContents(content, stageContent, replaceStart, replaceEnd);
    }

    private static String replaceInjectionTag(List<String> content, String stageTag, String stageContent) {
        int replaceStart = content.indexOf(stageTag);
        int replaceEnd = replaceStart + 1;
        return replaceContents(content, stageContent, replaceStart, replaceEnd);
    }

    /**
     * Replaces lines in `content` with new content. If `replaceStart` == `replaceEnd`, then the content is inserted at
     * the specified line number
     *
     * @param content the existing content
     * @param newContent the new content to add
     * @param replaceStart the starting line to replace (inclusive)
     * @param replaceEnd the ending replacement line (exclusive)
     *
     * @return the updated concatenated content
     */
    private static String replaceContents(List<String> content, String newContent, int replaceStart, int replaceEnd) {
        content.subList(replaceStart, replaceEnd).clear(); //the sublist is empty if start==end
        content.add(replaceStart, newContent);
        return String.join("\n", content);
    }

    /**
     * Update the Dockerfile with container stage logic
     *
     * @return updated Dockerfile content
     */
    public String updateDockerfileWithContainerStageLogic() {
        Path dockerfilePath = containerizeDepsMojo.getDockerfile().toPath();
        try(Stream<String> lines = Files.lines(dockerfilePath)) {
            LinkedList<String> content = lines
                    .map( l -> StringUtils.stripEnd(l, null))
                    .peek(l -> log.debug("Dockerfile line: {}", l))
                    .collect(Collectors.toCollection(LinkedList::new));
            updateDockerfileWithBuilderLogic(content);
            updateDockerfileWithFinalLogic(content);
            return String.join("\n", content);
        } catch (IOException e) {
            throw new HabushuException("Could not update Dockerfile with builder stage logic.", e);
        }
    }
}