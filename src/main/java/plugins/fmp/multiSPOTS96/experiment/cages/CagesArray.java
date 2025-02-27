package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.KymoIntervals;
import plugins.fmp.multiSPOTS96.experiment.SequenceCamData;
import plugins.fmp.multiSPOTS96.experiment.spots.EnumSpotMeasures;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotString;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.tools.Comparators;
import plugins.fmp.multiSPOTS96.tools.JComponents.Dialog;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class CagesArray {
	public ArrayList<Cage> cagesList = new ArrayList<Cage>();
	private KymoIntervals cagesListTimeIntervals = null;

	public int nCagesAlongX = 6;
	public int nCagesAlongY = 8;
	public int nColumnsPerCage = 2;
	public int nRowsPerCage = 1;

	// ---------- not saved to xml:
	public long detectFirst_Ms = 0;
	public long detectLast_Ms = 0;
	public long detectBin_Ms = 60000;
	public int detect_threshold = 0;
	public int detect_nframes = 0;

	// ----------------------------
	
	private final String csvSpotsMeasuresFileName = "SpotsMeasures.csv";

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";
//	private final String ID_DROSOTRACK = "drosoTrack";
//	private final String ID_NBITEMS = "nb_items";
//	private final String ID_CAGELIMITS = "Cage_Limits";
//	private final String ID_FLYDETECTED = "Fly_Detected";

	private final String ID_NCAGESALONGX = "N_cages_along_X";
	private final String ID_NCAGESALONGY = "N_cages_along_Y";
	private final String ID_NCOLUMNSPERCAGE = "N_columns_per_cage";
	private final String ID_NROWSPERCAGE = "N_rows_per_cage";

	private final String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";
	public final String ID_MS96_cages_XML = "MS96_cages.xml";
	public final String ID_MS96_spotsMeasures_XML = "MS96_spotsMeasures.xml";
	public final String ID_MS96_fliesPositions_XML = "MS96_fliesPositions.xml";

	public CagesArray() {
	}

	public CagesArray(int ncolumns, int nrows) {
		nCagesAlongX = ncolumns;
		nCagesAlongY = nrows;
		cagesList = new ArrayList<Cage>(ncolumns * nrows);
	}

	public void clearAllMeasures(int option_detectCage) {
		for (Cage cage : cagesList) {
			int cagenb = cage.getCageNumberInteger();
			if (option_detectCage < 0 || option_detectCage == cagenb)
				cage.clearMeasures();
		}
	}

	public void removeCages() {
		cagesList.clear();
	}

	public void mergeLists(CagesArray cagesm) {
		for (Cage cagem : cagesm.cagesList) {
			if (!isPresent(cagem))
				cagesList.add(cagem);
		}
	}

	// -------------

	public boolean saveCagesMeasures(String directory) {
		csvSaveCagesMeasures(directory);
		String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
		xmlWriteCagesToFileNoQuestion(tempName);
		return true;
	}

	public boolean loadCagesMeasures(String directory) {
		// csvLoadCagesMeasures(directory);
		String tempName = directory + File.separator + ID_MCDROSOTRACK_XML;
		xmlReadCagesFromFileNoQuestion(tempName);
		return true;
	}

	// -----------------------------------------------------

	final String csvSep = ";";

	private boolean csvSaveCagesMeasures(String directory) {
		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator + "CagesMeasures.csv");
			csvSaveDescriptionSection(csvWriter);
			csvSaveMeasuresSection(csvWriter, EnumCageMeasures.POSITION);
			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean csvSaveDescriptionSection(FileWriter csvWriter) {
		try {
			csvWriter.append("#" + csvSep + "DESCRIPTION" + csvSep + "Cages data\n");
			csvWriter.append("n cages=" + csvSep + Integer.toString(cagesList.size()) + "\n");
			if (cagesList.size() > 0)
				for (Cage cage : cagesList)
					csvWriter.append(cage.csvExportCageDescription(csvSep));

			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean csvSaveMeasuresSection(FileWriter csvWriter, EnumCageMeasures measuresType) {
		try {
//			csvWriter.append("#" + csvSep + "DESCRIPTION" + csvSep + "Cages data\n");
//			csvWriter.append("n cages=" + csvSep + Integer.toString(cagesList.size()) + "\n");
//			if (cagesList.size() > 0) {
//				for (Cage cage : cagesList)
//					csvWriter.append(cage.csvExportCageDescription(csvSep));
//			}
			csvWriter.append("#" + csvSep + "#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	// ----------------------------------------------------

	public boolean xmlReadCagesFromFile(Experiment exp) {
		String[] filedummy = null;
		String filename = exp.getResultsDirectory();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = Dialog.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i = 0; i < filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(csFile, exp);
			}
		}
		return wasOk;
	}

	public boolean xmlReadCagesFromFileNoQuestion(String tempname) {
		if (tempname == null)
			return false;
		final Document doc = XMLUtil.loadDocument(tempname);
		if (doc == null)
			return false;

		if (xmlLoadCages(XMLUtil.getRootElement(doc))) {
			return true;
		} else {
			System.out.println("Cages:xmlReadCagesFromFileNoQuestion() failed to load cages from file");
			return false;
		}
	}

	public boolean xmlReadCagesFromFileNoQuestion(String tempname, Experiment exp) {
		if (tempname == null)
			return false;
		final Document doc = XMLUtil.loadDocument(tempname);
		if (doc == null)
			return false;

		if (xmlLoadCages(XMLUtil.getRootElement(doc))) {
			transferCagesToSequenceAsROIs(exp.seqCamData);
			return true;
		} else {
			System.out.println("Cages:xmlReadCagesFromFileNoQuestion() failed to load cages from file");
			return false;
		}
	}

	public boolean xmlWriteCagesToFileNoQuestion(String tempname) {
		if (tempname == null)
			return false;
		final Document doc = XMLUtil.createDocument(true);
		if (doc == null)
			return false;

		Node node = XMLUtil.getRootElement(doc);
		xmlSaveCages(node);
		return XMLUtil.saveDocument(doc, tempname);
	}

	private boolean xmlSaveCages(Node node) {
		int index = 0;
		Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
		int ncages = cagesList.size();
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGX, nCagesAlongX);
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGESALONGY, nCagesAlongY);
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, nColumnsPerCage);
		XMLUtil.setAttributeIntValue(xmlVal, ID_NROWSPERCAGE, nRowsPerCage);
		for (Cage cage : cagesList) {
			cage.xmlSaveCage(xmlVal, index);
			index++;
		}
		return true;
	}

	private boolean xmlLoadCages(Node node) {
		cagesList.clear();
		Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
		if (xmlVal == null)
			return false;

		int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
		nCagesAlongX = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGX, nCagesAlongX);
		nCagesAlongY = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGESALONGY, nCagesAlongY);
		nColumnsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NCOLUMNSPERCAGE, nColumnsPerCage);
		nRowsPerCage = XMLUtil.getAttributeIntValue(xmlVal, ID_NROWSPERCAGE, nRowsPerCage);
		for (int index = 0; index < ncages; index++) {
			Cage cage = new Cage();
			cage.xmlLoadCage(xmlVal, index);
			cagesList.add(cage);
		}

		return true;
	}

	// --------------

	public void copy(ArrayList<Cage> cagesListFrom, boolean bCopyMeasures) {
		cagesList.clear();
		for (Cage cageFrom : cagesListFrom) {
			Cage cageTo = new Cage();
			cageTo.copyCage(cageFrom, bCopyMeasures);
			cagesList.add(cageTo);
		}
	}

	// --------------

	private boolean isPresent(Cage cagenew) {
		boolean flag = false;
		for (Cage cage : cagesList) {
			if (cage.getRoi().getName().contentEquals(cagenew.getRoi().getName())) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	private void addMissingCages(List<ROI2D> roiList) {
		for (ROI2D roi : roiList) {
			boolean found = false;
			if (roi.getName() == null)
				break;
			for (Cage cage : cagesList) {
				if (cage.getRoi() == null)
					break;
				if (roi.getName().equals(cage.getRoi().getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Cage cage = new Cage();
				cage.setRoi((ROI2DShape) roi);
				cagesList.add(cage);
			}
		}
	}

	private void removeOrphanCages(List<ROI2D> roiList) {
		// remove cages with names not in the list
		Iterator<Cage> iterator = cagesList.iterator();
		while (iterator.hasNext()) {
			Cage cage = iterator.next();
			boolean found = false;
			if (cage.getRoi() != null) {
				String cageRoiName = cage.getRoi().getName();
				for (ROI2D roi : roiList) {
					if (roi.getName().equals(cageRoiName)) {
						found = true;
						break;
					}
				}
			}
			if (!found)
				iterator.remove();
		}
	}

	public List<ROI2D> getROIsWithCageName(SequenceCamData seqCamData) {
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		List<ROI2D> roisCageList = new ArrayList<ROI2D>();
		for (ROI2D roi : roiList) {
			String csName = roi.getName();
			if ((roi instanceof ROI2DPolygon) || (roi instanceof ROI2DArea)) {
				if ((csName.length() > 4 && csName.substring(0, 4).contains("cage") || csName.contains("Polygon2D")))
					roisCageList.add(roi);
			}
		}
		return roisCageList;
	}

	public Cage getCageFromRowColCoordinates(int row, int column) {
		Cage cage_found = null;
		for (Cage cage : cagesList) {
			if (cage.arrayColumn == column && cage.arrayRow == row) {
				cage_found = cage;
				break;
			}
		}
		return cage_found;
	}

	// --------------

	public void transferCagesToSequenceAsROIs(SequenceCamData seqCamData) {
		seqCamData.removeROIsContainingString("cage");
		List<ROI2D> cageROIList = new ArrayList<ROI2D>(cagesList.size());
		for (Cage cage : cagesList)
			cageROIList.add(cage.getRoi());
		seqCamData.seq.addROIs(cageROIList, true);
	}

	public void transferROIsFromSequenceToCages(SequenceCamData seqCamData) {
		List<ROI2D> roiList = seqCamData.getROIsContainingString("cage");
		Collections.sort(roiList, new Comparators.ROI2D_Name_Comparator());
		addMissingCages(roiList);
		removeOrphanCages(roiList);
		Collections.sort(cagesList, new Comparators.Cage_Name_Comparator());
	}

	public void removeAllRoiDetFromSequence(SequenceCamData seqCamData) {
		ArrayList<ROI2D> seqlist = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : seqlist) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seqCamData.seq.removeROI(roi);
		}
	}

	public void transferNFliesFromCagesToSpots() {

		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				spot.prop.spotNFlies = cage.cageNFlies;
			}
		}
	}

	public void transferNFliesFromSpotsToCages(SpotsArray spotsArray) {
		for (Cage cage : cagesList) {
			int cagenb = cage.getCageNumberInteger();
			for (Spot spot : spotsArray.spotsList) {
				if (spot.cageID != cagenb)
					continue;
				cage.cageNFlies = spot.prop.spotNFlies;
			}
		}
	}

	public Cage getCageFromNumber(int number) {
		Cage cageFound = null;
		for (Cage cage : cagesList) {
			if (number == cage.getCageNumberInteger()) {
				cageFound = cage;
				break;
			}
		}
		return cageFound;
	}

	public Cage getCageFromID(int cageID) {
		Cage cageFound = null;
		for (Cage cage : cagesList) {
			if (cageID == cage.cageID) {
				cageFound = cage;
				break;
			}
		}
		return cageFound;
	}

	public List<ROI2D> getPositionsAsListOfROI2DRectanglesAtT(int t) {
		List<ROI2D> roiRectangleList = new ArrayList<ROI2D>(cagesList.size());
		for (Cage cage : cagesList) {
			ROI2D roiRectangle = cage.getRoiRectangleFromPositionAtT(t);
			if (roiRectangle != null)
				roiRectangleList.add(roiRectangle);
		}
		return roiRectangleList;
	}

	public void orderFlyPositions() {
		for (Cage cage : cagesList)
			Collections.sort(cage.flyPositions.flyPositionList, new Comparators.XYTaValue_Tindex_Comparator());
	}

	public void initFlyPositions(int option_cagenumber) {
		int nbcages = cagesList.size();
		for (int i = 0; i < nbcages; i++) {
			Cage cage = cagesList.get(i);
			if (option_cagenumber != -1 && cage.getCageNumberInteger() != option_cagenumber)
				continue;
			if (cage.cageNFlies > 0) {
				cage.flyPositions = new FlyPositions();
				cage.flyPositions.ensureCapacity(detect_nframes);
			}
		}
	}

	// ----------------

	public void computeBooleanMasksForCages() {
		for (Cage cage : cagesList) {
			try {
				cage.computeCageBooleanMask2D();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public int getLastIntervalFlyAlive(int cagenumber) {
		int flypos = -1;
		for (Cage cage : cagesList) {
			String cagenumberString = cage.getRoi().getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) {
				flypos = cage.flyPositions.getLastIntervalAlive();
				break;
			}
		}
		return flypos;
	}

	public boolean isFlyAlive(int cagenumber) {
		boolean isalive = false;
		for (Cage cage : cagesList) {
			String cagenumberString = cage.getRoi().getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) {
				isalive = (cage.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}

	public boolean isDataAvailable(int cagenumber) {
		boolean isavailable = false;
		for (Cage cage : cagesList) {
			String cagenumberString = cage.getRoi().getName().substring(4);
			if (Integer.valueOf(cagenumberString) == cagenumber) {
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}

	public int getHorizontalSpanOfCages() {
		int leftPixel = -1;
		int rightPixel = -1;

		for (Cage cage : cagesList) {
			ROI2D roiCage = cage.getRoi();
			Rectangle2D rect = roiCage.getBounds2D();
			int left = (int) rect.getX();
			int right = left + (int) rect.getWidth();
			if (leftPixel < 0 || left < leftPixel)
				leftPixel = left;
			if (right > rightPixel)
				rightPixel = right;
		}

		return rightPixel - leftPixel;
	}

	public Polygon2D getPolygon2DEnclosingAllCages() {
		if (cagesList.size() < 1 || cagesList.get(0).getRoi() == null)
			return null;
		Polygon2D polygon = getCoordinatesOfROI(cagesList.get(0).getRoi());
		for (Cage cage : cagesList) {
			int col = cage.arrayColumn;
			int row = cage.arrayRow;
			Polygon2D n = getCoordinatesOfROI(cage.getRoi());
			if (col == 0 && row == 0) {
				transferPointToPolygon(0, polygon, n);
			} else if (col >= (nCagesAlongX - 1) && row == 0) {
				transferPointToPolygon(3, polygon, n);
			} else if (col == (nCagesAlongX - 1) && row == (nCagesAlongY - 1)) {
				transferPointToPolygon(2, polygon, n);
			} else if (col == 0 && row >= (nCagesAlongY - 1)) {
				transferPointToPolygon(1, polygon, n);
			}
		}
		return polygon;
	}

	private void transferPointToPolygon(int i, Polygon2D dest, Polygon2D source) {
		dest.xpoints[i] = source.xpoints[i];
		dest.ypoints[i] = source.ypoints[i];
	}

	private Polygon2D getCoordinatesOfROI(ROI2D roi) {
		Polygon2D polygon = null;
		if (roi instanceof ROI2DPolygon) {
			polygon = ((ROI2DPolygon) roi).getPolygon2D();
		} else {
			Rectangle rect = roi.getBounds();
			polygon = new Polygon2D(rect);
		}
		return polygon;
	}

	// --------------------------------------------------------

	public void transferCageSpotsToSequenceAsROIs(SequenceCamData seqCamData) {
		seqCamData.removeROIsContainingString("spot");
		if (cagesList.size() > 0) {
			List<ROI2D> spotROIList = new ArrayList<ROI2D>(
					cagesList.get(0).spotsArray.spotsList.size() * cagesList.size());
			for (Cage cage : cagesList) {
				for (Spot spot : cage.spotsArray.spotsList)
					spotROIList.add(spot.getRoi());
			}
			seqCamData.seq.addROIs(spotROIList, true);
		}
	}

	public void transferROIsFromSequenceToCageSpots(SequenceCamData seqCamData) {
		List<ROI2D> listROISSpot = seqCamData.getROIsContainingString("spot");
		Collections.sort(listROISSpot, new Comparators.ROI_Name_Comparator());

		for (ROI2D roi : listROISSpot) {
			String roiName = roi.getName();
			if (roi instanceof ROI2DShape) {
				for (Cage cage : cagesList) {
					for (Spot spot : cage.spotsArray.spotsList) {
						String spotRoiName = spot.getRoi().getName();
						if (roiName.equals(spotRoiName)) {
							spot.setRoi((ROI2DShape) roi);
							spot.valid = true;
						}
					}
				}
			}
		}
	}

	public Spot getSpotFromROIName(String name) {
		Spot spotFound = null;
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().getName().contains(name)) {
					spotFound = spot;
					break;
				}
			}
			if (spotFound != null)
				break;
		}
		return spotFound;
	}

	public void initCagesAndSpotsWithNFlies(int nflies) {
		for (Cage cage : cagesList) {
			cage.cageNFlies = nflies;
			cage.setNFlies(nflies);
		}
	}

	public ArrayList<Spot> getSpotsEnclosed(ROI2DPolygon envelopeRoi) {
		if (envelopeRoi == null)
			return getSpotsSelected();

		ArrayList<Spot> enclosedSpots = new ArrayList<Spot>();
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				try {
					if (envelopeRoi.contains(spot.getRoi())) {
						spot.getRoi().setSelected(true);
						enclosedSpots.add(spot);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return enclosedSpots;
	}

	public ArrayList<Spot> getSpotsSelected() {
		ArrayList<Spot> enclosedSpots = new ArrayList<Spot>();
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().isSelected())
					enclosedSpots.add(spot);
			}
		}
		return enclosedSpots;
	}

	public Spot getSpotAtGlobalIndex(int indexT) {
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				ROI2D roi = spot.getRoi();
				int index = SpotString.getSpotArrayIndexFromSpotName(roi.getName());
				if (index == indexT) {
					return spot;
				}
			}
		}
		return null;
	}

	public int getTotalNumberOfSpots() {
		int nspots = 0;
		for (Cage cage : cagesList) {
			nspots += cage.spotsArray.spotsList.size();
		}
		return nspots;
	}

	public KymoIntervals getKymoIntervalsFromSpotsOFCage0() {
		Cage cage = cagesList.get(0);
		KymoIntervals intervals = cage.spotsArray.getKymoIntervalsFromSpots();
		return intervals;
	}

	public void mergeSpotsLists(CagesArray arrayToMerge) {
		for (Cage cage : cagesList) {
			for (Cage cageToMerge : arrayToMerge.cagesList) {
				if (cage.cagePosition != cageToMerge.cagePosition)
					continue;
				cage.spotsArray.mergeLists(cageToMerge.spotsArray);
			}
		}
	}

	public void setFilterOfSpotsToAnalyze(boolean setFilter, BuildSeriesOptions options) {
		for (Cage cage : cagesList) {
			cage.spotsArray.setFilterOfSpotsToAnalyze(setFilter, options);
		}
	}

	public void transferSumToSumClean() {
		for (Cage cage : cagesList) {
			cage.spotsArray.transferSumToSumClean();
		}
	}

	public void initLevel2DMeasures() {
		for (Cage cage : cagesList) {
			cage.spotsArray.initLevel2DMeasures();
		}
	}

	public boolean zzload_Spots(String resultsDirectory) {
		return false;
	}

	public void transferROIsMeasuresFromSequenceToSpots() {
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				spot.transferROIsMeasuresToLevel2D();
			}
		}
	}

	public void transferSpotsMeasuresToSequenceAsROIs(Sequence seq) {
		List<ROI2D> seqRoisList = seq.getROI2Ds(false);
		ROI2DUtilities.removeROI2DsMissingChar(seqRoisList, '_');
		List<ROI2D> newRoisList = new ArrayList<ROI2D>();
		int height = seq.getHeight();
		int i = 0;
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				List<ROI2D> listOfRois = spot.transferSpotMeasuresToROIs(height);
				for (ROI2D roi : listOfRois) {
					if (roi != null)
						roi.setT(i);
				}
				newRoisList.addAll(listOfRois);
				i++;
			}
		}
		ROI2DUtilities.mergeROI2DsListNoDuplicate(seqRoisList, newRoisList, seq);
		seq.removeAllROI();
		seq.addROIs(seqRoisList, false);
	}

	// ------------------------------------------------

	public KymoIntervals getKymoIntervalsFromSpots() {
		if (cagesListTimeIntervals == null) {
			cagesListTimeIntervals = new KymoIntervals();
			for (Cage cage : cagesList) {
				for (ROI2DAlongT roiFK : cage.getROIAlongTList()) {
					Long[] interval = { roiFK.getT(), (long) -1 };
					cagesListTimeIntervals.addIfNew(interval);
				}
			}
		}
		return cagesListTimeIntervals;
	}

	public int findKymoROI2DIntervalStart(long intervalT) {
		return cagesListTimeIntervals.findStartItem(intervalT);
	}

	public long getKymoROI2DIntervalsStartAt(int selectedItem) {
		return cagesListTimeIntervals.get(selectedItem)[0];
	}

	public int addKymoROI2DInterval(long start) {
		Long[] interval = { start, (long) -1 };
		int item = cagesListTimeIntervals.addIfNew(interval);

		for (Cage cage : cagesList) {
			List<ROI2DAlongT> listROI2DForKymo = cage.getROIAlongTList();
			ROI2D roi = cage.getRoi();
			if (item > 0)
				roi = (ROI2D) listROI2DForKymo.get(item - 1).getRoi_in().getCopy();
			listROI2DForKymo.add(item, new ROI2DAlongT(start, roi));
		}
		return item;
	}

	public void deleteKymoROI2DInterval(long start) {
		cagesListTimeIntervals.deleteIntervalStartingAt(start);
		for (Cage cage : cagesList)
			cage.removeROIAlongTListItem(start);
	}
	
	// --------------------------------------------------
	public boolean load_SpotsMeasures(String directory) {
		boolean flag = false;
		try {
			flag = csvLoadSpots(directory, EnumSpotMeasures.SPOTS_MEASURES);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}

	public boolean load_SpotsAll(String directory) {
		boolean flag = false;
		try {
			flag = csvLoadSpots(directory, EnumSpotMeasures.ALL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}

	public boolean save_SpotsAll(String directory) {
		boolean flag = false;
		try {
			flag = csvSaveSpots(directory);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}

	public boolean save_SpotsMeasures(String directory) {
		if (directory == null)
			return false;

		csvSaveSpots(directory);
		return true;
	}

	private boolean csvLoadSpots(String directory, EnumSpotMeasures option) throws Exception {
		String pathToCsv = directory + File.separator + csvSpotsMeasuresFileName;
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
				case "DESCRIPTION":
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

	public Cage getCageFromSpotRoiName(String name) {
		int cageID = SpotString.getCageIDFromSpotName(name);
		for (Cage cage: cagesList) {
			if (cage.cageID == cageID)
				return cage;
		}
		return null;
	}
	
	private String csvLoadSpotsArray(BufferedReader csvReader, String csvSep) {
		String row;
		try {
			row = csvReader.readLine();
			String[] data0 = row.split(csvSep);
			boolean dummyColumn = data0[0].contains("prefix");

			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(csvSep);
				if (data[0].equals("#"))
					return data[1];
				
				String name = data[dummyColumn ? 2 : 1];
				Cage cage = getCageFromSpotRoiName(name);
				if (cage == null) {
					System.out.println("cage not found in csvLoadSpotsArray");
					continue;
				}
					
				Spot spot = cage.getSpotFromRoiName(name);
				if (spot == null)
					spot = new Spot();
				spot.csvImportDescription(data, dummyColumn);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private String csvLoadSpotsDescription(BufferedReader csvReader, String csvSep) {
		String row;
		try {
			row = csvReader.readLine();
			row = csvReader.readLine();
			
			spotsDescription.csvImportSpotsDescriptionData(row);
			
			row = csvReader.readLine();
			String[] data = row.split(csvSep);
			if (data[0].substring(0, Math.min(data[0].length(), 5)).equals("n spot")) {
				int nspots = Integer.valueOf(data[1]);
				if (nspots >= spotsList.size())
					spotsList.ensureCapacity(nspots);
				else
					spotsList.subList(nspots, spotsList.size()).clear();
				row = csvReader.readLine();
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

}
