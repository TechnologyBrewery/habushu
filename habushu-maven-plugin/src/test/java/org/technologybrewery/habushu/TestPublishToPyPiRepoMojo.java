package org.technologybrewery.habushu;

import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PythonRepository;

import java.io.File;

/**
 * Contains method to make testing easier and set deploy Mojo values that would be done by Maven in normal use.
 */
public class TestPublishToPyPiRepoMojo extends PublishToPyPiRepoMojo {

    public TestPublishToPyPiRepoMojo() {
        super();

        //mimic defaults in Mojo:
        this.pypiRepoId = PythonRepository.PUBLIC_PYPI_REPO_ID;
        this.useDevRepository = false;
        this.devRepositoryId = PythonRepository.TEST_PYPI_REPO_ID;
        this.devRepositoryUrl = PythonRepository.TEST_PYPI_REPO_URL;

        this.skipDeploy = false;
        this.pypiUploadSuffix = "";
        this.devRepositoryUrlUploadSuffix = "legacy/";
        this.enableDevRepositoryUrlUploadSuffix = true;
    }

    public String getRepositoryUrl(boolean publishToDev) {
        PublishToPyPiRepoPoetry publishToPyPiRepoPoetry = new PublishToPyPiRepoPoetry(new File("target/"), getLog(), this);
        return publishToPyPiRepoPoetry.getRepositoryUrl(publishToDev);
    }

}
