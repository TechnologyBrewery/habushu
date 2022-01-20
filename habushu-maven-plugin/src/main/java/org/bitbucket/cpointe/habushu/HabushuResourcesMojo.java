package org.bitbucket.cpointe.habushu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.resources.CopyResourcesMojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overrides the default Maven resources plugin to copy artifacts to a staging directory for archiving in a future
 * lifecycle step. Also pulls down the python source directory along with project level artifacts. In a traditional Java
 * build within Maven, source is moved during compilation. But with an interpreted language, we need to pull it down as
 * a copy since there is no compilation.
 */
@Mojo(name = "resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true, threadSafe = true)
public class HabushuResourcesMojo extends CopyResourcesMojo {

    private static final Logger logger = LoggerFactory.getLogger(HabushuResourcesMojo.class);

    /**
     * The list of resources we want to transfer.
     */
    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    /**
     * Folder in which python source files are located.
     */
    @Parameter(property = "pythonSourceDirectory", required = true, defaultValue = "${project.basedir}/src/main/python")
    protected File pythonSourceDirectory;

    /**
     * Folder in which standard maven resources files are located.
     */
    @Parameter(property = "resourcesDirectory", required = true, defaultValue = "${project.basedir}/src/main/resources")
    protected File resourcesDirectory;

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + AbstractHabushuMojo.DEFAULT_STAGING_FOLDER, required = true)
    private File outputDirectory;

    /**
     * The name of the python package.  Defaults to artifactId.
     */
    @Parameter(property = "packageName", required = true, defaultValue = "${project.artifactId}")
    private String packageName;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException {
        List<Resource> pluginResources = new ArrayList<>();

        checkRequiredPackageFilesAndCreateIfNotAvailable(pythonSourceDirectory);

        Resource source = new Resource();
        source.setDirectory(pythonSourceDirectory.getAbsolutePath());
        pluginResources.add(source);

        Resource resourceDirectory = new Resource();
        resourceDirectory.setDirectory(resourcesDirectory.getAbsolutePath());
        pluginResources.add(resourceDirectory);

        Resource baseFolderResources = new Resource();
        baseFolderResources.setDirectory(".");
        baseFolderResources.addInclude("pom.xml");
        baseFolderResources.addInclude(AbstractHabushuMojo.VENV_DEPENDENCY_FILE_NAME);
        pluginResources.add(baseFolderResources);

        setResources(pluginResources);

        super.execute();
    }

    /**
     * Checks for the existence of LICENSE, README.md and setup.py.  If they do not exist then default files are generated.
     * @param packagePath
     * @throws IOException
     */
    private void checkRequiredPackageFilesAndCreateIfNotAvailable(File packagePath) throws MojoExecutionException {
        boolean licenseExists = new File(packagePath, "LICENSE").exists();
        boolean readmeExists = new File(packagePath, "README.md").exists();
        boolean setupExists = new File(packagePath, "setup.py").exists();

        try {
            if(!licenseExists) {
                logger.warn("LICENSE file not found.  Creating empty file for packaging.");
                createFileFromFileNameAndContent(packagePath, "LICENSE", "");
            }

            if(!readmeExists) {
                logger.warn("README.md file not found.  Creating empty file for packaging.");
                createFileFromFileNameAndContent(packagePath, "README.md", "");
            }
            if(!setupExists) {
                logger.warn("setup.py file not found. Creating new default setup.py");
                createSetupFile(packagePath);
            }
        } catch(IOException e) {
            logger.error("Error while creating package setup files: {}", e.getMessage());
            throw new MojoExecutionException("Error creating package setup files");
        }
    }

    /**
     * Creates a file and adds the provided content to the file.
     * @param packagePath
     * @param fileName
     * @param content
     * @throws IOException
     */
    private void createFileFromFileNameAndContent(File packagePath, String fileName, String content) throws IOException {
        BufferedWriter bw = null;
        try{
            File file = new File(packagePath, fileName);
            file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
            bw.write(content);
        } catch (IOException e) {
            logger.error("Couldn't create package files: {}", e.getMessage());
        } finally {
            if(bw != null){
                bw.close();
            }
        }
    }

    /**
     * Creates a default setup.py file used for installing the Python package.
     * @param packagePath
     * @throws IOException
     */
    private void createSetupFile(File packagePath) throws IOException {
        String content = "import setuptools\n\n" +
                "with open(\"README.md\", \"r\", encoding=\"utf-8\") as fh:\n" +
                "    long_description = fh.read()\n\n" +
                "setuptools.setup(\n" +
                "    name=\"" + packageName + "\",\n" +
                "    version=\"0.0.1\",\n" +
                "    description=\"Package generated by Habushu\",\n" +
                "    long_description=long_description,\n" +
                "    long_description_content_type=\"text/markdown\",\n" +
                "    packages=setuptools.find_packages(),\n" +
                "    zip_safe= True,\n" +
                "    classifiers=[\n" +
                "        \"Programming Language :: Python :: 3\",\n" +
                "        \"License :: OSI Approved :: MIT License\",\n" +
                "        \"Operating System :: OS Independent\",\n" +
                "    ],\n" +
                "    python_requires='>=3.9.1',\n" +
                ")";
        createFileFromFileNameAndContent(packagePath, "setup.py", content);
    }

}