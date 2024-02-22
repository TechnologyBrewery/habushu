package org.technologybrewery.habushu;

import java.io.File;
import java.util.List;

public class RetrieveWheelsTestMojo extends retrieveWheelsMojo{

    private File sampleWheelFile;

    public RetrieveWheelsTestMojo(File sampleWheelFile) {
        this.sampleWheelFile = sampleWheelFile;
    }

    void setWheelDependencies(List<WheelDependency> wheelDependencies) {
        this.wheelDependencies = wheelDependencies;
    }

    List<WheelDependency> getWheelDependencies() {
        return this.wheelDependencies;
    }

    protected File getPoetryCacheDirectory(){
        return getPoetryCacheDirectory();
    }

    public File getCachedWheelDirectory(String artifactId){
        String baseDirectory = new File("").getAbsolutePath();
        return new File(baseDirectory+"/src/test/resources/" + artifactId);
    }
}
