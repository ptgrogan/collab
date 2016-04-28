package edu.mit.collab;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

import edu.mit.collab.designer.DesignerFrame;
import hla.rti1516e.exceptions.RTIinternalError;

/**
 * The main class to launch a designer GUI.
 * 
 * @author Paul T. Grogan, pgrogan@stevens.edu
 */
public class DesignerMain {
	/**
	 * The main method to launch the designer application.
	 *
	 * @param args the arguments
	 */
	public static void main(final String[] args) {
		final int designerIndex;
		// validate designer index
		if(args.length < 1 || Integer.parseInt(args[0]) < 0) {
			throw new IllegalArgumentException(
					"Non-negative designer index must be specified in args[0].");
		} else {
			designerIndex = Integer.parseInt(args[0]);
		}

		// set jgoodies plastic 3d look and feel for better 
		// cross-platform support
		try {
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		// start designer frame in java swing thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					// create frame, pack, and make visible
					DesignerFrame f = new DesignerFrame(designerIndex);
					f.pack();
					f.setVisible(true);
				} catch (RTIinternalError ex) {
					ex.printStackTrace();
				}
			}
		});
	}
}
