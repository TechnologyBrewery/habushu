package org.technologybrewery.habushu.util;

/**
 * Common utility methods for pyenv.
 */
public class PyenvUtil {

    protected PyenvUtil() {
        // prevent instantiation of all static class
    }
    
    /**
     * Specifies the semver compliant requirement for the version of pyenv that
     * must be installed and available for Habushu to use.
     */
    public static final String PYENV_VERSION_REQUIREMENT = ">=1.2.21";
    
}
