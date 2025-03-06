package org.technologybrewery.habushu;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.clean.Fileset;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CleanHabushuPoetry extends AbstractCleanHabushu {

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param mojo Configurations for cleaning Habushu
     */
    protected CleanHabushuPoetry(File baseDir, Log log, CleanHabushuMojo mojo) {
        super(baseDir, log, mojo);
    }

    @Override
    public void doExecute() throws MojoExecutionException {

        List<Fileset> filesetsToDelete = new ArrayList<>();
        boolean removeVenvManually = false;
        String virtualEnvFullPath = null;
        try {
        PoetryCommandHelper commandHelper = new PoetryCommandHelper(baseDir);
        virtualEnvFullPath = commandHelper.execute(commandHelper.createEnvListFullPathCommand());
        } catch (RuntimeException e) {
            log.debug("Could not retrieve Poetry-managed virtual environment path - it likely does not exist", e);
        }
        
        virtualEnvFullPath = HabushuUtil.getCleanVirtualEnvironmentPath(virtualEnvFullPath);

        String inVirtualEnvironmentPath = HabushuUtil.getInProjectVirtualEnvironmentPath(cleanHabushuMojo.getWorkingDirectory());
        File venv = new File(inVirtualEnvironmentPath);

        boolean deleteVirtualEnv = checkInProjectVirtualEnvironment(virtualEnvFullPath, venv, inVirtualEnvironmentPath);

        if (deleteVirtualEnv) {
            if (StringUtils.isBlank(virtualEnvFullPath)) {
                log.warn("No Poetry virtual environment was detected for deletion.");
            } else {

                String virtualEnvName = new File(virtualEnvFullPath).getName();
                if (StringUtils.isNotBlank(virtualEnvName)) {
                    PoetryCommandHelper poetryHelper = new PoetryCommandHelper(cleanHabushuMojo.getWorkingDirectory());
                    List<String> arguments = new ArrayList<>();
                    arguments.add("env");
                    arguments.add("remove");
                    if (!".venv".equals(virtualEnvName)) {
                        arguments.add(virtualEnvName);
                    } else {
                        // While Poetry 1.8.3 and lower will unregister the .venv virtual environment, it doesn't
                        // remove it, creating confusion:
                        removeVenvManually = true;
                    }
                    poetryHelper.execute(arguments);
                }
            }
        }

        try {
            Fileset distArchivesFileset = createFileset(cleanHabushuMojo.getDistDirectory());
            filesetsToDelete.add(distArchivesFileset);

            Fileset targetArchivesFileset = createFileset(cleanHabushuMojo.getTargetDirectory());
            filesetsToDelete.add(targetArchivesFileset);

            if (removeVenvManually) {
                filesetsToDelete.add(createFileset(venv));
            }

        } catch (IllegalAccessException e) {
            throw new HabushuException("Could not write to private field in Fileset class.", e);
        }

        log.info(String.format("Deleting distribution archives at %s", cleanHabushuMojo.getDistDirectory()));
        log.info(String.format("Deleting target archives at %s", cleanHabushuMojo.getTargetDirectory()));

        setPrivateParentField("filesets", filesetsToDelete.toArray(new Fileset[0]));

        cleanHabushuMojo.cleanExecute();

    }


    /**
     * This is helper method to check to see if useInProjectVirtualEnvironment is on or venv file exist,
     * we clean up virtual environment file.
     * @param virtualEnvFullPath
     * @param venv
     * @param inVirtualEnvironmentPath
     * @return boolean to deleteVirtualEnv
     */
    private boolean checkInProjectVirtualEnvironment(String virtualEnvFullPath, File venv, String inVirtualEnvironmentPath )
    {
        boolean deleteVirtualEnv = cleanHabushuMojo.deleteVirtualEnv();
        if (cleanHabushuMojo.useInProjectVirtualEnvironment()) {
            log.debug("`in-project` virtual environment configured for this project");
            if (StringUtils.isNotBlank(virtualEnvFullPath) && !inVirtualEnvironmentPath.equals(virtualEnvFullPath)) {
                log.warn("'in-project' virtual environment is configured, but an external virtual environment was found!");
                log.warn("Deleting external virtual environment: " + virtualEnvFullPath);
                deleteVirtualEnv = true;
            }
        } else if (venv.exists()) {
            log.warn("'in-project' virtual environment is NOT configured, but an 'in-project' virtual environment was found!");
            log.warn("Deleting 'in-project' virtual environment: " + virtualEnvFullPath);
            deleteVirtualEnv = true;
        }
        return deleteVirtualEnv;
    }
}
