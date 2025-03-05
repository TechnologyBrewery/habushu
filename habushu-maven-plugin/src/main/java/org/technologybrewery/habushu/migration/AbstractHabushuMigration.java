package org.technologybrewery.habushu.migration;

import java.io.File;

import org.technologybrewery.baton.AbstractMigration;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;

public abstract class AbstractHabushuMigration extends AbstractMigration {

    protected boolean isPoetryProject(File pyProjectTomlFile) {
        return PackageManager.POETRY.equals(HabushuUtil.checkPythonPackageManager(pyProjectTomlFile));
    }
}
