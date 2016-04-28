package edu.mit.collab.manager;

import java.util.EventListener;

/**
 * The listener interface for receiving designer events.
 * The class that is interested in processing a designer
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addDesignerListener<code> method. When
 * the designer event occurs, that object's appropriate
 * method is invoked.
 *
 * @see DesignerEvent
 */
public interface DesignerListener extends EventListener {
	
	/**
	 * Method to notify that a designer has joined the application.
	 *
	 * @param e the event
	 */
	public void designerAdded(DesignerEvent e);
	
	/**
	 * Method to notify that a designer input vector has been modified.
	 *
	 * @param e the event
	 */
	public void designerInputModified(DesignerEvent e);
	
	/**
	 * Method to notify that a designer has left the application.
	 *
	 * @param e the event
	 */
	public void designerRemoved(DesignerEvent e);
	
	/**
	 * Method to notify that a designer state (e.g. ready status) 
	 * has been modified.
	 *
	 * @param e the event
	 */
	public void designerStateModified(DesignerEvent e);
}
