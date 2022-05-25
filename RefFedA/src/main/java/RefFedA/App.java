/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package RefFedA;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fraunhofer.iosb.tc_lib.IVCT_LoggingFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.IVCT_NullFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.IVCT_RTIambassador;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactory;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.AlreadyConnected;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.ConnectionFailed;
import hla.rti1516e.exceptions.CouldNotCreateLogicalTimeFactory;
import hla.rti1516e.exceptions.CouldNotOpenFDD;
import hla.rti1516e.exceptions.ErrorReadingFDD;
import hla.rti1516e.exceptions.FederateAlreadyExecutionMember;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateIsExecutionMember;
import hla.rti1516e.exceptions.FederateNameAlreadyInUse;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateOwnsAttributes;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.InconsistentFDD;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidLocalSettingsDesignator;
import hla.rti1516e.exceptions.InvalidResignAction;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.OwnershipAcquisitionPending;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.exceptions.UnsupportedCallbackModel;

public class App extends IVCT_NullFederateAmbassador{

    protected String FEDERATION_NAME = "NETN-Test-Federation";
    protected String FEDERATE_NAME = "RefFedA";
    protected String FEDERATE_TYPE = "RefFed";
    
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(App.class);
    private RTIambassador rtiAmbassador;
    private IVCT_LoggingFederateAmbassador loggingFederateAmbassador;
    private Interaction aggregate;
    private EncoderFactory encoderFactory;

    public App(Logger logger) throws RTIinternalError {
        super(logger);
        RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
        rtiAmbassador = new IVCT_RTIambassador (rtiFactory.getRtiAmbassador(), rtiFactory.getEncoderFactory(),log);
        loggingFederateAmbassador = new IVCT_LoggingFederateAmbassador(this, log);
        encoderFactory = rtiFactory.getEncoderFactory();
    }
    
    public void connectToRti() throws InconsistentFDD, ErrorReadingFDD, CouldNotOpenFDD, NotConnected, RTIinternalError, MalformedURLException, CouldNotCreateLogicalTimeFactory, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, FederateAlreadyExecutionMember, CallNotAllowedFromWithinCallback, ConnectionFailed, InvalidLocalSettingsDesignator, UnsupportedCallbackModel, AlreadyConnected, FederateNameAlreadyInUse {
        File fddFile = new File ("foms/TS-NETN-v4.0.xml");
        ArrayList<URL> foms = new ArrayList<>();
        foms.add(new File("foms/RPR-FOM-v2.0/RPR-Base_v2.0.xml").toURI().toURL());
        foms.add(new File("foms/RPR-FOM-v2.0/RPR-Aggregate_v2.0.xml").toURI().toURL());
        foms.add(new File("foms/NETN-FOM-v4.0/NETN-BASE.xml").toURI().toURL());
        foms.add(new File("foms/NETN-FOM-v4.0/NETN-MRM.xml").toURI().toURL());
        foms.add(new File("foms/TS-NETN-v4.0.xml").toURI().toURL());

        rtiAmbassador.connect(loggingFederateAmbassador, CallbackModel.HLA_IMMEDIATE);
        try {
            rtiAmbassador.createFederationExecution(FEDERATION_NAME, foms.toArray(new URL[foms.size()]));
        } catch (FederationExecutionAlreadyExists ignored) {
        }
        rtiAmbassador.joinFederationExecution(FEDERATE_NAME, FEDERATE_TYPE, FEDERATION_NAME);
    }
    
    public void setupDeclarations() throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError, InteractionClassNotDefined, SaveInProgress, RestoreInProgress, FederateServiceInvocationsAreBeingReportedViaMOM {
        aggregate = new Interaction.AggregateBuild(rtiAmbassador)
            .addRemoveSubunits(true)
            .build();

        aggregate.publish(rtiAmbassador);
        aggregate.subscribe(rtiAmbassador);
    }

    public void sendInteraction() throws FederateNotExecutionMember, NotConnected, InteractionClassNotPublished, InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress, RTIinternalError {
        aggregate.send(rtiAmbassador);
    }

    public void disconnectFromRti() throws FederateIsExecutionMember, CallNotAllowedFromWithinCallback, RTIinternalError, InvalidResignAction, OwnershipAcquisitionPending, FederateOwnsAttributes, FederateNotExecutionMember, NotConnected, FederationExecutionDoesNotExist {
        rtiAmbassador.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        try {
            rtiAmbassador.destroyFederationExecution(FEDERATION_NAME);
        } catch (FederatesCurrentlyJoined ignored) {
            log.trace("leave federation open for remaining federates");
        }
        rtiAmbassador.disconnect();
    }

    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) throws RTIinternalError, InconsistentFDD, ErrorReadingFDD, CouldNotOpenFDD, FederationExecutionAlreadyExists, NotConnected, MalformedURLException, CouldNotCreateLogicalTimeFactory, FederationExecutionDoesNotExist, SaveInProgress, RestoreInProgress, FederateAlreadyExecutionMember, CallNotAllowedFromWithinCallback, ConnectionFailed, InvalidLocalSettingsDesignator, UnsupportedCallbackModel, AlreadyConnected, FederateNameAlreadyInUse, NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, InteractionClassNotDefined, FederateIsExecutionMember, InvalidResignAction, OwnershipAcquisitionPending, FederateOwnsAttributes, FederatesCurrentlyJoined, InteractionClassNotPublished, InteractionParameterNotDefined, FederateServiceInvocationsAreBeingReportedViaMOM {
        int duration = 8000;
        int interval = 800;

        log.info("staring NETN reference federate A");
        App fed = new App(log);

        // get environment settings
        String name = System.getenv("federate");
        if (name != null) {
            fed.FEDERATE_NAME = name;
        }
        String durationString = System.getenv("duration");
        if (durationString != null) {
            duration = Integer.parseInt(durationString);
        }

        fed.connectToRti();

        fed.setupDeclarations();

        // TODO implement the test logic here
        while (duration > 0) {
            fed.sendInteraction();
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                return;
            }
            duration -= interval;
            log.info("working - remaining time {} ms", duration);
        }

        fed.disconnectFromRti();

        log.info("terminate NETN reference federate A");
    }

        // 6.13
        @Override
        public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
            log.warn("receiveInteraction not implemented");
        }
}
