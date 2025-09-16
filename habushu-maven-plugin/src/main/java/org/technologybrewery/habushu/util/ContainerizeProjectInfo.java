package org.technologybrewery.habushu.util;

import org.apache.maven.project.MavenProject;

import java.nio.file.Path;

public class ContainerizeProjectInfo {
    private final Path projectPath;
    private Path wheelPath;
    private Path requirementsFilePath;

    public ContainerizeProjectInfo(MavenProject project) {
        this.projectPath = project.getBasedir().toPath();
    }

    public ContainerizeProjectInfo(Path projectPath) {
        this.projectPath = projectPath;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public void setWheelPath(Path wheelPath) {
        this.wheelPath = wheelPath;
    }

    public Path getWheelPath() {
        return this.wheelPath;
    }

    public String getWheelName() {
        return wheelPath.getFileName().toString();
    }

    public Path getRequirementsFilePath() {
        return requirementsFilePath;
    }

    public void setRequirementsFilePath(Path requirementsFilePath) {
        this.requirementsFilePath = requirementsFilePath;
    }
}
