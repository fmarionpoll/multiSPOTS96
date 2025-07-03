package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.TInterval;
import plugins.fmp.multiSPOTS96.experiment.TIntervalsArray;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multiSPOTS96.tools.polyline.Level2D;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExport;

public class SpotsArray {
	public ArrayList<Spot> spotsList = new ArrayList<Spot>();
	private TIntervalsArray spotsListTimeIntervals = null;

	private final static String ID_SPOTTRACK = "spotTrack";
	private final static String ID_NSPOTS = "N_spots";

	private final static String ID_LISTOFSPOTS = "List_of_spots";
	private final static String ID_SPOT_ = "spot_";
	private final String csvFileName = "SpotsMeasures.csv";

	// ---------------------------------

	public boolean load_SpotsMeasures(String directory) {
		return load_Spots(directory, EnumSpotMeasures.SPOTS_MEASURES);
	}

	public boolean load_SpotsAll(String directory) {
		return load_Spots(directory, EnumSpotMeasures.ALL);
	}

	private boolean load_Spots(String directory, EnumSpotMeasures option) {
		boolean flag = false;
		if (directory == null)
			return flag;
		try {
			flag = csvLoadSpots(directory, option);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}

	public boolean save_SpotsAll(String directory) {
		if (directory == null)
			return false;

		return csvSaveSpots(directory);
	}

	public boolean save_SpotsMeasures(String directory) {
		if (directory == null)
			return false;

		return csvSaveSpots(directory);
	}

	// ---------------------------------

	public boolean xmlSaveSpotsArray(Node node) {
		Node nodeSpotsArray = XMLUtil.setElement(node, ID_LISTOFSPOTS);
		XMLUtil.setElementIntValue(nodeSpotsArray, ID_NSPOTS, spotsList.size());
		int i = 0;
		Collections.sort(spotsList);
		for (Spot spot : spotsList) {
			Node nodeSpot = XMLUtil.setElement(node, ID_SPOT_ + i);
			spot.saveToXML_SpotOnly(nodeSpot);
			i++;
		}
		return true;
	}

	public boolean xmlLoadSpotsArray(Node node) {
		Node nodeSpotsArray = XMLUtil.getElement(node, ID_LISTOFSPOTS);
		int nitems = XMLUtil.getElementIntValue(nodeSpotsArray, ID_NSPOTS, 0);
		spotsList = new ArrayList<Spot>(nitems);
		for (int i = 0; i < nitems; i++) {
			Node nodespot = XMLUtil.getElement(node, ID_SPOT_ + i);
			Spot spot = new Spot();
			spot.loadFromXML_SpotOnly(nodespot);
			if (!isPresent(spot))
				spotsList.add(spot);
		}
		return true;
	}

	private boolean xmlSave_ListOfSpots(Node nodedoc) {

		Node node = XMLUtil.getElement(nodedoc, ID_SPOTTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
		Node nodeSpotsArray = XMLUtil.setElement(node, ID_LISTOFSPOTS);
		XMLUtil.setElementIntValue(nodeSpotsArray, ID_NSPOTS, spotsList.size());

		int i = 0;
		Collections.sort(spotsList);
		for (Spot spot : spotsList) {
			Node nodeSpot = XMLUtil.setElement(node, ID_SPOT_ + i);
			spot.saveToXML_SpotOnly(nodeSpot);
			i++;
		}
		return true;
	}

	public boolean xmlSave_MCSpots_Descriptors(String csFileName) {
		if (csFileName != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) {
				xmlSave_ListOfSpots(XMLUtil.getRootElement(doc));
				return XMLUtil.saveDocument(doc, csFileName);
			}
		}
		return false;
	}

	public boolean xmlLoad_MCSpots_Descriptors(String csFileName) {
		boolean flag = false;
		if (csFileName == null)
			return flag;

		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			flag = xmlLoad_Spots_Only_v1(doc);
		}
		return flag;
	}

	private boolean xmlLoad_Spots_Only_v1(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_SPOTTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFSPOTS);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NSPOTS, 0);

		spotsList = new ArrayList<Spot>(nitems);
		for (int i = 0; i < nitems; i++) {
			Node nodecapillary = XMLUtil.getElement(node, ID_SPOT_ + i);
			Spot spot = new Spot();
			spot.loadFromXML_SpotOnly(nodecapillary);

			if (!isPresent(spot))
				spotsList.add(spot);
		}
		return true;
	}

	// ---------------------------------

	public void copySpotsInfos(SpotsArray fromSpotsArray) {
		copySpots(fromSpotsArray, false);
	}

	public void copySpots(SpotsArray fromSpotsArray, boolean bMeasures) {
		spotsList.clear();
		spotsList.ensureCapacity(fromSpotsArray.spotsList.size());
		for (Spot fromSpot : fromSpotsArray.spotsList) {
			Spot spot = new Spot();
			spot.copySpot(fromSpot, bMeasures);
			spotsList.add(spot);
		}
	}

	public void pasteSpotsInfos(SpotsArray toSpotsArray) {
		pasteSpots(toSpotsArray, false);
	}

	public void pasteSpots(SpotsArray toSpotsArray, boolean bMeasures) {
		for (Spot toSpot : toSpotsArray.spotsList) {
			for (Spot spot : spotsList) {
				if (spot.compareTo(toSpot) == 0) {
					toSpot.copySpot(spot, bMeasures);
					break;
				}
			}
		}
	}

	public boolean isPresent(Spot spotNew) {
		boolean flag = false;
		for (Spot spot : spotsList) {
			if (spot.compareTo(spotNew) == 0) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	public void mergeLists(SpotsArray sourceSpotList) {
		for (Spot spot : sourceSpotList.spotsList) {
			if (!isPresent(spot))
				spotsList.add(spot);
		}
	}

	public void adjustSpotsLevel2DMeasuresToImageWidth(int imageWidth) {
		for (Spot spot : spotsList)
			spot.adjustLevel2DMeasuresToImageWidth(imageWidth);
	}

	public void cropSpotsLevel2DMeasuresToImageWidth(int imageWidth) {
		for (Spot spot : spotsList)
			spot.cropLevel2DMeasuresToImageWidth(imageWidth);
	}

	public Spot getSpotFromName(String name) {
		Spot spotFound = null;
		for (Spot spot : spotsList) {
			if (spot.getRoi().getName().equals(name)) {
				spotFound = spot;
				break;
			}
		}
		return spotFound;
	}

	public Spot getSpotContainingName(String name) {
		Spot spotFound = null;
		for (Spot spot : spotsList) {
			if (spot.getRoi().getName().contains(name)) {
				spotFound = spot;
				break;
			}
		}
		return spotFound;
	}

	public boolean removeSpotFromArray(Spot spotToRemove) {
		for (int i = 0; i < spotsList.size(); i++) {
			Spot spot = spotsList.get(i);
			if (spot.equals(spotToRemove)) {
				spotsList.remove(i);
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------

	public double getScalingFactorToPhysicalUnits(EnumXLSExport xlsoption) {
		double scalingFactorToPhysicalUnits = 1.;
		return scalingFactorToPhysicalUnits;
	}

	public Polygon2D get2DPolygonEnclosingSpots() {
		Rectangle outerRectangle = null;
		for (Spot spot : spotsList) {
			Rectangle rect = spot.getRoi().getBounds();
			if (outerRectangle == null) {
				outerRectangle = rect;
			} else
				outerRectangle.add(rect);
		}
		if (outerRectangle == null)
			return null;

		return new Polygon2D(outerRectangle);
	}

	public void transferSumToSumClean() {
		int span = 10;
		for (Spot spot : spotsList) {
			if (spot.sum_in.values != null)
				spot.sum_clean.buildRunningMedian(span, spot.sum_in.values);
			else {
				Level2D level = spot.sum_in.getLevel2D();
				if (level != null && level.npoints > 0)
					spot.sum_clean.buildRunningMedian(span, level.ypoints);
			}
		}
	}

	public void initLevel2DMeasures() {
		for (Spot spot : spotsList)
			spot.initLevel2DMeasures();
	}

	// ------------------------------------------------

	public TIntervalsArray getKymoIntervalsFromSpots() {
		if (spotsListTimeIntervals == null) {
			spotsListTimeIntervals = new TIntervalsArray();
			for (Spot spot : spotsList) {
				for (ROI2DAlongT roiFK : spot.getROIAlongTList()) {
					TInterval interval = new TInterval(roiFK.getT(), (long) -1);
					spotsListTimeIntervals.addIfNew(interval);
				}
			}
		}
		return spotsListTimeIntervals;
	}

	public int findKymoROI2DIntervalStart(long intervalT) {
		return spotsListTimeIntervals.findStartItem(intervalT);
	}

	public long getKymoROI2DIntervalsStartAt(int selectedItem) {
		return spotsListTimeIntervals.getTIntervalAt(selectedItem).start;
	}

	public int addKymoROI2DInterval(long start) {
		TInterval interval = new TInterval(start, (long) -1);
		int item = spotsListTimeIntervals.addIfNew(interval);

		for (Spot spot : spotsList) {
			List<ROI2DAlongT> listROI2DForKymo = spot.getROIAlongTList();
			ROI2D roi = spot.getRoi();
			if (item > 0)
				roi = (ROI2D) listROI2DForKymo.get(item - 1).getRoi_in().getCopy();
			listROI2DForKymo.add(item, new ROI2DAlongT(start, roi));
		}
		return item;
	}

	public void deleteKymoROI2DInterval(long start) {
		spotsListTimeIntervals.deleteIntervalStartingAt(start);
		for (Spot spot : spotsList)
			spot.removeROIAlongTListItem(start);
	}

	// --------------------------------

	final String csvSep = ";";

	private boolean csvLoadSpots(String directory, EnumSpotMeasures option) throws Exception {
		String pathToCsv = directory + File.separator + csvFileName;
		File csvFile = new File(pathToCsv);
		if (!csvFile.isFile())
			return false;

		BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToCsv));
		String row;
		String sep = ";";
		while ((row = bufferedReader.readLine()) != null) {
			if (row.charAt(0) == '#')
				sep = String.valueOf(row.charAt(1));

			String[] data = row.split(sep);
			if (data[0].equals("#")) {
				switch (data[1]) {
				case "SPOTS_ARRAY":
					csvLoadSpotsDescription(bufferedReader, sep);
					break;
				case "SPOTS":
					csvLoadSpotsArray(bufferedReader, sep);
					break;
				case "AREA_SUM":
					csvLoadSpotsMeasures(bufferedReader, EnumSpotMeasures.AREA_SUM, sep);
					break;
				case "AREA_OUT":
					csvLoadSpotsMeasures(bufferedReader, EnumSpotMeasures.AREA_OUT, sep);
					break;
				case "AREA_DIFF":
					csvLoadSpotsMeasures(bufferedReader, EnumSpotMeasures.AREA_DIFF, sep);
					break;
				case "AREA_SUMCLEAN":
					csvLoadSpotsMeasures(bufferedReader, EnumSpotMeasures.AREA_SUMCLEAN, sep);
					break;
				case "AREA_FLYPRESENT":
					csvLoadSpotsMeasures(bufferedReader, EnumSpotMeasures.AREA_FLYPRESENT, sep);
					break;
				default:
					break;
				}
			}
		}
		bufferedReader.close();
		return true;
	}

	private String csvLoadSpotsArray(BufferedReader csvReader, String csvSep) {
		String row;
		try {
			row = csvReader.readLine();
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(csvSep);
				if (data[0].equals("#"))
					return data[1];
				Spot spot = getSpotFromName(data[0]);
				if (spot == null)
					spot = new Spot();
//				spot.prop.csvImportProperties(data); // TODO check if possible
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String csvLoadSpotsMeasures(BufferedReader csvReader, EnumSpotMeasures measureType, String csvSep) {
		String row;
		try {
			row = csvReader.readLine();
			boolean y = true;
			boolean x = row.contains("xi");
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(csvSep);
				if (data[0].equals("#"))
					return data[1];

				Spot spot = getSpotFromName(data[0]);
				if (spot == null)
					spot = new Spot();
				spot.csvImportMeasures_OneType(measureType, data, x, y);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------

	private boolean csvSaveSpots(String directory) {
		Path path = Paths.get(directory);
		if (!Files.exists(path))
			return false;

		try {
			FileWriter fileWriter = new FileWriter(directory + File.separator + csvFileName);
			csvSave_SpotsArraySection(fileWriter);
			csvSave_DescriptionSection(fileWriter);
			csvSave_MeasuresSection(fileWriter, EnumSpotMeasures.AREA_SUM);
			csvSave_MeasuresSection(fileWriter, EnumSpotMeasures.AREA_SUMCLEAN);
			csvSave_MeasuresSection(fileWriter, EnumSpotMeasures.AREA_OUT);
			csvSave_MeasuresSection(fileWriter, EnumSpotMeasures.AREA_DIFF);
			csvSave_MeasuresSection(fileWriter, EnumSpotMeasures.AREA_FLYPRESENT);
			fileWriter.flush();
			fileWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private String csvLoadSpotsDescription(BufferedReader csvReader, String csvSep) {
		String row;
		try {
			row = csvReader.readLine();
			String[] data = row.split(csvSep);
			String motif = data[0].substring(0, Math.min(data[0].length(), 6));
			if (motif.equals("n spot")) {
				int nspots = Integer.valueOf(data[1]);
				if (nspots >= spotsList.size())
					spotsList.ensureCapacity(nspots);
				else
					spotsList.subList(nspots, spotsList.size()).clear();
				row = csvReader.readLine();
				if (row != null)
					data = row.split(csvSep);
			}
			if (data[0].equals("#")) {
				return data[1];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean csvSave_SpotsArraySection(FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "#\n");
			csvWriter.append("#" + csvSep + "SPOTS_ARRAY" + csvSep + "multiSPOTS96 data\n");
			csvWriter.append("n spots=" + csvSep + Integer.toString(spotsList.size()) + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean csvSave_DescriptionSection(FileWriter csvWriter) {
		try {
			if (spotsList.size() > 0) {
				csvWriter.append(SpotProperties.csvExportSpotPropertiesHeader(csvSep));
				for (Spot spot : spotsList) {
					spot.prop.sourceName = spot.getRoi().getName();
					csvWriter.append(spot.prop.csvExportSpotProperties(csvSep));
				}
				csvWriter.append("#" + csvSep + "#\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean csvSave_MeasuresSection(FileWriter csvWriter, EnumSpotMeasures measureType) {
		try {
			if (spotsList.size() <= 1)
				return false;
			csvWriter.append(spotsList.get(0).csvExportMeasures_SectionHeader(measureType, csvSep));
			for (Spot spot : spotsList) {
				csvWriter.append(spot.csvExportMeasures_OneType(measureType, csvSep));
			}
			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	// ----------------------------------

	public void setFilterOfSpotsToAnalyze(boolean setFilter, BuildSeriesOptions options) {
		for (Spot spot : spotsList) {
			spot.okToAnalyze = true;
			if (!setFilter)
				continue;

			if (options.detectSelectedROIs && !spot.isIndexSelected(options.selectedIndexes))
				spot.okToAnalyze = false;
		}
	}

}