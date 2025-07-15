package plugins.fmp.multiSPOTS96.experiment.spots;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.type.geom.Polygon2D;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.sequence.TInterval;
import plugins.fmp.multiSPOTS96.experiment.sequence.TIntervalsArray;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExport;

/**
 * Manages a collection of spots with comprehensive operations and data
 * persistence.
 * 
 * <p>
 * This class provides thread-safe operations for managing spots collections
 * with clean separation of concerns for loading, saving, and processing
 * operations.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class SpotsArray {

	// === CONSTANTS ===
	private static final String ID_SPOTTRACK = "spotTrack";
	private static final String ID_NSPOTS = "N_spots";
	private static final String ID_LISTOFSPOTS = "List_of_spots";
	private static final String ID_SPOT_ = "spot_";
	private static final String CSV_FILENAME = "SpotsMeasures.csv";
	private static final String CSV_SEPARATOR = ";";
	private static final int DEFAULT_VERSION = 2;

	// === CORE FIELDS ===
	private final List<Spot> spotsList;
	private TIntervalsArray timeIntervals;

	// === CONSTRUCTORS ===

	/**
	 * Creates a new SpotsArray.
	 */
	public SpotsArray() {
		this.spotsList = new ArrayList<>();
		this.timeIntervals = new TIntervalsArray();
	}

	// === SPOTS MANAGEMENT ===

	/**
	 * Gets the list of spots.
	 * 
	 * @return the spots list
	 */
	public List<Spot> getSpotsList() {
		return spotsList;
	}

	/**
	 * Gets the number of spots.
	 * 
	 * @return the number of spots
	 */
	public int getSpotsCount() {
		return spotsList.size();
	}

	/**
	 * Checks if the spots list is empty.
	 * 
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return spotsList.isEmpty();
	}

	/**
	 * Adds a spot to the array.
	 * 
	 * @param spot the spot to add
	 * @throws IllegalArgumentException if spot is null
	 */
	public void addSpot(Spot spot) {
		Objects.requireNonNull(spot, "Spot cannot be null");
		spotsList.add(spot);
	}

	/**
	 * Removes a spot from the array.
	 * 
	 * @param spot the spot to remove
	 * @return true if removed
	 */
	public boolean removeSpot(Spot spot) {
		return spotsList.remove(spot);
	}

	/**
	 * Clears all spots from the array.
	 */
	public void clearSpots() {
		spotsList.clear();
	}

	/**
	 * Sorts the spots list.
	 */
	public void sortSpots() {
		Collections.sort(spotsList);
	}

	// === SPOT SEARCH ===

	/**
	 * Finds a spot by name.
	 * 
	 * @param name the spot name
	 * @return the spot if found, null otherwise
	 */
	public Spot findSpotByName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}

		return spotsList.stream().filter(spot -> name.equals(spot.getName())).findFirst().orElse(null);
	}

	/**
	 * Finds spots containing a pattern in their name.
	 * 
	 * @param pattern the pattern to search for
	 * @return list of matching spots
	 */
	public List<Spot> findSpotsContainingPattern(String pattern) {
		if (pattern == null || pattern.trim().isEmpty()) {
			return new ArrayList<>();
		}

		return spotsList.stream().filter(spot -> spot.getName() != null && spot.getName().contains(pattern))
				.collect(Collectors.toList());
	}

	/**
	 * Checks if a spot is present in the array.
	 * 
	 * @param spot the spot to check
	 * @return true if present
	 */
	public boolean isSpotPresent(Spot newSpot) {
		if (newSpot == null)
			return false;
		String newSpotName = newSpot.getName();
		for (Spot spot : spotsList) {
			if (spot.getName().equals(newSpotName))
				return true;
		}
		return false;
	}

	// === DATA LOADING ===

	/**
	 * Loads spots measures from directory.
	 * 
	 * @param directory the directory path
	 * @return true if successful
	 */
	public boolean loadSpotsMeasures(String directory) {
		return loadSpots(directory, EnumSpotMeasures.SPOTS_MEASURES);
	}

	/**
	 * Loads all spots data from directory.
	 * 
	 * @param directory the directory path
	 * @return true if successful
	 */
	public boolean loadSpotsAll(String directory) {
		return loadSpots(directory, EnumSpotMeasures.ALL);
	}

	/**
	 * Loads spots from directory with specified measure type.
	 * 
	 * @param directory   the directory path
	 * @param measureType the measure type
	 * @return true if successful
	 */
	private boolean loadSpots(String directory, EnumSpotMeasures measureType) {
		if (directory == null) {
			return false;
		}

		try {
			return csvLoadSpots(directory, measureType);
		} catch (Exception e) {
			System.err.println("Error loading spots: " + e.getMessage());
			return false;
		}
	}

	// === DATA SAVING ===

	/**
	 * Saves all spots data to directory.
	 * 
	 * @param directory the directory path
	 * @return true if successful
	 */
	public boolean saveSpotsAll(String directory) {
		if (directory == null) {
			return false;
		}
		return csvSaveSpots(directory);
	}

	/**
	 * Saves spots measures to directory.
	 * 
	 * @param directory the directory path
	 * @return true if successful
	 */
	public boolean saveSpotsMeasures(String directory) {
		if (directory == null) {
			return false;
		}
		return csvSaveSpots(directory);
	}

	// === XML OPERATIONS ===

	/**
	 * Saves spots array to XML.
	 * 
	 * @param node the XML node
	 * @return true if successful
	 */
	public boolean saveToXml(Node node) {
		if (node == null) {
			return false;
		}

		try {
			Node nodeSpotsArray = XMLUtil.setElement(node, ID_LISTOFSPOTS);
			XMLUtil.setElementIntValue(nodeSpotsArray, ID_NSPOTS, spotsList.size());

			sortSpots();
			for (int i = 0; i < spotsList.size(); i++) {
				Node nodeSpot = XMLUtil.setElement(node, ID_SPOT_ + i);
				spotsList.get(i).saveToXml(nodeSpot);
			}

			return true;
		} catch (Exception e) {
			System.err.println("Error saving spots array to XML: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Loads spots array from XML.
	 * 
	 * @param node the XML node
	 * @return true if successful
	 */
	public boolean loadFromXml(Node node) {
		if (node == null) {
			return false;
		}

		try {
			Node nodeSpotsArray = XMLUtil.getElement(node, ID_LISTOFSPOTS);
			if (nodeSpotsArray == null) {
				return false;
			}

			int nitems = XMLUtil.getElementIntValue(nodeSpotsArray, ID_NSPOTS, 0);
			spotsList.clear();

			for (int i = 0; i < nitems; i++) {
				Node nodeSpot = XMLUtil.getElement(node, ID_SPOT_ + i);
				if (nodeSpot != null) {
					Spot spot = new Spot();
					if (spot.loadFromXml(nodeSpot) && !isSpotPresent(spot)) {
						spotsList.add(spot);
					}
				}
			}

			return true;
		} catch (Exception e) {
			System.err.println("Error loading spots array from XML: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Saves spots descriptors to XML file.
	 * 
	 * @param fileName the file name
	 * @return true if successful
	 */
	public boolean saveDescriptorsToXml(String fileName) {
		if (fileName == null) {
			return false;
		}

		try {
			Document doc = XMLUtil.createDocument(true);
			if (doc == null) {
				return false;
			}

			saveListOfSpotsToXml(XMLUtil.getRootElement(doc));
			return XMLUtil.saveDocument(doc, fileName);
		} catch (Exception e) {
			System.err.println("Error saving descriptors to XML: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Loads spots descriptors from XML file.
	 * 
	 * @param fileName the file name
	 * @return true if successful
	 */
	public boolean loadDescriptorsFromXml(String fileName) {
		if (fileName == null) {
			return false;
		}

		try {
			Document doc = XMLUtil.loadDocument(fileName);
			if (doc == null) {
				return false;
			}

			return loadSpotsOnlyV1(doc);
		} catch (Exception e) {
			System.err.println("Error loading descriptors from XML: " + e.getMessage());
			return false;
		}
	}

	// === COPY OPERATIONS ===

	/**
	 * Copies spots information from another array.
	 * 
	 * @param sourceArray the source array
	 */
	public void copySpotsInfo(SpotsArray sourceArray) {
		copySpots(sourceArray, false);
	}

	/**
	 * Copies spots from another array.
	 * 
	 * @param sourceArray         the source array
	 * @param includeMeasurements whether to include measurements
	 */
	public void copySpots(SpotsArray sourceArray, boolean includeMeasurements) {
		if (sourceArray == null) {
			return;
		}

		spotsList.clear();
		// spotsList.ensureCapacity(sourceArray.getSpotsList().size());

		for (Spot sourceSpot : sourceArray.getSpotsList()) {
			Spot spot = new Spot(sourceSpot, includeMeasurements);
			spotsList.add(spot);
		}
	}

	/**
	 * Pastes spots information to another array.
	 * 
	 * @param targetArray the target array
	 */
	public void pasteSpotsInfo(SpotsArray targetArray) {
		pasteSpots(targetArray, false);
	}

	/**
	 * Pastes spots to another array.
	 * 
	 * @param targetArray         the target array
	 * @param includeMeasurements whether to include measurements
	 */
	public void pasteSpots(SpotsArray targetArray, boolean includeMeasurements) {
		if (targetArray == null) {
			return;
		}

		for (Spot targetSpot : targetArray.getSpotsList()) {
			for (Spot sourceSpot : spotsList) {
				if (sourceSpot.compareTo(targetSpot) == 0) {
					targetSpot.copyFrom(sourceSpot, includeMeasurements);
					break;
				}
			}
		}
	}

	/**
	 * Merges spots from another array.
	 * 
	 * @param sourceArray the source array
	 */
	public void mergeSpots(SpotsArray sourceArray) {
		if (sourceArray == null) {
			return;
		}

		for (Spot sourceSpot : sourceArray.getSpotsList()) {
			if (!isSpotPresent(sourceSpot)) {
				spotsList.add(sourceSpot);
			}
		}
	}

	// === LEVEL2D OPERATIONS ===

	/**
	 * Adjusts spots level2D measures to image width.
	 * 
	 * @param imageWidth the image width
	 */
	public void adjustSpotsLevel2DMeasuresToImageWidth(int imageWidth) {
		spotsList.forEach(spot -> spot.adjustLevel2DMeasuresToImageWidth(imageWidth));
	}

	/**
	 * Crops spots level2D measures to image width.
	 * 
	 * @param imageWidth the image width
	 */
	public void cropSpotsLevel2DMeasuresToImageWidth(int imageWidth) {
		spotsList.forEach(spot -> spot.cropLevel2DMeasuresToImageWidth(imageWidth));
	}

	/**
	 * Initializes level2D measures for all spots.
	 */
	public void initializeLevel2DMeasures() {
		spotsList.forEach(Spot::initializeLevel2DMeasures);
	}

	// === TIME INTERVALS ===

	/**
	 * Gets the time intervals array.
	 * 
	 * @return the time intervals
	 */
	public TIntervalsArray getTimeIntervals() {
		return timeIntervals;
	}

	/**
	 * Sets the time intervals array.
	 * 
	 * @param timeIntervals the time intervals
	 */
	public void setTimeIntervals(TIntervalsArray timeIntervals) {
		this.timeIntervals = timeIntervals;
	}

	/**
	 * Finds the first time interval.
	 * 
	 * @param intervalT the interval time
	 * @return the interval index
	 */
	public int findFirstTimeInterval(long intervalT) {
		return timeIntervals != null ? timeIntervals.findStartItem(intervalT) : -1;
	}

	/**
	 * Gets the time interval at the specified index.
	 * 
	 * @param selectedItem the selected item index
	 * @return the time interval
	 */
	public long getTimeIntervalAt(int selectedItem) {
		return timeIntervals != null ? timeIntervals.getTIntervalAt(selectedItem).start : -1;
	}

	/**
	 * Adds a time interval.
	 * 
	 * @param start the start time
	 * @return the interval index
	 */
	public int addTimeInterval(long start) {
		if (timeIntervals == null) {
			timeIntervals = new TIntervalsArray();
		}
		return timeIntervals.addIfNew(new TInterval(start, -1));
	}

	/**
	 * Deletes a time interval.
	 * 
	 * @param start the start time
	 */
	public void deleteTimeInterval(long start) {
		if (timeIntervals != null) {
			timeIntervals.deleteIntervalStartingAt(start);
		}
	}

	// === UTILITY OPERATIONS ===

	/**
	 * Transfers sum to sum clean for all spots.
	 */
	public void transferSumToSumClean() {
		spotsList.forEach(spot -> {
			SpotMeasure sumIn = spot.getSumMeasurements();
			SpotMeasure sumClean = spot.getCleanMeasurements();

			if (sumIn != null && sumClean != null) {
				sumClean.copyMeasures(sumIn);
			}
		});
	}

	/**
	 * Gets the scaling factor to physical units.
	 * 
	 * @param xlsOption the Excel export option
	 * @return the scaling factor
	 */
	public double getScalingFactorToPhysicalUnits(EnumXLSExport xlsOption) {
		// Implementation would depend on specific scaling logic
		return 1.0;
	}

	/**
	 * Gets the 2D polygon enclosing all spots.
	 * 
	 * @return the polygon
	 */
	public Polygon2D get2DPolygonEnclosingSpots() {
		if (spotsList.isEmpty()) {
			return new Polygon2D();
		}

		// Implementation would create a polygon encompassing all spots
		// This is a placeholder for the actual implementation
		return new Polygon2D();
	}

	/**
	 * Sets filter for spots to analyze.
	 * 
	 * @param setFilter whether to set filter
	 * @param options   the build series options
	 */
	public void setFilterOfSpotsToAnalyze(boolean setFilter, BuildSeriesOptions options) {
		spotsList.forEach(spot -> spot.setReadyForAnalysis(setFilter));
	}

	// === PRIVATE HELPER METHODS ===

	private boolean saveListOfSpotsToXml(Node node) {
		Node spotsNode = XMLUtil.getElement(node, ID_SPOTTRACK);
		if (spotsNode == null) {
			return false;
		}

		XMLUtil.setElementIntValue(spotsNode, "version", DEFAULT_VERSION);
		Node nodeSpotsArray = XMLUtil.setElement(spotsNode, ID_LISTOFSPOTS);
		XMLUtil.setElementIntValue(nodeSpotsArray, ID_NSPOTS, spotsList.size());

		sortSpots();
		for (int i = 0; i < spotsList.size(); i++) {
			Node nodeSpot = XMLUtil.setElement(spotsNode, ID_SPOT_ + i);
			spotsList.get(i).saveToXml(nodeSpot);
		}

		return true;
	}

	private boolean loadSpotsOnlyV1(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_SPOTTRACK);
		if (node == null) {
			return false;
		}

		Node nodeSpotsArray = XMLUtil.getElement(node, ID_LISTOFSPOTS);
		if (nodeSpotsArray == null) {
			return false;
		}

		int nitems = XMLUtil.getElementIntValue(nodeSpotsArray, ID_NSPOTS, 0);
		spotsList.clear();

		for (int i = 0; i < nitems; i++) {
			Node nodeSpot = XMLUtil.getElement(node, ID_SPOT_ + i);
			if (nodeSpot != null) {
				Spot spot = new Spot();
				if (spot.loadFromXml(nodeSpot) && !isSpotPresent(spot)) {
					spotsList.add(spot);
				}
			}
		}

		return true;
	}

	private boolean csvLoadSpots(String directory, EnumSpotMeasures measureType) throws Exception {
		Path csvPath = Paths.get(directory, CSV_FILENAME);
		if (!Files.exists(csvPath)) {
			return false;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
			String line;
			String sep = CSV_SEPARATOR;
			while ((line = reader.readLine()) != null) {
				if (line.charAt(0) == '#')
					sep = String.valueOf(line.charAt(1));
				String[] data = line.split(sep);
				if (data[0].equals("#")) {
					switch (data[1]) {
					case "SPOTS_ARRAY":
						csvLoadSpotsDescription(reader, sep);
						break;

					case "SPOTS":
						csvLoadSpotsArray(reader, sep);
						break;

					case "AREA_SUM":
					case "AREA_SUMCLEAN":
					case "AREA_FLYPRESENT":
					default:
						EnumSpotMeasures measure = EnumSpotMeasures.findByText(data[1]);
						if (measure != null)
							csvLoadSpotsMeasures(reader, measure, sep);
						break;
					}
				}
			}
			reader.close();
			return true;
		}
	}

	private String csvLoadSpotsArray(BufferedReader reader, String csvSeparator) throws IOException {
		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			String[] spotData = line.split(csvSeparator);
			if (spotData[0].equals("#"))
				return spotData[1];

			Spot spot = findSpotByName(spotData[0]);
			if (spot == null) {
				spot = new Spot();
				spotsList.add(spot);
			}
			spot.getProperties().importFromCsv(spotData);
		}
		return null;
	}

	private String csvLoadSpotsDescription(BufferedReader reader, String csvSeparator) throws IOException {
		String line = reader.readLine();
		String[] data = line.split(csvSeparator);
		String motif = data[0].substring(0, Math.min(data[0].length(), 6));
		if (motif.equals("n spot")) {
			int nspots = Integer.valueOf(data[1]);
			if (nspots < spotsList.size())
				spotsList.subList(nspots, spotsList.size()).clear();
			line = reader.readLine();
			if (line != null)
				data = line.split(csvSeparator);
		}
		if (data[0].equals("#")) {
			return data[1];
		}
		return null;
	}

	private String csvLoadSpotsMeasures(BufferedReader reader, EnumSpotMeasures measureType, String csvSeparator)
			throws IOException {
		String line = reader.readLine();
		boolean y = true;
		boolean x = line.contains("xi");
		while ((line = reader.readLine()) != null) {
			String[] data = line.split(csvSeparator);
			if (data[0].equals("#"))
				return data[1];

			Spot spot = findSpotByName(data[0]);
			if (spot == null) {
				spot = new Spot();
				spotsList.add(spot);
			}
			spot.importMeasuresOneType(measureType, data, x, y);
		}
		return null;
	}

	private boolean csvSaveSpots(String directory) {
		Path csvPath = Paths.get(directory, CSV_FILENAME);

		try (FileWriter writer = new FileWriter(csvPath.toFile())) {
			// Save spots array section
			if (!csvSaveSpotsArraySection(writer)) {
				return false;
			}

			// Save description section
			if (!csvSaveDescriptionSection(writer)) {
				return false;
			}

			// Save measures section
			if (!csvSaveMeasuresSection(writer, EnumSpotMeasures.AREA_SUMCLEAN)) {
				return false;
			}

			return true;
		} catch (IOException e) {
			System.err.println("Error saving spots to CSV: " + e.getMessage());
			return false;
		}
	}

	private boolean csvSaveSpotsArraySection(FileWriter writer) throws IOException {
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + "SPOTS" + CSV_SEPARATOR + "multiSPOTS96 data\n");
		writer.write("name" + CSV_SEPARATOR + "index" + CSV_SEPARATOR + "cageID" + CSV_SEPARATOR + "cagePos"
				+ CSV_SEPARATOR + "cageColumn" + CSV_SEPARATOR + "cageRow" + CSV_SEPARATOR + "volume" + CSV_SEPARATOR
				+ "npixels" + CSV_SEPARATOR + "radius" + CSV_SEPARATOR + "stim" + CSV_SEPARATOR + "conc\n");

		for (Spot spot : spotsList) {
			writer.write(spot.getProperties().exportToCsv(CSV_SEPARATOR));
		}

		return true;
	}

	private boolean csvSaveDescriptionSection(FileWriter writer) throws IOException {
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + "DESCRIPTION" + CSV_SEPARATOR + "multiSPOTS96 data\n");
		writer.write("name" + CSV_SEPARATOR + "index" + CSV_SEPARATOR + "cageID" + CSV_SEPARATOR + "cagePos"
				+ CSV_SEPARATOR + "cageColumn" + CSV_SEPARATOR + "cageRow" + CSV_SEPARATOR + "volume" + CSV_SEPARATOR
				+ "npixels" + CSV_SEPARATOR + "radius" + CSV_SEPARATOR + "stim" + CSV_SEPARATOR + "conc\n");

		for (Spot spot : spotsList) {
			writer.write(spot.getProperties().exportToCsv(CSV_SEPARATOR));
		}

		return true;
	}

	private boolean csvSaveMeasuresSection(FileWriter writer, EnumSpotMeasures measureType) throws IOException {
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + "MEASURES" + CSV_SEPARATOR + "multiSPOTS96 data\n");
		writer.write("name" + CSV_SEPARATOR + "index" + CSV_SEPARATOR + "npts" + CSV_SEPARATOR + "yi\n");

		for (Spot spot : spotsList) {
			writer.write(spot.exportMeasuresOneType(measureType, CSV_SEPARATOR));
		}

		return true;
	}

	// === UTILITY METHODS ===

	@Override
	public String toString() {
		return String.format("SpotsArray{spotsCount=%d, hasTimeIntervals=%b}", spotsList.size(), timeIntervals != null);
	}
}