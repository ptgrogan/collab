package edu.mit.collab;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

import edu.mit.collab.manager.ManagerFrame;
import hla.rti1516e.exceptions.RTIinternalError;

/**
 * The main class to launch a manager GUI.
 * 
 * @author Paul T. Grogan, pgrogan@stevens.edu
 */
public class ManagerMain {
	/**
	 * The main method to launch the manager application.
	 *
	 * @param args the arguments
	 */
	public static void main(final String[] args) {
		// set jgoodies plastic 3d look and feel for better 
		// cross-platform support
		try {
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		// start manager frame in java swing thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					// create frame, pack, and make visible
					ManagerFrame f = new ManagerFrame();
					f.pack();
					f.setVisible(true);
				} catch (RTIinternalError ex) {
					ex.printStackTrace();
				}
			}
		});
	}
}
