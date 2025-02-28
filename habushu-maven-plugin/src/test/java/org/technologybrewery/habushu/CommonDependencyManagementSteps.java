package org.technologybrewery.habushu;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is helper class that shares same logic for UvDependencyManagementSteps and PoetryDependencyManagementSteps
 */
public class CommonDependencyManagementSteps {

    protected static DependencyManagementTestMojo createMojoWithManagedDependency(String packageName, String operatorAndVersion, boolean isActive) {
        DependencyManagementTestMojo mojo = new DependencyManagementTestMojo();

        List<PackageDefinition> managedDependencies = new ArrayList<>();
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setPackageName(packageName);
        packageDefinition.setOperatorAndVersion(StringEscapeUtils.unescapeJava(operatorAndVersion));
        packageDefinition.setActive(isActive);
        managedDependencies.add(packageDefinition);

        mojo.setManagedDependencies(managedDependencies);

        return mojo;
    }

    protected static void createPyProjectTomlFiles(String baseFilePath, File originalPyProjectToml, File finalPyProjectToml) throws IOException {
        File baseFile = new File(baseFilePath);
        FileUtils.copyFile(baseFile, originalPyProjectToml);
        FileUtils.copyFile(baseFile, finalPyProjectToml);
    }

    protected static PackageDefinition getPackageDefinition(String packageName, String operatorVersion) {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setPackageName(packageName);
        packageDefinition.setOperatorAndVersion(operatorVersion);
        return packageDefinition;
    }

}
