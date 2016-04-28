package edu.mit.collab.util;

import java.util.EventListener;

/**
 * The listener interface for receiving input events.
 * The class that is interested in processing a input
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addInputListener<code> method. When
 * the input event occurs, that object's appropriate
 * method is invoked.
 *
 * @see InputEvent
 * 
 * @author Paul T. Grogan
 */
public interface InputListener extends EventListener {
	
	/**
	 * Input changed.
	 *
	 * @param event the event
	 */
	public void inputChanged(InputEvent event);
}
