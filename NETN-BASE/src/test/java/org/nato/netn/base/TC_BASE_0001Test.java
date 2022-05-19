package org.nato.netn.base;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;

@Execution(ExecutionMode.CONCURRENT)
public class TC_BASE_0001Test {
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(TC_BASE_0001Test.class);

    protected static String tcParamJson = "{ " +
        "    \"p1\" : \"1.0003\", " +
        "    \"fomFiles\": [ " +
                "\"../foms/TS-NETN-v4.0.xml\", " +
                "\"../foms/RPR-FOM-v2.0/RPR-Base_v2.0.xml\", " +
                "\"../foms/NETN-FOM-v4.0/NETN-BASE.xml\"] " +
        "}";


    @Test
    void testGetIVCT_BaseModel() {

    }

    @Test
    public void execute() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        tc.setFederationName("NETN-Test-Federation");
        tc.setSettingsDesignator("settingsDesignator");
        tc.setSutFederateName("TC_BASE_0001");
        tc.setSutName("sutName");
        tc.setTcParam(tcParamJson);
        tc.setTsName("testSuiteId");

        tc.setSkipOperatorMsg(true);

        log.info("test case started");
        tc.execute(log);
        log.info("test case finished");

    }

    @Test
    void testLogTestPurpose() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        tc.logTestPurpose(log);
    }

    @Test
    void testPerformTest() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        try {
            tc.performTest(log);
        } catch (TcInconclusiveIf e) {
            fail("test case shall not be inconclusive: {}", e);
        } catch (TcFailedIf e) {
            fail("test case shall not be inconclusive: {}", e);
        }
    }
    
    @Test
    void testPreambleAction() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        tc.setFederationName("NETN-Test-Federation");
        tc.setSettingsDesignator("settingsDesignator");
        tc.setSutFederateName("TC_BASE_0001");
        tc.setSutName("sutName");
        tc.setTcParam(tcParamJson);
        tc.setTsName("testSuiteId");

        tc.setSkipOperatorMsg(true);
        try {
            tc.preambleAction(log);
        } catch (TcInconclusiveIf e) {
            fail("test case shall not be inconclusive: {}", e);
        }
    }
    
    @Test
    void testPostambleAction() {
        TC_BASE_0001 tc = new TC_BASE_0001();
        try {
            tc.postambleAction(log);
        } catch (TcInconclusiveIf e) {
            fail("test case shall not be inconclusive: {}", e);
        }
    }
}
