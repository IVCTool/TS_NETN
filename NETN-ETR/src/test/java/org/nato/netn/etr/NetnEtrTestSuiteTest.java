/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.etr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ServiceLoader;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.IVCT_Verdict;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnEtrTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnEtrTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() throws FileNotFoundException, IOException, ParseException {
        log.trace("ServiceLoader test");
        NetnEtrTestSuite ts = new NetnEtrTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.etr.TC_Etr_0001");
        assertNotNull(tc);
    }

    @Test
    void testServiceLoader() {
        ServiceLoader<TestSuite> loader = ServiceLoader.load(TestSuite.class);
        for (TestSuite factory : loader) {
            String label = factory.getId();
            assertEquals(NetnEtrTestSuite.TEST_SUITE_ID, label);
            log.trace("found {} test suite", label);
        }
    }

    @Test
    void test() throws FileNotFoundException, IOException, ParseException {
        log.trace("Main test");
        NetnEtrTestSuite ts = new NetnEtrTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.etr.TC_Etr_0001");
        assertNotNull(tc);       
        IVCT_Verdict verdict = tc.execute(log);
        log.info("Test Case Verdict: {}", verdict);
        assertTrue(verdict.verdict == IVCT_Verdict.Verdict.PASSED);
    }
}
