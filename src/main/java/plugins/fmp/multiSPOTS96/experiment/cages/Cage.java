package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

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
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Cage {
	private ROI2D cageXROI2D = null;
	private ArrayList<ROI2DAlongT> listRoiAlongT = new ArrayList<ROI2DAlongT>();
	public int kymographIndex = -1;
	public BooleanMask2D cageMask2D = null;
	public FlyPositions flyPositions = new FlyPositions();

	public CageProperties prop = new CageProperties();
	public SpotsArray spotsArray = new SpotsArray();

	public boolean valid = false;
	public boolean bDetect = true;
	public boolean initialflyRemoved = false;

	private final String ID_CAGELIMITS = "CageLimits";
	private final String ID_FLYPOSITIONS = "FlyPositions";

	// --------------------------------------

	public Cage(ROI2DShape roi) {
		this.cageXROI2D = roi;
	}

	public Cage() {
	}

	// ------------------------------------

	public ROI2D getRoi() {
		return cageXROI2D;
	}

	public void setRoi(ROI2DShape roi) {
		cageXROI2D = roi;
		listRoiAlongT.clear();
	}

	public String getCageNumberFromCageRoiName() {
		if (prop.strCageNumber == null)
			prop.strCageNumber = cageXROI2D.getName().substring(cageXROI2D.getName().length() - 3);
		return prop.strCageNumber;
	}

	public int getCageNumberInteger() {
		int cagenb = -1;
		prop.strCageNumber = getCageNumberFromCageRoiName();
		if (prop.strCageNumber != null) {
			try {
				return Integer.parseInt(prop.strCageNumber);
			} catch (NumberFormatException e) {
				return cagenb;
			}
		}
		return cagenb;
	}

	public void clearMeasures() {
		flyPositions.clear();
	}

	public Point2D getCenterTopCage() {
		Rectangle2D rect = cageXROI2D.getBounds2D();
		Point2D pt = new Point2D.Double(rect.getX() + rect.getWidth() / 2, rect.getY());
		return pt;
	}

	public void copyCageInfo(Cage cageFrom, boolean bCopyMeasures) {
		prop.copy(cageFrom.prop);
		cageXROI2D = cageFrom.cageXROI2D;
		valid = false;
		if (bCopyMeasures)
			flyPositions.copyXYTaSeries(cageFrom.flyPositions);
		spotsArray.copySpotsInfos(cageFrom.spotsArray, bCopyMeasures);
	}

	public ROI2DRectangle getRoiRectangleFromPositionAtT(int t) {
		int nitems = flyPositions.flyPositionList.size();
		if (nitems == 0 || t >= nitems)
			return null;
		FlyPosition aValue = flyPositions.flyPositionList.get(t);

		ROI2DRectangle flyRoiR = new ROI2DRectangle(aValue.rectPosition);
		flyRoiR.setName("detR" + getCageNumberFromCageRoiName() + "_" + t);
		flyRoiR.setT(t);
		return flyRoiR;
	}

	public void transferRoisToPositions(List<ROI2D> detectedROIsList) {
		String filter = "detR" + getCageNumberFromCageRoiName();
		for (ROI2D roi : detectedROIsList) {
			String name = roi.getName();
			if (!name.contains(filter))
				continue;
			Rectangle2D rect = ((ROI2DRectangle) roi).getRectangle();
			int t = roi.getT();
			flyPositions.flyPositionList.get(t).rectPosition = rect;
		}
	}

	public void computeCageBooleanMask2D() throws InterruptedException {
		cageMask2D = cageXROI2D.getBooleanMask2D(0, 0, 1, true);
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
		spotsArray.xmlLoadSpotsArray(xmlVal);
		return true;
	}

	public boolean xmlSaveCage(Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage" + index);
		xmlSaveCageLimits(xmlVal);
		prop.xmlSaveCageParameters(xmlVal);
		spotsArray.xmlSaveSpotsArray(xmlVal);
		return true;
	}

	public boolean xmlLoadCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_CAGELIMITS);
		if (xmlVal2 != null) {
			cageXROI2D = (ROI2D) ROI.createFromXML(xmlVal2);
			cageXROI2D.setSelected(false);
		}
		return true;
	}

	public boolean xmlSaveCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_CAGELIMITS);
		if (cageXROI2D != null) {
			cageXROI2D.setSelected(false);
			cageXROI2D.saveToXML(xmlVal2);
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
		row.add(cageXROI2D.getName());
		row.add(Integer.toString(prop.cageNFlies));
		row.add(Integer.toString(prop.cageAge));
		row.add(prop.strCageComment);
		row.add(prop.strCageStrain);
		row.add(prop.strCageSex);

		int npoints = 0;
		if (cageXROI2D != null) {
			Polygon2D polygon = ((ROI2DPolygon) cageXROI2D).getPolygon2D();
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
		this.prop.cageNFlies = nFlies;
		for (Spot spot : spotsArray.spotsList) {
			spot.prop.spotNFlies = nFlies;
		}
	}

	public int addEllipseSpot(int spotIndex, Point2D.Double center, int radius) {

		if (spotsArray.spotsList == null)
			spotsArray.spotsList = new ArrayList<Spot>(1);
		int carreIndex = spotsArray.spotsList.size();
		Spot spot = createEllipseSpot(spotIndex, carreIndex, center, radius);
		spot.prop.cagePosition = spotsArray.spotsList.size();
		spotsArray.spotsList.add(spot);
		return spotsArray.spotsList.size();
	}

	private Spot createEllipseSpot(int spotArrayIndex, int cagePosition, Point2D.Double center, int radius) {
		Ellipse2D ellipse = new Ellipse2D.Double(center.x, center.y, 2 * radius, 2 * radius);
		ROI2DEllipse roiEllipse = new ROI2DEllipse(ellipse);
		roiEllipse.setName(SpotString.createSpotString(prop.cageID, cagePosition, spotArrayIndex));

		Spot spot = new Spot(roiEllipse);
		spot.prop.spotArrayIndex = spotArrayIndex;
		spot.prop.cageID = prop.cageID;
		spot.prop.cagePosition = cagePosition;
		spot.prop.spotRadius = radius;
		spot.prop.spotXCoord = (int) center.getX();
		spot.prop.spotYCoord = (int) center.getY();
		try {
			spot.prop.spotNPixels = (int) roiEllipse.getNumberOfPoints();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return spot;
	}

	public Spot getSpotFromRoiName(String name) {
		int cagePosition = SpotString.getSpotCagePositionFromSpotName(name);
		for (Spot spot : spotsArray.spotsList) {
			if (spot.prop.cagePosition == cagePosition)
				return spot;
		}
		return null;
	}

	// --------------------------------------------

	public List<ROI2DAlongT> getROIAlongTList() {

		if (listRoiAlongT.size() < 1)
			initROIAlongTList();
		return listRoiAlongT;
	}

	public ROI2DAlongT getROIAtT(long t) {
		if (listRoiAlongT.size() < 1)
			initROIAlongTList();

		ROI2DAlongT spotRoi = null;
		for (ROI2DAlongT item : listRoiAlongT) {
			if (t < item.getT())
				break;
			spotRoi = item;
		}
		return spotRoi;
	}

	public void removeROIAlongTListItem(long t) {
		ROI2DAlongT itemFound = null;
		for (ROI2DAlongT item : listRoiAlongT) {
			if (t != item.getT())
				continue;
			itemFound = item;
		}
		if (itemFound != null)
			listRoiAlongT.remove(itemFound);
	}

	private void initROIAlongTList() {
		listRoiAlongT.add(new ROI2DAlongT(0, cageXROI2D));
	}

}
