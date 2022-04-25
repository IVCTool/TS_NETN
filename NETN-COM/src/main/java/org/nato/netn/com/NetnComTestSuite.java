
package org.nato.netn.com;

import de.fraunhofer.iosb.tc_lib_if.AbstractTestCaseIf;
import de.fraunhofer.iosb.tc_lib.HlaTestSuite;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

public class NetnComTestSuite extends HlaTestSuite {

    public static final String TEST_SUITE_ID = "NETN-COM-4_0";

    static final org.slf4j.Logger log = LoggerFactory.getLogger(NetnComTestSuite.class);

    public NetnComTestSuite() throws FileNotFoundException, IOException, ParseException {
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
