package plugins.fmp.multiSPOTS96.tools.ROI2D;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;

import icy.roi.ROI;
import icy.roi.ROIEvent;
import icy.roi.ROIEvent.ROIEventType;
import icy.roi.ROIListener;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import icy.type.geom.Polyline2D;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class ROI2DGrid implements ROIListener {

	private Point2D.Double[][] grid;
	private ArrayList<ROI2DPolyLine> colRois;
	private ArrayList<ROI2DPolyLine> rowRois;
	private ArrayList<ROI2DPolygonPlus> areaRois;
	private int grid_n_rows = 0;
	private int grid_n_columns = 0;
	private boolean allowUpdate = true;

	public ROI2DGrid() {
	}

	public void createGridFromFrame(Polygon2D polygon, int n_columns, int n_rows) {
		this.grid_n_rows = n_rows + 1;
		this.grid_n_columns = n_columns + 1;
		grid = createGridWithPolygon(polygon, n_columns, n_rows);
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

	public ArrayList<ROI2DPolygonPlus> getAreaRois() {
		return areaRois;
	}
	
	public ArrayList<ROI2DPolygonPlus> getSelectedAreaRois() {
		ArrayList<ROI2DPolygonPlus> listCarres = new ArrayList<ROI2DPolygonPlus>(areaRois.size());
		for (ROI2DPolygonPlus roiP: areaRois) {
			if (roiP.isSelected)
				listCarres.add(roiP);
		}
		return listCarres;
	}
	
	public ROI2DPolygonPlus getAreaAt(int position) {
		ROI2DPolygonPlus roiP = areaRois.get(position); 
		int pos = roiP.cagePosition;
		if (pos != position) {
			roiP = null;
			for (ROI2DPolygonPlus roi: areaRois) {
				if (roi.cagePosition == position) {
					roiP = roi;
					break;
				}
			}
		}
		return roiP;
	}

	public Point2D.Double[][] getGridPoints() {
		return grid;
	}

	public void clearGridRois(Sequence seq) {
		if (rowRois != null && rowRois.size() > 0)
			seq.removeROIs(rowRois, false);
		if (colRois != null && colRois.size() > 0)
			seq.removeROIs(colRois, false);
		if (areaRois != null && areaRois.size() > 0)
			seq.removeROIs(areaRois, false);
	}

	public void gridToRois(String cageRoot, Color color, int width_interval,
			int height_interval) {

		areaRois = new ArrayList<ROI2DPolygonPlus>(grid_n_columns * grid_n_rows);
		int index = 0;
		for (int row = 0; row < (grid_n_rows - 1); row++) {
			for (int column = 0; column < (grid_n_columns - 1); column++) {
				ROI2DPolygonPlus roiP = createRoiPolygon(column, row, width_interval, height_interval);
				roiP.setName(cageRoot + String.format("%03d", index));
				roiP.setColor(color);
				roiP.cageRow = row;
				roiP.cageColumn = column;
				roiP.cagePosition = index;
				areaRois.add(roiP);
				index++;
			}
		}
	}

	private ROI2DPolygonPlus createRoiPolygon(int icol, int irow, int width, int height) {
		List<Point2D> points = new ArrayList<>();
		Point2D.Double pt = (Double) grid[icol][irow].clone();
		pt.x += width;
		pt.y += height;
		points.add(pt);

		pt = (Double) grid[icol][irow + 1].clone();
		pt.x += width;
		pt.y -= height;
		points.add(pt);

		pt = (Double) grid[icol + 1][irow + 1].clone();
		pt.x -= width;
		pt.y -= height;
		points.add(pt);

		pt = (Double) grid[icol + 1][irow].clone();
		pt.x -= width;
		pt.y += height;
		points.add(pt);

		ROI2DPolygonPlus roiP = new ROI2DPolygonPlus(points, irow, icol);
		return roiP;
	}

	private Point2D.Double[][] createGridWithPolygon(Polygon2D polygon, int nbcols, int nbrows) {

		if (polygon.npoints != 4)
			throw new IllegalArgumentException("Polygon must be 4-sided");
		if (nbcols <= 0 || nbrows <= 0)
			throw new IllegalArgumentException("There must be a positive number of parts per side");

		Point2D.Double[][] arrayPoints = new Point2D.Double[nbcols + 1][nbrows + 1];

		for (int col = 0; col <= nbcols; col++) {

			double ratioX0 = col / (double) nbcols;

			double x = polygon.xpoints[0] + (polygon.xpoints[3] - polygon.xpoints[0]) * ratioX0;
			double y = polygon.ypoints[0] + (polygon.ypoints[3] - polygon.ypoints[0]) * ratioX0;
			Point2D.Double ipoint0 = new Point2D.Double(x, y);

			x = polygon.xpoints[1] + (polygon.xpoints[2] - polygon.xpoints[1]) * ratioX0;
			y = polygon.ypoints[1] + (polygon.ypoints[2] - polygon.ypoints[1]) * ratioX0;
			Point2D.Double ipoint1 = new Point2D.Double(x, y);

			for (int row = 0; row <= nbrows; row++) {

				double ratioY0 = row / (double) nbrows;
				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY0;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY0;

				Point2D.Double point = new Point2D.Double(x, y);
				arrayPoints[col][row] = point;
			}
		}
		return arrayPoints;
	}

	private ROI2DPolyLine getVerticalROI(int icol) {
		List<Point2D> points = new ArrayList<Point2D>(grid_n_columns);
		for (int irow = 0; irow < grid_n_rows; irow++) {
			points.add(grid[icol][irow]);
		}
		return new ROI2DPolyLine(points);
	}

	private ROI2DPolyLine getHorizontalROI(int irow) {
		List<Point2D> points = new ArrayList<Point2D>(grid_n_columns);
		for (int icol = 0; icol < grid_n_columns; icol++) {
			points.add(grid[icol][irow]);
		}
		return new ROI2DPolyLine(points);
	}

	private Polyline2D getVerticalLine(int icol) {
		double[] xpoints = new double[grid_n_rows];
		double[] ypoints = new double[grid_n_rows];
		for (int irow = 0; irow < grid_n_rows; irow++) {
			xpoints[irow] = grid[icol][irow].x;
			ypoints[irow] = grid[icol][irow].y;
		}
		return new Polyline2D(xpoints, ypoints, grid_n_rows);
	}

	private Polyline2D getHorizontalLine(int irow) {
		double[] xpoints = new double[grid_n_rows];
		double[] ypoints = new double[grid_n_rows];
		for (int icol = 0; icol < grid_n_columns; icol++) {
			xpoints[icol] = grid[icol][irow].x;
			ypoints[icol] = grid[icol][irow].y;
		}
		return new Polyline2D(xpoints, ypoints, grid_n_columns);
	}

	private void updateGridFromVerticalROI(int icol, ROI2DPolyLine roi) {
		Polyline2D line = roi.getPolyline2D();
		for (int irow = 0; irow < grid_n_rows; irow++) {
			grid[icol][irow].x = line.xpoints[irow];
			grid[icol][irow].y = line.ypoints[irow];
		}
	}

	private void updateGridFromHorizontalROI(int irow, ROI2DPolyLine roi) {
		Polyline2D line = roi.getPolyline2D();
		for (int icol = 0; icol < grid_n_columns; icol++) {
			grid[icol][irow].x = line.xpoints[icol];
			grid[icol][irow].y = line.ypoints[icol];
		}
	}

	private void updateHorizontalROIFromGridValues(int irow) {
		ROI2DPolyLine roi = rowRois.get(irow);
		roi.setPolyline2D(getHorizontalLine(irow));
	}

	private void updateVerticalROIFromGridValues(int icol) {
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
				index = Integer.parseInt(name.substring(name.lastIndexOf("_") + 1));
			} catch (NumberFormatException e) {
				index = 0;
			}

			// System.out.println(roi.getName() + " : " +index+ " ___" + event.getType() + "
			// __ " + event.getPropertyName());
			allowUpdate = false;
			if (name.contains("row")) {
				updateGridFromHorizontalROI(index, (ROI2DPolyLine) roi);
				for (int i = 0; i < grid_n_columns; i++) {
					updateVerticalROIFromGridValues(i);
				}
			} else if (name.contains("col")) {
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
