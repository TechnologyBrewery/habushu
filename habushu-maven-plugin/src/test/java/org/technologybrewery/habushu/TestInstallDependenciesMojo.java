package org.technologybrewery.habushu;

import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Contains method to make testing easier and set deploy Mojo values that would be done by Maven in normal use.
 */
public class TestInstallDependenciesMojo extends InstallDependenciesMojo {

    public TestInstallDependenciesMojo() {
        super();

        //mimic defaults in Mojo:
        this.pypiRepoId = HabushuUtil.PUBLIC_PYPI_REPO_ID;
        this.useDevRepository = false;
        this.devRepositoryId = DEV_PYPI_REPO_ID;
        this.devRepositoryUrl = TEST_PYPI_REPOSITORY_URL;

        this.enablePypiSimpleSuffix = true;
        this.pypiSimpleSuffix = "simple";
    }

    public String getPypiSimpleRepoUrl(String pypiRepoUrl, boolean isPoetry) throws URISyntaxException {
        String pypiSimpleRepoUrl;
        if (isPoetry){
            InstallDependenciesPoetry installDependenciesPoetry = new InstallDependenciesPoetry(new File("target/"), getLog(), this);
            pypiSimpleRepoUrl = installDependenciesPoetry.getPyPiRepoSimpleIndexUrl(pypiRepoUrl);
        } else {
            InstallDependenciesUv installDependenciesUv = new InstallDependenciesUv(new File("target/"), getLog(), this);
            pypiSimpleRepoUrl = installDependenciesUv.getPyPiRepoSimpleIndexUrl(pypiRepoUrl);
        }
        return pypiSimpleRepoUrl;
    }

    public boolean getShouldAddPriority(boolean isPoetry){
        boolean shouldAddPriority;
        if (isPoetry){
            InstallDependenciesPoetry installDependenciesPoetry = new InstallDependenciesPoetry(new File("target/"), getLog(), this);
            shouldAddPriority = installDependenciesPoetry.shouldAddPriority();
        } else {
            InstallDependenciesUv installDependenciesUv = new InstallDependenciesUv(new File("target/"), getLog(), this);
            shouldAddPriority = installDependenciesUv.shouldAddPriority();
        }
        return shouldAddPriority;
    }

}
