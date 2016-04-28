package edu.mit.collab.designer;

import java.util.EventObject;

/**
 * <code>ManagerEvent</code> is used with <code>ManagerListener</code> 
 * listener to signal that a manager object has been modified.
 *
 * @see ManagerListener
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class ManagerEvent extends EventObject {
	private static final long serialVersionUID = -389874206436130676L;
	
	private final Manager manager;
	
	/**
	 * Instantiates a new manager event.
	 *
	 * @param source the source
	 * @param manager the manager
	 */
	public ManagerEvent(Object source, Manager manager) {
		super(source);
		this.manager = manager;
	}

	/**
	 * Gets the manager.
	 *
	 * @return the manager
	 */
	public Manager getManager() {
		return manager;
	}
}
