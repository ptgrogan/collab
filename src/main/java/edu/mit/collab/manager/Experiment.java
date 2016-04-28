package edu.mit.collab.manager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An object class to store the state of an experimental trial. Includes the
 * system models for both training and experimentation phases as well as other
 * information such as the name and number of designers.
 * 
 * Also contains transient state on the current model and status of
 * experimentation.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class Experiment implements Serializable {
	private static enum Mode {READY, TRAINING, EXPERIMENT, COMPLETE}

	private static final long serialVersionUID = -3201662085297309780L;;
	
	private final String name; // immutable
	private final int numberDesigners; // immutable
	private final List<SystemModel> trainingModels; // immutable
	private final List<SystemModel> experimentModels; // immutable
	
	private transient Mode mode = Mode.READY; // mutable
	private transient SystemModel activeModel; // mutable
	
	/**
	 * Instantiates a new experiment with trivial parameters.
	 */
	public Experiment() {
		name = "";
		numberDesigners = 0;
		trainingModels = new ArrayList<SystemModel>();
		experimentModels = new ArrayList<SystemModel>();
	}
	
	/**
	 * Instantiates a new experiment.
	 *
	 * @param name the name
	 * @param numberDesigners the number designers
	 * @param trainingModels the training models
	 * @param experimentModels the experiment models
	 */
	public Experiment(String name, 
			int numberDesigners, 
			List<SystemModel> trainingModels, 
			List<SystemModel> experimentModels) {
		this.name = name;
		this.numberDesigners = numberDesigners;

		// validate training models
		for(SystemModel model : trainingModels) {
			if(model.getNumberDesigners() != numberDesigners) {
				throw new IllegalArgumentException(
						"Training model must have " + 
						numberDesigners + " designers.");
			}
		}
		// create new list to prevent unexpected modification
		this.trainingModels = new ArrayList<SystemModel>(trainingModels);
		
		// validate experiment models
		for(SystemModel model : experimentModels) {
			if(model.getNumberDesigners() != numberDesigners) {
				throw new IllegalArgumentException(
						"Experimental model must have " + 
						numberDesigners + " designers.");
			}
		}
		// create new list structure to prevent unexpected modification
		this.experimentModels = new ArrayList<SystemModel>(experimentModels);
		// randomize order of experiment models
		Collections.shuffle(this.experimentModels);
	}
	
	/**
	 * Ends the training mode and starts the experimentation mode.
	 */
	public void endTrainingMode() {
		activeModel = null;
		mode = Mode.EXPERIMENT;
	}
	
	/**
	 * Gets the active model.
	 *
	 * @return the active model
	 */
	public SystemModel getActiveModel() {
		return activeModel;
	}
	
	/**
	 * Gets the experiment name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the number of designers for this experiment.
	 *
	 * @return the number designers
	 */
	public int getNumberDesigners() {
		return numberDesigners;
	}
	
	/**
	 * Checks if the experiment is complete (i.e. all 
	 * experimental models have been solved).
	 *
	 * @return true, if is complete
	 */
	public boolean isComplete() {
		return mode == Mode.COMPLETE;
	}
	
	/**
	 * Checks if the experiment is ready to start.
	 *
	 * @return true, if is ready
	 */
	public boolean isReady() {
		return mode == Mode.READY;
	}
	
	/**
	 * Checks if the experiment is currently in the training mode
	 * (i.e. the active model is in the training set).
	 *
	 * @return true, if is training
	 */
	public boolean isTraining() {
		return trainingModels.contains(activeModel);
	}
	
	/**
	 * Advances the experiment to the next model, either from the
	 * training set or experimentation set depending on the current
	 * mode.
	 */
	public void nextModel() {
		if(mode == Mode.READY) {
			// start experiment with first model in training set
			if(trainingModels.size() > 0) {
				mode = Mode.TRAINING; // set training mode (in case ready)
				activeModel = trainingModels.get(0);
			} else {
				mode = Mode.EXPERIMENT;
				nextModel();
			}
		} else if(mode == Mode.TRAINING) {
			// advance to next training model, modulo the number
			// of training models to allow repeated runs through
			activeModel = trainingModels.get(
					(trainingModels.indexOf(activeModel) + 1) % 
					trainingModels.size());
		} else if(mode == Mode.EXPERIMENT) {
			// in experimentation mode
			if(activeModel == null) {
				// start with first experiment model
				if(experimentModels.size() > 0) {
					activeModel = experimentModels.get(0);
				} else {
					mode = Mode.COMPLETE;
				}
			} else if(experimentModels.size() 
					> experimentModels.indexOf(activeModel) + 1) {
				// else if there is another model, move to next model
				activeModel = experimentModels.get(
						experimentModels.indexOf(activeModel) + 1);
			} else {
				// otherwise the experiment is complete if all models have
				// been solved
				activeModel = null;
				mode = Mode.COMPLETE;
			}
		}
	}
	
	/**
	 * Reads an object from an input stream. Used for custom serialization 
	 * to restore transient fields.
	 *
	 * @param in the in
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		// reset experiment state (sets transient field values)
		reset();
	}
	
	/**
	 * Resets an experiment to the initial state.
	 */
	public void reset() {
		mode = Mode.READY;
		activeModel = null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return name + " (" + numberDesigners + " designers)";
	}
}
