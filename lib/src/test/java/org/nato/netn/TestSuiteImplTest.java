/*
 * Testing the ServiceLoader for test suites
 */
package org.nato.netn;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import org.nato.ivct.tc_lib_if.TestSuite;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

class NetnTestSuiteTest {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnTestSuiteTest.class);
    
    @Test 
    void testServiceLoader() {
        log.trace("ServiceLoader test");
         AbstractTestCaseIf tc = null;
         ServiceLoader<TestSuite> loader = ServiceLoader.load(org.nato.ivct.tc_lib_if.TestSuite.class);
         for (TestSuite factory : loader) {
             tc = factory.getTestCase("org.nato.netn.ais.TC_AIS_0001");
             if (tc != null) break;
            }
        assertNotNull(tc);
    }
}
