package edu.mit.collab.util;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * The Class InputPanel.
 */
public abstract class InputPanel extends JPanel {
	private static final long serialVersionUID = 4677941323688111822L;

	/**
	 * Adds the input listener.
	 *
	 * @param listener the listener
	 */
	public void addInputListener(InputListener listener) {
		listenerList.add(InputListener.class, listener);
	}
	
	/**
	 * Bind key.
	 *
	 * @param keyStroke the key stroke
	 * @param action the action
	 */
	public abstract void bindKey(KeyStroke keyStroke, String name, 
			Action action);
	
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public abstract double getValue();
	
	/**
	 * Checks if is ready.
	 *
	 * @return true, if is ready
	 */
	public abstract boolean isReady();
	
	/**
	 * Removes the input listener.
	 *
	 * @param listener the listener
	 */
	public void removeInputListener(InputListener listener) {
		listenerList.remove(InputListener.class, listener);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#requestFocus()
	 */
	@Override
	public abstract void requestFocus();

	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#setEnabled(boolean)
	 */
	@Override
	public abstract void setEnabled(boolean enabled);
	
	/**
	 * Sets the ready.
	 *
	 * @param ready the new ready
	 */
	public abstract void setReady(boolean ready);
	
	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	public abstract void setValue(double value);
}
