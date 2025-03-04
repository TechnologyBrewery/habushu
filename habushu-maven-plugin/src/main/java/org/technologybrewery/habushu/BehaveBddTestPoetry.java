package org.technologybrewery.habushu;

import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

public class BehaveBddTestPoetry extends AbstractBehaveBddTest {

    public BehaveBddTestPoetry(Log log, BehaveBddTestMojo behaveBddTestMojo, PoetryCommandHelper poetryCommandHelper) {
        super(log, behaveBddTestMojo, poetryCommandHelper);
    }
}
