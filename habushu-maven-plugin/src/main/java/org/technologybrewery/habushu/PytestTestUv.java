package org.technologybrewery.habushu;

import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.UvCommandHelper;

public class PytestTestUv extends AbstractPytestTest {

    public PytestTestUv(Log log, PytestTestMojo pytestTestMojo, UvCommandHelper uvCommandHelper) {
        super(log, pytestTestMojo, uvCommandHelper);
    }
}
