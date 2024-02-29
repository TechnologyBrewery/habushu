package org.technologybrewery.habushu;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Work around. Surefire does not use JUnits Test Engine discovery
 * functionality. Alternatively execute the
 * org.junit.platform.console.ConsoleLauncher with the maven-antrun-plugin.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("specifications")
@ExcludeTags("manual")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.technologybrewery.habushu")
public class TestSpecifications {

}

