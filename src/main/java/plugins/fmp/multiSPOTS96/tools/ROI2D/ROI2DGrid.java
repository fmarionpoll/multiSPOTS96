package plugins.fmp.multiSPOTS96.tools.ROI2D;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.roi.ROI;
import icy.roi.ROIEvent;
import icy.roi.ROIEvent.ROIEventType;
import icy.roi.ROIListener;
import icy.type.geom.Polygon2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multiSPOTS96.tools.polyline.PolygonUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class ROI2DGrid extends ROI2DPolyLine implements ROIListener {

	Point2D.Double[][] grid;
	ArrayList<ROI2DPolyLine> listRois;
	int grid_n_rows = 0;
	int grid_n_columns = 0;
	boolean allowUpdate = true;

	public ROI2DGrid() {
	}

	public ArrayList<ROI2DPolyLine> createGridFromFrame(Polygon2D roiPolygon, int n_columns, int n_rows) {
		this.grid_n_rows = n_rows + 1;
		this.grid_n_columns = n_columns + 1;
		grid = PolygonUtilities.createGridWithPolygon(roiPolygon, n_columns, n_rows);
		listRois = new ArrayList<ROI2DPolyLine>((grid_n_columns) * (grid_n_rows));

		for (int icol = 0; icol < grid_n_columns; icol++) {
			ROI2DPolyLine roi = getVerticalROI(icol);
			roi.setName("col_" + icol);
			listRois.add(roi);
			roi.addListener(this);
		}

		for (int irow = 0; irow < grid_n_rows; irow++) {
			ROI2DPolyLine roi = getHorizontalROI(irow);
			roi.setName("row_" + irow);
			listRois.add(roi);
			roi.addListener(this);
		}
		return listRois;
	}

	ROI2DPolyLine getVerticalROI(int icol) {
		List<Point2D> points = new ArrayList<Point2D>(grid_n_columns);
		for (int irow = 0; irow < grid_n_rows; irow++) {
			points.add(grid[icol][irow]);
		}
		return new ROI2DPolyLine(points);
	}

	ROI2DPolyLine getHorizontalROI(int irow) {
		List<Point2D> points = new ArrayList<Point2D>(grid_n_columns);
		for (int icol = 0; icol < grid_n_columns; icol++) {
			points.add(grid[icol][irow]);
		}
		return new ROI2DPolyLine(points);
	}

	Polyline2D getVerticalLine(int icol) {
		double[] xpoints = new double[grid_n_rows];
		double[] ypoints = new double[grid_n_rows];
		for (int irow = 0; irow < grid_n_rows; irow++) {
			xpoints[irow] = grid[icol][irow].x;
			ypoints[irow] = grid[icol][irow].y;
		}
		return new Polyline2D(xpoints, ypoints, grid_n_rows);
	}

	Polyline2D getHorizontalLine(int irow) {
		double[] xpoints = new double[grid_n_rows];
		double[] ypoints = new double[grid_n_rows];
		for (int icol = 0; icol < grid_n_columns; icol++) {
			xpoints[irow] = grid[icol][irow].x;
			ypoints[irow] = grid[icol][irow].y;
		}
		return new Polyline2D(xpoints, ypoints, grid_n_columns);
	}

	void updateGridFromVerticalROI(int icol, ROI2DPolyLine roi) {
		Polyline2D line = roi.getPolyline2D();
		for (int irow = 0; irow < grid_n_rows; irow++) {
			grid[icol][irow].x = line.xpoints[irow];
			grid[icol][irow].y = line.ypoints[irow];
		}
	}

	void updateGridFromHorizontallROI(int irow, ROI2DPolyLine roi) {
		Polyline2D line = roi.getPolyline2D();
		for (int icol = 0; icol < grid_n_columns; icol++) {
			grid[icol][irow].x = line.xpoints[icol];
			grid[icol][irow].y = line.ypoints[icol];
		}
	}

	void updateHorizontalROIFromGridValues(int irow, ROI2DPolyLine roi) {
		roi.setPolyline2D(getHorizontalLine(irow));
	}

	void updateVerticalROIFromGridValues(int icol, ROI2DPolyLine roi) {
		roi.setPolyline2D(getVerticalLine(icol));
	}

	@Override
	public void roiChanged(ROIEvent event) {
		if (!allowUpdate)
			return;

		if (event.getType() == ROIEventType.ROI_CHANGED) {
			ROI roi = event.getSource();
			System.out.println(roi.getName() + " : " + event.getType() + " __ " + event.getPropertyName());
			return;
		}
	}

}
