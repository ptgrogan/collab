package edu.mit.collab.manager;

import java.util.EventObject;

/**
 * <code>DesignerEvent</code> is used with <code>DesignerListener</code> 
 * listener to signal that a designer object has been modified.
 *
 * @see DesignerListener
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class DesignerEvent extends EventObject {
	private static final long serialVersionUID = -389874206436130676L;
	
	private final Designer designer;
	
	/**
	 * Instantiates a new designer event.
	 *
	 * @param source the source
	 * @param designer the designer
	 */
	public DesignerEvent(Object source, Designer designer) {
		super(source);
		this.designer = designer;
	}
	
	/**
	 * Gets the designer.
	 *
	 * @return the designer
	 */
	public Designer getDesigner() {
		return designer;
	}
}
