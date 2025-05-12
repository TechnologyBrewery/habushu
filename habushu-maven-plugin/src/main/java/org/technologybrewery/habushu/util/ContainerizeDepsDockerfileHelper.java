package org.technologybrewery.habushu.util;

import org.apache.velocity.VelocityContext;
import org.technologybrewery.habushu.HabushuException;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

public class ContainerizeDepsDockerfileHelper {
    public static final String HABUSHU_FINAL_STAGE = "#HABUSHU_FINAL_STAGE";
    public static final String HABUSHU_BUILDER_STAGE = "#HABUSHU_BUILDER_STAGE";
    public static final String HABUSHU_COMMENT_START = " - HABUSHU GENERATED CODE (DO NOT MODIFY)";
    public static final String HABUSHU_COMMENT_END = " - HABUSHU GENERATED CODE (END)";
    public static final String POETRY_FINAL_STAGE_TEMPLATE = "templates/dockerfile_poetry_final_stage_template.vm";
    public static final String POETRY_BUILDER_STAGE_TEMPLATE = "templates/dockerfile_poetry_builder_stage_template.vm";
    public static final String UV_FINAL_STAGE_TEMPLATE = "templates/dockerfile_uv_final_stage_template.vm";
    public static final String UV_BUILDER_STAGE_TEMPLATE = "templates/dockerfile_uv_builder_stage_template.vm";
    public static final String BUILDER_STAGE = "BUILDER_STAGE";
    public static final String FINAL_STAGE = "FINAL_STAGE";


    private final VelocityEngine engine;

    /**
     * This class can be initialized to generate and write a dockerfile for venv use
     */
    public ContainerizeDepsDockerfileHelper() {
        Properties props = new Properties();
        props.setProperty("resource.loader", "classpath");
        props.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        props.setProperty("file.resource.loader.path", "src/main/resources/templates"); // Directory containing Dockerfile templates
        engine = new VelocityEngine(props);
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
            return POETRY_BUILDER_STAGE_TEMPLATE;
        } else {
            return POETRY_FINAL_STAGE_TEMPLATE;
        }
    }

    private String getUvStageTemplate(String stage) {
        if (BUILDER_STAGE.equals(stage)){
            return UV_BUILDER_STAGE_TEMPLATE;
        } else {
            return UV_FINAL_STAGE_TEMPLATE;
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

    private String getDockerTemplate(PackageManager packageManager, String stage, String anchorDirectory, String moduleBaseDir,
                                                  String owner, String baseImage, String poetryVersion,
                                                  String poetryPluginBundleVersion, String uvVersion) {
        String template;
        if (PackageManager.POETRY.equals(packageManager)) {
            ContainerizeDepsVelocityContextPoetry context = new ContainerizeDepsVelocityContextPoetry();
            setSharedVelocityTemplateContext(context, anchorDirectory, moduleBaseDir, owner, baseImage);
            setPoetryVelocityTemplateContext(context, poetryPluginBundleVersion, poetryVersion);
            template = getPoetryStageTemplate(stage);
            return createContainerStageContentFrom(context, template);
        } else {
            ContainerizeDepsVelocityContextUv context = new ContainerizeDepsVelocityContextUv();
            setSharedVelocityTemplateContext(context, anchorDirectory, moduleBaseDir, owner, baseImage);
            setUvVelocityTemplateContext(context, uvVersion);
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
     * @param dockerFile the Dockerfile to be updated
     * @param anchorDirectory the anchor directory
     * @param moduleBaseDir the module base directory
     * @param owner the uid/name to give ownership of the virtual env to within the container
     * @param builderBaseImage the image to use for bundling the virtual environment
     * @param finalBaseImage the image to use for running the virtual environment
     * @return updated Dockerfile content
     */
    public String updateDockerfileWithContainerStageLogic(PackageManager packageManager, File dockerFile, String anchorDirectory, String moduleBaseDir, String owner, String builderBaseImage, String finalBaseImage, String poetryVersion, String poetryPluginBundleVersion, String uvVersion) {

        String builderStageContent = getDockerTemplate(packageManager, BUILDER_STAGE, anchorDirectory, moduleBaseDir, owner, builderBaseImage, poetryVersion, poetryPluginBundleVersion, uvVersion);
        String finalStageContent = getDockerTemplate(packageManager, FINAL_STAGE, null, moduleBaseDir, owner, finalBaseImage, null, null, null);
        StringBuilder content = new StringBuilder();
        boolean builderStageContentIncluded = false;
        boolean finalStageContentIncluded = false;
        int firstFromLine = -1;

        boolean skipLine = false;
        try (BufferedReader buffer = new BufferedReader(new FileReader(dockerFile))) {
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
