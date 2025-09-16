package org.technologybrewery.habushu.util;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import org.codehaus.plexus.util.StringUtils;
import org.technologybrewery.habushu.HabushuException;

public class RequirementsFileHelper {
    public static final Pattern LOCAL_PATH = Pattern.compile("(^|\\s)[^\\s/:]*(/\\S+)+/?");
    public static final Pattern URI_PATH = Pattern.compile("file://\\S*?/\\S+", Pattern.CASE_INSENSITIVE);
    public static final String COMMENT = "#";
    private Path requirementsFilePath;
    private Path requirementsFileBaseDir;
    private Map<String, Path> parsedPathRequirements;

    public RequirementsFileHelper(Path requirementsFilePath, Path requirementsFileBaseDir) {
        this.requirementsFilePath = requirementsFilePath;
        this.requirementsFileBaseDir = requirementsFileBaseDir;
    }

    public Path getRequirementsFilePath() {
        return requirementsFilePath;
    }

    public Path getRequirementsFileBaseDir() {
        return requirementsFileBaseDir;
    }

    // Defensive copy of resolved path-based requirements since the Map returned by getResolvedPathRequirements is modifiable
    public List<Path> getPathBasedRequirements(){
        return new ArrayList<>(getResolvedPathRequirements().values());
    }

    /**
     * Rewrites path-based dependencies in the requirements file using the provided relocation mapping.
     *
     * @param relocationMapping mapping from the original resolved path to the
     * replacement path to write into requirements.txt
     * @throws HabushuException
     */
    public void relocatePathRequirements(Map<Path, RequirementReplacement> relocationMapping) {
        Path requirements = getRequirementsFilePath();
        Path tempFile = requirements.resolveSibling(requirements.getFileName() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile);
             Stream<String> lines = Files.lines(requirements)) {
            lines.forEach(line -> {
                try {
                    String updatedLine = relocatePathsOnLine(relocationMapping, line);
                    writer.write(updatedLine);
                    writer.newLine();
                } catch (IOException e) {
                    throw new HabushuException("Failed to update requirements line: " + line, e);
                }
            });

            writer.flush();
            Files.move(tempFile, requirements, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new HabushuException("Failed to update requirements file: " + requirements, e);
        }
    }

    /**
     * Rewrites a single requirements line by replacing the longest matching path token
     * with its relocated file URI (and optional hash).
     * Strips leading editable flags before replacement.
     *
     * @param relocationMapping mapping from resolved original path to its replacement info
     * @param line the original requirements line
     * @return the updated line with the matching token replaced
     */
    private String relocatePathsOnLine(Map<Path, RequirementReplacement> relocationMapping, String line) {
        String longestToken = "";
        String newPath = "";
        // strip editable tags for container installation -- we have to always do this because this line may have a
        // continuation escape and the next line has a replaceable token
        line = line.replaceFirst("(^|\\s+)-e\\s+", "");
        for (Map.Entry<String, Path> entry : getResolvedPathRequirements().entrySet()) {
            String token = entry.getKey();
            Path resolvedPath = entry.getValue();
            if (relocationMapping.containsKey(resolvedPath) && token.length() > longestToken.length()) {
                if (line.contains(token)) {
                    longestToken = token;
                    Path newFile = relocationMapping.get(resolvedPath).getNewFile();
                    newPath = "file://" + relocationMapping.get(resolvedPath).getNewToken();
                    if(!Files.isRegularFile(resolvedPath) && Files.exists(newFile)) {
                        newPath += "\\\n    --hash=sha256:" + calculateSha256(newFile);
                    }
                }
            }
        }
        return line.replace(longestToken, newPath);
    }

    private Map<String, Path> getResolvedPathRequirements() {
        if (parsedPathRequirements == null) {
            parsePathBasedRequirements();
        }
        return parsedPathRequirements;
    }

    /**
     * Parses the requirements file, skipping blank and comment lines.
     *
     * @throws HabushuException
     */
    private void parsePathBasedRequirements() {
        parsedPathRequirements = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(requirementsFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith(COMMENT) || line.trim().isEmpty()) {
                    continue;
                }
                parseRequirement(line);
            }
        } catch (URISyntaxException | IOException e) {
            throw new HabushuException("Failed to parse requirements file: " + requirementsFilePath, e);
        }
    }

    /**
     * Parses a single line in requirements file.
     * If it contains a local path or URI, records the token and its resolved absolute path in {@code parsedPathRequirements}.
     *
     * @param currentLine a single requirements line
     * @throws URISyntaxException
     */
    private void parseRequirement(String currentLine) throws URISyntaxException {
        String token = null;
        String path = null;
        Matcher uriMatcher = URI_PATH.matcher(currentLine);
        Matcher localMatcher = LOCAL_PATH.matcher(currentLine);
        if (uriMatcher.find()) {
            token = uriMatcher.group().trim();
            path = getPathFromUri(token.trim());
        } else if (localMatcher.find()) {
            token = path = localMatcher.group().trim();
        }

        if (path != null) {
            Path resolvedPath = requirementsFileBaseDir.resolve(path).toAbsolutePath().normalize();
            parsedPathRequirements.put(token, resolvedPath);
        }
    }

    private static String getPathFromUri(String uri) throws URISyntaxException {
        String path = null;
        URI requirementUri = new URI(uri);
        if ("file".equalsIgnoreCase(requirementUri.getScheme())) {
            //only need to collect File URIs that are on the local host machine, not any remote hosts
            if (StringUtils.isBlank(requirementUri.getAuthority()) || "localhost".equals(requirementUri.getAuthority())) {
                path = requirementUri.getPath();
            }
        }
        return path;
    }

    private static String calculateSha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return Hex.encodeHexString(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new HabushuException("Failed to calculate SHA256 hash for file: " + file, e);
        }
    }

    public static class RequirementReplacement {
        private Path newFile;
        private String newToken;

        public RequirementReplacement(Path newFile, String newToken) {
            this.newFile = newFile;
            this.newToken = newToken;
        }

        public Path getNewFile() {
            return newFile;
        }

        public String getNewToken() {
            return newToken;
        }
    }
}
