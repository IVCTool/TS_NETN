/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.physical;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class NetnPhysicalTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnPhysicalTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() {
        log.trace("ServiceLoader test");
        NetnPhysicalTestSuite ts = new NetnPhysicalTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.physical.TC_Physical_0001");
        assertNotNull(tc, "defined test case must be found");
    }

    @Test
    void testGetTestSuiteId () {
        NetnPhysicalTestSuite ts = new NetnPhysicalTestSuite();
        assertEquals(NetnPhysicalTestSuite.TEST_SUITE_ID, ts.getTestSuiteId());
        assertTrue(ts.getTestSuiteId().startsWith("NETN"));
    }

    @Test
    void testServiceLoader() {
        ServiceLoader<TestSuite> loader = ServiceLoader.load(TestSuite.class);
        for (TestSuite factory : loader) {
            String label = factory.getTestSuiteId();
            assertEquals(NetnPhysicalTestSuite.TEST_SUITE_ID, label);
            log.trace("found {} test suite", label);
        }
    }
}
