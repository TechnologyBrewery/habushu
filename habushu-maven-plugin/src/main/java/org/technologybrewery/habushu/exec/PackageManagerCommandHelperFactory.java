package org.technologybrewery.habushu.exec;

import java.io.File;

import org.apache.commons.lang3.NotImplementedException;
import org.technologybrewery.habushu.util.HabushuUtil;

public class PackageManagerCommandHelperFactory {
    public static final String PYPROJECT_TOML = "pyproject.toml";
    public static PackageManagerCommandHelper createPackageManagerCommandHelperSetup(File workingDirectory) {
        if (HabushuUtil.checkPythonPackageManager(new File(workingDirectory, PYPROJECT_TOML)) == HabushuUtil.PackageManager.POETRY){
            return new PoetryCommandHelper(workingDirectory);
        } else {
            // TODO: Implement UV Command Helper
            throw new NotImplementedException(" UV Implementation is not supported yet.");
        }
    }
}
