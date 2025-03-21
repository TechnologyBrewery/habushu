package org.technologybrewery.habushu.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.technologybrewery.habushu.HabushuException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ContainerizeDepsDockerfileHelper {
    private final VelocityEngine engine;
    private final VelocityContext context;
    private String dockerfileContent;
    private static final String SINGLE_REPO_PROJECT_DIR = "singleRepoProjectDir";
    private static final String CHOWN = "chownPlaceholder";
    private static final String BASE_IMAGE = "baseImage";
    private static final String FINAL_BASE_IMAGE = "finalBaseImage";
    private static final String ANCHOR_DIRECTORY = "anchorDirectory";
    private static final String POETRY_VERSION = "poetryVersion";
    private static final String POETRY_PLUGIN_BUNDLE_VERSION = "poetryPluginBundleVersion";
    private static final String POETRY_MONOREPO_DEPENDENCY_PLUGIN_VERSION = "poetryMonorepoDependencyPluginVersion";

    /**
     * This class can be initialized to generate and write a dockerfile for venv use
     */
    public ContainerizeDepsDockerfileHelper() {
        Properties props = new Properties();
        props.setProperty("resource.loader", "classpath");
        props.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        props.setProperty("file.resource.loader.path", "src/main/resources/templates"); // Directory containing Dockerfile templates
        context = new VelocityContext();
        engine = new VelocityEngine(props);
        engine.init();
    }

    /**
     * Update the Dockerfile with container stage logic
     * @param anchorDirectory the anchor directory
     * @param moduleBaseDir the module base directory
     * @param owner the uid/name to give ownership of the virtual env to within the container
     * @param baseImage the builder image to use for bundling the virtual environment
     * @param finalBaseImage the final image to use for running the virtual environment
     * @param poetryVersion the version of poetry to use for running the virtual envrionment
     * @param poetryMonorepoDependencyPluginVersion the version of the monorepo dependency plugin to use
     * @param poetryPluginBundleVersion the version of the poetry plugin bundle to use
     * @return updated Dockerfile content
     */
    public void setVelocityContext(
            String anchorDirectory,
            String moduleBaseDir,
            String owner,
            String baseImage,
            String finalBaseImage,
            String poetryVersion,
            String poetryMonorepoDependencyPluginVersion,
            String poetryPluginBundleVersion) {

        context.put(BASE_IMAGE, baseImage);
        context.put(FINAL_BASE_IMAGE, finalBaseImage);
        context.put(POETRY_VERSION, poetryVersion);
        context.put(POETRY_MONOREPO_DEPENDENCY_PLUGIN_VERSION, poetryMonorepoDependencyPluginVersion);
        context.put(POETRY_PLUGIN_BUNDLE_VERSION, poetryPluginBundleVersion);
        context.put(ANCHOR_DIRECTORY, anchorDirectory);
        context.put(SINGLE_REPO_PROJECT_DIR, moduleBaseDir);

        if (StringUtils.isNotEmpty(owner)) {
            String chown = new StringBuilder()
                    .append("--chown=")
                    .append(owner)
                    .toString();
            context.put(CHOWN, chown);
        } else {
            context.put(CHOWN, StringUtils.EMPTY);
        }
    }

    /**
     * Generate the dockerfile contents based on a given template
     * @param template
     */
    public void generateDockerfileContent(String template) {
        Template vmTemplate = engine.getTemplate(template);
        StringWriter writer = new StringWriter();
        vmTemplate.merge(context, writer);

        dockerfileContent = writer.toString();
    }

    /**
     * Write the docker file to the given output path
     * @param outputPath
     */
    public void writeDockerfile(Path outputPath) {
        try {
            Files.write(outputPath, dockerfileContent.getBytes());
        } catch (IOException e) {
            throw new HabushuException(String.format("Could not write generated dockerfile contents to %s", outputPath));
        }
    }
}
