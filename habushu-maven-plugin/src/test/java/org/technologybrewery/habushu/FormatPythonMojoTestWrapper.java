package org.technologybrewery.habushu;

import org.technologybrewery.habushu.exec.AbstractCommandHelper;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import java.io.File;
import java.util.Arrays;

public class FormatPythonMojoTestWrapper extends FormatPythonMojo {

    private boolean packageInstallAttempted = false;
    public void setSourceDirectory(File dir) {
        this.sourceDirectory = dir;
    }
    public void setTestDirectory(File dir) {
        this.testDirectory = dir;
    }
    protected void enableFormatter(boolean enabled) { this.useFormatter=enabled; }
    public boolean wasPackageInstallAttempted() {
        return packageInstallAttempted;
    }

    @Override
    protected PoetryCommandHelper createPoetryCommandHelper() {
        return new PoetryCommandHelper(this.sourceDirectory);
    }

    @Override
    protected UvCommandHelper createUvCommandHelper() {
        return new UvCommandHelper(this.sourceDirectory);
    }

    @Override
    protected void downloadFormatterIfNotPresent(AbstractCommandHelper helper) {
        if (!helper.isDependencyInstalled(FORMATTER_PACKAGE)) {
            super.downloadFormatterIfNotPresent(helper);
            packageInstallAttempted = true;
        } else {
            getLog().info(String.format("Successfully found %s dependency", FORMATTER_PACKAGE));
        }
    }

    @Override
    protected File getPyProjectTomlFile() {
        return new File("target/test-classes/test-formatter/workdir/pyproject.toml");
    }

    @Override
    protected File getPythonProjectBaseDir() {
        return new File("target/test-classes/test-formatter/workdir");
    }
}
