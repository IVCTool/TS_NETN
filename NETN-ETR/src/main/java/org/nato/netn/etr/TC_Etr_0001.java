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
import java.util.HexFormat;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.MoveTaskProgressStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskStatusEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;

import hla.rti1516e.FederateHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;

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
        // logger.info("Waypoints: " + netnTcParam.getWaypoints());
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
        baseModel.registerPubSub(federateHandle);
    }

    @Override
    protected void performTest(Logger logger) throws TcInconclusiveIf, TcFailedIf {
        logger.info("TC_Etr_0001 test case started");
        
        if (baseModel == null) {
            logger.error("test case not initialized");
            return;
        }
        
        // test for SupportedActions from SuT
        String [] sa = netnTcParam.getSupportedActions();
        EntityControlActionEnum32 eca = EntityControlActionEnum32.valueOf(sa[0]);
        logger.info("Test step - test SuT if it supports " + sa[0]);
        baseModel.waitForBaseEntitiesFromSuT();

        // wait for a minimum of one BaseEntity with requested supported action
        logger.info("Test step - test baseEntities from SuT if one of them supports " + sa);
        // List<BaseEntity> baseEntitiesFromSuT_SA = baseModel.waitForSupportedActions(eca);
        
        // take the first one (it definitely exists) and task it
        // BaseEntity be = baseEntitiesFromSuT_SA.get(0);

        // for testing only
        // BaseEntity be = baseEntitiesFromSuT_SA.get(0);
        BaseEntity be = null;
        try {
            be = new BaseEntity();
            UUIDStruct uis = new UUIDStruct();
            // uis.decode(new ByteWrapper("10390074-76f2-4533-80b3-c5ae2fc11373".getBytes(Charset.forName("UTF-16BE"))));
            byte [] bytes = HexFormat.of().parseHex("1039007476f2453380b3c5ae2fc11373");
            uis.decode(bytes);
            be.setUniqueId(uis);
        } catch (Exception e) {
            e.printStackTrace();;
        }



        // test for task id, which is used to task entities of SuT
        String taskId = netnTcParam.getTaskId();
        try {
            UUIDStruct us = new UUIDStruct();
            // us.decode(new ByteWrapper(taskId.getBytes(Charset.forName("UTF-16BE"))));
            us.decode(HexFormat.of().parseHex(taskId.replace("-", "")));
            logger.info("Send MoveByRoute task with id " + taskId + " to " + be.getUniqueId());
            UUIDStruct interactionId = baseModel.sendTask(be, us, netnTcParam.getWaypoints(), netnTcParam.getSpeed());

            // test SMC_Response, if task was accepted by SuT
            baseModel.waitForSMC_Responses();
            boolean accepted = baseModel.testSMC_Response(interactionId);
            logger.info("SuT responded to task with taskId " + taskId + ": " + accepted);

            if (accepted) {
                // test ETR_Status for accepted, executing
                baseModel.waitForETR_TaskStatus(us, TaskStatusEnum32.Accepted);
                logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Accepted);
                baseModel.waitForETR_TaskStatus(us, TaskStatusEnum32.Executing);
                logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Executing);     
                // log RPR-BASE attributes
                logger.info(baseModel.toString(be.getEntityType()));
                logger.info(baseModel.toString(be.getEntityIdentifier()));
                logger.info(baseModel.toString(be.getSpatial()));
                // test current tasks and task progress in BaseEntity
                logger.info("Task with id " + taskId + " is in the current tasks list: " + baseModel.testCurrentTasks(be, us));
                logger.info("Task progress for task id " + taskId + " found: " + baseModel.testTaskProgress(be, us, eca, MoveTaskProgressStruct.class));
                baseModel.waitForObservationReportsFromSuT();
                logger.info("Reports so far: " + baseModel.getReportIds());
                baseModel.waitForETR_TaskStatus(us, TaskStatusEnum32.Completed);
                logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Completed);
                logger.info(baseModel.toString(be.getSpatial()));
            } else {
                throw new TcInconclusive("Task " + taskId + " was not accepted");
            }
        } catch (RTIinternalError | NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | EncoderException | DecoderException e) {
            throw new TcInconclusive(e.getMessage());
        }
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
