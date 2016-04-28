package edu.mit.collab.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * The Class ManualQuantitativeOutputPanel.
 */
public class ManualQuantitativeOutputPanel extends OutputPanel {
	private static final long serialVersionUID = -3934341529444058089L;
	private static final int sliderTicksPerUnit = 1000;
	private static final double maxValue = 1.05d, minValue = -1.05d;
	private static final ImageIcon 
			withinRangeIcon = new ImageIcon(
					BasicOutputPanel.class.getClassLoader().getResource(
							"resources/tick.png")),
			outOfRangeIcon = new ImageIcon(
					BasicOutputPanel.class.getClassLoader().getResource(
							"resources/cross.png")),
			calculatorIcon = new ImageIcon(
					BasicOutputPanel.class.getClassLoader().getResource(
							"resources/calculator.png"));
	
	private double currentOutput; // mutable
	private final double targetOutput; // immutable
	private final JSlider outputSlider; // mutable
	private final Action calculateAction; // mutable
	private final JLabel scoreLabel; // mutable
	private final JLabel signalLabel; // mutable
	private boolean withinTargetRange; // mutable
	
	/**
	 * Instantiates a new output panel.
	 *
	 * @param designerIndex the designer index
	 * @param outputIndex the output index
	 * @param targetOutput the target output
	 * @param outputValue the output value
	 */
	public ManualQuantitativeOutputPanel(int designerIndex, int outputIndex, 
			final double targetOutput, double outputValue, String label) {
		this.targetOutput = targetOutput;
		
		// set layout
		setLayout(new BorderLayout());
		
		// create and add output slider to the center
		outputSlider = new JSlider((int)(sliderTicksPerUnit*minValue), 
				(int)(sliderTicksPerUnit*maxValue)) {
			private static final long serialVersionUID = -1430242296899154851L;
			
			/* (non-Javadoc)
			 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
			 */
			@Override
			public void paintComponent(Graphics g) {
				Graphics g2 = g.create();
				g2.setColor(Color.green);
				// subtract padding to left and right of slider bar, estimated at 6px
				double pixelsPerSliderTick = (getWidth() - 2*6d)/(getMaximum()-getMinimum());
				double targetOffset = targetOutput*sliderTicksPerUnit*pixelsPerSliderTick;
				double targetWidth = 2*ERROR_ALLOWED*sliderTicksPerUnit*pixelsPerSliderTick;
				g2.fillRect((int) (getWidth()/2d + targetOffset - targetWidth/2d), 0, (int) targetWidth, getHeight());
				g2.dispose();
				super.paintComponent(g);
			}
		};
		outputSlider.setOrientation(JSlider.HORIZONTAL);
		outputSlider.setEnabled(false);
		outputSlider.setOpaque(false);
		add(outputSlider, BorderLayout.CENTER);
		
		// create and add variable label to the left side
		if(label==null) {
			label = "<html>Y<sub>" + (designerIndex+1) + "," 
					+ (outputIndex+1) + "</sub></html>";
		}
		add(new JLabel(label, Utilities.getUserIcon(designerIndex), 
				JLabel.CENTER), BorderLayout.WEST);
		
		// create and add panel on right side
		JPanel eastPanel = new JPanel();
		eastPanel.setLayout(new FlowLayout());
		add(eastPanel, BorderLayout.EAST);
		
		calculateAction = new AbstractAction(null, calculatorIcon) {
			private static final long serialVersionUID = 280751888521797357L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// update score label
				scoreLabel.setText(String.format("%5.0f",Math.max(0, 
						1e4 - 1e4*Math.abs(currentOutput - targetOutput)/2)));
				
				// disable action
				calculateAction.setEnabled(false);
			}
		};
		JButton calculateButton = new JButton(calculateAction);
		eastPanel.add(calculateButton);
		
		// create and add score label to the right side
		scoreLabel = new JLabel("     ");
		scoreLabel.setHorizontalAlignment(JLabel.CENTER);
		scoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
		eastPanel.add(scoreLabel);
		
		// create and add signal label to the right side
		signalLabel = new JLabel();
		eastPanel.add(signalLabel);
		
		// initialize the value of the slider
		setValue(outputValue);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		outputSlider.setEnabled(enabled);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.util.OutputPanel#setValue(double)
	 */
	@Override
	public void setValue(double value) {
		
		if(currentOutput != value) {
			// update score label
			scoreLabel.setText("     ");
			
			// update calculate action
			calculateAction.setEnabled(true);
		}
		currentOutput = value;
		
		outputSlider.setValue((int)(sliderTicksPerUnit*currentOutput));
		
		// check if output is within target range
		withinTargetRange = Math.abs(currentOutput - targetOutput) < ERROR_ALLOWED;
		
		// check if output is outside visible range
		boolean outOfRange = currentOutput < minValue || currentOutput > maxValue;
		
		
		// update signal label
		signalLabel.setIcon(withinTargetRange?withinRangeIcon:outOfRangeIcon);
		
		// update background color
		setBackground(withinTargetRange?
				new Color(0xcc,0xff,0xcc):outOfRange?
						new Color(0xcc,0xcc,0xcc):
							new Color(0xff,0xcc,0xcc));
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.util.OutputPanel#isWithinRange()
	 */
	@Override
	public boolean isWithinRange() {
		return withinTargetRange;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.collab.util.OutputPanel#getValue()
	 */
	@Override
	public double getValue() {
		return outputSlider.getValue()/((double)sliderTicksPerUnit);
	}
}
