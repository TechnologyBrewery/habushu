package org.technologybrewery.habushu.enforcer;

import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

/**
 * Provides common functionality to define an Enforcer rule that verifies installation status and version of a tool.
 */
public abstract class AbstractRequireToolVersionRule extends AbstractEnforcerRule {

    /**
     * The version or version range of tool that must be met.  Passed to the `require<tool>Version` rule as `version`.
     */
    protected String version;

    /**
     * Version of the tool found.
     */
    protected String toolVersion;

    /**
     * Returns a {@Pair} where the left side is the installation status of the tool and the right side is the version
     * of the tool, if installed.
     *
     * @return installation/version pair for the tool
     */
    protected abstract Pair<Boolean, String> getInstalledAndVersionPair();

    /**
     * Returns the error message to display if the tool is not installed.
     *
     * @return tool installation message
     */
    protected abstract String getInstallationErrorMessage();

    /**
     * Returns the error message to display if the tool's version does not match the expected version.
     *
     * @param foundVersion the version of the tool that is installed
     * @return tool version mismatch message
     */
    protected abstract String getVersionErrorMessage(String foundVersion);

    /**
     * Executes the check to determine if a valid tool version is installed.
     *
     * @throws EnforcerRuleException on rule enforcement error
     */
    @Override
    public void execute() throws EnforcerRuleException {
        Pair<Boolean, String> results = getInstalledAndVersionPair();
        if (Boolean.FALSE.equals(results.getLeft())) {
            throw new EnforcerRuleException(getInstallationErrorMessage());

        } else {
            String foundVersion = results.getRight();
            Semver toolVersionSemver = new Semver(foundVersion, Semver.SemverType.NPM);
            if (!toolVersionSemver.satisfies(version)) {
                throw new EnforcerRuleException(getVersionErrorMessage(foundVersion));

            } else {
                toolVersion = foundVersion;

            }
        }
    }

    /**
     * Returns the unique descriptor for the tool type to allow the Enforcer Plugin to cache results.  For instance,
     * Maven might use "maven-version" or Helm might use "helm-version".
     *
     * @return unique cache descriptor
     */
    protected abstract String getCacheDescriptor();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheId() {
        return getCacheDescriptor() + "-" + toolVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
