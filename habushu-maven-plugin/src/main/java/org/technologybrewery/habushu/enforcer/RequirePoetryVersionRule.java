package org.technologybrewery.habushu.enforcer;

import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import javax.inject.Named;
import java.io.File;

/**
 * A Maven Enforcer Rule to ensure environment expectations around the version of Poetry installed are met.
 */
@Named("requirePoetryVersion")
public class RequirePoetryVersionRule extends AbstractEnforcerRule {

    /**
     * The version or version range of Poetry that must be met.  Passed to the `requirePoetryVersion` rule as `version`.
     */
    private String version;

    private String poetryVersion;

    /**
     * Executes the check to determine if a valid Poetry version is installed.
     *
     * @throws EnforcerRuleException on rule enforcement error
     */
    @Override
    public void execute() throws EnforcerRuleException {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(new File("./"));
        Pair<Boolean, String> poetryResults = poetryHelper.getIsPoetryInstalledAndVersion();
        if (Boolean.FALSE.equals(poetryResults.getLeft())) {
            throw new EnforcerRuleException("'poetry' is not currently installed! Execute 'pipx install poetry' to "
                    + "install or visit https://python-poetry.org/ for more information and installation options");

        } else {
            String foundVersion = poetryResults.getRight();
            Semver poetryVersionSemver = new Semver(foundVersion, Semver.SemverType.NPM);
            if (!poetryVersionSemver.satisfies(version)) {
                String errorMessage = String.format("Poetry version %s was installed - the project requires Poetry %s. "
                        + "Please update Poetry by executing 'poetry self update' or "
                        + "visit https://python-poetry.org/docs/#installation for more information", foundVersion,
                        version);
                throw new EnforcerRuleException(errorMessage);

            } else {
                poetryVersion = foundVersion;

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheId() {
        return "poetry-version-" + poetryVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
