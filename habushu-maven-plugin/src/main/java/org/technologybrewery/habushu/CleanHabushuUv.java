package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.clean.Fileset;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CleanHabushuUv extends AbstractCleanHabushu {

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir    base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param mojo Configurations for cleaning Habushu
     */
    protected CleanHabushuUv(File baseDir, Log log, CleanHabushuMojo mojo) {
        super(baseDir, log, mojo);
    }

    @Override
    public void doExecute() throws MojoExecutionException {

        List<Fileset> filesetsToDelete = new ArrayList<>();

        try {
            Fileset distArchivesFileset = createFileset(cleanHabushuMojo.getDistDirectory());
            filesetsToDelete.add(distArchivesFileset);

            Fileset targetArchivesFileset = createFileset(cleanHabushuMojo.getTargetDirectory());
            filesetsToDelete.add(targetArchivesFileset);
            boolean deleteVirtualEnv = cleanHabushuMojo.deleteVirtualEnv();

            //UV doesn't have command to explicitly delete virtual environment like poetry does, so we need to manually delete.
            if(deleteVirtualEnv)
            {
                String activatedVirtualEnvPath = System.getenv("VIRTUAL_ENV");
                String inVirtualEnvironmentPath = HabushuUtil.getInProjectVirtualEnvironmentPath(cleanHabushuMojo.getWorkingDirectory());

                if (cleanHabushuMojo.useInProjectVirtualEnvironment())
                {
                    if (StringUtils.isNotBlank(activatedVirtualEnvPath))
                    {
                        log.warn("'in-project' virtual environment is configured, but an external virtual environment was found!");
                        log.warn("Deleting external virtual environment: " + activatedVirtualEnvPath);
                        File venv = new File(activatedVirtualEnvPath);
                        deleteVirtualEnv(venv, activatedVirtualEnvPath, filesetsToDelete);

                    } else {
                        File venv = new File(inVirtualEnvironmentPath);
                        deleteVirtualEnv(venv, inVirtualEnvironmentPath, filesetsToDelete);
                    }

                } else {
                    File inVirtualEnv = new File(inVirtualEnvironmentPath);
                    if (inVirtualEnv.exists())
                    {
                        log.warn("'in-project' virtual environment is NOT configured, but an 'in-project' virtual environment was found!");
                        log.warn("Deleting 'in-project' virtual environment: " + inVirtualEnvironmentPath);
                        deleteVirtualEnv(inVirtualEnv, inVirtualEnvironmentPath, filesetsToDelete);
                    } else {
                        File venv = new File(activatedVirtualEnvPath);
                        deleteVirtualEnv(venv, activatedVirtualEnvPath, filesetsToDelete);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new HabushuException("Could not write to private field in Fileset class.", e);
        }

        log.info(String.format("Deleting distribution archives at %s", cleanHabushuMojo.getDistDirectory()));
        log.info(String.format("Deleting target archives at %s", cleanHabushuMojo.getTargetDirectory()));

        //This will add list of files to delete and will actually be deleted when we call CleanMojo.execute()
        setPrivateParentField("filesets", filesetsToDelete.toArray(new Fileset[0]));

        cleanHabushuMojo.cleanExecute();

    }

    /**
     * This is helper method add to list of fileSet to delete to clean up virtual environment file.
     * @param venv
     * @param virtualEnvironmentPath
     * @param filesetsToDelete
     */
    private void deleteVirtualEnv(File venv, String virtualEnvironmentPath, List<Fileset> filesetsToDelete ) throws IllegalAccessException {

        if (StringUtils.isBlank(virtualEnvironmentPath)) {
            log.warn("No UV virtual environment was detected for deletion.");
        }

        Fileset venvFileset = createFileset(venv);
        filesetsToDelete.add(venvFileset);
        log.info(String.format("Deleting venv at %s", venv));

    }
}
