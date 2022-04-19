/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.mrn;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import org.slf4j.LoggerFactory;

public class NetnMrnTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnMrnTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() {
        log.trace("ServiceLoader test");
        NetnMrnTestSuite ts = new NetnMrnTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.mrn.TC_MRN_0001");
        assertNotNull(tc);
    }
}
