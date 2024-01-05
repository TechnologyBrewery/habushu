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
 * Caches the generated poetry Wheel file when the
 * {@link cacheWheels} flag configuration is set to 
 * true during the {@link LifecyclePhase#INSTALL} 
 * build phase.
 * By default, the {@link cacheWheels} flag is set 
 * to false and will not cache the wheel files. 
 */
@Mojo(name = "cache-wheels", defaultPhase = LifecyclePhase.INSTALL)
public class CacheWheelsMojo extends AbstractHabushuMojo {

    @Parameter(property = "habushu.cacheWheels", required = false, defaultValue = "false")
    protected boolean cacheWheels;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if(cacheWheels){
            PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
            try{
                File wheelSourceDirectory = new File(project.getBuild().getDirectory());
                String poetryCacheDirectoryPath = poetryHelper.getPoetryCacheDirectoryPath();
                File poetryWheelCacheDirectory = new File(String.format("%s/cache/repositories/wheels/%s", poetryCacheDirectoryPath, project.getArtifactId()));
                //conditional will throw an error if cache directory isn't created 
                if(poetryWheelCacheDirectory.exists() || poetryWheelCacheDirectory.mkdirs()){
                    List<File> wheelFiles = Stream.of(wheelSourceDirectory.listFiles())
                                                .filter(file -> file.getAbsolutePath().endsWith(".whl"))
                                                .map(File::getAbsoluteFile)
                                                .collect(Collectors.toList());
                    for(File file : wheelFiles){
                        HabushuUtil.copyFile(file.getPath(), String.format("%s/%s", poetryWheelCacheDirectory.toPath().toString(), file.getName()));
                        getLog().info(String.format("Cached the %s file", file.getName()));
                    }
                }
            } catch (Exception e){
                throw new HabushuException("Could not cache the " + project.getArtifactId() + " wheel file(s)!", e);
            }
        }
    }    
}
