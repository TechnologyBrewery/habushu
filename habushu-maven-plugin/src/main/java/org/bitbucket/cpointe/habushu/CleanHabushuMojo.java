package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.clean.CleanMojo;
import org.apache.maven.plugins.clean.Fileset;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;

/**
 * Overrides the default {@link CleanMojo} behavior to additionally delete the
 * virtual environment that is created/managed by Poetry if the
 * {@link #deleteVirtualEnv} option is enabled.
 */
@Mojo(name = "clean-habushu", defaultPhase = LifecyclePhase.CLEAN)
public class CleanHabushuMojo extends CleanMojo {

    /**
     * Base directory in which Poetry projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected File workingDirectory;

    /**
     * Directory in which Poetry places generated source and wheel archive
     * distributions.
     */
    @Parameter(defaultValue = "${project.basedir}/dist", readonly = true, required = true)
    protected File distDirectory;

    /**
     * Enables the explicit deletion of the virtual environment that is
     * created/managed by Poetry.
     */
    @Parameter(property = "habushu.delete.virtualenv", required = true, defaultValue = "false")
    protected boolean deleteVirtualEnv;

    @Override
    public void execute() throws MojoExecutionException {

	if (deleteVirtualEnv) {
	    PoetryCommandHelper poetryHelper = new PoetryCommandHelper(this.workingDirectory);

	    String virtualEnvFullPath = poetryHelper.execute(Arrays.asList("env", "info", "--path"));
	    String virtualEnvName = new File(virtualEnvFullPath).getName();

	    getLog().info(String.format("Deleting virtual environment managed by Poetry %s...", virtualEnvName));
	    poetryHelper.execute(Arrays.asList("env", "remove", virtualEnvName));
	}

	List<Fileset> filesetsToDelete = new ArrayList<>();

	try {
	    Fileset distArchivesFileset = createFileset(distDirectory);
	    filesetsToDelete.add(distArchivesFileset);
	} catch (IllegalAccessException e) {
	    throw new MojoExecutionException("Could not write to private field in Fileset class.", e);
	}

	getLog().info(String.format("Deleting distribution archives at %s", distDirectory));

	setPrivateParentField("filesets", filesetsToDelete.toArray(new Fileset[0]));
	super.execute();
    }

    /**
     * Creates a new {@link Fileset} that may be used to identify a set of files
     * that are targeted for deletion by the {@link CleanMojo}.
     * 
     * @param directory directory that is desired for deletion.
     * @return
     * @throws IllegalAccessException
     */
    private Fileset createFileset(File directory) throws IllegalAccessException {
	Fileset fileset = new Fileset();
	FieldUtils.writeField(fileset, "directory", directory, true);
	return fileset;
    }

    /**
     * Sets a given field in the parent {@link CleanMojo} with a provided value.
     * This method is needed as {@link CleanMojo} is structured in a way that does
     * not easily facilitate extension.
     * 
     * @param fieldName the name of the field in {@link CleanMojo}
     * @param value     the field's intended value
     */
    private void setPrivateParentField(String fieldName, Object value) {
	Field fieldInParentClass;
	try {
	    fieldInParentClass = this.getClass().getSuperclass().getDeclaredField(fieldName);
	    fieldInParentClass.setAccessible(true);
	    fieldInParentClass.set(this, value);
	} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
	    throw new HabushuException("Could not write to field in CleanMojo class.", e);
	}

    }
}
