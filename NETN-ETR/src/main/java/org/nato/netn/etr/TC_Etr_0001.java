package org.nato.netn.etr;

import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.AbstractTestCase;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_LoggingFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import hla.rti1516e.FederateHandle;

public class TC_Etr_0001 extends AbstractTestCase {

    private NetnEtrIvctBaseModel baseModel = null;
    private NetnEtrTcParam netnTcParam = null;
    private IVCT_LoggingFederateAmbassador ivct_LoggingFederateAmbassador;
    private FederateHandle federateHandle;
    private JSONObject root = null;

    public void setTcParamFromFile(String fileName) {
        if (tcParam == null && root == null) {
            JSONParser parser = new JSONParser();
            URL url = this.getClass().getResource(fileName);
            try {
                Reader reader = new FileReader(url.getPath());
                root = (JSONObject) parser.parse(reader);   
            } catch (IOException | ParseException e) {
                throw new RuntimeException("Could not parse TcParam.json.");
            }
            this.setTcParam(root);
            this.setSkipOperatorMsg(true);
            System.out.println("root: " + root.toJSONString());        
        }       
    }

    @Override
    public String getTcParam() {
        if (tcParam == null) {
            setTcParamFromFile("/TcParam.json");
            return root.toJSONString();
        }
        return tcParam.toJSONString();
    }

    @Override
    protected IVCT_BaseModel getIVCT_BaseModel(String tcParamJson, Logger logger) throws TcInconclusive {
        try {
            netnTcParam = new NetnEtrTcParam(tcParamJson);
        } catch (ParseException | IOException e) {
            throw new TcInconclusive("unable to initialize test case root", e);
        }
        baseModel = new NetnEtrIvctBaseModel(logger, netnTcParam);
        baseModel.setSutFederateName(netnTcParam.getSutFederateName());
        baseModel.setFederationName(netnTcParam.getFederationName());
        this.setFederationName(netnTcParam.getFederationName());

        ivct_LoggingFederateAmbassador = new IVCT_LoggingFederateAmbassador(baseModel, logger);

        return baseModel;
    }

    @Override
    protected void logTestPurpose(Logger logger) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        stringBuilder.append("TEST PURPOSE\n");
        stringBuilder.append("This is a test case example for the NETN ETR test suite.\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        final String testPurpose = stringBuilder.toString();

        logger.info(testPurpose);        
    }

    @Override
    protected void preambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_Etr_0001 preamble");
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        // TODO establish precondition for testing
           
        // Initiate rti: connect and join
        logger.info("SUT federate: " + baseModel.getSutFederateName());
        federateHandle = baseModel.initiateRti(this.getClass().getSimpleName(), ivct_LoggingFederateAmbassador);
    }

    @Override
    protected void performTest(Logger logger) throws TcInconclusiveIf, TcFailedIf {
        logger.info("TC_Etr_0001 test case started");
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }
        
        // wait for a BaseEntity from our SuT
        baseModel.getBaseEntityFromSuT();

        // test for SupportedActions == MoveByRoute from SuT
        EntityControlActionEnum32 sa = EntityControlActionEnum32.MoveByRoute;
        logger.info("Test step - test SuT if it supports " + sa);
        baseModel.testSupportedActions(sa);
    }

    @Override
    protected void postambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_Etr_0001 postamble");
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        baseModel.terminate();
    }
    
}
