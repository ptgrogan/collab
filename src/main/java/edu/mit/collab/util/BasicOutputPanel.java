package edu.mit.collab.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSlider;

/**
 * The Class BasicOutputPanel.
 */
public class BasicOutputPanel extends OutputPanel {
	private static final long serialVersionUID = -3934341529444058089L;
	private static final int sliderTicksPerUnit = 1000;
	private static final double maxValue = 1.05d, minValue = -1.05d;
	private static final ImageIcon 
			withinRangeIcon = new ImageIcon(
					BasicOutputPanel.class.getClassLoader().getResource(
							"resources/tick.png")),
			outOfRangeIcon = new ImageIcon(
					BasicOutputPanel.class.getClassLoader().getResource(
							"resources/cross.png"));
	
	private final double targetOutput; // immutable
	private final JSlider outputSlider; // mutable
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
	public BasicOutputPanel(int designerIndex, int outputIndex, 
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
		
		// create and add signal label to the right side
		signalLabel = new JLabel();
		add(signalLabel, BorderLayout.EAST);
		
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
		outputSlider.setValue((int)(sliderTicksPerUnit*value));
		
		// check if output is within target range
		withinTargetRange = Math.abs(value - targetOutput) < ERROR_ALLOWED;
		
		// check if output is outside visible range
		boolean outOfRange = value < minValue || value > maxValue;
		
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
