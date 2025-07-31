package plugins.fmp.multiSPOTS96.tools.polyline;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import icy.type.geom.Polyline2D;

/**
 * Extended polyline class for 2D level operations and data manipulation. This
 * class extends Icy's Polyline2D and provides additional functionality for
 * working with level data, including resizing, mathematical operations, and
 * specialized data processing methods.
 * 
 * <p>
 * The Level2D class is particularly useful for:
 * <ul>
 * <li>Processing measurement data along lines</li>
 * <li>Resizing polylines to different widths</li>
 * <li>Performing mathematical operations on Y-coordinates</li>
 * <li>Computing derived values like polarization index</li>
 * <li>Threshold operations on level data</li>
 * </ul>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * // Create a level from a list of points
 * Level2D level = new Level2D(pointsList);
 * 
 * // Resize to a new width
 * Level2D resized = level.expandPolylineToNewWidth(1000);
 * 
 * // Apply mathematical operations
 * level.multiply_Y(2.0);
 * level.threshold_Y(0.5);
 * </pre>
 * 
 * @author MultiSPOTS96
 * @see icy.type.geom.Polyline2D
 */
public class Level2D extends Polyline2D {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(Level2D.class.getName());

	/** Default initial capacity for arrays */
//	private static final int DEFAULT_CAPACITY = 10;

	/** Growth factor for array expansion */
	private static final int GROWTH_FACTOR = 2;

	/** Minimum threshold value */
	private static final double MIN_THRESHOLD = 0.0;

	/** Maximum threshold value */
	private static final double MAX_THRESHOLD = 1.0;

	/** Epsilon for floating-point comparisons */
	private static final double EPSILON = 1e-10;

	/**
	 * Creates an empty Level2D polyline.
	 */
	public Level2D() {
		super();
	}

	/**
	 * Creates a Level2D with the specified number of points. X-coordinates are
	 * automatically set to sequential values (0, 1, 2, ...).
	 * 
	 * @param numPoints the number of points in the polyline
	 * @throws IllegalArgumentException if numPoints is negative
	 */
	public Level2D(int numPoints) {
		if (numPoints < 0) {
			throw new IllegalArgumentException("Number of points cannot be negative: " + numPoints);
		}

		this.npoints = numPoints;
		this.xpoints = new double[numPoints];
		this.ypoints = new double[numPoints];

		// Initialize X coordinates to sequential values
		for (int i = 0; i < numPoints; i++) {
			xpoints[i] = i;
		}
	}

	/**
	 * Creates a Level2D from an existing Polyline2D.
	 * 
	 * @param polyline the source polyline
	 * @throws IllegalArgumentException if polyline is null
	 */
	public Level2D(Polyline2D polyline) {
		if (polyline == null) {
			throw new IllegalArgumentException("Source polyline cannot be null");
		}

		this.npoints = polyline.npoints;
		this.xpoints = new double[npoints];
		this.ypoints = new double[npoints];

		System.arraycopy(polyline.xpoints, 0, this.xpoints, 0, npoints);
		System.arraycopy(polyline.ypoints, 0, this.ypoints, 0, npoints);
	}

	/**
	 * Creates a Level2D from arrays of coordinates.
	 * 
	 * @param xPoints   array of x-coordinates
	 * @param yPoints   array of y-coordinates
	 * @param numPoints number of points to use
	 * @throws IllegalArgumentException if arrays are null, numPoints is invalid, or
	 *                                  arrays are too small
	 */
	public Level2D(double[] xPoints, double[] yPoints, int numPoints) {
		if (xPoints == null || yPoints == null) {
			throw new IllegalArgumentException("Coordinate arrays cannot be null");
		}
		if (numPoints < 0) {
			throw new IllegalArgumentException("Number of points cannot be negative: " + numPoints);
		}
		if (xPoints.length < numPoints || yPoints.length < numPoints) {
			throw new IllegalArgumentException("Arrays are too small for the specified number of points");
		}

		this.npoints = numPoints;
		this.xpoints = new double[numPoints];
		this.ypoints = new double[numPoints];

		System.arraycopy(xPoints, 0, this.xpoints, 0, numPoints);
		System.arraycopy(yPoints, 0, this.ypoints, 0, numPoints);
	}

	/**
	 * Creates a Level2D from arrays of integer coordinates.
	 * 
	 * @param xPoints   array of x-coordinates
	 * @param yPoints   array of y-coordinates
	 * @param numPoints number of points to use
	 * @throws IllegalArgumentException if arrays are null, numPoints is invalid, or
	 *                                  arrays are too small
	 */
	public Level2D(int[] xPoints, int[] yPoints, int numPoints) {
		if (xPoints == null || yPoints == null) {
			throw new IllegalArgumentException("Coordinate arrays cannot be null");
		}
		if (numPoints < 0) {
			throw new IllegalArgumentException("Number of points cannot be negative: " + numPoints);
		}
		if (xPoints.length < numPoints || yPoints.length < numPoints) {
			throw new IllegalArgumentException("Arrays are too small for the specified number of points");
		}

		this.npoints = numPoints;
		this.xpoints = new double[numPoints];
		this.ypoints = new double[numPoints];

		for (int i = 0; i < numPoints; i++) {
			this.xpoints[i] = xPoints[i];
			this.ypoints[i] = yPoints[i];
		}
	}

	/**
	 * Creates a Level2D from a list of Point2D objects.
	 * 
	 * @param pointsList the list of points
	 * @throws IllegalArgumentException if pointsList is null
	 */
	public Level2D(List<Point2D> pointsList) {
		if (pointsList == null) {
			throw new IllegalArgumentException("Points list cannot be null");
		}

		this.npoints = pointsList.size();
		this.xpoints = new double[npoints];
		this.ypoints = new double[npoints];

		for (int i = 0; i < npoints; i++) {
			Point2D point = pointsList.get(i);
			if (point == null) {
				throw new IllegalArgumentException("Point at index " + i + " is null");
			}
			this.xpoints[i] = point.getX();
			this.ypoints[i] = point.getY();
		}
	}

	/**
	 * Inserts a series of Y-coordinates from a list of points into the polyline.
	 * 
	 * @param points the list of points to insert
	 * @param start  the starting index in the polyline
	 * @param end    the ending index in the polyline (exclusive)
	 * @return true if the insertion was successful, false otherwise
	 * @throws IllegalArgumentException if points is null or indices are invalid
	 */
	public boolean insertSeriesofYPoints(List<Point2D> points, int start, int end) {
		if (points == null) {
			throw new IllegalArgumentException("Points list cannot be null");
		}
		if (start < 0 || end > this.npoints || start >= end) {
			throw new IllegalArgumentException(
					"Invalid range: start=" + start + ", end=" + end + ", npoints=" + this.npoints);
		}

		int requiredSize = end - start;
		if (points.size() < requiredSize) {
			LOGGER.warning("Points list is too small for the specified range");
			return false;
		}

		try {
			for (int i = start, j = 0; i < end; i++, j++) {
				Point2D point = points.get(j);
				if (point != null) {
					ypoints[i] = point.getY();
				}
			}
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error inserting Y points", e);
			return false;
		}
	}

	/**
	 * Inserts Y-coordinates from an integer array into the polyline.
	 * 
	 * @param points the array of Y-coordinates
	 * @param start  the starting index in the polyline
	 * @param end    the ending index in the polyline (inclusive)
	 * @return true if the insertion was successful, false otherwise
	 * @throws IllegalArgumentException if points is null or indices are invalid
	 */
	public boolean insertYPoints(int[] points, int start, int end) {
		if (points == null) {
			throw new IllegalArgumentException("Points array cannot be null");
		}
		if (start < 0 || end >= this.npoints || start > end) {
			throw new IllegalArgumentException(
					"Invalid range: start=" + start + ", end=" + end + ", npoints=" + this.npoints);
		}

		int requiredSize = end - start + 1;
		if (points.length < requiredSize) {
			LOGGER.warning("Points array is too small for the specified range");
			return false;
		}

		try {
			for (int i = start, j = 0; i <= end; i++, j++) {
				this.ypoints[i] = points[j];
			}
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error inserting Y points", e);
			return false;
		}
	}

	@Override
	public Level2D clone() {
		Level2D cloned = new Level2D(npoints);
		System.arraycopy(this.xpoints, 0, cloned.xpoints, 0, npoints);
		System.arraycopy(this.ypoints, 0, cloned.ypoints, 0, npoints);
		return cloned;
	}

	/**
	 * Expands the polyline to a new width using linear interpolation.
	 * 
	 * @param imageWidth the new width
	 * @return a new Level2D with the expanded width
	 * @throws IllegalArgumentException if imageWidth is not positive
	 */
	public Level2D expandPolylineToNewWidth(int imageWidth) {
		if (imageWidth <= 0) {
			throw new IllegalArgumentException("Image width must be positive: " + imageWidth);
		}

		if (npoints <= 0) {
			return new Level2D(imageWidth);
		}

		try {
			double[] newXPoints = new double[imageWidth];
			double[] newYPoints = new double[imageWidth];

			for (int j = 0; j < npoints; j++) {
				int startIndex = j * imageWidth / npoints;
				int endIndex = (j + 1) * imageWidth / npoints;

				double currentY = ypoints[j];
				double nextY = (j + 1 < npoints) ? ypoints[j + 1] : currentY;

				for (int i = startIndex; i < endIndex && i < imageWidth; i++) {
					newXPoints[i] = i;
					if (endIndex > startIndex) {
						double ratio = (double) (i - startIndex) / (endIndex - startIndex);
						newYPoints[i] = currentY + (nextY - currentY) * ratio;
					} else {
						newYPoints[i] = currentY;
					}
				}
			}

			return new Level2D(newXPoints, newYPoints, imageWidth);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error expanding polyline", e);
			return new Level2D(imageWidth);
		}
	}

	/**
	 * Contracts the polyline to a new width by sampling.
	 * 
	 * @param imageWidth the new width
	 * @return a new Level2D with the contracted width
	 * @throws IllegalArgumentException if imageWidth is not positive
	 */
	public Level2D contractPolylineToNewWidth(int imageWidth) {
		if (imageWidth <= 0) {
			throw new IllegalArgumentException("Image width must be positive: " + imageWidth);
		}

		if (npoints <= 0) {
			return new Level2D(imageWidth);
		}

		try {
			double[] newXPoints = new double[imageWidth];
			double[] newYPoints = new double[imageWidth];

			for (int i = 0; i < imageWidth; i++) {
				int sourceIndex = i * npoints / imageWidth;
				sourceIndex = Math.min(sourceIndex, npoints - 1);

				newXPoints[i] = i;
				newYPoints[i] = ypoints[sourceIndex];
			}

			return new Level2D(newXPoints, newYPoints, imageWidth);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error contracting polyline", e);
			return new Level2D(imageWidth);
		}
	}

	/**
	 * Crops the polyline to a new width, padding with the last value if necessary.
	 * 
	 * @param imageWidth the new width
	 * @return a new Level2D with the cropped width
	 * @throws IllegalArgumentException if imageWidth is not positive
	 */
	public Level2D cropPolylineToNewWidth(int imageWidth) {
		if (imageWidth <= 0) {
			throw new IllegalArgumentException("Image width must be positive: " + imageWidth);
		}

		try {
			double[] newXPoints = new double[imageWidth];
			double[] newYPoints = new double[imageWidth];

			double lastValue = (npoints > 0) ? ypoints[npoints - 1] : 0.0;

			for (int i = 0; i < imageWidth; i++) {
				newXPoints[i] = i;
				newYPoints[i] = (i < npoints) ? ypoints[i] : lastValue;
			}

			return new Level2D(newXPoints, newYPoints, imageWidth);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error cropping polyline", e);
			return new Level2D(imageWidth);
		}
	}

	/**
	 * Crops the polyline to a new width (compatibility method).
	 * 
	 * @param npoints the target number of points
	 * @deprecated Use cropPolylineToNewWidth(int) instead
	 */
	@Deprecated
	public void cropToNPoints(int npoints) {
		Level2D cropped = cropPolylineToNewWidth(npoints);
		this.npoints = cropped.npoints;
		this.xpoints = cropped.xpoints;
		this.ypoints = cropped.ypoints;
	}

	/**
	 * Multiplies all Y-coordinates by a scalar value.
	 * 
	 * @param multiplier the multiplication factor
	 * @throws IllegalArgumentException if multiplier is NaN or infinite
	 */
	public void multiply_Y(double multiplier) {
		if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
			throw new IllegalArgumentException("Multiplier must be a finite number: " + multiplier);
		}

		for (int i = 0; i < npoints; i++) {
			ypoints[i] *= multiplier;
		}
	}

	/**
	 * Adds Y-coordinates from another Level2D to this one. Expands the arrays if
	 * necessary to accommodate the source data.
	 * 
	 * @param source the source Level2D to add
	 * @throws IllegalArgumentException if source is null
	 */
	public void add_Y(Level2D source) {
		if (source == null) {
			throw new IllegalArgumentException("Source Level2D cannot be null");
		}

		int sourcePoints = source.npoints;

		// Expand arrays if necessary
		if (sourcePoints > npoints) {
			ensureCapacity(sourcePoints);
		}

		// Add Y values
		for (int i = 0; i < sourcePoints; i++) {
			ypoints[i] += source.ypoints[i];
		}
	}

	/**
	 * Get maximum of the Y values
	 */
	public double getMaximum_Y() {
		double maximum = ypoints[0];
		for (int i = 0; i < npoints; i++) {
			if (ypoints[i] > maximum)
				maximum = ypoints[i];
		}
		return maximum;
	}

	/**
	 * Applies a threshold to all Y-coordinates. Values greater than the threshold
	 * are set to 1, others to 0.
	 * 
	 * @param threshold the threshold value
	 */
	public void threshold_Y(double threshold) {
		for (int i = 0; i < npoints; i++) {
			ypoints[i] = (ypoints[i] > threshold) ? MAX_THRESHOLD : MIN_THRESHOLD;
		}
	}

	/**
	 * Computes the polarization index (PI) from two data sources. PI = (data1 -
	 * data2) / (data1 + data2)
	 * 
	 * @param data1 the first data source
	 * @param data2 the second data source
	 * @throws IllegalArgumentException if either data source is null
	 */
	public void computePI_Y(Level2D data1, int n1, Level2D data2, int n2) {
		if (data1 == null || data2 == null) {
			throw new IllegalArgumentException("Data sources cannot be null");
		}

		int maxPoints = Math.max(data1.npoints, data2.npoints);
		ensureCapacity(maxPoints);

		for (int i = 0; i < maxPoints; i++) {
			double value1 = (i < data1.npoints) ? data1.ypoints[i]/(double)n1 : 0.0;
			double value2 = (i < data2.npoints) ? data2.ypoints[i]/(double)n2 : 0.0;
			double sum = value1 + value2;

			if (Math.abs(sum) > EPSILON) {
				ypoints[i] = (value1 - value2) / sum;
			} else {
				ypoints[i] = 0.0;
			}
		}
	}

	/**
	 * Computes the sum of Y-coordinates from two data sources.
	 * 
	 * @param data1 the first data source
	 * @param data2 the second data source
	 * @throws IllegalArgumentException if either data source is null
	 */
	public void computeSUM_Y(Level2D data1, int n1, Level2D data2, int n2) {
		if (data1 == null || data2 == null) {
			throw new IllegalArgumentException("Data sources cannot be null");
		}

		int maxPoints = Math.max(data1.npoints, data2.npoints);
		ensureCapacity(maxPoints);

		for (int i = 0; i < maxPoints; i++) {
			double value1 = (i < data1.npoints) ? data1.ypoints[i]/(double)n1 : 0.0;
			double value2 = (i < data2.npoints) ? data2.ypoints[i]/(double)n2 : 0.0;
			ypoints[i] = value1 + value2;
		}
	}

	/**
	 * Computes a binary presence indicator from two data sources. Result is 1 if
	 * either data source has a value > 0, otherwise 0.
	 * 
	 * @param data1 the first data source
	 * @param data2 the second data source
	 * @throws IllegalArgumentException if either data source is null
	 */
	public void computeIsPresent_Y(Level2D data1,int n1, Level2D data2, int n2) {
		if (data1 == null || data2 == null) {
			throw new IllegalArgumentException("Data sources cannot be null");
		}

		int maxPoints = Math.max(data1.npoints, data2.npoints);
		ensureCapacity(maxPoints);

		for (int i = 0; i < maxPoints; i++) {
			double value1 = (i < data1.npoints) ? data1.ypoints[i]/(double)n1 : 0.0;
			double value2 = (i < data2.npoints) ? data2.ypoints[i]/(double)n2 : 0.0;
			ypoints[i] = (value1 + value2 > EPSILON) ? MAX_THRESHOLD : MIN_THRESHOLD;
		}
	}

	/**
	 * Gets the current number of points in the polyline.
	 * 
	 * @return the number of points
	 */
	public int getPointCount() {
		return npoints;
	}

	/**
	 * Gets the Y-coordinate at the specified index.
	 * 
	 * @param index the index
	 * @return the Y-coordinate
	 * @throws IndexOutOfBoundsException if index is out of bounds
	 */
	public double getYAt(int index) {
		if (index < 0 || index >= npoints) {
			throw new IndexOutOfBoundsException("Index " + index + " is out of bounds [0, " + npoints + ")");
		}
		return ypoints[index];
	}

	/**
	 * Sets the Y-coordinate at the specified index.
	 * 
	 * @param index the index
	 * @param value the new Y-coordinate value
	 * @throws IndexOutOfBoundsException if index is out of bounds
	 */
	public void setYAt(int index, double value) {
		if (index < 0 || index >= npoints) {
			throw new IndexOutOfBoundsException("Index " + index + " is out of bounds [0, " + npoints + ")");
		}
		ypoints[index] = value;
	}

	// Private helper methods

	/**
	 * Ensures that the arrays have at least the specified capacity.
	 */
	private void ensureCapacity(int requiredCapacity) {
		if (requiredCapacity > npoints) {
			int newCapacity = Math.max(requiredCapacity, npoints * GROWTH_FACTOR);

			double[] newXPoints = new double[newCapacity];
			double[] newYPoints = new double[newCapacity];

			if (npoints > 0) {
				System.arraycopy(xpoints, 0, newXPoints, 0, npoints);
				System.arraycopy(ypoints, 0, newYPoints, 0, npoints);
			}

			// Initialize new X coordinates
			for (int i = npoints; i < newCapacity; i++) {
				newXPoints[i] = i;
			}

			this.xpoints = newXPoints;
			this.ypoints = newYPoints;
			this.npoints = requiredCapacity;
		}
	}
}
