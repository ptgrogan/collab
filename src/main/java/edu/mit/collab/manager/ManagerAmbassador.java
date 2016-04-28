package edu.mit.collab.manager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.event.EventListenerList;

import org.apache.commons.math3.linear.RealVector;

import edu.mit.collab.util.HLAfloatVector;
import edu.mit.collab.util.HLAintegerMatrix;
import edu.mit.collab.util.HLAstringVector;
import edu.mit.collab.util.Utilities;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.FederateHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.MessageRetractionHandle;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactory;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.AlreadyConnected;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.AttributeNotOwned;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.ConnectionFailed;
import hla.rti1516e.exceptions.CouldNotCreateLogicalTimeFactory;
import hla.rti1516e.exceptions.CouldNotOpenFDD;
import hla.rti1516e.exceptions.ErrorReadingFDD;
import hla.rti1516e.exceptions.FederateAlreadyExecutionMember;
import hla.rti1516e.exceptions.FederateIsExecutionMember;
import hla.rti1516e.exceptions.FederateNameAlreadyInUse;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateOwnsAttributes;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.InconsistentFDD;
import hla.rti1516e.exceptions.InvalidLocalSettingsDesignator;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.InvalidResignAction;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.ObjectClassNotPublished;
import hla.rti1516e.exceptions.ObjectInstanceNotKnown;
import hla.rti1516e.exceptions.OwnershipAcquisitionPending;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.exceptions.UnsupportedCallbackModel;

/**
 * The federate ambassador interface to the RTI for the manager application.
 * This class handles all of the interactions with the RTI including setting up
 * the connection to a federation and receiving all messages from other
 * federates.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class ManagerAmbassador extends NullFederateAmbassador {
	private static enum DesignerAction {ADD, INPUT_UPDATE, 
		STATE_UPDATE, REMOVE};
	
	// the variables below define configuration strings for various
	// commands issued to the RTI ambassador
	private static final String federateType = "manager";
	private static final String managerClassName = "HLAobjectRoot.Manager";
	private static final String outputAttributeName = "Output";
	private static final String initialInputAttributeName = "InitialInput";
	private static final String targetOutputAttributeName = "TargetOutput";
	private static final String activeModelAttributeName = "ActiveModel";
	private static final String inputIndicesAttributeName = "InputIndices";
	private static final String outputIndicesAttributeName = "OutputIndices";
	private static final String inputLabelsAttributeName = "InputLabels";
	private static final String outputLabelsAttributeName = "OutputLabels";
	private static final String designerClassName = "HLAobjectRoot.Designer";
	private static final String inputAttributeName = "Input";
	private static final String indexAttributeName = "Index";
	private static final String readyAttributeName = "Ready";
	
	private transient String objectInstanceName; // set upon connection to RTI

	private final RTIambassador rtiAmbassador; // immutable
	private final EncoderFactory encoderFactory; // immutable

    private final Properties properties; // mutable
	private final HLAfloatVector initialInput, targetOutput, outputs; // mutable
	private final HLAintegerMatrix inputIndices, outputIndices; // mutable
	private final HLAunicodeString activeModel; // mutable
	private final HLAstringVector inputLabels, outputLabels; // mutable
	private final EventListenerList listenerList = new EventListenerList(); // mutable
	
	// synchronized mutable map to support multi-threaded application
	private final Map<ObjectInstanceHandle, Designer> designers = 
			Collections.synchronizedMap(
					new HashMap<ObjectInstanceHandle, Designer>());
	
	/**
	 * Instantiates a new manager ambassador.
	 *
	 * @throws RTIinternalError the RTI internal error
	 */
	public ManagerAmbassador() throws RTIinternalError {	
        properties = new Properties();
        try {
          InputStream in = getClass().getClassLoader().getResourceAsStream(
              Utilities.PROPERTIES_PATH);
          properties.load(in);
          in.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        String rtiName = properties.getProperty("rtiName", null);
      
		// create the RTI factory and store ambassador and encoder objects
	    RtiFactory rtiFactory = null;
	    if(rtiName == null) {
	        rtiFactory = RtiFactoryFactory.getRtiFactory();
	    } else {
	        rtiFactory = RtiFactoryFactory.getRtiFactory(rtiName);
	    }
		rtiAmbassador = rtiFactory.getRtiAmbassador();
		encoderFactory = rtiFactory.getEncoderFactory();
		
		// create hla-compatible data elements for encoding/decoding values
		initialInput = new HLAfloatVector(encoderFactory);
		targetOutput = new HLAfloatVector(encoderFactory);
		outputs = new HLAfloatVector(encoderFactory);
		activeModel = encoderFactory.createHLAunicodeString();
		inputIndices = new HLAintegerMatrix(encoderFactory);
		outputIndices = new HLAintegerMatrix(encoderFactory);
		inputLabels = new HLAstringVector(encoderFactory);
		outputLabels = new HLAstringVector(encoderFactory);
	}
	
	/**
	 * Adds the designer listener.
	 *
	 * @param listener the listener
	 */
	public void addDesignerListener(DesignerListener listener) {
		// add listener to list
		listenerList.add(DesignerListener.class, listener);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#discoverObjectInstance(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.ObjectClassHandle, java.lang.String)
	 */
	public void discoverObjectInstance(ObjectInstanceHandle theObject,
			ObjectClassHandle theObjectClass, 
			String objectName) {
		// re-direct method to single method signature
		discoverObjectInstance(theObject, theObjectClass, objectName, null);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#discoverObjectInstance(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.ObjectClassHandle, java.lang.String)
	 */
	public void discoverObjectInstance(ObjectInstanceHandle theObject,
			ObjectClassHandle theObjectClass,
			String objectName,
			FederateHandle producingFederate) {
		// this method is called by the RTI when a new object is "discovered"
		// in this case, we are only expecting designers which should be added
		// to the list of discovered designers and request attribute updates
		try {
			// check if object is a designer (shouldn't be anything else!)
			if(theObjectClass.equals(
					rtiAmbassador.getObjectClassHandle(designerClassName))) {
				// create new designer object
				Designer design = new Designer(objectName);
				
				// add object class handle and designer object to 
				// thread-safe map using a synchronized block
				synchronized(designers) {
					designers.put(theObject, design);
				}
				
				// create a new attribute handle set to request updates of the
				// designer's attributes
				AttributeHandleSet attributes = rtiAmbassador
						.getAttributeHandleSetFactory().create();
				// add index attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(designerClassName),
						indexAttributeName));
				// add ready attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(designerClassName),
						readyAttributeName));
				// add input attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(designerClassName),
						inputAttributeName));
				// issue request attribute value update service call
				rtiAmbassador.requestAttributeValueUpdate(theObject, 
						attributes, new byte[0]);
			}
		} catch (Exception ex) {
			// in the case of an exception (from the request attribute value
			// update call), print stack trace and show error message
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "An exception of type " + 
					ex.getMessage() + " occurred while discovering an object. " +
					"See stack trace for more information.", 
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Fires a designer event corresponding to an observed action.
	 *
	 * @param action the action
	 * @param event the event
	 */
	private void fireDesignerEvent(DesignerAction action, 
			DesignerEvent event) {
		// get the list of designer listeners
		DesignerListener[] listeners = listenerList.getListeners(
				DesignerListener.class);
		
		// for each listener, notify using the appropriate method
		for(int i = 0; i < listeners.length; i++) {
			switch(action) {
			case ADD:
				listeners[i].designerAdded(event);
				break;
			case INPUT_UPDATE:
				listeners[i].designerInputModified(event);
				break;
			case STATE_UPDATE:
				listeners[i].designerStateModified(event);
				break;
			case REMOVE:
				listeners[i].designerRemoved(event);
			}
		}
	}
	
	/**
	 * Gets the instance name issued by the RTI.
	 *
	 * @return the instance name
	 */
	public String getInstanceName() {
		return objectInstanceName;
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#provideAttributeValueUpdate(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.AttributeHandleSet, byte[])
	 */
	public void provideAttributeValueUpdate(ObjectInstanceHandle theObject,
			AttributeHandleSet theAttributes, 
			byte[] userSuppliedTag) {
		// this method is called by the RTI when this object attribute values
		// owned by this federate are requested by another federate. in this
		// case, this corresponds to a designer requesting attribute values
		// of the manager
		try {
			// check to make sure that the object requested is this manager
			if(theObject.equals(rtiAmbassador.getObjectInstanceHandle(
					objectInstanceName))) {
				// create an attribute handle value map to store data
				AttributeHandleValueMap attributes = rtiAmbassador.
						getAttributeHandleValueMapFactory().create(6);

				// if the initial input is requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						initialInputAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							initialInputAttributeName), 
							initialInput.toByteArray());
				}
				
				// if the target output is requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						targetOutputAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							targetOutputAttributeName), 
							targetOutput.toByteArray());
				}

				// if the outputs are requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						outputAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							outputAttributeName), 
							outputs.toByteArray());
				}

				// if the active model is requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						activeModelAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							activeModelAttributeName), 
							activeModel.toByteArray());
				}

				// if the input indices are requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						inputIndicesAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							inputIndicesAttributeName), 
							inputIndices.toByteArray());
				}

				// if the output indices are requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						outputIndicesAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							outputIndicesAttributeName), 
							outputIndices.toByteArray());
				}

				// if the input labels are requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						inputLabelsAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							inputLabelsAttributeName), 
							inputLabels.toByteArray());
				}

				// if the output labels are requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						outputLabelsAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(managerClassName), 
							outputLabelsAttributeName), 
							outputLabels.toByteArray());
				}
				
				// use the rti's update attribute value service to issue updates
				rtiAmbassador.updateAttributeValues(
						rtiAmbassador.getObjectInstanceHandle(objectInstanceName), 
						attributes, new byte[0]);
			}
		}  catch (Exception ex) {
			// in the case of an exception (from the update attribute value
			// call), print stack trace and show error message
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "An exception of type " + 
					ex.getMessage() + " occurred while providing attribute " +
					"updates. See stack trace for more information.", 
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Configures the published object class attributes.
	 *
	 * @throws FederateNotExecutionMember the federate not execution member
	 * @throws NotConnected the not connected
	 * @throws NameNotFound the name not found
	 * @throws InvalidObjectClassHandle the invalid object class handle
	 * @throws RTIinternalError the RTI internal error
	 * @throws AttributeNotDefined the attribute not defined
	 * @throws ObjectClassNotDefined the object class not defined
	 * @throws SaveInProgress the save in progress
	 * @throws RestoreInProgress the restore in progress
	 */
	private void publish() 
			throws FederateNotExecutionMember, NotConnected, 
			NameNotFound, InvalidObjectClassHandle, RTIinternalError, 
			AttributeNotDefined, ObjectClassNotDefined, SaveInProgress, 
			RestoreInProgress {
		// create a new attribute handle set to store attributes
		AttributeHandleSet attributeHandleSet = rtiAmbassador
				.getAttributeHandleSetFactory().create();
		// add the outputs to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputAttributeName));
		// add the initial input to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				initialInputAttributeName));
		// add the target output to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				targetOutputAttributeName));
		// add the active model to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				activeModelAttributeName));
		// add the input indices to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				inputIndicesAttributeName));
		// add the output indices to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputIndicesAttributeName));
		// add the input labels to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				inputLabelsAttributeName));
		// add the output labels to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputLabelsAttributeName));
		// use the RTI service to publish object class attributes
		rtiAmbassador.publishObjectClassAttributes(
				rtiAmbassador.getObjectClassHandle(managerClassName),
				attributeHandleSet);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#reflectAttributeValues(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.AttributeHandleValueMap, byte[], hla.rti1516e.OrderType, hla.rti1516e.TransportationTypeHandle, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.MessageRetractionHandle, hla.rti1516e.FederateAmbassador.SupplementalReflectInfo)
	 */
	public void reflectAttributeValues(ObjectInstanceHandle theObject,
			AttributeHandleValueMap theAttributes,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			TransportationTypeHandle theTransport,
			LogicalTime theTime,
			OrderType receivedOrdering,
			MessageRetractionHandle retractionHandle,
			SupplementalReflectInfo reflectInfo) {
		// this method is called by the RTI when remote objects update their
		// values. this method must update any local representations of the 
		// remote objects to reflect the processed updates
		try {
			// check whether the object has been previously discovered
			Designer designer = null;
			synchronized(designers) {
				designer = designers.get(theObject);
			}
			
			if(designer != null) {
				// get the data corresponding to the index attribute
				ByteWrapper wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										designerClassName), 
								indexAttributeName));
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAinteger32BE index = encoderFactory.createHLAinteger32BE();
					index.decode(wrapper);
					
					// designers can only update their index once; process
					// update only if the current index is < 0 (uninitialized)
					if(designer.getIndex() < 0) {
						// update index value and fire event to notify
						// listeners that a designer has been "added" 
						// now that its index is defined
						designer.setIndex(index.getValue());
						fireDesignerEvent(DesignerAction.ADD, 
								new DesignerEvent(this, designer));
					}
				}
				
				// get the data corresponding to the input attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										designerClassName), 
								inputAttributeName));
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAfloatVector input = new HLAfloatVector(encoderFactory);
					input.decode(wrapper);
					
					// update input value and fire event to notify listeners
					designer.setInputVector(input.getValue());
					fireDesignerEvent(DesignerAction.INPUT_UPDATE, 
							new DesignerEvent(this, designer));
				}
				
				// get the data corresponding to the ready attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										designerClassName), 
								readyAttributeName));
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAboolean ready = encoderFactory.createHLAboolean();
					ready.decode(wrapper);
					
					// update ready value and fire event to notify listeners
					designer.setReady(ready.getValue());
					fireDesignerEvent(DesignerAction.STATE_UPDATE, 
							new DesignerEvent(this, designer));
				}
			} 
		} catch (Exception ex) {
			// in the case of an exception (from the various RTI calls), 
			// print stack trace and show error message
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "An exception of type " + 
					ex.getMessage() + " occurred while decoding an " +
					"attribute update. See stack trace for more information.", 
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#reflectAttributeValues(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.AttributeHandleValueMap, byte[], hla.rti1516e.OrderType, hla.rti1516e.TransportationTypeHandle, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.FederateAmbassador.SupplementalReflectInfo)
	 */
	public void reflectAttributeValues(ObjectInstanceHandle theObject,
			AttributeHandleValueMap theAttributes,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			TransportationTypeHandle theTransport,
			LogicalTime theTime,
			OrderType receivedOrdering,
			SupplementalReflectInfo reflectInfo) {
		// re-direct method to single method signature
		reflectAttributeValues(theObject, theAttributes, userSuppliedTag, 
				sentOrdering, theTransport, theTime, receivedOrdering, null, 
				reflectInfo);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#reflectAttributeValues(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.AttributeHandleValueMap, byte[], hla.rti1516e.OrderType, hla.rti1516e.TransportationTypeHandle, hla.rti1516e.FederateAmbassador.SupplementalReflectInfo)
	 */
	public void reflectAttributeValues(ObjectInstanceHandle theObject,
			AttributeHandleValueMap theAttributes,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			TransportationTypeHandle theTransport,
			SupplementalReflectInfo reflectInfo) {
		// re-direct method to single method signature
		reflectAttributeValues(theObject, theAttributes, userSuppliedTag, 
				sentOrdering, theTransport, null, null, reflectInfo);
	}

	/**
	 * Removes the designer listener.
	 *
	 * @param listener the listener
	 */
	public void removeDesignerListener(DesignerListener listener) {
		// remove the listener from the list
		listenerList.remove(DesignerListener.class, listener);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#removeObjectInstance(hla.rti1516e.ObjectInstanceHandle, byte[], hla.rti1516e.OrderType, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.MessageRetractionHandle, hla.rti1516e.FederateAmbassador.SupplementalRemoveInfo)
	 */
	public void removeObjectInstance(ObjectInstanceHandle theObject,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			LogicalTime theTime,
			OrderType receivedOrdering,
			MessageRetractionHandle retractionHandle,
			SupplementalRemoveInfo removeInfo) {
		// try to remove designer from the designer map
		Designer designer = null;
		synchronized(designers) {
			designer = designers.remove(theObject);
		}
		if(designer != null) {
			// notify listeners that designer has been removed
			fireDesignerEvent(DesignerAction.REMOVE, 
					new DesignerEvent(this, designer));
		}
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#removeObjectInstance(hla.rti1516e.ObjectInstanceHandle, byte[], hla.rti1516e.OrderType, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.FederateAmbassador.SupplementalRemoveInfo)
	 */
	public void removeObjectInstance(ObjectInstanceHandle theObject,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			LogicalTime theTime,
			OrderType receivedOrdering,
			SupplementalRemoveInfo removeInfo) {
		// re-direct method to single method signature
		removeObjectInstance(theObject, userSuppliedTag, sentOrdering, 
				theTime, receivedOrdering, null, removeInfo);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#removeObjectInstance(hla.rti1516e.ObjectInstanceHandle, byte[], hla.rti1516e.OrderType, hla.rti1516e.FederateAmbassador.SupplementalRemoveInfo)
	 */
	public void removeObjectInstance(ObjectInstanceHandle theObject,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			SupplementalRemoveInfo removeInfo) {
		// re-direct method to single method signature
		removeObjectInstance(theObject, userSuppliedTag, sentOrdering, 
				null, null, null, removeInfo);
	}
	
	/**
	 * Shuts down the application. Resigns from the federation execution, 
	 * attempts to destroy federation execution, and disconnects from the RTI.
	 *
	 * @throws InvalidResignAction the invalid resign action
	 * @throws OwnershipAcquisitionPending the ownership acquisition pending
	 * @throws FederateOwnsAttributes the federate owns attributes
	 * @throws CallNotAllowedFromWithinCallback the call not allowed from within callback
	 * @throws RTIinternalError the RTI internal error
	 * @throws FederateIsExecutionMember the federate is execution member
	 */
	public void shutDown() 
			throws InvalidResignAction, OwnershipAcquisitionPending, 
			FederateOwnsAttributes, CallNotAllowedFromWithinCallback, 
			RTIinternalError, FederateIsExecutionMember {
		
		// try to resign from the federation execution; ignore
		// exceptions if already resigned or not connected
		try {
			rtiAmbassador.resignFederationExecution(
					ResignAction.DELETE_OBJECTS_THEN_DIVEST);
		} catch (FederateNotExecutionMember ignored) {
		} catch (NotConnected ignored) { }
		
		// once resigned, try to destroy federation; ignore
		// exceptions if other federates still joined, federation
		// already destroyed, or not connected
		try {
			rtiAmbassador.destroyFederationExecution(
			    properties.getProperty("federationName", "collab"));
		} catch (FederatesCurrentlyJoined ignored) {
		} catch (FederationExecutionDoesNotExist ignored) {
		} catch (NotConnected ignored) {
		}
		
		// disconnect from the rti
		rtiAmbassador.disconnect();
	}
	
	/**
	 * Starts up the application. Connects to the RTI, creates/joins the 
	 * federation execution, publishes and subscribes to appropriate object
	 * attributes, and registers the manager object class instance.
	 *
	 * @throws ConnectionFailed the connection failed
	 * @throws InvalidLocalSettingsDesignator the invalid local settings designator
	 * @throws UnsupportedCallbackModel the unsupported callback model
	 * @throws CallNotAllowedFromWithinCallback the call not allowed from within callback
	 * @throws RTIinternalError the RTI internal error
	 * @throws InconsistentFDD the inconsistent FDD
	 * @throws ErrorReadingFDD the error reading FDD
	 * @throws CouldNotOpenFDD the could not open FDD
	 * @throws NotConnected the not connected
	 * @throws MalformedURLException the malformed url exception
	 * @throws CouldNotCreateLogicalTimeFactory the could not create logical time factory
	 * @throws FederateNameAlreadyInUse the federate name already in use
	 * @throws FederationExecutionDoesNotExist the federation execution does not exist
	 * @throws SaveInProgress the save in progress
	 * @throws RestoreInProgress the restore in progress
	 * @throws FederateNotExecutionMember the federate not execution member
	 * @throws NameNotFound the name not found
	 * @throws InvalidObjectClassHandle the invalid object class handle
	 * @throws AttributeNotDefined the attribute not defined
	 * @throws ObjectClassNotDefined the object class not defined
	 * @throws ObjectClassNotPublished 
	 * @throws ObjectInstanceNotKnown 
	 */
	public void startUp() 
			throws ConnectionFailed, InvalidLocalSettingsDesignator, 
			UnsupportedCallbackModel, CallNotAllowedFromWithinCallback, 
			RTIinternalError, InconsistentFDD, ErrorReadingFDD, 
			CouldNotOpenFDD, NotConnected, MalformedURLException, 
			CouldNotCreateLogicalTimeFactory, FederateNameAlreadyInUse, 
			FederationExecutionDoesNotExist, SaveInProgress, 
			RestoreInProgress, FederateNotExecutionMember, NameNotFound, 
			InvalidObjectClassHandle, AttributeNotDefined, 
			ObjectClassNotDefined, ObjectInstanceNotKnown, 
			ObjectClassNotPublished {
		// try to connect to the RTI; ignore if already connected
		try {
			// use the HLA_Evoked model to require explicit callbacks
			rtiAmbassador.connect(this, CallbackModel.HLA_IMMEDIATE);
		} catch(AlreadyConnected ignored) { }

		// try to create the federation execution using the FOM file;
		// ignore if already exists
		try {
			rtiAmbassador.createFederationExecution(
			    properties.getProperty("federationName", "collab"), 
			    getClass().getClassLoader().getResource(
			        properties.getProperty("fomPath", "resources/collab.xml")));
		} catch(FederationExecutionAlreadyExists ignored) { }
		
		// try to join the federation execution; ignore if already joined
		try {
			rtiAmbassador.joinFederationExecution(
					federateType, properties.getProperty("federationName", "collab"), 
					new URL[0]);
		} catch(FederateAlreadyExecutionMember ignored) { }
		
		// publish and subscribe to object class attributes
		publish();
		subscribe();
		
		// register the object instance name
		objectInstanceName = rtiAmbassador.getObjectInstanceName(
				rtiAmbassador.registerObjectInstance(
						rtiAmbassador.getObjectClassHandle(
								managerClassName)));
	}
	
	/**
	 * Subscribe to designer object class attributes.
	 *
	 * @throws FederateNotExecutionMember the federate not execution member
	 * @throws NotConnected the not connected
	 * @throws AttributeNotDefined the attribute not defined
	 * @throws ObjectClassNotDefined the object class not defined
	 * @throws SaveInProgress the save in progress
	 * @throws RestoreInProgress the restore in progress
	 * @throws RTIinternalError the RTI internal error
	 * @throws NameNotFound the name not found
	 * @throws InvalidObjectClassHandle the invalid object class handle
	 */
	private void subscribe() 
			throws FederateNotExecutionMember, NotConnected, 
			AttributeNotDefined, ObjectClassNotDefined, 
			SaveInProgress, RestoreInProgress, RTIinternalError, 
			NameNotFound, InvalidObjectClassHandle {
		// create an attribute handle set
		AttributeHandleSet attributeHandleSet = rtiAmbassador
				.getAttributeHandleSetFactory().create();
		// add the input attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				inputAttributeName));
		// add the index attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				indexAttributeName));
		// add the ready attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				readyAttributeName));
		// use the RTI service to subscribe to the defined attributes
		rtiAmbassador.subscribeObjectClassAttributes(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				attributeHandleSet);
	}
	
	/**
	 * Sends updates for a modified system model.
	 *
	 * @param systemModel the system model
	 * @throws FederateNotExecutionMember the federate not execution member
	 * @throws NotConnected the not connected
	 * @throws NameNotFound the name not found
	 * @throws InvalidObjectClassHandle the invalid object class handle
	 * @throws RTIinternalError the RTI internal error
	 * @throws EncoderException the encoder exception
	 * @throws AttributeNotOwned the attribute not owned
	 * @throws AttributeNotDefined the attribute not defined
	 * @throws ObjectInstanceNotKnown the object instance not known
	 * @throws SaveInProgress the save in progress
	 * @throws RestoreInProgress the restore in progress
	 */
	public void updateModelAttributes(Experiment experiment) 
			throws FederateNotExecutionMember, NotConnected, NameNotFound, 
			InvalidObjectClassHandle, RTIinternalError, EncoderException, 
			AttributeNotOwned, AttributeNotDefined, ObjectInstanceNotKnown, 
			SaveInProgress, RestoreInProgress {
		// get the active model from the experiment
		SystemModel model = experiment==null ? 
				null : experiment.getActiveModel();
		// create an attribute handle value map to store data
		AttributeHandleValueMap attributes = 
				rtiAmbassador.getAttributeHandleValueMapFactory().create(6);
		
		// initial input
		if(model != null) {
			// if model is not null, set initial input
			initialInput.setValue(model.getInitialVector());
		}
		// add initial input to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				initialInputAttributeName), 
				initialInput.toByteArray());
		
		// target output
		if(model != null) {
			// if model is not null, set target output
			targetOutput.setValue(model.getTargetVector());
		}
		// add target output to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				targetOutputAttributeName), 
				targetOutput.toByteArray());
		
		// output
		if(model != null) {
			// if model is not null, set output to output of initial inputs
			outputs.setValue(model.getOutputVector(model.getInitialVector()));
		}
		// add output to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputAttributeName), 
				outputs.toByteArray());
		
		// input labels
		if(model != null) {
			// if model is not null, set input labels
			inputLabels.setValue(model.getInputLabels());
		}
		// add input labels to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				inputLabelsAttributeName), 
				inputLabels.toByteArray());
		
		// output labels
		if(model != null) {
			// if model is not null, set output labels
			outputLabels.setValue(model.getOutputLabels());
		}
		// add output labels to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputLabelsAttributeName), 
				outputLabels.toByteArray());
		
		// active model
		if(experiment == null) {
			// if experiment is null, set active model to empty string
			activeModel.setValue("");
		} else if(experiment.isReady()) {
			// if experiment is ready, set active model string
			activeModel.setValue("Ready...");
		} else if(experiment.isComplete()) {
			// if experiment is complete, set active model string
			activeModel.setValue("Complete!");
		} else {
			// otherwise, set active model to model name
			activeModel.setValue(model.getName());
		}
		// add active model to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				activeModelAttributeName), 
				activeModel.toByteArray());
		
		// input indices
		if(experiment==null) {
			// if experiment is null, use trivial input indices
			inputIndices.setValue(new int[0][0]);
		} else if(model == null) {
			// if model is null, use nearly-trivial input indices
			inputIndices.setValue(new int[experiment.getNumberDesigners()][0]);
		} else {
			// otherwise use model input indices
			inputIndices.setValue(model.getInputIndices());
		}
		// add input indices to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				inputIndicesAttributeName), 
				inputIndices.toByteArray());
		
		// output indices
		if(experiment==null) {
			// if experiment is null, use trivial output indices
			outputIndices.setValue(new int[0][0]);
		} else if(model == null) {
			// if model is null, use nearly-trivial output indices
			outputIndices.setValue(new int[experiment.getNumberDesigners()][0]);
		} else {
			// otherwise use model output indices
			outputIndices.setValue(model.getOutputIndices());
		}
		// add output indices to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputIndicesAttributeName), 
				outputIndices.toByteArray());
		
		// use RTI service to update attribute values using map
		rtiAmbassador.updateAttributeValues(
				rtiAmbassador.getObjectInstanceHandle(objectInstanceName), 
				attributes, new byte[0]);
	}
	
	/**
	 * Updates the output attribute.
	 *
	 * @param outputVector the output vector
	 * @throws FederateNotExecutionMember the federate not execution member
	 * @throws NotConnected the not connected
	 * @throws NameNotFound the name not found
	 * @throws InvalidObjectClassHandle the invalid object class handle
	 * @throws RTIinternalError the RTI internal error
	 * @throws EncoderException the encoder exception
	 * @throws AttributeNotOwned the attribute not owned
	 * @throws AttributeNotDefined the attribute not defined
	 * @throws ObjectInstanceNotKnown the object instance not known
	 * @throws SaveInProgress the save in progress
	 * @throws RestoreInProgress the restore in progress
	 */
	public void updateOutputAttributes(RealVector outputVector) 
			throws FederateNotExecutionMember, NotConnected, NameNotFound, 
			InvalidObjectClassHandle, RTIinternalError, EncoderException, 
			AttributeNotOwned, AttributeNotDefined, ObjectInstanceNotKnown, 
			SaveInProgress, RestoreInProgress {
		// create an attribute handle value map to store data
		AttributeHandleValueMap attributes = 
				rtiAmbassador.getAttributeHandleValueMapFactory().create(1);
		// set HLA data element to output value
		outputs.setValue(outputVector);
		// add outputs to map
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputAttributeName), 
				outputs.toByteArray());
		// use HLA service to update attribute values using map
		rtiAmbassador.updateAttributeValues(
				rtiAmbassador.getObjectInstanceHandle(objectInstanceName), 
				attributes, new byte[0]);
	}
}
