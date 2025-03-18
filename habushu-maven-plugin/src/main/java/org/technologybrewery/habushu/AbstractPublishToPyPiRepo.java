package org.technologybrewery.habushu;

import com.google.common.base.Predicates;
import io.github.itning.retry.RetryException;
import io.github.itning.retry.Retryer;
import io.github.itning.retry.RetryerBuilder;
import io.github.itning.retry.strategy.stop.StopStrategies;
import io.github.itning.retry.strategy.stop.StopStrategy;
import io.github.itning.retry.strategy.wait.WaitStrategies;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPublishToPyPiRepo {

    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * Instance of PublishToPyPiRepoMojo
     */
    protected PublishToPyPiRepoMojo publishToPyPiRepoMojo;


    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir    base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param mojo Configurations for installing Dependencies
     */
    protected AbstractPublishToPyPiRepo(File baseDir, Log log, PublishToPyPiRepoMojo mojo) {
        this.baseDir = baseDir;
        this.log = log;
        this.publishToPyPiRepoMojo = mojo;
    }


    public abstract void doExecute() throws MojoExecutionException, MojoFailureException;


    protected Retryer<Boolean> getRetryer() {
        RetryerBuilder<Boolean> retryBuilder = RetryerBuilder.<Boolean>newBuilder();

        if (publishToPyPiRepoMojo.getPypiPushRetries() != 0) {
            StopStrategy stopStrategy = (publishToPyPiRepoMojo.getPypiPushRetries() < 0) ? StopStrategies.neverStop()
                    : StopStrategies.stopAfterAttempt(publishToPyPiRepoMojo.getPypiPushRetries());

            retryBuilder.retryIfResult(Predicates.<Boolean>equalTo(Boolean.FALSE))
                    .retryIfRuntimeException()
                    .withStopStrategy(stopStrategy)
                    .withWaitStrategy(WaitStrategies.fibonacciWait(publishToPyPiRepoMojo.getPypiPushRetryMultiplier(), publishToPyPiRepoMojo.getPypiPushRetryMaxTimeout(), TimeUnit.MINUTES));
        }

        return retryBuilder.build();
    }

    protected String getRepositoryUrl(boolean publishToDev) {
        String repoUrl = addTrailingSlash(publishToPyPiRepoMojo.getPypiRepoUrl());
        if (publishToDev) {
            repoUrl = addTrailingSlash(publishToPyPiRepoMojo.getDevRepositoryUrl()) + addTrailingSlash(publishToPyPiRepoMojo.getDevRepositoryUrlUploadSuffix());
        } else if (!StringUtils.isEmpty(publishToPyPiRepoMojo.getPypiUploadSuffix())) {
            repoUrl += addTrailingSlash(publishToPyPiRepoMojo.getPypiUploadSuffix());
        }
        return repoUrl;
    }

    protected static String addTrailingSlash(String inputUrl) {
        if (StringUtils.isNotBlank(inputUrl) && !StringUtils.endsWith(inputUrl, "/")) {
            // PEP-0694 likes a trailing slash:
            inputUrl += "/";
        }

        return inputUrl;
    }

    protected void invokePublish(CommandHelper commandHelper, List<Pair<String, Boolean>> publishToRepoWithCredsArgs) {
        Callable<Boolean> callable = getPyPiPushCallable(commandHelper, publishToRepoWithCredsArgs);
        Retryer<Boolean> retryer = getRetryer();

        try {
            Boolean result = retryer.call(callable);

            if (Boolean.FALSE.equals(result)) {
                throw new HabushuException("Push to PyPI repository failed!");
            }

        } catch (RetryException e) {
            throw new HabushuException("Exceeded retry setting of: " + publishToPyPiRepoMojo.getPypiPushRetries(), e);
        } catch (ExecutionException e) {
            throw new HabushuException("Could not execute PyPI push!", e);
        }

    }

    protected Callable<Boolean> getPyPiPushCallable(CommandHelper commandHelper, List<Pair<String, Boolean>> publishToRepoWithCredsArgs) {
        Callable<Boolean> callable = new Callable<>() {
            private boolean firstAttempt = true;

            public Boolean call() throws Exception {
                if (!HabushuUtil.isCurrentPackageManagerUv(publishToPyPiRepoMojo.getPyProjectTomlFile())) {
                    if (!firstAttempt) {
                        publishToRepoWithCredsArgs.removeIf(arg -> "--build".equals(arg.getLeft()));
                        log.debug("Removing build command from retry due to the general issue error described in https://github.com/python-poetry/cleo/issues/351");
                    } else {
                        firstAttempt = false;
                    }
                }

                int result = commandHelper.executeWithSensitiveArgsAndLogOutput(publishToRepoWithCredsArgs);
                if (result != 0) {
                    log.warn("PyPI Publish process result code: " + result);
                }
                return result == 0;
            }
        };

        return callable;
    }

    protected boolean checkPublishToDev(boolean rebuildPackage){
        boolean publishToDev = rebuildPackage && publishToPyPiRepoMojo.useDevRepository();
        if (publishToDev) {
            log.info("Publishing to dev repository (useDevRepository=true, dev version being published)");
        }
        return publishToDev;
    }


}
