package org.bitbucket.cpointe.habushu;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PyenvCommandHelper;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;

/**
 * Attaches to the {@link LifecyclePhase#VALIDATE} phase to ensure that the all
 * pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine. These include:
 * <ul>
 * <li>pyenv</li>
 * <li>Poetry (installed version must satisfy
 * {@link #POETRY_VERSION_REQUIREMENT})</li>
 * <li>Required Poetry plugins (currently only
 * {@code poetry-lock-groups-plugin})</li>
 * </ul>
 */
@Mojo(name = "validate-pyenv-and-poetry", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidatePyenvAndPoetryMojo extends AbstractHabushuMojo {

    /**
     * Specifies the semver compliant requirement for the version of Poetry that
     * must be installed and available for Habushu to use.
     */
    protected static final String POETRY_VERSION_REQUIREMENT = "~1.2.0";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

	List<String> missingRequiredToolMsgs = new ArrayList<>();

	if (usePyenv) {
	    PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();
	    getLog().info("Checking if pyenv is installed...");
	    if (!pyenvHelper.isPyenvInstalled()) {
		missingRequiredToolMsgs.add(
			"'pyenv' is not currently installed! Please install pyenv and try again. Visit https://github.com/pyenv/pyenv for more information.");
	    }
	}

	getLog().info("Checking if Poetry is installed...");
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
	Pair<Boolean, String> poetryInstallStatusAndVersion = poetryHelper.getIsPoetryInstalledAndVersion();

	if (!poetryInstallStatusAndVersion.getLeft()) {
	    missingRequiredToolMsgs.add(
		    "'poetry' is not currently installed! Execute 'curl -sSL https://install.python-poetry.org | python -' to install or visit https://python-poetry.org/ for more information and installation options");
	}

	Semver poetryVersionSemver = new Semver(poetryInstallStatusAndVersion.getRight(), SemverType.NPM);
	if (!poetryVersionSemver.satisfies(POETRY_VERSION_REQUIREMENT)) {
	    missingRequiredToolMsgs.add(String.format(
		    "Poetry version %s was installed - Habushu requires that installed version of Poetry satisfies %s.  Please update Poetry by executing 'poetry self update' or visit https://python-poetry.org/docs/#installation for more information",
		    poetryInstallStatusAndVersion.getRight(), POETRY_VERSION_REQUIREMENT));
	}

	if (!missingRequiredToolMsgs.isEmpty()) {
	    throw new MojoExecutionException(StringUtils.join(missingRequiredToolMsgs, System.lineSeparator()));

	}

	if (this.useLockWithGroups) {
	    getLog().info("Checking for updates to poetry-lock-groups-plugin...");
	    poetryHelper.installPoetryPlugin("poetry-lock-groups-plugin");
	}
    }

}
