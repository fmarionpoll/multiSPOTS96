package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DUtilities;
import plugins.fmp.multiSPOTS96.tools.polyline.Level2D;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExport;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Spot implements Comparable<Spot> {

	private ROI2DShape spotROI2D = null;
	private ArrayList<ROI2DAlongT> listSpotRoiAlongT = new ArrayList<ROI2DAlongT>();
	public int kymographIndex = -1;
	public int spotCamData_T = -1;
	public int spotKymograph_T = -1;
	public String spotFilenameTIFF = null;
	public IcyBufferedImage spotImage = null;
	public BooleanMask2D mask2DSpot = null;
	public SpotProperties prop = new SpotProperties();

	public SpotMeasure sum_in = new SpotMeasure("sum");
	public SpotMeasure sum_clean = new SpotMeasure("clean");
	public SpotMeasure flyPresent = new SpotMeasure("flyPresent");

	public boolean valid = true;
	public boolean okToAnalyze = true;

	private final String ID_META = "metaMC";

	private final String ID_INTERVALS = "INTERVALS";
	private final String ID_NINTERVALS = "nintervals";
	private final String ID_INTERVAL = "interval_";

	// ----------------------------------------------------

	public Spot(ROI2DShape roi) {
		this.spotROI2D = roi;
	}

	public Spot() {
	}

	public Spot(Spot spotFrom, boolean bMeasures) {
		copySpot(spotFrom, bMeasures);
	}

	@Override
	public int compareTo(Spot o) {
		if (o != null)
			return (this.spotROI2D.getName()).compareTo(o.spotROI2D.getName());
		return 1;
	}

	// ------------------------------------------

	public void copySpot(Spot spotFrom, boolean bMeasures) {
		prop.copy(spotFrom.prop);
		spotROI2D = (ROI2DShape) spotFrom.spotROI2D.getCopy();
		if (bMeasures) {
			sum_in.copyMeasures(spotFrom.sum_in);
			sum_clean.copyMeasures(spotFrom.sum_clean);
			flyPresent.copyMeasures(spotFrom.flyPresent);
		}
	}

	public void addMeasures(Spot spotFrom) {
		sum_in.addMeasures(spotFrom.sum_in);
		sum_clean.addMeasures(spotFrom.sum_clean);
		flyPresent.addMeasures(spotFrom.flyPresent);
	}

	public void computePI(Spot spot1, Spot spot2) {
		sum_in.computePI(spot1.sum_in, spot2.sum_in);
		sum_clean.computePI(spot1.sum_clean, spot2.sum_clean);
		flyPresent.combineIsPresent(spot1.flyPresent, spot2.flyPresent);
	}

	public void computeSUM(Spot spot1, Spot spot2) {
		sum_in.computeSUM(spot1.sum_in, spot2.sum_in);
		sum_clean.computeSUM(spot1.sum_clean, spot2.sum_clean);
		flyPresent.combineIsPresent(spot1.flyPresent, spot2.flyPresent);
	}

	public ROI2D getRoi() {
		return spotROI2D;
	}

	public void setRoi(ROI2DShape roi) {
		this.spotROI2D = roi;
		listSpotRoiAlongT.clear();
	}

	public void setName(int cageID, int spotID) {
		String name = "spot_" + String.format("%03d", cageID) + "_" + String.format("%03d", spotID);
		if (spotROI2D != null)
			spotROI2D.setName(name);
		prop.sourceName = name;
	}

	public String getName() {
		if (prop.sourceName == null) {
			prop.sourceName = getRoi().getName();
		}
		return prop.sourceName;
	}

	public String getCombinedSimulusConcentrationFields() {
		return new String(prop.stimulus + "_" + prop.concentration);
	}

	public String getCagePosition(EnumXLSExport xlsExportOption) {
		String value = null;
		switch (xlsExportOption) {
		case DISTANCE:
		case ISALIVE:
			value = String.valueOf(prop.cagePosition) + "(T=B)";
			break;

		case XYIMAGE:
		case XYTOPCAGE:
		case XYTIPCAPS:
			if (prop.cagePosition == 0)
				value = "x";
			else
				value = "y";
			break;
		default:
			value = String.valueOf(prop.cagePosition);
			break;
		}
		return value;
	}

	public String getField(EnumXLSColumnHeader fieldEnumCode) {
		String stringValue = null;
		switch (fieldEnumCode) {
		case SPOT_STIM:
			stringValue = prop.stimulus;
			break;
		case SPOT_CONC:
			stringValue = prop.concentration;
			break;
		case SPOT_VOLUME:
			stringValue = String.valueOf(prop.spotVolume);
			break;
		default:
			break;
		}
		return stringValue;
	}

	public void setField(EnumXLSColumnHeader fieldEnumCode, String stringValue) {
		switch (fieldEnumCode) {
		case SPOT_STIM:
			prop.stimulus = stringValue;
			break;
		case SPOT_CONC:
			prop.concentration = stringValue;
			break;
		case SPOT_VOLUME:
			double value = Double.valueOf(stringValue);
			prop.spotVolume = value;
			break;
		default:
			break;
		}
	}

	public Point2D getSpotCenter() {
		Point pt = spotROI2D.getPosition();
		Rectangle rect = spotROI2D.getBounds();
		pt.translate(rect.height / 2, rect.width / 2);
		return pt;
	}

	private SpotMeasure getSpotArea(EnumXLSExport option) {
		switch (option) {
		case AREA_SUM:
			return sum_in;
		case AREA_SUMCLEAN:
			return sum_clean;
		case AREA_FLYPRESENT:
			return flyPresent;
		default:
			return null;
		}
	}

	public boolean isIndexSelected(List<Integer> selectedIndexes) {
		if (selectedIndexes == null || selectedIndexes.size() < 1)
			return true;

		for (int i : selectedIndexes) {
			if (i == prop.spotArrayIndex) {
				return true;
			}
		}
		return false;
	}

	// -----------------------------------------

	public boolean isThereAnyMeasuresDone(EnumXLSExport option) {
		SpotMeasure spotArea = getSpotArea(option);
		if (spotArea != null)
			return spotArea.isThereAnyMeasuresDone();
		return false;
	}

	public ArrayList<Double> getSpotMeasuresForXLSPass1(EnumXLSExport option, long seriesBinMs, long outputBinMs) {
		SpotMeasure spotArea = getSpotArea(option);
		if (spotArea != null)
			return spotArea.getLevel2D_Y_subsampled(seriesBinMs, outputBinMs);
		return null;
	}

	public void restoreClippedSpotMeasures() {
		restoreClippedMeasures(sum_in);
		restoreClippedMeasures(sum_clean);
		restoreClippedMeasures(flyPresent);
	}

	private void restoreClippedMeasures(SpotMeasure spotMeasure) {
		if (spotMeasure.getLevel2DNPoints() > 0)
			spotMeasure.restoreCroppedLevel2D();
	}

	public void transferROIsMeasuresToLevel2D() {
		if (sum_in != null)
			sum_in.transferROItoLevel2D();
		if (sum_clean != null)
			sum_clean.transferROItoLevel2D();
		if (flyPresent != null)
			flyPresent.transferROItoLevel2D();
	}

	// -----------------------------------------------------------------------------

	public boolean loadFromXML_SpotOnly(Node node) {
		final Node nodeMeta = XMLUtil.getElement(node, ID_META);
		boolean flag = (nodeMeta != null);
		if (flag) {
			prop.loadFromXML(node);
			spotROI2D = (ROI2DShape) ROI2DUtilities.loadFromXML_ROI(nodeMeta);
			if (spotROI2D != null)
				spotROI2D.setColor(prop.color);
			loadFromXML_SpotAlongT(node);
		}
		return flag;
	}

	private boolean loadFromXML_SpotAlongT(Node node) {
		listSpotRoiAlongT.clear();
		final Node nodeMeta2 = XMLUtil.getElement(node, ID_INTERVALS);
		if (nodeMeta2 == null)
			return false;
		int nitems = XMLUtil.getElementIntValue(nodeMeta2, ID_NINTERVALS, 0);
		if (nitems > 0) {
			for (int i = 0; i < nitems; i++) {
				Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL + i);
				ROI2DAlongT roiInterval = new ROI2DAlongT();
				roiInterval.loadFromXML(node_i);
				listSpotRoiAlongT.add(roiInterval);

				if (i == 0) {
					spotROI2D = (ROI2DShape) listSpotRoiAlongT.get(0).getRoi_in();
				}
			}
		}
		return true;
	}

	public boolean saveToXML_SpotOnly(Node node) {
		if (!valid)
			return false;

		final Node nodeMeta = XMLUtil.setElement(node, ID_META);
		if (nodeMeta == null)
			return false;

		prop.saveToXML(node);
		ROI2DUtilities.saveToXML_ROI(nodeMeta, spotROI2D);

		boolean flag = saveToXML_SpotAlongT(node);
		return flag;
	}

	private boolean saveToXML_SpotAlongT(Node node) {
		final Node nodeMeta2 = XMLUtil.setElement(node, ID_INTERVALS);
		if (nodeMeta2 == null)
			return false;
		int nitems = listSpotRoiAlongT.size();
		XMLUtil.setElementIntValue(nodeMeta2, ID_NINTERVALS, nitems);
		if (nitems > 0) {
			for (int i = 0; i < nitems; i++) {
				Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL + i);
				listSpotRoiAlongT.get(i).saveToXML(node_i);
			}
		}
		return true;
	}

	// --------------------------------------------

	public List<ROI2DAlongT> getROIAlongTList() {
		if (listSpotRoiAlongT.size() < 1)
			initROIAlongTList();
		return listSpotRoiAlongT;
	}

	public ROI2DAlongT getROIAtT(long t) {
		if (listSpotRoiAlongT.size() < 1)
			initROIAlongTList();

		ROI2DAlongT spotRoi = null;
		for (ROI2DAlongT item : listSpotRoiAlongT) {
			if (t < item.getT())
				break;
			spotRoi = item;
		}
		return spotRoi;
	}

	public void removeROIAlongTListItem(long t) {
		ROI2DAlongT itemFound = null;
		for (ROI2DAlongT item : listSpotRoiAlongT) {
			if (t != item.getT())
				continue;
			itemFound = item;
		}
		if (itemFound != null)
			listSpotRoiAlongT.remove(itemFound);
	}

	private void initROIAlongTList() {
		listSpotRoiAlongT.add(new ROI2DAlongT(0, spotROI2D));
	}

	// --------------------------------------------

	public void adjustLevel2DMeasuresToImageWidth(int imageWidth) {
		sum_in.adjustLevel2DToImageWidth(imageWidth);
		sum_clean.adjustLevel2DToImageWidth(imageWidth);
		flyPresent.adjustLevel2DToImageWidth(imageWidth);
	}

	public void cropLevel2DMeasuresToImageWidth(int imageWidth) {
		sum_in.cropLevel2DToNPoints(imageWidth);
		sum_clean.cropLevel2DToNPoints(imageWidth);
		flyPresent.cropLevel2DToNPoints(imageWidth);
	}

	public void initLevel2DMeasures() {
		sum_in.initLevel2D_fromMeasureValues(getRoi().getName());
		sum_clean.initLevel2D_fromMeasureValues(getRoi().getName());
		flyPresent.initLevel2D_fromBooleans(getRoi().getName());
	}

	public void buildRunningMedianFromSumLevel2D(int imageHeight) {
		int span = 10;
		if (sum_in.values != null)
			sum_clean.buildRunningMedian(span, sum_in.values);
		else
			sum_clean.buildRunningMedian(span, sum_in.getLevel2D().ypoints);
		sum_clean.initLevel2D_fromMeasureValues(sum_clean.getName());
	}

	public List<ROI2D> transferSpotMeasuresToROIs(int imageHeight) {
		List<ROI2D> measuresRoisList = new ArrayList<ROI2D>();
		if (sum_in.getLevel2DNPoints() != 0)
			measuresRoisList.add(sum_in.getROIForImage(spotROI2D.getName(), spotKymograph_T, imageHeight));
		if (sum_clean.getLevel2DNPoints() != 0)
			measuresRoisList.add(sum_clean.getROIForImage(spotROI2D.getName(), spotKymograph_T, imageHeight));
		if (flyPresent.getLevel2DNPoints() != 0)
			measuresRoisList.add(flyPresent.getROIForImage(spotROI2D.getName(), spotKymograph_T, 10));
		return measuresRoisList;
	}

	public void transferROItoMeasures(ROI2D roi, int imageHeight) {
		String name = roi.getName();
		if (name.contains(sum_in.getName())) {
			transferROItoMeasureValue(roi, imageHeight, sum_in);
		} else if (name.contains(sum_clean.getName())) {
			transferROItoMeasureValue(roi, imageHeight, sum_clean);
		} else if (name.contains(flyPresent.getName())) {
			transferROItoMeasureBoolean(roi, flyPresent);
		}
	}

	private void transferROItoMeasureValue(ROI2D roi, int imageHeight, SpotMeasure spotMeasure) {
		if (roi instanceof ROI2DPolyLine) {
			Level2D level2D = new Level2D(((ROI2DPolyLine) roi).getPolyline2D());
			level2D.multiply_Y(imageHeight);
			spotMeasure.setLevel2D(level2D);
		}
	}

	private void transferROItoMeasureBoolean(ROI2D roi, SpotMeasure spotMeasure) {
		if (roi instanceof ROI2DPolyLine) {
			Level2D level2D = new Level2D(((ROI2DPolyLine) roi).getPolyline2D());
			level2D.threshold_Y(1.);
			spotMeasure.setLevel2D(level2D);
		}
	}

	// -----------------------------------------------------------------------------

	public String csvExportMeasures_SectionHeader(EnumSpotMeasures measureType, String csvSep) {
		StringBuffer sbf = new StringBuffer();
		List<String> listExplanation1 = Arrays.asList("\n name", "index", "npts", "yi", "\n");
		String explanation1 = String.join(csvSep, listExplanation1);
		switch (measureType) {
		case AREA_SUM:
		case AREA_SUMCLEAN:
		case AREA_FLYPRESENT:
		case AREA_OUT:
		case AREA_DIFF:
			sbf.append("#" + csvSep + measureType.toString() + csvSep + explanation1);
			break;
		default:
			sbf.append("#" + csvSep + "UNDEFINED" + csvSep + "------------\n");
			break;
		}
		return sbf.toString();
	}

	public String csvExportMeasures_OneType(EnumSpotMeasures measureType, String csvSep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append(spotROI2D.getName() + csvSep + prop.spotArrayIndex + csvSep);
		switch (measureType) {
		case AREA_SUM:
			sum_in.cvsExportYDataToRow(sbf, csvSep);
			break;
		case AREA_SUMCLEAN:
			sum_clean.cvsExportYDataToRow(sbf, csvSep);
			break;
		case AREA_FLYPRESENT:
			flyPresent.cvsExportYDataToRow(sbf, csvSep);
			break;
		default:
			break;
		}
		sbf.append("\n");
		return sbf.toString();
	}

	public void csvImportMeasures_OneType(EnumSpotMeasures measureType, String[] data, boolean x, boolean y) {
		if (x && y) {
			switch (measureType) {
			case AREA_SUM:
				sum_in.csvImportXYDataFromRow(data, 2);
				break;
			case AREA_SUMCLEAN:
				sum_clean.csvImportXYDataFromRow(data, 2);
				break;
			case AREA_FLYPRESENT:
				flyPresent.csvImportXYDataFromRow(data, 2);
				break;
			default:
				break;
			}
		} else if (!x && y) {
			switch (measureType) {
			case AREA_SUM:
				sum_in.csvImportYDataFromRow(data, 2);
				break;
			case AREA_SUMCLEAN:
				sum_clean.csvImportYDataFromRow(data, 2);
				break;
			case AREA_FLYPRESENT:
				flyPresent.csvImportYDataFromRow(data, 2);
				break;
			default:
				break;
			}
		}
	}

}
