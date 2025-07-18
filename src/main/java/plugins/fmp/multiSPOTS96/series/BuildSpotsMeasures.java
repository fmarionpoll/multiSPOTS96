package plugins.fmp.multiSPOTS96.series;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageCursor;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.sequence.SequenceCamData;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.ViewerFMP;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DProcessingException;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformOptions;

public class BuildSpotsMeasures extends BuildSeries {
	public Sequence seqData = new Sequence();
	private ViewerFMP vData = null;
	private ImageTransformOptions transformOptions01 = null;
	ImageTransformInterface transformFunctionSpot = null;
	ImageTransformOptions transformOptions02 = null;
	ImageTransformInterface transformFunctionFly = null;

	// --------------------------------------------

	void analyzeExperiment(Experiment exp) {
		getTimeLimitsOfSequence(exp);
		loadExperimentDataToMeasureSpots(exp);
		exp.cagesArray.setFilterOfSpotsToAnalyze(true, options);
		openViewers(exp);

		if (measureSpots(exp))
			saveComputation(exp);

		exp.cagesArray.setFilterOfSpotsToAnalyze(false, options);
		closeViewers();
	}

	private boolean loadExperimentDataToMeasureSpots(Experiment exp) {
		exp.load_MS96_experiment();
		exp.seqCamData.attachSequence(
				exp.seqCamData.getImageLoader().initSequenceFromFirstImage(exp.seqCamData.getImagesList(true)));

		boolean flag = exp.load_MS96_cages();
		if (exp.seqCamData.getTimeManager().getBinDurationMs() == 0)
			exp.loadFileIntervalsFromSeqCamData();

		return flag;
	}

	private void saveComputation(Experiment exp) {
		if (options.doCreateBinDir)
			exp.setBinSubDirectory(exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults();
		if (directory == null)
			return;

		exp.cagesArray.transferMeasuresToLevel2D();
		exp.cagesArray.medianFilterFromSumToSumClean();

		exp.save_MS96_experiment();
		exp.save_MS96_spotsMeasures();
	}

	private void initMeasureSpots(Experiment exp) {
		initMasks2D(exp);
		initSpotsDataArrays(exp);

		if (transformFunctionSpot == null) {
			transformOptions01 = new ImageTransformOptions();
			transformOptions01.transformOption = options.transform01;
			transformOptions01.copyResultsToThe3planes = false;
			transformOptions01.setSingleThreshold(options.spotThreshold, options.spotThresholdUp);
			transformFunctionSpot = options.transform01.getFunction();

			transformOptions02 = new ImageTransformOptions();
			transformOptions02.transformOption = options.transform02;
			transformOptions02.copyResultsToThe3planes = false;
			transformFunctionFly = options.transform02.getFunction();
		}
	}

	private boolean measureSpots(Experiment exp) {
		if (exp.cagesArray.getTotalNumberOfSpots() < 1) {
			System.out.println("DetectAreas:measureAreas Abort (1): nbspots = 0");
			return false;
		}

		threadRunning = true;
		stopFlag = false;
		exp.build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList();
		int iiFirst = 0;
		int iiLast = exp.seqCamData.getImageLoader().getNTotalFrames();
		vData.setTitle(exp.seqCamData.getCSCamFileName() + ": " + iiFirst + "-" + iiLast);
		ProgressFrame progressBar1 = new ProgressFrame("Analyze stack");

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("measureSpots");
		processor.setPriority(Processor.NORM_PRIORITY);
		int ntasks = iiLast - iiFirst;
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(ntasks);
		tasks.clear();

		initMeasureSpots(exp);

		for (int ii = iiFirst; ii < iiLast; ii++) {
			if (options.concurrentDisplay) {
				IcyBufferedImage sourceImage0 = imageIORead(exp.seqCamData.getFileNameFromImageList(ii));
				seqData.setImage(0, 0, sourceImage0);
				vData.setTitle("Frame #" + ii + " /" + iiLast);
			}

			final int t = ii;
			progressBar1.setMessage("Analyze frame: " + t + "//" + iiLast);
			String fileName = exp.seqCamData.getFileNameFromImageList(t);
			if (fileName == null) {
				System.out.println("filename null at t=" + t);
				continue;
			}

			final IcyBufferedImage sourceImage = imageIORead(fileName);
			final IcyBufferedImage transformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage,
					transformOptions01);
			final IcyBufferedImage transformToDetectFly = transformFunctionFly.getTransformedImage(sourceImage,
					transformOptions02);
			final IcyBufferedImageCursor cursorToDetectFly = new IcyBufferedImageCursor(transformToDetectFly);
			final IcyBufferedImageCursor cursorToMeasureArea = new IcyBufferedImageCursor(transformToMeasureArea);

//			tasks.add(processor.submit(new Runnable() {
//				@Override
//				public void run() {

			int ii_local = t - iiFirst;
			for (Cage cage : exp.cagesArray.cagesList) {
				for (Spot spot : cage.spotsArray.getSpotsList()) {
					if (!spot.isReadyForAnalysis()) {
						continue;
					}

					ROI2DAlongT roiT = spot.getRoiAtTime(t);
					ResultsThreshold results = measureSpotOverThreshold(cursorToMeasureArea, cursorToDetectFly, roiT);
					spot.getFlyPresent().setIsPresent(ii_local, results.nPoints_fly_present);
					spot.getSum().setValueAt(ii_local, results.sumOverThreshold / results.npoints_in);
					if (results.nPoints_no_fly != results.npoints_in)
						spot.getSum().setValueAt(ii_local,
								results.sumTot_no_fly_over_threshold / results.nPoints_no_fly);
				}
			}
		}
//			}));
//		}

//		waitFuturesCompletion(processor, tasks, null);
		progressBar1.close();
		return true;
	}

	private ResultsThreshold measureSpotOverThreshold(IcyBufferedImageCursor cursorToMeasureArea,
			IcyBufferedImageCursor cursorToDetectFly, ROI2DAlongT roiT) {

		ResultsThreshold result = new ResultsThreshold();
		Point[] maskPoints = roiT.getMaskPoints();
		if (maskPoints == null) {
			result.npoints_in = 0;
			return result;
		}
		result.npoints_in = maskPoints.length;

		for (int offset = 0; offset < maskPoints.length; offset++) {
			Point pt = maskPoints[offset];
			int value = (int) cursorToMeasureArea.get((int) pt.getX(), (int) pt.getY(), 0);
			int value_to_detect_fly = (int) cursorToDetectFly.get((int) pt.getX(), (int) pt.getY(), 0);

			boolean isFlyThere = isFlyPresent(value_to_detect_fly);
			if (!isFlyThere) {
				result.nPoints_no_fly++;
			} else
				result.nPoints_fly_present++;

			if (isOverThreshold(value)) {
				result.sumOverThreshold += value;
				result.nPointsOverThreshold++;
				if (!isFlyThere) {
					result.sumTot_no_fly_over_threshold += value;
				}
			}
		}
		return result;
	}

	private boolean isFlyPresent(double value) {
		boolean flag = value > options.flyThreshold;
		if (!options.flyThresholdUp)
			flag = !flag;
		return flag;
	}

	private boolean isOverThreshold(double value) {
		boolean flag = value > options.spotThreshold;
		if (!options.spotThresholdUp)
			flag = !flag;
		return flag;
	}

	private void initSpotsDataArrays(Experiment exp) {
		int nFrames = exp.seqCamData.getImageLoader().getNTotalFrames();
		int spotArrayGlobalIndex = 0;
		for (Cage cage : exp.cagesArray.cagesList) {
			int spotPosition = 0;
			for (Spot spot : cage.spotsArray.getSpotsList()) {
				spot.getProperties().setCagePosition(spotPosition);
				spot.getProperties().setCageID(cage.getProperties().getCageID());
				spot.getProperties().setSpotArrayIndex(spotArrayGlobalIndex);
				spot.getSum().setValuesArray(new double[nFrames]);
				spot.getSumClean().setValuesArray(new double[nFrames]);
				spot.getFlyPresent().setIsPresentArray(new int[nFrames]);
				spotArrayGlobalIndex++;
				spotPosition++;
			}
		}
	}

	private void initMasks2D(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData.getSequence() == null)
			seqCamData.attachSequence(
					exp.seqCamData.getImageLoader().initSequenceFromFirstImage(exp.seqCamData.getImagesList(true)));

		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.getSpotsList()) {
				if (spot.getRoiAlongTList().size() < 1)
					spot.initRoiTList(spot.getRoi());

				for (ROI2DAlongT roiT : spot.getRoiAlongTList()) {
					try {
						roiT.buildMask2DFromInputRoi();
					} catch (ROI2DProcessingException e) {
						System.err.println(
								"Error building mask for ROI at time " + roiT.getTimePoint() + ": " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void closeViewers() {
		closeViewer(vData);
		closeSequence(seqData);
	}

	private void openViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					seqData = newSequence(exp.seqCamData.getCSCamFileName(), exp.seqCamData.getSeqImage(0, 0));
					vData = new ViewerFMP(seqData, true, true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}