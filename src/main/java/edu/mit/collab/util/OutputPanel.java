package edu.mit.collab.util;

import javax.swing.JPanel;

/**
 * The Class OutputPanel.
 */
public abstract class OutputPanel extends JPanel {
	private static final long serialVersionUID = 5549445039639583362L;

	public static final double ERROR_ALLOWED = 0.05;

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public abstract double getValue();
	
	/**
	 * Checks if is within range.
	 *
	 * @return true, if is within range
	 */
	public abstract boolean isWithinRange();
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#setEnabled(boolean)
	 */
	@Override
	public abstract void setEnabled(boolean enabled);
	
	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	public abstract void setValue(double value);
}
