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
	ArrayList<ROI2DPolyLine> colRois;
	ArrayList<ROI2DPolyLine> rowRois;
	int grid_n_rows = 0;
	int grid_n_columns = 0;
	boolean allowUpdate = true;

	public ROI2DGrid() {
	}

	public void createGridFromFrame(Polygon2D roiPolygon, int n_columns, int n_rows) {
		this.grid_n_rows = n_rows + 1;
		this.grid_n_columns = n_columns + 1;
		grid = PolygonUtilities.createGridWithPolygon(roiPolygon, n_columns, n_rows);
		colRois = new ArrayList<ROI2DPolyLine>(grid_n_columns);
		rowRois = new ArrayList<ROI2DPolyLine>(grid_n_rows);

		for (int icol = 0; icol < grid_n_columns; icol++) {
			ROI2DPolyLine roi = getVerticalROI(icol);
			roi.setName("col_" + icol);
			colRois.add(roi);
			roi.addListener(this);
		}

		for (int irow = 0; irow < grid_n_rows; irow++) {
			ROI2DPolyLine roi = getHorizontalROI(irow);
			roi.setName("row_" + irow);
			rowRois.add(roi);
			roi.addListener(this);
		}
	}
	
	public ArrayList<ROI2DPolyLine> getHorizontalRois() {
		return rowRois;
	}
	
	public ArrayList<ROI2DPolyLine> getVerticalRois() {
		return colRois;
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
			xpoints[icol] = grid[icol][irow].x;
			ypoints[icol] = grid[icol][irow].y;
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

	void updateGridFromHorizontalROI(int irow, ROI2DPolyLine roi) {
		Polyline2D line = roi.getPolyline2D();
		for (int icol = 0; icol < grid_n_columns; icol++) {
			grid[icol][irow].x = line.xpoints[icol];
			grid[icol][irow].y = line.ypoints[icol];
		}
	}

	void updateHorizontalROIFromGridValues(int irow) {
		ROI2DPolyLine roi = rowRois.get(irow);
		roi.setPolyline2D(getHorizontalLine(irow));
	}

	void updateVerticalROIFromGridValues(int icol) {
		ROI2DPolyLine roi = colRois.get(icol);
		roi.setPolyline2D(getVerticalLine(icol));
	}

	@Override
	public void roiChanged(ROIEvent event) {
		if (!allowUpdate)
			return;

		if (event.getType() == ROIEventType.ROI_CHANGED) {
			ROI roi = event.getSource();
			String name = roi.getName();
			int index = 0;
			try {
				   index = Integer.parseInt(name.substring(name.lastIndexOf("_")+1));
				}
				catch (NumberFormatException e) {
				   index = 0;
				}
			
			//System.out.println(roi.getName() + " : " +index+ " ___" + event.getType() + " __ " + event.getPropertyName());
			allowUpdate = false;
			if (name.contains("row")) {
				updateGridFromHorizontalROI(index, (ROI2DPolyLine) roi);
				for (int i = 0; i < grid_n_columns; i++) {
					updateVerticalROIFromGridValues(i);
				}
			}
			else if (name.contains("col")){
				updateGridFromVerticalROI(index, (ROI2DPolyLine) roi);
				for (int i = 0; i < grid_n_rows; i++) {
					updateHorizontalROIFromGridValues(i);
				}
			}
			allowUpdate = true;
			return;
		}
	}

}
