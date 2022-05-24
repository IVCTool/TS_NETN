package RefFedA;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;


public class Interaction {
    private final InteractionClassHandle messageId;
    private final ParameterHandle removeSubunits;
    
    private Interaction(InteractionClassHandle messageId, ParameterHandle removeSubunits) {
        this.messageId = messageId;
        this.removeSubunits = removeSubunits;
    }

    public InteractionClassHandle getClassHandle() {
        return messageId;
    }

    
    public static class InteractionBuilder {
        private RTIambassador rtiAambassador;
        private InteractionClassHandle messageId;
        private ParameterHandle removeSubunits;

        public InteractionBuilder (RTIambassador rtiAmbassador, String messageName) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError {
            this.rtiAambassador = rtiAmbassador;
            this.messageId = rtiAmbassador.getInteractionClassHandle(messageName);
        }
        
        public InteractionBuilder removeSubunits (String theName) throws NameNotFound, InvalidInteractionClassHandle, FederateNotExecutionMember, NotConnected, RTIinternalError {
            this.removeSubunits = rtiAambassador.getParameterHandle(this.messageId, theName);
            return this;
        }
        
        public Interaction build() {
            Interaction aggregate = new Interaction(messageId, removeSubunits);
            return aggregate;
        }
    }

}