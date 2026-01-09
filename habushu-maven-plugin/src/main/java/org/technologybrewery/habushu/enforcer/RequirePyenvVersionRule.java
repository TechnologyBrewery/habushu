package org.technologybrewery.habushu.enforcer;

import org.apache.commons.lang3.tuple.Pair;
import org.technologybrewery.habushu.exec.PyenvCommandHelper;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

/**
 * A Maven Enforcer Rule to ensure environment expectations around the version of pyenv installed are met.
 */
@Named("requirePyenvVersion")
public class RequirePyenvVersionRule extends AbstractRequireToolVersionRule {

    @Inject
    public RequirePyenvVersionRule() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<Boolean, String> getInstalledAndVersionPair() {
        PyenvCommandHelper pyenvHelper = new PyenvCommandHelper(new File("./"));
        return pyenvHelper.getIsPyenvInstalledAndVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInstallationErrorMessage() {
        return "'pyenv' is not currently installed! Please install pyenv and try again. "
                + "Visit https://github.com/pyenv/pyenv for more information.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getVersionErrorMessage(String foundVersion) {
        return String.format("pyenv version %s was installed - the project requires pyenv %s. Please update pyenv by " 
                + "following the instructions at https://github.com/pyenv/pyenv.", foundVersion, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCacheDescriptor() {
        return "pyenv-version";
    }
}