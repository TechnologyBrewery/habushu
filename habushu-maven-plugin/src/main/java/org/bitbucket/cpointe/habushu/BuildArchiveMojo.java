package org.bitbucket.cpointe.habushu;

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;

/**
 * Delegates to Poetry to builds the source and wheel archives of this project
 * as a part of the {@link LifecyclePhase#PACKAGE} build phase.
 */
@Mojo(name = "build-archive", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildArchiveMojo extends AbstractHabushuMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
	getLog().info("Building source and wheel archives...");
	poetryHelper.executeAndLogOutput(Arrays.asList("build"));
    }

}
