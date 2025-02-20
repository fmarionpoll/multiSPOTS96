package plugins.fmp.multiSPOTS96.experiment;

import java.util.Iterator;
import java.util.List;

import icy.roi.ROI;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.CagesArray;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotString;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROIUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class ExperimentUtils {

	public static void transferCamDataROIStoSpots(Experiment exp) {
		if (exp.cagesArray == null)
			exp.cagesArray = new CagesArray();

		List<ROI> listROIsSpots = ROIUtilities.getROIsContainingString("spot", exp.seqCamData.seq);
		for (ROI roi : listROIsSpots) {
			boolean found = false;
			for (Cage cage : exp.cagesArray.cagesList) {
				for (Spot spot : cage.spotsArray.spotsList) {
					if (spot.getRoi() != null && roi.getName().equals(spot.getRoi().getName())) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				String name = roi.getName();
				ROI2DPolygon roi_new = new ROI2DPolygon();
				int cageID = SpotString.getCageIDFromSpotName(name);
				int cagePosition = SpotString.getSpotCagePositionFromSpotName(name);
				if (cageID >= 0 && cagePosition >= 0) {
					Cage cage = exp.cagesArray.getCageFromID(cageID);
					cage.spotsArray.spotsList.add(new Spot(roi_new));
				}
			}
		}
	}

	public void removeSpotsWithNoCamDataROI(Experiment exp) {
		if (exp.cagesArray == null)
			exp.cagesArray = new CagesArray();

		List<ROI> listROIsSpots = ROIUtilities.getROIsContainingString("spot", exp.seqCamData.seq);

		// spot with no corresponding roi? remove
		for (Cage cage : exp.cagesArray.cagesList) {
			Iterator<Spot> iterator = cage.spotsArray.spotsList.iterator();
			while (iterator.hasNext()) {
				Spot spot = iterator.next();
				boolean found = false;
				for (ROI roi : listROIsSpots) {
					if (roi.getName().equals(spot.getRoi().getName())) {
						found = true;
						break;
					}
				}
				if (!found)
					iterator.remove();
			}
		}
	}

	public static void transferSpotsToCamDataSequence(Experiment exp) {
		if (exp.cagesArray == null)
			return;

		List<ROI> listROISSpots = ROIUtilities.getROIsContainingString("spot", exp.seqCamData.seq);
		// roi with no corresponding cap? add ROI
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				boolean found = false;
				for (ROI roi : listROISSpots) {
					if (roi.getName().equals(spot.getRoi().getName())) {
						found = true;
						break;
					}
				}
				if (!found)
					exp.seqCamData.seq.addROI(spot.getRoi());
			}
		}
	}

	public static void transferCagesToCamDataSequence(Experiment exp) {
		if (exp.cagesArray == null)
			return;

		List<ROI> roisAlreadyTransferred = ROIUtilities.getROIsContainingString("cage", exp.seqCamData.seq);
		// roi with no corresponding cap? add ROI
		for (Cage cage : exp.cagesArray.cagesList) {
			boolean found = false;
			for (ROI roi : roisAlreadyTransferred) {
				if (roi.getName().equals(cage.getRoi().getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				exp.seqCamData.seq.addROI(cage.getRoi());
		}
	}
}
