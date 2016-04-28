package edu.mit.collab.designer;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.mit.collab.util.OutputPanel;

/**
 * The Manager class is used by a designer to maintain
 * data on a remote manager. It must be initialized with
 * an immutable instance name (upon discovery in the HLA ambassador)
 * however the other data members can be modified.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class Manager {
	private final String instanceName; // immutable
	
	private String activeModel; // mutable
	private RealVector initialInput; // mutable
	private RealVector targetOutput; // mutable
	private RealVector output; // mutable
	private int[][] inputIndices; // mutable
	private int[][] outputIndices; // mutable
	private String[] inputLabels; // mutable
	private String[] outputLabels; // mutable
	
	/**
	 * Instantiates a new manager.
	 */
	public Manager(String instanceName) {
		this.instanceName = instanceName;
		activeModel = "";
		initialInput = new ArrayRealVector();
		targetOutput = new ArrayRealVector();
		output = new ArrayRealVector();
		inputIndices = new int[0][0];
		outputIndices = new int[0][0];
		inputLabels = new String[0];
		outputLabels = new String[0];
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public synchronized boolean equals(Object o) {
		// managers are equal if they have the same instance name
		return (o instanceof Manager) 
				&& instanceName.equals(((Manager)o).getInstanceName());
	}
	
	/**
	 * Gets the active model.
	 *
	 * @return the active model
	 */
	public synchronized String getActiveModel() {
		return activeModel;
	}
	
	/**
	 * Gets the initial input.
	 *
	 * @param designerIndex the designer index
	 * @return the initial input
	 */
	public synchronized RealVector getInitialInput(int designerIndex) {
		// if input indices are incorrectly sized, return an empty vector
		if(inputIndices.length - 1 < designerIndex) {
			return new ArrayRealVector();
		}
		
		// create a new vector, set the entries, and return
		RealVector initialInputs = new ArrayRealVector(
				inputIndices[designerIndex].length);
		for(int i = 0; i < inputIndices[designerIndex].length; i++) {
			initialInputs.setEntry(i, initialInput.getEntry(
					inputIndices[designerIndex][i]));
		}
		return initialInputs;
	}
	
	/**
	 * Gets the input labels.
	 *
	 * @param designerIndex the designer index
	 * @return the input labels
	 */
	public synchronized String[] getInputLabels(int designerIndex) {
		if(inputIndices.length - 1 < designerIndex) {
			return new String[0];
		}
		
		String[] labels = new String[inputIndices[designerIndex].length];
		for(int i = 0; i < inputIndices[designerIndex].length; i++) {
			labels[i] = inputLabels[inputIndices[designerIndex][i]];
		}
		return labels;
	}
	
	/**
	 * Gets the instance name.
	 *
	 * @return the instance name
	 */
	public synchronized String getInstanceName() {
		return instanceName;
	}
	
	/**
	 * Gets the output.
	 *
	 * @param designerIndex the designer index
	 * @return the output
	 */
	public synchronized RealVector getOutput(int designerIndex) {
		// if output indices are incorrectly sized, return an empty vector
		if(outputIndices.length - 1 < designerIndex) {
			return new ArrayRealVector();
		}
		
		// create a new vector, set the entries, and return
		RealVector outputs = new ArrayRealVector(
				outputIndices[designerIndex].length);
		for(int i = 0; i < outputIndices[designerIndex].length; i++) {
			outputs.setEntry(i, 
					output.getEntry(outputIndices[designerIndex][i]));
		}
		return outputs;
	}
	
	/**
	 * Gets the output labels.
	 *
	 * @param designerIndex the designer index
	 * @return the output labels
	 */
	public synchronized String[] getOutputLabels(int designerIndex) {
		if(outputIndices.length - 1 < designerIndex) {
			return new String[0];
		}
		
		String[] labels = new String[outputIndices[designerIndex].length];
		for(int i = 0; i < outputIndices[designerIndex].length; i++) {
			labels[i] = outputLabels[outputIndices[designerIndex][i]];
		}
		return labels;
	}
	
	/**
	 * Gets the target output.
	 *
	 * @param designerIndex the designer index
	 * @return the target output
	 */
	public synchronized RealVector getTargetOutput(int designerIndex) {
		// if output indices are incorrectly sized, return an empty vector
		if(outputIndices.length - 1 < designerIndex) {
			return new ArrayRealVector();
		}
		
		// create a new vector, set the entries, and return
		RealVector targetOutputs = new ArrayRealVector(
				outputIndices[designerIndex].length);
		for(int i = 0; i < outputIndices[designerIndex].length; i++) {
			targetOutputs.setEntry(i, targetOutput.getEntry(
					outputIndices[designerIndex][i]));
		}
		return targetOutputs;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public synchronized int hashCode() {
		// provide new hashcode function to conform to equals method
		return instanceName.hashCode();
	}
	
	/**
	 * Checks if is solved.
	 *
	 * @return true, if is solved
	 */
	public synchronized boolean isSolved() { 
		// task can only be solved if there is at least one output
		boolean solved = targetOutput.getDimension() > 0;
		
		// task is solved if every output is within range of target output
		for(int i = 0; i < targetOutput.getDimension(); i++) {
			if(Math.abs(output.getEntry(i)-targetOutput.getEntry(i)) >= OutputPanel.ERROR_ALLOWED) {
				solved = false;
			}
		}
		return solved;
	}
	
	/**
	 * Sets the active model.
	 *
	 * @param activeModel the new active model
	 */
	public synchronized void setActiveModel(String activeModel) {
		this.activeModel = activeModel;
	}
	
	/**
	 * Sets the initial input.
	 *
	 * @param initialInput the new initial input
	 */
	public synchronized void setInitialInput(RealVector initialInput) {
		// set a copy of the initial input to protect
		// against unexpected modification
		this.initialInput = initialInput.copy();
	}

	/**
	 * Sets the input indices.
	 *
	 * @param inputIndices the new input indices
	 */
	public synchronized void setInputIndices(int[][] inputIndices) {
		this.inputIndices = inputIndices;
	}
	
	/**
	 * Sets the input labels.
	 *
	 * @param inputLabels the new input labels
	 */
	public synchronized void setInputLabels(String[] inputLabels) {
		this.inputLabels = inputLabels;
	}
	
	/**
	 * Sets the output.
	 *
	 * @param output the new output
	 */
	public synchronized void setOutput(RealVector output) {
		// set a copy of the output to protect
		// against unexpected modification
		this.output = output.copy();
	}

	/**
	 * Sets the output indices.
	 *
	 * @param outputIndices the new output indices
	 */
	public synchronized void setOutputIndices(int[][] outputIndices) {
		this.outputIndices = outputIndices;
	}

	/**
	 * Sets the output labels.
	 *
	 * @param outputLabels the new output labels
	 */
	public synchronized void setOutputLabels(String[] outputLabels) {
		this.outputLabels = outputLabels;
	}
	
	/**
	 * Sets the target output.
	 *
	 * @param targetOutput the new target output
	 */
	public synchronized void setTargetOutput(RealVector targetOutput) {
		// set a copy of the target output to protect
		// against unexpected modification
		this.targetOutput = targetOutput.copy();
	}
}
