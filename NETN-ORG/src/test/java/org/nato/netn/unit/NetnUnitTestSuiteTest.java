/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ServiceLoader;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnUnitTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnUnitTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() throws FileNotFoundException, IOException, ParseException {
        log.trace("ServiceLoader test");
        NetnUnitTestSuite ts = new NetnUnitTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.unit.TC_UNIT_0001");
        assertNotNull(tc);
    }

    @Test
    void testServiceLoader() {
        ServiceLoader<TestSuite> loader = ServiceLoader.load(TestSuite.class);
        for (TestSuite factory : loader) {
            String label = factory.getId();
            assertEquals(NetnUnitTestSuite.TEST_SUITE_ID, label);
            log.trace("found {} test suite", label);
        }
    }
}
