package org.technologybrewery.habushu;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.clean.CleanMojo;
import org.apache.maven.plugins.clean.Fileset;

import java.io.File;
import java.lang.reflect.Field;


public abstract class AbstractCleanHabushu {

    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * Instance of CleanHabushuMojo
     */
    protected CleanHabushuMojo cleanHabushuMojo;



    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir    base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param mojo Configurations for cleaning Habushu
     */
    protected AbstractCleanHabushu(File baseDir, Log log, CleanHabushuMojo mojo) {
        this.baseDir = baseDir;
        this.log = log;
        this.cleanHabushuMojo = mojo;
    }

    public abstract void doExecute() throws MojoExecutionException;


    /**
     * Creates a new {@link Fileset} that may be used to identify a set of files
     * that are targeted for deletion by the {@link CleanMojo}.
     *
     * @param directory directory that is desired for deletion.
     * @return
     * @throws IllegalAccessException
     */
    protected Fileset createFileset(File directory) throws IllegalAccessException {
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
    protected void setPrivateParentField(String fieldName, Object value) {
        Field fieldInParentClass;
        try {
            fieldInParentClass = cleanHabushuMojo.getClass().getSuperclass().getDeclaredField(fieldName);
            fieldInParentClass.setAccessible(true);
            fieldInParentClass.set(cleanHabushuMojo, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new HabushuException("Could not write to field in CleanMojo class.", e);
        }
    }
}
