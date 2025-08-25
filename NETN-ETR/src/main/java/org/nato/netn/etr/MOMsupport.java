package org.nato.netn.etr;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.nato.ivct.OmtEncodingHelpers.Core.HLAroot;
import org.nato.ivct.OmtEncodingHelpers.Core.OmtEncodingHelperException;
import org.nato.ivct.OmtEncodingHelpers.Core.datatypes.HLAhandle;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAinteractionRoot;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportInteractionPublication;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportInteractionSubscription;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportObjectClassPublication;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLAreportObjectClassSubscription;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLArequestPublications;
import org.nato.ivct.OmtEncodingHelpers.Core.interactions.HLArequestSubscriptions;
import org.nato.ivct.OmtEncodingHelpers.Core.objects.HLAfederate;

import hla.rti1516e.FederateHandle;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;

public class MOMsupport {
    private List<String> subscribedInteractions = new CopyOnWriteArrayList<>();
    private List<String> publishedInteractions = new CopyOnWriteArrayList<>();

    private static HLAhandle getHandleFromSuT(String name) throws NameNotFound, FederateNotExecutionMember, NotConnected,
            RTIinternalError, OmtEncodingHelperException {
        FederateHandle fh = HLAroot.getRtiAmbassador().getFederateHandle(name);
        byte[] fha = new byte[4];
        fh.encode(fha, 0);
        HLAhandle hh = new HLAhandle();
        for (int i = 0; i < fha.length; i++) {
            hh.addElement(HLAroot.getEncoderFactory().createHLAbyte(fha[i]));
        }
        return hh;
    }

    public static void requestPublicationReportsFromFederate(String fName)
            throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, OmtEncodingHelperException,
            InvalidInteractionClassHandle, EncoderException, InteractionClassNotPublished,
            InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress {
        HLArequestPublications reqPublications = new HLArequestPublications();
        reqPublications.setHLAfederate(getHandleFromSuT(fName).toByteArray());
        reqPublications.send();
    }

    public static void requestSubscriptionReportsFromFederate(String fName)
            throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, OmtEncodingHelperException,
            InvalidInteractionClassHandle, EncoderException, InteractionClassNotPublished,
            InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress {
        HLArequestSubscriptions reqSubscriptions = new HLArequestSubscriptions();
        reqSubscriptions.setHLAfederate(getHandleFromSuT(fName).toByteArray());
        reqSubscriptions.send();
    }

    private static void subscribeToFederateInformation()
            throws NameNotFound, InvalidObjectClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError,
            OmtEncodingHelperException, AttributeNotDefined, ObjectClassNotDefined, SaveInProgress, RestoreInProgress {
        HLAfederate.addSub(HLAfederate.Attributes.HLAfederateHandle);
        HLAfederate.addSub(HLAfederate.Attributes.HLAfederateName);
        HLAfederate.addSub(HLAfederate.Attributes.HLAfederateType);
        HLAfederate.addSub(HLAfederate.Attributes.HLAfederateHost);
        HLAfederate.addSub(HLAfederate.Attributes.HLARTIversion);
        HLAfederate.sub();
    }

    public static void subscribeHLAreports()
            throws FederateServiceInvocationsAreBeingReportedViaMOM, InteractionClassNotDefined, SaveInProgress,
            RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError, NameNotFound,
            OmtEncodingHelperException, InvalidObjectClassHandle, AttributeNotDefined, ObjectClassNotDefined {
        subscribeToFederateInformation();
        (new HLAreportObjectClassPublication()).subscribe();
        (new HLAreportObjectClassSubscription()).subscribe();
        (new HLAreportInteractionPublication()).subscribe();
        (new HLAreportInteractionSubscription()).subscribe();
    }

    public void addPublishedInteraction(String ia) {
        publishedInteractions.add(ia);
    }

    public void addSubscribedInteraction(String ia) {
        subscribedInteractions.add(ia);
    }

    public boolean testInteractionPublication(HLAinteractionRoot ia) {
        return publishedInteractions.stream().anyMatch(s -> s.equals(ia.getHlaClassName()));
    }

    public boolean testInteractionSubscription(HLAinteractionRoot ia) {
        return subscribedInteractions.stream().anyMatch(s -> s.equals(ia.getHlaClassName()));
    }

    public boolean testInteractionPublication(HLAinteractionRoot ia, List<String> subInteractions) {
        String base = ia.getHlaClassName();
        return subInteractions.stream().allMatch(si -> publishedInteractions.stream().anyMatch(s -> s.equals(base + "." + si)));
    }

    public boolean testInteractionSubscription(HLAinteractionRoot ia, List<String> subInteractions) {
        String base = ia.getHlaClassName();
        return subInteractions.stream().allMatch(si -> subscribedInteractions.stream().anyMatch(s -> s.equals(base + "." + si)));
    }
        
}
