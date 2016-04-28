package edu.mit.collab.designer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.mit.collab.util.InputEvent;
import edu.mit.collab.util.InputListener;
import edu.mit.collab.util.InputPanel;
import edu.mit.collab.util.InputPanelFactory;
import edu.mit.collab.util.OutputPanel;
import edu.mit.collab.util.OutputPanelFactory;
import edu.mit.collab.util.Utilities;
import hla.rti1516e.exceptions.RTIinternalError;

/**
 * This is the graphical user interface to the designer application. It 
 * controls the assigned model inputs and provides feedback on assigned 
 * outputs.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class DesignerFrame extends JFrame 
		implements ManagerListener, InputListener {
	private static enum Mode {Ready, Initializing, Initialized, 
		Running, Waiting, Solved, Complete}
	private static final long serialVersionUID = -4866808562296766482L;
	
	// configuration option to allow constant feedback from designers, i.e.
	// not require each designer to press "update" button to receive updates
	private static final boolean constantFeedbackMode = true;
	
	private final int designerIndex; // immutable
	private final DesignerAmbassador designerAmbassador; // immutable
	
	private final JLabel activeModelLabel; // mutable
	private final JPanel modelPanel; // mutable
	private InputPanel[] inputPanels; // mutable
	private OutputPanel[] outputPanels; // mutable

	private Mode mode; // mutable
	private Manager manager; // mutable
	
	// action to submit a design
	private final Action submitAction = new AbstractAction("Submit", 
			new ImageIcon(getClass().getClassLoader()
					.getResource("resources/add.png"))) {
		private static final long serialVersionUID = 5671259193377510485L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			sendStateUpdate(true);
		}
	};
	
	// action to cancel a design submission
	private final Action cancelAction = new AbstractAction("Cancel", 
			new ImageIcon(getClass().getClassLoader()
					.getResource("resources/cancel.png"))) {
		private static final long serialVersionUID = 5671259193377510485L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			sendStateUpdate(false);
		}
	};
	
	// action to gracefully exit the application
	private final Action exitAction = new AbstractAction("Exit") {
		private static final long serialVersionUID = 5879052291158987588L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			exit();
		}
	};

	// action to toggle between full screen and windowed mode
	private final Action fullscreenAction = new AbstractAction("Fullscreen", 
			new ImageIcon(getClass().getClassLoader()
					.getResource("resources/arrow_inout.png"))) {
		private static final long serialVersionUID = -5248342087657327750L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			toggleFullscreen();
		}
	};
	
	// actions to focus on input sliders 0-8
	private final Action[] focusActions = new Action[]{
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 0) {
						inputPanels[0].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 1) {
						inputPanels[1].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 2) {
						inputPanels[2].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 3) {
						inputPanels[3].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 4) {
						inputPanels[4].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 5) {
						inputPanels[5].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 6) {
						inputPanels[6].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 7) {
						inputPanels[7].requestFocus();
					}
				}
			},
			new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					if(inputPanels.length > 8) {
						inputPanels[8].requestFocus();
					}
				}
			}
	};
	
	/**
	 * Instantiates a new designer frame.
	 *
	 * @param designerIndex the designer index
	 * @throws RTIinternalError the RTI internal error
	 */
	public DesignerFrame(int designerIndex) throws RTIinternalError {
		// set title and icon image
		super("Designer " + (designerIndex+1));
		setIconImage(Utilities.getUserIcon(designerIndex).getImage());

		// set minimum size of window (preferred size will vary 
		// with number of inputs and outputs)
		setMinimumSize(new Dimension(200,300));

		// initialize federate ambassador and add the frame as a listener
		this.designerIndex = designerIndex;
		designerAmbassador = new DesignerAmbassador(designerIndex);
		designerAmbassador.addManagerListener(this);

		// try to start the federate ambassador
		try {
			designerAmbassador.startUp();
			// immediately update index attribute (only time this is done)
			designerAmbassador.updateIndexAttribute(designerIndex);
			
			if(designerAmbassador.getInstanceName().indexOf(
					"HLAobjectRoot") >= 0) {
				// format frame title for pRTI instance names, i.e. only use
				// DesignerXYZ from HLAobjectRoot.DesignerXYZ
				setTitle(designerAmbassador.getInstanceName().substring(
						designerAmbassador.getInstanceName().indexOf('.') + 1, 
						designerAmbassador.getInstanceName().length()));
			} else {
				// format title for other RTI implementations
				setTitle(designerAmbassador.getInstanceName());
			}
		} catch (Exception e) {
			e.printStackTrace();
			showErrorDialog(e.getMessage());
		}

		// add a window listener to gracefully exit application
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});

		setJMenuBar(createMenuBar());
		
		// create designer panel
		JPanel designerPanel = new JPanel();
		designerPanel.setFocusable(true);
		designerPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		designerPanel.setLayout(new BorderLayout());
		
		// add binding for CTRL+F (toggle fullscreen)
		designerPanel.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK),
				"toggleFullscreen");
		designerPanel.getActionMap().put("toggleFullscreen", fullscreenAction);

		// add binding for ENTER (submit design)
		designerPanel.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
		designerPanel.getActionMap().put("submit", submitAction);
		
		// add binding for ESC (cancel design submission)
		designerPanel.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
		designerPanel.getActionMap().put("cancel", cancelAction);
		
		// add bindings for numeric keys 1-9 (change input focus)
		for(int k = 0; k < focusActions.length; k++) {
			designerPanel.getInputMap().put(KeyStroke.getKeyStroke(
					new Integer(k+1).toString()), "focus" + k+1);
			designerPanel.getActionMap().put("focus" + k+1, focusActions[k]);
		}
		
		// create and add active model label to top of panel
		activeModelLabel = new JLabel("");
		designerPanel.add(activeModelLabel, BorderLayout.NORTH);
		
		// create and add blank model panel to center of panel
		modelPanel = new JPanel();
		modelPanel.setLayout(new GridBagLayout());
		designerPanel.add(modelPanel, BorderLayout.CENTER);
		
		if(!constantFeedbackMode) {
			// if not operating in constant feedback mode, add a
			// submit button to the bottom of the panel
			designerPanel.add(new JButton(submitAction), BorderLayout.SOUTH);
		}
		
		// set content pane of this frame to be the designer panel
		setContentPane(designerPanel);
		
		// initialize for a blank model
		initialize();
	}
	
	/**
	 * Creates the menu bar.
	 *
	 * @return the j menu bar
	 */
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		// create file menu and add menu items for each relevant action
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new JMenuItem(exitAction));
		menuBar.add(fileMenu);
		
		// create view menu and add menu items for each relevant action
		JMenu viewMenu = new JMenu("View");
		fullscreenAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
		fullscreenAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));
		viewMenu.add(new JCheckBoxMenuItem(fullscreenAction));
		menuBar.add(viewMenu);
		
		// create design menu and add menu items for each relevant action
		JMenu designMenu = new JMenu("Design");
		submitAction.setEnabled(false);
		submitAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		submitAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		designMenu.add(new JMenuItem(submitAction));
		cancelAction.setEnabled(false);
		cancelAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		cancelAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		designMenu.add(new JMenuItem(cancelAction));
		menuBar.add(designMenu);
		
		return menuBar;
	}	
	
	/**
	 * Exits the application.
	 */
	private void exit() {
		// dispose of the frame
		dispose();
		
		// try to shut down the federate ambassador
		try {
			designerAmbassador.shutDown();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// exit the application
		System.exit(0);
	}

	/**
	 * Initializes the window for a new model.
	 */
	private void initialize() {
		// remove all existing components from the model panel
		modelPanel.removeAll();
		
		// set the active model text to an appropriate string
		activeModelLabel.setText((manager == null ? "" 
				: "Task: " + manager.getActiveModel()));
		
		if(manager == null 
				|| manager.getActiveModel().equals("") 
				|| manager.getActiveModel().equals("Ready...")) {
			// if manager is not yet joined, does not have an experiment
			// selected, or is ready to start experiment, display mostly
			// blank panel and set ready mode
			outputPanels = new OutputPanel[0];
			inputPanels = new InputPanel[0];
			mode = Mode.Ready;
		} else if(manager.getActiveModel().equals("Complete!")) {
			// if manager has completed experiment, display mostly blank
			// panel and set complete mode
			outputPanels = new OutputPanel[0];
			inputPanels = new InputPanel[0];
			mode = Mode.Complete;
		} else {
			// otherwise if manager has a model loaded, initialize the new
			// model panel
			
			// set mode to initializing to prevent updates from being processed
			mode = Mode.Initializing;
			
			JPanel outputPanel = new JPanel();
			outputPanel.setLayout(new BoxLayout(outputPanel, 
					BoxLayout.PAGE_AXIS));

			// create default grid bag constraints
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5,5,5,5);
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1;
			c.weighty = 1;
			
			// create and lay out the output panels horizontally at the top
			// of the panel
			outputPanels = new OutputPanel[Math.min(manager.getTargetOutput(
					designerIndex).getDimension(), manager.getOutput(
							designerIndex).getDimension())];
			for(int i = 0; i < outputPanels.length; i++) {
				outputPanels[i] = OutputPanelFactory.createBasicOutputPanel(designerIndex, i, 
						manager.getTargetOutput(designerIndex).getEntry(i), 
						manager.getOutput(designerIndex).getEntry(i), 
						manager.getOutputLabels(designerIndex)[i]);
				outputPanel.add(outputPanels[i]);
			}

			modelPanel.add(new JScrollPane(outputPanel), c);
			
			// lay out input panels
			
			JPanel inputPanel = new JPanel();
			inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.LINE_AXIS));
			
			// create and lay out the input panels vertically below the
			// output panels
			inputPanels = new InputPanel[manager.getInitialInput(
					designerIndex).getDimension()];
			for(int i = 0; i < inputPanels.length; i++) {
				inputPanels[i] = InputPanelFactory.createBasicInputPanelWithoutSolution(
						designerIndex, i, 
						manager.getInitialInput(designerIndex).getEntry(i), 
						manager.getInputLabels(designerIndex)[i]);
				inputPanels[i].addInputListener(this);
				// add a key listener to listen for numeric key presses
				for(int k = 0; k < focusActions.length; k++) {
					inputPanels[i].bindKey(KeyStroke.getKeyStroke(
							new Integer(k+1).toString()), 
							"focus" + (k+1), focusActions[k]);
				}
				inputPanel.add(inputPanels[i]);
			}
			
			c.gridy++;
			c.weightx = 1;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			modelPanel.add(new JScrollPane(inputPanel), c);
			
			// set mode to initialized
			mode = Mode.Initialized;
			
			// try to set the first input panel to be focused
			setInputFocus(0);
		}
		

		// finally, validate the layout and pack to re-size the window
		// and repaint to force graphics to update
		if(!isUndecorated()) {
			// don't pack if in fullscreen mode
			pack();
		} else {
			validate();
		}
		repaint();

		// set input panels and actions based on current mode
		for(int i = 0; i < inputPanels.length; i++) {
			inputPanels[i].setEnabled(
					mode == Mode.Initialized || mode == Mode.Running);
		}
		submitAction.setEnabled(
				mode == Mode.Initialized || mode == Mode.Running);
		cancelAction.setEnabled(mode == Mode.Waiting);
	}
	
	private Component lastFocusOwner;

	/* (non-Javadoc)
	 * @see edu.mit.collab.util.InputListener#inputChanged(edu.mit.collab.util.InputEvent)
	 */
	@Override
	public void inputChanged(InputEvent event) {
		if(mode == Mode.Initialized || mode == Mode.Running) {
			// if model panel is initialized or task running, set running mode
			mode = Mode.Running;
			
			if(constantFeedbackMode) {
				lastFocusOwner = this.getFocusOwner();
				for(int i = 0; i < inputPanels.length; i++) {
					inputPanels[i].setEnabled(false);
				}
				mode = Mode.Waiting;
			}
			
			// aggregate the input vector based on input panel values
			RealVector inputVector = new ArrayRealVector(inputPanels.length);
			for(int i = 0; i < inputPanels.length; i++) {
				inputVector.setEntry(i, inputPanels[i].getValue());
			}
			
			// try to update the input attributes using the federate ambassador
			try {
				designerAmbassador.updateInputAttribute(inputVector);
			} catch(Exception ex) {
				ex.printStackTrace();
				showErrorDialog(ex.getMessage());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.designer.ManagerListener#managerAdded(edu.mit.collab.designer.ManagerEvent)
	 */
	@Override
	public void managerAdded(final ManagerEvent e) {
		// act on the event in the swing event thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// only register one manager at a time
				if(manager == null) {
					manager = e.getManager();
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see edu.mit.collab.designer.ManagerListener#managerModelModified(edu.mit.collab.designer.ManagerEvent)
	 */
	@Override
	public void managerModelModified(final ManagerEvent e) {
		// act on the event in the swing event thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// initialize panel if responding to same manager
				if(e.getManager().equals(manager)) {
					initialize();
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.designer.ManagerListener#managerOutputModified(edu.mit.collab.designer.ManagerEvent)
	 */
	@Override
	public void managerOutputModified(final ManagerEvent e) {
		// act on the event in the swing event thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(e.getManager().equals(manager)
						&& (mode == Mode.Initialized 
						|| mode == Mode.Running 
						|| mode == Mode.Waiting)) {
					// update the outputs if the model panel is initialized,
					// running, or waiting for other input submission
					updateOutputs();
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.designer.ManagerListener#managerRemoved(edu.mit.collab.designer.ManagerEvent)
	 */
	@Override
	public void managerRemoved(final ManagerEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// if manager matches, set to null
				if(e.getManager().equals(manager)) {
					manager = null;
				}
			}
		});
	}
	
	/**
	 * Sends a state update using the federate ambassador.
	 *
	 * @param ready if the designer is ready (i.e. the design is submitted)
	 */
	private void sendStateUpdate(boolean ready) {
		// if design is submitted, switch to waiting mode
		mode = ready ? Mode.Waiting : Mode.Running;
		
		// disable input panels and actions if waiting
		for(int i = 0; i < inputPanels.length; i++) {
			inputPanels[i].setEnabled(!ready);
		}
		submitAction.setEnabled(!ready);
		cancelAction.setEnabled(ready);
		
		// try to update the attribute using the federate ambassador
		try {
			designerAmbassador.updateStateAttribute(ready);
		} catch(Exception ex) {
			ex.printStackTrace();
			showErrorDialog(ex.getMessage());
		}
	}

	/**
	 * Sets the input focus to the panel at the passed index position. This 
	 * method fails gracefully (i.e. does nothing) if no panel exists at 
	 * such index.
	 *
	 * @param index the input panel 0-based index.
	 */
	private void setInputFocus(int index) {
		if(inputPanels.length > index) {
			inputPanels[index].requestFocus();
		}
	}

	/**
	 * Show error dialog.
	 *
	 * @param message the message
	 */
	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", 
				JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Toggles between fullscreen and windowed mode.
	 */
	private void toggleFullscreen() {
		if(!isUndecorated()) {
			// make full-screen
			setVisible(false);
	 		dispose();
	 		setUndecorated(true);
	 		getJMenuBar().setVisible(false);
	 		GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().setFullScreenWindow(this);
	 		// set to 800x600 pixels
	 		GraphicsEnvironment.getLocalGraphicsEnvironment()
	 				.getDefaultScreenDevice().setDisplayMode(new DisplayMode(
	 						800, 600, 16, DisplayMode.REFRESH_RATE_UNKNOWN));
	 		setVisible(true);
		} else {
			// make windowed
			setVisible(false);
			dispose();
			setUndecorated(false);
	 		getJMenuBar().setVisible(true);
			GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().setFullScreenWindow(null);
	 		pack();
			repaint();
	 		setLocationRelativeTo(null);
			setVisible(true);
		}
	}

	/**
	 * Updates the output panels.
	 */
	private void updateOutputs() {
		// update each output panel value base don manager outputs
		for(int i = 0; i < manager.getOutput(designerIndex).getDimension(); i++) {
			outputPanels[i].setValue(manager.getOutput(designerIndex).getEntry(i));
		}
		// revert from waiting state (if necessary)
		sendStateUpdate(false);
		
		// if the task is solved, mark as solved
		if(manager.isSolved()) {
			mode = Mode.Solved;
			activeModelLabel.setText("Solved!");
			
			// fix layout problems with new text
			if(!isUndecorated()) {
				// don't pack if in fullscreen mode
				pack();
			} else {
				validate();
			}
		}
		
		// disable input panels if solved
		for(int i = 0; i < inputPanels.length; i++) {
			inputPanels[i].setEnabled(mode != Mode.Solved);
		}
		
		// restore last focus owner
		if(lastFocusOwner != null) {
			lastFocusOwner.requestFocus();
		}
		
		// disable/enable actions if waiting/running
		submitAction.setEnabled(
				mode == Mode.Initialized || mode == Mode.Running);
		cancelAction.setEnabled(mode == Mode.Waiting);
		
	}
}