
package org.nato.netn.mrn;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnMrnTestSuite implements TestSuite {

    public static final String TEST_SUITE_ID = "NETN-MRN-4_0";

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnMrnTestSuite.class);

    public NetnMrnTestSuite() {
        super();
        log.trace("Test Suite {} loading", NetnMrnTestSuite.class);
    }

    @Override
    public AbstractTestCaseIf getTestCase(String testCaseId) {
        log.trace("got test case id {} to find", testCaseId);
        if (testCaseId.equals(TC_MRN_0001.class.getName())) {
            log.trace("found it");
            return new TC_MRN_0001();
        }
        log.trace("nothing found");
        return null;
    }
    
    @Override
    public String getTestSuiteId() {
        return TEST_SUITE_ID;
    }
}
