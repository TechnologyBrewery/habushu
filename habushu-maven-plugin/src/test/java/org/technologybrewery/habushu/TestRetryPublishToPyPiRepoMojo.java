package org.technologybrewery.habushu;

import io.github.itning.retry.RetryException;
import io.github.itning.retry.Retryer;
import org.apache.commons.lang3.tuple.Pair;
import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class TestRetryPublishToPyPiRepoMojo extends PublishToPyPiRepoMojo {

    protected int finalRetryNumber;

    public TestRetryPublishToPyPiRepoMojo(int numberOfRetries, int finalRetryNumber) {
        super();

        this.packaging = "habushu";
        this.useDevRepository = false;
        this.pypiPushRetryMultiplier = 50;
        this.pypiPushRetryMaxTimeout = 1;

        if (numberOfRetries >= 0) {
            this.pypiPushRetries = numberOfRetries;
        }

        this.finalRetryNumber = finalRetryNumber;

    }


    protected Callable<Boolean> getPyPiPushCallable(CommandHelper commandHelper, List<Pair<String, Boolean>> publishToOfficialPypiRepoArgs) {
        return new Callable<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() throws IOException {
                if (counter < finalRetryNumber) {
                    counter++;
                    getLog().warn("Faux publish to PyPI failed (attempt " + counter + ")");

                    // simulate both returning false (non-0 return code) and a RuntimeException:
                    if (counter % 2 == 0) {
                        throw new HabushuException();
                    } else {
                        return false;
                    }
                }

                return true;
            }
        };
    }

    public void invokePublishUsingPoetry(PoetryCommandHelper poetryHelper, List<Pair<String, Boolean>> publishToRepoWithCredsArgs){
        PublishToPyPiRepoPoetry publishToPyPiRepoPoetry = new PublishToPyPiRepoPoetry(new File("target/"), getLog(), this);
        Callable<Boolean> callable = getPyPiPushCallable(poetryHelper, publishToRepoWithCredsArgs);
        Retryer<Boolean> retryer = publishToPyPiRepoPoetry.getRetryer();
        callRetryer(retryer, callable);
    }

    public void invokePublishUsingUv(UvCommandHelper uvCommandHelper, List<Pair<String, Boolean>> publishToRepoWithCredsArgs){
        PublishToPyPiRepoUv publishToPyPiRepoUv = new PublishToPyPiRepoUv(new File("target/"), getLog(), this);
        Callable<Boolean> callable = getPyPiPushCallable(uvCommandHelper, publishToRepoWithCredsArgs);
        Retryer<Boolean> retryer = publishToPyPiRepoUv.getRetryer();
        callRetryer(retryer, callable);
    }

    public void callRetryer(Retryer<Boolean> retryer, Callable<Boolean> callable){
        try {
            Boolean result = retryer.call(callable);

            if (Boolean.FALSE.equals(result)) {
                throw new HabushuException("Push to PyPI repository failed!");
            }

        } catch (RetryException e) {
            throw new HabushuException("Exceeded retry setting of: " + getPypiPushRetries(), e);
        } catch (ExecutionException e) {
            throw new HabushuException("Could not execute PyPI push!", e);
        }
    }
}
