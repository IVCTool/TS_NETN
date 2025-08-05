package org.nato.netn.etr;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.nato.ivct.OmtEncodingHelpers.Core.HLAroot;
import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAhandle;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLArequestPublications;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLArequestSubscriptions;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.datatypes.EntityControlActionsStruct;
import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.AbstractTestCase;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_LoggingFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.AttributeNotOwned;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

public class TC_Etr_0002 extends AbstractTestCase {
    private NetnEtrIvctBaseModel2 baseModel = null;
    private FederateHandle federateHandle;
    private NetnEtrTcParam netnTcParam = null;
    private JSONObject root = null;
    private boolean selfTest = true;
    private IVCT_LoggingFederateAmbassador ivct_LoggingFederateAmbassador;

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
        baseModel = new NetnEtrIvctBaseModel2(logger, netnTcParam);
        baseModel.setSutFederateName(netnTcParam.getSutFederateName());
        baseModel.setFederationName(netnTcParam.getFederationName());
        this.setFederationName(netnTcParam.getFederationName());
        selfTest = netnTcParam.getSelfTest();
        ivct_LoggingFederateAmbassador = new IVCT_LoggingFederateAmbassador(baseModel, logger);

        return baseModel;
    }

    @Override
    protected void logTestPurpose(Logger logger) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        stringBuilder.append("TEST PURPOSE\n");
        stringBuilder.append("This test case evaluates the IRs for NETN ETR:\n");
        stringBuilder.append("SuT will be tested in the role of a tasker.\n");
        stringBuilder.append("---------------------------------------------------------------------\n");
        final String testPurpose = stringBuilder.toString();

        logger.info(testPurpose);    
    }

    @Override
    protected void preambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_Etr_0002 preamble");
        
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        // TODO: establish precondition for testing
           
        // Initiate rti: connect and join
        logger.info("SUT federate: " + baseModel.getSutFederateName());
        federateHandle = baseModel.initiateRti(this.getClass().getSimpleName(), ivct_LoggingFederateAmbassador);
        baseModel.registerPubSub(federateHandle);
    }

    @Override
    protected void performTest(Logger logger) throws TcInconclusiveIf, TcFailedIf {
        FederateHandle fh = null;
        String fn = netnTcParam.getSutFederateName();
        try {
            fh = HLAroot.getRtiAmbassador().getFederateHandle(fn);
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException e) {
            throw new TcInconclusiveIf("Could not retrieve federate handle for " + fn);
        }

        byte [] fha = new byte [4];
        fh.encode(fha, 0);
        try {
            HLAhandle hh = new HLAhandle();
            for (int i = 0; i < fha.length; i++) {
                hh.addElement(HLAroot.getEncoderFactory().createHLAbyte(fha[i]));
            }

            HLArequestPublications reqPublications = new HLArequestPublications();
            reqPublications.setHLAfederate(hh.toByteArray());
            reqPublications.send();
            HLArequestSubscriptions reqSubscriptions = new HLArequestSubscriptions();
            reqSubscriptions.setHLAfederate(hh.toByteArray());
            reqSubscriptions.send();
            // publish object class and attributes we will send later on
            BaseEntity be = new BaseEntity();
            be.publishSupportedActions();
            be.publishEntityIdentifier();
            be.publishEntityType();
            be.publishUniqueId();
            be.publishSpatial();
            be.publish();
            baseModel.updateBaseEntity();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InvalidInteractionClassHandle | EncoderException 
                | InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined 
                | SaveInProgress | RestoreInProgress | InvalidObjectClassHandle | AttributeNotDefined | ObjectClassNotDefined 
                | AttributeNotOwned | ObjectInstanceNotKnown e) {
            throw new TcInconclusiveIf(e.getMessage());
        }
    }

    @Override
    protected void postambleAction(Logger logger) throws TcInconclusiveIf {
        logger.info("TC_Etr_0002 postamble");
        
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }

        baseModel.terminate();
    }

}
