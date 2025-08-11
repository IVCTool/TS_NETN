package org.nato.netn.etr;

import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.AbstractTestCase;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_LoggingFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.TcFailed;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib_if.TcFailedIf;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.MoveTaskProgressStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskStatusEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.CancelTasks;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.ETR_Report;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.MoveByRoute;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.RequestTaskStatus;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;

import hla.rti1516e.FederateHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

/**
 * Test case implementation for the ETR Capability Badge - SuT in task receiver role
 * <p>For test coverage see <a href="https://github.com/IVCTool/TS_NETN/blob/b1817c4e16487b654d72debc9d8884357b46c5a8/NETN-ETR/src/main/resources/NetnEtrTestSuite.json">Test coverage</a></p>
 * <p>and for IR descriptions
 * see <a href="https://github.com/IVCTool/TS_NETN/blob/b1817c4e16487b654d72debc9d8884357b46c5a8/NETN-ETR/src/main/resources/NetnEtrTestSuite.json">List of IRs for ETR</a>
 * </p>
 */
public class TC_Etr_0001 extends AbstractTestCase {

    private NetnEtrIvctBaseModel baseModel = null;
    private NetnEtrTcParam netnTcParam = null;
    private IVCT_LoggingFederateAmbassador ivct_LoggingFederateAmbassador;
    private FederateHandle federateHandle;
    private JSONObject root = null;
    private boolean selfTest = true;
    private boolean cancelTask = false;

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
        logger.info("Waypoints: " + netnTcParam.getWaypoints());
        baseModel.setSutFederateName(netnTcParam.getSutFederateName());
        baseModel.setFederationName(netnTcParam.getFederationName());
        this.setFederationName(netnTcParam.getFederationName());
        selfTest = netnTcParam.getSelfTest();
        cancelTask = netnTcParam.getCancelTask();
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
        stringBuilder.append("SuT will be tested in the role of a task receiver.\n");
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

        // TODO: establish precondition for testing
           
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
        
        try {
            MOMsupport.requestPublicationReportsFromFederate(getSutFederateName());
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | InvalidInteractionClassHandle | EncoderException | InteractionClassNotPublished
                | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress
                | OmtEncodingHelperException e) {
            throw new TcInconclusiveIf("Could not request publication reports from RTI." + e.getMessage());
        }

        // ETR00002, ETR00015
        String [] sa = netnTcParam.getSupportedActions();
        EntityControlActionEnum32 eca = EntityControlActionEnum32.valueOf(sa[0]);
        logger.info("Test step - test SuT if it supports " + sa[0]);
        if (!selfTest) {
            baseModel.waitForBaseEntitiesFromSuT();
        }

        // wait for a minimum of one BaseEntity with requested supported action
        logger.info("Test step - test baseEntities from SuT if one of them supports " + sa);
        BaseEntity be = null;
        if (!selfTest) {
            List<BaseEntity> baseEntitiesFromSuT_SA = baseModel.waitForSupportedActions(eca);
            // take the first one (it definitely exists) and task it
            be = baseEntitiesFromSuT_SA.get(0);
        }

        // test for task id, which is used to task entities of SuT
        String taskId = netnTcParam.getTaskId();
        try {
            UUIDStruct us = new UUIDStruct();
            // us.decode(new ByteWrapper(taskId.getBytes(Charset.forName("UTF-16BE"))));
            us.decode(HexFormat.of().parseHex(taskId.replace("-", "")));
            logger.info("Send MoveByRoute task with id " + taskId + " to " + be.getUniqueId());
            MoveByRoute mbr = baseModel.createTask(netnTcParam.getWaypoints(), netnTcParam.getSpeed());
            UUIDStruct interactionId = baseModel.sendTask(mbr, be, us);
            
            // ETR00003, ETR00016
            if (!selfTest) baseModel.waitForSMC_Responses();
            boolean accepted = (selfTest) ? true : baseModel.testSMC_Response(interactionId);
            logger.info("SuT responded to task with taskId " + taskId + ": " + accepted);
            
            if (accepted) {
                // ETR00004, ETR00017
                if (selfTest) baseModel.addTaskStatus(us, TaskStatusEnum32.Accepted);
                baseModel.waitForETR_TaskStatus(us, TaskStatusEnum32.Accepted);
                logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Accepted);
                logger.info("Task with id " + taskId + " is in the planned tasks list: " + baseModel.testPlannedTasks(be, us));
                if (selfTest) baseModel.addTaskStatus(us, TaskStatusEnum32.Executing);
                baseModel.waitForETR_TaskStatus(us, TaskStatusEnum32.Executing);
                logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Executing);     
                // log RPR-BASE attributes
                logger.info(baseModel.toString(be.getEntityType()));
                logger.info(baseModel.toString(be.getEntityIdentifier()));
                logger.info(baseModel.toString(be.getSpatial()));
                // ETR00009, ETR00018
                RequestTaskStatus rts = baseModel.createRequestTaskStatus(us, be);
                UUIDStruct uid = baseModel.sendSMCControl(rts, be);
                logger.info("RequestTaskStatus sent to BaseEntity " + be.getUniqueId() + " for taskId " + taskId);
                // if (selfTest) baseModel.addTaskStatus(us, TaskStatusEnum32.Executing);
                // baseModel.waitForETR_TaskStatusWithCount(us, TaskStatusEnum32.Executing, 2);
                // logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Executing);
                
                // ETR00005, ETR00019
                logger.info("Task with id " + taskId + " is in the current tasks list: " + baseModel.testCurrentTasks(be, us));
                logger.info("Task progress for task id " + taskId + " found: " + baseModel.testTaskProgress(be, us, eca, MoveTaskProgressStruct.class));
                
                // ETR00007
                if (!baseModel.geMoMsupport().testInteractionPublication(new ETR_Report(), Arrays.asList("ObservationReport", "PositionStatusReport"))) throw new TcFailed("ETR00007");
                // only as a sematic test if SuT sends any reports
                baseModel.waitForObservationReportsFromSuT();
                logger.info("Reports so far: " + baseModel.getReportIds());
                
                if (cancelTask) {
                    // ETR00010
                    CancelTasks ct = baseModel.createCancelTasks(us, be);
                    uid = baseModel.sendSMCControl(ct, be);
                    if (selfTest) baseModel.addTaskStatus(us, TaskStatusEnum32.Cancelled);
                    baseModel.waitForETR_TaskStatus(uid, TaskStatusEnum32.Cancelled);
                    logger.info("Task with id " + us + " cancelled.");
                } else {
                    if (selfTest) baseModel.addTaskStatus(us, TaskStatusEnum32.Completed);
                    baseModel.waitForETR_TaskStatus(us, TaskStatusEnum32.Completed);
                    logger.info("Status from task with id " + taskId + " is " + TaskStatusEnum32.Completed);
                    logger.info(baseModel.toString(be.getSpatial())); // one could test here for be.getSpatial() == netnTcParam.getWaypoints().getLast(), but that would be a semantic test
                }

            } else {
                throw new TcInconclusive("Task " + taskId + " was not accepted");
            }
        } catch (RTIinternalError | NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | EncoderException | DecoderException | OmtEncodingHelperException e) {
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
