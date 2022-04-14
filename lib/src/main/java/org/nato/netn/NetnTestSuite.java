
package org.nato.netn;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import org.nato.ivct.tc_lib_if.TestSuite;
import org.nato.netn.ais.*;
import org.slf4j.LoggerFactory;

public class NetnTestSuite implements TestSuite {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnTestSuite.class);

    public NetnTestSuite() {
        super();
        log.trace("Test Suite {} loading", NetnTestSuite.class);
    }

    @Override
    public AbstractTestCaseIf getTestCase(String testCaseId) {
        log.trace("got test case id {} to find", testCaseId);
        if (testCaseId.equals(TC_AIS_0001.class.getName())) {
            log.trace("found it");
            return new TC_AIS_0001();
        }
        log.trace("nothing found");
        return null;
    }
}
