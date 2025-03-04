package org.technologybrewery.habushu;

import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvCommandHelper;

public class BehaveBddTestUv extends AbstractBehaveBddTest {

    public BehaveBddTestUv(Log log, BehaveBddTestMojo behaveBddTestMojo, UvCommandHelper uvCommandHelper) {
        super(log, behaveBddTestMojo, uvCommandHelper);
    }
}
