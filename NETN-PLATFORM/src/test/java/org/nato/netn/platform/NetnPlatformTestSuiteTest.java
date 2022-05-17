/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.platform;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class NetnPlatformTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnPlatformTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() throws FileNotFoundException, IOException, ParseException {
        log.trace("ServiceLoader test");
        NetnPlatformTestSuite ts = new NetnPlatformTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.platform.TC_Platform_0001");
        assertNotNull(tc, "defined test case must be found");
    }

    @Test
    void testGetTestSuiteId () throws FileNotFoundException, IOException, ParseException {
        NetnPlatformTestSuite ts = new NetnPlatformTestSuite();
        assertEquals(NetnPlatformTestSuite.TEST_SUITE_ID, ts.getId());
        assertTrue(ts.getId().startsWith("NETN"));
    }

    @Test
    void testServiceLoader() {
        ServiceLoader<TestSuite> loader = ServiceLoader.load(TestSuite.class);
        for (TestSuite factory : loader) {
            String label = factory.getId();
            assertEquals(NetnPlatformTestSuite.TEST_SUITE_ID, label);
            log.trace("found {} test suite", label);
        }
    }
}
