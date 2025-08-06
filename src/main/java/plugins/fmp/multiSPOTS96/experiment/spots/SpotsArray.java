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

import icy.util.XMLUtil;
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

	// === CONSTRUCTORS ===

	public SpotsArray() {
		this.spotsList = new ArrayList<>();
	}

	// === SPOTS MANAGEMENT ===

	public List<Spot> getSpotsList() {
		return spotsList;
	}

	public int getSpotsCount() {
		return spotsList.size();
	}

	public boolean isEmpty() {
		return spotsList.isEmpty();
	}

	public void addSpot(Spot spot) {
		Objects.requireNonNull(spot, "Spot cannot be null");
		spotsList.add(spot);
	}

	public boolean removeSpot(Spot spot) {
		return spotsList.remove(spot);
	}

	public void clearSpots() {
		spotsList.clear();
	}

	public void sortSpots() {
		Collections.sort(spotsList);
	}

	// === SPOT SEARCH ===

	public Spot findSpotByName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}

		return spotsList.stream().filter(spot -> name.equals(spot.getName())).findFirst().orElse(null);
	}

	public List<Spot> findSpotsContainingPattern(String pattern) {
		if (pattern == null || pattern.trim().isEmpty()) {
			return new ArrayList<>();
		}

		return spotsList.stream().filter(spot -> spot.getName() != null && spot.getName().contains(pattern))
				.collect(Collectors.toList());
	}

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

	public boolean loadSpotsMeasures(String directory) {
		return loadSpots(directory, EnumSpotMeasures.SPOTS_MEASURES);
	}

	public boolean loadSpotsAll(String directory) {
		return loadSpots(directory, EnumSpotMeasures.ALL);
	}

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

	public boolean saveSpotsAll(String directory) {
		if (directory == null) {
			return false;
		}
		return csvSaveSpots(directory);
	}

	public boolean saveSpotsMeasures(String directory) {
		if (directory == null) {
			return false;
		}
		return csvSaveSpots(directory);
	}

	// === OPTIMIZED CSV WRITING ===

	public boolean saveSpotsMeasuresOptimized(String directory) {
		if (directory == null) {
			return false;
		}
		return csvSaveSpotsOptimized(directory);
	}

	private boolean csvSaveSpotsOptimized(String directory) {
		Path csvPath = Paths.get(directory, CSV_FILENAME);
		try (FileWriter writer = new FileWriter(csvPath.toFile())) {
			// Write header sections
			writeCsvHeader(writer);

			// Write spots data in chunks to reduce memory pressure
			writeSpotsDataOptimized(writer);
			writeMeasuresDataOptimized(writer, EnumSpotMeasures.AREA_SUM);
			writeMeasuresDataOptimized(writer, EnumSpotMeasures.AREA_SUMCLEAN);

			return true;
		} catch (IOException e) {
			System.err.println("Error in optimized CSV writing: " + e.getMessage());
			return false;
		} finally {
			// Force cleanup after writing
			forcePostWritingCleanup();
		}
	}

	private void writeCsvHeader(FileWriter writer) throws IOException {
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + "SPOTS_ARRAY" + CSV_SEPARATOR + "multiSPOTS96 data\n");
		writer.write("n spots=" + CSV_SEPARATOR + spotsList.size() + "\n");
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + "SPOTS" + CSV_SEPARATOR + "multiSPOTS96 data\n");
		writer.write("name" + CSV_SEPARATOR + "index" + CSV_SEPARATOR + "cageID" + CSV_SEPARATOR + "cagePos"
				+ CSV_SEPARATOR + "cageColumn" + CSV_SEPARATOR + "cageRow" + CSV_SEPARATOR + "volume" + CSV_SEPARATOR
				+ "npixels" + CSV_SEPARATOR + "radius" + CSV_SEPARATOR + "stim" + CSV_SEPARATOR + "conc\n");
	}

	private void writeSpotsDataOptimized(FileWriter writer) throws IOException {
		int chunkSize = 100; // Process 100 spots at a time
		int processed = 0;

		for (int i = 0; i < spotsList.size(); i += chunkSize) {
			int endIndex = Math.min(i + chunkSize, spotsList.size());

			// Process chunk
			for (int j = i; j < endIndex; j++) {
				Spot spot = spotsList.get(j);
				writer.write(spot.getProperties().exportToCsv(CSV_SEPARATOR));
			}

			processed += (endIndex - i);

			// Light cleanup every 400 spots for better performance
			if (processed % 400 == 0) {
				System.gc();
				Thread.yield();
			}
		}
	}

	private void writeMeasuresDataOptimized(FileWriter writer, EnumSpotMeasures measureType) throws IOException {
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + measureType.toString() + CSV_SEPARATOR + "v0\n");
		writer.write("name" + CSV_SEPARATOR + "index" + CSV_SEPARATOR + "npts" + CSV_SEPARATOR + "yi\n");

		int chunkSize = 100; // Process 100 spots at a time
		int processed = 0;

		for (int i = 0; i < spotsList.size(); i += chunkSize) {
			int endIndex = Math.min(i + chunkSize, spotsList.size());

			// Process chunk
			for (int j = i; j < endIndex; j++) {
				Spot spot = spotsList.get(j);
				writer.write(spot.exportMeasuresOneType(measureType, CSV_SEPARATOR));
			}

			processed += (endIndex - i);

			// Light cleanup every 400 spots for better performance
			if (processed % 400 == 0) {
				System.gc();
				Thread.yield();
			}
		}
	}

	private void forcePostWritingCleanup() {
		System.gc();
		Thread.yield();
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
			System.err.println("ERROR: Null node provided for SpotsArray save");
			return false;
		}
		try {
			Node nodeSpotsArray = XMLUtil.setElement(node, ID_LISTOFSPOTS);
			if (nodeSpotsArray == null) {
				System.err.println("ERROR: Could not create List_of_spots element");
				return false;
			}

			XMLUtil.setElementIntValue(nodeSpotsArray, ID_NSPOTS, spotsList.size());

			sortSpots();
			int savedSpots = 0;
			for (int i = 0; i < spotsList.size(); i++) {
				try {
					Node nodeSpot = XMLUtil.setElement(node, ID_SPOT_ + i);
					if (nodeSpot == null) {
						System.err.println("ERROR: Could not create spot element for index " + i);
						continue;
					}

					Spot spot = spotsList.get(i);
					if (spot == null) {
						System.err.println("WARNING: Null spot at index " + i);
						continue;
					}

					boolean spotSuccess = spot.saveToXml(nodeSpot);
					if (spotSuccess) {
						savedSpots++;
					} else {
						System.err.println("ERROR: Failed to save spot at index " + i);
					}
				} catch (Exception e) {
					System.err.println("ERROR saving spot at index " + i + ": " + e.getMessage());
				}
			}
			return savedSpots > 0; // Return true if at least one spot was saved

		} catch (Exception e) {
			System.err.println("ERROR during SpotsArray save: " + e.getMessage());
			e.printStackTrace();
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
			System.err.println("ERROR: Null node provided for SpotsArray load");
			return false;
		}
		try {
			Node nodeSpotsArray = XMLUtil.getElement(node, ID_LISTOFSPOTS);
			if (nodeSpotsArray == null) {
				System.err.println("ERROR: Could not find List_of_spots element");
				return false;
			}

			int nitems = XMLUtil.getElementIntValue(nodeSpotsArray, ID_NSPOTS, 0);
			if (nitems < 0) {
				System.err.println("ERROR: Invalid number of spots: " + nitems);
				return false;
			}

			// System.out.println(" Loading " + nitems + " spots");
			spotsList.clear();

			int loadedSpots = 0;
			for (int i = 0; i < nitems; i++) {
				try {
					Node nodeSpot = XMLUtil.getElement(node, ID_SPOT_ + i);
					if (nodeSpot == null) {
						System.err.println("WARNING: Could not find spot element for index " + i);
						continue;
					}

					Spot spot = new Spot();
					boolean spotSuccess = spot.loadFromXml(nodeSpot);
					if (spotSuccess && !isSpotPresent(spot)) {
						spotsList.add(spot);
						loadedSpots++;
					} else if (!spotSuccess) {
						System.err.println("ERROR: Failed to load spot at index " + i);
					} else {
						// System.out.println(" Skipped duplicate spot at index " + i);
					}
				} catch (Exception e) {
					System.err.println("ERROR loading spot at index " + i + ": " + e.getMessage());
				}
			}
			return loadedSpots > 0; // Return true if at least one spot was loaded

		} catch (Exception e) {
			System.err.println("ERROR during SpotsArray load: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

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

	public void copySpotsInfo(SpotsArray sourceArray) {
		copySpots(sourceArray, false);
	}

	public void copySpots(SpotsArray sourceArray, boolean includeMeasurements) {
		if (sourceArray == null) {
			return;
		}

		spotsList.clear();
		for (Spot sourceSpot : sourceArray.getSpotsList()) {
			Spot spot = new Spot(sourceSpot, includeMeasurements);
			spotsList.add(spot);
		}
	}

	public void pasteSpotsInfo(SpotsArray targetArray) {
		pasteSpots(targetArray, false);
	}

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

	public void adjustSpotsLevel2DMeasuresToImageWidth(int imageWidth) {
		spotsList.forEach(spot -> spot.adjustLevel2DMeasuresToImageWidth(imageWidth));
	}

	public void cropSpotsLevel2DMeasuresToImageWidth(int imageWidth) {
		spotsList.forEach(spot -> spot.cropLevel2DMeasuresToImageWidth(imageWidth));
	}

	public void initializeLevel2DMeasures() {
		spotsList.forEach(Spot::initializeLevel2DMeasures);
	}

	public void transferMeasuresToLevel2D() {
		spotsList.forEach(Spot::transferMeasuresToLevel2D);
	}

	// === UTILITY OPERATIONS ===

	public void medianFilterFromSumToSumClean() {
		int span = 10;
		spotsList.forEach(spot -> {
			SpotMeasure sumIn = spot.getSum();
			SpotMeasure sumClean = spot.getSumClean();
			if (sumIn != null && sumClean != null) {
				sumClean.buildRunningMedianFromValuesArray(span, sumIn.getValues());
			}
		});
	}

	public double getScalingFactorToPhysicalUnits(EnumXLSExport xlsOption) {
		// Implementation would depend on specific scaling logic
		return 1.0;
	}

//	public Polygon2D get2DPolygonEnclosingSpots() {
//		if (spotsList.isEmpty()) {
//			return new Polygon2D();
//		}
//
//		// Implementation would create a polygon encompassing all spots
//		// This is a placeholder for the actual implementation
//		return new Polygon2D();
//	}

	public void setReadyToAnalyze(boolean setFilter, BuildSeriesOptions options) {
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
				spot.getProperties().importFromCsv(spotData);
			}

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

			// Save measures section
			if (!csvSaveMeasuresSection(writer, EnumSpotMeasures.AREA_SUM)) {
				return false;
			}

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
		writer.write("#" + CSV_SEPARATOR + "SPOTS_ARRAY" + CSV_SEPARATOR + "multiSPOTS96 data\n");
		writer.write("n spots=" + CSV_SEPARATOR + spotsList.size() + "\n");
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

	private boolean csvSaveMeasuresSection(FileWriter writer, EnumSpotMeasures measureType) throws IOException {
		writer.write("#" + CSV_SEPARATOR + "#\n");
		writer.write("#" + CSV_SEPARATOR + measureType.toString() + CSV_SEPARATOR + "v0\n");
		writer.write("name" + CSV_SEPARATOR + "index" + CSV_SEPARATOR + "npts" + CSV_SEPARATOR + "yi\n");

		for (Spot spot : spotsList) {
			writer.write(spot.exportMeasuresOneType(measureType, CSV_SEPARATOR));
		}

		return true;
	}

	// === UTILITY METHODS ===

	@Override
	public String toString() {
		return String.format("SpotsArray{spotsCount=%d}", spotsList.size());
	}
}