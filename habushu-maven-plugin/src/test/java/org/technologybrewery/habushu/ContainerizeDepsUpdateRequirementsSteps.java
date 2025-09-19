package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.technologybrewery.habushu.util.RequirementsFileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ContainerizeDepsUpdateRequirementsSteps {
    private static final Path REQUIRED_WHEELS = Path.of("target/containerize-support/required");
    private RequirementsFileHelper requirementsHelper;
    private Map<Path, RequirementsFileHelper.RequirementReplacement> pathMappings;
    private Collection<Path> parseResult;

    @After("@containerizeDependenciesUpdateRequirements")
    public void after() throws IOException {
        if (requirementsHelper != null) {
            FileUtils.deleteDirectory(requirementsHelper.getRequirementsFileBaseDir().toFile());
        }
    }

    @Given("the following requirement file based at {string}:")
    public void theFollowingRequirementFileBasedAt(String baseDir, String contents) throws IOException {
        Path requirementsBaseDir = Path.of(injectAbsPathInRequirements(baseDir));
        Path requirementsFilePath = requirementsBaseDir.resolve("dist").resolve("requirements.txt");
        contents = injectAbsPathInRequirements(contents);
        Files.createDirectories(requirementsFilePath.getParent());
        Files.writeString(requirementsFilePath, contents);
        requirementsHelper = new RequirementsFileHelper(requirementsFilePath, requirementsBaseDir);
    }

    @Given("the following path mappings:")
    public void theFollowingPathMappings(Map<String, String> mappings) throws IOException {
        pathMappings = new HashMap<>(mappings.size());
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String key = entry.getKey();
            key = injectAbsPathInRequirements(key);
            String value = entry.getValue();
            value = injectAbsPathInRequirements(value);
            Path path = Path.of(value);
            // Create a file that can be used to calculate the SHA-256 hash if it's required
            Path hashableFile = REQUIRED_WHEELS.resolve(path.getFileName());
            if (!Files.exists(hashableFile)) {
                Files.createDirectories(hashableFile.getParent());
                Files.writeString(hashableFile, "SomeWheelContentInZipFormat:" + hashableFile.getFileName());
            }
            pathMappings.put(Path.of(key), new RequirementsFileHelper.RequirementReplacement(hashableFile, value));
        }
    }

    @When("wheels are relocated to staging directory")
    public void wheelsAreRelocatedToStagingDirectory() {
        requirementsHelper.relocatePathRequirements(pathMappings);
    }

    @When("path-based requirements are retrieved")
    public void path_based_requirements_are_retrieved(){
        parseResult = requirementsHelper.getPathBasedRequirements();
    }

    @Then("the requirements file is updated to:")
    public void theRequirementsFileIsUpdatedTo(String expectedContent) throws IOException {
        List<String> actualContent = Files.readAllLines(requirementsHelper.getRequirementsFilePath());
        String[] split = expectedContent.split("\n");
        for (int i = 0; i < split.length; i++) {
            String expectedLine = split[i];
            String actualLine = actualContent.get(i);
            Assertions.assertEquals(expectedLine, actualLine, "Line " + i + " of requirements file does not match.");
        }
    }

    @Then("the following paths are returned:")
    public void theFollowingPathsAreReturnedPathBasedRequirements(List<String> paths) {
        List<String> expectedPaths = paths.stream()
                .map(this::injectAbsPathInRequirements)
                .collect(Collectors.toList());
        ArrayList<String> notFound = new ArrayList<>();
        ArrayList<String> extra = new ArrayList<>();
        for (String expectedPath : expectedPaths) {
            if (!parseResult.contains(Path.of(expectedPath))) {
                notFound.add(expectedPath);
            }
        }
        for (Path path : parseResult) {
            if (!expectedPaths.contains(path.toString())) {
                extra.add(path.toString());
            }
        }
        Assertions.assertEquals(notFound, List.of(), "Expected paths were not found.");
        Assertions.assertEquals(List.of(), extra, "Extra unexpected paths were found");
    }

    private String injectAbsPathInRequirements(String contents) {
        contents = contents.replaceAll("/<workingdir>", Path.of("").toAbsolutePath().toString());
        return contents;
    }
}
