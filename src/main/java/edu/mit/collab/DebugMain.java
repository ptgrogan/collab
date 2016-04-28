package edu.mit.collab;

import java.awt.Point;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

import edu.mit.collab.designer.DesignerFrame;
import edu.mit.collab.manager.ManagerFrame;

/**
 * The main class for debugging - launches one manager and three designer GUIs.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class DebugMain {
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// set the look and feel to the jgoodies plastic 3d theme
		// (looks better across platforms)
		try {
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
		} catch (Exception e) {}
		
		// start up gui components in swing thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					// start manager frame and make visible
					ManagerFrame m = new ManagerFrame();
					m.pack();
					m.setVisible(true);
					
					// start three designer frames and make visible
					for(int i = 0; i < 3; i++) {
						DesignerFrame d = new DesignerFrame(i);
						d.pack();
						// position to the right of the manager frame
						d.setLocation(new Point(
								m.getLocation().x 
									+ m.getWidth() 
									+ i*d.getWidth(), 
								m.getLocation().y));
						d.setVisible(true);
					}
				} catch (Exception ex) {
					// show error message
					JOptionPane.showMessageDialog(null, 
							"An exception of type " + ex.getMessage() + 
							" occurred while starting. Please see the " +
							"stack trace for more details.", 
							"Error", 
							JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
		});
	}
}
