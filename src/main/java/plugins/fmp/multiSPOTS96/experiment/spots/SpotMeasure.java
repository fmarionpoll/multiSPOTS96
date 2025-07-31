package plugins.fmp.multiSPOTS96.experiment.spots;

import java.util.Arrays;
import java.util.Objects;

/**
 * Encapsulates spot measurements with clean separation of concerns and
 * validation.
 * 
 * <p>
 * This class provides comprehensive measurement capabilities for spots
 * including level2D data, values, presence indicators, and ROI management.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class SpotMeasure {

	// === CONSTANTS ===
//	private static final String DEFAULT_NAME = "no_name";
	private static final double DEFAULT_FACTOR = 1.0;
//	private static final int DEFAULT_SPAN = 5;
//	private static final double DEFAULT_THRESHOLD = 1.0;

	// === CORE FIELDS ===

	private double[] values;
	private double[] valuesNormalized;
	private int[] isPresent;
	private double factor;
	private int measuredFromNSpots = 1;
	private String name;

	// === CONSTRUCTORS ===

	/**
	 * Creates a new SpotMeasure with the specified name.
	 * 
	 * @param name the measure name
	 * @throws IllegalArgumentException if name is null
	 */
	public SpotMeasure(String name) {
		this.name = Objects.requireNonNull(name, "Name cannot be null");
		this.factor = DEFAULT_FACTOR;

	}

	// === CORE OPERATIONS ===

	/**
	 * Copies measurements from another SpotMeasure.
	 * 
	 * @param source the source measure
	 */
	public void copyMeasures(SpotMeasure source) {
		if (source == null) {
			return;
		}

		if (source.values != null && source.values.length > 0) {
			this.values = Arrays.copyOf(source.values, source.values.length);
		}

		if (source.isPresent != null && source.isPresent.length > 0) {
			this.isPresent = Arrays.copyOf(source.isPresent, source.isPresent.length);
		}
	}

	/**
	 * Adds measurements from another SpotMeasure.
	 * 
	 * @param source the source measure
	 */
	public void addMeasures(SpotMeasure source) {
		if (source == null) {
			return;
		}

		if (source.values != null && source.values.length > 0) {
			addValues(source.values);
		}

		if (source.isPresent != null && source.isPresent.length > 0) {
			addPresence(source.isPresent);
		}

		measuredFromNSpots++;
	}

	/**
	 * Computes PI (Performance Index) from two measures.
	 * 
	 * @param measure1 the first measure
	 * @param measure2 the second measure
	 */
	public void computePI(SpotMeasure measure1, int n1, SpotMeasure measure2, int n2) {
		if (measure1 == null || measure2 == null) {
			return;
		}

		if (measure1.values != null && measure1.values.length > 0 && measure2.values != null
				&& measure2.values.length > 0) {
			this.values = new double[measure1.valuesNormalized.length];
			for (int i = 0; i < measure1.valuesNormalized.length; i++) {
				double value1 = measure1.valuesNormalized[i] / (double) n1;
				double value2 = measure2.valuesNormalized[i] / (double) n2;
				double sum = value1 + value2;
				this.values[i] = sum > 0 ? (value1 - value2) / sum : 0;
			}
		}
	}

	/**
	 * Computes sum from two measures.
	 * 
	 * @param measure1 the first measure
	 * @param measure2 the second measure
	 */
	public void computeSUM(SpotMeasure measure1, int n1, SpotMeasure measure2, int n2) {
		if (measure1 == null || measure2 == null) {
			return;
		}

		if (measure1.values != null && measure1.values.length > 0 && measure2.values != null
				&& measure2.values.length > 0) {
			this.values = new double[measure1.values.length];
			for (int i = 0; i < measure1.values.length; i++) {
				this.values[i] = measure1.values[i] / (double) n1 + measure2.values[i] / (double) n2;
			}
		}
	}

	public double getMaximumValue() {
		double maximum = 0.;
		for (int i = 0; i < values.length; i++) {
			if (this.values[i] > maximum)
				maximum = this.values[i];
		}
		return maximum;
	}

	public void normalizeValuesTo(double norm) {
		for (int i = 0; i < values.length; i++) {
			values[i] = values[i] / norm;
		}
	}

	public void normalizeValues() {
		double maximum = getMaximumValue();
		normalizeValuesTo(maximum);
	}

	/**
	 * Combines presence indicators from two measures.
	 * 
	 * @param measure1 the first measure
	 * @param measure2 the second measure
	 */
	public void combineIsPresent(SpotMeasure measure1, int n1, SpotMeasure measure2, int n2) {
		if (measure1 == null || measure2 == null) {
			return;
		}

		if (isPresent != null && measure1.isPresent != null && measure2.isPresent != null) {
			if (isPresent.length != measure1.isPresent.length) {
				isPresent = new int[measure1.isPresent.length];
			}
			for (int i = 0; i < isPresent.length; i++) {
				isPresent[i] = measure1.isPresent[i] + measure2.isPresent[i];
			}
		}
	}

	// === NAME MANAGEMENT ===

	/**
	 * Gets the name of this measure.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this measure.
	 * 
	 * @param name the name
	 */
	public void setName(String name) {
		this.name = Objects.requireNonNull(name, "Name cannot be null");
	}

	// === DATA ACCESS ===

	/**
	 * Gets the values array.
	 * 
	 * @return the values array
	 */
	public double[] getValues() {
		return values;
	}

	/**
	 * Sets a single value in the values array.
	 * 
	 * @param index the index to set
	 * @param value the value to set
	 */
	public void setValueAt(int index, double value) {
		if (values != null && index >= 0 && index < values.length) {
			values[index] = value;
		}
	}

	/**
	 * Sets a single value in the values array.
	 * 
	 * @param index the index to set
	 * @param value the value to set
	 */
	public double getValueAt(int index) {
		return values[index];
	}

	/**
	 * Sets the values array.
	 * 
	 * @param values the values array
	 */
	public void setValues(double[] values) {
		this.values = values;
	}

	/**
	 * Gets the presence array.
	 * 
	 * @return the presence array
	 */
	public int[] getIsPresentArray() {
		return isPresent;
	}

	/**
	 * Sets the presence array.
	 * 
	 * @param isPresent the presence array
	 */
	public void setIsPresent(int[] isPresent) {
		this.isPresent = isPresent;
	}

	/**
	 * Sets a single value in the presence array.
	 * 
	 * @param index the index to set
	 * @param value the value to set
	 */
	public void setIsPresent(int index, int value) {
		if (isPresent != null && index >= 0 && index < isPresent.length) {
			isPresent[index] = value;
		}
	}

	/**
	 * Gets the factor.
	 * 
	 * @return the factor
	 */
	public double getFactor() {
		return factor;
	}

	/**
	 * Sets the factor.
	 * 
	 * @param factor the factor
	 */
	public void setFactor(double factor) {
		this.factor = factor;
	}

	public int getMeasuredFromNSpots() {
		return measuredFromNSpots;
	}

	public void setMeasuredFromNSpots(int n) {
		this.measuredFromNSpots = n;
	}

	// === PRIVATE HELPER METHODS ===

	private void addValues(double[] sourceValues) {
		if (this.values == null) {
			this.values = new double[sourceValues.length];
		}

		for (int i = 0; i < sourceValues.length; i++) {
			this.values[i] += sourceValues[i];
		}
	}

	private void addPresence(int[] sourcePresence) {
		if (this.isPresent == null) {
			this.isPresent = new int[sourcePresence.length];
		}

		for (int i = 0; i < sourcePresence.length; i++) {
			this.isPresent[i] += sourcePresence[i];
		}
	}

	// === UTILITY METHODS ===

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		SpotMeasure other = (SpotMeasure) obj;
		return Objects.equals(name, other.name) && Double.compare(factor, other.factor) == 0
				&& Arrays.equals(values, other.values) && Arrays.equals(isPresent, other.isPresent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, factor, Arrays.hashCode(values), Arrays.hashCode(isPresent));
	}

	@Override
	public String toString() {
		return String.format("SpotMeasure{name='%s', factor=%.2f, hasValues=%b, hasPresence=%b}", name, factor,
				values != null, isPresent != null);
	}
}
