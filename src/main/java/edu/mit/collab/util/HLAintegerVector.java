package edu.mit.collab.util;

import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.DataElementFactory;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAvariableArray;

/**
 * <code>HLAintegerVector</code> is a wrapper around the HLAvariableArray class 
 * to provide improved functionality for storing integer arrays in HLA data 
 * types. In particular, it provides <code>getValue</code> and 
 * <code>setValue</code> functions to automatically transform data to and from 
 * the <code>int[]</code> object classes.
 * 
 * @author Paul T. Grogan, ptgrogan@mit.edu
 */
public class HLAintegerVector implements DataElement {
	private final HLAvariableArray<HLAinteger32BE> hlaArray;
	
	/**
	 * Instantiates a new HLAintegerVector object.
	 *
	 * @param encoderFactory the HLA encoder factory
	 */
	public HLAintegerVector(final EncoderFactory encoderFactory) {
		// initialize the underlying HLA data type, which is a
		// variable array of integer values in this case
		hlaArray = encoderFactory.createHLAvariableArray(
				// define a data element factory to generate integer
				// data elements on demand
				new DataElementFactory<HLAinteger32BE>() {
					@Override
					public HLAinteger32BE createElement(int index) {
						return encoderFactory.createHLAinteger32BE();
					}
				});
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.encoding.DataElement#decode(byte[])
	 */
	@Override
	public void decode(byte[] bytes) throws DecoderException {
		// access built-in function of underlying data type
		hlaArray.decode(bytes);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.encoding.DataElement#decode(hla.rti1516e.encoding.ByteWrapper)
	 */
	@Override
	public void decode(ByteWrapper byteWrapper) throws DecoderException {
		// access built-in function of underlying data type
		hlaArray.decode(byteWrapper);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.encoding.DataElement#encode(hla.rti1516e.encoding.ByteWrapper)
	 */
	@Override
	public void encode(ByteWrapper byteWrapper) throws EncoderException {
		// access built-in function of underlying data type
		hlaArray.encode(byteWrapper);
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.encoding.DataElement#getEncodedLength()
	 */
	@Override
	public int getEncodedLength() {
		// access built-in function of underlying data type
		return hlaArray.getEncodedLength();
	}

	/* (non-Javadoc)
	 * @see hla.rti1516e.encoding.DataElement#getOctetBoundary()
	 */
	@Override
	public int getOctetBoundary() {
		// access built-in function of underlying data type
		return hlaArray.getOctetBoundary();
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public int[] getValue() {
		// create an integer array with the same size as the
		// underlying HLA array variable
		int[] vector = new int[hlaArray.size()];
		for(int i = 0; i < hlaArray.size(); i++) {
			// set each array entry from the HLA array
			vector[i] = hlaArray.get(i).getValue();
		}
		return vector;
	}
	
	/**
	 * Sets the value.
	 *
	 * @param vector the new value
	 */
	public void setValue(int[] vector) {
		// resize the HLA array to match the vector size
		hlaArray.resize(vector.length);
		for(int i = 0; i < vector.length; i++) {
			// set each array entry from the array
			hlaArray.get(i).setValue(vector[i]);
		}
	}
	
	/* (non-Javadoc)
	 * @see hla.rti1516e.encoding.DataElement#toByteArray()
	 */
	@Override
	public byte[] toByteArray() throws EncoderException {
		// access built-in function of underlying data type
		return hlaArray.toByteArray();
	}
}
