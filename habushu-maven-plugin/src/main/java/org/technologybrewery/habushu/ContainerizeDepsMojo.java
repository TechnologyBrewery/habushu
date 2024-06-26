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
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.ContainerizeDepsDockerfileHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Stages the source files of a monorepo dependency
 * to the target directory along with the source files of
 * any transitive path-based dependencies.
 */
@Mojo(name = "containerize-dependencies", defaultPhase = LifecyclePhase.PACKAGE)
public class ContainerizeDepsMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(ContainerizeDepsMojo.class);

    @Component
    protected MavenSession session;

    /**
     * The desired version of Python to use.
     */
    @Parameter(defaultValue = PyenvAndPoetrySetup.PYTHON_DEFAULT_VERSION_REQUIREMENT, property = "habushu.pythonVersion")
    protected String pythonVersion;

    /**
     * Should Habushu use pyenv to manage the utilized version of Python?
     */
    @Parameter(defaultValue = "true", property = "habushu.usePyenv")
    protected boolean usePyenv;

    /**
     * Indicates whether Habushu should leverage the
     * {@code poetry-monorepo-dependency-plugin} to rewrite any local path
     * dependencies (to other Poetry projects) as versioned packaged dependencies in
     * generated wheel/sdist archives. If {@code true}, Habushu will replace
     * invocations of Poetry's {@code build} and {@code publish} commands in the
     * {@link BuildDeploymentArtifactsMojo} and {@link PublishToPyPiRepoMojo} with
     * the extensions of those commands exposed by the
     * {@code poetry monorepo-dependency-plugin}, which are
     * {@code build-rewrite-path-deps} and {@code publish-rewrite-path-deps}
     * respectively.
     * <p>
     * Typically, this flag will only be {@code true} when deploying/releasing
     * Habushu modules within a CI environment that are part of a monorepo project
     * structure which multiple Poetry projects depend on one another.
     */
    @Parameter(defaultValue = "false", property = "habushu.rewriteLocalPathDepsInArchives")
    protected boolean rewriteLocalPathDepsInArchives;

    /**
     * File specifying the location of a generated shell script that will attempt to
     * install the specified version of Python using "pyenv install --patch" with a
     * patch that attempts to resolve the expected compilation error.
     */
    @Parameter(defaultValue = "pyenv-patch-install-python-version.sh", readonly = true)
    private String patchInstallScriptRelativeToBuildDirectory;

    /**
     * Working directory relative to the basedir - typically working directory and basedir are synonymous.
     */
    @Parameter(readonly = true)
    protected String workingDirectoryRelativeToBasedir;

    /**
     * Expected subdirectory of the monorepo dependency's basedir
     * in which Poetry places generated source and wheel archive distributions.
     */
    @Parameter(defaultValue = "/dist", readonly = true)
    protected String distDirectoryRelativeToBasedir;

    /**
     * Expected subdirectory of the monorepo dependency's basedir
     * in which Maven places build-time artifacts. Should NOT include dist items.
     */
    @Parameter(defaultValue = "/target", readonly = true)
    protected String targetDirectoryRelativeToBasedir;

    /**
     * Location of where containerization files will be placed.
     */
    @Parameter(defaultValue = "${project.build.directory}/containerize-support", readonly = true)
    protected String containerizeSupportDirectory;

    /**
     * Upstream directory that houses all necessary monorepo dependencies.
     * Monorepo dependency source files will be copied from here.
     */
    @Parameter(readonly = true)
    protected File anchorSourceDirectory;

    /**
     * Update dockerfile
     */
    @Parameter(defaultValue = "true", property = "habushu.updateDockerfile")
    protected boolean updateDockerfile;

    /**
     * Dockerfile to be updated with the stage content
     */
    @Parameter(property = "habushu.dockerfile")
    protected File dockerfile;

    private Path anchorOutputDirectory;

    protected final String HABUSHU = "habushu";
    protected final String GLOB_RECURSIVE_ALL = "/**";
    /**
     * Overriding to allow execution in non-habushu projects.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (this.anchorSourceDirectory == null) {
            this.anchorSourceDirectory = new File(session.getExecutionRootDirectory());
        }

        if (this.workingDirectoryRelativeToBasedir == null) {
            this.workingDirectoryRelativeToBasedir = "";
        }

        ProjectCollectionResult result = getHabushuProjects();
        try {
            Path targetProjectPath = copySourceCode(result);
            if (this.updateDockerfile) {
                if (this.dockerfile == null) {
                    throw new HabushuException("`updateDockerfile` is set to true but `dockerfile` is not specified");
                }
                performDockerfileUpdateForVirtualEnvironment(targetProjectPath);
            }
        } catch (IOException e) {
            throw new HabushuException(e);
        }
    }

    /**
     * Copies the relevant source files by leveraging {@link FileSet}s to filter appropriately.
     * @param projectCollection corresponding projects of the pom's habushu-type dependencies
     * @return the relative path from the staging root to the primary project being containerized
     * @throws IOException
     * @throws MojoExecutionException
     */
    protected Path copySourceCode(ProjectCollectionResult projectCollection) throws IOException, MojoExecutionException {
        this.anchorOutputDirectory = Path.of(containerizeSupportDirectory, this.anchorSourceDirectory.getName());
        Path srcRoot = this.anchorSourceDirectory.toPath();
        Path primaryProjectPath = null;

        Map<Path, FileSet> dependencyFileSets = new HashMap<>();
        for (MavenProject project : projectCollection.getAllProjects()) {
            Path projectPath = getWorkingDirectoryPath(project);
            Path relativeProjectPath = srcRoot.relativize(projectPath);
            FileSet fileSet = getDefaultFileSet(project);
            fileSet.setDirectory(projectPath.toString());
            dependencyFileSets.put(relativeProjectPath, fileSet);
            if (project.equals(projectCollection.getPrimaryProject())) {
                primaryProjectPath = relativeProjectPath;
            }
        }
        if( primaryProjectPath == null ) {
            throw new HabushuException("Primary project was not included in the set of projects. Ensure the habushu project is in the build and the pom dependencies are configured correctly");
        }

        FileSetManager fileSetManager = new FileSetManager();
        for (Path project : dependencyFileSets.keySet()) {
            FileSet fileSet = dependencyFileSets.get(project);
            logger.info("Staging {} monorepo dependency files from {}.",
                    fileSetManager.getIncludedFiles(fileSet).length,
                    project.getFileName()
            );
            for (String includedFile : fileSetManager.getIncludedFiles(fileSet)) {
                Path relativePath = project.resolve(includedFile);
                Files.createDirectories(this.anchorOutputDirectory.resolve(relativePath).getParent());
                Files.copy(srcRoot.resolve(relativePath), this.anchorOutputDirectory.resolve(relativePath), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return primaryProjectPath;
    }

    private Path getWorkingDirectoryPath(MavenProject project) {
        return project.getBasedir().toPath().resolve(workingDirectoryRelativeToBasedir);
    }

    private FileSet getDefaultFileSet(MavenProject project) throws MojoExecutionException {
        FileSet fileSet = new FileSet();
        fileSet.addExclude(distDirectoryRelativeToBasedir + GLOB_RECURSIVE_ALL);
        fileSet.addExclude(targetDirectoryRelativeToBasedir + GLOB_RECURSIVE_ALL);

        // find the virtual environment path for the given Habushu-packaged project
        String virtualEnvironmentPath = HabushuUtil.findCurrentVirtualEnvironmentFullPath(
                pythonVersion,
                usePyenv,
                new File(project.getBuild().getDirectory() + patchInstallScriptRelativeToBuildDirectory),
                new File(project.getBasedir() + workingDirectoryRelativeToBasedir),
                rewriteLocalPathDepsInArchives,
                getLog()
        );
        virtualEnvironmentPath = HabushuUtil.getCleanVirtualEnvironmentPath(virtualEnvironmentPath);

        // if a valid virtual environment was found, relativize and add to the fileSet for exclusions
        if (virtualEnvironmentPath != null && !virtualEnvironmentPath.isEmpty()
                && new File(virtualEnvironmentPath).isDirectory()) {
            Path venvDirRelativeToBasedir = getWorkingDirectoryPath(project)
                    .relativize(Paths.get(virtualEnvironmentPath));
            fileSet.addExclude(venvDirRelativeToBasedir + GLOB_RECURSIVE_ALL);
        }

        return fileSet;
    }

    /**
     * Checks listed habushu-type dependencies against the set of projects included in the Maven build's session
     * @return the corresponding Maven projects that match the habushu-type dependencies
     */
    protected ProjectCollectionResult getHabushuProjects() {
        ProjectCollectionResult collectionResult;
        Set<Dependency> directHabushuDeps = session.getCurrentProject().getDependencies().stream()
                .filter(d -> HABUSHU.equals(d.getType()))
                .collect(Collectors.toSet());
        // TODO: modify this exception throw, once support for
        //  more than one direct monorepo dep specification is implemented
        if (directHabushuDeps.size() > 1) {
            throw new HabushuException("More than one `habushu` packaged dependency was found."
                    + "Only one habushu-type dependency should be specified.");

        } else if (directHabushuDeps.size() == 1) {
            collectionResult = new ProjectCollectionResult(directHabushuDeps.iterator().next());
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
    protected ProjectCollectionResult collectHabushuDependenciesAsProjects(MavenProject currentProject, ProjectCollectionResult collectionResult) {
        Set<String> habushuDeps = currentProject.getDependencies().stream()
                .filter(d -> HABUSHU.equals(d.getType()))
                .map(ContainerizeDepsMojo::toGav)
                .collect(Collectors.toSet());
        for (MavenProject project : getSession().getProjects()) {
            if (habushuDeps.contains(toGav(project))) {
                logger.info("Found project {} as habushu-type dependency.", project);
                collectionResult.addProject(project);
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

    protected void setAnchorSourceDirectory(File anchorSourceDirectory) {
        this.anchorSourceDirectory = anchorSourceDirectory;
    }

    public Path getAnchorOutputDirectory() {
        return anchorOutputDirectory;
    }

    protected void setDockerfile(File dockerfile) {
        this.dockerfile = dockerfile;
    }

    protected void setUpdateDockerfile(boolean update) {
        this.updateDockerfile = update;
    }

    protected void performDockerfileUpdateForVirtualEnvironment(Path targetProjectPath) {
        Path outputDir = session.getCurrentProject().getBasedir().toPath().relativize(this.anchorOutputDirectory);
        String updatedDockerfile =
                ContainerizeDepsDockerfileHelper.updateDockerfileWithContainerStageLogic(
                        this.dockerfile, outputDir.toString(), targetProjectPath.toString());

        try (Writer writer = new FileWriter(this.dockerfile)) {
            writer.write(updatedDockerfile);

        } catch (IOException e) {
            throw new HabushuException("Unable to update Dockerfile.", e);
        }
    }

    /**
      * Result object for collecting Maven projects that are required to containerize a given Habushu project.  There is
      * one "primary" project that is the direct target of containerization.  Other Habushu projects are included when
      * they are monorepo dependencies of the primary project.
      */
    protected static class ProjectCollectionResult {
        private final Dependency directDependency;
        private final Set<MavenProject> habushuProjects; //includes primaryProject
        private MavenProject primaryProject;
        public ProjectCollectionResult(Dependency directDependency) {
            this.directDependency = directDependency;
            this.habushuProjects = new HashSet<>();
        }
        public void addProject(MavenProject project) {
            this.habushuProjects.add(project);
            if (toGav(directDependency).equals(toGav(project))) {
                primaryProject = project;
            }
        }
        /**
         * @return all projects including the primary project
         */
        public Set<MavenProject> getAllProjects() {
            return habushuProjects;
        }
        public MavenProject getPrimaryProject() {
            return primaryProject;
        }
    }

}
