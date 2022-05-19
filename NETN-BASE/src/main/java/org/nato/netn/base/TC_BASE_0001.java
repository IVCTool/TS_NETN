package org.nato.netn.base;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.AbstractTestCase;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_LoggingFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;
import hla.rti1516e.FederateHandle;

public class TC_BASE_0001 extends AbstractTestCase {

    private NetnBaseIvctBaseModel baseModel = null;
    private NetnBaseTcParam tcParam = null;
    private IVCT_LoggingFederateAmbassador ivct_LoggingFederateAmbassador;
    private FederateHandle federateHandle;

    @Override
    protected IVCT_BaseModel getIVCT_BaseModel(String tcParamJson, Logger logger) throws TcInconclusive {
        try {
            tcParam = new NetnBaseTcParam(tcParamJson);
        } catch (ParseException e) {
            throw new TcInconclusive("unable to initialize test case parameter", e);
        }
        baseModel = new NetnBaseIvctBaseModel(logger, tcParam);

        ivct_LoggingFederateAmbassador = new IVCT_LoggingFederateAmbassador(baseModel, logger);

        return baseModel;
    }

    @Override
    protected void logTestPurpose(Logger logger) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        stringBuilder.append("TEST PURPOSE\n");
        stringBuilder.append("This is a test case example for the NETN test suite.\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        final String testPurpose = stringBuilder.toString();

        logger.info(testPurpose);        
    }

    @Override
    protected void preambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_BASE_0001 preamble");
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        // TODO establish precondition for testing
        
        // Initiate rti
        federateHandle = baseModel.initiateRti(this.getSutFederateName(), ivct_LoggingFederateAmbassador);
    }

    @Override
    protected void performTest(Logger logger) throws TcInconclusiveIf, TcFailedIf {
        logger.info("TC_BASE_0001 test case started");
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        // TODO implement the test logic here
        int duration = 8000;
        int interval = 800;
        while (duration > 0) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                return;
            }
            duration -= interval;
            logger.info("testing - remaining time {} ms", duration);
        }
        
        logger.info("TC_BASE_0001 test case passed");
    }

    @Override
    protected void postambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_BASE_0001 postamble");
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        // TODO clean up test artefacts after finished test
        
    }
    
}
