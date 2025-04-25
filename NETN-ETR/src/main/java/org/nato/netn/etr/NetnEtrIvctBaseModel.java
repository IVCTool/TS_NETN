package org.nato.netn.etr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.ArrayOfTaskDefinitionsStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.MoveByRouteTaskStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskDefinitionStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskStatusEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.WaypointStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.ETR_TaskStatus;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.MoveByRoute;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.datatypes.EntityControlActionsStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.interactions.SMC_Response;
import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_TcParam;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.MessageRetractionHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

public class NetnEtrIvctBaseModel extends IVCT_BaseModel {

    interface Callback {
        boolean call();
    }

    // design issue: logger is private in IVCT_BaseModel
    private Logger logger;
    private HashMap<ObjectInstanceHandle, BaseEntity> knownBaseEntities = new HashMap<>();
    private List<BaseEntity> baseEntitiesFromSuT = new ArrayList<>();
    private List<BaseEntity> baseEntitiesFromSuTWithSA;
    private List<ETR_TaskStatus> taskStatusList = new ArrayList<>();
    private List<SMC_Response> responses = new ArrayList<>();
    private static long RESCHEDULE = 500;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private BaseEntity subscribedAttributes;

    public NetnEtrIvctBaseModel(Logger logger, IVCT_TcParam ivct_TcParam) throws TcInconclusive {
        super(logger, ivct_TcParam);
        this.logger = logger;
        BaseEntity.initialize(ivct_rti);
        subscribedAttributes = subscribeAttributes();
        publishInteractions();
        subscribeInteractions();
    }

    private MoveByRouteTaskStruct createTask() throws RTIinternalError {
        MoveByRouteTaskStruct t = new MoveByRouteTaskStruct(); // standard move type is CrossCountry
        WaypointStruct wp1 = new WaypointStruct();
        wp1.getLocation().setY(48.066);
        wp1.getLocation().setX(11.649);
        wp1.getLocation().setZ(0.0);
        WaypointStruct wp2 = new WaypointStruct();
        wp2.getLocation().setY(48.0684);
        wp2.getLocation().setX(11.642);
        wp2.getLocation().setZ(0.0);
        t.getArrayOfWaypointsStruct().addElement(wp1);
        t.getArrayOfWaypointsStruct().addElement(wp2);
        return t;
    }

    // returns UUID from MoveByRoute interaction
    public UUIDStruct sendTask(BaseEntity be, UUIDStruct taskId) throws TcInconclusiveIf {
        UUIDStruct uniqueId = null;
        try {
            MoveByRoute mbr = new MoveByRoute();
            mbr.setTaskParameters(createTask());
            uniqueId = new UUIDStruct();
            UUID u = UUID.randomUUID();
            uniqueId.encode(new ByteWrapper(u.toString().getBytes()));
            mbr.setUniqueId(uniqueId);
            mbr.setTaskId(uniqueId);
            mbr.setEntity(be.getUniqueId());
            mbr.send();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress | InvalidObjectClassHandle | EncoderException e) {
            throw new TcInconclusiveIf("Could not send task MoveByRoute.");
        }
        return uniqueId;
    }

    // callback section
    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        if (checkSuTHandle(receiveInfo.getProducingFederate())) {
            try {
                String receivedClass = ivct_rti.getInteractionClassName(interactionClass);
                ETR_TaskStatus ts = new ETR_TaskStatus();
                if (receivedClass.equals(ts.getHlaClassName())) {
                    ts.decode(theParameters);
                    taskStatusList.add(ts);
                }
                SMC_Response re = new SMC_Response();
                if (receivedClass.equals(re.getHlaClassName())) {
                    re.decode(theParameters);
                    responses.add(re);
                }                
            } catch (InvalidInteractionClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError | NameNotFound | OmtEncodingHelperException | DecoderException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final LogicalTime theTime, final OrderType receivedOrdering, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        this.logger.warn("receiveInteraction not implemented");
    }


    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final LogicalTime theTime, final OrderType receivedOrdering, final MessageRetractionHandle retractionHandle, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        this.logger.warn("receiveInteraction not implemented");
    }

    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName) throws FederateInternalError {
        try {
            String receivedClass = ivct_rti.getObjectClassName(theObjectClass);
            if (receivedClass.equals(new BaseEntity().getHlaClassName())) {
                if (!knownBaseEntities.keySet().contains(theObject)) {
                    BaseEntity obj = new BaseEntity();
                    obj.setObjectHandle(theObject);
                    knownBaseEntities.put(theObject, obj);
                }
            }             
        } catch (InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError | NameNotFound | EncoderException | OmtEncodingHelperException e) {
            throw new FederateInternalError(e.getMessage());
        }
    }

    // Only active, if switch 'conveyProducingFederate' is enabled
    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName, final FederateHandle producingFederate) throws FederateInternalError {
        discoverObjectInstance(theObject, theObjectClass, objectName);
        BaseEntity be = checkSutHandle(producingFederate, theObject);
        if (be != null && !baseEntitiesFromSuT.contains(be)) {
            requestAttributeValueUpdate(theObject);
            baseEntitiesFromSuT.add(be);
        }
    }    

    @Override
    public void informAttributeOwnership(final ObjectInstanceHandle theObject, final AttributeHandle theAttribute, final FederateHandle theOwner) throws FederateInternalError {
        BaseEntity be = checkSutHandle(theOwner, theObject);
        if (be != null  && !baseEntitiesFromSuT.contains(be)) {
            requestAttributeValueUpdate(theObject);
            baseEntitiesFromSuT.add(be);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes,
            byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport,
            LogicalTime theTime, OrderType receivedOrdering, MessageRetractionHandle retractionHandle,
            SupplementalReflectInfo reflectInfo) throws FederateInternalError {
        logger.trace("reflectAttributeValues with retractionHandle");
        reflectAttributeValues(theObject, theAttributes, userSuppliedTag, sentOrdering, theTransport, reflectInfo);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes,
            byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport,
            LogicalTime theTime, OrderType receivedOrdering, SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        logger.trace("reflectAttributeValues with reflectInfo");
        reflectAttributeValues(theObject, theAttributes, userSuppliedTag, sentOrdering, theTransport, reflectInfo);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes,
            byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport,
            SupplementalReflectInfo reflectInfo) throws FederateInternalError {
        logger.trace("reflectAttributeValues without time");
        
        if (baseEntitiesFromSuT.isEmpty()) return;
        Optional<BaseEntity> obe = baseEntitiesFromSuT.stream().filter(be -> be.getObjectHandle() == theObject).findAny();
        if (obe.isEmpty()) return;

        BaseEntity baseEntityFromSuT = obe.get();
        baseEntityFromSuT.clear();

        try {
            baseEntityFromSuT.decode(theAttributes);
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected
                | RTIinternalError | DecoderException e) {
            logger.error("reflectAttributeValues received Exception", e);
        }
    }

    // end of callback section
    private boolean checkSuTHandle(FederateHandle fh) {
        try {
            FederateHandle sutHandle = ivct_rti.getFederateHandle(getSutFederateName());
            return sutHandle.equals(fh);
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            e.printStackTrace();
        }
        return false;
    }

    private BaseEntity checkSutHandle(FederateHandle theOwner, ObjectInstanceHandle theObject) {
		try {
			BaseEntity baseEntity = knownBaseEntities.get(theObject);
			FederateHandle sutHandle = ivct_rti.getFederateHandle(getSutFederateName());
			if ((sutHandle.equals(theOwner)) && (baseEntity != null)) {
				return baseEntity;
			}
		} catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
			logger.warn("System under Test federate \"{}\" not yet found", getSutFederateName());
		}
		return null;
    }

    private BaseEntity subscribeAttributes() throws TcInconclusive {
        try {
            // this is BaseEntity as defined in NETN-ETR
            // the additional attributes from RPR-BASE BaseEnetity are not available here
            BaseEntity baseEntity = new BaseEntity();
            baseEntity.subscribeSupportedActions();
            baseEntity.subscribeCurrentTasks();
            baseEntity.subscribeTaskProgress();
            baseEntity.subscribeUniqueId();
            baseEntity.subscribe();
            return baseEntity;
        } catch (Exception e) {
            throw new TcInconclusive(e.getMessage());
        }
    }

    private void publishInteractions() throws TcInconclusive {
        try {
            MoveByRoute mbr = new MoveByRoute();
            mbr.publish();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InteractionClassNotDefined | SaveInProgress | RestoreInProgress e) {
            throw new TcInconclusive("Could not publish MoveByRoute interaction class.");
        }
    }

    private void subscribeInteractions() throws TcInconclusive {
        try {
            SMC_Response resp = new SMC_Response();
            resp.subscribe();
            ETR_TaskStatus status = new ETR_TaskStatus();
            status.subscribe();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | FederateServiceInvocationsAreBeingReportedViaMOM | InteractionClassNotDefined | SaveInProgress | RestoreInProgress e) {
            throw new TcInconclusive("Could not subscribe to " + SMC_Response.class.getSimpleName());
        }
    }

    private void waitWhile(ExecutorService executorService, Callback f, String rs) {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            try {
                while (f.call()) {Thread.sleep(RESCHEDULE);}
            } catch (InterruptedException e) {
                //
            }
            return rs;
        }, executorService);

        f1.thenAccept(result -> {
            logger.info(result + " from SuT federate " + getSutFederateName() + " received.");
        })
        .exceptionally(throwable -> {
            logger.error("Error occurred in : waitForSupportedActions.f1 " + throwable.getMessage());
            return null;
        });        
    }

    public void requestAttributeValueUpdate(ObjectInstanceHandle baseEntityFromSuT) {
        // trigger reflection of attributes for objectHandle of baseEntityFromSuT
        try {
            ivct_rti.requestAttributeValueUpdate(baseEntityFromSuT, subscribedAttributes.getSubscribedAttributes(), null);
        } catch (AttributeNotDefined | ObjectInstanceNotKnown | SaveInProgress | RestoreInProgress
                | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            //
        }
    }

    public void waitForBaseEntitiesFromSuT() {
        //
        waitWhile(executorService, () -> {return baseEntitiesFromSuT.isEmpty();}, getSutFederateName());
    }

    // testing
    public List<BaseEntity> waitForSupportedActions(EntityControlActionEnum32 sat) {
        waitWhile(executorService, () -> {filterSupportedActions(sat);return baseEntitiesFromSuTWithSA.isEmpty();}, getSutFederateName());
        return baseEntitiesFromSuTWithSA;
    }

    public void waitForSMC_Responses() {
        waitWhile(executorService, () -> {return responses.isEmpty();}, getSutFederateName());
    }

    public void waitForETR_TaskStatus(int num) {
        waitWhile(executorService, () -> {return taskStatusList.size() > (num - 1);}, getSutFederateName());
    }

    private void filterSupportedActions(EntityControlActionEnum32 sat) {
        //
        baseEntitiesFromSuTWithSA = baseEntitiesFromSuT.stream().filter(be -> {
            boolean flag = false;
            try {
                EntityControlActionsStruct ec = be.getSupportedActions();
                
                for (HLAinteger32BE value : ec) {
                    if (EntityControlActionEnum32.get(value.getValue()).equals(sat)) {
                        flag = true;
                        break;
                    }
                }
            } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected
                    | RTIinternalError | EncoderException | DecoderException e) {
                e.printStackTrace();
            }
            return flag;
        }).collect(Collectors.toList());
    }

    public boolean testCurrentTasks(BaseEntity be, UUIDStruct reqTaskId) {
        //
        boolean ret = false;
        try {
            ArrayOfTaskDefinitionsStruct currentTasks = be.getCurrentTasks();
            for (TaskDefinitionStruct td : currentTasks) {
                if (td.getTaskId().equals(reqTaskId)) {
                    ret = true;
                    break;
                }
            }
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError
                | EncoderException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean testSMC_Response(UUIDStruct uid) {
        Optional<SMC_Response> or = responses.stream().filter(r -> r.getAction().equals(uid)).findAny();
        if (or.isEmpty()) return false;
        return or.get().getStatus();
    }

    public TaskStatusEnum32 testETR_TaskStatus(UUIDStruct uid) throws EncoderException, DecoderException {
        List<ETR_TaskStatus> l = taskStatusList.stream().filter(r -> r.getTask().equals(uid)).collect(Collectors.toList());
        // fetch the last one
        Collections.reverse(l);
        if (l.isEmpty()) return null;
        return l.get(0).getStatus();
    }

    public void terminate() {
        terminateRti();
        executorService.shutdown();
    }
}
