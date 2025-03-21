package org.technologybrewery.habushu.util;

import org.apache.velocity.Template;
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
    private AbstractContainerizeDepsVelocityContext context;
    private String dockerfileContent;

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

    /**
     *  Generates values for variable placeholders in template based on the given Poetry or Uv context
     * @param context
     */
    public void setVelocityContext(AbstractContainerizeDepsVelocityContext context) {
        this.context = context;
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
