package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multiSPOTS96.tools.polyline.Level2D;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

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
	private static final String DEFAULT_NAME = "no_name";
	private static final double DEFAULT_FACTOR = 1.0;
	private static final int DEFAULT_SPAN = 5;
	private static final double DEFAULT_THRESHOLD = 1.0;

	// === CORE FIELDS ===
	private Level2D level2D;
	private Level2D level2DOld;
	private double[] values;
	private int[] isPresent;
	private ROI2DPolyLine roi;
	private double factor;
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
		this.level2D = new Level2D();
		this.level2DOld = new Level2D();
	}

	/**
	 * Creates a new SpotMeasure with the specified name and limit points.
	 * 
	 * @param name  the measure name
	 * @param limit the limit points
	 * @throws IllegalArgumentException if name is null
	 */
	public SpotMeasure(String name, List<Point2D> limit) {
		this(name);
		if (limit != null && !limit.isEmpty()) {
			setLevel2D(new Level2D(limit));
		}
	}

	// === CORE OPERATIONS ===

	/**
	 * Copies level2D data from another SpotMeasure.
	 * 
	 * @param source the source measure
	 */
	public void copyLevel2D(SpotMeasure source) {
		if (source != null && source.getLevel2D() != null) {
			setLevel2D(source.getLevel2D().clone());
		}
	}

	/**
	 * Copies measurements from another SpotMeasure.
	 * 
	 * @param source the source measure
	 */
	public void copyMeasures(SpotMeasure source) {
		if (source == null) {
			return;
		}

		copyLevel2D(source);

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

		if (level2D != null && source.level2D != null) {
			level2D.add_Y(source.level2D);
		}

		if (source.values != null && source.values.length > 0) {
			addValues(source.values);
		}

		if (source.isPresent != null && source.isPresent.length > 0) {
			addPresence(source.isPresent);
		}
	}

	/**
	 * Computes PI (Performance Index) from two measures.
	 * 
	 * @param measure1 the first measure
	 * @param measure2 the second measure
	 */
	public void computePI(SpotMeasure measure1, SpotMeasure measure2) {
		if (measure1 == null || measure2 == null) {
			return;
		}

		if (level2D != null && measure1.level2D != null && measure2.level2D != null) {
			if (level2D.npoints != measure1.level2D.npoints) {
				level2D = new Level2D(measure1.level2D.npoints);
			}
			level2D.computePI_Y(measure1.level2D, measure2.level2D);
		}

		if (measure1.values != null && measure1.values.length > 0 && measure2.values != null
				&& measure2.values.length > 0) {
			this.values = new double[measure1.values.length];
			for (int i = 0; i < measure1.values.length; i++) {
				double sum = measure1.values[i] + measure2.values[i];
				this.values[i] = sum > 0 ? (measure1.values[i] - measure2.values[i]) / sum : 0;
			}
		}
	}

	/**
	 * Computes sum from two measures.
	 * 
	 * @param measure1 the first measure
	 * @param measure2 the second measure
	 */
	public void computeSUM(SpotMeasure measure1, SpotMeasure measure2) {
		if (measure1 == null || measure2 == null) {
			return;
		}

		if (level2D != null && measure1.level2D != null && measure2.level2D != null) {
			if (level2D.npoints != measure1.level2D.npoints) {
				level2D = new Level2D(measure1.level2D.npoints);
			}
			level2D.computeSUM_Y(measure1.level2D, measure2.level2D);
		}

		if (measure1.values != null && measure1.values.length > 0 && measure2.values != null
				&& measure2.values.length > 0) {
			this.values = new double[measure1.values.length];
			for (int i = 0; i < measure1.values.length; i++) {
				this.values[i] = measure1.values[i] + measure2.values[i];
			}
		}
	}

	/**
	 * Combines presence indicators from two measures.
	 * 
	 * @param measure1 the first measure
	 * @param measure2 the second measure
	 */
	public void combineIsPresent(SpotMeasure measure1, SpotMeasure measure2) {
		if (measure1 == null || measure2 == null) {
			return;
		}

		if (level2D != null && measure1.level2D != null && measure2.level2D != null) {
			if (level2D.npoints != measure1.level2D.npoints) {
				level2D = new Level2D(measure1.level2D.npoints);
			}
			level2D.computeIsPresent_Y(measure1.level2D, measure2.level2D);
		}
	}

	// === LEVEL2D MANAGEMENT ===

	/**
	 * Clears the level2D data.
	 */
	public void clearLevel2D() {
		setLevel2D(new Level2D());
	}

	/**
	 * Initializes level2D from measure values.
	 * 
	 * @param name the name for the level2D
	 */
	public void initLevel2DFromMeasureValues(String name) {
		setName(name);

		if (values == null || values.length == 0) {
			return;
		}

		int npoints = values.length;
		double[] xpoints = new double[npoints];
		double[] ypoints = new double[npoints];

		for (int i = 0; i < npoints; i++) {
			xpoints[i] = i;
			ypoints[i] = values[i];
		}

		setLevel2D(new Level2D(xpoints, ypoints, npoints));
	}

	/**
	 * Initializes level2D from boolean values.
	 * 
	 * @param name the name for the level2D
	 */
	public void initLevel2DFromBooleans(String name) {
		setName(name);

		if (isPresent == null || isPresent.length == 0) {
			return;
		}

		int npoints = isPresent.length;
		double[] xpoints = new double[npoints];
		double[] ypoints = new double[npoints];

		for (int i = 0; i < npoints; i++) {
			xpoints[i] = i;
			ypoints[i] = isPresent[i] > 0 ? 1.0 : 0.0;
		}

		setLevel2D(new Level2D(xpoints, ypoints, npoints));
	}

	/**
	 * Gets the number of points in level2D.
	 * 
	 * @return the number of points
	 */
	public int getLevel2DNPoints() {
		return level2D != null ? level2D.npoints : 0;
	}

	/**
	 * Gets the level2D data.
	 * 
	 * @return the level2D
	 */
	public Level2D getLevel2D() {
		return level2D;
	}

	/**
	 * Sets the level2D data.
	 * 
	 * @param level2D the level2D to set
	 */
	public void setLevel2D(Level2D level2D) {
		this.level2D = level2D;
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

	// === ROI MANAGEMENT ===

	/**
	 * Gets the ROI associated with this measure.
	 * 
	 * @return the ROI
	 */
	public ROI2DPolyLine getRoi() {
		return roi;
	}

	/**
	 * Sets the ROI for this measure.
	 * 
	 * @param roi the ROI to set
	 */
	public void setRoi(ROI2DPolyLine roi) {
		this.roi = roi;
	}

	// === VALIDATION ===

	/**
	 * Checks if there are any measurements done.
	 * 
	 * @return true if measurements exist
	 */
	public boolean isThereAnyMeasuresDone() {
		return level2D != null && level2D.npoints > 0;
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
	public int[] getIsPresent() {
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
	 * Sets a single value in the values array.
	 * 
	 * @param index the index to set
	 * @param value the value to set
	 */
	public void setValue(int index, double value) {
		if (values != null && index >= 0 && index < values.length) {
			values[index] = value;
		}
	}

	/**
	 * Sets a single value in the presence array.
	 * 
	 * @param index the index to set
	 * @param value the value to set
	 */
	public void setPresence(int index, int value) {
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

	// === LEVEL2D DATA PROCESSING ===

	/**
	 * Gets level2D Y data subsampled.
	 * 
	 * @param seriesBinMs the series bin in milliseconds
	 * @param outputBinMs the output bin in milliseconds
	 * @return the subsampled data
	 */
	public List<Double> getLevel2D_Y_subsampled(long seriesBinMs, long outputBinMs) {
		if (level2D == null || level2D.npoints == 0) {
			return new ArrayList<>();
		}

		long maxMs = (level2D.ypoints.length - 1) * seriesBinMs;
		long npoints = (maxMs / outputBinMs) + 1;

		List<Double> result = new ArrayList<>();
		for (long i = 0; i < npoints; i++) {
			long timeMs = i * outputBinMs;
			int index = (int) (timeMs / seriesBinMs);

			if (index < level2D.ypoints.length) {
				result.add(level2D.ypoints[index]);
			} else {
				result.add(0.0);
			}
		}

		return result;
	}

	/**
	 * Gets level2D Y data.
	 * 
	 * @return the Y data
	 */
	public List<Double> getLevel2D_Y() {
		if (level2D == null || level2D.ypoints == null) {
			return new ArrayList<>();
		}

		List<Double> result = new ArrayList<>();
		for (double value : level2D.ypoints) {
			result.add(value);
		}
		return result;
	}

	/**
	 * Adjusts level2D to image width.
	 * 
	 * @param imageWidth the image width
	 */
	public void adjustLevel2DToImageWidth(int imageWidth) {
		if (level2D == null) {
			return;
		}

		if (level2DOld == null || level2DOld.npoints == 0) {
			level2DOld = level2D.clone();
		}

		if (level2D.npoints > imageWidth) {
			level2D.cropPolylineToNewWidth(imageWidth);
		}
	}

	/**
	 * Crops level2D to specified number of points.
	 * 
	 * @param npoints the number of points
	 */
	public void cropLevel2DToNPoints(int npoints) {
		if (level2D != null) {
			level2D.cropPolylineToNewWidth(npoints);
		}
	}

	/**
	 * Restores cropped level2D.
	 * 
	 * @return the number of points restored
	 */
	public int restoreCroppedLevel2D() {
		if (level2DOld != null && level2DOld.npoints > 0) {
			level2D = level2DOld.clone();
			return level2DOld.npoints;
		}
		return 0;
	}

	// === ROI PROCESSING ===

	/**
	 * Gets ROI for image.
	 * 
	 * @param name        the ROI name
	 * @param time        the time
	 * @param imageHeight the image height
	 * @return the ROI
	 */
	public ROI2DPolyLine getROIForImage(String name, int time, int imageHeight) {
		if (level2D == null || level2D.npoints == 0) {
			return null;
		}

		Polyline2D polyline = getPolyline2DFromLevel2D(level2D, imageHeight);
		if (polyline == null) {
			return null;
		}

		ROI2DPolyLine result = new ROI2DPolyLine(polyline);
		result.setName(name);
		setROI2DColorAndStrokeFromName(result);
		return result;
	}

	/**
	 * Gets polyline2D from level2D.
	 * 
	 * @param level2D     the level2D
	 * @param imageHeight the image height
	 * @return the polyline2D
	 */
	public Polyline2D getPolyline2DFromLevel2D(Level2D level2D, int imageHeight) {
		if (level2D == null || level2D.npoints == 0) {
			return null;
		}
		return level2D.expandPolylineToNewWidth(imageHeight);
	}

	/**
	 * Sets ROI2D color and stroke from name.
	 * 
	 * @param roi the ROI to configure
	 */
	private void setROI2DColorAndStrokeFromName(ROI2DPolyLine roi) {
		if (roi == null) {
			return;
		}

		if (name.contains("sum")) {
			roi.setColor(Color.RED);
			roi.setStroke(2);
		} else if (name.contains("clean")) {
			roi.setColor(Color.GREEN);
			roi.setStroke(1);
		} else if (name.contains("flyPresent")) {
			roi.setColor(Color.BLUE);
			roi.setStroke(1);
		} else {
			roi.setColor(Color.YELLOW);
			roi.setStroke(1);
		}
	}

	/**
	 * Transfers ROI to level2D.
	 */
	public void transferROItoLevel2D() {
		if (roi == null) {
			return;
		}

		Polyline2D polyline = roi.getPolyline2D();
		if (polyline != null) {
			level2D = new Level2D(polyline);
		}
	}

	// === MEDIAN PROCESSING ===

	/**
	 * Builds running median.
	 * 
	 * @param span    the span
	 * @param yvalues the Y values
	 */
	public void buildRunningMedian(int span, double[] yvalues) {
		if (yvalues == null || yvalues.length == 0) {
			return;
		}

		int npoints = yvalues.length;
		double[] medianValues = new double[npoints];

		for (int i = 0; i < npoints; i++) {
			int start = Math.max(0, i - span / 2);
			int end = Math.min(npoints - 1, i + span / 2);
			int count = end - start + 1;

			double[] window = new double[count];
			for (int j = 0; j < count; j++) {
				window[j] = yvalues[start + j];
			}

			Arrays.sort(window);
			medianValues[i] = window[count / 2];
		}

		// Update level2D with median values
		if (level2D != null) {
			level2D.ypoints = medianValues;
		}
	}

	// === OFFSET COMPENSATION ===

	/**
	 * Compensates offset using selected ROI.
	 * 
	 * @param roi the ROI to use for compensation
	 * @param add whether to add or subtract the offset
	 */
	public void compensateOffsetUsingSelectedRoi(ROI2D roi, boolean add) {
		if (roi == null || level2D == null) {
			return;
		}

		// Implementation would depend on specific offset compensation logic
		// This is a placeholder for the actual implementation
	}

	/**
	 * Cuts and interpolates points enclosed in selected ROI.
	 * 
	 * @param roi the ROI
	 */
	public void cutAndInterpolatePointsEnclosedInSelectedRoi(ROI2D roi) {
		if (roi == null || level2D == null) {
			return;
		}

		// Implementation would depend on specific interpolation logic
		// This is a placeholder for the actual implementation
	}

	// === CSV EXPORT/IMPORT ===

	/**
	 * Exports XY data to CSV row.
	 * 
	 * @param sbf       the string buffer
	 * @param separator the separator
	 * @return true if successful
	 */
	public boolean exportXYDataToCsv(StringBuilder sbf, String separator) {
		if (level2D == null || level2D.npoints == 0) {
			return false;
		}
		sbf.append(level2D.ypoints.length);
		sbf.append(separator);
		for (int i = 0; i < level2D.npoints; i++) {
			if (i > 0) {
				sbf.append(separator);
			}
			sbf.append(level2D.xpoints[i]).append(separator).append(level2D.ypoints[i]);
		}

		return true;
	}

	/**
	 * Exports Y data to CSV row.
	 * 
	 * @param sbf       the string buffer
	 * @param separator the separator
	 * @return true if successful
	 */
	public boolean exportYDataToCsv(StringBuilder sbf, String separator) {
		if (level2D == null || level2D.ypoints == null) {
			return false;
		}
		sbf.append(level2D.ypoints.length);
		sbf.append(separator);
		for (int i = 0; i < level2D.ypoints.length; i++) {
			if (i > 0) {
				sbf.append(separator);
			}
			sbf.append(level2D.ypoints[i]);
		}

		return true;
	}

	/**
	 * Imports XY data from CSV row.
	 * 
	 * @param data    the CSV data
	 * @param startAt the starting index
	 * @return true if successful
	 */
	public boolean importXYDataFromCsv(String[] data, int startAt) {
		if (data == null || data.length < startAt + 2) {
			return false;
		}

		try {
			int npoints = (data.length - startAt) / 2;
			double[] xpoints = new double[npoints];
			double[] ypoints = new double[npoints];

			for (int i = 0; i < npoints; i++) {
				xpoints[i] = Double.parseDouble(data[startAt + i * 2]);
				ypoints[i] = Double.parseDouble(data[startAt + i * 2 + 1]);
			}

			setLevel2D(new Level2D(xpoints, ypoints, npoints));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Imports Y data from CSV row.
	 * 
	 * @param data    the CSV data
	 * @param startAt the starting index
	 * @return true if successful
	 */
	public boolean importYDataFromCsv(String[] data, int startAt) {
		if (data == null || data.length < startAt + 1) {
			return false;
		}

		try {
			int npoints = data.length - startAt;
			double[] ypoints = new double[npoints];

			for (int i = 0; i < npoints; i++) {
				ypoints[i] = Double.parseDouble(data[startAt + i]);
			}

			double[] xpoints = new double[npoints];
			for (int i = 0; i < npoints; i++) {
				xpoints[i] = i;
			}

			setLevel2D(new Level2D(xpoints, ypoints, npoints));
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
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
				&& Objects.equals(level2D, other.level2D) && Arrays.equals(values, other.values)
				&& Arrays.equals(isPresent, other.isPresent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, factor, level2D, Arrays.hashCode(values), Arrays.hashCode(isPresent));
	}

	@Override
	public String toString() {
		return String.format("SpotMeasure{name='%s', factor=%.2f, level2DPoints=%d, hasValues=%b, hasPresence=%b}",
				name, factor, getLevel2DNPoints(), values != null, isPresent != null);
	}
}
