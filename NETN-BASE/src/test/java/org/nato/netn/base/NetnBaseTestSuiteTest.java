/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ServiceLoader;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnBaseTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnBaseTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() throws FileNotFoundException, IOException, ParseException {
        log.trace("ServiceLoader test");
        NetnBaseTestSuite ts = new NetnBaseTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.base.TC_BASE_0001");
        assertNotNull(tc);
    }

    @Test
    void testServiceLoader() {
        ServiceLoader<TestSuite> loader = ServiceLoader.load(TestSuite.class);
        for (TestSuite factory : loader) {
            String label = factory.getId();
            assertEquals(NetnBaseTestSuite.TEST_SUITE_ID, label);
            log.trace("found {} test suite", label);
        }
    }

    @Test
    void testParam () throws Exception {
        ServiceLoader<TestSuite> loader = ServiceLoader.load(TestSuite.class);
        TestSuite netnTestSuite = null;
        for (TestSuite factory : loader) {
            if (NetnBaseTestSuite.TEST_SUITE_ID.equalsIgnoreCase(factory.getId())) {
                netnTestSuite = factory;
                break;
            }
        }
        assertNotNull(netnTestSuite);
        JSONObject params = netnTestSuite.getParameterTemplate();
        String p1 = (String) params.get("p1");
        assertEquals("valueA", p1);
        String p2 = (String) params.get("p2");
        assertEquals("valueB", p2);
    }
}
