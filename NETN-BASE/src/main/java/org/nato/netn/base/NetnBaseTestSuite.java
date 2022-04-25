
package org.nato.netn.base;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib.HlaTestSuite;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

public class NetnBaseTestSuite extends HlaTestSuite {

    public NetnBaseTestSuite() throws FileNotFoundException, IOException, ParseException {
        super();
    }

    public static final String TEST_SUITE_ID = "NETN-BASE-4_0";
    JSONObject description;

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnBaseTestSuite.class);

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
