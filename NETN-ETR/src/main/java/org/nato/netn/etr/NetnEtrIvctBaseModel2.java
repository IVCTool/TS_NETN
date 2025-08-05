package org.nato.netn.etr;

import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.nato.ivct.OmtEncodingHelpers.Core.HLAroot;
import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAhandle;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAhandleList;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAinteractionSubList;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAinteractionSubscription;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportInteractionPublication;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportInteractionSubscription;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportObjectClassPublication;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportObjectClassSubscription;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLArequestPublications;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLArequestSubscriptions;
import org.nato.ivct.OmtEncodingHelpers.Core.objects.HLAfederate;
import org.nato.ivct.OmtEncodingHelpers.Netn.Base.datatypes.UUIDStruct;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.datatypes.EntityControlActionEnum32;
import org.nato.ivct.OmtEncodingHelpers.Netn.Etr.objects.BaseEntity;
import org.nato.ivct.OmtEncodingHelpers.Netn.Smc.datatypes.EntityControlActionsStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.EntityIdentifierStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.EntityTypeStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.FederateIdentifierStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.SpatialStaticStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.SpatialVariantStruct;
import org.nato.ivct.OmtEncodingHelpers.RPR.Base.datatypes.WorldLocationStruct;
import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_TcParam;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import edu.nps.moves.disutil.CoordinateConversions;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleFactory;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.InteractionClassHandleFactory;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectClassHandleFactory;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.AttributeNotOwned;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
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

public class NetnEtrIvctBaseModel2 extends IVCT_BaseModel {
    private Logger logger;
    private NetnEtrTcParam netParam;
    private UUIDStruct entityId = null;
    private BaseEntity be = null;

    public NetnEtrIvctBaseModel2(Logger logger, NetnEtrTcParam ivct_TcParam) throws TcInconclusive {
        super(logger, ivct_TcParam);
        netParam = ivct_TcParam;
        this.logger = logger;
        BaseEntity.initialize(ivct_rti);
        try {
            entityId = createRandomUUID();
        } catch (RTIinternalError | DecoderException e) {
            e.printStackTrace();
        }
        try {
            be = createBaseEntity(15.55502, 58.38580);
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError
                | EncoderException | TcInconclusive | OmtEncodingHelperException | DecoderException e) {
            throw new TcInconclusive("Could not create base entity.");
        }
    }

    private void subscribeHLAreports() throws TcInconclusive {
        try {
            HLAfederate.addSub(HLAfederate.Attributes.HLAfederateHandle);
            HLAfederate.addSub(HLAfederate.Attributes.HLAfederateName);
            HLAfederate.addSub(HLAfederate.Attributes.HLAfederateType);
            HLAfederate.addSub(HLAfederate.Attributes.HLAfederateHost);
            HLAfederate.addSub(HLAfederate.Attributes.HLARTIversion);
            HLAfederate.sub();
            (new HLAreportObjectClassPublication()).subscribe();
            (new HLAreportObjectClassSubscription()).subscribe();
            (new HLAreportInteractionPublication()).subscribe();
            (new HLAreportInteractionSubscription()).subscribe();

            HLArequestPublications reqPublications = new HLArequestPublications();
            HLArequestSubscriptions reqSubscriptions = new HLArequestSubscriptions();
            reqPublications.publish();
            reqSubscriptions.publish();                    
        } catch (NameNotFound | InvalidObjectClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError
                | OmtEncodingHelperException | AttributeNotDefined | ObjectClassNotDefined | SaveInProgress | RestoreInProgress | FederateServiceInvocationsAreBeingReportedViaMOM | InteractionClassNotDefined e) {
            throw new TcInconclusive("Could not pub/sub for HLAreports" + e.getMessage());
        }

    }

    public void registerPubSub(FederateHandle fh) throws TcInconclusive {
        if (fh == null) {
            throw new TcInconclusive("Call initializeRTI first.");
        }
        subscribeHLAreports();
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
    
    public static UUIDStruct createRandomUUID() throws RTIinternalError, DecoderException {
        UUIDStruct randomUUID = new UUIDStruct();
        UUID u = UUID.randomUUID();
        randomUUID.decode(HexFormat.of().parseHex(u.toString().replace("-", "")));
        return randomUUID;       
    }

    private void process(HLAinteractionSubscription sub) {
        System.out.println("<<<<<<<<<<<<<<<< " + getHLAinteractionClassName(sub.getHLAinteractionClass()));
    }

    public void updateBaseEntity() throws AttributeNotOwned, AttributeNotDefined, ObjectInstanceNotKnown, SaveInProgress, RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError, NameNotFound, InvalidObjectClassHandle, EncoderException {
        be.update();
    }

    public BaseEntity createBaseEntity(double lat, double lon) throws NameNotFound, InvalidObjectClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError, EncoderException, OmtEncodingHelperException, TcInconclusive, DecoderException {
        String [] sa = netParam.getSupportedActions();
        BaseEntity be = new BaseEntity();
        EntityControlActionsStruct eac = new EntityControlActionsStruct();
        for (int i=0;i<sa.length;i++) {
            EntityControlActionEnum32 eca = EntityControlActionEnum32.valueOf(sa[i]);
            eac.addElement(eca.getDataElement());
        }
        be.setSupportedActions(eac);
        be.setUniqueId(entityId);
        //
        EntityIdentifierStruct eis = new EntityIdentifierStruct();
        eis.setEntityNumber((short)1); // only one available in this federate
        FederateIdentifierStruct fis = new FederateIdentifierStruct();
        fis.setSiteID((short)0);
        fis.setApplicationID((short)0);
        eis.setFederateIdentifier(fis);
        be.setEntityIdentifier(eis);
        //
        EntityTypeStruct ets = new EntityTypeStruct();
        be.setEntityType(ets);
        //
        SpatialVariantStruct svs = new SpatialVariantStruct();
        SpatialStaticStruct sss = new SpatialStaticStruct();
        WorldLocationStruct wls = new WorldLocationStruct();
        double [] wcs = CoordinateConversions.getXYZfromLatLonDegrees(lat, lon, 0.0);
        wls.setX(wcs[0]);
        wls.setY(wcs[1]);
        wls.setZ(wcs[2]);
        sss.setWorldLocation(wls);
        svs.setSpatialStatic(sss);
        be.setSpatial(svs);
        //  
        return be;
    }
    
    // callback section
    @Override      
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        try {
            AttributeHandleFactory ahf = HLAroot.getRtiAmbassador().getAttributeHandleFactory();
            ObjectClassHandleFactory ochf = HLAroot.getRtiAmbassador().getObjectClassHandleFactory();

            String receivedClass = ivct_rti.getInteractionClassName(interactionClass);
            System.out.println("++++++ " + receivedClass);                
            //
            HLAreportInteractionSubscription ris = HLAreportInteractionSubscription.discover(interactionClass);
            if (ris != null) {
                logger.info("--------------> HLAreportInteractionSubscription received");
                ris.clear();
                ris.decode(theParameters);
                HLAinteractionSubList hhl = ris.getHLAinteractionClassList();
                StreamSupport.stream(hhl.spliterator(), false).forEach(h -> process(h));
            }
            //
            HLAreportObjectClassSubscription rocs = HLAreportObjectClassSubscription.discover(interactionClass);
            if (rocs != null) {
                logger.info("--------------> HLAreportObjectClassSubscription received");
                rocs.clear();
                rocs.decode(theParameters);
                HLAhandle och = rocs.getHLAObjectClass();
                byte [] obj = och.toByteArray();
                ObjectClassHandle ocha = ochf.decode(obj, 4);
                String cn = HLAroot.getRtiAmbassador().getObjectClassName(ocha);
                //System.out.println("~~~~~~~~~~ class name " + cn);
                if (cn.equals("HLAobjectRoot.BaseEntity")) {
                    HLAhandleList hl = rocs.getHlAattributeList();
                    boolean found = StreamSupport.stream(hl.spliterator(), false).anyMatch(ha -> {
                        byte [] ba = ha.toByteArray();
                        try {
                            AttributeHandle ah = ahf.decode(ba, 4);
                            String ocn = HLAroot.getRtiAmbassador().getAttributeName(ocha, ah);
                            //System.out.println("############# attribute "+ ocn + " found.");
                            return ocn.equals("SupportedActions");
                        } catch (CouldNotDecode | FederateNotExecutionMember | NotConnected | RTIinternalError | AttributeNotDefined | InvalidAttributeHandle | InvalidObjectClassHandle | OmtEncodingHelperException e) {
                            e.printStackTrace();
                            return false;
                        }
                    });
                    if (found) {
                        logger.info("Found supportedActions attribute in BaseEntity.");
                    }
                }
            }
        } catch (InvalidInteractionClassHandle | FederateNotExecutionMember | NotConnected | RTIinternalError | OmtEncodingHelperException | DecoderException | CouldNotDecode | InvalidObjectClassHandle e) {
            e.printStackTrace();
        }
    }   

    @Override
    public void provideAttributeValueUpdate(final ObjectInstanceHandle theObject, final AttributeHandleSet theAttributes, final byte[] userSuppliedTag) throws FederateInternalError {
        if (theObject.equals(be.getObjectHandle())) {
            try {
                updateBaseEntity();
            } catch (AttributeNotOwned | AttributeNotDefined | ObjectInstanceNotKnown | SaveInProgress
                    | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError | NameNotFound
                    | InvalidObjectClassHandle | EncoderException e) {
                throw new FederateInternalError(e.getMessage());
            }
        }
    }

    public void terminate() {
        terminateRti();
    }    
}
