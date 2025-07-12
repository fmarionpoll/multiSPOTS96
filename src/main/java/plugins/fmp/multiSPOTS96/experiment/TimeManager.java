package plugins.fmp.multiSPOTS96.experiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class TimeManager {
	private static final Logger LOGGER = Logger.getLogger(TimeManager.class.getName());

	private int indexTimePattern = -1;

	private long firstImage_ms = 0;
	private long lastImage_ms = 0;
	private long duration_ms = 0;
	private long binImage_ms = 0;
	private long[] camImages_array_ms = null;

	private long binFirst_ms = 0;
	private long binLast_ms = 0;
	private long binDuration_ms = 60000;

	private long deltaImage;

	private FileNameTimePattern[] timePatternArray = new FileNameTimePattern[] { new FileNameTimePattern(),
			new FileNameTimePattern("yyyy-MM-dd_HH-mm-ss", "\\d{4}-\\d{2}-\\d{2}_\\d{2}\\-\\d{2}\\-\\d{2}"),
			new FileNameTimePattern("yy-MM-dd_HH-mm-ss", "\\d{2}-\\d{2}-\\d{2}_\\d{2}\\-\\d{2}\\-\\d{2}"),
			new FileNameTimePattern("yy.MM.dd_HH.mm.ss", "\\d{2}.\\d{2}.\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}") };

	public TimeManager() {
	}

	public FileTime getFileTimeFromStructuredName(ImageLoader imageLoader, int t) {
		long timeInMs = 0;
		String fileName = fileComponent(imageLoader.getFileNameFromImageList(t));

		if (fileName == null) {
			timeInMs = timePatternArray[0].getDummyTime(t);
		} else {
			if (indexTimePattern < 0) {
				indexTimePattern = findProperFilterIfAny(fileName);
			}
			FileNameTimePattern tp = timePatternArray[indexTimePattern];
			timeInMs = tp.getTimeFromString(fileName, t);
		}

		return FileTime.fromMillis(timeInMs);
	}

	public FileTime getFileTimeFromFileAttributes(ImageLoader imageLoader, int t) {
		String filename = imageLoader.getFileNameFromImageList(t);
		if (filename == null) {
			LOGGER.warning("Null filename for index " + t);
			return null;
		}

		FileTime filetime = null;
		try {
			File file = new File(filename);
			Path filePath = file.toPath();
			BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
			long milliseconds = attributes.creationTime().to(TimeUnit.MILLISECONDS);
			if ((milliseconds > Long.MIN_VALUE) && (milliseconds < Long.MAX_VALUE)) {
				Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
				filetime = FileTime.fromMillis(creationDate.getTime());
			}
		} catch (IOException e) {
			LOGGER.warning("Failed to get file attributes: " + e.getMessage());
		}
		return filetime;
	}

	public FileTime getFileTimeFromJPEGMetaData(ImageLoader imageLoader, int t) {
		String filename = imageLoader.getFileNameFromImageList(t);
		if (filename == null) {
			LOGGER.warning("Null filename for index " + t);
			return null;
		}

		FileTime filetime = null;
		try {
			File file = new File(filename);
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

			if (directory != null && directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
				Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
				filetime = FileTime.fromMillis(date.getTime());
			} else {
				LOGGER.warning("EXIF data not found in file: " + filename);
			}
		} catch (ImageProcessingException e) {
			LOGGER.warning("Image processing error: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.warning("IO error reading metadata: " + e.getMessage());
		} catch (Exception e) {
			LOGGER.warning("Error reading JPEG metadata: " + e.getMessage());
		}
		return filetime;
	}

	private int findProperFilterIfAny(String fileName) {
		for (int i = 1; i < timePatternArray.length; i++) {
			if (timePatternArray[i].findMatch(fileName))
				return i;
		}
		return 0; // Default pattern
	}

	private String fileComponent(String fname) {
		if (fname == null) {
			return null;
		}

		int pos = fname.lastIndexOf("/");
		if (pos > -1) {
			return fname.substring(pos + 1);
		}

		pos = fname.lastIndexOf("\\");
		if (pos > -1) {
			return fname.substring(pos + 1);
		}

		return fname;
	}

	// Getters and setters
	public long getBinImage_ms() {
		return binImage_ms;
	}

	public void setBinImage_ms(long value) {
		binImage_ms = value;
	}

	public long getBinFirst_ms() {
		return binFirst_ms;
	}

	public void setBinFirst_ms(long time_ms) {
		binFirst_ms = time_ms;
	}

	public long getBinLast_ms() {
		return binLast_ms;
	}

	public void setBinLast_ms(long time_ms) {
		binLast_ms = time_ms;
	}

	public long getFirstImageMs() {
		return firstImage_ms;
	}

	public void setFirstImageMs(long timeMs) {
		this.firstImage_ms = timeMs;
	}

	public long getLastImageMs() {
		return lastImage_ms;
	}

	public void setLastImageMs(long timeMs) {
		this.lastImage_ms = timeMs;
	}

	public long getDurationMs() {
		return duration_ms;
	}

	public void setDurationMs(long timeMs) {
		this.duration_ms = timeMs;
	}

	public long getBinDurationMs() {
		return binDuration_ms;
	}

	public void setBinDurationMs(long durationMs) {
		this.binDuration_ms = durationMs;
	}

	public long[] getCamImagesArrayMs() {
		return camImages_array_ms;
	}

	public void setCamImagesArrayMs(long[] imagesArray) {
		this.camImages_array_ms = imagesArray;
	}

	public long getDeltaImage() {
		return deltaImage;
	}

	public void setDeltaImage(long value) {
		deltaImage = value;
	}
}