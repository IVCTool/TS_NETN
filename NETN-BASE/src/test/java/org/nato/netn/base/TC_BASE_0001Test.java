package org.nato.netn.base;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;

@Execution(ExecutionMode.CONCURRENT)
public class TC_BASE_0001Test {
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(TC_BASE_0001Test.class);

    @Test
    void testGetIVCT_BaseModel() {

    }

    @Test
    public void execute() throws InterruptedException {
        TC_BASE_0001 tc = new TC_BASE_0001();
        tc.setFederationName("federationName");
        tc.setSettingsDesignator("settingsDesignator");
        tc.setSutFederateName("sutFederateName");
        tc.setSutName("sutName");
        tc.setTcParam("param");
        tc.setTsName("testSuiteId");
        
        log.info("test case started");
        // tc.execute(log);
        int duration = 8000;
        int interval = 800;
        while (duration > 0) {
            Thread.sleep(interval);
            duration -= interval;
            log.info("testing - remaining time {} ms", duration);
        }
        log.info("test case finished");

    }

    @Test
    void testLogTestPurpose() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        tc.logTestPurpose(log);
    }

    @Test
    void testPerformTest() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        try {
            tc.performTest(log);
        } catch (TcInconclusiveIf e) {
            fail("test case shall not be inconclusive: {}", e);
        } catch (TcFailedIf e) {
            fail("test case shall not be inconclusive: {}", e);
        }
    }

    @Test
    void testPostambleAction() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        try {
            tc.postambleAction(log);
        } catch (TcInconclusiveIf e) {
            fail("test case shall not be inconclusive: {}", e);
        }
    }

    @Test
    void testPreambleAction() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        try {
            tc.preambleAction(log);
        } catch (TcInconclusiveIf e) {
            fail("test case shall not be inconclusive: {}", e);
        }
    }
}
