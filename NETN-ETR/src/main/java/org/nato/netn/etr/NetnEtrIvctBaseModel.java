package org.nato.netn.etr;

import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.nato.ivct.OmtEncodingHelpers.Core.HLAroot;
import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAhandle;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAhandleList;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportInteractionPublication;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.ArrayOfUuidStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.MoveByRouteTaskStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskProgressVariantRecord;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.TaskStatusEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.WaypointStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.CancelTasks;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.ETR_TaskStatus;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.MoveByRoute;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.ObservationReport;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.PositionStatusReport;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.RequestTaskStatus;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.interactions.Task;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.interactions.SMC_EntityControl;
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
import edu.nps.moves.disutil.CoordinateConversions;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.InteractionClassHandleFactory;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.MessageRetractionHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAfixedRecord;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidAttributeHandle;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

public class NetnEtrIvctBaseModel extends IVCT_BaseModel {

    // design issue: logger is private in IVCT_BaseModel
    private Logger logger;
    private ConcurrentHashMap<ObjectInstanceHandle, BaseEntity> knownBaseEntities = new ConcurrentHashMap<>();
    private List<BaseEntity> baseEntitiesFromSuT = new CopyOnWriteArrayList<>();
    private List<BaseEntity> baseEntitiesFromSuTWithSA;
    private List<ETR_TaskStatus> taskStatusList = new CopyOnWriteArrayList<>();
    private List<SMC_Response> responses = new CopyOnWriteArrayList<>();
    private List<ObservationReport> reports = new CopyOnWriteArrayList<>();
    private List<PositionStatusReport> positions = new CopyOnWriteArrayList<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private BaseEntity subscribedAttributes;
    private BlockingSupport bs = null;
    private MOMsupport ms = new MOMsupport();

    public NetnEtrIvctBaseModel(Logger logger, IVCT_TcParam ivct_TcParam) throws TcInconclusive {
        super(logger, ivct_TcParam);
        this.logger = logger;
        bs = new BlockingSupport(executorService, 100, logger);
        BaseEntity.initialize(ivct_rti);
    }

    public MOMsupport geMoMsupport() {
        return ms;
    }

    public void registerPubSub(FederateHandle fh) throws TcInconclusive {
        if (fh == null) {
            throw new TcInconclusive("Call initializeRTI first.");
        }
        subscribedAttributes = subscribeAttributes();
        publishInteractions();
        subscribeInteractions();
        try {
            MOMsupport.subscribeHLAreports();
        } catch (FederateServiceInvocationsAreBeingReportedViaMOM | InteractionClassNotDefined | SaveInProgress
                | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError | NameNotFound
                | InvalidObjectClassHandle | AttributeNotDefined | ObjectClassNotDefined
                | OmtEncodingHelperException e) {
            throw new TcInconclusive("Could not pub/sub for HLAreports" + e.getMessage());
        }
    }

    public void addTaskStatus(UUIDStruct us, TaskStatusEnum32 tse) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, OmtEncodingHelperException {
        ETR_TaskStatus ts = new ETR_TaskStatus();
        ts.setTask(us);
        ts.setStatus(tse);
        taskStatusList.add(ts);
        logger.info("Task status " + tse + " for " + us + " injected");
        logger.info("Task status list " + taskStatusList.stream().map(t -> {
            try {
                return t.getStatus();
            } catch (EncoderException | DecoderException e) {
                return TaskStatusEnum32.Error;
            }
        }).collect(Collectors.toList()));
    }

    public MoveByRoute createTask(List<NetnEtrTcParam.Point2D> waypoints, float speed) throws RTIinternalError, NameNotFound, FederateNotExecutionMember, NotConnected, OmtEncodingHelperException {
        MoveByRouteTaskStruct t = new MoveByRouteTaskStruct(); // standard move type is CrossCountry
        for (NetnEtrTcParam.Point2D wpc : waypoints) {
            WaypointStruct wp = new WaypointStruct();
            double [] wcs = CoordinateConversions.getXYZfromLatLonDegrees(wpc.getX(), wpc.getY(), 0.0);
            
            wp.getLocation().setX(wcs[0]);
            wp.getLocation().setY(wcs[1]);
            wp.getLocation().setZ(wcs[2]);
            wp.setSpeed(speed);
            wp.setSegmentMaxWidth(1.0f);
            t.getArrayOfWaypointsStruct().addElement(wp);   
        }
        MoveByRoute mbr = new MoveByRoute();
        mbr.setTaskParameters(t);        
        return mbr;
    }

    public CancelTasks createCancelTasks(UUIDStruct us, BaseEntity be) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, OmtEncodingHelperException, InvalidObjectClassHandle, EncoderException {
        CancelTasks ct = new CancelTasks();
        ArrayOfUuidStruct auid = new ArrayOfUuidStruct();
        auid.addElement(us);
        ct.setTasks(auid);
        ct.setEntity(be.getUniqueId());
        return ct;
    }

    public RequestTaskStatus createRequestTaskStatus(UUIDStruct us, BaseEntity be) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, OmtEncodingHelperException, InvalidObjectClassHandle, EncoderException {
        RequestTaskStatus rts = new RequestTaskStatus();
        if (us != null) {
            ArrayOfUuidStruct auid = new ArrayOfUuidStruct();
            auid.addElement(us);
            rts.setTasks(auid);
        }
        rts.setEntity(be.getUniqueId());
        return rts;
    }

    public static UUIDStruct createRandomUUID() throws RTIinternalError, DecoderException {
        UUIDStruct randomUUID = new UUIDStruct();
        UUID u = UUID.randomUUID();
        randomUUID.decode(HexFormat.of().parseHex(u.toString().replace("-", "")));
        return randomUUID;       
    }
    
    public UUIDStruct sendSMCControl(SMC_EntityControl ec, BaseEntity be) throws TcInconclusiveIf {
        UUIDStruct uniqueId = null;
        try {
            uniqueId = createRandomUUID();
            ec.setEntity(be.getUniqueId());
            ec.setUniqueId(uniqueId);
            ec.send();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidObjectClassHandle
                | EncoderException | DecoderException | InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress | OmtEncodingHelperException e) {
           throw new TcInconclusiveIf("Could not send task " + ec.getClass().getSimpleName());
        }
        return uniqueId;
    }

    // returns UUID from Task interaction
    public UUIDStruct sendTask(Task t, BaseEntity be, UUIDStruct taskId) throws TcInconclusiveIf {
        UUIDStruct uniqueId = null;
        try {
            uniqueId = createRandomUUID();
            t.setUniqueId(uniqueId);
            t.setTaskId(taskId);
            t.setTasker(uniqueId);
            t.setEntity(be.getUniqueId());
            t.send();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress | InvalidObjectClassHandle | EncoderException | DecoderException e) {
            throw new TcInconclusiveIf("Could not send task " + t.getClass().getSimpleName());
        }
        return uniqueId;
    }
    
    private String getHLAinteractionClassName(HLAhandle h) {
        InteractionClassHandleFactory x;
        try {
            x = HLAroot.getRtiAmbassador().getInteractionClassHandleFactory();
            byte [] ba = h.toByteArray();
            // skip the length field: offset 4
            InteractionClassHandle ich = x.decode(ba, 4);
            return HLAroot.getRtiAmbassador().getInteractionClassName(ich);            
        } catch (FederateNotExecutionMember | NotConnected | OmtEncodingHelperException | CouldNotDecode | RTIinternalError | EncoderException | InvalidInteractionClassHandle e) {
            e.printStackTrace();
            return null;
        }         
    }
        
    private void process(HLAhandle h) {
        String si = getHLAinteractionClassName(h);
        logger.info("Federate published interaction " + si);
        ms.addPublishedInteraction(si);
    }

    // callback section
    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        if (true) {
            try {
                String receivedClass = ivct_rti.getInteractionClassName(interactionClass);
                //
                ETR_TaskStatus ts = new ETR_TaskStatus();
                if (receivedClass.equals(ts.getHlaClassName())) {
                    ts.decode(theParameters);
                    taskStatusList.add(ts);
                    logger.trace("ETR_TaskStatus received: " + ts.getTask() + " " + ts.getStatus());
                }
                //
                SMC_Response re = new SMC_Response();
                if (receivedClass.equals(re.getHlaClassName())) {
                    re.decode(theParameters);
                    responses.add(re);
                    logger.trace("SMC_Response received: " + re.getAction() + " " + re.getStatus());
                }
                //
                ObservationReport or = new ObservationReport();
                if (receivedClass.equals(or.getHlaClassName())) {
                    or.decode(theParameters);
                    reports.add(or);
                    logger.trace("ObservationReport received: " + or.getReportId());
                }
                //
                PositionStatusReport psr = new PositionStatusReport();
                if (receivedClass.equals(psr.getHlaClassName())) {
                    psr.decode(theParameters);
                    positions.add(psr);
                    logger.trace("PositionStatusReport received: " + psr.getHeading() + " " + psr.getPosition() + " " + psr.getSpeed());
                }
                //
                HLAreportInteractionPublication rip = HLAreportInteractionPublication.discover(interactionClass);
                if (rip != null) {
                    rip.clear();
                    rip.decode(theParameters);
                    HLAhandleList hl = rip.getHLAinteractionClassList();
                    StreamSupport.stream(hl.spliterator(), false).forEach(h -> process(h));
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
                    requestAttributeValueUpdate(theObject);
                    baseEntitiesFromSuT.add(obj);
                }
            }             
        } catch (InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError | NameNotFound | EncoderException | OmtEncodingHelperException e) {
            throw new FederateInternalError(e.getMessage());
        }
    }

    // Only active, if switch 'conveyProducingFederate' is enabled
    // this callback delivers only base entities from SuT federate
    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName, final FederateHandle producingFederate) throws FederateInternalError {
        BaseEntity be = checkSutHandle(producingFederate, theObject);
        if (be != null && !baseEntitiesFromSuT.contains(be)) {
            requestAttributeValueUpdate(theObject);
            baseEntitiesFromSuT.add(be);
        } else {
            discoverObjectInstance(theObject, theObjectClass, objectName);            
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
        Optional<BaseEntity> obe = baseEntitiesFromSuT.stream().filter(be -> be.getObjectHandle().equals(theObject)).findAny();
        if (obe.isEmpty()) return;

        BaseEntity baseEntityFromSuT = obe.get();
        baseEntityFromSuT.clear();

        try {
            logger.trace("Attribute Name: " + ivct_rti.getAttributeName(baseEntityFromSuT.getClassHandle(), theAttributes.keySet().iterator().next()));
            baseEntityFromSuT.decode(theAttributes);
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected
                | RTIinternalError | DecoderException | AttributeNotDefined | InvalidAttributeHandle e) {
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
            baseEntity.subscribePlannedTasks();
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
            CancelTasks ct = new CancelTasks();
            ct.publish();
            RequestTaskStatus rts = new RequestTaskStatus();
            rts.publish();
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | InteractionClassNotDefined | SaveInProgress | RestoreInProgress e) {
            throw new TcInconclusive("Could not publish interaction class " + e.getMessage());
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
            throw new TcInconclusive("Could not subscribe to one of {SMC_Response, ETR_TaskStatus, ObservationReport, PositionStatusReport}");
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
        bs.waitWhile(() -> {return baseEntitiesFromSuT.isEmpty();}, "BaseEntity", -1);
    }

    // testing
    public List<BaseEntity> waitForSupportedActions(EntityControlActionEnum32 sat) {
        bs.waitWhile(() -> {filterSupportedActions(sat);return baseEntitiesFromSuTWithSA.isEmpty();}, "Supported actions", -1);
        return baseEntitiesFromSuTWithSA;
    }

    public void waitForSMC_Responses() {
        bs.waitWhile(() -> {return responses.isEmpty();}, "SMC_Reponses", -1);
    }

    public void waitForETR_TaskStatus(UUIDStruct taskId, TaskStatusEnum32 ts) {
        bs.waitWhile(() -> {return !testETR_TaskStatus(taskId, ts);}, "ETR_TaskStatus " + ts, -1);
    }

    public void waitForETR_TaskStatusWithCount(UUIDStruct taskId, TaskStatusEnum32 ts, long cnt) {
        bs.waitWhile(() -> {return cnt > countETR_TaskStatus(taskId, ts);}, "ETR_TaskStatus " + ts, -1);
    }

    public void waitForObservationReportsFromSuT() {
        // set timeout here!
        bs.waitWhile(() -> {return reports.isEmpty();}, "ObservationReport", 5);
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

    public boolean testPlannedTasks(BaseEntity be, UUIDStruct reqTaskId) {
        //
        try {
            return StreamSupport.stream(be.getPlannedTasks().spliterator(), false).anyMatch(
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
                logger.trace("ETR_TaskStatus: " + r.getTask() + " " + r.getStatus());
                logger.trace("ETR_TaskStatus uid: " + uid + " ts: " + ts);
                return r.getTask().equals(uid) && r.getStatus().equals(ts);
            } catch (EncoderException | DecoderException e) {
                logger.error("Error in testETR_TaskStatus: " + e.getMessage());
                return false;
            }
        });
    }

    private long countETR_TaskStatus(UUIDStruct uid, TaskStatusEnum32 ts) {
        return taskStatusList.stream().filter(r -> {
            try {
                logger.trace("ETR_TaskStatus: " + r.getTask() + " " + r.getStatus());
                logger.trace("ETR_TaskStatus uid: " + uid + " ts: " + ts);
                return r.getTask().equals(uid) && r.getStatus().equals(ts);
            } catch (EncoderException | DecoderException e) {
                logger.error("Error in testETR_TaskStatus: " + e.getMessage());
                return false;
            }
        }).count();
    }

    public List<String> getReportIds() {
        return reports.stream().map(r -> r.getReportId().toString()).collect(Collectors.toList());
    }

    public String toString(EntityTypeStruct et) {
        if (et == null) {
            return "No EntityType provided.";
        }
        return ("EntityType: (" + 
            "Kind: " + (int)et.getEntityKind() + ", " + 
            "Domain: " + (int)et.getDomain() + "," + 
            "CountryCode: " + et.getCountryCode() + ", " + 
            "Category: " + (int)et.getCategory() + ", " +
            "Subcategory: " + (int)et.getSubcategory() + ", " +
        ")");        
    }

    public String toString(EntityIdentifierStruct eis) {
        if (eis == null) {
            return "No EintityIdentifier provided.";
        }
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
        if (svs == null) {
            return "No position provided.";
        }
        if (svs.getDataElement() == null) {
            return "No position provided.";
        }        
        HLAfixedRecord fr = (HLAfixedRecord)svs.getDataElement().getValue();
        if (fr == null) {
            return "No position provided.";
        }
        WorldLocationStruct loc = (WorldLocationStruct)fr.get(0);
        double [] wc = new double [] {loc.getX(), loc.getY(), loc.getZ()};
        double [] lld = CoordinateConversions.xyzToLatLonDegrees(wc);
        return "Spatial: (" +
            "Position: " + "(" +
                "X: " + lld[0] + ", " + 
                "Y: " + lld[1] + ", " + 
                "Z: " + lld[2] + ", " +
            ")" + 
        ")";
    }

    public void terminate() {
        terminateRti();
        executorService.shutdown();
    }
}
