package org.nato.netn.base;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class NetnRefFed {
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnRefFed.class);
    
    private boolean running = false;
    private int duration = 2000;
    private int interval = 1000;
    
    @Test
    public void run() throws InterruptedException {
        log.info("starting reference federate");
        running = true;
        while (duration > 0 && running) {
            Thread.sleep(interval);
            duration -= interval;
            log.info("running - remaining time {} ms", duration);
        }
        log.info("reference federated stopped");
    }

    public void stop() {
        running = false;
    }
}
