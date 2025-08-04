package plugins.fmp.multiSPOTS96.series;

import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import plugins.fmp.multiSPOTS96.experiment.sequence.SequenceCamData;

/**
 * Streaming image processor to avoid loading entire stack.
 * 
 * This class provides efficient image loading and processing by loading images
 * on-demand rather than loading the entire stack into memory.
 */
public class StreamingImageProcessor {
	private final ArrayList<String> imageFiles;
	private final int startFrame;
	private final int endFrame;
	private volatile boolean running = false;
	private Thread prefetchThread;
	private final MemoryMonitor memoryMonitor;
	private final long MEMORY_PRESSURE_THRESHOLD_MB = 5;
	private final double MEMORY_USAGE_THRESHOLD_PERCENT = 30.0;

	public StreamingImageProcessor(MemoryMonitor memoryMonitor) {
		this.imageFiles = new ArrayList<>();
		this.startFrame = 0;
		this.endFrame = 0;
		this.memoryMonitor = memoryMonitor;
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
		clearAllImages();
	}

	public void clearAllImages() {
		// No buffer to clear - images are loaded on demand and immediately discarded
		System.out.println("No image buffer to clear - using on-demand loading");
	}

	public IcyBufferedImage getImage(int frameIndex) {
		// Check memory pressure before loading
		if (memoryMonitor.getMemoryUsagePercent() > MEMORY_USAGE_THRESHOLD_PERCENT) {
			System.gc();
			Thread.yield();
		}

		// Load image on demand - NO BUFFERING to prevent memory accumulation
		String fileName = imageFiles.get(frameIndex);
		if (fileName == null) {
			System.err.println("No filename found for frame " + frameIndex);
			return null;
		}

		// Only skip loading if memory is critically low
		if (memoryMonitor.getAvailableMemoryMB() < MEMORY_PRESSURE_THRESHOLD_MB
				&& memoryMonitor.getMemoryUsagePercent() > 95.0) {
			System.out.println("Critical memory pressure: " + memoryMonitor.getMemoryUsagePercent() + "%. Available: "
					+ memoryMonitor.getAvailableMemoryMB() + "MB. Skipping frame " + frameIndex);
			return null;
		}

		long memoryBefore = memoryMonitor.getUsedMemoryMB();
		IcyBufferedImage image = null;

		// Try native I/O first, then fallback to Icy's original method
		System.out.println("Loading frame " + frameIndex + " sequentially -- " + fileName);
		image = loadImageWithNativeIO(fileName);
		if (image == null) {
			System.out.println("Native I/O failed, falling back to Icy's imageIORead for frame " + frameIndex);
			image = imageIORead(fileName);
		} else {
			// Validate the loaded image
			try {
				Object data = image.getDataXY(0);
				if (data == null) {
					System.out.println("Native I/O created invalid image, falling back to Icy's imageIORead for frame "
							+ frameIndex);
					image = imageIORead(fileName);
				} else {
					System.out.println("Successfully loaded frame " + frameIndex + " with native I/O");
				}
			} catch (Exception e) {
				System.out.println(
						"Error validating native I/O image, falling back to Icy's imageIORead for frame " + frameIndex);
				image = imageIORead(fileName);
			}
		}

		long memoryAfter = memoryMonitor.getUsedMemoryMB();

		if (image != null) {
			long memoryDelta = memoryAfter - memoryBefore;
			long estimatedFootprint = memoryMonitor.estimateImageMemoryFootprint(image);

			if (memoryDelta > 10 || estimatedFootprint > 1) { // Log if significant memory increase or large
																// footprint
				System.out.println("Frame " + frameIndex + " loaded: " + memoryDelta + "MB memory increase "
						+ "(expected ~0.27MB). Estimated footprint: " + estimatedFootprint + "MB. " + "Total memory: "
						+ memoryAfter + "MB");
			}

			// Force immediate cleanup after loading
			System.gc();
			Thread.yield();

		} else {
			System.err.println("Failed to load image for frame " + frameIndex + " from file: " + fileName);
		}

		return image;
	}

	/**
	 * Nuclear option: Load image using native Java I/O to bypass Icy's memory
	 * issues
	 */
	private IcyBufferedImage loadImageWithNativeIO(String fileName) {
		try {
			// Use Java's native image I/O
			java.io.File file = new java.io.File(fileName);
			if (!file.exists()) {
				return null;
			}

			// Load with Java's ImageIO
			java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(file);
			if (bufferedImage == null) {
				return null;
			}

			// Convert to IcyBufferedImage with proper data type
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();

			// Determine the number of channels based on the original image
			int numChannels = bufferedImage.getColorModel().getNumComponents();
			if (numChannels == 1) {
				// Grayscale image - preserve as single channel
				IcyBufferedImage icyImage = new IcyBufferedImage(width, height, 1, icy.type.DataType.BYTE);

				// Extract grayscale data
				byte[] grayData = new byte[width * height];
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int rgb = bufferedImage.getRGB(x, y);
						// Convert RGB to grayscale using standard luminance formula
						int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF)
								+ 0.114 * (rgb & 0xFF));
						grayData[y * width + x] = (byte) gray;
					}
				}

				icyImage.setDataXY(0, grayData);
				grayData = null;

				// Clear the Java BufferedImage immediately
				bufferedImage.flush();
				bufferedImage = null;

				// Validate the created image
				try {
					Object data = icyImage.getDataXY(0);
					if (data == null) {
						System.err.println("Created grayscale image has null data");
						return null;
					}
				} catch (Exception e) {
					System.err.println("Error validating grayscale image: " + e.getMessage());
					return null;
				}

				return icyImage;

			} else {
				// Color image - preserve as RGB
				IcyBufferedImage icyImage = new IcyBufferedImage(width, height, 3, icy.type.DataType.BYTE);

				// Copy pixel data directly - more efficient approach
				int[] pixels = new int[width * height];
				bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

				// Create data arrays for each channel
				byte[] redData = new byte[width * height];
				byte[] greenData = new byte[width * height];
				byte[] blueData = new byte[width * height];

				for (int i = 0; i < pixels.length; i++) {
					int pixel = pixels[i];
					redData[i] = (byte) ((pixel >> 16) & 0xFF); // Red channel
					greenData[i] = (byte) ((pixel >> 8) & 0xFF); // Green channel
					blueData[i] = (byte) (pixel & 0xFF); // Blue channel
				}

				// Set the data for each channel
				icyImage.setDataXY(0, redData);
				icyImage.setDataXY(1, greenData);
				icyImage.setDataXY(2, blueData);

				// Clear arrays immediately
				pixels = null;
				redData = null;
				greenData = null;
				blueData = null;

				// Clear the Java BufferedImage immediately
				bufferedImage.flush();
				bufferedImage = null;

				// Validate the created image
				try {
					Object data = icyImage.getDataXY(0);
					if (data == null) {
						System.err.println("Created RGB image has null data");
						return null;
					}
				} catch (Exception e) {
					System.err.println("Error validating RGB image: " + e.getMessage());
					return null;
				}

				return icyImage;
			}

		} catch (Exception e) {
			System.out.println("Native I/O failed for " + fileName + ": " + e.getMessage());
			return null;
		}
	}

	private void prefetchImages() {
		// No prefetching - images are loaded on demand only
		System.out.println("Prefetching disabled - using on-demand loading only");

		// Just wait for the processing to complete
		while (running) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	// Placeholder method - this should be implemented to use Icy's image loading
	private IcyBufferedImage imageIORead(String fileName) {
		// This method should be implemented to use Icy's image loading mechanism
		// For now, return null to indicate fallback is needed
		System.out.println("Icy imageIORead not implemented - using fallback");
		return null;
	}
}