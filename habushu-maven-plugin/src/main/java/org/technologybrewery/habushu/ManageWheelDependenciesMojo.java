package org.technologybrewery.habushu;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;

/**
 * Helper mojo that encapsulates management of wheel dependencies,
 * both caching and retrieving of wheel artifacts into and from
 * Poetry cache during the {@link LifecyclePhase#INSTALL} build phase. 
 *
 * @param cacheWheels       A boolean that when implemented will cache a project's 
 *                          wheel files in poetry.
 * @param wheelDependencies A List of Wheel Dependencies which will identify wheel 
 *                          files by {@WheelDependency.artifactId} in poetry cache and place them into 
 *                          a given {@WheelDependency.targetDirectory}. This logic specifically targets
 *                          wheel artifacts cached by the {@param cacheWheels} parameter and REQUIRES 
 *                          the requested wheel to have first been cached prior to setting this config
 * @throws HabushuException
 */
@Mojo(name = "manage-wheels", defaultPhase = LifecyclePhase.INSTALL)
public class ManageWheelDependenciesMojo extends AbstractHabushuMojo {
    /**
     * A boolean that when implemented will cache a project's wheel files in poetry.
     */
    @Parameter(property = "habushu.cacheWheels", required = false, defaultValue = "false")
    protected boolean cacheWheels;

    /**
    * A List of Wheel Dependencies to retrieve from poetry cache. 
    */    
    @Parameter(property = "habushu.wheelDependencies", required = false)
    protected List<WheelDependency> wheelDependencies;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (cacheWheels) {
            cacheWheels();
        }

        if (!wheelDependencies.isEmpty()) {
            processWheelDependencies();
        }
    }

    private void cacheWheels() {
        getLog().info("Processing Cache Wheels..");
        try {
            File wheelSourceDirectory = getProjectBuildDirectory();
            File poetryWheelCacheDirectory = getCachedWheelDirectory(project.getArtifactId());
            // conditional will throw an error if cache directory isn't created
            if (poetryWheelCacheDirectory.exists() || poetryWheelCacheDirectory.mkdirs()) {
                List<File> wheelFiles = Stream.of(wheelSourceDirectory.listFiles())
                        .filter(file -> file.getAbsolutePath().endsWith(".whl"))
                        .map(File::getAbsoluteFile)
                        .collect(Collectors.toList());
                for (File file : wheelFiles) {
                    HabushuUtil.copyFile(file.getPath(),
                            String.format("%s/%s", poetryWheelCacheDirectory.toPath().toString(), file.getName()));
                    getLog().info(String.format("Cached the %s file", file.getName()));
                }
            }
        } catch (Exception e) {
            throw new HabushuException("Could not cache the " + project.getArtifactId() + " wheel file(s)!", e);
        }
    }

    protected void processWheelDependencies() {
        getLog().info(String.format("Processing %s Wheel Dependencies..", wheelDependencies.size()));
        try {
            for (WheelDependency wd : wheelDependencies) {
                File poetryCacheWheelDirectory = getCachedWheelDirectory(wd.getArtifactId());
                String targetDirectory = wd.getTargetDirectory();

                if(poetryCacheWheelDirectory.exists()){
                    List<File> wheelFiles = Stream.of(poetryCacheWheelDirectory.listFiles())
                            .filter(file -> file.getAbsolutePath().endsWith(".whl"))
                            .map(File::getAbsoluteFile)
                            .collect(Collectors.toList());

                    if(wheelFiles.size()==0){
                        getLog().warn(String.format("Did not find any %s wheels in poetry cache.", wd.getArtifactId()));
                        getLog().warn("Consider using the `cacheWheel` configuration to cache the wheel artifact before depending on it.");
                    } else {
                        for (File file : wheelFiles) {
                            HabushuUtil.copyFile(file.getPath(), String.format("%s/%s", targetDirectory, file.getName()));
                            getLog().info(String.format("Retrieved the cached %s file", file.getName()));
                        }
                    }         
                } else{
                    getLog().warn(String.format("Could not locate %s in poetry cache.", wd.getArtifactId()));
                    getLog().warn("Consider using the `cacheWheel` configuration to cache the wheel artifact before depending on it.");
                }
            }
        } catch (Exception e) {
            throw new HabushuException("Could not process Wheel Dependencies!", e);
        }
    }

    public File getCachedWheelDirectory(String artifactId) {
        try {
            PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
            String poetryCacheDirectoryPath = poetryHelper.getPoetryCacheDirectoryPath();
            return new File(String.format("%s/cache/repositories/wheels/%s", poetryCacheDirectoryPath, artifactId));
        } catch (Exception e) {
            throw new HabushuException("Could not get the Poetry Cache Wheel directory!", e);
        }
    }

    protected File getProjectBuildDirectory() {
        return new File(project.getBuild().getDirectory());
    }
}
