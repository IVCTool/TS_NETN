package RefFedA;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAbyte;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAoctet;
import hla.rti1516e.encoding.HLAunicodeChar;
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

import java.util.HashMap;
import java.util.Map;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;


/**
 * Container class for RTI interactions
 */
public class Interaction {

    public static Map<ParameterHandle, DataElement> knownDataElements = new HashMap<>();

    private final InteractionClassHandle classHandle;   public InteractionClassHandle getClassHandle() { return classHandle; }
    private final ParameterHandleValueMap parameters;   public ParameterHandleValueMap getParameters() { return parameters; }
    protected final HashMap<String,ParameterHandle> parameterHandles;
    
    private Interaction(InteractionClassHandle messageId, ParameterHandleValueMap parameters, HashMap<String,ParameterHandle> handles) {
        this.classHandle = messageId;
        this.parameters = parameters;
        this.parameterHandles = handles;
    }

    public void clear() {
        parameters.clear();
    }

    public void setParameter(String keyName, DataElement value) throws Exception {
        ParameterHandle handle = parameterHandles.get(keyName);
        if (handle == null) {
            throw new Exception("key not found");
        }
        parameters.put(handle, value.toByteArray());
        knownDataElements.put(handle, value);
    }

    public ByteWrapper getParameter(String keyName) throws Exception {
        ParameterHandle handle = parameterHandles.get(keyName);
        if (handle == null) {
            throw new Exception("key not found");
        }
        return parameters.getValueReference(handle);
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

        public static Map<InteractionClassHandle, InteractionBuilder> knownBuilder = new HashMap<>();

        protected RTIambassador rtiAambassador;
        protected InteractionClassHandle messageId;
        protected ParameterHandleValueMap parameters;
        protected EncoderFactory encoderFactory;
        protected HashMap<String,ParameterHandle> parameterHandles;

        public InteractionBuilder (RTIambassador rtiAmbassador, String messageName) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError {
            this.rtiAambassador = rtiAmbassador;
            this.messageId = rtiAmbassador.getInteractionClassHandle(messageName);
            this.parameters = rtiAmbassador.getParameterHandleValueMapFactory().create(1);
            this.encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
            this.parameterHandles = new HashMap<>();
            knownBuilder.put(this.messageId, this);
        }
        
        public InteractionBuilder addParameter (String parameterName, byte[] value) throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            ParameterHandle handle = this.rtiAambassador.getParameterHandle(this.messageId, parameterName);
            parameterHandles.put(parameterName, handle);
            parameters.put(handle, value);
            return this;
        }

        public Interaction build() {
            Interaction aggregate = new Interaction(messageId, parameters, parameterHandles);
            return aggregate;
        }

        public static Interaction parse (InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters) {
            InteractionBuilder builder = knownBuilder.get(interactionClass);
            if (builder != null) {
                return new Interaction(interactionClass,theParameters,null);
            }
            
            return null;
        }
    }


    public static class AggregateInteraction extends Interaction {

        public static String MessageId      = "MRM_Interaction.Request.Aggregate";
        public static String EventId        = "EventId";        // HLAbyte
        public static String Federate       = "Federate";       // HLAunicodeChar
        public static String AggregateUnit  = "AggregateUnit";  // HLAoctet
        public static String RemoveSubunits = "RemoveSubunits"; // HLAinteger32BE

        HLAbyte eventId;
        HLAunicodeChar federate;
        HLAoctet aggregateUnit;
        HLAinteger32BE removeSubunits;

        private AggregateInteraction(InteractionClassHandle messageId, ParameterHandleValueMap parameters, HashMap<String,ParameterHandle> handles) {
            super(messageId, parameters, handles);
        }

        public void setValueEventId(byte value) throws Exception {
            eventId.setValue(value);
            setParameter(EventId, eventId);
        }

        public void setValueFederate(short value) throws Exception {
            federate.setValue(value);
            setParameter(EventId, federate);
        }

        public void setValueAggregateUnit(byte value) throws Exception {
            aggregateUnit.setValue(value);
            setParameter(AggregateUnit, aggregateUnit);
        }

        public void setValueRemoveSubunits(int value) throws Exception {
            removeSubunits.setValue(value);
            setParameter(RemoveSubunits, removeSubunits);
        }
    }

    /**
     * Model specific  builder for Aggregate interactions
     */
    public static class AggregateBuilder extends InteractionBuilder {

        protected HLAbyte eventId = null;
        protected HLAunicodeChar federate = null;
        protected HLAoctet aggregateUnit = null;
        protected HLAinteger32BE removeSubunits = null;

        public AggregateBuilder(RTIambassador rtiAmbassador)
                throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, InvalidInteractionClassHandle, EncoderException {
            super(rtiAmbassador, "MRM_Interaction.Request.Aggregate");
        }

        public AggregateBuilder addEventId() throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            eventId = encoderFactory.createHLAbyte();
            addParameter(AggregateInteraction.EventId, eventId.toByteArray());
            return this;
        }

        public AggregateBuilder addFederate() throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            federate = encoderFactory.createHLAunicodeChar();
            addParameter(AggregateInteraction.Federate, federate.toByteArray());
            return this;
        }

        public AggregateBuilder addRemoveSubunits() throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            removeSubunits = encoderFactory.createHLAinteger32BE();
            addParameter(AggregateInteraction.RemoveSubunits, removeSubunits.toByteArray());
            return this;
        }

        public AggregateBuilder addAggregateUnit() throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            aggregateUnit = encoderFactory.createHLAoctet();
            addParameter(AggregateInteraction.AggregateUnit, aggregateUnit.toByteArray());
            return this;
        }

        public void setAggregateUnit(byte value) throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            ParameterHandle handle = this.rtiAambassador.getParameterHandle(this.messageId, "AggregateUnit");
            ByteWrapper wrapper = parameters.getValueReference(handle);
            wrapper.put(value);
        }

        public AggregateInteraction build () {
            AggregateInteraction aggregate = new AggregateInteraction(messageId, parameters, parameterHandles);
            aggregate.eventId = encoderFactory.createHLAbyte();
            aggregate.federate = encoderFactory.createHLAunicodeChar();
            aggregate.aggregateUnit = encoderFactory.createHLAoctet();
            aggregate.removeSubunits = encoderFactory.createHLAinteger32BE();

            return aggregate;
        }
    }

}