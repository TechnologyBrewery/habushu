package org.technologybrewery.habushu;

import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.util.List;

/**
 * Contains method to make testing easier and set default Mojo values that would be done by Maven in normal use.
 */
public class DependencyManagementTestMojo extends InstallDependenciesMojo {

    private Semver poetryVersion;

    public DependencyManagementTestMojo() {
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

    protected void setPoetryVersion(String version) {
        this.poetryVersion = new Semver(version);
    }

    protected Semver getPoetryVersion() {
        return poetryVersion != null ? poetryVersion : new Semver("1.5.0");
    }

    protected void processManagedDependencyMismatchesPoetry(){
        InstallDependenciesPoetry installDependenciesPoetry = new InstallDependenciesPoetry(new File("target/"), getLog(), this);
        installDependenciesPoetry.processManagedDependencyMismatches();
    }

    protected void processManagedDependencyMismatchesUv(){
        InstallDependenciesUv installDependenciesUv = new InstallDependenciesUv(new File("target/"), getLog(), this);
        installDependenciesUv.processManagedDependencyMismatches();


    }
}
