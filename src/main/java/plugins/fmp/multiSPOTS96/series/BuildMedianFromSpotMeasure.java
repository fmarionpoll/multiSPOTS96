package plugins.fmp.multiSPOTS96.series;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;

public class BuildMedianFromSpotMeasure extends BuildSeries {

	// -----------------------------------

	void analyzeExperiment(Experiment exp) {
		if (!exp.load_MS96_cages())
			return;

		exp.zloadKymographs();
		int imageHeight = exp.seqKymos.getSequence().getHeight();
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				spot.buildRunningMedianFromSumLevel2D(imageHeight);
			}
		}
		exp.save_MS96_spotsMeasures();
		exp.seqKymos.closeSequence();
	}

}
