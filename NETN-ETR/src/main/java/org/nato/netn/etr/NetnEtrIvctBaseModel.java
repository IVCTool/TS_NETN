package org.nato.netn.etr;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.ArrayOfTaskDefinitionsStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.ArrayOfTaskProgressStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.MoveByRouteTaskStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskDefinitionStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.WaypointStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.MoveByRoute;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.datatypes.EntityControlActionsStruct;
import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_TcParam;
import de.fraunhofer.iosb.tc_lib_if.TcInconclusiveIf;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.MessageRetractionHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
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
    private volatile BaseEntity baseEntityFromSuT = null;
    private volatile boolean requestedActionSupported = false;
    private volatile boolean requestedTaskIdFound = false;
    private EntityControlActionEnum32 supportedAction2Test;
    private UUIDStruct taskId2Test;
    private static long RESCHEDULE = 200;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public NetnEtrIvctBaseModel(Logger logger, IVCT_TcParam ivct_TcParam) {
        super(logger, ivct_TcParam);
        this.logger = logger;
        BaseEntity.initialize(ivct_rti);
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
    public UUID sendTask(UUIDStruct taskId) throws TcInconclusiveIf {
        UUID u = null;
        if (baseEntityFromSuT == null) {
            throw new TcInconclusiveIf("call first getBaseEntityFromSuT.");
        }
        try {
            MoveByRoute mbr = new MoveByRoute();
            mbr.setTaskParameters(createTask());
            UUIDStruct uniqueId = new UUIDStruct();
            u = UUID.randomUUID();
            uniqueId.encode(new ByteWrapper(u.toString().getBytes()));
            mbr.setUniqueId(uniqueId);
            mbr.setTaskId(uniqueId);
            mbr.setEntity(baseEntityFromSuT.getUniqueId());
            mbr.send();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress | InvalidObjectClassHandle | EncoderException e) {
            throw new TcInconclusiveIf("Could not send task MoveByRoute.");
        }
        return u;
    }
    // callback section
    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName) throws FederateInternalError {
        try {
            String receivedClass = ivct_rti.getObjectClassName(theObjectClass);
            if (receivedClass.equals(new BaseEntity().getHlaClassName())) {
                BaseEntity obj = new BaseEntity();
                obj.setObjectHandle(theObject);
                knownBaseEntities.put(theObject, obj);
            }             
        } catch (InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError | NameNotFound | EncoderException | OmtEncodingHelperException e) {
            throw new FederateInternalError(e.getMessage());
        }
    }

    // Only active, if switch 'conveyProducingFederate' is enabled
    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName, final FederateHandle producingFederate) throws FederateInternalError {
        discoverObjectInstance(theObject, theObjectClass, objectName);
        baseEntityFromSuT = checkSutHandle(producingFederate, theObject);
    }    

    @Override
    public void informAttributeOwnership(final ObjectInstanceHandle theObject, final AttributeHandle theAttribute, final FederateHandle theOwner) throws FederateInternalError {
        baseEntityFromSuT = checkSutHandle(theOwner, theObject);
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
        
        if (baseEntityFromSuT == null) return;
        if (!theObject.equals(baseEntityFromSuT.getObjectHandle())) return;
 
        baseEntityFromSuT.clear();
        try {
            baseEntityFromSuT.decode(theAttributes);
            //
            EntityControlActionsStruct supportedActions = baseEntityFromSuT.getSupportedActions();
            for (HLAinteger32BE value : supportedActions) {
                EntityControlActionEnum32 ev = EntityControlActionEnum32.get(value.getValue());
                if (ev.equals(supportedAction2Test)) {
                    // our SuT states that it supports the requested action
                    requestedActionSupported = true;
                    break;
                }
            }
            //
            ArrayOfTaskDefinitionsStruct currentTasks = baseEntityFromSuT.getCurrentTasks();
            for (TaskDefinitionStruct tds : currentTasks) {
                if (tds.getTaskId().equals(taskId2Test)) {
                    requestedTaskIdFound = true;
                    break;
                }
            }
            //
            ArrayOfTaskProgressStruct taskProgress = baseEntityFromSuT.getTaskProgress();
            //
            UUIDStruct uniqueId = baseEntityFromSuT.getUniqueId();
            //
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected
                | RTIinternalError | DecoderException e) {
            logger.error("reflectAttributeValues received Exception", e);
        }
    }
    // end of callback section

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

    private BaseEntity subscribeAttributes() throws TcInconclusiveIf {
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
            throw new TcInconclusiveIf(e.getMessage());
        }
    }

    private void publishInteractions() {
        try {
            MoveByRoute mbr = new MoveByRoute();
            // mbr.publishTaskId();
            // mbr.publishUniqueId();
            mbr.publish();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InteractionClassNotDefined | SaveInProgress | RestoreInProgress e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    public BaseEntity getBaseEntityFromSuT() throws TcInconclusiveIf {

        // wait for a BaseEntity from our SuT
        waitWhile(executorService, () -> {return baseEntityFromSuT == null;}, "BaseEntity");

        // trigger reflection of attributes for objectHandle of baseEntityFromSuT
        try {
            ivct_rti.requestAttributeValueUpdate(baseEntityFromSuT.getObjectHandle(), subscribeAttributes().getSubscribedAttributes(), null);
        } catch (AttributeNotDefined | ObjectInstanceNotKnown | SaveInProgress | RestoreInProgress
                | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            throw new TcInconclusiveIf(e.getMessage());
        }
        return baseEntityFromSuT;
    }

    public void testSupportedActions(EntityControlActionEnum32 sat) {
        //
        supportedAction2Test = sat;
        waitWhile(executorService, () -> {return !requestedActionSupported;}, "Supported Actions");        
    }

    public void testCurrentTasks(UUIDStruct reqTaskId) {
        //
        taskId2Test = reqTaskId; 
        waitWhile(executorService, () -> {return !requestedTaskIdFound;}, "Supported Actions");        
    }

    public void terminate() {
        terminateRti();
        executorService.shutdown();
    }
}
