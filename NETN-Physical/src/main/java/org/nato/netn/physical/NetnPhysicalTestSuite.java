
package org.nato.netn.physical;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnPhysicalTestSuite implements TestSuite {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnPhysicalTestSuite.class);

    public NetnPhysicalTestSuite() {
        super();
        log.trace("Test Suite {} loading", NetnPhysicalTestSuite.class);
    }

    @Override
    public AbstractTestCaseIf getTestCase(String testCaseId) {
        log.trace("got test case id {} to find", testCaseId);
        if (testCaseId.equals(TC_Physical_0001.class.getName())) {
            log.trace("found it");
            return new TC_Physical_0001();
        }
        log.trace("nothing found");
        return null;
    }
}
