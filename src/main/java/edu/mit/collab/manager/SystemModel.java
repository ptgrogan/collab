package edu.mit.collab.manager;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.google.gson.Gson;

/**
 * An object model for a linear system of inputs and outputs.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class SystemModel {
	private final String name;
	private final RealMatrix couplingMatrix;
	private final RealVector targetVector;
	private final int[][] inputIndices;
	private final int[][] outputIndices;
	private final String[] inputLabels;
	private final String[] outputLabels;

	/**
	 * Instantiates a new system model with default (trivial) values.
	 */
	public SystemModel() {
		name = "";
		couplingMatrix = new Array2DRowRealMatrix();
		targetVector = new ArrayRealVector(couplingMatrix.getRowDimension());
		inputIndices = new int[0][0];
		outputIndices = new int[0][0];
		inputLabels = new String[0];
		outputLabels = new String[0];
	}

	/**
	 * Instantiates a new system model.
	 *
	 * @param name the name
	 * @param couplingMatrix the coupling matrix
	 * @param targetVector the target vector
	 * @param inputIndices the input indices
	 * @param outputIndices the output indices
	 * @param inputLabels the input labels
	 * @param outputLabels the output labels
	 */
	public SystemModel(String name, RealMatrix couplingMatrix, 
			RealVector targetVector, int[][] inputIndices, 
			int[][] outputIndices, String[] inputLabels, String[] outputLabels) {
		if(couplingMatrix.getColumnDimension() < 1) {
			throw new IllegalArgumentException(
					"Coupling matrix must have at least 1 input variable.");
		}
		if(couplingMatrix.getRowDimension() < 1) {
			throw new IllegalArgumentException(
					"Coupling matrix must have at least 1 output variable.");
		}
		if(couplingMatrix.isSquare()) {
			throw new IllegalArgumentException(
					"Coupling matrix must be square for a unique solution.");
		}
		if(couplingMatrix.getColumnDimension() != targetVector.getDimension()) {
			throw new IllegalArgumentException(
					"The number of inputs must match the number of solution values.");
		}
		if(inputIndices.length != outputIndices.length) {
			throw new IllegalArgumentException(
					"The number of designers must be consistent between " +
					"input indices and output indices.");
		}
		this.name = name;
		this.couplingMatrix = couplingMatrix;
		this.targetVector = targetVector;
		boolean[] inputAssigned = new boolean[getNumberInputs()];
		for(int d = 0; d < inputIndices.length; d++) {
			for(int d_i = 0; d_i < inputIndices[d].length; d_i++) {
				if(inputAssigned[inputIndices[d][d_i]]) {
					throw new IllegalArgumentException("Input index " + inputIndices[d][d_i] 
							+ " can only be assigned to one designer.");
				}
				inputAssigned[inputIndices[d][d_i]] = true;
			}
		}
		for(int i = 0; i < inputAssigned.length; i++) {
			if(!inputAssigned[i]) {
				throw new IllegalArgumentException("Input index " + i 
						+ " must be assigned to a designer.");
			}
		}
		boolean[] outputAssigned = new boolean[getNumberOutputs()];
		for(int d = 0; d < outputIndices.length; d++) {
			for(int d_i = 0; d_i < outputIndices[d].length; d_i++) {
				if(outputAssigned[outputIndices[d][d_i]]) {
					throw new IllegalArgumentException("Output index " + outputIndices[d][d_i] 
									+ " can only be assigned to one designer.");
				}
				outputAssigned[outputIndices[d][d_i]] = true;
			}
		}
		for(int i = 0; i < outputAssigned.length; i++) {
			if(!outputAssigned[i]) {
				throw new IllegalArgumentException("Output index " + i 
						+ " must be assigned to a designer.");
			}
		}
		this.inputIndices = inputIndices;
		this.outputIndices = outputIndices;
		
		if(inputLabels.length != getNumberInputs()) {
			throw new IllegalArgumentException(
					"Input labels must have a label for each input.");
		}
		this.inputLabels = inputLabels;
		
		if(outputLabels.length != getNumberOutputs()) {
			throw new IllegalArgumentException(
					"Output labels must have a label for each output.");
		}
		this.outputLabels = outputLabels;
	}
	
	/**
	 * Gets the initial vector.
	 *
	 * @return the initial vector
	 */
	public RealVector getInitialVector() {
		return new ArrayRealVector(getNumberInputs());
	}
	
	/**
	 * Gets the input error.
	 *
	 * @param inputVector the input vector
	 * @return the input error
	 */
	public double getInputError(RealVector inputVector) {
		return getSolutionVector().getDistance(inputVector);
	}
	
	/**
	 * Gets the input indices.
	 *
	 * @return the input indices
	 */
	public int[][] getInputIndices() {
		return inputIndices;
	}
	
	/**
	 * Gets the input labels.
	 *
	 * @return the input labels
	 */
	public String[] getInputLabels() {
		return inputLabels;
	}
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the number designers.
	 *
	 * @return the number designers
	 */
	public int getNumberDesigners() {
		return inputIndices.length;
	}
	
	/**
	 * Gets the number inputs.
	 *
	 * @return the number inputs
	 */
	public int getNumberInputs() {
		return couplingMatrix.getColumnDimension();
	}
	
	/**
	 * Gets the number outputs.
	 *
	 * @return the number outputs
	 */
	public int getNumberOutputs() {
		return couplingMatrix.getRowDimension();
	}
	
	/**
	 * Gets the output error.
	 *
	 * @param inputVector the input vector
	 * @return the output error
	 */
	public double getOutputError(RealVector inputVector) {
		return targetVector.getDistance(getOutputVector(inputVector));
	}
	
	/**
	 * Gets the output indices.
	 *
	 * @return the output indices
	 */
	public int[][] getOutputIndices() {
		return outputIndices;
	}
	
	/**
	 * Gets the output labels.
	 *
	 * @return the output labels
	 */
	public String[] getOutputLabels() {
		return outputLabels;
	}
	
	/**
	 * Gets the output vector.
	 *
	 * @param inputVector the input vector
	 * @return the output vector
	 */
	public RealVector getOutputVector(RealVector inputVector) {
		return couplingMatrix.operate(inputVector);
	}
	
	/**
	 * Gets the solution vector.
	 *
	 * @return the solution vector
	 */
	public RealVector getSolutionVector() {
		return new LUDecomposition(couplingMatrix).getSolver().getInverse().operate(targetVector);
	}
	
	/**
	 * Gets the target vector.
	 *
	 * @return the target vector
	 */
	public RealVector getTargetVector() {
		return targetVector.copy();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		Gson gson = new Gson();
		return name + " (matrix: " + couplingMatrix 
				+ ", target: " + targetVector
				+ ", input indices: " + gson.toJson(inputIndices)
				+ ", output indices: " + gson.toJson(outputIndices) + ")";
	}
}
