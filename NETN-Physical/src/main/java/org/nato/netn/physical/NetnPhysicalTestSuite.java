
package org.nato.netn.physical;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib.HlaTestSuite;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

public class NetnPhysicalTestSuite extends HlaTestSuite {

    public static final String TEST_SUITE_ID = "NETN-PHYSICAL-4_0";

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnPhysicalTestSuite.class);

    public NetnPhysicalTestSuite() throws FileNotFoundException, IOException, ParseException {
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
