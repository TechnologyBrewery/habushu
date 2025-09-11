package org.technologybrewery.habushu;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.ContainerizeDepsDockerfileHelper;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Stages the source files of a monorepo dependency
 * to the target directory along with the source files of
 * any transitive path-based dependencies.
 */
@Mojo(name = "containerize-dependencies", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class ContainerizeDepsMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(ContainerizeDepsMojo.class);
    private PackageManager packageManager;

    @Component
    protected MavenSession session;

    /**
     * The directory in which the collected Python project files will be staged for containerization.
     */
    @Parameter(defaultValue = "${project.build.directory}/containerize-support", property = "habushu.stagingDirectory")
    protected File stagingDirectory;

    /**
     * Controls whether a Dockerfile is updated with logic to copy and build the Habushu project and its dependencies
     * within the container. If set to false, the Dockerfile will not be updated.
     */
    @Parameter(defaultValue = "true", property = "habushu.updateDockerfile")
    protected boolean updateDockerfile;

    /**
     * Dockerfile to be updated with containerization logic. Must be set if `updateDockerfile` is true.
     */
    @Parameter(property = "habushu.dockerfile")
    protected File dockerfile;

    /**
     * The directory that will serve as the context for the Docker build. This directory must contain the `stagingDirectory`.
     * Defaults to the project's base directory.
     */
    @Parameter(defaultValue = "${project.basedir}", property = "habushu.dockerContext")
    protected File dockerContext;

    /**
     * The user to set as the owner of the virtual env. This is useful when the Docker build is run as a non-root user.
     * Set to an empty string to disable.
     */
    @Parameter(defaultValue = "1001", property = "habushu.dockerUser")
    protected String dockerUser;

    /**
     * The directory permissions for the virtual env.
     * Set to an empty string to disable.
     */
    @Parameter(defaultValue = "744", property = "habushu.dockerVenvDirectoryPermissions")
    protected String dockerVenvDirectoryPermissions;

    /**
     * Overwrite with Docker template path if a custom template is preferred.
     */
    @Parameter(property = "habushu.dockerTemplatePath")
    protected File dockerTemplatePath;


    /**
     * The default Dockerfile builder stage template for Poetry. Overwrite if a custom template is preferred.
     */
    @Parameter(defaultValue = "templates/dockerfile_builder_stage_template.vm", property = "habushu.dockerBuilderStageTemplate")
    protected String dockerBuilderStageTemplate;

    /**
     * The default Dockerfile final stage template for Poetry. Overwrite if a custom template is preferred.
     */
    @Parameter(defaultValue = "templates/dockerfile_final_stage_template.vm", property = "habushu.dockerFinalStageTemplate")
    protected String dockerFinalStageTemplate;

    /**
     * The base image to use for building the virtual env. This base image will be used to bundle the virtual
     * environment for the target project. As the venv must be built on the same platform as the final runtime to ensure
     * compatibility, this image must share a platform with {@link #dockerFinalBase}. The base image must have the target
     * Python version resolvable via the PATH.
     */
    @Parameter(defaultValue = "docker.io/python:3.12", property = "habushu.dockerBuilderBase")
    protected String dockerBuilderBase;

    /**
     * The base image to use for final packaging of the virtual env. This base image will be used to run the final
     * container runtime.  As the venv must be built on the same platform as the final runtime to ensure compatibility,
     * this image must share a platform with {@link #dockerBuilderBase}. The base image must have the target Python
     * version resolvable via the PATH.
     */
    @Parameter(defaultValue = "docker.io/python:3.12-slim", property = "habushu.dockerFinalBase")
    protected String dockerFinalBase;

    /**
     * Overriding to allow execution in non-habushu projects.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        Path sourceRoot = Path.of(session.getExecutionRootDirectory());
        ProjectStack result = getHabushuProjects();
        try {
            setPackageManager(result.getPrimaryProject());
            stageHabushuProjects(sourceRoot, result);
            if (this.updateDockerfile) {
                if (this.dockerfile == null) {
                    throw new HabushuException("`updateDockerfile` is set to true but `dockerfile` is not specified");
                }
                performDockerfileUpdateForVirtualEnvironment(result);
            }
        } catch (IOException e) {
            throw new HabushuException("Failed to prepare containerization of Habushu dependency", e);
        }
    }

    /**
     * Copies the relevant source files by leveraging {@link FileSet}s to filter appropriately.
     *
     * @param sourceRoot the root directory that contains the source files of the projects
     * @param projectCollection corresponding projects of the pom's habushu-type dependencies
     * @throws IOException if an error occurs while copying files
     */
    protected void stageHabushuProjects(Path sourceRoot, ProjectStack projectCollection) throws IOException {
        Path destRoot = getStagingPath();
        Files.createDirectories(destRoot);

        for (ProjectInfo info : projectCollection.getAllProjects()) {
            Path projectPath = info.getProject().getBasedir().toPath();
            Path relativeProjectPath = sourceRoot.relativize(projectPath);
            Path wheelName = stageProject(sourceRoot, destRoot, relativeProjectPath);
            if (wheelName == null) {
                throw new HabushuException("Failed to find wheel file for " + info.getProject().getArtifactId());
            }
            info.setWheelName(wheelName);
        }
    }

    /**
     * Moves the files identified by the given {@link FileSet} from the source root to the destination root, preserving
     * the relative path of the project.
     *
     * @param sourceRoot the root directory that contains the project
     * @param destRoot the root directory to copy sources into
     * @param relativeProjectPath the relative path of the project from the source/destination root
     * @return the name of the wheel for the given project
     * @throws IOException
     */
    protected Path stageProject(Path sourceRoot, Path destRoot, Path relativeProjectPath) throws IOException {
        Path copiedWheel = null;
        logger.info("Staging monorepo dependency files from {}.", relativeProjectPath.getFileName());
        Path projectDir = sourceRoot.resolve(relativeProjectPath);
        Path distDir = projectDir.resolve("dist");
        CommandHelper commandHelper = createCommandHelper(projectDir);
        if (Files.isDirectory(distDir)) {
            String wheelPattern = constructWheelNamePattern(commandHelper);
            //Multiple wheels will most often be found for dev snapshots, as the install phase will create
            //`<version>.dev0` and the deploy phase will create `<version>.dev<timestamp>`. So choose the most
            //up-to-date. Sort by name as tie-breaker if modification time is the same.
            logger.info("Searching for wheels matching {}", wheelPattern);
            Set<Path> wheels = new TreeSet<>(Comparator.comparing(Path::toString));
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(distDir, wheelPattern)) {
                ds.forEach(wheels::add);
            }
            Path sourceWheel = null;
            for (Path wheel : wheels) {
                Path unusedWheel;
                if (sourceWheel == null || Files.getLastModifiedTime(sourceWheel).compareTo(Files.getLastModifiedTime(wheel)) <= 0) {
                    unusedWheel = sourceWheel;
                    sourceWheel = wheel;
                    copiedWheel = destRoot.resolve(wheel.getFileName());
                    Files.copy(wheel, copiedWheel, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    unusedWheel = wheel;
                }
                if (unusedWheel != null) {
                    logger.warn("Multiple wheels found for project [{}]! Choosing [{}] over [{}]",
                            projectDir.getFileName(), sourceWheel.getFileName(), unusedWheel.getFileName());
                }
            }
        }
        return copiedWheel;
    }


    /**
     * Checks listed habushu-type dependencies against the set of projects included in the Maven build's session
     * @return the corresponding Maven projects that match the habushu-type dependencies
     */
    protected ProjectStack getHabushuProjects() {
        ProjectStack collectionResult;
        Set<Dependency> directHabushuDeps = session.getCurrentProject().getDependencies().stream()
                .filter(d -> HabushuUtil.HABUSHU.equals(d.getType()))
                .collect(Collectors.toSet());
        if (directHabushuDeps.size() > 1) {
            throw new HabushuException("More than one `habushu` packaged dependency was found."
                    + "Only one habushu-type dependency should be specified.");

        } else if (directHabushuDeps.size() == 1) {
            collectionResult = new ProjectStack(directHabushuDeps.iterator().next());
        } else {
            throw new HabushuException("No `habushu` packaged dependencies were found to containerize.");
        }
        collectHabushuDependenciesAsProjects(project, collectionResult);
        if (collectionResult.hasPendingProjects()) {
            String habushuDependency = getProjectHabushuDependencies(getProject()).toString().split(":")[1];
            String message = "Habushu project to containerize was not included in the provided projects (-pl)." +
                    " Ensure the Habushu project is in the build and the POM dependencies are configured correctly." +
                    String.format("%n Add :%s to project build scope to resolve", habushuDependency);
            throw new HabushuException(message);
        }
        return collectionResult;
    }

    /**
     * Collects the projects with habushu-type dependencies and adds them to the given project set
     * @param currentProject the project to interrogate the habushu-type dependencies against
     * @param collectionResult the result object to add the projects to
     */
    protected void collectHabushuDependenciesAsProjects(MavenProject currentProject, ProjectStack collectionResult) {
        collectionResult.addPendingProjects(getProjectHabushuDependencies(currentProject));
        for (MavenProject project : getSession().getAllProjects()) {
            if (collectionResult.isPendingProject(toGav(project))) {
                logger.info("Found project {} as habushu-type dependency.", project);
                collectionResult.pushProject(project);
                collectHabushuDependenciesAsProjects(project, collectionResult);
            }
        }
    }

    protected static String toGav(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
    }
    protected static String toGav(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
    }

    protected MavenSession getSession() {
        return this.session;
    }

    protected Path getStagingPath() {
        return stagingDirectory.toPath();
    }

    protected void setPackageManager(MavenProject primaryProject) {
        Path baseDir = primaryProject.getBasedir().toPath();
        File pyprojectPath = baseDir.resolve("pyproject.toml").toFile();
        this.packageManager = HabushuUtil.checkPythonPackageManager(pyprojectPath);
    }

    protected void setDockerfile(File dockerfile) {
        this.dockerfile = dockerfile;
    }

    protected void setUpdateDockerfile(boolean update) {
        this.updateDockerfile = update;
    }

    protected void performDockerfileUpdateForVirtualEnvironment(ProjectStack result) {
        List<String> orderedProjectWheels = result.getAllProjects().stream()
                .map(ProjectInfo::getWheelName)
                .collect(Collectors.toList());
        ContainerizeDepsDockerfileHelper helper = new  ContainerizeDepsDockerfileHelper(this, orderedProjectWheels);
        String updatedDockerfile = helper.updateDockerfileWithContainerStageLogic();

        try (Writer writer = new FileWriter(this.dockerfile)) {
            writer.write(updatedDockerfile);
        } catch (IOException e) {
            throw new HabushuException("Unable to update Dockerfile.", e);
        }
    }

    protected CommandHelper createCommandHelper(Path projectDir) {
        if (packageManager == PackageManager.POETRY) {
            return new PoetryCommandHelper(projectDir.toFile());
        } else {
            return new UvCommandHelper(projectDir.toFile());
        }
    }

    protected String constructWheelNamePattern(CommandHelper commandHelper) {
        String normalizedName = commandHelper.getProjectName().replace('-', '_');
        String version = commandHelper.getProjectVersion();
        //there could be a build descriptor, or a missing implied 0 as in 1.0.0.dev0
        return normalizedName + "-" + version + "*-*-none-any.whl";
    }

    private static Set<String> getProjectHabushuDependencies(MavenProject currentProject) {
        return currentProject.getDependencies().stream()
                .filter(d -> HabushuUtil.HABUSHU.equals(d.getType()))
                .map(ContainerizeDepsMojo::toGav)
                .collect(Collectors.toSet());
    }

    public File getDockerfile() {
        return dockerfile;
    }

    public String getDockerUser() {
        return dockerUser;
    }

    public String getDockerVenvDirectoryPermissions(){
        return dockerVenvDirectoryPermissions;
    }

    public File getDockerTemplatePath() {
        return dockerTemplatePath;
    }

    public String getDockerBuilderStageTemplate() {
        return dockerBuilderStageTemplate;
    }

    public String getDockerFinalStageTemplate() {
        return dockerFinalStageTemplate;
    }

    public String getDockerBuilderBase() {
        return dockerBuilderBase;
    }

    public String getDockerFinalBase() {
        return dockerFinalBase;
    }

    public List<String> getExtraWheels() {
        //todo: add mojo param
        return Collections.emptyList();
    }

    public String getStagingDirectoryRelativeToContext() {
        return dockerContext.toPath().relativize(stagingDirectory.toPath()).toString();
    }

    /**
     * Result object for collecting Maven projects that are required to containerize a given Habushu project.  There is
     * one "primary" project that is the direct target of containerization.  Other Habushu projects are included when
     * they are monorepo dependencies of the primary project.
     */
    protected static class ProjectStack {
        private final Dependency directDependency;
        private final Deque<ProjectInfo> habushuProjects; //includes primaryProject
        private final Set<String> pendingProjectGavs;
        private MavenProject primaryProject;

        public ProjectStack(Dependency directDependency) {
            this.directDependency = directDependency;
            this.habushuProjects = new ArrayDeque<>();
            this.pendingProjectGavs = new HashSet<>();
            pendingProjectGavs.add(toGav(directDependency));
        }

        public void pushProject(MavenProject project) {
            habushuProjects.push(new ProjectInfo(project));
            pendingProjectGavs.remove(toGav(project));
            if (toGav(directDependency).equals(toGav(project))) {
                primaryProject = project;
            }
        }

        /**
         * @return all projects including the primary project
         */
        public Collection<ProjectInfo> getAllProjects() {
            return habushuProjects;
        }

        /**
         * @return the primary project
         */
        public MavenProject getPrimaryProject() {
            return primaryProject;
        }

        public void addPendingProjects(Set<String> habushuDependencies) {
            pendingProjectGavs.addAll(habushuDependencies);
        }

        public boolean isPendingProject(String gav) {
            return pendingProjectGavs.contains(gav);
        }

        public boolean hasPendingProjects() {
            return !pendingProjectGavs.isEmpty();
        }
    }

    protected static class ProjectInfo {
        private final MavenProject project;
        private String wheelName;

        private ProjectInfo(MavenProject project) {
            this.project = project;
        }

        public MavenProject getProject() {
            return project;
        }

        public String getWheelName() {
            return wheelName;
        }

        public void setWheelName(Path wheelName) {
            this.wheelName = wheelName.getFileName().toString();
        }
    }
}
