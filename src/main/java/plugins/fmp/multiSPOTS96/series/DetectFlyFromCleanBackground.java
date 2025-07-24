package plugins.fmp.multiSPOTS96.series;

import java.awt.geom.Rectangle2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformEnums;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformOptions;

public class DetectFlyFromCleanBackground extends BuildSeries {
	private DetectFlyTools find_flies = new DetectFlyTools();
	public boolean viewInternalImages = true;

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) {
		if (!loadSeqCamDataAndCages(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect2(exp);
		exp.cagesArray.orderFlyPositions();
		if (!stopFlag)
			exp.save_MS96_fliesPositions();
		exp.seqCamData.closeSequence();
//		closeSequence(seqNegative);
	}

	private void runFlyDetect2(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		exp.cagesArray.initFlyPositions(options.detectCage);
		options.threshold = options.thresholdDiff;

		if (exp.loadReferenceImage()) {
			openFlyDetectViewers(exp);
			findFliesInAllFrames(exp);
		}
	}

	private void findFliesInAllFrames(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = ImageTransformEnums.SUBTRACT_REF;
		transformOptions.backgroundImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getReferenceImage());
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();

		int totalFrames = exp.seqCamData.getImageLoader().getNTotalFrames();
		for (int index = 0; index < totalFrames; index++) {
			int t_from = index;
			String title = "Frame #" + t_from + "/" + exp.seqCamData.getImageLoader().getNTotalFrames();
			progressBar.setMessage(title);

			IcyBufferedImage workImage = imageIORead(exp.seqCamData.getFileNameFromImageList(t_from));
			IcyBufferedImage negativeImage = transformFunction.getTransformedImage(workImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, negativeImage);
				vNegative.setTitle(title);
				List<Rectangle2D> listRectangles = find_flies.findFlies(negativeImage, t_from);
				displayRectanglesAsROIs(seqNegative, listRectangles, true);
				seqNegative.endUpdate();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		progressBar.close();
	}

}