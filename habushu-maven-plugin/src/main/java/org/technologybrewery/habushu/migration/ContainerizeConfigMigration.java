package org.technologybrewery.habushu.migration;

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.technologybrewery.baton.util.pom.PomHelper;
import org.technologybrewery.baton.util.pom.PomModifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.technologybrewery.baton.util.pom.LocationAwareMavenReader.END;
import static org.technologybrewery.baton.util.pom.LocationAwareMavenReader.START;

/**
 * Migrates configuration of the containerize-dependencies goal to account for the new containerization approach of using
 * the wheel files instead of building from source.
 */
public class ContainerizeConfigMigration extends AbstractHabushuMigration {
    public static final Logger logger = LoggerFactory.getLogger(ContainerizeConfigMigration.class);
    public static final String HABUSHU_PLUGIN_COORDINATE = "org.technologybrewery.habushu:habushu-maven-plugin";
    public static final List<String> REMOVE_CONFIGS = List.of(
            "defaultSourceSet",
            "dockerPoetryVersion",
            "dockerPoetryPluginBundleVersion",
            "dockerUvVersion"
    );
    public static final Map<String, String> RENAME_CONFIGS = Map.of(
            "dockerPoetryBuilderStageTemplatePath", "dockerBuilderStageTemplate",
            "dockerPoetryFinalStageTemplatePath", "dockerFinalStageTemplate",
            "dockerUvBuilderStageTemplatePath", "dockerBuilderStageTemplate",
            "dockerUvFinalStageTemplatePath", "dockerFinalStageTemplate",
            "skipPoetryLockUpdate", "skipLockUpdate"
    );

    private List<ConfigurationContainer> habushuConfiguration;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        try {
            Model model = PomHelper.getLocationAnnotatedModel(file);
            habushuConfiguration = new ArrayList<>();
            for (PluginContainer plugins : getPluginContainers(model)) {
                Plugin habushu = plugins.getPluginsAsMap().get(HABUSHU_PLUGIN_COORDINATE);
                if (habushu != null) {
                    if(isConfigured(habushu)) {
                        habushuConfiguration.add(habushu);
                    }
                    for (PluginExecution execution : habushu.getExecutions()) {
                        if(isConfigured(execution)) {
                            habushuConfiguration.add(execution);
                        }
                    }
                }
            }
            return !habushuConfiguration.isEmpty();
        } catch (Exception e) {
            logger.warn("Failed to load POM file to check for Habushu configurations. " + e.getMessage());
            logger.debug("  stacktrace:", e);
            return false;
        }
    }

    @Override
    protected boolean performMigration(File file) {
        PomModifications allModifications = new PomModifications();
        for (ConfigurationContainer configured : habushuConfiguration) {
            Xpp3Dom configuration = (Xpp3Dom) configured.getConfiguration();
            PomModifications configModifications = new PomModifications();
            int removed = 0;
            for (int i = 0; i < configuration.getChildren().length; i++) {
                Xpp3Dom child = configuration.getChild(i);
                if (shouldDelete(child)) {
                    configModifications.add(removeConfig(configured, i));
                    removed++;
                } else if (shouldRename(child)) {
                    configModifications.add(renameConfig(configured, i));
                }
            }
            if (removed == configuration.getChildCount()) {
                allModifications.add(removeConfigurationTag(configured));
            } else {
                allModifications.addAll(configModifications);
            }
        }
        return allModifications.isEmpty() || PomHelper.writeModifications(file, allModifications.finalizeMods());
    }

    private static List<PluginContainer> getPluginContainers(Model model) {
        List<PluginContainer> containers = new ArrayList<>();
        BuildBase build = model.getBuild();
        containers.add(build);
        if (build.getPluginManagement() != null) {
            containers.add(build.getPluginManagement());
        }
        for (Profile profile : model.getProfiles()) {
            build = profile.getBuild();
            containers.add(build);
            if (build.getPluginManagement() != null) {
                containers.add(build.getPluginManagement());
            }
        }
        return containers;
    }

    private static boolean isConfigured(ConfigurationContainer configured) {
        Object configuration = configured.getConfiguration();
        if (configuration instanceof Xpp3Dom) {
            return ((Xpp3Dom) configuration).getChildCount() > 0;
        }
        return false;
    }

    private static PomModifications.Deletion removeConfig(ConfigurationContainer configurable, int configIndex) {
        InputLocation start = getConfigStart(configurable, configIndex);
        InputLocation end = getConfigEnd(configurable, configIndex);
        return new PomModifications.Deletion(start, end);
    }

    private static PomModifications.Replacement renameConfig(ConfigurationContainer configurable, int configIndex) {
        Xpp3Dom child = ((Xpp3Dom) configurable.getConfiguration()).getChild(configIndex);
        InputLocation start = getConfigStart(configurable, configIndex);
        InputLocation end = getConfigEnd(configurable, configIndex);
        String newTag = RENAME_CONFIGS.get(child.getName());
        String renamed = StringUtils.repeat(" ", start.getColumnNumber() - 1)
                + "<" + newTag + ">"
                + child.getValue()
                + "</" + newTag + ">\n";
        return new PomModifications.Replacement(start, end, renamed);
    }

    private static PomModifications.Deletion removeConfigurationTag(ConfigurationContainer configurable) {
        InputLocation configStart = configurable.getLocation("configuration" + START);
        InputLocation configEnd = configurable.getLocation("configuration" + END);
        return new PomModifications.Deletion(configStart, configEnd);
    }

    private static InputLocation getConfigStart(ConfigurationContainer configurable, int configIndex) {
        Xpp3Dom configuration = (Xpp3Dom) configurable.getConfiguration();
        Xpp3Dom child = configuration.getChild(configIndex);
        InputLocation contentStart = (InputLocation) child.getInputLocation();
        int tagLength = child.getName().length() + 2;
        return new InputLocation(contentStart.getLineNumber(), contentStart.getColumnNumber() - tagLength);
    }

    private static InputLocation getConfigEnd(ConfigurationContainer configurable, int configIndex) {
        InputLocation end;
        Xpp3Dom configuration = (Xpp3Dom) configurable.getConfiguration();
        if (isNotLastItem(configuration, configIndex)) {
            end = getConfigStart(configurable, configIndex + 1);
        } else {
            end = configurable.getLocation("configuration" + END);
        }
        return new InputLocation(end.getLineNumber() - 1, Integer.MAX_VALUE); //unknown line length
    }

    private static boolean isNotLastItem(Xpp3Dom configuration, int index) {
        return index + 1 != configuration.getChildren().length;
    }

    private static boolean shouldDelete(Xpp3Dom child) {
        return REMOVE_CONFIGS.contains(child.getName());
    }

    private static boolean shouldRename(Xpp3Dom child) {
        return RENAME_CONFIGS.containsKey(child.getName());
    }
}