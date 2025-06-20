package plugins.fmp.multiSPOTS96.series;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformOptions;

public class DetectSpotsOutline extends BuildSeries {
	public boolean buildBackground = true;
	public boolean detectFlies = true;
	public DetectSpotsTools find_spots = new DetectSpotsTools();

	// -----------------------------------------------------

	void analyzeExperiment(Experiment exp) {
		if (!zloadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runSpotsDetect(exp);
		if (!stopFlag)
			exp.save_MS96_fliesPositions();
		exp.seqCamData.closeSequence();
		closeSequence(seqNegative);
	}

	private void runSpotsDetect(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting spots...");
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		ImageTransformInterface transformFunction = options.transformop.getFunction();
		// find_spots.options = options;

		int t_from = (int) options.fromFrame;
		String title = "Frame #" + t_from + "/" + exp.seqCamData.nTotalFrames;
		progressBar.setMessage(title);

		IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t_from));
		IcyBufferedImage workImage = transformFunction.getTransformedImage(sourceImage, transformOptions);
		try {
			find_spots.findSpots(exp, options, workImage);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		progressBar.close();
	}
}
