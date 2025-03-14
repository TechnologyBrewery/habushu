package org.technologybrewery.habushu;

import java.io.File;

/**
 * Contains method to make testing easier and set deploy Mojo values that would be done by Maven in normal use.
 */
public class TestPublishToPyPiRepoMojo extends PublishToPyPiRepoMojo {

    public TestPublishToPyPiRepoMojo() {
        super();

        //mimic defaults in Mojo:
        this.pypiRepoId = PUBLIC_PYPI_REPO_ID;
        this.useDevRepository = false;
        this.devRepositoryId = DEV_PYPI_REPO_ID;
        this.devRepositoryUrl = TEST_PYPI_REPOSITORY_URL;

        this.skipDeploy = false;
        this.pypiUploadSuffix = "";
        this.devRepositoryUrlUploadSuffix = "legacy/";
    }

    public String getRepositoryUrl(boolean publishToDev) {
        PublishToPyPiRepoPoetry publishToPyPiRepoPoetry = new PublishToPyPiRepoPoetry(new File("target/"), getLog(), this);
        return publishToPyPiRepoPoetry.getRepositoryUrl(publishToDev);
    }

    public String addTrailingSlash(String inputUrl) {
        return AbstractPublishToPyPiRepo.addTrailingSlash(inputUrl);
    }

}
