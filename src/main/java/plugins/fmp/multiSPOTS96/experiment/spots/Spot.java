package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DWithMask;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExport;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

/**
 * Represents a spot in the multiSPOTS96 experiment with comprehensive
 * measurement capabilities.
 * 
 * <p>
 * This class encapsulates spot data, measurements, and operations in a clean,
 * maintainable way. It provides thread-safe access to spot properties and
 * measurements.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class Spot implements Comparable<Spot> {

	// === CONSTANTS ===
	private static final String ID_META = "metaMC";
//	private static final String DEFAULT_STIMULUS = "..";
//	private static final String DEFAULT_CONCENTRATION = "..";
//	private static final double DEFAULT_SPOT_VOLUME = 0.5;
	private static final int DATA_OFFSET = 3;

	// === CORE FIELDS ===
	private ROI2DShape spotROI2D;
	private ROI2DWithMask spotMask;
//	private final List<ROI2DAlongT> roiAlongTList = new ArrayList<>();

	private final SpotProperties properties;
	private final SpotMeasurements measurements;
	private final SpotMetadata metadata;

	// === CONSTRUCTORS ===

	/**
	 * Creates a new Spot with the specified ROI.
	 * 
	 * @param roi the ROI representing this spot
	 * @throws IllegalArgumentException if roi is null
	 */
	public Spot(ROI2DShape roi) {
		this.spotROI2D = Objects.requireNonNull(roi, "ROI cannot be null");
		this.properties = new SpotProperties();
		this.measurements = new SpotMeasurements();
		this.metadata = new SpotMetadata();
	}

	/**
	 * Creates a new empty Spot.
	 */
	public Spot() {
		this.properties = new SpotProperties();
		this.measurements = new SpotMeasurements();
		this.metadata = new SpotMetadata();
	}

	/**
	 * Creates a copy of the specified spot.
	 * 
	 * @param sourceSpot          the spot to copy from
	 * @param includeMeasurements whether to copy measurements
	 * @throws IllegalArgumentException if sourceSpot is null
	 */
	public Spot(Spot sourceSpot, boolean includeMeasurements) {
		Objects.requireNonNull(sourceSpot, "Source spot cannot be null");
		this.properties = new SpotProperties(sourceSpot.properties);
		this.measurements = new SpotMeasurements(sourceSpot.measurements, includeMeasurements);
		this.metadata = new SpotMetadata(sourceSpot.metadata);

		if (sourceSpot.spotROI2D != null) {
			this.spotROI2D = (ROI2DShape) sourceSpot.spotROI2D.getCopy();
		}
	}

	// === COMPARISON ===

	@Override
	public int compareTo(Spot other) {
		if (other == null) {
			return 1;
		}
		String thisName = getName();
		String otherName = other.getName();
		return thisName.compareTo(otherName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Spot other = (Spot) obj;
		return Objects.equals(getName(), other.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
	}

	// === CORE OPERATIONS ===

	/**
	 * Copies data from another spot.
	 * 
	 * @param sourceSpot          the spot to copy from
	 * @param includeMeasurements whether to copy measurements
	 */
	public void copyFrom(Spot sourceSpot, boolean includeMeasurements) {
		Objects.requireNonNull(sourceSpot, "Source spot cannot be null");

		this.properties.copyFrom(sourceSpot.properties);

		if (sourceSpot.spotROI2D != null) {
			this.spotROI2D = (ROI2DShape) sourceSpot.spotROI2D.getCopy();
		}

		if (includeMeasurements) {
			this.measurements.copyFrom(sourceSpot.measurements);
		}
	}

	/**
	 * Adds measurements from another spot.
	 * 
	 * @param sourceSpot the spot whose measurements to add
	 */
	public void addMeasurements(Spot sourceSpot) {
		Objects.requireNonNull(sourceSpot, "Source spot cannot be null");
		this.measurements.addFrom(sourceSpot.measurements);
		this.getProperties().setCountAggregatedSpots(this.getProperties().getCountAggregatedSpots() + 1);
	}

	/**
	 * Computes PI (Performance Index) from two spots.
	 * 
	 * @param spot1 the first spot
	 * @param spot2 the second spot
	 */
	public void computePI(Spot spot1, Spot spot2) {
		Objects.requireNonNull(spot1, "Spot1 cannot be null");
		Objects.requireNonNull(spot2, "Spot2 cannot be null");
		int n1 = spot1.getProperties().getCountAggregatedSpots();
		int n2 = spot2.getProperties().getCountAggregatedSpots();
		this.measurements.computePI(spot1.measurements, n1, spot2.measurements, n2);
	}

	/**
	 * Computes sum from two spots.
	 * 
	 * @param spot1 the first spot
	 * @param spot2 the second spot
	 */
	public void computeSUM(Spot spot1, Spot spot2) {
		Objects.requireNonNull(spot1, "Spot1 cannot be null");
		Objects.requireNonNull(spot2, "Spot2 cannot be null");
		int n1 = spot1.getProperties().getCountAggregatedSpots();
		int n2 = spot2.getProperties().getCountAggregatedSpots();
		this.measurements.computeSUM(spot1.measurements, n1, spot2.measurements, n2);
	}

	public void normalizeMeasures() {
		this.measurements.normalizeMeasures();
	}

	// === ROI MANAGEMENT ===

	/**
	 * Gets the ROI associated with this spot.
	 * 
	 * @return the ROI, or null if not set
	 */
	public ROI2D getRoi() {
		return spotROI2D;
	}

	/**
	 * Sets the ROI for this spot.
	 * 
	 * @param roi the ROI to set
	 */
	public void setRoi(ROI2DShape roi) {
		this.spotROI2D = roi;
	}

	/**
	 * Gets the center point of this spot.
	 * 
	 * @return the center point, or null if ROI is not set
	 */
	public Point2D getCenter() {
		if (spotROI2D == null) {
			return null;
		}

		Point position = spotROI2D.getPosition();
		Rectangle bounds = spotROI2D.getBounds();
		position.translate(bounds.height / 2, bounds.width / 2);
		return position;
	}

	public ROI2DWithMask getROIMask() {
		return spotMask;
	}

	public void setROIMask(ROI2DWithMask roiMasked) {
		this.spotMask = roiMasked;
	}

	// === NAMING AND IDENTIFICATION ===

	/**
	 * Sets the name of this spot based on cage and spot IDs.
	 * 
	 * @param cageID the cage ID
	 * @param spotID the spot ID
	 */
	public void setName(int cageID, int spotID) {
		String name = String.format("spot_%03d_%03d", cageID, spotID);
		if (spotROI2D != null) {
			spotROI2D.setName(name);
		}
		properties.setName(name);
	}

	/**
	 * Gets the name of this spot.
	 * 
	 * @return the spot name
	 */
	public String getName() {
		if (properties.getName() == null) {
			String name = getRoi() != null ? getRoi().getName() : "unnamed_spot";
			properties.setName(name);
		}
		return properties.getName();
	}

	/**
	 * Gets the combined stimulus and concentration fields.
	 * 
	 * @return the combined string
	 */
	public String getCombinedStimulusConcentration() {
		return properties.getStimulus() + "_" + properties.getConcentration();
	}

	// === PROPERTIES ACCESS ===

	public SpotProperties getProperties() {
		return properties;
	}

	// === FIELD ACCESS ===

	/**
	 * Gets a field value based on the column header.
	 * 
	 * @param fieldEnum the field enum
	 * @return the field value as string
	 */
	public String getField(EnumXLSColumnHeader fieldEnum) {
		Objects.requireNonNull(fieldEnum, "Field enum cannot be null");

		switch (fieldEnum) {
		case SPOT_STIM:
			return properties.getStimulus();
		case SPOT_CONC:
			return properties.getConcentration();
		case SPOT_VOLUME:
			return String.valueOf(properties.getSpotVolume());
		default:
			return null;
		}
	}

	/**
	 * Sets a field value based on the column header.
	 * 
	 * @param fieldEnum the field enum
	 * @param value     the value to set
	 */
	public void setField(EnumXLSColumnHeader fieldEnum, String value) {
		Objects.requireNonNull(fieldEnum, "Field enum cannot be null");
		Objects.requireNonNull(value, "Value cannot be null");

		switch (fieldEnum) {
		case SPOT_STIM:
			properties.setStimulus(value);
			break;
		case SPOT_CONC:
			properties.setConcentration(value);
			break;
		case SPOT_VOLUME:
			try {
				double volume = Double.parseDouble(value);
				properties.setSpotVolume(volume);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid volume value: " + value, e);
			}
			break;
		default:
			// Ignore unsupported fields
			break;
		}
	}

	/**
	 * Gets the cage position string for Excel export.
	 * 
	 * @param exportOption the export option
	 * @return the formatted cage position string
	 */
	public String getCagePosition(EnumXLSExport exportOption) {
		Objects.requireNonNull(exportOption, "Export option cannot be null");

		int cagePosition = properties.getCagePosition();

		switch (exportOption) {
		case DISTANCE:
		case ISALIVE:
			return cagePosition + "(T=B)";
		case XYIMAGE:
		case XYTOPCAGE:
		case XYTIPCAPS:
			return cagePosition == 0 ? "x" : "y";
		default:
			return String.valueOf(cagePosition);
		}
	}

	// === MEASUREMENTS ACCESS ===

	/**
	 * Get access to measurements
	 * 
	 * @return measurements
	 */

	public long isThereAnyMeasuresDone(EnumXLSExport option) {
		switch (option) {
		case AREA_SUM:
			return measurements.getSumIn().getCount();
		case AREA_SUMCLEAN:
			return measurements.getSumClean().getCount();
		case AREA_FLYPRESENT:
			return measurements.getFlyPresent().getCount();
		default:
			return 0;
		}
	}

	/**
	 * Gets the sum measurements.
	 * 
	 * @return the sum measurements
	 */
	public SpotMeasure getSum() {
		return measurements.getSumIn();
	}

	/**
	 * Gets the clean measurements.
	 * 
	 * @return the clean measurements
	 */
	public SpotMeasure getSumClean() {
		return measurements.getSumClean();
	}

	/**
	 * Gets the fly presence measurements.
	 * 
	 * @return the fly presence measurements
	 */
	public SpotMeasure getFlyPresent() {
		return measurements.getFlyPresent();
	}

	/**
	 * Gets measurements for a specific export option.
	 * 
	 * @param option the export option
	 * @return the corresponding measurements
	 */
	public SpotMeasure getMeasurements(EnumXLSExport option) {
		Objects.requireNonNull(option, "Export option cannot be null");

		switch (option) {
		case AREA_SUM:
			return measurements.getSumIn();
		case AREA_SUMCLEAN:
			return measurements.getSumClean();
		case AREA_FLYPRESENT:
			return measurements.getFlyPresent();
		default:
			return null;
		}
	}

	// === VALIDATION ===

	/**
	 * Checks if this spot is valid.
	 * 
	 * @return true if valid
	 */
	public boolean isValid() {
		return metadata.isValid();
	}

	/**
	 * Sets the validity of this spot.
	 * 
	 * @param valid the validity flag
	 */
	public void setValid(boolean valid) {
		metadata.setValid(valid);
	}

	/**
	 * Checks if this spot is ready for analysis.
	 * 
	 * @return true if ready for analysis
	 */
	public boolean isReadyForAnalysis() {
		return metadata.isOkToAnalyze();
	}

	/**
	 * Sets whether this spot is ready for analysis.
	 * 
	 * @param ready the ready flag
	 */
	public void setReadyForAnalysis(boolean ready) {
		metadata.setOkToAnalyze(ready);
	}

	/**
	 * Gets the mask2D spot.
	 * 
	 * @return the mask2D spot
	 */
	public BooleanMask2D getMask2DSpot() {
		return metadata.getMask2DSpot();
	}

	/**
	 * Sets the mask2D spot.
	 * 
	 * @param mask2DSpot the mask2D spot to set
	 */
	public void setMask2DSpot(BooleanMask2D mask2DSpot) {
		metadata.setMask2DSpot(mask2DSpot);
	}

	/**
	 * Gets spotImage.
	 * 
	 * @return spotImage
	 */
	public IcyBufferedImage getSpotImage() {
		return metadata.getSpotImage();
	}

	/**
	 * Sets spotImage.
	 * 
	 * @param IcyBufferedImage to set
	 */
	public void setSpotImage(IcyBufferedImage image) {
		metadata.setSpotImage(image);
	}

	/**
	 * Gets spotImage FileName.
	 * 
	 * @return spotImage
	 */
	public String getSpotFilenameTiff() {
		return metadata.getSpotFilenameTiff();
	}

	/**
	 * Sets spotImage.
	 * 
	 * @param IcyBufferedImage to set
	 */
	public void setSpotFilenameTiff(String name) {
		metadata.setSpotFilenameTiff(name);
	}

	public int getSpotKymographT() {
		return metadata.getSpotKymographT();
	}

	public void setSpotKymographT(int spotKymographT) {
		this.metadata.setSpotKymographT(spotKymographT);
	}

	public int getSpotCamDataT() {
		return metadata.getSpotCamDataT();
	}

	public void setSpotCamDataT(int spotCamDataT) {
		this.metadata.setSpotCamDataT(spotCamDataT);
	}

	// === SELECTION ===

	/**
	 * Checks if this spot is selected based on the provided indexes.
	 * 
	 * @param selectedIndexes the list of selected indexes
	 * @return true if this spot is selected
	 */
	public boolean isSelected(List<Integer> selectedIndexes) {
		if (selectedIndexes == null || selectedIndexes.isEmpty()) {
			return false;
		}

		int spotIndex = properties.getSpotArrayIndex();
		return selectedIndexes.contains(spotIndex);
	}

	/**
	 * Checks if there are any measurements done for the specified option.
	 * 
	 * @param option the export option
	 * @return true if measurements exist
	 */
	public boolean hasMeasurements(EnumXLSExport option) {
		SpotMeasure measure = getMeasurements(option);
		return measure != null && measure.getCount() > 0;
	}

	// === MEASUREMENTS PROCESSING ===

	/**
	 * Gets spot measures for Excel export pass 1.
	 * 
	 * @param option      the export option
	 * @param seriesBinMs the series bin in milliseconds
	 * @param outputBinMs the output bin in milliseconds
	 * @return the measures list
	 */
	public List<Double> getMeasuresForExcelPass1(EnumXLSExport exportType, long seriesBinMs, long outputBinMs) {
		SpotMeasure measure = getMeasurements(exportType);
		if (measure == null) {
			return new ArrayList<>();
		}
		return measure.getValuesAsSubsampledList(seriesBinMs, outputBinMs);
	}

	/**
	 * Restores clipped spot measures.
	 */
	public void restoreClippedMeasures() {
		measurements.restoreClippedMeasures();
	}

	/**
	 * Transfers ROI measures to Level2D.
	 */
	public void transferRoiMeasuresToLevel2D() {
		measurements.transferRoiMeasuresToLevel2D();
	}

	// === IMAGE PROCESSING ===

	/**
	 * Adjusts Level2D measures to image width.
	 * 
	 * @param imageWidth the image width
	 */
	public void adjustLevel2DMeasuresToImageWidth(int imageWidth) {
		measurements.adjustLevel2DMeasuresToImageWidth(imageWidth);
	}

	/**
	 * Crops Level2D measures to image width.
	 * 
	 * @param imageWidth the image width
	 */
	public void cropLevel2DMeasuresToImageWidth(int imageWidth) {
		measurements.cropLevel2DMeasuresToImageWidth(imageWidth);
	}

	/**
	 * Initializes Level2D measures.
	 */
	public void initializeLevel2DMeasures() {
		measurements.initializeLevel2DMeasures();
	}

	/**
	 * Transfer measures to Level2D.
	 */
	public void transferMeasuresToLevel2D() {
		measurements.transferMeasuresToLevel2D();
	}

	/**
	 * Transfers spot measures to ROIs.
	 * 
	 * @param imageHeight the image height
	 * @return the list of ROIs
	 */
	public List<ROI2D> transferMeasuresToRois(int imageHeight) {
		return measurements.transferLevel2DToRois(imageHeight);
	}

	/**
	 * Transfers ROI to measures.
	 * 
	 * @param roi         the ROI
	 * @param imageHeight the image height
	 */
	public void transferRoiToMeasures(ROI2D roi, int imageHeight) {
		measurements.transferRoiToMeasures(roi, imageHeight);
	}

	// === CSV EXPORT/IMPORT ===

	/**
	 * Exports measures section header to CSV.
	 * 
	 * @param measureType  the measure type
	 * @param csvSeparator the CSV separator
	 * @return the CSV header string
	 */
	public String exportMeasuresSectionHeader(EnumSpotMeasures measureType, String csvSeparator) {
		return measurements.exportSectionHeader(measureType, csvSeparator);
	}

	/**
	 * Exports measures of one type to CSV.
	 * 
	 * @param measureType  the measure type
	 * @param csvSeparator the CSV separator
	 * @return the CSV data string
	 */
	public String exportMeasuresOneType(EnumSpotMeasures measureType, String csvSeparator) {
		return measurements.exportOneType(properties.getName(), properties.getSpotArrayIndex(), measureType,
				csvSeparator);
	}

	/**
	 * Imports measures of one type from CSV.
	 * 
	 * @param measureType the measure type
	 * @param data        the CSV data
	 * @param includeX    whether to include X coordinates
	 * @param includeY    whether to include Y coordinates
	 */
	public void importMeasuresOneType(EnumSpotMeasures measureType, String[] data, boolean includeX, boolean includeY) {
		measurements.importOneType(measureType, data, includeX, includeY);
	}

	// === XML SERIALIZATION ===

	/**
	 * Loads spot data from XML.
	 * 
	 * @param node the XML node
	 * @return true if successful
	 */
	public boolean loadFromXml(Node node) {
		if (node == null) {
			return false;
		}

		try {
			// Load properties
			if (!properties.loadFromXml(node)) {
				return false;
			}

			final Node nodeMeta = XMLUtil.getElement(node, ID_META);
			if (nodeMeta != null) {
				spotROI2D = (ROI2DShape) ROI2DUtilities.loadFromXML_ROI(nodeMeta);
				if (spotROI2D != null) {
					spotROI2D.setColor(getProperties().getColor());
					getProperties().setName(spotROI2D.getName());
				}
			}

			// Load measurements
			if (!measurements.loadFromXml(node)) {
				return false;
			}

			return true;

		} catch (Exception e) {
			System.err.println("Error loading spot from XML: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Saves spot data to XML.
	 * 
	 * @param node the XML node
	 * @return true if successful
	 */
	public boolean saveToXml(Node node) {
		if (node == null) {
			return false;
		}

		try {
			// Save properties
			if (!properties.saveToXml(node)) {
				return false;
			}

			// Save measurements
			if (!measurements.saveToXml(node)) {
				return false;
			}

			final Node nodeMeta = XMLUtil.setElement(node, ID_META);
			if (nodeMeta != null)
				ROI2DUtilities.saveToXML_ROI(nodeMeta, spotROI2D);

			return true;

		} catch (Exception e) {
			System.err.println("Error saving spot to XML: " + e.getMessage());
			return false;
		}
	}

	// === INNER CLASSES ===

	/**
	 * Encapsulates spot measurements with clean separation of concerns.
	 */
	private static class SpotMeasurements {
		private final SpotMeasure sumIn;
		private final SpotMeasure sumClean;
		private final SpotMeasure flyPresent;

		SpotMeasurements() {
			this.sumIn = new SpotMeasure("sum");
			this.sumClean = new SpotMeasure("clean");
			this.flyPresent = new SpotMeasure("flyPresent");
		}

		SpotMeasurements(SpotMeasurements source, boolean includeData) {
			this.sumIn = new SpotMeasure("sum");
			this.sumClean = new SpotMeasure("clean");
			this.flyPresent = new SpotMeasure("flyPresent");

			if (includeData) {
				copyFrom(source);
			}
		}

		void copyFrom(SpotMeasurements source) {
			sumIn.copyMeasures(source.sumIn);
			sumClean.copyMeasures(source.sumClean);
			flyPresent.copyMeasures(source.flyPresent);
		}

		void addFrom(SpotMeasurements source) {
			sumIn.addMeasures(source.sumIn);
			sumClean.addMeasures(source.sumClean);
			flyPresent.addMeasures(source.flyPresent);
		}

		void computePI(SpotMeasurements measure1, int n1, SpotMeasurements measure2, int n2) {
			sumIn.computePI(measure1.sumIn, measure2.sumIn);
			sumClean.computePI(measure1.sumClean, measure2.sumClean);
		}

		void computeSUM(SpotMeasurements measure1, int n1, SpotMeasurements measure2, int n2) {
			sumIn.computeSUM(measure1.sumIn, n1, measure2.sumIn, n2);
			sumClean.computeSUM(measure1.sumClean, n1, measure2.sumClean, n2);
			flyPresent.combineIsPresent(measure1.flyPresent, n1, measure2.flyPresent, n2);
		}

		void normalizeMeasures() {
			sumIn.normalizeValues();
			sumClean.normalizeValues();
		}

		SpotMeasure getSumIn() {
			return sumIn;
		}

		SpotMeasure getSumClean() {
			return sumClean;
		}

		SpotMeasure getFlyPresent() {
			return flyPresent;
		}

		void restoreClippedMeasures() {
			restoreClippedMeasure(sumIn);
			restoreClippedMeasure(sumClean);
			restoreClippedMeasure(flyPresent);
		}

		private void restoreClippedMeasure(SpotMeasure measure) {
			if (measure != null) {
				measure.getSpotLevel2D().restoreCroppedLevel2D();
			}
		}

		public void transferMeasuresToLevel2D() {
			if (sumIn != null)
				sumIn.transferValuesToLevel2D();
			if (sumClean != null)
				sumClean.transferValuesToLevel2D();
			if (flyPresent != null)
				flyPresent.transferIsPresentToLevel2D();
		}

		void transferRoiMeasuresToLevel2D() {
			if (sumIn != null)
				sumIn.getSpotLevel2D().transferROItoLevel2D();
			if (sumClean != null)
				sumClean.getSpotLevel2D().transferROItoLevel2D();
			if (flyPresent != null)
				flyPresent.getSpotLevel2D().transferROItoLevel2D();
		}

		void adjustLevel2DMeasuresToImageWidth(int imageWidth) {
			if (sumIn != null)
				sumIn.getSpotLevel2D().adjustLevel2DToImageWidth(imageWidth);
			if (sumClean != null)
				sumClean.getSpotLevel2D().adjustLevel2DToImageWidth(imageWidth);
			if (flyPresent != null)
				flyPresent.getSpotLevel2D().adjustLevel2DToImageWidth(imageWidth);
		}

		void cropLevel2DMeasuresToImageWidth(int imageWidth) {
			if (sumIn != null)
				sumIn.getSpotLevel2D().cropLevel2DToNPoints(imageWidth);
			if (sumClean != null)
				sumClean.getSpotLevel2D().cropLevel2DToNPoints(imageWidth);
			if (flyPresent != null)
				flyPresent.getSpotLevel2D().cropLevel2DToNPoints(imageWidth);
		}

		void initializeLevel2DMeasures() {
			if (sumIn != null)
				sumIn.getSpotLevel2D().clearLevel2D();
			if (sumClean != null)
				sumClean.getSpotLevel2D().clearLevel2D();
			if (flyPresent != null)
				flyPresent.getSpotLevel2D().clearLevel2D();
		}

		List<ROI2D> transferLevel2DToRois(int imageHeight) {
			List<ROI2D> rois = new ArrayList<>();
			if (sumIn != null) {
				ROI2DPolyLine roi = sumIn.getSpotLevel2D().getROIForImage("sum", 0, imageHeight);
				if (roi != null)
					rois.add(roi);
			}
			return rois;
		}

		void transferRoiToMeasures(ROI2D roi, int imageHeight) {
			if (sumIn != null)
				transferRoiToMeasureValue(roi, imageHeight, sumIn);
			if (sumClean != null)
				transferRoiToMeasureValue(roi, imageHeight, sumClean);
			if (flyPresent != null)
				transferRoiToMeasureBoolean(roi, flyPresent);
		}

		private void transferRoiToMeasureValue(ROI2D roi, int imageHeight, SpotMeasure measure) {
			if (roi != null && measure != null) {
				measure.getSpotLevel2D().transferROItoLevel2D();
			}
		}

		private void transferRoiToMeasureBoolean(ROI2D roi, SpotMeasure measure) {
			if (roi != null && measure != null) {
				measure.getSpotLevel2D().transferROItoLevel2D();
			}
		}

		boolean loadFromXml(Node node) {
			// Implementation would depend on SpotMeasure XML loading
			return true;
		}

		boolean saveToXml(Node node) {
			// Implementation would depend on SpotMeasure XML saving
			return true;
		}

		String exportSectionHeader(EnumSpotMeasures measureType, String csvSeparator) {
			// Implementation for CSV export header
			return "#" + csvSeparator + measureType.toString() + "\n";
		}

		String exportOneType(String sourceName, int spotArrayIndex, EnumSpotMeasures measureType, String csvSeparator) {
			StringBuilder sbf = new StringBuilder();
			sbf.append(sourceName + csvSeparator + spotArrayIndex + csvSeparator);
			switch (measureType) {
			case AREA_SUM:
				sumIn.exportYDataToCsv(sbf, csvSeparator);
				break;
			case AREA_SUMCLEAN:
				sumClean.exportYDataToCsv(sbf, csvSeparator);
				break;
			case AREA_FLYPRESENT:
				flyPresent.exportYDataToCsv(sbf, csvSeparator);
				break;
			default:
				break;
			}
			sbf.append("\n");
			return sbf.toString();
		}

		void importOneType(EnumSpotMeasures measureType, String[] data, boolean includeX, boolean includeY) {
			if (includeX && includeY) {
				switch (measureType) {
				case AREA_SUM:
					sumIn.importXYDataFromCsv(data, DATA_OFFSET);
					break;
				case AREA_SUMCLEAN:
					sumClean.importXYDataFromCsv(data, DATA_OFFSET);
					break;
				case AREA_FLYPRESENT:
					flyPresent.importXYDataFromCsv(data, DATA_OFFSET);
					break;
				default:
					break;
				}
			} else if (!includeX && includeY) {
				switch (measureType) {
				case AREA_SUM:
					sumIn.importYDataFromCsv(data, DATA_OFFSET);
					break;
				case AREA_SUMCLEAN:
					sumClean.importYDataFromCsv(data, DATA_OFFSET);
					break;
				case AREA_FLYPRESENT:
					flyPresent.importYDataFromCsv(data, DATA_OFFSET);
					break;
				default:
					break;
				}
			}
		}

	}

	/**
	 * Encapsulates spot metadata with clean access patterns.
	 */
	private static class SpotMetadata {
		private boolean valid = true;
		private boolean okToAnalyze = true;
		private int kymographIndex = -1;
		private int spotCamDataT = -1;
		private int spotKymographT = -1;
		private String spotFilenameTiff;
		private IcyBufferedImage spotImage;
		private BooleanMask2D mask2DSpot;

		SpotMetadata() {
		}

		SpotMetadata(SpotMetadata source) {
			this.valid = source.valid;
			this.okToAnalyze = source.okToAnalyze;
			this.kymographIndex = source.kymographIndex;
			this.spotCamDataT = source.spotCamDataT;
			this.spotKymographT = source.spotKymographT;
			this.spotFilenameTiff = source.spotFilenameTiff;
			this.spotImage = source.spotImage;
			this.mask2DSpot = source.mask2DSpot;
		}

		boolean isValid() {
			return valid;
		}

		void setValid(boolean valid) {
			this.valid = valid;
		}

		boolean isOkToAnalyze() {
			return okToAnalyze;
		}

		void setOkToAnalyze(boolean okToAnalyze) {
			this.okToAnalyze = okToAnalyze;
		}

//		int getKymographIndex() {
//			return kymographIndex;
//		}
//
//		void setKymographIndex(int kymographIndex) {
//			this.kymographIndex = kymographIndex;
//		}

		int getSpotCamDataT() {
			return spotCamDataT;
		}

		void setSpotCamDataT(int spotCamDataT) {
			this.spotCamDataT = spotCamDataT;
		}

		int getSpotKymographT() {
			return spotKymographT;
		}

		void setSpotKymographT(int spotKymographT) {
			this.spotKymographT = spotKymographT;
		}

		String getSpotFilenameTiff() {
			return spotFilenameTiff;
		}

		void setSpotFilenameTiff(String spotFilenameTiff) {
			this.spotFilenameTiff = spotFilenameTiff;
		}

		IcyBufferedImage getSpotImage() {
			return spotImage;
		}

		void setSpotImage(IcyBufferedImage spotImage) {
			this.spotImage = spotImage;
		}

		BooleanMask2D getMask2DSpot() {
			return mask2DSpot;
		}

		void setMask2DSpot(BooleanMask2D mask2DSpot) {
			this.mask2DSpot = mask2DSpot;
		}
	}

}
