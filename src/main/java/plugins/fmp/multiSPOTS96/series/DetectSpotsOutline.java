package plugins.fmp.multiSPOTS96.series;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformOptions;

public class DetectSpotsOutline extends BuildSeries {
	public boolean buildBackground = true;
	public boolean detectFlies = true;
	public DetectFlyTools find_flies = new DetectFlyTools();

	// -----------------------------------------------------

	void analyzeExperiment(Experiment exp) {
		if (!zloadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runSpotsDetect(exp);
		exp.cagesArray.orderFlyPositions();
		if (!stopFlag)
			exp.save_MS96_fliesPositions();
		exp.seqCamData.closeSequence();
		closeSequence(seqNegative);
	}

	private void runSpotsDetect(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.cagesArray.initFlyPositions(options.detectCage);

		openFlyDetectViewers(exp);
		findSpotsInAllCages(exp);
	}

	private void findSpotsInAllCages(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting spots...");
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		ImageTransformInterface transformFunction = options.transformop.getFunction();

		int t_from = options.referenceFrame;
		String title = "Frame #" + t_from + "/" + exp.seqCamData.nTotalFrames;
		progressBar.setMessage(title);

		IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t_from));
		IcyBufferedImage workImage = transformFunction.getTransformedImage(sourceImage, transformOptions);
		try {
//			seqNegative.beginUpdate();
//			seqNegative.setImage(0, 0, workImage);
//			vNegative.setTitle(title);
//			List<Rectangle2D> listRectangles = 
			find_flies.findFlies(workImage, t_from);
//			displayRectanglesAsROIs(seqNegative, listRectangles, true);
//			seqNegative.endUpdate();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		progressBar.close();
	}
}
