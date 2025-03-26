package org.technologybrewery.habushu.enforcer;

import org.apache.commons.lang3.tuple.Pair;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import javax.inject.Named;
import java.io.File;

/**
 * A Maven Enforcer Rule to ensure environment expectations around the version of uv installed are met.
 */
@Named("requireUvVersion")
public class RequireUvVersionRule extends AbstractRequireToolVersionRule {

    /**
     * {@inheritDoc}
     */
    protected Pair<Boolean, String> getInstalledAndVersionPair() {
        UvCommandHelper uvHelper = new UvCommandHelper(new File("./"));
        return uvHelper.getIsUvInstalledAndVersion();
    }

    /**
     * {@inheritDoc}
     */
    protected String getInstallationErrorMessage() {
        return "'uv' is not currently installed! Execute 'pipx install uv' to "
                + "install or visit https://docs.astral.sh/uv/getting-started/installation/ for more information "
                + "and installation options";
    }

    /**
     * {@inheritDoc}
     */
    protected String getVersionErrorMessage(String foundVersion) {
        return String.format("uv version %s was installed - the project requires uv %s. Please update uv by executing "
                + "'uv self update' or visit https://docs.astral.sh/uv/getting-started/installation/ for more "
                + "information", foundVersion, version);
    }

    /**
     * {@inheritDoc}
     */
    protected String getCacheDescriptor() {
        return "uv-version";
    }

}
