package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotString;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Cage implements Comparable<Cage>, AutoCloseable {
	private static final Logger LOGGER = Logger.getLogger(ModernCage.class.getName());

	private ROI2D cageROI2D = null;
	public int kymographIndex = -1;
	public BooleanMask2D cageMask2D = null;
	public CageProperties prop = new CageProperties();

	public FlyPositions flyPositions = new FlyPositions();
	public SpotsArray spotsArray = new SpotsArray();
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public boolean valid = false;
	public boolean bDetect = true;
	public boolean initialflyRemoved = false;

	private final String ID_CAGELIMITS = "CageLimits";
	private final String ID_FLYPOSITIONS = "FlyPositions";

	// --------------------------------------

	public Cage(ROI2DShape roi) {
		this.cageROI2D = roi;
	}

	public Cage() {
	}

	@Override
	public int compareTo(Cage o) {
		if (o != null)
			return (this.cageROI2D.getName()).compareTo(o.cageROI2D.getName());
		return 1;
	}

	// ------------------------------------
	public SpotsArray getSpotsArray() {
		return spotsArray;
	}

	public CageProperties getProperties() {
		return prop;
	}

	public ROI2D getRoi() {
		return cageROI2D;
	}

	public void setRoi(ROI2DShape roi) {
		cageROI2D = roi;
	}

	public String getCageNumberFromRoiName() {
		prop.strCageNumber = cageROI2D.getName().substring(cageROI2D.getName().length() - 3);
		return prop.strCageNumber;
	}

	public void clearMeasures() {
		flyPositions.clear();
	}

	public Point2D getCenterTopCage() {
		Rectangle2D rect = cageROI2D.getBounds2D();
		Point2D pt = new Point2D.Double(rect.getX() + rect.getWidth() / 2, rect.getY());
		return pt;
	}

	public void copyCageInfo(Cage cageFrom) {
		copyCage(cageFrom, false);
	}

	public void copyCage(Cage cageFrom, boolean bMeasures) {
		prop.copy(cageFrom.prop);
		cageROI2D = (ROI2D) cageFrom.cageROI2D.getCopy();
		valid = false;
		if (bMeasures)
			flyPositions.copyXYTaSeries(cageFrom.flyPositions);
		spotsArray.copySpotsInfo(cageFrom.spotsArray);
	}

	public void pasteCageInfo(Cage cageTo) {
		prop.paste(cageTo.prop);
		cageTo.cageROI2D = (ROI2D) cageROI2D.getCopy();
		spotsArray.pasteSpotsInfo(cageTo.spotsArray);
	}

	public void pasteCage(Cage cageTo, boolean bMeasures) {
		prop.paste(cageTo.prop);
		cageTo.cageROI2D = (ROI2D) cageROI2D.getCopy();
		spotsArray.pasteSpots(cageTo.spotsArray, bMeasures);
		if (bMeasures)
			flyPositions.copyXYTaSeries(cageTo.flyPositions);
	}

	public String getField(EnumXLSColumnHeader fieldEnumCode) {
		String stringValue = null;
		switch (fieldEnumCode) {
		case CAGE_SEX:
			stringValue = prop.flySex;
			break;
		case CAGE_AGE:
			stringValue = String.valueOf(prop.flyAge);
			break;
		case CAGE_STRAIN:
			stringValue = prop.flyStrain;
			break;
		default:
			break;
		}
		return stringValue;
	}

	public void setField(EnumXLSColumnHeader fieldEnumCode, String stringValue) {
		switch (fieldEnumCode) {
		case CAGE_SEX:
			prop.flySex = stringValue;
			break;
		case CAGE_AGE:
			int ageValue = Integer.valueOf(stringValue);
			prop.flyAge = ageValue;
			break;
		case CAGE_STRAIN:
			prop.flyStrain = stringValue;
			break;
		default:
			break;
		}
	}

	public ROI2DRectangle getRoiRectangleFromPositionAtT(int t) {
		int nitems = flyPositions.flyPositionList.size();
		if (nitems == 0 || t >= nitems)
			return null;
		FlyPosition aValue = flyPositions.flyPositionList.get(t);

		ROI2DRectangle flyRoiR = new ROI2DRectangle(aValue.rectPosition);
		flyRoiR.setName("detR" + getCageNumberFromRoiName() + "_" + t);
		flyRoiR.setT(t);
		return flyRoiR;
	}

	public void transferRoisToPositions(List<ROI2D> detectedROIsList) {
		String filter = "detR" + getCageNumberFromRoiName();
		for (ROI2D roi : detectedROIsList) {
			String name = roi.getName();
			if (!name.contains(filter))
				continue;
			Rectangle2D rect = ((ROI2DRectangle) roi).getRectangle();
			int t = (int) roi.getT();
			flyPositions.flyPositionList.get(t).rectPosition = rect;
		}
	}

	public void computeCageBooleanMask2D() throws InterruptedException {
		cageMask2D = cageROI2D.getBooleanMask2D(0, 0, 1, true);
	}

	// -------------------------------------

	public boolean xmlLoadCage(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Cage" + index);
		if (xmlVal == null)
			return false;
		xmlLoadCageLimits(xmlVal);
		prop.xmlLoadCageParameters(xmlVal);
		cageROI2D.setColor(prop.color);
		spotsArray.loadFromXml(xmlVal);
		return true;
	}

	public boolean xmlSaveCage(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage" + index);
		xmlSaveCageLimits(xmlVal);
		prop.xmlSaveCageParameters(xmlVal);
		spotsArray.saveToXml(xmlVal);
		return true;
	}

	public boolean xmlLoadCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_CAGELIMITS);
		if (xmlVal2 != null) {
			cageROI2D = (ROI2D) ROI.createFromXML(xmlVal2);
			cageROI2D.setSelected(false);
		}
		return true;
	}

	public boolean xmlSaveCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_CAGELIMITS);
		if (cageROI2D != null) {
			cageROI2D.setSelected(false);
			cageROI2D.saveToXML(xmlVal2);
		}
		return true;
	}

	public boolean xmlLoadFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_FLYPOSITIONS);
		if (xmlVal2 != null) {
			flyPositions.loadXYTseriesFromXML(xmlVal2);
			return true;
		}
		return false;
	}

	public boolean xmlSaveFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_FLYPOSITIONS);
		flyPositions.saveXYTseriesToXML(xmlVal2);
		return true;
	}

	// -----------------------------------------

	public String csvExportCageDescription(String sep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row = new ArrayList<String>();
		row.add(prop.strCageNumber);
		row.add(cageROI2D.getName());
		row.add(Integer.toString(prop.cageNFlies));
		row.add(Integer.toString(prop.flyAge));
		row.add(prop.comment);
		row.add(prop.flyStrain);
		row.add(prop.flySex);

		int npoints = 0;
		if (cageROI2D != null) {
			Polygon2D polygon = ((ROI2DPolygon) cageROI2D).getPolygon2D();
			row.add(Integer.toString(polygon.npoints));
			for (int i = 0; i < npoints; i++) {
				row.add(Integer.toString((int) polygon.xpoints[i]));
				row.add(Integer.toString((int) polygon.ypoints[i]));
			}
		} else
			row.add("0");
		sbf.append(String.join(sep, row));
		sbf.append("\n");
		return sbf.toString();
	}

	// --------------------------------------------------------

	public void setNFlies(int nFlies) {
		this.getProperties().setCageNFlies(nFlies);
	}

	public int addEllipseSpot(Point2D.Double center, int radius) {
		int index = spotsArray.getSpotsCount();
		Spot spot = createEllipseSpot(index, center, radius);
		spot.getProperties().setCagePosition(spotsArray.getSpotsCount());
		spotsArray.addSpot(spot);
		return spotsArray.getSpotsCount();
	}

	private Spot createEllipseSpot(int cagePosition, Point2D.Double center, int radius) {
		Ellipse2D ellipse = new Ellipse2D.Double(center.x, center.y, 2 * radius, 2 * radius);
		ROI2DEllipse roiEllipse = new ROI2DEllipse(ellipse);
		roiEllipse.setName(SpotString.createSpotString(prop.cageID, cagePosition));
		Spot spot = new Spot(roiEllipse);
		spot.getProperties().setCageID(prop.cageID);
		spot.getProperties().setCagePosition(cagePosition);
		spot.getProperties().setSpotRadius(radius);
		spot.getProperties().setSpotXCoord((int) center.getX());
		spot.getProperties().setSpotYCoord((int) center.getY());
		try {
			spot.getProperties().setSpotNPixels((int) roiEllipse.getNumberOfPoints());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return spot;
	}

	public Spot getSpotFromRoiName(String name) {
		int cagePosition = SpotString.getSpotCagePositionFromSpotName(name);
		for (Spot spot : spotsArray.getSpotsList()) {
			if (spot.getProperties().getCagePosition() == cagePosition)
				return spot;
		}
		return null;
	}

	// --------------------------------------------

	public void mapSpotsToCageColumnRow() {
		Rectangle rect = cageROI2D.getBounds();
		int deltaX = rect.width / 8;
		int deltaY = rect.height / 4;
		for (Spot spot : spotsArray.getSpotsList()) {
			Rectangle rectSpot = spot.getRoi().getBounds();
			spot.getProperties().setCageColumn((rectSpot.x - rect.x) / deltaX);
			spot.getProperties().setCageRow((rectSpot.y - rect.y) / deltaY);
		}
	}

	public void cleanUpSpotNames() {
		for (int i = 0; i < spotsArray.getSpotsList().size(); i++) {
			Spot spot = spotsArray.getSpotsList().get(i);
			spot.setName(prop.cageID, i);
			spot.getProperties().setCageID(prop.cageID);
			spot.getProperties().setCagePosition(i);
		}
	}

	public void updateSpotsStimulus_i() {
		ArrayList<String> stimulusArray = new ArrayList<String>(8);
		for (Spot spot : spotsArray.getSpotsList()) {
			String test = spot.getProperties().getStimulus();
			stimulusArray.add(test);
			spot.getProperties().setStimulusI(test + "_" + findNumberOfIdenticalItems(test, stimulusArray));
		}
	}

	private int findNumberOfIdenticalItems(String test, ArrayList<String> array) {
		int items = 0;
		for (String element : array)
			if (element.equals(test))
				items++;
		return items;
	}

	public Spot combineSpotsWithSameStimConc(String stim, String conc) {
		Spot spotCombined = null;
		for (Spot spotSource : spotsArray.getSpotsList()) {
			if (stim.equals(spotSource.getProperties().getStimulus())
					&& conc.equals(spotSource.getProperties().getConcentration())) {
				if (spotCombined == null) {
					spotCombined = new Spot(spotSource, true);
				} else {
					spotCombined.addMeasurements(spotSource);
				}
			}
		}
		return spotCombined;
	}

	public Spot createSpotPI(Spot spot1, Spot spot2) {
		if (spot1 == null || spot2 == null)
			return null;
		Spot spotPI = new Spot();
		spotPI.getProperties().setCageID(spot1.getProperties().getCageID());
		spotPI.getProperties().setSourceName("PI");
		spotPI.getProperties().setStimulus("PI");
		spotPI.getProperties().setConcentration(
				spot1.getCombinedStimulusConcentration() + " / " + spot2.getCombinedStimulusConcentration());
		spotPI.computePI(spot1, spot2);
		return spotPI;
	}

	public Spot createSpotSUM(Spot spot1, Spot spot2) {
		if (spot1 == null || spot2 == null)
			return null;
		Spot spotSUM = new Spot();
		spotSUM.getProperties().setCageID(spot1.getProperties().getCageID());
		spotSUM.getProperties().setSourceName("SUM");
		spotSUM.getProperties().setStimulus("SUM");
		spotSUM.getProperties().setConcentration(
				spot1.getCombinedStimulusConcentration() + " / " + spot2.getCombinedStimulusConcentration());
		spotSUM.computeSUM(spot1, spot2);
		return spotSUM;
	}

	@Override
	public void close() throws Exception {
		if (closed.compareAndSet(false, true)) {
			LOGGER.fine("Closing cage: "); // + data.getName());
			// Cleanup resources if needed
			flyPositions.clear();
		}
	}
}
