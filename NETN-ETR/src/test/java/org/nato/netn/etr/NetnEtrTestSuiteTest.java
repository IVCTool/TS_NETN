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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.IVCT_Verdict;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnEtrTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnEtrTestSuiteTest.class);
    static final String local_designator = "crcAddress=localhost:8989";
    static final String pitis_designator = "crcAddress=crc.PITIS-223@localhost:8688";
    
    private void ETR_test(String designator, String tc_name) throws FileNotFoundException, IOException, ParseException {
        log.trace("Test: " + tc_name);
        NetnEtrTestSuite ts = new NetnEtrTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase(tc_name);
        assertNotNull(tc);       
        tc.setSettingsDesignator(designator);
        IVCT_Verdict verdict = tc.execute(log);
        log.info("Test Case Verdict: {}", verdict);
        assertTrue(verdict.verdict == IVCT_Verdict.Verdict.PASSED);
    }
    
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
    @Tag("local")
    void testETR_0001atLocalhost() throws FileNotFoundException, IOException, ParseException {
        ETR_test(local_designator, "org.nato.netn.etr.TC_Etr_0001");
    }

    @Test
    @Tag("Pitis")
    void testETR_0001atPITIS223() throws FileNotFoundException, IOException, ParseException {
        ETR_test(pitis_designator, "org.nato.netn.etr.TC_Etr_0001");
    }

    @Test
    @Tag("local")
    void testETR_0002atLocalhost() throws FileNotFoundException, IOException, ParseException {
        ETR_test(local_designator, "org.nato.netn.etr.TC_Etr_0002");
    }

    @Test
    @Tag("Pitis")
    void testETR_0002atPITIS223() throws FileNotFoundException, IOException, ParseException {
        ETR_test(pitis_designator, "org.nato.netn.etr.TC_Etr_0002");
    }    
}
