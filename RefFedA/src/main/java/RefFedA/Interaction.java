package RefFedA;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandleValueMap;


/**
 * Container class for RTI interactions
 */
public class Interaction {

    private final InteractionClassHandle classHandle;   public InteractionClassHandle getClassHandle() { return classHandle; }
    private final ParameterHandleValueMap parameters;   public ParameterHandleValueMap getParameters() { return parameters; }
    
    private Interaction(InteractionClassHandle messageId, ParameterHandleValueMap parameters) {
        this.classHandle = messageId;
        this.parameters = parameters;
    }

    public void subscribe (RTIambassador rtiAambassador) throws FederateServiceInvocationsAreBeingReportedViaMOM, InteractionClassNotDefined, SaveInProgress, RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError  {
        rtiAambassador.subscribeInteractionClass(classHandle);
    }

    public void publish (RTIambassador rtiAambassador) throws InteractionClassNotDefined, SaveInProgress, RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError {
        rtiAambassador.publishInteractionClass(classHandle);
    }

    public void send (RTIambassador rtiAambassador) throws InteractionClassNotPublished, InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress, FederateNotExecutionMember, NotConnected, RTIinternalError {
        rtiAambassador.sendInteraction(classHandle, parameters, null);
    }

    /**
     * Generic Interaction Builder to create Interaction instances
     */
    public static class InteractionBuilder {

        protected RTIambassador rtiAambassador;
        protected InteractionClassHandle messageId;
        protected ParameterHandleValueMap parameters;
        protected EncoderFactory encoderFactory;

        public InteractionBuilder (RTIambassador rtiAmbassador, String messageName) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError {
            this.rtiAambassador = rtiAmbassador;
            this.messageId = rtiAmbassador.getInteractionClassHandle(messageName);
            parameters = rtiAmbassador.getParameterHandleValueMapFactory().create(1);
            encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        }
        
        public InteractionBuilder addParameter (String parameterName, byte[] value) throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            parameters.put(this.rtiAambassador.getParameterHandle(this.messageId, parameterName), value);
            return this;
        }

        public Interaction build() {
            Interaction aggregate = new Interaction(messageId, parameters);
            return aggregate;
        }
    }

    /**
     * Model specific  builder for Aggregate interactions
     */
    public static class AggregateBuild extends InteractionBuilder {

        public AggregateBuild(RTIambassador rtiAmbassador)
                throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError {
            super(rtiAmbassador, "MRM_Interaction.Request.Aggregate");
        }

        public AggregateBuild addRemoveSubunits(boolean b) throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            this.addParameter("RemoveSubunits",  encoderFactory.createHLAboolean(b).toByteArray());
            return this;
        }
    }

}