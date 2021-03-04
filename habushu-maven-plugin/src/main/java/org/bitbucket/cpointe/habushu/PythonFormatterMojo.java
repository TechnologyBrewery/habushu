package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.ReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A formatting plugin designed to format all Python files found in certain directories.
 */
@Mojo(name = "format-python", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PythonFormatterMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(PythonFormatterMojo.class);

    private static final String TAB_CHAR = "\t";
    private static final String FOUR_SPACES = "    ";

    /**
     * The type of file to be formatted by this Mojo. Defaults to "py".
     */
    @Parameter(property = "fileExtensionsTargeted", required = true, defaultValue = "py")
    protected List<String> fileExtensionsTargeted;

    /**
     * Folder in which Python source files are located.
     */
    @Parameter(property = "pythonSourceMainDirectory", required = true, defaultValue = "${project.basedir}/src/main/python")
    protected File pythonSourceMainDirectory;

    /**
     * Folder in which Python test files are located.
     */
    @Parameter(property = "pythonSourceTestDirectory", required = true, defaultValue = "${project.basedir}/src/test/python")
    protected File pythonSourceTestDirectory;

    /**
     * The source encoding used for python.
     */
    @Parameter(property = "projectBuildSourceEncoding", defaultValue = "${project.build.sourceEncoding}")
    private String projectBuildSourceEncoding;

    @Override
    public void execute() throws MojoExecutionException {
        if (StringUtils.isEmpty(projectBuildSourceEncoding)) {
            projectBuildSourceEncoding = ReaderFactory.FILE_ENCODING;
            logger.warn("File encoding has not been set, using platform encoding {}, i.e. build is platform dependent!",
                    projectBuildSourceEncoding);

        }

        if (pythonSourceMainDirectory.exists()) {
            formatPythonFilesInDirectory(pythonSourceMainDirectory);

        } else {
            logger.warn("Directory {} does not exist!", pythonSourceMainDirectory);

        }

        if (pythonSourceTestDirectory.exists()) {
            formatPythonFilesInDirectory(pythonSourceTestDirectory);

        } else {
            logger.warn("Directory {} does not exist!", pythonSourceTestDirectory);

        }
    }

    /**
     * Formats all Python files found within, or recursively under, the given directory.
     * 
     * @param directory
     *            the directory containing files to format
     */
    private void formatPythonFilesInDirectory(File directory) {
        Collection<File> pythonFilesToFormat = listPythonFilesInDirectory(directory);

        if (!pythonFilesToFormat.isEmpty()) {
            for (File pyFile : pythonFilesToFormat) {
                logger.debug("Formatting {}", pyFile.getName());
                replaceTabsWithSpaces(pyFile);
            }
        }

        logger.info("Formatted {} files found in {}", pythonFilesToFormat.size(), directory);
    }

    /**
     * Replace tabs with spaces in the provided file.
     * 
     * @param pythonFile
     *            file to format
     */
    private void replaceTabsWithSpaces(File pythonFile) {
        Charset charset = Charset.forName(projectBuildSourceEncoding);
        String content = null;

        if (!pythonFile.exists()) {
            logger.info("Could not find path to file: {}", pythonFile.getName());
            return;
        }

        Path path = Paths.get(pythonFile.getAbsolutePath());
        try {
            content = new String(Files.readAllBytes(path), charset);
            content = content.replace(TAB_CHAR, FOUR_SPACES);
            Files.write(path, content.getBytes(charset));
        } catch (IOException e) {
            throw new HabushuException("Error encountered when attempting to format Python file!", e);
        }
    }

    /**
     * Recursively lists all Python files found in the given directory and all sub-directories.
     * 
     * @param directory
     *            the root directory from which to search recursively down
     * @return all Python files found
     */
    private Collection<File> listPythonFilesInDirectory(File directory) {
        String[] fileExtensions = fileExtensionsTargeted.toArray(new String[0]);
        return FileUtils.listFiles(directory, fileExtensions, true);

    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
