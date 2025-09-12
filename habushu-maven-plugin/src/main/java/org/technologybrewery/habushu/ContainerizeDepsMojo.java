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
import org.technologybrewery.habushu.util.ContainerizeProjectInfo;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;
import org.technologybrewery.habushu.util.RequirementsFileHelper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        ContainerizeProjectInfo result = getHabushuProject();
        try {
            setPackageManager(result.getProjectPath());
            stageHabushuProject(result);
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
     * @param primaryProjectInfo information for the project to be staged for containerization
     * @throws IOException if an error occurs while copying files
     */
    protected void stageHabushuProject(ContainerizeProjectInfo primaryProjectInfo) throws IOException {
        Path destRoot = getStagingPath();
        Files.createDirectories(destRoot);

        // Stage primary app wheel and requirements file
        stageProject(primaryProjectInfo, destRoot, true);

        // The path-based dependencies will fail to actualize in the docker container
        // We will stage them into the docker image then update the requirements file to have the new staged paths
        RequirementsFileHelper requirementsFileHelper =
                new RequirementsFileHelper(primaryProjectInfo.getRequirementsFilePath(),
                        primaryProjectInfo.getProjectPath());

        // Get the list of local path-based dependencies in the requirements file
        List<Path> pathBasedRequirements = requirementsFileHelper.getPathBasedRequirements();

        // Stage the local files to the new location
        Map<Path, RequirementsFileHelper.RequirementReplacement> relocatedWheelPaths = new HashMap<>();
        for (Path pathBasedRequirement : pathBasedRequirements) {
            Path stagedPathBasedRequirement;
            if (Files.isDirectory(pathBasedRequirement)) {
                //requirement path points to another python project
                ContainerizeProjectInfo requirementInfo = new ContainerizeProjectInfo(pathBasedRequirement);
                stageProject(requirementInfo, destRoot, false);
                stagedPathBasedRequirement = requirementInfo.getWheelPath();
            } else {
                //assume a direct file is an archive that can be directly installed
                stagedPathBasedRequirement =
                        stageWheel(Collections.singleton(pathBasedRequirement), destRoot, pathBasedRequirement);
            }
            // Using WHEEL_HOUSE environment variable as the desired path because it's being used in dockerfile_builder_stage_template.vm
            RequirementsFileHelper.RequirementReplacement replacement = new RequirementsFileHelper.RequirementReplacement(
                    stagedPathBasedRequirement, "${WHEEL_HOUSE}/" + stagedPathBasedRequirement.getFileName());
            relocatedWheelPaths.put(pathBasedRequirement, replacement);
        }

        // Update the requirements file with the new path locations
        requirementsFileHelper.relocatePathRequirements(relocatedWheelPaths);
    }

    /**
     * Stages the wheel (and optionally the requirements file) for a given Python project
     *
     * @param projectInfo       the project to stage and populate with staging info
     * @param destRoot          the root directory to copy sources into
     * @param stageRequirements whether to stage the requirements.txt file for the given project
     * @throws IOException      if a file cannot be staged due to a file system issue
     * @throws HabushuException if a wheel (or requirements file) could not be found for the project
     */
    protected void stageProject(ContainerizeProjectInfo projectInfo, Path destRoot, boolean stageRequirements) throws IOException {
        logger.info("Staging monorepo dependency files from {}.", projectInfo.getProjectPath().getFileName());
        Path projectDir = projectInfo.getProjectPath();
        Path distDir = projectDir.resolve("dist");
        CommandHelper commandHelper = createCommandHelper(projectDir);
        if (Files.isDirectory(distDir)) {
            // Get the requirements.txt from dist
            if (stageRequirements) {
                Path requirements = distDir.resolve("requirements.txt").normalize();
                Path copiedRequirements = stageRequirements(requirements, destRoot);
                projectInfo.setRequirementsFilePath(copiedRequirements);
            }

            String wheelPattern = constructWheelNamePattern(commandHelper);
            //Multiple wheels will most often be found for dev snapshots, as the install phase will create
            //`<version>.dev0` and the deploy phase will create `<version>.dev<timestamp>`. So choose the most
            //up-to-date. Sort by name as tie-breaker if modification time is the same.
            logger.info("Searching for wheels matching {}", wheelPattern);
            Set<Path> wheels = new TreeSet<>(Comparator.comparing(Path::toString));
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(distDir, wheelPattern)) {
                ds.forEach(wheels::add);
            }
            Path copiedWheel = stageWheel(wheels, destRoot, projectDir);
            projectInfo.setWheelPath(copiedWheel);
        }
        if (projectInfo.getWheelPath() == null) {
            throw new HabushuException("No wheels found for project: " + projectDir);
        }
        if (stageRequirements && projectInfo.getRequirementsFilePath() == null) {
            throw new HabushuException("No requirements file found for project: " + projectDir);
        }
    }

    private Path stageWheel(Set<Path> wheels, Path destRoot, Path projectDir) throws IOException {
        Path sourceWheel = null;
        Path copiedWheel = null;
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
        return copiedWheel;
    }

    private Path stageRequirements(Path requirements, Path destRoot) throws IOException {
        Path copiedRequirements = destRoot.resolve(requirements.getFileName());
        return Files.copy(requirements, copiedRequirements, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Checks listed habushu-type dependencies against the set of projects included in the Maven build's session
     * @return the corresponding Maven project that matches the habushu-type dependency
     *
     * @throws HabushuException if no habushu-type dependencies are found, or if more than one is found
     */
    protected ContainerizeProjectInfo getHabushuProject() {
        Set<Dependency> directHabushuDeps = session.getCurrentProject().getDependencies().stream()
                .filter(d -> HabushuUtil.HABUSHU.equals(d.getType()))
                .collect(Collectors.toSet());
        if (directHabushuDeps.size() > 1) {
            StringBuilder foundHabushuDependencies = new StringBuilder("The following dependencies were found:");
            for (Dependency dependency : directHabushuDeps) {
                foundHabushuDependencies.append(String.format("\n\t%s:%s", dependency.getGroupId(), dependency.getArtifactId()));
            }

            throw new HabushuException("More than one `habushu` packaged dependency was found. "
                    + "Only one habushu-type dependency should be specified. " + foundHabushuDependencies);

        } else if (directHabushuDeps.size() == 1) {
            Dependency habushuDependency = directHabushuDeps.iterator().next();
            ContainerizeProjectInfo projectInfo = lookUpProject(habushuDependency);
            if (projectInfo == null) {
                throw new HabushuException("Unable to find project for `habushu` dependency: " + habushuDependency);
            }
            return projectInfo;
        } else {
            throw new HabushuException("No `habushu` packaged dependencies were found to containerize.");
        }
    }

    protected ContainerizeProjectInfo lookUpProject(Dependency dependencyToLookUp) {
        for (MavenProject project : getSession().getAllProjects()) {
            if (toGav(dependencyToLookUp).equals(toGav(project))) {
                logger.info("Found project {} as habushu-type dependency.", project);
                return new ContainerizeProjectInfo(project);
            }
        }
        return null;
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

    protected void setPackageManager(Path projectBaseDir) {
        File pyprojectPath = projectBaseDir.resolve("pyproject.toml").toFile();
        this.packageManager = HabushuUtil.checkPythonPackageManager(pyprojectPath);
    }

    protected void setDockerfile(File dockerfile) {
        this.dockerfile = dockerfile;
    }

    protected void setUpdateDockerfile(boolean update) {
        this.updateDockerfile = update;
    }

    protected void performDockerfileUpdateForVirtualEnvironment(ContainerizeProjectInfo primaryProject) {
        ContainerizeDepsDockerfileHelper helper = new  ContainerizeDepsDockerfileHelper(this, primaryProject);
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

    public String getStagingDirectoryRelativeToContext() {
        return dockerContext.toPath().relativize(stagingDirectory.toPath()).toString();
    }
}
