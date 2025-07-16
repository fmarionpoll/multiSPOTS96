package plugins.fmp.multiSPOTS96.tools.ROI2D;

import java.awt.Point;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.util.XMLUtil;

/**
 * Enhanced ROI2D along time with proper encapsulation and validation.
 * Represents a ROI that exists at a specific time point with associated mask
 * data.
 * 
 * @author MultiSPOTS96 Team
 * @version 2.0
 */
public class ROI2DAlongT implements XMLPersistent {

	private static final Logger logger = Logger.getLogger(ROI2DAlongT.class.getName());

	// Private fields with proper encapsulation
	private int index = 0;
	private long timePoint = 0;
	private ROI2D inputRoi = null;
	private ArrayList<ArrayList<int[]>> masksList = null;
	private BooleanMask2D inputMask = null;
	private BooleanMask2D inputMaskNoFly = null;
	private Point[] maskPoints = null;
	private int yMax = 0;
	private int yMin = 0;

	/**
	 * Creates a new ROI2DAlongT with specified time and ROI.
	 * 
	 * @param timePoint The time point for this ROI
	 * @param roi       The ROI at this time point
	 * @throws ROI2DValidationException If parameters are invalid
	 */
	public ROI2DAlongT(long timePoint, ROI2D roi) throws ROI2DValidationException {
		setTimePoint(timePoint);
		setInputRoi(roi);
	}

	/**
	 * Creates a new ROI2DAlongT with default values.
	 */
	public ROI2DAlongT() {
		// Default constructor
	}

	/**
	 * Gets the time point.
	 * 
	 * @return The time point
	 */
	public long getTimePoint() {
		return timePoint;
	}

	/**
	 * Gets the time point (compatibility method).
	 * 
	 * @return The time point
	 * @deprecated Use getTimePoint() instead
	 */
	@Deprecated
	public long getT() {
		return timePoint;
	}

	/**
	 * Sets the time point.
	 * 
	 * @param timePoint The time point (must be non-negative)
	 * @throws ROI2DValidationException If the time point is negative
	 */
	public void setTimePoint(long timePoint) throws ROI2DValidationException {
		if (timePoint < 0) {
			throw new ROI2DValidationException("timePoint", timePoint, "Time point must be non-negative");
		}
		this.timePoint = timePoint;
	}

	/**
	 * Gets the index.
	 * 
	 * @return The index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index.
	 * 
	 * @param index The index (must be non-negative)
	 * @throws ROI2DValidationException If the index is negative
	 */
	public void setIndex(int index) throws ROI2DValidationException {
		if (index < 0) {
			throw new ROI2DValidationException("index", index, "Index must be non-negative");
		}
		this.index = index;
	}

	/**
	 * Gets the input ROI.
	 * 
	 * @return The input ROI, or null if not set
	 */
	public ROI2D getInputRoi() {
		return inputRoi;
	}

	/**
	 * Sets the input ROI.
	 * 
	 * @param roi The input ROI to set
	 * @throws ROI2DValidationException If the ROI is invalid
	 */
	public void setInputRoi(ROI2D roi) throws ROI2DValidationException {
		if (roi != null) {
			ROI2DValidator.validateROI2D(roi, "roi");
			this.inputRoi = (ROI2D) roi.getCopy();
		} else {
			this.inputRoi = null;
		}

		// Clear dependent data when ROI changes
		clearMaskData();
	}

	/**
	 * Gets the masks list.
	 * 
	 * @return The masks list, or null if not set
	 */
	public ArrayList<ArrayList<int[]>> getMasksList() {
		return masksList;
	}

	/**
	 * Sets the masks list.
	 * 
	 * @param masksList The masks list to set
	 */
	public void setMasksList(ArrayList<ArrayList<int[]>> masksList) {
		this.masksList = masksList;
	}

	/**
	 * Gets the input mask.
	 * 
	 * @return The input mask, or null if not built
	 */
	public BooleanMask2D getInputMask() {
		return inputMask;
	}

	/**
	 * Gets the input mask without fly.
	 * 
	 * @return The input mask without fly, or null if not set
	 */
	public BooleanMask2D getInputMaskNoFly() {
		return inputMaskNoFly;
	}

	/**
	 * Sets the input mask without fly.
	 * 
	 * @param inputMaskNoFly The mask to set
	 */
	public void setInputMaskNoFly(BooleanMask2D inputMaskNoFly) {
		this.inputMaskNoFly = inputMaskNoFly;
	}

	/**
	 * Gets the mask points.
	 * 
	 * @return The mask points array, or null if not built
	 */
	public Point[] getMaskPoints() {
		return maskPoints != null ? maskPoints.clone() : null;
	}

	/**
	 * Gets the maximum Y coordinate.
	 * 
	 * @return The maximum Y coordinate
	 */
	public int getYMax() {
		return yMax;
	}

	/**
	 * Gets the minimum Y coordinate.
	 * 
	 * @return The minimum Y coordinate
	 */
	public int getYMin() {
		return yMin;
	}

	/**
	 * Builds the 2D mask from the input ROI.
	 * 
	 * @throws ROI2DProcessingException If mask building fails
	 */
	public void buildMask2DFromInputRoi() throws ROI2DProcessingException {
		if (inputRoi == null) {
			throw new ROI2DProcessingException("buildMask2D", "Input ROI is not set");
		}

		try {
			inputMask = inputRoi.getBooleanMask2D(0, 0, 1, true); // z, t, c, inclusive
			maskPoints = inputMask.getPoints();
			calculateYBounds();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ROI2DProcessingException("buildMask2D", "Operation was interrupted", e);
		} catch (Exception e) {
			throw new ROI2DProcessingException("buildMask2D", "Failed to build mask from ROI", e);
		}
	}

	/**
	 * Calculates the height of the 2D mask.
	 * 
	 * @return The height of the mask (yMax - yMin + 1)
	 * @throws ROI2DProcessingException If mask points are not available
	 */
	public int getMask2DHeight() throws ROI2DProcessingException {
		if (maskPoints == null || maskPoints.length == 0) {
			throw new ROI2DProcessingException("getMask2DHeight",
					"Mask points are not available. Call buildMask2DFromInputRoi() first");
		}

		calculateYBounds();
		return (yMax - yMin + 1);
	}

	/**
	 * Calculates the Y bounds (min and max) from mask points.
	 */
	private void calculateYBounds() {
		if (maskPoints == null || maskPoints.length == 0) {
			yMax = 0;
			yMin = 0;
			return;
		}

		yMax = maskPoints[0].y;
		yMin = yMax;

		for (int i = 1; i < maskPoints.length; i++) {
			if (maskPoints[i].y < yMin) {
				yMin = maskPoints[i].y;
			}
			if (maskPoints[i].y > yMax) {
				yMax = maskPoints[i].y;
			}
		}
	}

	/**
	 * Clears all mask-related data.
	 */
	private void clearMaskData() {
		inputMask = null;
		inputMaskNoFly = null;
		maskPoints = null;
		yMax = 0;
		yMin = 0;
	}

	/**
	 * Checks if mask data is available.
	 * 
	 * @return true if mask data is built, false otherwise
	 */
	public boolean hasMaskData() {
		return inputMask != null && maskPoints != null;
	}

	/**
	 * Creates a summary string of this ROI along time.
	 * 
	 * @return A formatted summary string
	 */
	public String getSummary() {
		return String.format("ROI2DAlongT[index=%d, time=%d, hasMask=%s, points=%d]", index, timePoint, hasMaskData(),
				maskPoints != null ? maskPoints.length : 0);
	}

	@Override
	public boolean loadFromXML(Node node) {
		try {
			final Node nodeMeta = XMLUtil.getElement(node, ROI2DConstants.XML.ID_META);
			if (nodeMeta == null) {
				logger.warning("No metadata node found in XML");
				return false;
			}

			index = XMLUtil.getElementIntValue(nodeMeta, ROI2DConstants.XML.ID_INDEX, 0);
			timePoint = XMLUtil.getElementLongValue(nodeMeta, ROI2DConstants.XML.ID_START, 0);

			// Validate loaded values
			if (index < 0) {
				logger.warning("Invalid index in XML: " + index);
				index = 0;
			}
			if (timePoint < 0) {
				logger.warning("Invalid time point in XML: " + timePoint);
				timePoint = 0;
			}

			inputRoi = ROI2DUtilities.loadFromXML_ROI(nodeMeta);

			// Clear dependent data since we loaded a new ROI
			clearMaskData();

			return true;
		} catch (Exception e) {
			logger.severe("Failed to load ROI2DAlongT from XML: " + e.getMessage());
			return false;
		}
	}

	@Override
	public boolean saveToXML(Node node) {
		try {
			final Node nodeMeta = XMLUtil.setElement(node, ROI2DConstants.XML.ID_META);
			if (nodeMeta == null) {
				logger.warning("Failed to create metadata node in XML");
				return false;
			}

			XMLUtil.setElementIntValue(nodeMeta, ROI2DConstants.XML.ID_INDEX, index);
			XMLUtil.setElementLongValue(nodeMeta, ROI2DConstants.XML.ID_START, timePoint);

			if (inputRoi != null) {
				ROI2DUtilities.saveToXML_ROI(nodeMeta, inputRoi);
			}

			return true;
		} catch (Exception e) {
			logger.severe("Failed to save ROI2DAlongT to XML: " + e.getMessage());
			return false;
		}
	}

	@Override
	public String toString() {
		return getSummary();
	}
}
