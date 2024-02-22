package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RetrieveWheelsSteps {

    private RetrieveWheelsTestMojo mojo; 
    File sampleWheelFile = new File("src/test/resources/testCacheDirectory/base-test-wheel.whl");

    @After
    public void cleanUp() {
        resetTargetDirectory();
    }

    @Given("a Habushu configuration with no wheel dependencies entries")
    public void a_habushu_configuration_with_no_wheel_dependency_entries() throws Exception {
        mojo = new RetrieveWheelsTestMojo(sampleWheelFile);
        mojo.setWheelDependencies(new ArrayList<>());
    }

    @Given("a Habushu configuration with a wheel dependency")
    public void a_habushu_configuration_with_a_wheel_dependency_with_and() {
        mojo = new RetrieveWheelsTestMojo(sampleWheelFile);
        List<WheelDependency> wheelDependencies = new ArrayList<>();
        WheelDependency wheelDependency = new WheelDependency();
        wheelDependency.setArtifactId("testCacheDirectory");
        wheelDependency.setTargetDirectory("src/test/resources/testTargetDirectory");
        wheelDependencies.add(wheelDependency);
        mojo.setWheelDependencies(wheelDependencies);
    }

    @When("Habushu executes retrieve wheel dependencies")
    public void habushu_executes_retrieve_wheel_dependencies() throws Exception {
        mojo.processWheelDependencies();
    }

    @Then("no wheel artifacts are copied")
    public void no_wheel_artifacts_are_copied() {
        //asset the wheel dependency target directory doesn't contain the poetry cache wheel artifact        
        Assertions.assertFalse(checkIfWheelWasCopied(), "Expected the wheel artifact not to be copied, and it was!");
    }

    /**
     * Asserts that the wheel artifact Identified by artifactId has been copied exactly into the target
     * directory. For instance, if the passed in artifactId has a cooresponding directory in poetry cache
     * a file at "poetry_cache_dir/artifacts/wheels/myWheelArtifact.whl" and the target directory has a file at 
     * "targetDirectory/myWheelArtifact.whl", this method will pass.
     */    
    @Then("the wheel artifact is copied")
    public void the_wheel_artifact_is_copied() {
        //asset the wheel dependency target directory contains the poetry cache wheel artifact
        Assertions.assertTrue(checkIfWheelWasCopied(), "Expected the wheel artifact in the target directory, but didn't find it!");
    }

    private boolean checkIfWheelWasCopied(){
        boolean isWheelCopied = false;
        String artifactId = "";
        String targetDirectory = ""; 
        List<WheelDependency> wheelDependencies = mojo.getWheelDependencies();
        for (WheelDependency wd : wheelDependencies) {
            artifactId = wd.getArtifactId();
            targetDirectory = wd.getTargetDirectory();
            File artifactPoetryCacheDirectory = mojo.getCachedWheelDirectory(artifactId);
            List<File> wheelFiles = Stream.of(artifactPoetryCacheDirectory.listFiles())
                                          .filter(file -> file.getAbsolutePath().endsWith(".whl"))
                                          .map(File::getAbsoluteFile)
                                          .collect(Collectors.toList());    
            for (File f : wheelFiles){
                isWheelCopied = new File(targetDirectory, f.getName()).exists();
                System.out.println(targetDirectory);
                System.out.println(f.getName());
                System.out.println(isWheelCopied);
            }     
        }  
        return isWheelCopied;      
    }

    private void resetTargetDirectory(){
        File baseTestWheelFile = new File("src/test/resources/testTargetDirectory/base-test-wheel.whl");
        if(baseTestWheelFile.exists()) baseTestWheelFile.delete();
    }
}
