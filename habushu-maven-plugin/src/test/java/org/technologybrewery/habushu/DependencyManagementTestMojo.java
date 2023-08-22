package org.technologybrewery.habushu;

import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.util.List;

/**
 * Contains method to make testing easier and set defautl Mojo values that would be done by Maven in normal use.
 */
public class DependencyManagementTestMojo extends InstallDependenciesMojo {

    private File pyProjectTomlFile;

    private Semver poetryVersion;

    public DependencyManagementTestMojo(File pyProjectTomlFile) {
        this.pyProjectTomlFile = pyProjectTomlFile;

        //mimic defaults in Mojo:
        this.updateManagedDependenciesWhenFound = true;
        this.overridePackageVersion = true;
    }

    void setManagedDependencies(List<PackageDefinition> managedDependencies) {
        this.managedDependencies = managedDependencies;
    }

    void setUpdateManagedDependenciesWhenFound(boolean shouldUpdate) {
        this.updateManagedDependenciesWhenFound = shouldUpdate;
    }

    void setFailOnManagedDependenciesMismatches(boolean shouldFail) {
        this.failOnManagedDependenciesMismatches = shouldFail;
    }

    protected File getPoetryPyProjectTomlFile() {
        return pyProjectTomlFile;
    }

    protected void setPoetryVersion(String version) {
        this.poetryVersion = new Semver(version);
    }

    protected Semver getPoetryVersion() {
        return poetryVersion != null ? poetryVersion : new Semver("1.5.0");
    }
}
