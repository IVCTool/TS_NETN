package org.nato.netn.etr;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
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
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

public class NetnEtrIvctBaseModel extends IVCT_BaseModel {

    // design issue: logger is private in IVCT_BaseModel
    private Logger logger;
    private BaseEntity baseEntity;
    private HashMap<ObjectInstanceHandle, BaseEntity> knownBaseEntities = new HashMap<>();
    private volatile BaseEntity baseEntityFromSuT = null;
    private volatile boolean requestedActionSupported = false;
    private EntityControlActionEnum32 supportedAction2Test;
    private static long RESCHEDULE = 200;

    public NetnEtrIvctBaseModel(Logger logger, IVCT_TcParam ivct_TcParam) {
        super(logger, ivct_TcParam);
        this.logger = logger;
        BaseEntity.initialize(ivct_rti);
    }

    // callback section
    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName) throws FederateInternalError {
        try {
            String receivedClass = ivct_rti.getObjectClassName(theObjectClass);
            if (receivedClass.equals(baseEntity.getHlaClassName())) {
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
        BaseEntity baseEntity = knownBaseEntities.get(theObject); // should be baseEntityFromSuT
        if (baseEntity != null) {
            baseEntity.clear();
            try {
                baseEntity.decode(theAttributes);
                EntityControlActionsStruct supportedActions = baseEntity.getSupportedActions();
                for (HLAinteger32BE value : supportedActions) {
                    EntityControlActionEnum32 ev = EntityControlActionEnum32.get(value.getValue());
                    if (ev.equals(supportedAction2Test)) {
                        // our SuT states that it supports the requested action
                        requestedActionSupported = true;
                    }
                }
            } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected
                    | RTIinternalError | DecoderException e) {
                logger.error("reflectAttributeValues received Exception", e);
            }
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

    private BaseEntity subscribeSupportedActions() throws TcInconclusiveIf {
        try {
            baseEntity = new BaseEntity();
            baseEntity.subscribeSupportedActions();
            baseEntity.subscribe();
            return baseEntity;
        } catch (Exception e) {
            throw new TcInconclusiveIf(e.getMessage());
        }
    }

    public void testSupportedActions(EntityControlActionEnum32 sat) throws TcInconclusiveIf {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        BaseEntity subscribedEntity = subscribeSupportedActions();
        supportedAction2Test = sat;

        // wait for SupportedActions in baseEntityFromSuT
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            try {
                while (baseEntityFromSuT == null) {Thread.sleep(RESCHEDULE);}
            } catch (InterruptedException e) {
                //
            }
            return "Supported Actions";
        }, executorService);

        f1.thenAccept(result -> {
            logger.info(result + " from federate " + getSutFederateName() + " received.");
        })
        .exceptionally(throwable -> {
            logger.error("Error occurred in : waitForSupportedActions.f1 " + throwable.getMessage());
            return null;
        });

        // trigger reflection of attributes for objectHandle of baseEntityFromSuT
        try {
            ivct_rti.requestAttributeValueUpdate(baseEntityFromSuT.getObjectHandle(), subscribedEntity.getSubscribedAttributes(), null);
        } catch (AttributeNotDefined | ObjectInstanceNotKnown | SaveInProgress | RestoreInProgress
                | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
            throw new TcInconclusiveIf(e.getMessage());
        }

        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
            try {
                while (!requestedActionSupported) {Thread.sleep(RESCHEDULE);}
            } catch (InterruptedException e) {
                //
            }
            return "Supported Actions: MoveByRoute";
        }, executorService);

        f2.thenAccept(result -> {
            logger.info(result + " from federate " + getSutFederateName() + " received.");
        })
        .exceptionally(throwable -> {
            logger.error("Error occurred in : waitForSupportedActions.f2 " + throwable.getMessage());
            return null;
        });        

        executorService.shutdown();        
    }
}
