package org.nato.netn.etr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.MoveByRouteTaskStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskProgressVariantRecord;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskStatusEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.WaypointStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.ETR_TaskStatus;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.MoveByRoute;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.ObservationReport;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.PositionStatusReport;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.interactions.SMC_Response;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.EntityIdentifierStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.EntityTypeStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.FederateIdentifierStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.SpatialVariantStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.WorldLocationStruct;
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
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAfixedRecord;
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
    private ConcurrentHashMap<ObjectInstanceHandle, BaseEntity> knownBaseEntities = new ConcurrentHashMap<>();
    private List<BaseEntity> baseEntitiesFromSuT = new CopyOnWriteArrayList<>();
    private List<BaseEntity> baseEntitiesFromSuTWithSA;
    private List<ETR_TaskStatus> taskStatusList = new CopyOnWriteArrayList<>();
    private List<SMC_Response> responses = new CopyOnWriteArrayList<>();
    private List<ObservationReport> reports = new CopyOnWriteArrayList<>();
    private List<PositionStatusReport> positions = new CopyOnWriteArrayList<>();
    private static long RESCHEDULE = 500;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private BaseEntity subscribedAttributes;

    public NetnEtrIvctBaseModel(Logger logger, IVCT_TcParam ivct_TcParam) throws TcInconclusive {
        super(logger, ivct_TcParam);
        this.logger = logger;
        BaseEntity.initialize(ivct_rti);
    }

    public void registerPubSub(FederateHandle fh) throws TcInconclusive {
        if (fh == null) {
            throw new TcInconclusive("Call initializeRTI first.");
        }
        subscribedAttributes = subscribeAttributes();
        publishInteractions();
        subscribeInteractions();
    }

    private MoveByRouteTaskStruct createTask(List<NetnEtrTcParam.Point2D> waypoints, float speed) throws RTIinternalError {
        MoveByRouteTaskStruct t = new MoveByRouteTaskStruct(); // standard move type is CrossCountry
        for (NetnEtrTcParam.Point2D wpc : waypoints) {
            WaypointStruct wp = new WaypointStruct();
            wp.getLocation().setY(wpc.getY());
            wp.getLocation().setX(wpc.getX());
            wp.getLocation().setZ(0.0);
            wp.setSpeed(speed);
            wp.setSegmentMaxWidth(1.0f);
            t.getArrayOfWaypointsStruct().addElement(wp);   
        }
        return t;
    }

    // returns UUID from MoveByRoute interaction
    public UUIDStruct sendTask(BaseEntity be, UUIDStruct taskId, List<NetnEtrTcParam.Point2D> waypoints, float speed) throws TcInconclusiveIf {
        UUIDStruct uniqueId = null;
        try {
            MoveByRoute mbr = new MoveByRoute();
            mbr.setTaskParameters(createTask(waypoints, speed));
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
                //
                ETR_TaskStatus ts = new ETR_TaskStatus();
                if (receivedClass.equals(ts.getHlaClassName())) {
                    ts.decode(theParameters);
                    taskStatusList.add(ts);
                }
                //
                SMC_Response re = new SMC_Response();
                if (receivedClass.equals(re.getHlaClassName())) {
                    re.decode(theParameters);
                    responses.add(re);
                }
                //
                ObservationReport or = new ObservationReport();
                if (receivedClass.equals(or.getHlaClassName())) {
                    or.decode(theParameters);
                    reports.add(or);
                }
                //
                PositionStatusReport psr = new PositionStatusReport();
                if (receivedClass.equals(psr.getHlaClassName())) {
                    psr.decode(theParameters);
                    positions.add(psr);
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
            // to see how class object merging works, refer to 
            // B. Möller et al., Extended FOM Module Merging Capabilities, 2013
            BaseEntity baseEntity = new BaseEntity();
            baseEntity.subscribeSupportedActions();
            baseEntity.subscribeCurrentTasks();
            baseEntity.subscribeTaskProgress();
            baseEntity.subscribeUniqueId();
            // additional attributes from RPR-BASE BaseEntity
            baseEntity.subscribeEntityType();
            baseEntity.subscribeEntityIdentifier();
            baseEntity.subscribeSpatial();
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
            ObservationReport or = new ObservationReport();
            or.subscribe();
            PositionStatusReport psr = new PositionStatusReport();
            psr.subscribe();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | FederateServiceInvocationsAreBeingReportedViaMOM | InteractionClassNotDefined | SaveInProgress | RestoreInProgress e) {
            throw new TcInconclusive("Could not subscribe to one of {SMC_Response, ETR_TaskStatus, ObservationReort, PositionStatusReport}");
        }
    }

    private void waitWhile(ExecutorService executorService, Callback f, String rs, int timeout) {
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
        try {
            // block here
            if (timeout > 0) f1.get(timeout, TimeUnit.SECONDS);
            else f1.get();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        } 
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
        waitWhile(executorService, () -> {return baseEntitiesFromSuT.isEmpty();}, "BaseEntity", -1);
    }

    // testing
    public List<BaseEntity> waitForSupportedActions(EntityControlActionEnum32 sat) {
        waitWhile(executorService, () -> {filterSupportedActions(sat);return baseEntitiesFromSuTWithSA.isEmpty();}, "Supported actions", -1);
        return baseEntitiesFromSuTWithSA;
    }

    public void waitForSMC_Responses() {
        waitWhile(executorService, () -> {return responses.isEmpty();}, "SMC_Reponses", -1);
    }

    public void waitForETR_TaskStatus(UUIDStruct taskId, TaskStatusEnum32 ts) {
        waitWhile(executorService, () -> {return testETR_TaskStatus(taskId, ts);}, "ETR_TaskStatus", -1);
    }

    public void waitForObservationReportsFromSuT() {
        // set timeout here!
        waitWhile(executorService, () -> {return reports.isEmpty();}, "ObservationReport", 5);
    }

    private void filterSupportedActions(EntityControlActionEnum32 sat) {
        //
        baseEntitiesFromSuTWithSA = baseEntitiesFromSuT.stream().filter(be -> {
            try {
                return StreamSupport.stream(be.getSupportedActions().spliterator(), false).anyMatch(v -> {
                    try {
                        return EntityControlActionEnum32.get(v.getValue()).equals(sat);
                    } catch (DecoderException e) {
                        return false;
                    }
                });
            } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected
                    | RTIinternalError | EncoderException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    public boolean testCurrentTasks(BaseEntity be, UUIDStruct reqTaskId) {
        //
        try {
            return StreamSupport.stream(be.getCurrentTasks().spliterator(), false).anyMatch(
                ct -> ct.getTaskId().equals(reqTaskId)
                );
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError
                | EncoderException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean testTaskProgress(BaseEntity be, UUIDStruct reqTaskId, EntityControlActionEnum32 eca, Class<?> cls) {
        //
        try {
            return StreamSupport.stream(be.getTaskProgress().spliterator(), false).filter(tp -> tp.getXTaskId().equals(reqTaskId)).anyMatch(tp -> {
                TaskProgressVariantRecord rec = tp.getProgressData();
                try {
                    EntityControlActionEnum32 disc = EntityControlActionEnum32.get(rec.getDiscriminant().getValue());
                    DataElement de = rec.getDataElement().getValue();
                    return (disc.equals(eca) && de.getClass().equals(cls));                    
                } catch (DecoderException e) {
                    return false;
                }
            });
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError
                | EncoderException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean testSMC_Response(UUIDStruct uid) {
        Optional<SMC_Response> or = responses.stream().filter(r -> r.getAction().equals(uid)).findAny();
        if (or.isEmpty()) return false;
        return or.get().getStatus();
    }

    private boolean testETR_TaskStatus(UUIDStruct uid, TaskStatusEnum32 ts) {
        return taskStatusList.stream().anyMatch(r -> {
            try {
                return r.getTask().equals(uid) && r.getStatus().equals(ts);
            } catch (EncoderException | DecoderException e) {
                return false;
            }
        });
    }

    public List<String> getReportIds() {
        return reports.stream().map(r -> r.getReportId().toString()).collect(Collectors.toList());
    }

    public String toString(EntityTypeStruct et) {
        return ("EntityType: (" + 
            "Kind: " + (int)et.getEntityKind() + ", " + 
            "Domain: " + (int)et.getDomain() + "," + 
            "CountryCode: " + et.getCountryCode() + ", " + 
            "Category: " + (int)et.getCategory() + ", " +
            "Subcategory: " + (int)et.getSubcategory() + ", " +
        ")");        
    }

    public String toString(EntityIdentifierStruct eis) {
        FederateIdentifierStruct fi = eis.getFederateIdentifier();
        return "EntityIdentifier: (" + 
            "EintityNumber: " + eis.getEntityNumber() + ", " + 
            "FederteIdentifier: (" + 
                "SiteID: " + fi.getSiteID() + 
                "ApplicationID: " + fi.getApplicationID() +
            ")" +
        ")";
    }

    public String toString(SpatialVariantStruct svs) {
        HLAfixedRecord fr = (HLAfixedRecord)svs.getDataElement().getValue();
        WorldLocationStruct loc = (WorldLocationStruct)fr.get(0);
        return "Spatial: (" +
            "Position: " + "(" +
                "X: " + loc.getX() + ", " + 
                "Y: " + loc.getY() + ", " + 
                "Z: " + loc.getZ() + ", " +
            ")" + 
        ")";
    }

    public void terminate() {
        terminateRti();
        executorService.shutdown();
    }
}
