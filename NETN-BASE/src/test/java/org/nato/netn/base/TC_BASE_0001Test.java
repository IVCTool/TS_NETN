package org.nato.netn.base;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;

public class TC_BASE_0001Test {
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(TC_BASE_0001Test.class);

    @Test
    void testGetIVCT_BaseModel() {

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
