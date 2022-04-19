
package org.nato.netn.base;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnBaseTestSuite implements TestSuite {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnBaseTestSuite.class);

    public NetnBaseTestSuite() {
        super();
        log.trace("Test Suite {} loading", NetnBaseTestSuite.class);
    }

    @Override
    public AbstractTestCaseIf getTestCase(String testCaseId) {
        log.trace("got test case id {} to find", testCaseId);
        if (testCaseId.equals(TC_BASE_0001.class.getName())) {
            log.trace("found it");
            return new TC_BASE_0001();
        }
        log.trace("nothing found");
        return null;
    }
}
