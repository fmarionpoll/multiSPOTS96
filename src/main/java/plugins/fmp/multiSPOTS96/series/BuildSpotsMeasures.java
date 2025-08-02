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
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DProcessingException;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DValidationException;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DWithMask;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformInterface;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformOptions;

public class BuildSpotsMeasures extends BuildSeries {
	public Sequence seqData = new Sequence();
	private ViewerFMP vData = null;
	private ImageTransformOptions transformOptions01 = null;
	ImageTransformInterface transformFunctionSpot = null;
	ImageTransformOptions transformOptions02 = null;
	ImageTransformInterface transformFunctionFly = null;

	// Memory optimization constants - now configurable via options
	// private static final int BATCH_SIZE = 10; // Process frames in batches
	// private static final int MAX_CONCURRENT_TASKS = 4; // Limit concurrent
	// processing
	// private static final boolean ENABLE_MEMORY_CLEANUP = true;

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
//		if (options.doCreateBinDir)
//			exp.setBinSubDirectory(exp.getBinNameFromKymoFrameStep());
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
		if (!exp.seqCamData.build_MsTimesArray_From_FileNamesList())
			return false;

		int iiFirst = 0;
		int iiLast = exp.seqCamData.getImageLoader().getNTotalFrames();
		vData.setTitle(exp.seqCamData.getCSCamFileName() + ": " + iiFirst + "-" + iiLast);
		ProgressFrame progressBar1 = new ProgressFrame("Analyze stack");

		final Processor processor = new Processor(Math.min(options.maxConcurrentTasks, SystemUtil.getNumberOfCPUs()));
		processor.setThreadName("measureSpots");
		processor.setPriority(Processor.NORM_PRIORITY);

		initMeasureSpots(exp);

		// Process frames in batches to control memory usage
		for (int batchStart = iiFirst; batchStart < iiLast; batchStart += options.batchSize) {
			if (stopFlag)
				break;

			int batchEnd = Math.min(batchStart + options.batchSize, iiLast);
			processFrameBatch(exp, batchStart, batchEnd, iiFirst, iiLast, processor, progressBar1);

			// Force garbage collection between batches
			if (options.enableMemoryCleanup) {
				System.gc();
			}
		}

		progressBar1.close();
		return true;
	}

	private void processFrameBatch(Experiment exp, int batchStart, int batchEnd, int iiFirst, int iiLast,
			Processor processor, ProgressFrame progressBar1) {

		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(batchEnd - batchStart);

		for (int ii = batchStart; ii < batchEnd; ii++) {
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

			tasks.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					processSingleFrame(exp, t, iiFirst, fileName);
				}
			}));
		}

		waitFuturesCompletion(processor, tasks, null);
	}

	private void processSingleFrame(Experiment exp, int frameIndex, int iiFirst, String fileName) {
		IcyBufferedImage sourceImage = null;
		IcyBufferedImage transformToMeasureArea = null;
		IcyBufferedImage transformToDetectFly = null;
		IcyBufferedImageCursor cursorToDetectFly = null;
		IcyBufferedImageCursor cursorToMeasureArea = null;

		try {
			sourceImage = imageIORead(fileName);
			transformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage, transformOptions01);
			transformToDetectFly = transformFunctionFly.getTransformedImage(sourceImage, transformOptions02);

			cursorToDetectFly = new IcyBufferedImageCursor(transformToDetectFly);
			cursorToMeasureArea = new IcyBufferedImageCursor(transformToMeasureArea);

			int ii_local = frameIndex - iiFirst;
			for (Cage cage : exp.cagesArray.cagesList) {
				for (Spot spot : cage.spotsArray.getSpotsList()) {
					if (!spot.isReadyForAnalysis()) {
						continue;
					}

					ROI2DWithMask roiT = spot.getROIMask();
					ResultsThreshold results = measureSpotOverThreshold(cursorToMeasureArea, cursorToDetectFly, roiT);
					spot.getFlyPresent().setIsPresentAt(ii_local, results.nPoints_fly_present);
					spot.getSum().setValueAt(ii_local, results.sumOverThreshold / results.npoints_in);
					if (results.nPoints_no_fly != results.npoints_in)
						spot.getSum().setValueAt(ii_local,
								results.sumTot_no_fly_over_threshold / results.nPoints_no_fly);
				}
			}
		} finally {
			// Clean up image resources
			if (options.enableMemoryCleanup) {
				if (sourceImage != null)
					sourceImage = null;
				if (transformToMeasureArea != null)
					transformToMeasureArea = null;
				if (transformToDetectFly != null)
					transformToDetectFly = null;
				if (cursorToDetectFly != null)
					cursorToDetectFly = null;
				if (cursorToMeasureArea != null)
					cursorToMeasureArea = null;
			}
		}
	}

	private ResultsThreshold measureSpotOverThreshold(IcyBufferedImageCursor cursorToMeasureArea,
			IcyBufferedImageCursor cursorToDetectFly, ROI2DWithMask roiT) {

		ResultsThreshold result = new ResultsThreshold();

		if (options.usePrimitiveArrays) {
			// Use memory-efficient primitive arrays instead of Point objects
			int[][] maskCoords = roiT.getMaskPointsAsArrays();
			if (maskCoords == null) {
				result.npoints_in = 0;
				return result;
			}

			int[] maskX = maskCoords[0];
			int[] maskY = maskCoords[1];
			result.npoints_in = maskX.length;

			for (int offset = 0; offset < maskX.length; offset++) {
				int x = maskX[offset];
				int y = maskY[offset];
				int value = (int) cursorToMeasureArea.get(x, y, 0);
				int value_to_detect_fly = (int) cursorToDetectFly.get(x, y, 0);

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
		} else {
			// Use original Point[] approach for backward compatibility
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
				spot.getSum().setValues(new double[nFrames]);
				spot.getSumClean().setValues(new double[nFrames]);
				spot.getFlyPresent().setIsPresent(new int[nFrames]);
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
				ROI2DWithMask roiT = null;
				try {
					roiT = new ROI2DWithMask(spot.getRoi());
					roiT.buildMask2DFromInputRoi();
				} catch (ROI2DProcessingException | ROI2DValidationException e) {
					System.err.println("Error building mask for ROI: " + e.getMessage());
					e.printStackTrace();
				}
				spot.setROIMask(roiT);
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