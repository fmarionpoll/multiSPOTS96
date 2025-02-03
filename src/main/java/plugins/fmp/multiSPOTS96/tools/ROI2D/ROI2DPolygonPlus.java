package plugins.fmp.multiSPOTS96.tools.ROI2D;

import java.awt.geom.Point2D;
import java.util.List;

import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class ROI2DPolygonPlus extends ROI2DPolygon {

	public int cageRow = -1;
	public int cageColumn = -1;
	public int cagePosition = -1;
	public boolean isSelected = false;

	public ROI2DPolygonPlus() {
		super();
	}

	public ROI2DPolygonPlus(int cageRow, int cageColumn) {
		super();
		this.cageRow = cageRow;
		this.cageColumn = cageColumn;
	}

	public ROI2DPolygonPlus(List<Point2D> points, int cageRow, int cageColumn) {
		super(points);
		this.cageRow = cageRow;
		this.cageColumn = cageColumn;
	}
}
