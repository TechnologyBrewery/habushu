package org.technologybrewery.habushu.util;

import org.technologybrewery.habushu.HabushuException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ContainerizeDepsDockerfileHelper {
    public static final String HABUSHU_FINAL_STAGE = "#HABUSHU_FINAL_STAGE";
    public static final String HABUSHU_BUILDER_STAGE = "#HABUSHU_BUILDER_STAGE";
    public static final String HABUSHU_COMMENT_START = " - HABUSHU GENERATED CODE (DO NOT MODIFY)";
    public static final String HABUSHU_COMMENT_END = " - HABUSHU GENERATED CODE (END)";
    public static final String REPLACE_WITH_SINGLE_REPO_PROJECT_DIR = "REPLACE_WITH_SINGLE_REPO_PROJECT_DIR";
    public static final String ANCHOR_DIRECTORY = "ANCHOR_DIRECTORY";
    public static final String FINAL_STAGE_TEMPLATE = "dockerfile_final_stage_template";
    public static final String BUILDER_STAGE_TEMPLATE = "dockerfile_builder_stage_template";

    /**
     * Create the container stage content with given template, anchor directory and the single module's base directory
     * @param template container stage template name
     * @param anchorDirectory the anchor directory
     * @param moduleBaseDir the module base directory
     * @return container stage content
     */
    public static String createContainerStageContentFrom(String template, String anchorDirectory, String moduleBaseDir) {
        StringBuilder content = new StringBuilder();
        InputStream inputStream = ContainerizeDepsDockerfileHelper.class.getClassLoader().getResourceAsStream(template);

        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream); BufferedReader buffer = new BufferedReader(inputStreamReader)) {
            String line = buffer.readLine();

            while (line != null) {
                line = line.strip();
                if (line.contains(ANCHOR_DIRECTORY)) {
                    line = line.replaceAll(ANCHOR_DIRECTORY, anchorDirectory);
                }
                if (line.contains(REPLACE_WITH_SINGLE_REPO_PROJECT_DIR)) {
                    line = line.replaceAll(REPLACE_WITH_SINGLE_REPO_PROJECT_DIR, moduleBaseDir);
                }
                content.append(line).append("\n");
                line = buffer.readLine();
            }
            inputStream.close();
        } catch (IOException e) {
            throw new HabushuException("Could not read from file.", e);
        }
        return content.toString();
    }

    /**
     * Update the Dockerfile with container stage logic
     * @param dockerFile the Dockerfile to be updated
     * @param anchorDirectory the anchor directory
     * @param moduleBaseDir the module base directory
     * @return updated Dockerfile content
     */
    public static String updateDockerfileWithContainerStageLogic(File dockerFile, String anchorDirectory, String moduleBaseDir) {
        String builderStageContent = ContainerizeDepsDockerfileHelper.createContainerStageContentFrom(BUILDER_STAGE_TEMPLATE, anchorDirectory, moduleBaseDir);
        String finalStageContent = ContainerizeDepsDockerfileHelper.createContainerStageContentFrom(FINAL_STAGE_TEMPLATE, null, null);
        StringBuilder content = new StringBuilder();
        Boolean builderStageContentIncluded = false;
        Boolean finalStageContentIncluded = false;

        boolean skipLine = false;
        try (BufferedReader buffer = new BufferedReader(new FileReader(dockerFile))) {
            String line = buffer.readLine();

            while (line != null) {
                line = line.strip();

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
                content.insert(0, wrapWithHabushuComment(builderStageContent, HABUSHU_BUILDER_STAGE) + "\n\n");
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
        contentBuilder.append(content);
        contentBuilder.append(stage).append(HABUSHU_COMMENT_END);
        return contentBuilder.toString();
    }
}
