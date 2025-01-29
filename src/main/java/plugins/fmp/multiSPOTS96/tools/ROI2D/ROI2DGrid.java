package plugins.fmp.multiSPOTS96.tools.ROI2D;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROIEvent;
import icy.roi.ROIListener;
import icy.roi.ROIEvent.ROIEventType;
import icy.type.geom.Polygon2D;

import plugins.fmp.multiSPOTS96.tools.polyline.PolygonUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class ROI2DGrid extends ROI2DPolyLine implements ROIListener {

	Point2D.Double[][] arrayPoints;
	ArrayList<ROI2D> listRois;

	public ROI2DGrid() {
	}

	public ArrayList<ROI2D>  createGridFromFrame(Polygon2D roiPolygon, int n_columns, int n_rows) {

		arrayPoints = PolygonUtilities.createGridWithPolygon(roiPolygon, n_columns, n_rows);
		listRois = new ArrayList<ROI2D>((n_columns+1)*(n_rows+1));
				
		for (int icol = 0; icol <= n_columns; icol++) {
			List<Point2D> points = new ArrayList<Point2D>(n_columns+1);
			for (int irow = 0; irow <= n_rows; irow++) {
				points.add(arrayPoints[icol][irow]);
			}
			ROI2DPolyLine roi = new ROI2DPolyLine(points);
			roi.setName("col_"+icol);
			listRois.add(roi);
			roi.addListener(this);
		}

		for (int irow = 0; irow <= n_rows; irow++) {
			List<Point2D> points = new ArrayList<Point2D>(n_rows+1);
			for (int icol = 0; icol <= n_columns; icol++) {
				points.add(arrayPoints[icol][irow]);
			}
			ROI2DPolyLine roi = new ROI2DPolyLine(points);
			roi.setName("row_"+irow);
			listRois.add(roi);
			roi.addListener(this);
		}
		return listRois;
	}

	
	@Override
	public void roiChanged(ROIEvent event) {
		ROI roi = event.getSource();
    	System.out.println(roi.getName());
        if (event.getType() == ROIEventType.ROI_CHANGED) {
        	System.out.println(event.getType());
            return;
        }
	}

}
