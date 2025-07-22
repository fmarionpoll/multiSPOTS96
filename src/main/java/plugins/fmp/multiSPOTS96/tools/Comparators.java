package plugins.fmp.multiSPOTS96.tools;

import java.util.Comparator;
import java.util.logging.Logger;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.FlyPosition;
import plugins.fmp.multiSPOTS96.experiment.cages.FlyPositions;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResults;

/**
 * Collection of comparator classes for MultiSPOTS96 data structures. This class
 * provides various comparator implementations for sorting different types of
 * objects used throughout the MultiSPOTS96 plugin.
 * 
 * <p>
 * The comparators handle null values gracefully and provide consistent sorting
 * behavior for collections of ROIs, spots, cages, and other experiment-related
 * objects.
 * </p>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * List&lt;Spot&gt; spots = getSpots();
 * Collections.sort(spots, new Comparators.Spot_Name());
 * 
 * List&lt;Cage&gt; cages = getCages();
 * Collections.sort(cages, new Comparators.Cage_Name());
 * </pre>
 * 
 * @author MultiSPOTS96
 */
public class Comparators {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(Comparators.class.getName());

	/** Default cage grid width for position calculations */
	private static final int CAGE_GRID_WIDTH = 8;

	/**
	 * Comparator for ROI objects based on their names. Handles null values by
	 * treating them as greater than non-null values.
	 */
	public static class ROI_Name implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			String name1 = o1.getName();
			String name2 = o2.getName();

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for ROI2D objects based on their names. Handles null values by
	 * treating them as greater than non-null values.
	 */
	public static class ROI2D_Name implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			String name1 = o1.getName();
			String name2 = o2.getName();

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for Spot objects based on their cage position. Calculates position
	 * as (cageRow * 8 + cageColumn) for consistent ordering.
	 */
	public static class Spot_cagePosition implements Comparator<Spot> {
		@Override
		public int compare(Spot spot1, Spot spot2) {
			if (spot1 == null && spot2 == null) {
				return 0;
			}
			if (spot1 == null) {
				return 1;
			}
			if (spot2 == null) {
				return -1;
			}

			int y1 = spot1.getProperties().getCageRow() * CAGE_GRID_WIDTH + spot1.getProperties().getCageColumn();
			int y2 = spot2.getProperties().getCageRow() * CAGE_GRID_WIDTH + spot2.getProperties().getCageColumn();
			return Integer.compare(y1, y2);
		}
	}

	/**
	 * Comparator for ROI2D objects based on their time position (T coordinate).
	 * Handles null values by treating them as greater than non-null values.
	 */
	public static class ROI2D_T implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			return o1.getT() - o2.getT();
		}
	}

	/**
	 * Comparator for Sequence objects based on their names. Handles null values by
	 * treating them as greater than non-null values.
	 */
	public static class Sequence_Name implements Comparator<Sequence> {
		@Override
		public int compare(Sequence o1, Sequence o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			String name1 = o1.getName();
			String name2 = o2.getName();

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for XLSResults objects based on their names. Handles null values
	 * by treating them as greater than non-null values.
	 */
	public static class XLSResults_Name implements Comparator<XLSResults> {
		@Override
		public int compare(XLSResults o1, XLSResults o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			String name1 = o1.getName();
			String name2 = o2.getName();

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for FlyPositions objects based on their names. Handles null values
	 * by treating them as greater than non-null values.
	 */
	public static class XYTaSeries_Name implements Comparator<FlyPositions> {
		@Override
		public int compare(FlyPositions o1, FlyPositions o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			String name1 = o1.name;
			String name2 = o2.name;

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for Cage objects based on their ROI names. Handles null values by
	 * treating them as greater than non-null values.
	 */
	public static class Cage_Name implements Comparator<Cage> {
		@Override
		public int compare(Cage o1, Cage o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			ROI2D roi1 = o1.getRoi();
			ROI2D roi2 = o2.getRoi();

			if (roi1 == null && roi2 == null) {
				return 0;
			}
			if (roi1 == null) {
				return 1;
			}
			if (roi2 == null) {
				return -1;
			}

			String name1 = roi1.getName();
			String name2 = roi2.getName();

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for Spot objects based on their ROI names. Handles null values by
	 * treating them as greater than non-null values.
	 */
	public static class Spot_Name implements Comparator<Spot> {
		@Override
		public int compare(Spot o1, Spot o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			ROI2D roi1 = o1.getRoi();
			ROI2D roi2 = o2.getRoi();

			if (roi1 == null && roi2 == null) {
				return 0;
			}
			if (roi1 == null) {
				return 1;
			}
			if (roi2 == null) {
				return -1;
			}

			String name1 = roi1.getName();
			String name2 = roi2.getName();

			if (name1 == null && name2 == null) {
				return 0;
			}
			if (name1 == null) {
				return 1;
			}
			if (name2 == null) {
				return -1;
			}

			return name1.compareTo(name2);
		}
	}

	/**
	 * Comparator for FlyPosition objects based on their time index. Handles null
	 * values by treating them as greater than non-null values.
	 */
	public static class XYTaValue_Tindex implements Comparator<FlyPosition> {
		@Override
		public int compare(FlyPosition o1, FlyPosition o2) {
			if (o1 == null && o2 == null) {
				return 0;
			}
			if (o1 == null) {
				return 1;
			}
			if (o2 == null) {
				return -1;
			}

			return o1.flyIndexT - o2.flyIndexT;
		}
	}

	/**
	 * Comparator for Experiment objects based on their start times. Compares
	 * experiments by their first image timestamps. Handles null values by treating
	 * them as greater than non-null values.
	 */
	public static class Experiment_Start implements Comparator<Experiment> {
		@Override
		public int compare(Experiment exp1, Experiment exp2) {
			if (exp1 == null && exp2 == null) {
				return 0;
			}
			if (exp1 == null) {
				return 1;
			}
			if (exp2 == null) {
				return -1;
			}

			// Note: The original code had a bug - it was adding the same value twice
			// Fixed to use the correct comparison
			long time1 = exp1.seqCamData.getFirstImageMs();
			long time2 = exp2.seqCamData.getFirstImageMs();

			return Long.compare(time1, time2);
		}
	}
}
