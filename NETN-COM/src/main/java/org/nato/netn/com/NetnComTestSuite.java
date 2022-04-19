
package org.nato.netn.com;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib_if.TestSuite;

import org.slf4j.LoggerFactory;

public class NetnComTestSuite implements TestSuite {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnComTestSuite.class);

    public NetnComTestSuite() {
        super();
        log.trace("Test Suite {} loading", NetnComTestSuite.class);
    }

    @Override
    public AbstractTestCaseIf getTestCase(String testCaseId) {
        log.trace("got test case id {} to find", testCaseId);
        if (testCaseId.equals(TC_COM_0001.class.getName())) {
            log.trace("found it");
            return new TC_COM_0001();
        }
        log.trace("nothing found");
        return null;
    }
}
