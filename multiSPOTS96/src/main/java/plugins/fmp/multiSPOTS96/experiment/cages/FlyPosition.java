package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class FlyPosition {
	public Rectangle2D rectPosition = new Rectangle2D.Double(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	public ROI2DArea flyRoi = null;
	public int flyIndexT = 0;
	public boolean bAlive = false;
	public boolean bSleep = false;
	public boolean bPadded = false;
	public double distance = 0.;
	public double sumDistance = 0;
	public double axis1 = 0.;
	public double axis2 = 0.;

	public FlyPosition() {
	}

	public FlyPosition(int indexT) {
		this.flyIndexT = indexT;
	}

	public FlyPosition(int indexT, Rectangle2D rectangle) {
		if (rectangle != null)
			this.rectPosition.setRect(rectangle);
		flyIndexT = indexT;
	}

	public FlyPosition(int indexT, Rectangle2D rectangle, ROI2DArea roiArea) {
		if (rectangle != null)
			this.rectPosition.setRect(rectangle);
		flyRoi = new ROI2DArea(roiArea);
		flyIndexT = indexT;
	}

	public FlyPosition(int indexT, Rectangle2D rectangle, boolean alive) {
		if (rectangle != null)
			this.rectPosition.setRect(rectangle);
		this.flyIndexT = indexT;
		this.bAlive = alive;
	}

	public void copy(FlyPosition source) {
		flyIndexT = source.flyIndexT;
		bAlive = source.bAlive;
		bSleep = source.bSleep;
		bPadded = source.bPadded;
		distance = source.distance;
		rectPosition.setRect(source.rectPosition);
		if (source.flyRoi != null && source.flyRoi.getBounds().height > 0 && source.flyRoi.getBounds().width > 0)
			flyRoi = new ROI2DArea(source.flyRoi);
		axis1 = source.axis1;
		axis2 = source.axis2;
	}

	Point2D getCenterRectangle() {
		return new Point2D.Double(rectPosition.getX() + rectPosition.getWidth() / 2,
				rectPosition.getY() + rectPosition.getHeight() / 2);
	}

	// --------------------------------------------

	public boolean loadXYTvaluesFromXML(Node node) {
		if (node == null)
			return false;

		Element node_XYTa = XMLUtil.getElement(node, "XYTa");
		if (node_XYTa != null) {
			double xR = XMLUtil.getAttributeDoubleValue(node_XYTa, "xR", Double.NaN);
			double yR = XMLUtil.getAttributeDoubleValue(node_XYTa, "yR", Double.NaN);
			double wR = XMLUtil.getAttributeDoubleValue(node_XYTa, "wR", Double.NaN);
			double hR = XMLUtil.getAttributeDoubleValue(node_XYTa, "hR", Double.NaN);
			if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
				rectPosition.setRect(xR, yR, wR, hR);
			} else {
				xR = XMLUtil.getAttributeDoubleValue(node_XYTa, "x", Double.NaN);
				yR = XMLUtil.getAttributeDoubleValue(node_XYTa, "y", Double.NaN);
				if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
					xR -= 2.;
					yR -= 2.;
					wR = 4.;
					hR = 4.;
					rectPosition.setRect(xR, yR, wR, hR);
				}
			}

			flyIndexT = XMLUtil.getAttributeIntValue(node_XYTa, "t", 0);
			bAlive = XMLUtil.getAttributeBooleanValue(node_XYTa, "a", false);
			bSleep = XMLUtil.getAttributeBooleanValue(node_XYTa, "s", false);
		}

		Element node_roi = XMLUtil.getElement(node, "roi");
		if (node_roi != null) {
			if (flyRoi == null)
				flyRoi = new ROI2DArea();
			flyRoi.loadFromXML(node_roi);
		}

		return false;
	}

	public boolean saveXYTvaluesToXML(Node node) {
		if (node == null)
			return false;

		Element node_XYTa = XMLUtil.addElement(node, "XYTa");

		if (!Double.isNaN(rectPosition.getX())) {
			XMLUtil.setAttributeDoubleValue(node_XYTa, "xR", rectPosition.getX());
			XMLUtil.setAttributeDoubleValue(node_XYTa, "yR", rectPosition.getY());
			XMLUtil.setAttributeDoubleValue(node_XYTa, "wR", rectPosition.getWidth());
			XMLUtil.setAttributeDoubleValue(node_XYTa, "hR", rectPosition.getHeight());
		}

		XMLUtil.setAttributeDoubleValue(node_XYTa, "t", flyIndexT);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "a", bAlive);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "s", bSleep);

		Element node_roi = XMLUtil.addElement(node, "roi");
		if (flyRoi != null)
			flyRoi.saveToXML(node_roi);
		return false;
	}

	// --------------------------------------------

	public boolean cvsExportXYWHData(StringBuffer sbf, String sep) {
		cvsExportXYData(sbf, sep);
		sbf.append(StringUtil.toString((double) rectPosition.getWidth()));
		sbf.append(sep);
		sbf.append(StringUtil.toString((double) rectPosition.getHeight()));
		sbf.append(sep);
		return true;
	}

	public boolean cvsExportXYData(StringBuffer sbf, String sep) {
		sbf.append(StringUtil.toString(flyIndexT));
		sbf.append(sep);
		sbf.append(StringUtil.toString((double) rectPosition.getX()));
		sbf.append(sep);
		sbf.append(StringUtil.toString((double) rectPosition.getY()));
		sbf.append(sep);
		return true;
	}

	public boolean csvImportXYWHData(String[] data, int startAt) {
		int npoints = 5;
		if (data.length < npoints + startAt - 1)
			return false;

		int offset = startAt;
		flyIndexT = Integer.valueOf(data[offset]);
		offset++;
		double xR = Double.valueOf(data[offset]);
		offset++;
		double yR = Double.valueOf(data[offset]);
		offset++;
		double wR = Double.valueOf(data[offset]);
		offset++;
		double hR = Double.valueOf(data[offset]);
		offset++;
		rectPosition.setRect(xR, yR, wR, hR);

		return true;
	}

	public boolean csvImportXYData(String[] data, int startAt) {
		int npoints = 3;
		if (data.length < npoints + startAt - 1)
			return false;

		int offset = startAt;
		flyIndexT = Integer.valueOf(data[offset]);
		offset++;
		double xR = Double.valueOf(data[offset]);
		offset++;
		double yR = Double.valueOf(data[offset]);
		offset++;

		if (!Double.isNaN(xR) && !Double.isNaN(yR)) {
			xR -= 2.;
			yR -= 2.;
			double wR = 4.;
			double hR = 4.;
			rectPosition.setRect(xR, yR, wR, hR);
		}

		return true;
	}

}
