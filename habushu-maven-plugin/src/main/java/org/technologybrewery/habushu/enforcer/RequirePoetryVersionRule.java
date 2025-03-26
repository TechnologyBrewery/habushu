package org.technologybrewery.habushu.enforcer;

import org.apache.commons.lang3.tuple.Pair;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import javax.inject.Named;
import java.io.File;

/**
 * A Maven Enforcer Rule to ensure environment expectations around the version of Poetry installed are met.
 */
@Named("requirePoetryVersion")
public class RequirePoetryVersionRule extends AbstractRequireToolVersionRule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<Boolean, String> getInstalledAndVersionPair() {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(new File("./"));
        return poetryHelper.getIsPoetryInstalledAndVersion();
    }

    /**
     * {@inheritDoc}
     */
    protected String getInstallationErrorMessage() {
        return "'poetry' is not currently installed! Execute 'pipx install poetry' to install or visit "
                + "https://python-poetry.org/ for more information and installation options";
    }

    /**
     * {@inheritDoc}
     */
    protected String getVersionErrorMessage(String foundVersion) {
        return String.format("Poetry version %s was installed - the project requires Poetry %s.Please update Poetry by "
                + " executing 'poetry self update' or visit https://python-poetry.org/docs/#installation for more "
                + "information", foundVersion, version);
    }

    /**
     * {@inheritDoc}
     */
    protected String getCacheDescriptor() {
        return "poetry-version";
    }

}
