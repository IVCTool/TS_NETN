/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.com;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import org.slf4j.LoggerFactory;

public class NetnComTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnComTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() {
        log.trace("ServiceLoader test");
        NetnComTestSuite ts = new NetnComTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.com.TC_COM_0001");
        assertNotNull(tc);
    }
}
