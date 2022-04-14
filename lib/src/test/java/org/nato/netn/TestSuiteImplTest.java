/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import org.slf4j.LoggerFactory;

class NetnTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() {
        log.trace("ServiceLoader test");
        NetnTestSuite ts = new NetnTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.ais.TC_AIS_0001");
        assertNotNull(tc);
    }
}
