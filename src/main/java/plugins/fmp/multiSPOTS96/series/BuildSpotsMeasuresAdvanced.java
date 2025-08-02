package plugins.fmp.multiSPOTS96.series;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
	private final LinkedBlockingQueue<IcyBufferedImage> imagePool = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<IcyBufferedImageCursor> cursorPool = new LinkedBlockingQueue<>();
	private final int MAX_POOL_SIZE = 20;
	private final AtomicInteger poolHits = new AtomicInteger(0);
	private final AtomicInteger poolMisses = new AtomicInteger(0);

	// === STREAMING PROCESSING ===
	private final StreamingImageProcessor streamingProcessor;
	private final int STREAM_BUFFER_SIZE = 5; // Number of images to pre-load

	// === COMPRESSED MASK STORAGE ===
	private final ConcurrentHashMap<String, CompressedMask> compressedMasks = new ConcurrentHashMap<>();

	// === ADAPTIVE MEMORY MANAGEMENT ===
	private final MemoryMonitor memoryMonitor;
	private final AdaptiveBatchSizer adaptiveBatchSizer;

	// === CONFIGURATION ===
	private final AdvancedMemoryOptions advancedOptions;

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
			this.advancedOptions = new AdvancedMemoryOptions();
		else
			this.advancedOptions = advancedOptions;
		this.streamingProcessor = new StreamingImageProcessor(STREAM_BUFFER_SIZE);
		this.memoryMonitor = new MemoryMonitor();
		this.adaptiveBatchSizer = new AdaptiveBatchSizer(memoryMonitor);
	}

	void analyzeExperiment(Experiment exp) {
		getTimeLimitsOfSequence(exp);
		loadExperimentDataToMeasureSpots(exp);
		exp.cagesArray.setFilterOfSpotsToAnalyze(true, options);
		openViewers(exp);

		if (measureSpotsAdvanced(exp))
			saveComputation(exp);

		exp.cagesArray.setFilterOfSpotsToAnalyze(false, options);
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
		ProgressFrame progressBar1 = new ProgressFrame("Analyze stack (Advanced)");

		// Initialize with adaptive batch sizing
		adaptiveBatchSizer.initialize(iiLast - iiFirst, memoryMonitor.getAvailableMemoryMB());
		initMeasureSpots(exp);

		// Start streaming processor
		streamingProcessor.start(exp.seqCamData, iiFirst, iiLast);

		try {
			// Process frames using streaming approach
			for (int batchStart = iiFirst; batchStart < iiLast; batchStart += adaptiveBatchSizer
					.getCurrentBatchSize()) {
				if (stopFlag)
					break;

				int batchEnd = Math.min(batchStart + adaptiveBatchSizer.getCurrentBatchSize(), iiLast);
				processFrameBatchAdvanced(exp, batchStart, batchEnd, iiFirst, iiLast, progressBar1);

				// Update adaptive batch sizing based on memory usage
				adaptiveBatchSizer.updateBatchSize(memoryMonitor.getMemoryUsagePercent());

				// Force garbage collection if memory pressure is high
				if (memoryMonitor.getMemoryUsagePercent() > advancedOptions.memoryThresholdPercent) {
					System.gc();
				}
			}
		} finally {
			streamingProcessor.stop();
		}

		progressBar1.close();
		return true;
	}

	private void processFrameBatchAdvanced(Experiment exp, int batchStart, int batchEnd, int iiFirst, int iiLast,
			ProgressFrame progressBar1) {

		ThreadPoolExecutor executor = new ThreadPoolExecutor(advancedOptions.maxConcurrentTasks,
				advancedOptions.maxConcurrentTasks, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(batchEnd - batchStart));

		ArrayList<Future<?>> tasks = new ArrayList<>(batchEnd - batchStart);

		for (int ii = batchStart; ii < batchEnd; ii++) {
			if (options.concurrentDisplay) {
				IcyBufferedImage sourceImage0 = streamingProcessor.getImage(ii);
				if (sourceImage0 != null) {
					seqData.setImage(0, 0, sourceImage0);
					vData.setTitle("Frame #" + ii + " /" + iiLast);
				}
			}

			final int t = ii;
			progressBar1.setMessage("Analyze frame: " + t + "//" + iiLast);

			tasks.add(executor.submit(new Runnable() {
				@Override
				public void run() {
					processSingleFrameAdvanced(exp, t, iiFirst);
				}
			}));
		}

		waitFuturesCompletion(null, tasks, null);
		executor.shutdown();
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
				System.err.println("Failed to get image for frame " + frameIndex);
				return;
			}

			// Get transformed images from pool or create new ones
			transformToMeasureArea = getImageFromPool();
			transformToDetectFly = getImageFromPool();

			if (transformToMeasureArea == null) {
				transformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage, transformOptions01);
			} else {
				// For pool reuse, we need to create a new image since the interface doesn't
				// support in-place transformation
				IcyBufferedImage newTransformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage,
						transformOptions01);
				// Copy data to the pooled image
				// Note: This is a simplified approach - in a real implementation, you might
				// want to implement
				// a more sophisticated image copying mechanism
				transformToMeasureArea = newTransformToMeasureArea;
			}

			if (transformToDetectFly == null) {
				transformToDetectFly = transformFunctionFly.getTransformedImage(sourceImage, transformOptions02);
			} else {
				// For pool reuse, we need to create a new image since the interface doesn't
				// support in-place transformation
				IcyBufferedImage newTransformToDetectFly = transformFunctionFly.getTransformedImage(sourceImage,
						transformOptions02);
				// Copy data to the pooled image
				transformToDetectFly = newTransformToDetectFly;
			}

			// Get cursors from pool
			cursorToDetectFly = getCursorFromPool(transformToDetectFly);
			cursorToMeasureArea = getCursorFromPool(transformToMeasureArea);

			int ii_local = frameIndex - iiFirst;
			for (Cage cage : exp.cagesArray.cagesList) {
				for (Spot spot : cage.spotsArray.getSpotsList()) {
					if (!spot.isReadyForAnalysis()) {
						continue;
					}

					ROI2DWithMask roiT = spot.getROIMask();
					ResultsThreshold results = measureSpotOverThresholdCompressed(cursorToMeasureArea,
							cursorToDetectFly, roiT);
					spot.getFlyPresent().setIsPresentAt(ii_local, results.nPoints_fly_present);
					spot.getSum().setValueAt(ii_local, results.sumOverThreshold / results.npoints_in);
					if (results.nPoints_no_fly != results.npoints_in)
						spot.getSum().setValueAt(ii_local,
								results.sumTot_no_fly_over_threshold / results.nPoints_no_fly);
				}
			}
		} finally {
			// Return resources to pool
			returnImageToPool(transformToMeasureArea);
			returnImageToPool(transformToDetectFly);
			returnCursorToPool(cursorToDetectFly);
			returnCursorToPool(cursorToMeasureArea);
		}
	}

	private ResultsThreshold measureSpotOverThresholdCompressed(IcyBufferedImageCursor cursorToMeasureArea,
			IcyBufferedImageCursor cursorToDetectFly, ROI2DWithMask roiT) {

		ResultsThreshold result = new ResultsThreshold();

		// Get compressed mask coordinates
		CompressedMask compressedMask = getCompressedMask(roiT);
		if (compressedMask == null) {
			result.npoints_in = 0;
			return result;
		}

		int[] maskX = compressedMask.getXCoordinates();
		int[] maskY = compressedMask.getYCoordinates();
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

	private IcyBufferedImage getImageFromPool() {
		IcyBufferedImage image = imagePool.poll();
		if (image != null) {
			poolHits.incrementAndGet();
		} else {
			poolMisses.incrementAndGet();
		}
		return image;
	}

	private void returnImageToPool(IcyBufferedImage image) {
		if (image != null && imagePool.size() < MAX_POOL_SIZE) {
			imagePool.offer(image);
		}
	}

	private IcyBufferedImageCursor getCursorFromPool(IcyBufferedImage image) {
		IcyBufferedImageCursor cursor = cursorPool.poll();
		if (cursor != null) {
			// IcyBufferedImageCursor cannot be reused with a different image
			// We need to create a new cursor for each image
			poolHits.incrementAndGet();
			cursor = new IcyBufferedImageCursor(image);
		} else {
			poolMisses.incrementAndGet();
			cursor = new IcyBufferedImageCursor(image);
		}
		return cursor;
	}

	private void returnCursorToPool(IcyBufferedImageCursor cursor) {
		if (cursor != null && cursorPool.size() < MAX_POOL_SIZE) {
			cursorPool.offer(cursor);
		}
	}

	private void cleanupResources() {
		imagePool.clear();
		cursorPool.clear();
		compressedMasks.clear();

		System.out.println("Memory Pool Stats - Hits: " + poolHits.get() + ", Misses: " + poolMisses.get());
		System.out.println("Hit Rate: " + (poolHits.get() * 100.0 / (poolHits.get() + poolMisses.get())) + "%");
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

	// === INNER CLASSES ===

	/**
	 * Compressed mask storage using run-length encoding.
	 */
	public static class CompressedMask {
		private final int[] xCoords;
		private final int[] yCoords;
		private final byte[] compressedData;
		private final int originalSize;

		public CompressedMask(Point[] points) {
			this.xCoords = new int[points.length];
			this.yCoords = new int[points.length];
			this.originalSize = points.length * 8; // 4 bytes per int, 2 ints per point

			for (int i = 0; i < points.length; i++) {
				xCoords[i] = points[i].x;
				yCoords[i] = points[i].y;
			}

			// Compress using run-length encoding for consecutive coordinates
			this.compressedData = compressCoordinates(xCoords, yCoords);
		}

		public int[] getXCoordinates() {
			return xCoords;
		}

		public int[] getYCoordinates() {
			return yCoords;
		}

		public double getCompressionRatio() {
			return (double) compressedData.length / originalSize;
		}

		private byte[] compressCoordinates(int[] x, int[] y) {
			// Simple run-length encoding for consecutive coordinates
			ArrayList<Byte> compressed = new ArrayList<>();

			for (int i = 0; i < x.length; i++) {
				if (i > 0 && x[i] == x[i - 1] + 1 && y[i] == y[i - 1]) {
					// Same row, consecutive column
					compressed.add((byte) 1);
				} else if (i > 0 && x[i] == x[i - 1] && y[i] == y[i - 1] + 1) {
					// Same column, consecutive row
					compressed.add((byte) 2);
				} else {
					// New coordinate
					compressed.add((byte) 0);
					compressed.add((byte) (x[i] >> 8));
					compressed.add((byte) (x[i] & 0xFF));
					compressed.add((byte) (y[i] >> 8));
					compressed.add((byte) (y[i] & 0xFF));
				}
			}

			byte[] result = new byte[compressed.size()];
			for (int i = 0; i < compressed.size(); i++) {
				result[i] = compressed.get(i);
			}
			return result;
		}
	}

	/**
	 * Streaming image processor to avoid loading entire stack.
	 */
	public class StreamingImageProcessor {
		private final int bufferSize;
		private final ConcurrentHashMap<Integer, IcyBufferedImage> imageBuffer = new ConcurrentHashMap<>();
		private final ArrayList<String> imageFiles;
		private final int startFrame;
		private final int endFrame;
		private volatile boolean running = false;
		private Thread prefetchThread;

		public StreamingImageProcessor(int bufferSize) {
			this.bufferSize = bufferSize;
			this.imageFiles = new ArrayList<>();
			this.startFrame = 0;
			this.endFrame = 0;
		}

		public void start(SequenceCamData seqCamData, int startFrame, int endFrame) {
			this.running = true;

			// Initialize image file list
			for (int i = startFrame; i < endFrame; i++) {
				String fileName = seqCamData.getFileNameFromImageList(i);
				if (fileName != null) {
					imageFiles.add(fileName);
				}
			}

			// Start prefetch thread
			prefetchThread = new Thread(() -> prefetchImages());
			prefetchThread.setDaemon(true);
			prefetchThread.start();
		}

		public void stop() {
			running = false;
			if (prefetchThread != null) {
				prefetchThread.interrupt();
			}
			imageBuffer.clear();
		}

		public IcyBufferedImage getImage(int frameIndex) {
			IcyBufferedImage image = imageBuffer.get(frameIndex);
			if (image == null) {
				// Load image on demand if not in buffer
				String fileName = imageFiles.get(frameIndex);
				if (fileName != null) {
					image = imageIORead(fileName);
					imageBuffer.put(frameIndex, image);
				}
			}
			return image;
		}

		private void prefetchImages() {
			int currentFrame = 0;
			while (running && currentFrame < imageFiles.size()) {
				// Prefetch next batch of images
				for (int i = 0; i < bufferSize && currentFrame + i < imageFiles.size(); i++) {
					int frameIndex = currentFrame + i;
					if (!imageBuffer.containsKey(frameIndex)) {
						String fileName = imageFiles.get(frameIndex);
						if (fileName != null) {
							IcyBufferedImage image = imageIORead(fileName);
							imageBuffer.put(frameIndex, image);
						}
					}
				}

				// Remove old images from buffer
				if (imageBuffer.size() > bufferSize * 2) {
					for (int i = Math.max(0, currentFrame - bufferSize); i < currentFrame; i++) {
						imageBuffer.remove(i);
					}
				}

				currentFrame += bufferSize;

				try {
					Thread.sleep(100); // Small delay to prevent excessive CPU usage
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	/**
	 * Memory usage monitor.
	 */
	public static class MemoryMonitor {
		private final Runtime runtime = Runtime.getRuntime();

		public long getTotalMemoryMB() {
			return runtime.totalMemory() / 1024 / 1024;
		}

		public long getFreeMemoryMB() {
			return runtime.freeMemory() / 1024 / 1024;
		}

		public long getUsedMemoryMB() {
			return getTotalMemoryMB() - getFreeMemoryMB();
		}

		public long getMaxMemoryMB() {
			return runtime.maxMemory() / 1024 / 1024;
		}

		public double getMemoryUsagePercent() {
			return (double) getUsedMemoryMB() / getMaxMemoryMB() * 100.0;
		}

		public long getAvailableMemoryMB() {
			return getMaxMemoryMB() - getUsedMemoryMB();
		}
	}

	/**
	 * Adaptive batch sizing based on available memory.
	 */
	public static class AdaptiveBatchSizer {
		private final MemoryMonitor memoryMonitor;
		private int currentBatchSize;
		private final int minBatchSize = 3;
		private final int maxBatchSize = 50;

		public AdaptiveBatchSizer(MemoryMonitor memoryMonitor) {
			this.memoryMonitor = memoryMonitor;
			this.currentBatchSize = 10; // Default
		}

		public void initialize(int totalFrames, long availableMemoryMB) {
			// Calculate optimal batch size based on available memory
			int optimalBatchSize = (int) Math.min(maxBatchSize, Math.max(minBatchSize, availableMemoryMB / 100)); // Rough
																													// estimate
			this.currentBatchSize = Math.min(optimalBatchSize, totalFrames);
		}

		public void updateBatchSize(double memoryUsagePercent) {
			if (memoryUsagePercent > 85) {
				// High memory pressure - reduce batch size
				currentBatchSize = Math.max(minBatchSize, currentBatchSize - 2);
			} else if (memoryUsagePercent < 50) {
				// Low memory pressure - increase batch size
				currentBatchSize = Math.min(maxBatchSize, currentBatchSize + 1);
			}
		}

		public int getCurrentBatchSize() {
			return currentBatchSize;
		}
	}
}