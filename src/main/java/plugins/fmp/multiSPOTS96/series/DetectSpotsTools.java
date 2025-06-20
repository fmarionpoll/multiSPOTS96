package plugins.fmp.multiSPOTS96.series;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class DetectSpotsTools {
	// public BuildSeriesOptions options = null;

	// -----------------------------------------------------

	BooleanMask2D[] findBlobs(ROI2DArea roiAll, BooleanMask2D cageMask) throws InterruptedException {
		if (cageMask == null)
			return null;

		ROI2DArea roi = new ROI2DArea(roiAll.getBooleanMask(true).getIntersection(cageMask));

		BooleanMask2D roiBooleanMask = roi.getBooleanMask(true);
		return roiBooleanMask.getComponents();
	}

	public ROI2DArea binarizeImage(IcyBufferedImage img, BuildSeriesOptions options) {
		if (img == null)
			return null;
		boolean[] mask = new boolean[img.getSizeX() * img.getSizeY()];
		if (options.btrackWhite) {
			byte[] arrayRed = img.getDataXYAsByte(0);
			byte[] arrayGreen = img.getDataXYAsByte(1);
			byte[] arrayBlue = img.getDataXYAsByte(2);
			for (int i = 0; i < arrayRed.length; i++) {
				float r = (arrayRed[i] & 0xFF);
				float g = (arrayGreen[i] & 0xFF);
				float b = (arrayBlue[i] & 0xFF);
				float intensity = (r + g + b) / 3f;
				mask[i] = (intensity) > options.threshold;
			}
		} else {
			byte[] arrayChan = img.getDataXYAsByte(options.videoChannel);
			for (int i = 0; i < arrayChan.length; i++)
				mask[i] = (((int) arrayChan[i]) & 0xFF) < options.threshold;
		}
		BooleanMask2D bmask = new BooleanMask2D(img.getBounds(), mask);
		return new ROI2DArea(bmask);
	}

	public void findSpots(Experiment exp, BuildSeriesOptions options, IcyBufferedImage workimage)
			throws InterruptedException {
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		processor.setThreadName("detectFlies");
		processor.setPriority(Processor.NORM_PRIORITY);
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(exp.cagesArray.cagesList.size());
		futures.clear();

		final ROI2DArea binarizedImageRoi = binarizeImage(workimage, options);

		for (Cage cage : exp.cagesArray.cagesList) {
			if (options.detectCage != -1 && cage.prop.cageID != options.detectCage)
				continue;
			cage.spotsArray = new SpotsArray();
			futures.add(processor.submit(new Runnable() {
				@Override
				public void run() {
					int spotID = 0;
					BooleanMask2D[] blobs;
					try {
						blobs = findBlobs(binarizedImageRoi, cage.cageMask2D);
						for (int i = 0; i < blobs.length; i++) {
							int npoints = blobs[i].getNumberOfPoints();
							if (npoints < 2)
								continue;

							List<Point> points;
							try {
								points = blobs[i].getConnectedContourPoints();
								if (points != null) {
									List<Point2D> points2s = points.stream()
											.map(point -> new Point2D.Double(point.getX(), point.getY()))
											.collect(Collectors.toList());
									ROI2DPolygon roi = new ROI2DPolygon(points2s);
									roi.setName("spot_" + spotID);
									Spot spot = new Spot(roi);
									cage.spotsArray.spotsList.add(spot);
									spotID++;
								}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}));
		}
		waitDetectCompletion(processor, futures, null);
		processor.shutdown();
	}

//	public ROI2DArea binarizeInvertedImage(IcyBufferedImage img, int threshold) {
//		if (img == null)
//			return null;
//		boolean[] mask = new boolean[img.getSizeX() * img.getSizeY()];
//		if (options.btrackWhite) {
//			byte[] arrayRed = img.getDataXYAsByte(0);
//			byte[] arrayGreen = img.getDataXYAsByte(1);
//			byte[] arrayBlue = img.getDataXYAsByte(2);
//			for (int i = 0; i < arrayRed.length; i++) {
//				float r = (arrayRed[i] & 0xFF);
//				float g = (arrayGreen[i] & 0xFF);
//				float b = (arrayBlue[i] & 0xFF);
//				float intensity = (r + g + b) / 3f;
//				mask[i] = (intensity < threshold);
//			}
//		} else {
//			byte[] arrayChan = img.getDataXYAsByte(options.videoChannel);
//			for (int i = 0; i < arrayChan.length; i++)
//				mask[i] = (((int) arrayChan[i]) & 0xFF) > threshold;
//		}
//		BooleanMask2D bmask = new BooleanMask2D(img.getBounds(), mask);
//		return new ROI2DArea(bmask);
//	}

//	public void initParametersForDetection(Experiment exp, BuildSeriesOptions options) {
//		this.options = options;
//		exp.cagesArray.detect_nframes = (int) (((exp.cagesArray.detectLast_Ms - exp.cagesArray.detectFirst_Ms)
//				/ exp.cagesArray.detectBin_Ms) + 1);
//		exp.cagesArray.clearAllMeasures(options.detectCage);
//		cages = exp.cagesArray;
//		cages.computeBooleanMasksForCages();
//		rectangleAllCages = null;
//		for (Cage cage : cages.cagesList) {
//			if (cage.prop.cageNFlies < 1)
//				continue;
//			Rectangle rect = cage.getRoi().getBounds();
//			if (rectangleAllCages == null)
//				rectangleAllCages = new Rectangle(rect);
//			else
//				rectangleAllCages.add(rect);
//		}
//	}

	protected void waitDetectCompletion(Processor processor, ArrayList<Future<?>> futuresArray,
			ProgressFrame progressBar) {
		int frame = 1;
		int nframes = futuresArray.size();

		while (!futuresArray.isEmpty()) {
			final Future<?> f = futuresArray.get(futuresArray.size() - 1);
			if (progressBar != null)
				progressBar.setMessage("Analyze frame: " + (frame) + "//" + nframes);
			try {
				f.get();
			} catch (ExecutionException e) {
				System.out
						.println("FlyDetectTools:waitDetectCompletion - frame:" + frame + " Execution exception: " + e);
			} catch (InterruptedException e) {
				System.out.println("FlyDetectTools:waitDetectCompletion - Interrupted exception: " + e);
			}
			futuresArray.remove(f);
			frame++;
		}
	}

}
