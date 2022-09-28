package org.bitbucket.cpointe.habushu;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PyenvCommandHelper;

/**
 * Attaches to the {@link LifecyclePhase#VALIDATE} phase to ensure that both
 * pyenv and Poetry are installed and available on the developer's machine.
 */
@Mojo(name = "validate-pyenv-and-poetry", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidatePyenvAndPoetryMojo extends AbstractHabushuMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<String> missingRequiredToolMsgs = new ArrayList<>();

		if (usePyenv) {
			PyenvCommandHelper pyenvHelper = createPyenvCommandHelper();

			getLog().info("Checking if pyenv is installed...");
			if(!pyenvHelper.isPyenvInstalled()) {
				missingRequiredToolMsgs.add(
						"'pyenv' is not currently installed! Please install pyenv and try again. Visit https://github.com/pyenv/pyenv for more information.");
			}
		}

		getLog().info("Checking if Poetry is installed...");
		PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
		boolean poetryInstalled = poetryHelper.isPoetryInstalled();

		if (!poetryInstalled) {
			missingRequiredToolMsgs.add(
					"'poetry' is not currently installed! Execute 'curl -sSL https://install.python-poetry.org | python -' to install or visit https://github.com/python-poetry/poetry for more information and installation options");
		}

		if(!missingRequiredToolMsgs.isEmpty()) {
			throw new MojoExecutionException(StringUtils.join(missingRequiredToolMsgs, System.lineSeparator()));
		}
		if(this.useLockWithGroups) {
			getLog().info("Checking for updates to poetry-lock-groups-plugin...");
			poetryHelper.installPoetryPlugin("poetry-lock-groups-plugin");
		}
    }

}
