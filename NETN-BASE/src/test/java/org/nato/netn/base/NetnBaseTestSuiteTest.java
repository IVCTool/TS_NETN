/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.base;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import org.slf4j.LoggerFactory;

public class NetnBaseTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnBaseTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() {
        log.trace("ServiceLoader test");
        NetnBaseTestSuite ts = new NetnBaseTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.base.TC_BASE_0001");
        assertNotNull(tc);
    }
}
