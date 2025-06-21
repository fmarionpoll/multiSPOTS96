package plugins.fmp.multiSPOTS96.tools;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.FlyPosition;
import plugins.fmp.multiSPOTS96.experiment.cages.FlyPositions;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResults;

public class Comparators {
	//
	public static class ROI_Name implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class ROI2D_Name implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class Spot_XYPosition implements Comparator<Spot> {
		@Override
		public int compare(Spot spot1, Spot spot2) {
			Rectangle2D rect1 = spot1.getRoi().getBounds2D();
			Rectangle2D rect2 = spot2.getRoi().getBounds2D();
			Point2D.Double x1 = getCenterPoint(rect1);
			Point2D.Double x2 = getCenterPoint(rect2);
			int result = Double.compare(x1.getX(), x2.getX());
			if (result == 0)
				result = Double.compare(x1.getY(), x2.getY());
			return result;
		}
	}

	private static Point2D.Double getCenterPoint(Rectangle2D rectangle) {
		return new Point2D.Double(rectangle.getX() + rectangle.getWidth() / 2,
				rectangle.getY() + rectangle.getHeight() / 2);
	}

	public static class ROI2D_T implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			return o1.getT() - o2.getT();
		}
	}

	public static class Sequence_Name implements Comparator<Sequence> {
		@Override
		public int compare(Sequence o1, Sequence o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class XLSResults_Name implements Comparator<XLSResults> {
		@Override
		public int compare(XLSResults o1, XLSResults o2) {
			return o1.name.compareTo(o2.name);
		}
	}

	public static class XYTaSeries_Name implements Comparator<FlyPositions> {
		@Override
		public int compare(FlyPositions o1, FlyPositions o2) {
			return o1.name.compareTo(o2.name);
		}
	}

	public static class Cage_Name implements Comparator<Cage> {
		@Override
		public int compare(Cage o1, Cage o2) {
			return o1.getRoi().getName().compareTo(o2.getRoi().getName());
		}
	}

	public static class Spot_Name implements Comparator<Spot> {
		@Override
		public int compare(Spot o1, Spot o2) {
			return o1.getRoi().getName().compareTo(o2.getRoi().getName());
		}
	}

	public static class XYTaValue_Tindex implements Comparator<FlyPosition> {
		@Override
		public int compare(FlyPosition o1, FlyPosition o2) {
			return o1.flyIndexT - o2.flyIndexT;
		}
	}

	public static class Experiment_Start implements Comparator<Experiment> {
		@Override
		public int compare(Experiment exp1, Experiment exp2) {
			return Long.compare(exp1.seqCamData.firstImage_ms + exp1.seqCamData.binFirst_ms,
					exp2.seqCamData.firstImage_ms + exp2.seqCamData.binFirst_ms);
		}
	}


}
