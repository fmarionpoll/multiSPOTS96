package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
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
import plugins.fmp.multiSPOTS96.experiment.sequence.ROIOperation;
import plugins.fmp.multiSPOTS96.experiment.sequence.SequenceCamData;
import plugins.fmp.multiSPOTS96.experiment.sequence.TInterval;
import plugins.fmp.multiSPOTS96.experiment.sequence.TIntervalsArray;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DValidationException;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotString;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.tools.Comparators;
import plugins.fmp.multiSPOTS96.tools.JComponents.Dialog;
import plugins.fmp.multiSPOTS96.tools.JComponents.exceptions.FileDialogException;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class CagesArray {
	public ArrayList<Cage> cagesList = new ArrayList<Cage>();
	private TIntervalsArray cagesListTimeIntervals = null;

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

	private final String ID_CAGES = "Cages";
	private final String ID_NCAGES = "n_cages";

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

	public CagesArray(ArrayList<Cage> cagesListFrom) {
		copyCagesInfos(cagesListFrom);
	}

	public CagesArray(int ncolumns, int nrows) {
		nCagesAlongX = ncolumns;
		nCagesAlongY = nrows;
		cagesList = new ArrayList<Cage>(ncolumns * nrows);
	}

	public void clearAllMeasures(int option_detectCage) {
		for (Cage cage : cagesList) {
			if (option_detectCage < 0 || option_detectCage == cage.prop.cageID)
				cage.clearMeasures();
		}
	}

	public void removeCages() {
		cagesList.clear();
	}

	public void mergeLists(CagesArray cageArrayToMerge) {
		for (Cage cageAdded : cageArrayToMerge.cagesList) {
			if (!isPresent(cageAdded))
				cagesList.add(cageAdded);
		}
	}

	public void copyCagesInfos(ArrayList<Cage> cagesListFrom) {
		copyCages(cagesListFrom, false);
	}

	public void copyCages(ArrayList<Cage> cagesListFrom, boolean bMeasures) {
		cagesList.clear();
		nCagesAlongX = 0;
		nCagesAlongY = 0;
		for (Cage cageFrom : cagesListFrom) {
			Cage cage = new Cage();
			cage.copyCage(cageFrom, bMeasures);
			cagesList.add(cage);
			if (nCagesAlongX < cageFrom.prop.arrayColumn)
				nCagesAlongX = cageFrom.prop.arrayColumn;
			if (nCagesAlongY < cageFrom.prop.arrayRow)
				nCagesAlongY = cageFrom.prop.arrayRow;
		}
	}

	public void pasteCagesInfos(ArrayList<Cage> cagesListTo) {
		pasteCages(cagesListTo, false);
	}

	public void pasteCages(ArrayList<Cage> cagesListTo, boolean bMeasures) {
		for (Cage cageTo : cagesListTo) {
			int fromID = cageTo.prop.cageID;
			for (Cage cage : cagesList) {
				if (cage.prop.cageID == fromID) {
					cage.pasteCage(cageTo, bMeasures);
					break;
				}
			}
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
		try {
			filedummy = Dialog.selectFiles(directory, "xml");
		} catch (FileDialogException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		List<ROI2D> roiList = seqCamData.getSequence().getROI2Ds();
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
			if (cage.prop.arrayColumn == column && cage.prop.arrayRow == row) {
				cage_found = cage;
				break;
			}
		}
		return cage_found;
	}

	public Cage findFirstSelectedCage() {
		Cage cageFound = null;
		for (Cage cage : cagesList) {
			ROI2D roi = cage.getRoi();
			if (roi.isSelected()) {
				cageFound = cage;
				break;
			}
		}
		return cageFound;
	}

	public Cage findFirstCageWithSelectedSpot() {
		Cage cageFound = null;
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				ROI2D roi = spot.getRoi();
				if (roi.isSelected()) {
					return cage;
				}
			}
		}
		return cageFound;
	}

	public Cage getCageFromNumber(int number) {
		Cage cageFound = null;
		for (Cage cage : cagesList) {
			if (number == cage.prop.cageID) {
				cageFound = cage;
				break;
			}
		}
		return cageFound;
	}

	public Cage getCageFromID(int cageID) {
		for (Cage cage : cagesList) {
			if (cage.prop.cageID == cageID)
				return cage;
		}
		return null;
	}

	public Cage getCageFromName(String name) {
		for (Cage cage : cagesList) {
			if (cage.getRoi().getName().equals(name))
				return cage;
		}
		return null;
	}

	public Cage getCageFromSpotName(String name) {
		int cageID = SpotString.getCageIDFromSpotName(name);
		return getCageFromID(cageID);
	}

	public Cage getCageFromSpotROIName(String name) {
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().getName().contains(name))
					return cage;
			}
		}
		return null;
	}

	// --------------

	public void transferCagesToSequenceAsROIs(SequenceCamData seqCamData) {
		// Use modern ROI operation for removing existing cage ROIs
		seqCamData.processROIs(ROIOperation.removeROIs("cage"));

		List<ROI2D> cageROIList = new ArrayList<ROI2D>(cagesList.size());
		for (Cage cage : cagesList)
			cageROIList.add(cage.getRoi());
		Sequence sequence = seqCamData.getSequence();
		if (sequence != null)
			sequence.addROIs(cageROIList, true);
	}

	public void transferROIsFromSequenceToCages(SequenceCamData seqCamData) {
		// Use modern ROI finding API
		List<ROI2D> roiList = seqCamData.findROIs("cage");
		Collections.sort(roiList, new Comparators.ROI2D_Name());
		addMissingCages(roiList);
		removeOrphanCages(roiList);
		Collections.sort(cagesList, new Comparators.Cage_Name());
	}

	public void removeAllRoiDetFromSequence(SequenceCamData seqCamData) {
		ArrayList<ROI2D> seqlist = seqCamData.getSequence().getROI2Ds();
		for (ROI2D roi : seqlist) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seqCamData.getSequence().removeROI(roi);
		}
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
			Collections.sort(cage.flyPositions.flyPositionList, new Comparators.XYTaValue_Tindex());
	}

	public void initFlyPositions(int option_cagenumber) {
		int nbcages = cagesList.size();
		for (int i = 0; i < nbcages; i++) {
			Cage cage = cagesList.get(i);
			if (option_cagenumber != -1 && cage.prop.cageID != option_cagenumber)
				continue;
			if (cage.prop.cageNFlies > 0) {
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
		Polygon2D polygon = getROIPolygon2D(cagesList.get(0).getRoi());
		for (Cage cage : cagesList) {
			int col = cage.prop.arrayColumn;
			int row = cage.prop.arrayRow;
			Polygon2D n = getROIPolygon2D(cage.getRoi());
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

	private Polygon2D getROIPolygon2D(ROI2D roi) {
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
		if (cagesList.size() > 0) {
			List<ROI2D> spotROIList = new ArrayList<ROI2D>(
					cagesList.get(0).spotsArray.spotsList.size() * cagesList.size());
			for (Cage cage : cagesList) {
				for (Spot spot : cage.spotsArray.spotsList)
					spotROIList.add(spot.getRoi());
			}
			Collections.sort(spotROIList, new Comparators.ROI2D_Name());
			seqCamData.getSequence().addROIs(spotROIList, true);
		}
	}

	public void transferROIsFromSequenceToCageSpots(SequenceCamData seqCamData) {
		// Use modern ROI finding API
		List<ROI2D> listSeqRois = seqCamData.findROIs("spot");
//		int T = 0;
//		Viewer v = seqCamData.getSequence().getFirstViewer();
//		if (v != null)
//			T = v.getPositionT();
		Collections.sort(listSeqRois, new Comparators.ROI_Name());
		for (Cage cage : cagesList) {
			Iterator<Spot> iteratorSpots = cage.spotsArray.spotsList.iterator();
			while (iteratorSpots.hasNext()) {
				Spot spot = iteratorSpots.next();
				String spotRoiName = spot.getRoi().getName();
				boolean found = false;

				Iterator<ROI2D> iteratorSeqRois = listSeqRois.iterator();
				while (iteratorSeqRois.hasNext()) {
					ROI2D roi = iteratorSeqRois.next();
					String roiName = roi.getName();
					if (roiName.equals(spotRoiName)) {
						spot.setRoi((ROI2DShape) roi);
						found = true;
						iteratorSeqRois.remove();
						break;
					}
				}
				if (!found)
					iteratorSpots.remove();
			}
		}
	}

	public Spot getSpotFromROIName(String name) {
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().getName().contains(name))
					return spot;
			}
		}
		return null;
	}

	public void initCagesAndSpotsWithNFlies(int nflies) {
		for (Cage cage : cagesList) {
			cage.prop.cageNFlies = nflies;
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

	public SpotsArray getAllSpotsArray() {
		SpotsArray spotsArray = new SpotsArray();
		spotsArray.spotsList = new ArrayList<Spot>();
		for (Cage cage : cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				spotsArray.spotsList.add(spot);
			}
		}
		return spotsArray;
	}

	public Spot getSpotAtGlobalIndex(int indexT) {
		int i = 0;
		for (Cage cage : cagesList) {
			int count = cage.spotsArray.spotsList.size();
			if (i + count - 1 < indexT) {
				i += count;
				continue;
			}
			Spot spot = cage.spotsArray.spotsList.get(indexT - i);
			return spot;
		}
		return null;
	}

	public int getSpotGlobalPosition(Spot spot) {
		int i = 0;
		int cageID = spot.prop.cageID;
		for (Cage cage : cagesList) {
			int count = cage.spotsArray.spotsList.size();
			if (cageID != cage.prop.cageID) {
				i += count;
				continue;
			}
			String name = spot.getRoi().getName();
			for (int j = 0; j < cage.spotsArray.spotsList.size(); j++) {
				if (name.equals(cage.spotsArray.spotsList.get(j).getRoi().getName())) {
					return i + j;
				}
			}
		}
		return 0;
	}

	public int getTotalNumberOfSpots() {
		int nspots = 0;
		for (Cage cage : cagesList) {
			nspots += cage.spotsArray.spotsList.size();
		}
		return nspots;
	}

	public TIntervalsArray getCagesListTimeIntervals() {
		return cagesListTimeIntervals;
	}

	public void mergeSpotsLists(CagesArray arrayToMerge) {
		for (Cage cage : cagesList) {
			for (Cage cageToMerge : arrayToMerge.cagesList) {
				if (cage.prop.cagePosition != cageToMerge.prop.cagePosition)
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

	public int findCagesListFirstTInterval(long intervalT) {
		if (cagesListTimeIntervals == null) {
			cagesListTimeIntervals = new TIntervalsArray();
			addCagesListTInterval(0);
		}
		return cagesListTimeIntervals.findStartItem(intervalT);
	}

	public long getCagesListTIntervalsAt(int selectedItem) {
		if (cagesListTimeIntervals == null)
			addCagesListTInterval(0);
		return cagesListTimeIntervals.getTIntervalAt(selectedItem).start;
	}

	public int addCagesListTInterval(long start) {
		long end = -1;
		TInterval interval = new TInterval(start, end);
		int item = cagesListTimeIntervals.addIfNew(interval);

		for (Cage cage : cagesList) {
			List<ROI2DAlongT> listCageRoiAlongT = cage.getListROIAlongT();
			ROI2D roi = cage.getRoi();
			if (item > 0)
				roi = (ROI2D) listCageRoiAlongT.get(item - 1).getInputRoi().getCopy();
			try {
				listCageRoiAlongT.add(item, new ROI2DAlongT(start, roi));
			} catch (ROI2DValidationException e) {
				System.err.println("Error creating ROI2DAlongT for cage: " + e.getMessage());
				e.printStackTrace();
			}

			if (cage.spotsArray.findSpotsListFirstTInterval(start) < 0) {
				cage.spotsArray.addSpotsListTInterval(start);
			}

		}
		return item;
	}

	public void deleteCagesListTInterval(long start) {
		cagesListTimeIntervals.deleteIntervalStartingAt(start);
		for (Cage cage : cagesList) {
			cage.removeROIAlongTListItem(start);
			cage.spotsArray.deleteSpotsListTInterval(start);
		}
	}

	// --------------------------------------------------

	public boolean load_SpotsMeasures(String directory) {
		boolean flag = getSpotsArrayFromAllCages().load_SpotsMeasures(directory);
		return flag;
	}

	public boolean load_SpotsAll(String directory) {
		boolean flag = getSpotsArrayFromAllCages().load_SpotsAll(directory);
		return flag;
	}

	public boolean save_SpotsAll(String directory) {
		boolean flag = getSpotsArrayFromAllCages().save_SpotsAll(directory);
		return flag;
	}

	public boolean save_SpotsMeasures(String directory) {
		if (directory == null)
			return false;
		SpotsArray localSpotsArray = getSpotsArrayFromAllCages();
		localSpotsArray.save_SpotsMeasures(directory);
		return true;
	}

	public SpotsArray getSpotsArrayFromAllCages() {
		SpotsArray spotsArray = new SpotsArray();
		if (cagesList.size() > 0) {
			int nspots = cagesList.size() * cagesList.get(0).spotsArray.spotsList.size();
			spotsArray.spotsList.ensureCapacity(nspots);
			for (Cage cage : cagesList) {
				spotsArray.spotsList.addAll(cage.spotsArray.spotsList);
			}
		}
		return spotsArray;
	}

	public void mapSpotsToCagesColumnRow() {
		for (Cage cage : cagesList) {
			cage.mapSpotsToCageColumnRow();
		}
	}

	public void cleanUpSpotNames() {
		for (Cage cage : cagesList) {
			cage.cleanUpSpotNames();
		}
	}
}
