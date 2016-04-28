package edu.mit.collab.util;

/**
 * A factory for creating InputPanel objects.
 */
public abstract class InputPanelFactory {
	
	/**
	 * Creates a new InputPanel object.
	 *
	 * @param designerIndex the designer index
	 * @param inputIndex the input index
	 * @param initialValue the initial value
	 * @param label the label
	 * @return the basic input panel
	 */
	public static BasicInputPanel createBasicInputPanelWithoutSolution(int designerIndex, 
			int inputIndex, double initialValue, String label) {
		return new BasicInputPanel(designerIndex, inputIndex, 
				initialValue, Double.MAX_VALUE, label);
	}

	/**
	 * Creates a new InputPanel object.
	 *
	 * @param designerIndex the designer index
	 * @param inputIndex the input index
	 * @param initialValue the initial value
	 * @param solutionValue the solution value
	 * @param label the label
	 * @return the basic input panel
	 */
	public static BasicInputPanel createBasicInputPanelWithSolution(int designerIndex, 
			int inputIndex, double initialValue, 
			double solutionValue, String label) {
		return new BasicInputPanel(designerIndex, inputIndex, 
				initialValue, solutionValue, label);
	}
}
