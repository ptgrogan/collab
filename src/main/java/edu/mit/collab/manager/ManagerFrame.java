package edu.mit.collab.manager;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.mit.collab.util.InputPanel;
import edu.mit.collab.util.InputPanelFactory;
import edu.mit.collab.util.OutputPanel;
import edu.mit.collab.util.OutputPanelFactory;
import edu.mit.collab.util.Utilities;
import hla.rti1516e.exceptions.RTIinternalError;

/**
 * This is the graphical user interface to the manager application. It 
 * controls the advancement of models and can transition between training
 * and experimental modes.
 * 
 * Each of the designers' inputs is represented as a disabled input slider
 * and each of the designers' outputs is also visible. In the manager
 * application, the solution input values are also visible.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class ManagerFrame extends JFrame implements DesignerListener {
	private static enum Mode {Ready, Initialized, Running, Solved, Complete}
	private static final long serialVersionUID = -4866808562296766482L;
	
	// configuration option to allow constant feedback from designers, i.e.
	// not require each designer to press "update" button to receive updates
	private static final boolean constantFeedback = true;

	private final File logFile; // immutable
	private final ManagerAmbassador managerAmbassador; // immutable
	
	private Experiment experiment; // mutable
	private final Set<Designer> designers = new HashSet<Designer>(); // mutable
	private final JPanel modelPanel; // mutable
	private final JLabel activeModelLabel; // mutable
	private InputPanel[] inputPanels; // mutable
	private OutputPanel[] outputPanels; // mutable
	private final XYSeriesCollection errorDataset; // mutable
	private final XYSeries errorSeries; // mutable
	private final XYSeriesCollection inputDataset; // mutable
	private XYSeries[] inputSeries, solutionSeries; // mutable
	private final JFreeChart errorChart, inputChart; // mutable
	private final JTextField logCommentText; // mutable
	
	private Mode mode; // mutable
	private long startTime; // mutable
	
	// action to log comments
	private final Action logCommentAction = new AbstractAction("Log", 
			new ImageIcon(getClass().getClassLoader()
					.getResource("resources/pencil.png"))) {
		private static final long serialVersionUID = -6911789719643032313L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(!logCommentText.getText().isEmpty()) {
				log("comment",logCommentText.getText());
				logCommentText.setText("");
			}
		}
	};
	
	// action to reset experiment
	private final Action resetAction = new AbstractAction("Reset") {
		private static final long serialVersionUID = 8731970476073997327L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			resetExperiment();
		}
	};
	
	// action to open an experiment
	private final Action openAction = new AbstractAction("Open") {
		private static final long serialVersionUID = 8731970476073997327L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			openExperiment();
		}
	};
	
	// action to close an experiment
	private final Action closeAction = new AbstractAction("Close") {
		private static final long serialVersionUID = -2929253323012094376L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			closeExperiment();
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
	
	// action to advance to next model in experiment
	private final Action nextModelAction = new AbstractAction("Next Model", 
			new ImageIcon(getClass().getClassLoader()
					.getResource("resources/arrow_right.png"))) {
		private static final long serialVersionUID = 2441873772423897829L;
		@Override
		public void actionPerformed(ActionEvent e) {
			if((mode == Mode.Initialized || mode == Mode.Running) && 
					JOptionPane.CANCEL_OPTION == JOptionPane.showConfirmDialog(
							getContentPane(), 
							"Advance without completing current task?", 
							"Confirm",
							JOptionPane.OK_CANCEL_OPTION)) {
				return;
			}
			nextModel();
		}
	};
	
	// action to end the training mode
	private final Action endTrainingAction = new AbstractAction("End Training",
			new ImageIcon(getClass().getClassLoader()
					.getResource("resources/arrow_switch.png"))) {
		private static final long serialVersionUID = -7938329920483460804L;
		@Override
		public void actionPerformed(ActionEvent e) {
			experiment.endTrainingMode();
			nextModel();
		}
	};
	
	/**
	 * Instantiates a new manager frame.
	 *
	 * @throws RTIinternalError the RTI internal error
	 */
	public ManagerFrame() throws RTIinternalError {
		// set title and icon image
		super("Manager");
		setIconImage(new ImageIcon(getClass().getClassLoader()
				.getResource("resources/group.png")).getImage());
		
		// set minimum size of window (preferred size will vary 
		// with number of inputs and outputs)
		setMinimumSize(new Dimension(800,600));
		
		// initialize federate ambassador and add the frame as a listener
		managerAmbassador = new ManagerAmbassador();
		managerAmbassador.addDesignerListener(this);

		// try to start the federate ambassador
		try {
			managerAmbassador.startUp();
			if(managerAmbassador.getInstanceName().indexOf(
					"HLAobjectRoot") >= 0) {
				// format frame title for pRTI instance names, i.e. only use
				// ManagerXYZ from HLAobjectRoot.ManagerXYZ
				setTitle(managerAmbassador.getInstanceName().substring(
						managerAmbassador.getInstanceName().indexOf('.') + 1, 
						managerAmbassador.getInstanceName().length()));
			} else {
				// format title for other RTI implementations
				setTitle(managerAmbassador.getInstanceName());
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
		
		// create a menu bar
		setJMenuBar(createMenuBar());

		// create error plotting datasets and charts
		errorDataset = new XYSeriesCollection();
		errorSeries = new XYSeries(new Integer(0), false, false);
		errorDataset.addSeries(errorSeries);
		errorChart = ChartFactory.createXYLineChart("Output Error", 
				null, 
				null, 
				errorDataset, 
				PlotOrientation.VERTICAL, 
				false, 
				false, 
				false);
		errorChart.setBackgroundPaint(getBackground());
		if(errorChart.getPlot() instanceof XYPlot) {
			XYPlot xyPlot = (XYPlot) errorChart.getPlot();
			XYItemRenderer renderer = new StandardXYItemRenderer(
					StandardXYItemRenderer.SHAPES_AND_LINES);
			renderer.setSeriesShape(0, new Ellipse2D.Double(-2,-2,4,4));
			xyPlot.setRenderer(renderer);
			xyPlot.setBackgroundPaint(Color.WHITE);
			xyPlot.setDomainGridlinePaint(Color.GRAY);
			xyPlot.setRangeGridlinePaint(Color.GRAY);
		}
		
		// create input plotting datasets and charts
		inputDataset = new XYSeriesCollection();
		inputChart = ChartFactory.createXYLineChart("Input Values", 
				null, 
				null, 
				inputDataset, 
				PlotOrientation.VERTICAL, 
				true,
				false, 
				false);
		inputChart.setBackgroundPaint(getBackground());
		if(inputChart.getPlot() instanceof XYPlot) {
			XYPlot xyPlot = (XYPlot) inputChart.getPlot();
			xyPlot.setBackgroundPaint(Color.WHITE);
			xyPlot.setDomainGridlinePaint(Color.GRAY);
			xyPlot.setRangeGridlinePaint(Color.GRAY);
			XYItemRenderer renderer = new StandardXYItemRenderer(
					StandardXYItemRenderer.SHAPES_AND_LINES);
			xyPlot.setRenderer(renderer);
		}

		// create comment logging components
		logCommentText = new JTextField(15);
		logCommentText.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "logComment");
		logCommentText.getActionMap().put("logComment", logCommentAction);
		
		// create log file
		logFile = new File(new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(
				new Date()) + ".log");
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// create manager panel
		JPanel managerPanel = new JPanel();
		managerPanel.setFocusable(true);
		managerPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		managerPanel.setLayout(new BorderLayout());
		
		// create and add active model label to top of panel
		activeModelLabel = new JLabel("");
		managerPanel.add(activeModelLabel, BorderLayout.NORTH);
		
		// create and add blank model panel to center of panel
		modelPanel = new JPanel();
		modelPanel.setLayout(new GridBagLayout());
		managerPanel.add(modelPanel, BorderLayout.CENTER);
		
		// create and add control panel to bottom of panel
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout(5, 5));
		controlPanel.add(new JLabel("Comment: "), BorderLayout.WEST);
		controlPanel.add(logCommentText, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		buttonPanel.add(new JButton(logCommentAction), BorderLayout.EAST);
		buttonPanel.add(new JButton(nextModelAction));
		buttonPanel.add(new JButton(endTrainingAction));
		controlPanel.add(buttonPanel, BorderLayout.EAST);
		managerPanel.add(controlPanel, BorderLayout.SOUTH);
		
		// set content pane of this frame to be the manager panel
		setContentPane(managerPanel);
		
		// initialize with a null experiment
		setExperiment(null);
	}
	
	/**
	 * Closes an experiment.
	 */
	private void closeExperiment() {
		// use confirm dialog to verify experiment should be closed
		if(experiment != null
				&& JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
						this, "Are you sure you want to close?", 
						"Confirm Close", JOptionPane.YES_NO_OPTION)) {
			setExperiment(null);
		}
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
		resetAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		resetAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		fileMenu.add(new JMenuItem(resetAction));
		openAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		openAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		fileMenu.add(new JMenuItem(openAction));
		closeAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		closeAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		closeAction.setEnabled(false);
		fileMenu.add(new JMenuItem(closeAction));
        fileMenu.addSeparator();
		fileMenu.add(new JMenuItem(exitAction));
		menuBar.add(fileMenu);

		// create view menu and add menu items for each relevant action
		JMenu viewMenu = new JMenu("View");
		fullscreenAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
		fullscreenAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		viewMenu.add(new JCheckBoxMenuItem(fullscreenAction));
		menuBar.add(viewMenu);

		// create manage menu and add menu items for each relevant action
		JMenu manageMenu = new JMenu("Manage");
		logCommentAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 
						KeyEvent.CTRL_DOWN_MASK));
		logCommentAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);
		manageMenu.add(new JMenuItem(logCommentAction));
		nextModelAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_N, 
						KeyEvent.CTRL_DOWN_MASK));
		nextModelAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
		manageMenu.add(new JMenuItem(nextModelAction));
		endTrainingAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke(KeyEvent.VK_D, 
						KeyEvent.CTRL_DOWN_MASK));
		endTrainingAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_D);
		manageMenu.add(new JMenuItem(endTrainingAction));
		menuBar.add(manageMenu);
		
		return menuBar;
	}	
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.manager.DesignerListener#designerAdded(edu.mit.collab.manager.DesignerEvent)
	 */
	@Override
	public void designerAdded(final DesignerEvent e) {
		// act on event in swing event thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// check for duplicate index values
				for(Designer d : designers) {
					if(d.getIndex() == e.getDesigner().getIndex()) {
						// duplicate designer
						showErrorDialog(e.getDesigner().getInstanceName() +
								" has a duplicate index (" + 
								e.getDesigner().getIndex() +
								") and did not join.");
						return;
					}
				}
				// index is not a duplicate, add designer
				showInformationDialog(e.getDesigner().getInstanceName() + 
						" joined with index " + e.getDesigner().getIndex() + 
						".");
				// add the designer to the local set
				designers.add(e.getDesigner());
			}
		});
	}

	/* (non-Javadoc)
	 * @see edu.mit.collab.manager.DesignerListener#designerInputModified(edu.mit.collab.manager.DesignerEvent)
	 */
	@Override
	public void designerInputModified(final DesignerEvent e) {
		// act on event in swing event thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// respond to input changes if model is initialized or running 
				// and designer is in local set
				if((mode == Mode.Initialized || mode == Mode.Running) 
						&& designers.contains(e.getDesigner())) {
					// determine which inputs designer has control of
					int[] inputs = experiment.getActiveModel()
							.getInputIndices()[e.getDesigner().getIndex()];
					// update value of each corresponding input panel
					for(int i = 0; i < inputs.length; i++) {
						inputPanels[inputs[i]].setValue(
								e.getDesigner().getInputVector().getEntry(i));
					}
					// if providing constant feedback, check whether to update 
					// outputs after every input modification
					if(constantFeedback) {
						updateOutputs();
					}
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see edu.mit.collab.manager.DesignerListener#designerRemoved(edu.mit.collab.manager.DesignerEvent)
	 */
	@Override
	public void designerRemoved(final DesignerEvent e) {
		// act on event in swing event thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// remove the designer from the local set
				designers.remove(e.getDesigner());
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.manager.DesignerListener#designerStateModified(edu.mit.collab.manager.DesignerEvent)
	 */
	@Override
	public void designerStateModified(final DesignerEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// respond to state changes if model is initialized or running
				// and designer is in local set
				if((mode == Mode.Initialized || mode == Mode.Running) 
						&& designers.contains(e.getDesigner())) {
					// determine which inputs designer has control of
					int[] inputs = experiment.getActiveModel()
							.getInputIndices()[e.getDesigner().getIndex()];
					// update corresponding input panels
					for(int i = 0; i < inputs.length; i++) {
						inputPanels[inputs[i]].setReady(e.getDesigner().isReady());
					}
					// if NOT constant feedback, check whether to update 
					// outputs after every state modification
					if(!constantFeedback) {
						updateOutputs();
					}
				}
			}
		});
	}
	
	/**
	 * Exits the application gracefully.
	 */
	private void exit() {
		// dispose of the frame
		dispose();
		
		// try to shut down the federate ambassador
		try {
			managerAmbassador.shutDown();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// exit the application
		System.exit(0);
	}
	
	private String vectorToString(RealVector vector, boolean isInput) {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		for(int i = 0; i < vector.getDimension(); i++) {
			if(isInput) {
				// format input vectors like 0.00
				builder.append(String.format("%.2f",vector.getEntry(i)));
			} else {
				// format output/target vectors like 0.00000
				builder.append(String.format("%.5f",vector.getEntry(i)));
			}
			if(i < vector.getDimension() - 1) {
				builder.append("; ");
			}
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Initializes the window for a new system model.
	 *
	 * @param model the model
	 */
	private void initializeModel(SystemModel model) {
		log("initialized", model==null? "null" : 
			"name=\"" + model.getName() + "\"; target=" 
				+ vectorToString(model.getTargetVector(), false));
		
		// remove all existing components from model panel
		modelPanel.removeAll();
		
		if(model == null) {
			// a null model could mean that no experiment is loaded, an 
			// experiment has not yet been started, or an experiment
			// is complete. handle each case below
			if(experiment == null) {
				// if experiment is null, set model label
				activeModelLabel.setText("");
				mode = Mode.Ready;
			} else if(experiment.isReady()) {
				// if experiment is ready, set model label
				activeModelLabel.setText("Ready...");
				mode = Mode.Ready;
			} else if(experiment.isComplete()){
				// if experiment is complete, set model label
				activeModelLabel.setText("Complete!");
				mode = Mode.Complete;
			}
			// define zero-length arrays for all other components
			outputPanels = new OutputPanel[0];
			inputPanels = new InputPanel[0];
			inputSeries = new XYSeries[0];
			solutionSeries = new XYSeries[0];
		} else {
			// set model label
			activeModelLabel.setText("Task: " + model.getName());
			
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
			
			// create and lay out output panels horizontally on the left
			// side of the panel
			outputPanels = new OutputPanel[model.getNumberOutputs()];
			for(int d = 0; d < model.getOutputIndices().length; d++) {
				// for each designer: handle outputs
				for(int d_o = 0; 
						d_o < model.getOutputIndices()[d].length; d_o++) {
					// for each designer output: create and add 
					// corresponding output panel
					int i = model.getOutputIndices()[d][d_o];
					outputPanels[i] = OutputPanelFactory.createBasicOutputPanel(d, d_o, 
							model.getTargetVector().getEntry(i),
							model.getOutputVector(
									model.getInitialVector()).getEntry(i),
									model.getOutputLabels()[i]);
					outputPanel.add(outputPanels[i]);
				}
			}

			// lay out output panels
			modelPanel.add(new JScrollPane(outputPanel), c);

			// lay out input panels
			
			JPanel inputPanel = new JPanel();
			inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.LINE_AXIS));

			// create and lay out input panels vertically to the right of the
			// output panels
			inputPanels = new InputPanel[model.getNumberInputs()];
			// clear out input dataset and other plotting data
			inputDataset.removeAllSeries();
			inputSeries = new XYSeries[model.getNumberInputs()];
			solutionSeries = new XYSeries[model.getNumberInputs()];
			errorSeries.clear();
			XYPlot xyPlot = (XYPlot) inputChart.getPlot();
			XYItemRenderer renderer = xyPlot.getRenderer();
			for(int d = 0; d < model.getInputIndices().length; d++) {
				// for each designer: handle inputs
				for(int d_i = 0; 
						d_i < model.getInputIndices()[d].length; d_i++) {
					// for each designer input: create and add 
					// corresponding input panel
					int i = model.getInputIndices()[d][d_i];
					inputPanels[i] = InputPanelFactory.createBasicInputPanelWithSolution(
							d, d_i, 
							model.getInitialVector().getEntry(i),
							model.getSolutionVector().getEntry(i),
							model.getInputLabels()[i]);
					inputPanel.add(inputPanels[i]);
					
					// also create and add series for input and solutions
					inputSeries[i] = new XYSeries(
							"X_" + (d+1) + "," + (d_i+1), 
							false, false);
					solutionSeries[i] = new XYSeries(
							"X_" + (d+1) + "," + (d_i+1) + "*", 
							false, false);
					inputDataset.addSeries(inputSeries[i]);
					inputDataset.addSeries(solutionSeries[i]);
					renderer.setSeriesShape(2*i + 0, 
							Utilities.getSeriesShape(d_i));
					renderer.setSeriesPaint(2*i + 0, 
							Utilities.getUserColor(d));
					renderer.setSeriesStroke(2*i + 0, 
							new BasicStroke(2f));
					renderer.setSeriesShape(2*i + 1, 
							new Ellipse2D.Double());
					renderer.setSeriesPaint(2*i + 1, 
							Utilities.getUserColor(d));
					renderer.setSeriesStroke(2*i + 1, 
							new BasicStroke(1f, 
									BasicStroke.CAP_SQUARE, 
									BasicStroke.JOIN_MITER, 
									10f, 
									new float[]{5f, 10f}, 
									0f));
				}
			}

			c.gridx++;
			modelPanel.add(new JScrollPane(inputPanel), c);

			// add error chart in new panel
			ChartPanel distanceChartPanel = new ChartPanel(errorChart);
			distanceChartPanel.setOpaque(false);
			// set preferred size to prevent large plots from being displayed
			distanceChartPanel.setPreferredSize(new Dimension(300,200));
			c.gridy++;
			c.gridx = 0;
			modelPanel.add(distanceChartPanel, c);
			// add input chart in new panel
			ChartPanel inputChartPanel = new ChartPanel(inputChart);
			inputChartPanel.setOpaque(false);
			// set preferred size to prevent large plots from being displayed
			inputChartPanel.setPreferredSize(new Dimension(300,200));
			c.gridx++;
			modelPanel.add(inputChartPanel, c);
			mode = Mode.Initialized;
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
		
		// set input components to be enabled/disabled
		logCommentText.setEnabled(experiment != null);
		logCommentAction.setEnabled(experiment != null);
		nextModelAction.setEnabled(experiment != null);
		endTrainingAction.setEnabled(experiment != null 
				&& experiment.isTraining());
		
		// tell the federate ambassador to update model attributes to reflect
		// the newly-loaded system model
		try {
			managerAmbassador.updateModelAttributes(experiment);
		} catch (Exception e) {
			showErrorDialog(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Logs a comment.
	 *
	 * @param message the message to log
	 */
	private void log(String label, String data) {
		try {
			// create writer objects
			FileWriter fw = new FileWriter(logFile, true);
			// write line with time, message, and newline
			fw.write(System.currentTimeMillis() + "," + label + "," 
					+ data + System.getProperty("line.separator"));
			// close the writer
			fw.close();
			System.out.println("Manager Log: " + data);
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Advances to the next model in the experiment.
	 */
	private void nextModel() {
		// make sure there are enough designers to actually complete the task
		if(designers.size() == experiment.getNumberDesigners()) {
			experiment.nextModel();
			initializeModel(experiment.getActiveModel());
		} else {
			showErrorDialog("There are not enough designers for the task " +
					"(need " + experiment.getNumberDesigners() + ").");
		}
	}
	
	/**
	 * Open an experiment from file.
	 */
	private void openExperiment() {
		// close existing experiment
		closeExperiment();
		
		if(experiment == null) {
			// create file chooser to browse for json file
			JFileChooser fileChooser = new JFileChooser(".");
			fileChooser.setFileFilter(
					new FileNameExtensionFilter("JSON files","json"));
			if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(this)) {
				// load experiment from file
				try {
					setExperiment(Utilities.readExperiment(
							fileChooser.getSelectedFile()));
					log("opened", experiment.getName());
				} catch (IOException e) {
					showErrorDialog(e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Resets the experiment.
	 */
	private void resetExperiment() {
		Experiment experiment = this.experiment;
		closeExperiment();
		if(this.experiment == null) {
			experiment.reset();
			setExperiment(experiment);
		}
	}
	
	/**
	 * Sets the experiment.
	 *
	 * @param experiment the new experiment
	 */
	public void setExperiment(Experiment experiment) {
		this.experiment = experiment;
		closeAction.setEnabled(experiment != null);
		resetAction.setEnabled(experiment != null);
		if(experiment==null) {
			initializeModel(null);
		} else {
			initializeModel(experiment.getActiveModel());
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
	 * Show information dialog.
	 *
	 * @param message the message
	 */
	private void showInformationDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "Information", 
				JOptionPane.INFORMATION_MESSAGE);
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
	 		GraphicsEnvironment.getLocalGraphicsEnvironment().
	 				getDefaultScreenDevice().setFullScreenWindow(this);
	 		setVisible(true);
		} else {
			// make windowed
			setVisible(false);
			dispose();
			setUndecorated(false);
			GraphicsEnvironment.getLocalGraphicsEnvironment().
					getDefaultScreenDevice().setFullScreenWindow(null);
	 		pack();
	 		setLocationRelativeTo(null);
			setVisible(true);
		}
	}
	
	/**
	 * Updates outputs if the correct conditions are observed.
	 */
	private void updateOutputs() {
		boolean sendUpdates = true;
		// if not in constant feedback mode, only send updates 
		// if all designers are ready
		for(Designer designer : designers) {
			sendUpdates = sendUpdates 
					&& designer.isReady() || constantFeedback;
		}
		// if in initialized mode, take note of the start time and make running
		if(sendUpdates && mode == Mode.Initialized) {
			mode = Mode.Running;
			startTime = new Date().getTime();
		}
		// if updates should be sent and in running mode:
		if(sendUpdates && mode == Mode.Running) {
			// aggregate all inputs from designers
			RealVector inputVector = new ArrayRealVector(inputPanels.length);
			for(int i = 0; i < inputPanels.length; i++) {
				inputVector.setEntry(i, inputPanels[i].getValue());
			}
			
			// calculate outputs
			RealVector outputVector = experiment.getActiveModel().
					getOutputVector(inputVector);
			
			// update output panels and determine if task is solved
			boolean solved = true;
			for(int i = 0; i < outputPanels.length; i++) {
				outputPanels[i].setValue(outputVector.getEntry(i));
				// task is solved if all outputs are within range
				solved = solved && outputPanels[i].isWithinRange();
			}
			
			double eventTime = (new Date().getTime()-startTime)/1000d;
			
			// add new entry to error series
			errorSeries.addOrUpdate(eventTime, 
					experiment.getActiveModel().getOutputError(inputVector));
			for(int i = 0; 
					i < experiment.getActiveModel().getNumberInputs(); 
					i++) {
				// add new entry to input series
				inputSeries[i].addOrUpdate(eventTime, 
						inputVector.getEntry(i));
				// add new entry to solution series
				solutionSeries[i].addOrUpdate(eventTime, 
						experiment.getActiveModel().getSolutionVector()
								.getEntry(i));
			}
			
			log("updated", "input=" + vectorToString(inputVector, true) 
					+ "; output=" + vectorToString(outputVector, false));
			
			// issue update call to federate ambassador
			try {
				managerAmbassador.updateOutputAttributes(outputVector);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			
			// if task is solved, update state and enabled actions
			if(solved) {
				try {
					final AudioInputStream inputStream;
					if(Math.random() < 0.5) {
						inputStream = AudioSystem.getAudioInputStream(
								getClass().getClassLoader().getResourceAsStream(
										"resources/success-1.wav"));
					} else {
						inputStream = AudioSystem.getAudioInputStream(
								getClass().getClassLoader().getResourceAsStream(
										"resources/success-2.wav"));
					}
					// run audio in separate thread
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Clip clip = AudioSystem.getClip();
								clip.open(inputStream);
								clip.start();
							} catch (LineUnavailableException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).run();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
				}
				
				log("solved", experiment.getActiveModel().getName());
				mode = Mode.Solved;
			}
		}
	}
}