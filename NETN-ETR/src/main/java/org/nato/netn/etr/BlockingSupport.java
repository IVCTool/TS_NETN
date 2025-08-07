package org.nato.netn.etr;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

public class BlockingSupport {
    
    interface Callback {
        boolean call();
    }

    private Logger logger;
    private ExecutorService executorService = null;
    private long reschedule = 500;

    public BlockingSupport(ExecutorService es, long rs, Logger l) {
        executorService = es;
        reschedule = rs;
        logger = l;
    }

    public void waitWhile(Callback f, String rs, int timeout) {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting thread waitWhile for " + rs);
                while (f.call()) {Thread.sleep(reschedule);}
            } catch (InterruptedException e) {
                logger.error("waitWhile: "  + e.getMessage());
            }
            return rs;
        }, executorService);
        f1.thenAccept(result -> {
            logger.info(result + " from SuT federate received.");
        })
        .exceptionally(throwable -> {
            logger.error("Error occurred in : waitForSupportedActions.f1 " + throwable.getMessage());
            return null;
        });
        try {
            // block here
            if (timeout > 0) f1.get(timeout, TimeUnit.SECONDS);
            else f1.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("-------> " + e.getMessage());
        } catch (TimeoutException e) {
            logger.info("Timeout reached for " + rs);
            f1.complete(rs);
        } 
    }

}
