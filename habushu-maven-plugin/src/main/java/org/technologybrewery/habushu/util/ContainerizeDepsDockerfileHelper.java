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

public class ContainerizeDepsDockerfileHelper {
    private final ContainerizeDepsMojo containerizeDepsMojo;
    private final VelocityEngine engine;
    public static final String HABUSHU_FINAL_STAGE = "#HABUSHU_FINAL_STAGE";
    public static final String HABUSHU_BUILDER_STAGE = "#HABUSHU_BUILDER_STAGE";
    public static final String HABUSHU_COMMENT_START = " - HABUSHU GENERATED CODE (DO NOT MODIFY)";
    public static final String HABUSHU_COMMENT_END = " - HABUSHU GENERATED CODE (END)";
    private static final String BUILDER_STAGE = "BUILDER_STAGE";
    private static final String FINAL_STAGE = "FINAL_STAGE";
    private static final Logger log = LoggerFactory.getLogger(ContainerizeDepsDockerfileHelper.class);



    /**
     * This class can be initialized to generate and write a dockerfile for venv use
     */
    public ContainerizeDepsDockerfileHelper(ContainerizeDepsMojo containerizeDepsMojo) {
        this.containerizeDepsMojo = containerizeDepsMojo;
        this.engine = new VelocityEngine();

        File dockerTemplatePath = containerizeDepsMojo.getDockerTemplatePath();
        if (dockerTemplatePath != null){
            setVelocityPropertiesForFilePath(dockerTemplatePath);
        } else {
            setVelocityPropertiesForClassPath();
        }
    }

    private void setVelocityPropertiesForClassPath() {
        engine.setProperty("resource.loader", "classpath");
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        engine.init();
    }

    private void setVelocityPropertiesForFilePath(File templateDir) {
        engine.setProperty("resource.loader", "file");
        engine.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        engine.setProperty("file.resource.loader.path", templateDir.getAbsolutePath());
        engine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        engine.init();
    }

    private void setSharedVelocityTemplateContext(AbstractContainerizeDepsVelocityContext context,
                                                  String anchorDirectory, String singleRepoProjectDir, String owner,
                                                  String baseImage) {
        context.setAnchorDirectory(anchorDirectory);
        context.setSingleRepoProjectDir(singleRepoProjectDir);
        context.setOwner(owner);
        context.setBaseImage(baseImage);
    }

    private String getPoetryStageTemplate(String stage) {
        if (BUILDER_STAGE.equals(stage)){
            return containerizeDepsMojo.getDockerPoetryBuilderStageTemplatePath();
        } else {
            return containerizeDepsMojo.getDockerPoetryFinalStageTemplatePath();
        }
    }

    private String getUvStageTemplate(String stage) {
        if (BUILDER_STAGE.equals(stage)){
            return containerizeDepsMojo.getDockerUvBuilderStageTemplatePath();
        } else {
            return containerizeDepsMojo.getDockerUvFinalStageTemplatePath();
        }
    }

    private void setPoetryVelocityTemplateContext(ContainerizeDepsVelocityContextPoetry context,
                                                  String poetryPluginBundleVersion, String poetryVersion) {
        context.setPluginBundleVersion(poetryPluginBundleVersion);
        context.setVersion(poetryVersion);
    }

    private void setUvVelocityTemplateContext(ContainerizeDepsVelocityContextUv context,
                                                  String uvVersion) {
        context.setVersion(uvVersion);
    }

    private String getDockerTemplate(String stage, String anchorDirectory, String moduleBaseDir, String baseImage) {
        String template;
        if (PackageManager.POETRY.equals(containerizeDepsMojo.packageManager)) {
            ContainerizeDepsVelocityContextPoetry context = new ContainerizeDepsVelocityContextPoetry();
            setSharedVelocityTemplateContext(context, anchorDirectory, moduleBaseDir, containerizeDepsMojo.getDockerUser(), baseImage);
            setPoetryVelocityTemplateContext(context, containerizeDepsMojo.getDockerPoetryPluginBundleVersion(), containerizeDepsMojo.getDockerPoetryVersion());
            template = getPoetryStageTemplate(stage);
            return createContainerStageContentFrom(context, template);
        } else {
            ContainerizeDepsVelocityContextUv context = new ContainerizeDepsVelocityContextUv();
            setSharedVelocityTemplateContext(context, anchorDirectory, moduleBaseDir, containerizeDepsMojo.getDockerUser(), baseImage);
            setUvVelocityTemplateContext(context, containerizeDepsMojo.getDockerUvVersion());
            template = getUvStageTemplate(stage);
            return createContainerStageContentFrom(context, template);
        }
    }

    /**
     * Generate the dockerfile contents based on a given template
     * @param context VelocityContext
     * @param template the Poetry or uv template
     */
    public String createContainerStageContentFrom(VelocityContext context, String template){
        Template vmTemplate = engine.getTemplate(template);
        StringWriter writer = new StringWriter();
        vmTemplate.merge(context, writer);
        return writer.toString();
    }

    /**
     * Update the Dockerfile with container stage logic
     * @param anchorDirectory the anchor directory
     * @return updated Dockerfile content
     */
    public String updateDockerfileWithContainerStageLogic(String anchorDirectory, String moduleBaseDir) {

        String builderStageContent = getDockerTemplate(BUILDER_STAGE, anchorDirectory, moduleBaseDir, containerizeDepsMojo.getDockerBuilderBase());
        String finalStageContent = getDockerTemplate(FINAL_STAGE, null, moduleBaseDir, containerizeDepsMojo.getDockerFinalBase());
        StringBuilder content = new StringBuilder();
        boolean builderStageContentIncluded = false;
        boolean finalStageContentIncluded = false;
        int firstFromLine = -1;

        boolean skipLine = false;
        try (BufferedReader buffer = new BufferedReader(new FileReader(containerizeDepsMojo.getDockerfile()))) {
            String line = buffer.readLine();

            while (line != null) {
                line = line.stripTrailing();
                if(firstFromLine < 0 && line.strip().startsWith("FROM")) {
                    firstFromLine = content.length();
                }

                // start skipping the line if reads HABUSHU_COMMENT_START
                if (!skipLine && line.contains(HABUSHU_COMMENT_START)) {
                    skipLine = true;
                }

                // end skipping the line when reads HABUSHU_COMMENT_END
                if (skipLine && line.contains(HABUSHU_COMMENT_END)) {
                    skipLine = false;
                }

                if (!skipLine) {
                    if (line.contains(HABUSHU_BUILDER_STAGE)) {
                        line = wrapWithHabushuComment(builderStageContent, HABUSHU_BUILDER_STAGE);
                        builderStageContentIncluded = true;
                    }

                    if (line.contains(HABUSHU_FINAL_STAGE)) {
                        line = wrapWithHabushuComment(finalStageContent, HABUSHU_FINAL_STAGE);
                        finalStageContentIncluded = true;
                    }
                    content.append(line).append("\n");
                }
                line = buffer.readLine();
            }
            if (!builderStageContentIncluded) {
                content.insert(Integer.max(firstFromLine, 0), wrapWithHabushuComment(builderStageContent, HABUSHU_BUILDER_STAGE) + "\n\n");
            }
            if (!finalStageContentIncluded) {
                content.append("\n");
                content.append(wrapWithHabushuComment(finalStageContent, HABUSHU_FINAL_STAGE)).append("\n");
            }
        } catch (IOException e) {
            throw new HabushuException("Could not update Dockerfile with container stage logic.", e);
        }
        return content.toString();
    }

    private static String wrapWithHabushuComment(String content, String stage) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(stage).append(HABUSHU_COMMENT_START).append("\n");
        contentBuilder.append(content).append("\n");
        contentBuilder.append(stage).append(HABUSHU_COMMENT_END);
        return contentBuilder.toString();
    }
}
