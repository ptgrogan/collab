package edu.mit.collab.util;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.swing.ImageIcon;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.mit.collab.manager.Experiment;

/**
 * An abstract class used to access utility functions such as JSON
 * reading and writing.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public abstract class Utilities {
    public static String PROPERTIES_PATH = "resources/config.properties";
    
	// an array of user icons to represent designers
	// indexed by designer number
	private static final ImageIcon[] userIcons = new ImageIcon[]{
		new ImageIcon(Utilities.class.getClassLoader()
				.getResource("resources/user_red.png")),
		new ImageIcon(Utilities.class.getClassLoader()
				.getResource("resources/user_green.png")),
		new ImageIcon(Utilities.class.getClassLoader()
				.getResource("resources/user.png")),
		new ImageIcon(Utilities.class.getClassLoader()
				.getResource("resources/user_female.png")),
		new ImageIcon(Utilities.class.getClassLoader()
				.getResource("resources/user_orange.png")),
		new ImageIcon(Utilities.class.getClassLoader()
				.getResource("resources/user_gray.png"))
		};
	
	// an array of colors to represent designers indexed by designer number
	private static final Color[] userColors = new Color[]{Color.red, 
		Color.green, Color.blue, Color.pink, Color.orange, Color.gray};
	
	// an array of shapes to represent different series indexed by number
	private static final Shape[] seriesShapes = new Shape[]{
			new Ellipse2D.Double(-4,-4,8,8), 
			new Rectangle(-4,-4,8,8),
			new Polygon(new int[]{-4,0,4}, new int[]{3,-6,3},3)};
	
	public static Gson getGson() {
	  // a Gson builder object which is configured to work with the
	  // mathematical vector and matrix classes
	  return new GsonBuilder()
	      // register a type adapter to serialize RealMatrix objects
	      .registerTypeAdapter(RealMatrix.class, 
	          new JsonSerializer<RealMatrix>() {
	        @Override
	        public JsonElement serialize(RealMatrix src, Type typeOfSrc,
	            JsonSerializationContext context) {
	          // create empty JSON data array for rows
	          JsonArray matrix = new JsonArray();
	          for(int i = 0; i < src.getRowDimension(); i++) {
	            // create empty JSON data array for column
	            JsonArray vector = new JsonArray();
	            for(int j = 0; j < src.getColumnDimension(); j++) {
	              // set row i, column j from source matrix
	              vector.add(new JsonPrimitive(src.getEntry(i, j)));
	            }
	            // add row vector to matrix
	            matrix.add(vector);
	          }
	          return matrix;
	        }
	      })
	      // register a type adapter to deserialize RealMatrix objects
	      .registerTypeAdapter(RealMatrix.class, 
	          new JsonDeserializer<RealMatrix>() {
	        @Override
	        public RealMatrix deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context)
	                throws JsonParseException {
	          // convert JSON element to JSON array
	          JsonArray matrix = json.getAsJsonArray();
	          // create empty double matrix with same number of rows
	          double[][] data = new double[matrix.size()][];
	          for(int i = 0; i < matrix.size(); i++) {
	            // get the ith row in the JSON array
	            JsonArray vector = matrix.get(i).getAsJsonArray();
	            // create empty double array with same number of columns
	            data[i] = new double[vector.size()];
	            for(int j = 0; j < vector.size(); j++) {
	              // set row i, column j from row vector entry
	              data[i][j] = vector.get(j).getAsDouble();
	            }
	          }
	          // return properly-typed RealMatrix object
	          return new Array2DRowRealMatrix(data);
	        }
	      })
	      // register a type adapter to create RealMatrix objects
	      .registerTypeAdapter(RealMatrix.class, 
	          new InstanceCreator<RealMatrix>() {
	        @Override
	        public RealMatrix createInstance(Type type) {
	          // return a new RealMatrix object
	          return new Array2DRowRealMatrix();
	        }
	      })
	      // register a type adapter to serialize RealVector objects
	      .registerTypeAdapter(RealVector.class, 
	          new JsonSerializer<RealVector>() {
	        @Override
	        public JsonElement serialize(RealVector src, Type typeOfSrc,
	            JsonSerializationContext context) {
	          // create an empty JSON array
	          JsonArray vector = new JsonArray();
	          for(int i = 0; i < src.getDimension(); i++) {
	            // set ith entry from source vector
	            vector.add(new JsonPrimitive(src.getEntry(i)));
	          }
	          return vector;
	        }
	      })
	      // register a type adapter to deserialize RealVector objects
	      .registerTypeAdapter(RealVector.class, 
	          new JsonDeserializer<RealVector>() {
	        @Override
	        public RealVector deserialize(JsonElement json, Type typeOfT,
	            JsonDeserializationContext context)
	                throws JsonParseException {
	          // parse the JSON element as a JSON array
	          JsonArray vector = json.getAsJsonArray();
	          // create a new double array with the same size
	          double[] data = new double[vector.size()];
	          for(int i = 0; i < vector.size(); i++) {
	            // set the ith vector entry from the JSON array
	            data[i] = vector.get(i).getAsDouble();
	          }
	          // return a properly-typed RealVector object
	          return new ArrayRealVector(data);
	        }
	      })
	      // register a type adapter to create RealVector objects
	      .registerTypeAdapter(RealVector.class, 
	          new InstanceCreator<RealVector>() {
	        @Override
	        public RealVector createInstance(Type type) {
	          // return a new RealVector object
	          return new ArrayRealVector();
	        }
	      })
	      // create gson object
	      .create();
	}
	
	/**
	 * Gets the series shape.
	 *
	 * @param seriesIndex the series index
	 * @return the series shape
	 */
	public static Shape getSeriesShape(int seriesIndex) {
		// return the ith shape, where i is the series index
		// modulo the number of shapes. this makes the shapes repeat
		// only after all shapes have been used
		return seriesShapes[seriesIndex % seriesShapes.length];
	}
	
	/**
	 * Gets the user color.
	 *
	 * @param designerIndex the designer index
	 * @return the user color
	 */
	public static Color getUserColor(int designerIndex) {
		// return the ith color, where i is the designer index
		// modulo the number of colors. this makes the colors repeat
		// only after all colors have been used by designers
		return userColors[designerIndex % userColors.length];
	}

	/**
	 * Gets the user icon.
	 *
	 * @param designerIndex the designer index
	 * @return the user icon
	 */
	public static ImageIcon getUserIcon(int designerIndex) {
		// return the ith user icon, where i is the designer index
		// modulo the number of user icons. this makes the icons repeat
		// only after all different icons have been used by designers
		return userIcons[designerIndex % userIcons.length];
	}
	
	/**
	 * Reads a JSON-formatted experiment object from file.
	 *
	 * @param filepath the filepath
	 * @return the experiment
	 * @throws IOException 
	 */
	public static Experiment readExperiment(File file) throws IOException {
		// create a string-builder to efficiently read in JSON data
		StringBuilder jsonBuilder = new StringBuilder();
		// create file reader and buffered reader
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line;
		// do while the next line is not null (not reached end of file)
		while((line = br.readLine()) != null) {
			// append line to string builder
			jsonBuilder.append(line);
		}
		// closer readers
		br.close();
		fr.close();
		// parse JSON into Experiment object using custom GSON object
		return getGson().fromJson(jsonBuilder.toString(), Experiment.class);
	}
	
	/**
	 * Writes an Experiment object to a JSON-formatted file.
	 *
	 * @param experiment the experiment
	 * @param file the file
	 */
	public static void writeExperiment(Experiment experiment, File file) 
			throws IOException {
		// create a file writer and buffered writer
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		// write the JSON-ified experiment to file
		bw.write(getGson().toJson(experiment));
		// flush and close writers
		bw.flush();
		bw.close();
		fw.close();
	}
}