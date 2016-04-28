package edu.mit.collab.designer;

import java.util.EventListener;

/**
 * The listener interface for receiving manager events.
 * The class that is interested in processing a manager
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addManagerListener<code> method. When
 * the manager event occurs, that object's appropriate
 * method is invoked.
 *
 * @see ManagerEvent
 */
public interface ManagerListener extends EventListener {
	
	/**
	 * Method to notify that a manager has joined the application.
	 *
	 * @param e the event
	 */
	public void managerAdded(ManagerEvent e);
	
	/**
	 * Method to notify that a manager's active model has been modified
	 * to reflect a new design task.
	 *
	 * @param e the event
	 */
	public void managerModelModified(ManagerEvent e);
	
	/**
	 * Method to notify that a manager's output has been modified to 
	 * reflect a new set of inputs from designers.
	 *
	 * @param e the event
	 */
	public void managerOutputModified(ManagerEvent e);
	
	/**
	 * Method to notify that a manger has left the application.
	 *
	 * @param e the event
	 */
	public void managerRemoved(ManagerEvent e);
}
