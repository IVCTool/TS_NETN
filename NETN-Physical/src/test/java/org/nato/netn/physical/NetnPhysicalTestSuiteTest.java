/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn.physical;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class NetnPhysicalTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnPhysicalTestSuiteTest.class);
    
    @Test 
    void testTestCaseLoader() {
        log.trace("ServiceLoader test");
        NetnPhysicalTestSuite ts = new NetnPhysicalTestSuite();
        AbstractTestCaseIf tc = ts.getTestCase("org.nato.netn.physical.TC_Physical_0001");
        assertNotNull(tc);
    }
}
