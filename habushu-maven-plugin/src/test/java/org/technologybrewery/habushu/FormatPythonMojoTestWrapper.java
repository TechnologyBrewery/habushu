package org.technologybrewery.habushu;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;

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
    protected void downloadFormatterIfNotPresent(CommandHelper helper) {

        // These are pre-requesite steps to showing installed dependencies
        if (helper instanceof PoetryCommandHelper) {
            helper.execute(Arrays.asList("sync", "--no-root"));
        } else { // UvCommandHelper
            helper.execute(List.of("sync"));
        }

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
