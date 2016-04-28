package edu.mit.collab.util;

import java.util.EventObject;

/**
 * <code>InputEvent</code> is used with <code>InputListener</code> listener
 * to signal that a designer input has been modified.
 *
 * @see InputListener
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class InputEvent extends EventObject {
	private static final long serialVersionUID = 3436106201961020297L;
	
	final int designerIndex, inputIndex;
	final double inputValue;

	/**
	 * Instantiates a new input event.
	 *
	 * @param source the source
	 * @param designerIndex the designer index
	 * @param inputIndex the input index
	 * @param inputValue the input value
	 */
	public InputEvent(Object source, int designerIndex, int inputIndex, 
			double inputValue) {
		super(source);
		this.designerIndex = designerIndex;
		this.inputIndex = inputIndex;
		this.inputValue = inputValue;
	}
}
