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
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import org.technologybrewery.habushu.util.ContainerizeDepsDockerfileHelper;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;
import org.technologybrewery.habushu.util.PoetryContainerizeDepsDockerfileHelper;
import org.technologybrewery.habushu.util.UvContainerizeDepsDockerfileHelper;
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
import java.util.Deque;
import java.util.Set;
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
    public String anchorDirectory;
    public String moduleBaseDir;

    @Component
    protected MavenSession session;

    /**
     * The directory in which the collected Python project files will be staged for containerization.
     */
    @Parameter(defaultValue = "${project.build.directory}/containerize-support", property = "habushu.stagingDirectory")
    protected File stagingDirectory;

    /**
     * For each Python project that is identified as required for containerization, the files identified by this fileset
     * will be copied to the staging directory. It is not currently possible to define different filesets for different
     * projects. If not set, defaults to "{habushu.sourceDirectory}/**", "*.toml", "*.lock" and "README.md".
     */
    @Parameter
    protected FileSet defaultSourceSet;

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
    @Parameter(defaultValue = "templates/dockerfile_poetry_builder_stage_template.vm", property = "habushu.dockerPoetryBuilderStageTemplatePath")
    protected String dockerPoetryBuilderStageTemplatePath;

    /**
     * The default Dockerfile final stage template for Poetry. Overwrite if a custom template is preferred.
     */
    @Parameter(defaultValue = "templates/dockerfile_poetry_final_stage_template.vm", property = "habushu.dockerPoetryFinalStageTemplatePath")
    protected String dockerPoetryFinalStageTemplatePath;

    /**
     * The default Dockerfile builder stage template for Poetry. Overwrite if a custom template is preferred.
     */
    @Parameter(defaultValue = "templates/dockerfile_uv_builder_stage_template.vm", property = "habushu.dockerUvBuilderStageTemplatePath")
    protected String dockerUvBuilderStageTemplatePath;

    /**
     * The default Dockerfile final stage template for Poetry. Overwrite if a custom template is preferred.
     */
    @Parameter(defaultValue = "templates/dockerfile_uv_final_stage_template.vm", property = "habushu.dockerUvFinalStageTemplatePath")
    protected String dockerUvFinalStageTemplatePath;

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
     * The version of Poetry to install in the container.
     */
    @Parameter(defaultValue = "2.0.1", property = "habushu.dockerPoetryVersion")
    protected String dockerPoetryVersion;

    /**
     * The version of the poetry-plugin-bundle to install in the container.
     */
    @Parameter(defaultValue = "1.5.0", property = "habushu.dockerPoetryPluginBundleVersion")
    protected String dockerPoetryPluginBundleVersion;

    /**
     * The version of uv to install in the container.
     */
    @Parameter(defaultValue = "0.6.2", property = "habushu.dockerUvVersion")
    protected String dockerUvVersion;

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
            setModuleBaseDir(stageHabushuProjects(sourceRoot, result).toString());
            if (this.updateDockerfile) {
                if (this.dockerfile == null) {
                    throw new HabushuException("`updateDockerfile` is set to true but `dockerfile` is not specified");
                }
                performDockerfileUpdateForVirtualEnvironment();
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
     * @return the relative path from the staging root to the primary project being containerized
     * @throws IOException if an error occurs while copying files
     */
    protected Path stageHabushuProjects(Path sourceRoot, ProjectStack projectCollection) throws IOException {
        Path destRoot = getStagingPath();
        Path primaryProjectPath = null;

        for (ProjectInfo info : projectCollection.getAllProjects()) {
            Path projectPath = info.getProject().getBasedir().toPath();
            Path relativeProjectPath = sourceRoot.relativize(projectPath);
            if (info.getProject().equals(projectCollection.getPrimaryProject())) {
                primaryProjectPath = relativeProjectPath;
            }

            FileSet sourceFileSet = getSourceSet();
            sourceFileSet.setDirectory(projectPath.toString());
            Path wheelName = stageProject(sourceRoot, destRoot, sourceFileSet, relativeProjectPath);
            info.setWheelFile(wheelName);
        }
        if( primaryProjectPath == null ) {
            String habushuDependency = getProjectHabushuDependencies(getProject()).toString().split(":")[1];
            StringBuilder message = new StringBuilder()
                    .append("Habushu project to containerize was not included in the provided projects (-pl).")
                    .append(" Ensure the Habushu project is in the build and the POM dependencies are configured correctly.")
                    .append(String.format("%n Add :%s to project build scope to resolve", habushuDependency));
            throw new HabushuException(message.toString());
        }
        return primaryProjectPath;
    }

    /**
     * Moves the files identified by the given {@link FileSet} from the source root to the destination root, preserving
     * the relative path of the project.
     *
     * @param sourceRoot the root directory that contains the project
     * @param destRoot the root directory to copy sources into
     * @param sourceFileSet the set of files to copy
     * @param relativeProjectPath the relative path of the project from the source/destination root
     * @return the name of the wheel for the given project
     * @throws IOException
     */
    protected Path stageProject(Path sourceRoot, Path destRoot, FileSet sourceFileSet, Path relativeProjectPath) throws IOException {
        FileSetManager fileSetManager = new FileSetManager();
        logger.info("Staging {} monorepo dependency files from {}.",
                fileSetManager.getIncludedFiles(sourceFileSet).length,
                relativeProjectPath.getFileName()
        );
        for (String includedFile : fileSetManager.getIncludedFiles(sourceFileSet)) {
            Path relativeFilePath = relativeProjectPath.resolve(includedFile);
            Files.createDirectories(destRoot.resolve(relativeFilePath).getParent());
            Files.copy(sourceRoot.resolve(relativeFilePath), destRoot.resolve(relativeFilePath));
        }

        Path copiedWheel = null;
        Path projectDir = sourceRoot.resolve(relativeProjectPath);
        Path distDir = projectDir.resolve("dist");
        CommandHelper commandHelper = packageManager == PackageManager.POETRY ?
                new PoetryCommandHelper(projectDir.toFile()) :
                new UvCommandHelper(projectDir.toFile());
        if (Files.isDirectory(distDir)) {
            Files.createDirectories(destRoot);
            String normalizedName = commandHelper.getProjectName().replace('-', '_');
            String version = commandHelper.getProjectVersion();
            //there could be a build descriptor, or a missing implied 0 as in 1.0.0.dev0
            String wheelGlob = normalizedName + "-" + version + "*-*-none-any.whl";
            logger.info("Searching for wheels matching {}", wheelGlob);
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(distDir, wheelGlob)) {
                for (Path wheel : ds) {
                    if (copiedWheel != null) {
                        throw new HabushuException(
                                String.format("Multiple wheels found for project [%s]! found %s and %s",
                                        projectDir.getFileName(),
                                        copiedWheel,
                                        wheel.getFileName()));
                    }
                    copiedWheel = destRoot.resolve(wheel.getFileName());
                    Files.copy(wheel, copiedWheel, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return copiedWheel;
    }

    protected FileSet getSourceSet() {
        if( defaultSourceSet != null ) {
            return defaultSourceSet;
        }

        FileSet fileSet = new FileSet();
        Path srcPath = sourceDirectory.toPath();
        Path basePath = project.getBasedir().toPath();
        Path relativeSrc = basePath.relativize(srcPath);
        fileSet.addInclude(relativeSrc +"/**");
        fileSet.addInclude("*.toml");
        fileSet.addInclude("*.lock");
        fileSet.addInclude("README.md");

        return fileSet;
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
        return collectHabushuDependenciesAsProjects(project, collectionResult);
    }

    /**
     * Collects the projects with habushu-type dependencies and adds them to the given project set
     * @param currentProject the project to interrogate the habushu-type dependencies against
     * @param collectionResult the result object to add the projects to
     */
    protected ProjectStack collectHabushuDependenciesAsProjects(MavenProject currentProject, ProjectStack collectionResult) {
        Set<String> habushuDeps = getProjectHabushuDependencies(currentProject);
        for (MavenProject project : getSession().getAllProjects()) {
            if (habushuDeps.contains(toGav(project))) {
                logger.info("Found project {} as habushu-type dependency.", project);
                collectionResult.pushProject(project);
                collectHabushuDependenciesAsProjects(project, collectionResult);
            }
        }
        return collectionResult;
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

    protected void setAnchorDirectory(String anchorDirectory) { this.anchorDirectory = anchorDirectory; }

    protected void setModuleBaseDir(String moduleBaseDir) { this.moduleBaseDir = moduleBaseDir; }

    protected void setDockerfile(File dockerfile) {
        this.dockerfile = dockerfile;
    }

    protected void setUpdateDockerfile(boolean update) {
        this.updateDockerfile = update;
    }

    protected void performDockerfileUpdateForVirtualEnvironment() {
        setAnchorDirectory(dockerContext.toPath().relativize(getStagingPath()).toString());
        ContainerizeDepsDockerfileHelper helper;
        if (PackageManager.POETRY.equals(packageManager)){
            helper = new PoetryContainerizeDepsDockerfileHelper(this);
        } else {
            helper = new UvContainerizeDepsDockerfileHelper(this);
        }
        String updatedDockerfile =
                helper.updateDockerfileWithContainerStageLogic();

        try (Writer writer = new FileWriter(this.dockerfile)) {
            writer.write(updatedDockerfile);

        } catch (IOException e) {
            throw new HabushuException("Unable to update Dockerfile.", e);
        }
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

    public String getDockerPoetryBuilderStageTemplatePath(){
        return dockerPoetryBuilderStageTemplatePath;
    }

    public String getDockerPoetryFinalStageTemplatePath(){
        return dockerPoetryFinalStageTemplatePath;
    }

    public String getDockerUvBuilderStageTemplatePath(){
        return dockerUvBuilderStageTemplatePath;
    }

    public String getDockerUvFinalStageTemplatePath(){
        return dockerUvFinalStageTemplatePath;
    }

    public String getDockerBuilderBase() {
        return dockerBuilderBase;
    }

    public String getDockerFinalBase() {
        return dockerFinalBase;
    }

    public String getDockerPoetryVersion() {
        return dockerPoetryVersion;
    }

    public String getDockerPoetryPluginBundleVersion() {
        return dockerPoetryPluginBundleVersion;
    }

    public String getDockerUvVersion() {
        return dockerUvVersion;
    }

    /**
     * Result object for collecting Maven projects that are required to containerize a given Habushu project.  There is
     * one "primary" project that is the direct target of containerization.  Other Habushu projects are included when
     * they are monorepo dependencies of the primary project.
     */
    protected static class ProjectStack {
        private final Dependency directDependency;
        private final Deque<ProjectInfo> habushuProjects; //includes primaryProject
        private MavenProject primaryProject;

        public ProjectStack(Dependency directDependency) {
            this.directDependency = directDependency;
            this.habushuProjects = new ArrayDeque<>();
        }

        public void pushProject(MavenProject project) {
            this.habushuProjects.push(new ProjectInfo(project));
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
    }

    protected static class ProjectInfo {
        private final MavenProject project;
        private Path wheelFile;

        private ProjectInfo(MavenProject project) {
            this.project = project;
        }

        public MavenProject getProject() {
            return project;
        }

        public Path getWheelFile() {
            return wheelFile;
        }

        public void setWheelFile(Path wheelFile) {
            this.wheelFile = wheelFile;
        }
    }
}
