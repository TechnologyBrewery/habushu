package org.technologybrewery.habushu;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.baton.BatonMojo;

/**
 * Overriding the baton-maven-plugin so we can get access to Habushu's classpath for migration configuration.
 */
@Mojo(name = "baton-migrate", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class MigrateMojo extends BatonMojo {
}
