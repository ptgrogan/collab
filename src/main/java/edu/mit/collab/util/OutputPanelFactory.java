package edu.mit.collab.util;

/**
 * A factory for creating OutputPanel objects.
 */
public abstract class OutputPanelFactory {
	
	/**
	 * Creates a new OutputPanel object.
	 *
	 * @param designerIndex the designer index
	 * @param outputIndex the output index
	 * @param targetOutput the target output
	 * @param outputValue the output value
	 * @param label the label
	 * @return the basic output panel
	 */
	public static BasicOutputPanel createBasicOutputPanel(int designerIndex, 
			int outputIndex, double targetOutput, double outputValue, 
			String label) {
		return new BasicOutputPanel(designerIndex, outputIndex, 
				targetOutput, outputValue, label);
	}

	/**
	 * Creates a new OutputPanel object.
	 *
	 * @param designerIndex the designer index
	 * @param outputIndex the output index
	 * @param targetOutput the target output
	 * @param outputValue the output value
	 * @param label the label
	 * @return the quantitative output panel
	 */
	public static QuantitativeOutputPanel createQuantitativeOutputPanel(int designerIndex, 
			int outputIndex, double targetOutput, double outputValue, 
			String label) {
		return new QuantitativeOutputPanel(designerIndex, outputIndex, 
				targetOutput, outputValue, label);
	}

	/**
	 * Creates a new OutputPanel object.
	 *
	 * @param designerIndex the designer index
	 * @param outputIndex the output index
	 * @param targetOutput the target output
	 * @param outputValue the output value
	 * @param label the label
	 * @return the manual quantitative output panel
	 */
	public static ManualQuantitativeOutputPanel createManualQuantitativeOutputPanel(
			int designerIndex, int outputIndex, double targetOutput, 
			double outputValue, String label) {
		return new ManualQuantitativeOutputPanel(designerIndex, outputIndex, 
				targetOutput, outputValue, label);
	}
}
