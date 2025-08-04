package plugins.fmp.multiSPOTS96.series;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageCursor;
import icy.sequence.Sequence;
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

/**
 * Advanced optimized version of BuildSpotsMeasures with streaming processing,
 * compressed mask storage, and memory pool optimizations.
 * 
 * Features: - Streaming image processing to avoid loading entire stack -
 * Compressed mask storage using run-length encoding - Memory pool for reusing
 * image objects - Adaptive batch sizing based on available memory - Advanced
 * memory monitoring and management
 */
public class BuildSpotsMeasuresAdvanced extends BuildSeries {

	// === MEMORY POOL ===
	// Removed image pooling due to null pointer issues
	private final LinkedBlockingQueue<IcyBufferedImageCursor> cursorPool = new LinkedBlockingQueue<>();
	private final int MAX_POOL_SIZE = 20;
	private final AtomicInteger poolHits = new AtomicInteger(0);
	private final AtomicInteger poolMisses = new AtomicInteger(0);

	// === STREAMING PROCESSING ===
	private final StreamingImageProcessor streamingProcessor;

	// === COMPRESSED MASK STORAGE ===
	private final ConcurrentHashMap<String, CompressedMask> compressedMasks = new ConcurrentHashMap<>();

	// === ADAPTIVE MEMORY MANAGEMENT ===
	private final MemoryMonitor memoryMonitor;
	private final AdaptiveBatchSizer adaptiveBatchSizer;

	// === MEMORY OPTIMIZATION ADDITIONS ===
	private final long MEMORY_PRESSURE_THRESHOLD_MB = 5; // Memory pressure threshold (reduced from 10)
	private final double MEMORY_USAGE_THRESHOLD_PERCENT = 30.0; // Memory usage threshold (reduced from 50.0)
	private final boolean USE_NATIVE_IO_ONLY = false; // Nuclear option: bypass Icy entirely (disabled for testing)

	// === TRADITIONAL FIELDS ===
	public Sequence seqData = new Sequence();
	private ViewerFMP vData = null;
	private ImageTransformOptions transformOptions01 = null;
	ImageTransformInterface transformFunctionSpot = null;
	ImageTransformOptions transformOptions02 = null;
	ImageTransformInterface transformFunctionFly = null;

	// --------------------------------------------

	public BuildSpotsMeasuresAdvanced(AdvancedMemoryOptions advancedOptions) {
		if (advancedOptions == null)
			new AdvancedMemoryOptions();
		else {
		}
		this.memoryMonitor = new MemoryMonitor();
		this.streamingProcessor = new StreamingImageProcessor(memoryMonitor);
		this.adaptiveBatchSizer = new AdaptiveBatchSizer(memoryMonitor);
	}

	void analyzeExperiment(Experiment exp) {
		getTimeLimitsOfSequence(exp);
		loadExperimentDataToMeasureSpots(exp);
		exp.cagesArray.setReadyToAnalyze(true, options);
		openViewers(exp);

		if (measureSpotsAdvanced(exp))
			saveComputation(exp);

		exp.cagesArray.setReadyToAnalyze(false, options);
		closeViewers();
		cleanupResources();
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
		String directory = exp.getDirectoryToSaveResults();
		if (directory == null)
			return;

		exp.cagesArray.transferMeasuresToLevel2D();
		exp.cagesArray.medianFilterFromSumToSumClean();

		exp.save_MS96_experiment();
		exp.save_MS96_spotsMeasures();
	}

	private void initMeasureSpots(Experiment exp) {
		initMasks2DCompressed(exp);
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

	private boolean measureSpotsAdvanced(Experiment exp) {
		if (exp.cagesArray.getTotalNumberOfSpots() < 1) {
//			System.out.println("DetectAreas:measureAreas Abort (1): nbspots = 0");
			return false;
		}

		threadRunning = true;
		stopFlag = false;
		if (!exp.seqCamData.build_MsTimesArray_From_FileNamesList())
			return false;

		int iiFirst = 0;
		int iiLast = exp.seqCamData.getImageLoader().getNTotalFrames();
		vData.setTitle(exp.seqCamData.getCSCamFileName() + ": " + iiFirst + "-" + iiLast);
		ProgressFrame progressBar1 = new ProgressFrame("Analyze stack (Advanced)");

		// Log initial memory state
//		System.out.println("=== Memory Optimization Analysis ===");
//		System.out.println("Initial memory usage: " + memoryMonitor.getUsedMemoryMB() + "MB / "
//				+ memoryMonitor.getMaxMemoryMB() + "MB (" + memoryMonitor.getMemoryUsagePercent() + "%)");
//		System.out.println("Available memory: " + memoryMonitor.getAvailableMemoryMB() + "MB");
//		System.out.println("Total frames to process: " + (iiLast - iiFirst));
//		System.out.println("Memory thresholds - Available: " + MEMORY_PRESSURE_THRESHOLD_MB + "MB, Usage: "
//				+ MEMORY_USAGE_THRESHOLD_PERCENT + "%");

		// Initialize with adaptive batch sizing
		adaptiveBatchSizer.initialize(iiLast - iiFirst, memoryMonitor.getAvailableMemoryMB());
		initMeasureSpots(exp);

		// Start streaming processor
		streamingProcessor.start(exp.seqCamData, iiFirst, iiLast);

		long startTime = System.currentTimeMillis();
		int processedBatches = 0;

		try {
			// Process frames using streaming approach
			for (int batchStart = iiFirst; batchStart < iiLast; batchStart += adaptiveBatchSizer
					.getCurrentBatchSize()) {
				if (stopFlag)
					break;

				int batchEnd = Math.min(batchStart + adaptiveBatchSizer.getCurrentBatchSize(), iiLast);

				// Log memory state before batch
				long batchStartMemory = memoryMonitor.getUsedMemoryMB();
//				System.out.println("Processing batch " + (processedBatches + 1) + " (frames " + batchStart + "-"
//						+ (batchEnd - 1) + "). Memory: " + batchStartMemory + "MB ("
//						+ memoryMonitor.getMemoryUsagePercent() + "%)");

				processFrameBatchAdvanced(exp, batchStart, batchEnd, iiFirst, iiLast, progressBar1);

				// Log memory state after batch
				long batchEndMemory = memoryMonitor.getUsedMemoryMB();
//				System.out.println("Batch " + (processedBatches + 1) + " completed. Memory: " + batchEndMemory + "MB ("
//						+ memoryMonitor.getMemoryUsagePercent() + "%). Delta: " + (batchEndMemory - batchStartMemory)
//						+ "MB");

				processedBatches++;

				// Update adaptive batch sizing based on memory usage
				adaptiveBatchSizer.updateBatchSize(memoryMonitor.getMemoryUsagePercent());

				// Force garbage collection if memory pressure is high
				if (memoryMonitor.getMemoryUsagePercent() > 60.0) { // Reduced from
																	// advancedOptions.memoryThresholdPercent (80%)
//					System.out.println("High memory pressure detected: " + memoryMonitor.getMemoryUsagePercent()
//							+ "%. Forcing GC...");
					System.gc();
					Thread.yield(); // Give GC time to work

					// Log memory after GC
//					System.out.println("After GC: " + memoryMonitor.getUsedMemoryMB() + "MB ("
//							+ memoryMonitor.getMemoryUsagePercent() + "%)");
				}

				// Also force GC every few batches to prevent memory buildup
				if (processedBatches % 5 == 0) {
//					System.out.println("Periodic GC after " + processedBatches + " batches. Memory: "
//							+ memoryMonitor.getUsedMemoryMB() + "MB");
					System.gc();
					Thread.yield();
				}

				// Check if we need to pause processing due to memory pressure
				if (memoryMonitor.getMemoryUsagePercent() > 98) {
//					System.out.println("Critical memory pressure: " + memoryMonitor.getMemoryUsagePercent()
//							+ "%. Pausing processing...");
					try {
						Thread.sleep(1000); // Wait for memory to be freed
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); // Restore interrupt status
//						System.out.println("Processing interrupted during memory pause");
					}
					System.gc();
//					System.out.println("After pause: " + memoryMonitor.getUsedMemoryMB() + "MB ("
//							+ memoryMonitor.getMemoryUsagePercent() + "%)");
				}
			}
		} finally {
			streamingProcessor.stop();
		}

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;

		// Log final memory state
		System.out.println("=== Processing Complete ===");
		System.out.println("Total processing time: " + totalTime + "ms (" + (totalTime / 1000.0) + "s)");
		System.out.println("Processed " + processedBatches + " batches");
		System.out.println("Final memory usage: " + memoryMonitor.getUsedMemoryMB() + "MB / "
				+ memoryMonitor.getMaxMemoryMB() + "MB (" + memoryMonitor.getMemoryUsagePercent() + "%)");
		System.out.println("Available memory: " + memoryMonitor.getAvailableMemoryMB() + "MB");

		// Force final cleanup
		System.out.println("Performing final cleanup...");

		// Clear image buffer completely
		streamingProcessor.clearAllImages();

		// Try to force Icy to release all cached data
		try {
			// Force Icy to clear any internal caches
			System.out.println("Attempting to clear Icy internal caches...");

			// Try to access Icy's internal image cache if it exists
			try {
				Class<?> icyImageCacheClass = Class.forName("icy.image.ImageCache");
				java.lang.reflect.Method clearCacheMethod = icyImageCacheClass.getDeclaredMethod("clearCache");
				if (clearCacheMethod != null) {
					clearCacheMethod.setAccessible(true);
					clearCacheMethod.invoke(null);
					System.out.println("Cleared Icy image cache");
				}
			} catch (Exception e) {
				System.out.println("Could not clear Icy image cache: " + e.getMessage());
			}

			// Try to force Icy to release any internal buffers
			try {
				Class<?> icyImageClass = Class.forName("icy.image.IcyBufferedImage");
				java.lang.reflect.Method disposeMethod = icyImageClass.getDeclaredMethod("dispose");
				if (disposeMethod != null) {
					disposeMethod.setAccessible(true);
					// This is just to check if the method exists
					System.out.println("IcyBufferedImage has dispose method available");
				}
			} catch (Exception e) {
				System.out.println("IcyBufferedImage dispose method not available: " + e.getMessage());
			}

		} catch (Exception e) {
//			System.out.println("Error during Icy cache cleanup: " + e.getMessage());
		}

		// Multiple GC passes with longer delays
		for (int i = 0; i < 7; i++) {
			System.gc();
			try {
				Thread.sleep(1000); // Give much more time for GC to work
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
//			System.out.println("GC pass " + (i + 1) + ": " + memoryMonitor.getUsedMemoryMB() + "MB ("
//					+ memoryMonitor.getMemoryUsagePercent() + "%)");
		}

//		System.out.println("After final cleanup: " + memoryMonitor.getUsedMemoryMB() + "MB ("
//				+ memoryMonitor.getMemoryUsagePercent() + "%)");

		progressBar1.close();
		return true;
	}

	private void processFrameBatchAdvanced(Experiment exp, int batchStart, int batchEnd, int iiFirst, int iiLast,
			ProgressFrame progressBar1) {

		// Check memory pressure before processing batch
		if (memoryMonitor.getMemoryUsagePercent() > MEMORY_USAGE_THRESHOLD_PERCENT) {
//			System.out
//					.println("Memory pressure detected: " + memoryMonitor.getMemoryUsagePercent() + "%. Forcing GC...");
			System.gc();
			Thread.yield(); // Give GC time to work
		}

		// Process frames SEQUENTIALLY to maintain order and prevent scrambled results
		for (int ii = batchStart; ii < batchEnd; ii++) {
			if (stopFlag) {
				break;
			}

			if (options.concurrentDisplay) {
				IcyBufferedImage sourceImage0 = streamingProcessor.getImage(ii);
				if (sourceImage0 != null) {
					seqData.setImage(0, 0, sourceImage0);
					vData.setTitle("Frame #" + ii + " /" + iiLast);
				}
			}

			progressBar1.setMessage("Analyze frame: " + ii + "//" + iiLast);

			// Process frame sequentially to maintain order
//			System.out.println("Processing frame " + ii + " sequentially");
			processSingleFrameAdvanced(exp, ii, iiFirst);
		}
	}

	private void processSingleFrameAdvanced(Experiment exp, int frameIndex, int iiFirst) {
		IcyBufferedImage sourceImage = null;
		IcyBufferedImage transformToMeasureArea = null;
		IcyBufferedImage transformToDetectFly = null;
		IcyBufferedImageCursor cursorToDetectFly = null;
		IcyBufferedImageCursor cursorToMeasureArea = null;

		try {
			// Get image from streaming processor
			sourceImage = streamingProcessor.getImage(frameIndex);
			if (sourceImage == null) {
				System.err.println("Failed to get image for frame " + frameIndex + ". Skipping processing.");
				return;
			}

			// Check if image data is valid
			try {
				Object data = sourceImage.getDataXY(0);
				if (data == null) {
					System.err.println("Source image data is null for frame " + frameIndex + ". Skipping processing.");
					return;
				}
			} catch (Exception e) {
				System.err.println("Error accessing source image data for frame " + frameIndex + ": " + e.getMessage()
						+ ". Skipping processing.");
				return;
			}

			// Create new transformed images
			transformToMeasureArea = null;
			transformToDetectFly = null;

			// Always create new transformed images - the pool approach is causing issues
			transformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage, transformOptions01);
			transformToDetectFly = transformFunctionFly.getTransformedImage(sourceImage, transformOptions02);

			// Validate transformed images
			if (transformToMeasureArea == null) {
				System.err.println("transformToMeasureArea is null for frame " + frameIndex);
				return;
			}
			try {
				Object data = transformToMeasureArea.getDataXY(0);
				if (data == null) {
					System.err.println("transformToMeasureArea data is null for frame " + frameIndex);
					return;
				}
			} catch (Exception e) {
				System.err.println(
						"Error accessing transformToMeasureArea data for frame " + frameIndex + ": " + e.getMessage());
				return;
			}

			if (transformToDetectFly == null) {
				System.err.println("transformToDetectFly is null for frame " + frameIndex);
				return;
			}
			try {
				Object data = transformToDetectFly.getDataXY(0);
				if (data == null) {
					System.err.println("transformToDetectFly data is null for frame " + frameIndex);
					return;
				}
			} catch (Exception e) {
				System.err.println(
						"Error accessing transformToDetectFly data for frame " + frameIndex + ": " + e.getMessage());
				return;
			}

			// Get cursors from pool
			cursorToDetectFly = getCursorFromPool(transformToDetectFly);
			cursorToMeasureArea = getCursorFromPool(transformToMeasureArea);

			// Validate cursors
			if (cursorToDetectFly == null || cursorToMeasureArea == null) {
				System.err.println("Failed to create cursors for frame " + frameIndex);
				return;
			}

			int ii_local = frameIndex - iiFirst;
			for (Cage cage : exp.cagesArray.cagesList) {
				for (Spot spot : cage.spotsArray.getSpotsList()) {
					if (!spot.isReadyForAnalysis()) {
						continue;
					}

					ROI2DWithMask roiT = spot.getROIMask();
					if (roiT == null) {
						System.err.println("ROI mask is null for spot in frame " + frameIndex);
						continue;
					}

					ResultsThreshold results = measureSpotOverThresholdCompressed(cursorToMeasureArea,
							cursorToDetectFly, roiT, transformToMeasureArea);

					// Validate results before setting values
					if (results.npoints_in > 0) {
						spot.getFlyPresent().setIsPresentAt(ii_local, results.nPoints_fly_present);
						spot.getSum().setValueAt(ii_local, results.sumOverThreshold / results.npoints_in);
						if (results.nPoints_no_fly != results.npoints_in)
							spot.getSum().setValueAt(ii_local,
									results.sumTot_no_fly_over_threshold / results.nPoints_no_fly);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Error processing frame " + frameIndex + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Simplified cleanup - just nullify references to let GC handle it
			// The setDataXY(null) approach is causing issues with Icy's internal state
			if (sourceImage != null) {
				sourceImage = null;
			}
			if (transformToMeasureArea != null) {
				transformToMeasureArea = null;
			}
			if (transformToDetectFly != null) {
				transformToDetectFly = null;
			}

			// Clear cursors to release references
			if (cursorToDetectFly != null) {
				cursorToDetectFly = null;
			}
			if (cursorToMeasureArea != null) {
				cursorToMeasureArea = null;
			}

			// Force cleanup after each frame
			System.gc();
			Thread.yield();
		}
	}

	private ResultsThreshold measureSpotOverThresholdCompressed(IcyBufferedImageCursor cursorToMeasureArea,
			IcyBufferedImageCursor cursorToDetectFly, ROI2DWithMask roiT, IcyBufferedImage transformToMeasureArea) {

		ResultsThreshold result = new ResultsThreshold();

		// Validate inputs
		if (cursorToMeasureArea == null || cursorToDetectFly == null) {
			System.err.println("Null cursor provided to measureSpotOverThresholdCompressed");
			result.npoints_in = 0;
			return result;
		}

		// Get compressed mask coordinates
		CompressedMask compressedMask = getCompressedMask(roiT);
		if (compressedMask == null) {
			result.npoints_in = 0;
			return result;
		}

		int[] maskX = compressedMask.getXCoordinates();
		int[] maskY = compressedMask.getYCoordinates();

		// Validate mask coordinates
		if (maskX == null || maskY == null || maskX.length == 0 || maskY.length == 0) {
			System.err.println("Invalid mask coordinates");
			result.npoints_in = 0;
			return result;
		}

		result.npoints_in = maskX.length;

		try {
			// Validate the image before getting dimensions
			if (transformToMeasureArea == null) {
				System.err.println("transformToMeasureArea is null in measureSpotOverThresholdCompressed");
				result.npoints_in = 0;
				return result;
			}

			// Get image dimensions for bounds checking - use the images directly
			// since we have access to them in the calling method
			int imageWidth = transformToMeasureArea.getSizeX();
			int imageHeight = transformToMeasureArea.getSizeY();

			for (int offset = 0; offset < maskX.length; offset++) {
				int x = maskX[offset];
				int y = maskY[offset];

				// Validate coordinates are within image bounds
				if (x < 0 || y < 0 || x >= imageWidth || y >= imageHeight) {
					continue; // Skip out-of-bounds pixels
				}

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
		} catch (Exception e) {
			System.err.println("Error in measureSpotOverThresholdCompressed: " + e.getMessage());
			result.npoints_in = 0;
		}

		return result;
	}

	private CompressedMask getCompressedMask(ROI2DWithMask roiT) {
		String maskKey = roiT.getInputRoi().getName() + "_" + System.identityHashCode(roiT.getInputRoi());

		return compressedMasks.computeIfAbsent(maskKey, key -> {
			Point[] maskPoints = roiT.getMaskPoints();
			if (maskPoints == null || maskPoints.length == 0) {
				return null;
			}
			return new CompressedMask(maskPoints);
		});
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

	private void initMasks2DCompressed(Experiment exp) {
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

	// === MEMORY POOL MANAGEMENT ===

	private IcyBufferedImageCursor getCursorFromPool(IcyBufferedImage image) {
		if (image == null) {
			System.err.println("Cannot create cursor for null image");
			return null;
		}

		// Always create a new cursor for each image to avoid issues
		poolMisses.incrementAndGet();
		return new IcyBufferedImageCursor(image);
	}

	private void cleanupResources() {
		cursorPool.clear();
		compressedMasks.clear();
//
//		System.out.println("Memory Pool Stats - Hits: " + poolHits.get() + ", Misses: " + poolMisses.get());
//		System.out.println("Hit Rate: " + (poolHits.get() * 100.0 / (poolHits.get() + poolMisses.get())) + "%");
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

	// === INNER CLASSES REMOVED - NOW EXTERNAL CLASSES ===
}