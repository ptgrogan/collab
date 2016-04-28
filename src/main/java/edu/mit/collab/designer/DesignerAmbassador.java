package edu.mit.collab.designer;

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

import com.google.gson.Gson;

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
 * The federate ambassador interface to the RTI for the designer application.
 * This class handles all of the interactions with the RTI including setting up
 * the connection to a federation and receiving all messages from other
 * federates.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class DesignerAmbassador extends NullFederateAmbassador {
	private static enum ManagerAction {ADD, MODEL_UPDATE, 
		OUTPUT_UPDATE, REMOVE};
		
	// the variables below define configuration strings for various
	// commands issued to the RTI ambassador
	private static final String federateType = "designer";
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
	
	private transient String objectInstanceName;  // set upon connection to RTI

	private final RTIambassador rtiAmbassador; // immutable
	private final EncoderFactory encoderFactory; // immutable

    private final Properties properties; // mutable
	private final HLAfloatVector input; // mutable
	private final HLAinteger32BE index; // mutable
	private final HLAboolean ready; // mutable
	private final EventListenerList listenerList = new EventListenerList(); // mutable

	// synchronized mutable map to support multi-threaded application
	private final Map<ObjectInstanceHandle, Manager> managers = 
			Collections.synchronizedMap(
					new HashMap<ObjectInstanceHandle, Manager>());
	
	/**
	 * Instantiates a new designer ambassador.
	 *
	 * @param designerIndex the designer index
	 * @throws RTIinternalError the RTI internal error
	 */
	public DesignerAmbassador(int designerIndex) throws RTIinternalError {
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
        RtiFactory rtiFactory;
        if(rtiName == null) {
            rtiFactory = RtiFactoryFactory.getRtiFactory();
        } else {
            rtiFactory = RtiFactoryFactory.getRtiFactory(rtiName);
        }
		rtiAmbassador = rtiFactory.getRtiAmbassador();
		encoderFactory = rtiFactory.getEncoderFactory();
		
		// create hla-compatible data elements for encoding/decoding values
		input = new HLAfloatVector(encoderFactory);
		index = encoderFactory.createHLAinteger32BE(designerIndex);
		ready = encoderFactory.createHLAboolean();
	}
	
	/**
	 * Adds the manager listener.
	 *
	 * @param listener the listener
	 */
	public void addManagerListener(ManagerListener listener) {
		// add listener to list
		listenerList.add(ManagerListener.class, listener);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#discoverObjectInstance(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.ObjectClassHandle, java.lang.String)
	 */
	@Override
	public void discoverObjectInstance(ObjectInstanceHandle theObject,
			ObjectClassHandle theObjectClass, 
			String objectName) {
		// re-direct method to single method signature
		discoverObjectInstance(theObject, theObjectClass, objectName, null);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#discoverObjectInstance(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.ObjectClassHandle, java.lang.String)
	 */
	@Override
	public void discoverObjectInstance(ObjectInstanceHandle theObject,
			ObjectClassHandle theObjectClass,
			String objectName,
			FederateHandle producingFederate) {
		// this method is called by the RTI when a new object is "discovered"
		// in this case, we are only expecting managers which should be added
		// to the list of discovered managers and request attribute updates
		try {
			// check if object is a designer (shouldn't be anything else!)
			if(theObjectClass.equals(
					rtiAmbassador.getObjectClassHandle(managerClassName))) {
				// create new object model
				Manager manager = new Manager(objectName);
				
				// add object class handle and manager object to 
				// thread-safe map using a synchronized block
				synchronized(managers) {
					managers.put(theObject, manager);
				}

				// create a new attribute handle set to request updates of the
				// manager's attributes
				AttributeHandleSet attributes = rtiAmbassador
						.getAttributeHandleSetFactory().create();
				// add initial input attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						initialInputAttributeName));
				// add target output attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						targetOutputAttributeName));
				// add output attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						outputAttributeName));
				// add active model attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						activeModelAttributeName));
				// add input indices attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						inputIndicesAttributeName));
				// add output indices attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						outputIndicesAttributeName));
				// add input labels attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						inputLabelsAttributeName));
				// add output labels attribute
				attributes.add(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(managerClassName), 
						outputLabelsAttributeName));
				// issue request attribute value update service call
				rtiAmbassador.requestAttributeValueUpdate(theObject, 
						attributes, new byte[0]);
				
				// notify listeners that a manager has been discovered
				fireManagerEvent(ManagerAction.ADD, 
						new ManagerEvent(this, manager));
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
	 * Fires a manager event corresponding to an observed action.
	 *
	 * @param action the action
	 * @param event the event
	 */
	private void fireManagerEvent(ManagerAction action, ManagerEvent event) {
		// get the list of manager listeners
		ManagerListener[] listeners = listenerList.getListeners(
				ManagerListener.class);

		// for each listener, notify using the appropriate method
		for(int i = 0; i < listeners.length; i++) {
			switch(action) {
			case ADD:
				listeners[i].managerAdded(event);
				break;
			case MODEL_UPDATE:
				listeners[i].managerModelModified(event);
				break;
			case OUTPUT_UPDATE:
				listeners[i].managerOutputModified(event);
				break;
			case REMOVE:
				listeners[i].managerRemoved(event);
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
						getAttributeHandleValueMapFactory().create(3);
				
				// if the input is requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(designerClassName), 
						inputAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(designerClassName), 
							inputAttributeName), input.toByteArray());
				}
				
				// if the index is requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(designerClassName), 
						indexAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(designerClassName), 
							indexAttributeName), index.toByteArray());
				}
				
				// if the ready state is requested, add it to the map
				if(theAttributes.contains(rtiAmbassador.getAttributeHandle(
						rtiAmbassador.getObjectClassHandle(designerClassName), 
						readyAttributeName))) {
					attributes.put(rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(designerClassName),
							readyAttributeName), ready.toByteArray());
				}
				
				// use the rti's update attribute value service to issue updates
				rtiAmbassador.updateAttributeValues(
						rtiAmbassador.getObjectInstanceHandle(objectInstanceName),
						attributes, new byte[0]);
			}
		} catch (Exception ex) {
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
			throws FederateNotExecutionMember, NotConnected, NameNotFound, 
			InvalidObjectClassHandle, RTIinternalError, AttributeNotDefined, 
			ObjectClassNotDefined, SaveInProgress, RestoreInProgress {
		// create a new attribute handle set to store attributes
		AttributeHandleSet attributeHandleSet = rtiAmbassador.
				getAttributeHandleSetFactory().create();
		// add the input to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				inputAttributeName));
		// add the index to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				indexAttributeName));
		// add the ready state to the set
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				readyAttributeName));
		// use the RTI service to publish object class attributes
		rtiAmbassador.publishObjectClassAttributes(
				rtiAmbassador.getObjectClassHandle(designerClassName),
				attributeHandleSet);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#reflectAttributeValues(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.AttributeHandleValueMap, byte[], hla.rti1516e.OrderType, hla.rti1516e.TransportationTypeHandle, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.MessageRetractionHandle, hla.rti1516e.FederateAmbassador.SupplementalReflectInfo)
	 */
	@Override
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
		
		// create a gson object to help with log message formatting
		Gson gson = new Gson();
		
		try {
			// check whether the object has been previously discovered
			Manager manager = null;
			synchronized(managers) {
				manager = managers.get(theObject);
			}
			
			// if manager has not been discovered, simply return
			if(manager == null) {
				return;
			}
			
			// get the data corresponding to the active model attribute
			ByteWrapper wrapper = theAttributes.getValueReference(
					rtiAmbassador.getAttributeHandle(
							rtiAmbassador.getObjectClassHandle(
									managerClassName), 
									activeModelAttributeName));
			
			if(wrapper != null) {
				// active model has changed -- start a complete model update
				
				// decode into an HLA data element
				HLAunicodeString string = 
						encoderFactory.createHLAunicodeString();
				string.decode(wrapper);
				System.out.println("Designer " + (index.getValue()+1) +
						" Log: setting manager active model to " + 
						string.getValue());

				// update manager object
				manager.setActiveModel(string.getValue());
				
				// get the data corresponding to the initial input attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
								initialInputAttributeName));
				
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAfloatVector vector = new HLAfloatVector(encoderFactory);
					vector.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) +
							" Log: setting manager initial input to " + 
							vector.getValue());
					
					// update manager object
					manager.setInitialInput(vector.getValue());
				}
				
				// get the data corresponding to the target output attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
										targetOutputAttributeName));
				
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAfloatVector vector = new HLAfloatVector(encoderFactory);
					vector.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) +
							" Log: setting manager target output to " + 
							vector.getValue());

					// update manager object
					manager.setTargetOutput(vector.getValue());
				}
				

				// get the data corresponding to the output attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
										outputAttributeName));
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAfloatVector vector = 
							new HLAfloatVector(encoderFactory);
					vector.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) +
							" Log: setting manager output to " + 
							vector.getValue());

					// update manager object
					manager.setOutput(vector.getValue());
				}

				// get the data corresponding to the input indices attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName),
										inputIndicesAttributeName));
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAintegerMatrix matrix = 
							new HLAintegerMatrix(encoderFactory);
					matrix.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) +
							" Log: setting manager input indices to " + 
							gson.toJson(matrix.getValue()));

					// update manager object
					manager.setInputIndices(matrix.getValue());
				}

				// get the data corresponding to the output indices attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
										outputIndicesAttributeName));
				
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAintegerMatrix matrix = 
							new HLAintegerMatrix(encoderFactory);
					matrix.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) + 
							" Log: setting manager output indices to " + 
							gson.toJson(matrix.getValue()));

					// update manager object
					manager.setOutputIndices(matrix.getValue());
				}

				// get the data corresponding to the input labels attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
										inputLabelsAttributeName));
				
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAstringVector vector = 
							new HLAstringVector(encoderFactory);
					vector.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) + 
							" Log: setting manager input labels to " + 
							gson.toJson(vector.getValue()));

					// update manager object
					manager.setInputLabels(vector.getValue());
				}

				// get the data corresponding to the output labels attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
										outputLabelsAttributeName));
				
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAstringVector vector = 
							new HLAstringVector(encoderFactory);
					vector.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) + 
							" Log: setting manager output labels to " + 
							gson.toJson(vector.getValue()));

					// update manager object
					manager.setOutputLabels(vector.getValue());
				}

				// update manager model
				fireManagerEvent(ManagerAction.MODEL_UPDATE, 
						new ManagerEvent(this, manager));
			} else {
				// get the data corresponding to the output attribute
				wrapper = theAttributes.getValueReference(
						rtiAmbassador.getAttributeHandle(
								rtiAmbassador.getObjectClassHandle(
										managerClassName), 
										outputAttributeName));
				if(wrapper != null) {
					// wrapper has data; decode into an HLA data element
					HLAfloatVector vector = 
							new HLAfloatVector(encoderFactory);
					vector.decode(wrapper);
					System.out.println("Designer " + (index.getValue()+1) +
							" Log: setting manager output to " + 
							vector.getValue());

					// update manager object and set flag to update output
					manager.setOutput(vector.getValue());
					
					// fire update event
					fireManagerEvent(ManagerAction.OUTPUT_UPDATE, 
							new ManagerEvent(this, manager));
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
	@Override
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
				sentOrdering, theTransport, theTime, receivedOrdering, 
				null, reflectInfo);
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#reflectAttributeValues(hla.rti1516e.ObjectInstanceHandle, hla.rti1516e.AttributeHandleValueMap, byte[], hla.rti1516e.OrderType, hla.rti1516e.TransportationTypeHandle, hla.rti1516e.FederateAmbassador.SupplementalReflectInfo)
	 */
	@Override
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
	public void removeManagerListener(ManagerListener listener) {
		// remove the lister from the list
		listenerList.remove(ManagerListener.class, listener);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#removeObjectInstance(hla.rti1516e.ObjectInstanceHandle, byte[], hla.rti1516e.OrderType, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.MessageRetractionHandle, hla.rti1516e.FederateAmbassador.SupplementalRemoveInfo)
	 */
	@Override
	public void removeObjectInstance(ObjectInstanceHandle theObject,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			LogicalTime theTime,
			OrderType receivedOrdering,
			MessageRetractionHandle retractionHandle,
			SupplementalRemoveInfo removeInfo) {
		// try to remove manager from the manager map
		Manager manager = null;
		synchronized(managers) {
			manager = managers.remove(theObject);
		}
		if(manager != null) {
			// notify listeners that manager has been removed
			fireManagerEvent(ManagerAction.REMOVE, 
					new ManagerEvent(this, manager));
		}
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.NullFederateAmbassador#removeObjectInstance(hla.rti1516e.ObjectInstanceHandle, byte[], hla.rti1516e.OrderType, hla.rti1516e.LogicalTime, hla.rti1516e.OrderType, hla.rti1516e.FederateAmbassador.SupplementalRemoveInfo)
	 */
	@Override
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
	@Override
	public void removeObjectInstance(ObjectInstanceHandle theObject,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			SupplementalRemoveInfo removeInfo) {
		// re-direct method to single method signature
		removeObjectInstance(theObject, userSuppliedTag, sentOrdering, 
				null, null, null, removeInfo);
	}

	/**
	 * Shut down.
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
			rtiAmbassador.destroyFederationExecution(properties.getProperty("federationName", "collab"));
		} catch (FederatesCurrentlyJoined ignored) {
		} catch (FederationExecutionDoesNotExist ignored) {
		} catch (NotConnected ignored) {
		}
		
		// disconnect from the rti
		rtiAmbassador.disconnect();
	}

	/**
	 * Start up.
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
	 * @throws MalformedURLException the malformed URL exception
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
	 * @throws ObjectInstanceNotKnown the object instance not known
	 * @throws ObjectClassNotPublished the object class not published
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
			rtiAmbassador.joinFederationExecution("Designer " + index.getValue(),
					federateType, properties.getProperty("federationName", "collab"));
		} catch(FederateAlreadyExecutionMember ignored) { }
		
		// publish and subscribe to object class attributes
		publish();
		subscribe();

		// register the object instance name
		objectInstanceName = rtiAmbassador.getObjectInstanceName(
				rtiAmbassador.registerObjectInstance(
						rtiAmbassador.getObjectClassHandle(
								designerClassName)));
	}
	
	/**
	 * Subscribe.
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
			AttributeNotDefined, ObjectClassNotDefined, SaveInProgress, 
			RestoreInProgress, RTIinternalError, NameNotFound, 
			InvalidObjectClassHandle {
		// create an attribute handle set
		AttributeHandleSet attributeHandleSet = rtiAmbassador
				.getAttributeHandleSetFactory().create();
		// add the initial input attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				initialInputAttributeName));
		// add the target output attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				targetOutputAttributeName));
		// add the output attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputAttributeName));
		// add the active model attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				activeModelAttributeName));
		// add the input indices attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				inputIndicesAttributeName));
		// add the output indices attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputIndicesAttributeName));
		// add the input labels indices attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				inputLabelsAttributeName));
		// add the output labels attribute
		attributeHandleSet.add(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				outputLabelsAttributeName));
		// use the RTI service to subscribe to the defined attributes
		rtiAmbassador.subscribeObjectClassAttributes(
				rtiAmbassador.getObjectClassHandle(managerClassName), 
				attributeHandleSet);
	}
	
	/**
	 * Update index attribute.
	 *
	 * @param indexValue the index value
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
	public void updateIndexAttribute(int indexValue) 
			throws FederateNotExecutionMember, NotConnected, NameNotFound, 
			InvalidObjectClassHandle, RTIinternalError, EncoderException, 
			AttributeNotOwned, AttributeNotDefined, ObjectInstanceNotKnown, 
			SaveInProgress, RestoreInProgress {
		// create an attribute handle value map to store data
		AttributeHandleValueMap attributes = rtiAmbassador
				.getAttributeHandleValueMapFactory().create(1);
		
		// set HLA data element value and add to map
		index.setValue(indexValue);
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				indexAttributeName), index.toByteArray());
		System.out.println("Designer " + (index.getValue()+1) + 
				" Log: setting index value to " + index.getValue());
		
		// use RTI service to update attribute values using map
		rtiAmbassador.updateAttributeValues(
				rtiAmbassador.getObjectInstanceHandle(objectInstanceName), 
				attributes, new byte[0]);
	}
	
	/**
	 * Update input attribute.
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
	public void updateInputAttribute(RealVector inputValue) 
			throws FederateNotExecutionMember, NotConnected, NameNotFound, 
			InvalidObjectClassHandle, RTIinternalError, EncoderException, 
			AttributeNotOwned, AttributeNotDefined, ObjectInstanceNotKnown, 
			SaveInProgress, RestoreInProgress {
		// create an attribute handle value map to store data
		AttributeHandleValueMap attributes = rtiAmbassador
				.getAttributeHandleValueMapFactory().create(1);
		
		// set HLA data element value and add to map
		input.setValue(inputValue);
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				inputAttributeName), input.toByteArray());
		System.out.println("Designer " + (index.getValue()+1) + 
				" Log: setting input value to " + input.getValue());
		
		// use RTI service to update attribute values using map
		rtiAmbassador.updateAttributeValues(
				rtiAmbassador.getObjectInstanceHandle(objectInstanceName), 
				attributes, new byte[0]);
	}
	
	/**
	 * Update ready attribute.
	 *
	 * @param readyValue the ready value
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
	public void updateStateAttribute(boolean readyValue) 
			throws FederateNotExecutionMember, NotConnected, NameNotFound, 
			InvalidObjectClassHandle, RTIinternalError, EncoderException, 
			AttributeNotOwned, AttributeNotDefined, ObjectInstanceNotKnown, 
			SaveInProgress, RestoreInProgress {
		// create an attribute handle value map to store data
		AttributeHandleValueMap attributes = rtiAmbassador
				.getAttributeHandleValueMapFactory().create(1);
		
		// set HLA data element value and add to map
		ready.setValue(readyValue);
		attributes.put(rtiAmbassador.getAttributeHandle(
				rtiAmbassador.getObjectClassHandle(designerClassName), 
				readyAttributeName), ready.toByteArray());
		System.out.println("Designer " + (index.getValue()+1) + 
				" Log: setting ready value to " + ready.getValue());
		
		// use RTI service to update attribute values using map
		rtiAmbassador.updateAttributeValues(
				rtiAmbassador.getObjectInstanceHandle(objectInstanceName), 
				attributes, new byte[0]);
	}
}
