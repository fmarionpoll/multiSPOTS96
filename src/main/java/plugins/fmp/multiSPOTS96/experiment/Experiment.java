package plugins.fmp.multiSPOTS96.experiment;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.CagesArray;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.Directories;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;

public class Experiment {
	public final static String RESULTS = "results";
	public final static String BIN = "bin_";

	private String camDataImagesDirectory = null;
	private String resultsDirectory = null;
	private String binSubDirectory = null;

	public SequenceCamData seqCamData = null;
	public SequenceKymos seqKymos = null;
	public Sequence seqReference = null;
	public CagesArray cagesArray = new CagesArray();

	public FileTime firstImage_FileTime;
	public FileTime lastImage_FileTime;

	public ExperimentProperties expProperties = new ExperimentProperties();
	public int col = -1;
	public Experiment chainToPreviousExperiment = null;
	public Experiment chainToNextExperiment = null;
	public long chainImageFirst_ms = 0;
	public int experimentID = 0;

	private final static String ID_VERSION = "version";
	private final static String ID_VERSIONNUM = "1.0.0";
	private final static String ID_TIMEFIRSTIMAGE = "fileTimeImageFirstMinute";
	private final static String ID_TIMELASTIMAGE = "fileTimeImageLastMinute";

	private final static String ID_BINT0 = "indexBinT0";
	private final static String ID_FRAMEFIRST = "indexFrameFirst";
	private final static String ID_NFRAMES = "nFrames";
	private final static String ID_FRAMEDELTA = "indexFrameDelta";

	private final static String ID_TIMEFIRSTIMAGEMS = "fileTimeImageFirstMs";
	private final static String ID_TIMELASTIMAGEMS = "fileTimeImageLastMs";
	private final static String ID_FIRSTKYMOCOLMS = "firstKymoColMs";
	private final static String ID_LASTKYMOCOLMS = "lastKymoColMs";
	private final static String ID_BINKYMOCOLMS = "binKymoColMs";

	private final static String ID_IMAGESDIRECTORY = "imagesDirectory";
	private final static String ID_MCEXPERIMENT = "MCexperiment";
	private final String ID_MS96_experiment_XML = "MS96_experiment.xml";
	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";

	private final static int EXPT_DIRECTORY = 1;
	private final static int IMG_DIRECTORY = 2;
	private final static int BIN_DIRECTORY = 3;
	// ----------------------------------

	public Experiment() {
		seqCamData = new SequenceCamData();
		seqKymos = new SequenceKymos();
	}

	public Experiment(String expDirectory) {
		seqCamData = new SequenceCamData();
		seqKymos = new SequenceKymos();
		this.resultsDirectory = expDirectory;
	}

	public Experiment(SequenceCamData seqCamData) {
		this.seqCamData = seqCamData;
		this.seqKymos = new SequenceKymos();
		resultsDirectory = this.seqCamData.getImagesDirectory() + File.separator + RESULTS;
		getFileIntervalsFromSeqCamData();
		load_MS96_experiment(concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML));
	}

	public Experiment(ExperimentDirectories eADF) {
		camDataImagesDirectory = eADF.getCameraImagesDirectory();
		resultsDirectory = eADF.getResultsDirectory();
		binSubDirectory = eADF.getBinSubDirectory();
		seqCamData = new SequenceCamData();
		String fileName = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
		load_MS96_experiment(fileName);

		seqCamData.imagesDirectory = eADF.getCameraImagesDirectory();
		List<String> imagesList = ExperimentDirectories.getImagesListFromPathV2(seqCamData.imagesDirectory, "jpg");
		seqCamData.loadImageList(imagesList);
		if (eADF.cameraImagesList.size() > 1)
			getFileIntervalsFromSeqCamData();

		if (eADF.kymosImagesList != null && eADF.kymosImagesList.size() > 0) {
			seqKymos = new SequenceKymos(eADF.kymosImagesList);
		}
	}

	// ----------------------------------

	public String getResultsDirectory() {
		return resultsDirectory;
	}

	public String toString() {
		return resultsDirectory;
	}

	public void setResultsDirectory(String fileName) {
		resultsDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
	}

	public String getKymosBinFullDirectory() {
		String filename = resultsDirectory;
		if (binSubDirectory != null)
			filename += File.separator + binSubDirectory;
		return filename;
	}

	public void setBinSubDirectory(String bin) {
		binSubDirectory = bin;
	}

	public String getBinSubDirectory() {
		return binSubDirectory;
	}

	public boolean createDirectoryIfDoesNotExist(String directory) {
		Path pathDir = Paths.get(directory);
		if (Files.notExists(pathDir)) {
			try {
				Files.createDirectory(pathDir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out
						.println("Experiment:createDirectoryIfDoesNotExist() Creating directory failed: " + directory);
				return false;
			}
		}
		return true;
	}

	public void checkKymosDirectory(String kymosSubDirectory) {
		if (kymosSubDirectory == null) {
			List<String> listTIFFlocations = Directories.getSortedListOfSubDirectoriesWithTIFF(getResultsDirectory());
			if (listTIFFlocations.size() < 1)
				return;
			boolean found = false;
			for (String subDir : listTIFFlocations) {
				String test = subDir.toLowerCase();
				if (test.contains(Experiment.BIN)) {
					kymosSubDirectory = subDir;
					found = true;
					break;
				}
				if (test.contains(Experiment.RESULTS)) {
					found = true;
					break;
				}
			}
			if (!found) {
				int lowest = getBinStepFromDirectoryName(listTIFFlocations.get(0)) + 1;
				for (String subDir : listTIFFlocations) {
					int val = getBinStepFromDirectoryName(subDir);
					if (val < lowest) {
						lowest = val;
						kymosSubDirectory = subDir;
					}
				}
			}
		}
		setBinSubDirectory(kymosSubDirectory);
	}

	public void setCameraImagesDirectory(String name) {
		camDataImagesDirectory = name;
	}

	public String getCameraImagesDirectory() {
		return camDataImagesDirectory;
	}

	public void closeSequences() {
		if (seqKymos != null)
			seqKymos.closeSequence();
		if (seqCamData != null)
			seqCamData.closeSequence();
		if (seqReference != null)
			seqReference.close();
	}

	public boolean zopenPositionsMeasures() {
		if (seqCamData == null)
			seqCamData = new SequenceCamData();
		load_MS96_experiment();
		getFileIntervalsFromSeqCamData();

		if (seqKymos == null)
			seqKymos = new SequenceKymos();

		return zxmlReadDrosoTrack(null);
	}

	private String getRootWithNoResultNorBinString(String directoryName) {
		String name = directoryName.toLowerCase();
		while (name.contains(RESULTS) || name.contains(BIN))
			name = Paths.get(resultsDirectory).getParent().toString();
		return name;
	}

	private SequenceCamData loadImagesForSequenceCamData(String filename) {
		camDataImagesDirectory = ExperimentDirectories.getImagesDirectoryAsParentFromFileName(filename);
		List<String> imagesList = ExperimentDirectories.getImagesListFromPathV2(camDataImagesDirectory, "jpg");
		seqCamData = null;
		if (imagesList.size() > 0) {
			seqCamData = new SequenceCamData();
			seqCamData.setImagesList(imagesList);
			seqCamData.attachSequence(seqCamData.loadSequenceFromImagesList(imagesList));
		}
		return seqCamData;
	}

	public boolean loadCamDataSpots() {
		load_MS96_cages();
		if (seqCamData != null && seqCamData.seq != null) {
			seqCamData.removeROIsContainingString("spot");
			cagesArray.transferCageSpotsToSequenceAsROIs(seqCamData);
		}
		return (seqCamData != null && seqCamData.seq != null);
	}

	public SequenceCamData openSequenceCamData() {
		loadImagesForSequenceCamData(camDataImagesDirectory);
		if (seqCamData != null) {
			load_MS96_experiment();
			getFileIntervalsFromSeqCamData();
		}
		return seqCamData;
	}

	public void getFileIntervalsFromSeqCamData() {
		if (seqCamData != null
				&& (seqCamData.firstImage_ms < 0 || seqCamData.lastImage_ms < 0 || seqCamData.binImage_ms < 0)) {
			loadFileIntervalsFromSeqCamData();
		}
	}

	public void loadFileIntervalsFromSeqCamData() {
		if (seqCamData != null) {
			seqCamData.setImagesDirectory(camDataImagesDirectory);
			firstImage_FileTime = seqCamData.getFileTimeFromStructuredName((int) seqCamData.absoluteIndexFirstImage);
			lastImage_FileTime = seqCamData.getFileTimeFromStructuredName(seqCamData.nTotalFrames - 1);
			if (firstImage_FileTime != null && lastImage_FileTime != null) {
				seqCamData.firstImage_ms = firstImage_FileTime.toMillis();
				seqCamData.lastImage_ms = lastImage_FileTime.toMillis();
				if (seqCamData.nTotalFrames > 1)
					seqCamData.binImage_ms = (seqCamData.lastImage_ms - seqCamData.firstImage_ms)
							/ (seqCamData.nTotalFrames - 1);
				if (seqCamData.binImage_ms == 0)
					System.out.println("Experiment:loadFileIntervalsFromSeqCamData() error / file interval size");
			} else {
				System.out.println("Experiment:loadFileIntervalsFromSeqCamData() error / file intervals of "
						+ seqCamData.getImagesDirectory());
			}
		}
	}

	public long[] build_MsTimeIntervalsArray_From_SeqCamData_FileNamesList() {
		int nFrames = seqCamData.imagesList.size();
		if (nFrames != seqCamData.nTotalFrames)
			System.out.println("error: nFrames (seqCamData.camImagesList.size()):" + nFrames
					+ " is different from seqCamData.nTotalFrames:" + seqCamData.nTotalFrames);
		seqCamData.camImages_array_ms = new long[seqCamData.nTotalFrames];

		FileTime firstImage_FileTime = seqCamData.getFileTimeFromStructuredName(0);
		long firstImage_ms = firstImage_FileTime.toMillis();
		for (int i = 0; i < seqCamData.nTotalFrames; i++) {
			FileTime image_FileTime = seqCamData.getFileTimeFromStructuredName(i);
			long image_ms = image_FileTime.toMillis() - firstImage_ms;
			seqCamData.camImages_array_ms[i] = image_ms;
		}
		return seqCamData.camImages_array_ms;
	}

	public int findNearestIntervalWithBinarySearch(long value, int low, int high) {
		int result = -1;
		if (high - low > 1) {
			int mid = (low + high) / 2;

			if (seqCamData.camImages_array_ms[mid] > value)
				result = findNearestIntervalWithBinarySearch(value, low, mid);
			else if (seqCamData.camImages_array_ms[mid] < value)
				result = findNearestIntervalWithBinarySearch(value, mid, high);
			else
				result = mid;
		} else
			result = Math.abs(value - seqCamData.camImages_array_ms[low]) < Math
					.abs(value - seqCamData.camImages_array_ms[high]) ? low : high;

		return result;
	}

	public String getBinNameFromKymoFrameStep() {
		return BIN + seqCamData.binDuration_ms / 1000;
	}

	public String getDirectoryToSaveResults() {
		Path dir = Paths.get(resultsDirectory);
		if (binSubDirectory != null)
			dir = dir.resolve(binSubDirectory);
		String directory = dir.toAbsolutePath().toString();
		if (!createDirectoryIfDoesNotExist(directory))
			directory = null;
		return directory;
	}

	// -------------------------------
	// -------------------------------

	public boolean load_MS96_experiment() {
		if (resultsDirectory == null && seqCamData != null) {
			camDataImagesDirectory = seqCamData.getImagesDirectory();
			resultsDirectory = camDataImagesDirectory + File.separator + RESULTS;
		}
		String csFileName = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
		return load_MS96_experiment(csFileName);
	}

	private boolean load_MS96_experiment(String csFileName) {
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc == null)
			return false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
		if (node == null)
			return false;

		String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
		if (!version.equals(ID_VERSIONNUM))
			return false;

		seqCamData.firstImage_ms = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGEMS, 0);
		seqCamData.lastImage_ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGEMS, 0);
		if (seqCamData.lastImage_ms <= 0) {
			seqCamData.firstImage_ms = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, 0) * 60000;
			seqCamData.lastImage_ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, 0) * 60000;
		}

		seqCamData.absoluteIndexFirstImage = XMLUtil.getElementLongValue(node, ID_FRAMEFIRST, -1);
		if (seqCamData.absoluteIndexFirstImage < 0)
			seqCamData.absoluteIndexFirstImage = XMLUtil.getElementLongValue(node, ID_BINT0, -1);
		if (seqCamData.absoluteIndexFirstImage < 0)
			seqCamData.absoluteIndexFirstImage = 0;
		seqCamData.fixedNumberOfImages = XMLUtil.getElementLongValue(node, ID_NFRAMES, -1);
		seqCamData.deltaImage = XMLUtil.getElementLongValue(node, ID_FRAMEDELTA, 1);
		seqCamData.binFirst_ms = XMLUtil.getElementLongValue(node, ID_FIRSTKYMOCOLMS, -1);
		seqCamData.binLast_ms = XMLUtil.getElementLongValue(node, ID_LASTKYMOCOLMS, -1);
		seqCamData.binDuration_ms = XMLUtil.getElementLongValue(node, ID_BINKYMOCOLMS, -1);

		ugly_checkOffsetValues();
		expProperties.loadXML_Properties(node);
		return true;
	}

	public boolean save_MS96_experiment() {
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null) {
			Node xmlRoot = XMLUtil.getRootElement(doc, true);
			Node node = XMLUtil.setElement(xmlRoot, ID_MCEXPERIMENT);
			if (node == null)
				return false;

			XMLUtil.setElementValue(node, ID_VERSION, ID_VERSIONNUM);
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGEMS, seqCamData.firstImage_ms);
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGEMS, seqCamData.lastImage_ms);

			XMLUtil.setElementLongValue(node, ID_FRAMEFIRST, seqCamData.absoluteIndexFirstImage);
			XMLUtil.setElementLongValue(node, ID_BINT0, seqCamData.absoluteIndexFirstImage);
			XMLUtil.setElementLongValue(node, ID_NFRAMES, seqCamData.fixedNumberOfImages);
			XMLUtil.setElementLongValue(node, ID_FRAMEDELTA, seqCamData.deltaImage);

			XMLUtil.setElementLongValue(node, ID_FIRSTKYMOCOLMS, seqCamData.binFirst_ms);
			XMLUtil.setElementLongValue(node, ID_LASTKYMOCOLMS, seqCamData.binLast_ms);
			XMLUtil.setElementLongValue(node, ID_BINKYMOCOLMS, seqCamData.binDuration_ms);

			expProperties.saveXML_Properties(node);

			if (camDataImagesDirectory == null)
				camDataImagesDirectory = seqCamData.getImagesDirectory();
			XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, camDataImagesDirectory);

			String tempname = concatenateExptDirectoryWithSubpathAndName(null, ID_MS96_experiment_XML);
			return XMLUtil.saveDocument(doc, tempname);
		}
		return false;
	}

	private void ugly_checkOffsetValues() {
		if (seqCamData.firstImage_ms < 0)
			seqCamData.firstImage_ms = 0;
		if (seqCamData.lastImage_ms < 0)
			seqCamData.lastImage_ms = 0;
		if (seqCamData.binFirst_ms < 0)
			seqCamData.binFirst_ms = 0;
		if (seqCamData.binLast_ms < 0)
			seqCamData.binLast_ms = 0;
		if (seqCamData.binDuration_ms < 0)
			seqCamData.binDuration_ms = 60000;
	}

	// -------------------------------

	private String getXML_MS96_cages_Location(String XMLfileName) {
		String fileName = findFile_3Locations(XMLfileName, EXPT_DIRECTORY, BIN_DIRECTORY, IMG_DIRECTORY);
		if (fileName == null)
			fileName = concatenateExptDirectoryWithSubpathAndName(null, XMLfileName);
		return fileName;
	}

	public boolean load_MS96_cages() {
		String fileName = getXML_MS96_cages_Location(cagesArray.ID_MS96_cages_XML);
		return cagesArray.xmlReadCagesFromFileNoQuestion(fileName, this);
	}

	public boolean save_MS96_cages() {
		String fileName = getXML_MS96_cages_Location(cagesArray.ID_MS96_cages_XML);
		return cagesArray.xmlWriteCagesToFileNoQuestion(fileName);
	}

	// -------------------------------

	public boolean load_MS96_spotsMeasures() {
		return cagesArray.load_SpotsMeasures(getResultsDirectory());
	}

	public boolean save_MS96_spotsMeasures() {
		return cagesArray.save_SpotsMeasures(getResultsDirectory());
	}

	public boolean load_MS96_fliesPositions() {
		return false;
	}

	public boolean save_MS96_fliesPositions() {
		return false;
	}

	public boolean load_MS96_kymographs() {
		return false;
	}

	public boolean save_MS96_kymographs() {
		return false;
	}

	// -------------------------------

	final String csvSep = ";";

//	private boolean csvSave_DescriptionSection(FileWriter csvWriter) {
//		try {
//			csvWriter.append(expProperties.csvExportExperimentSectionHeader(csvSep));
//			csvWriter.append(expProperties.csvExportExperimentProperties(csvSep));
//			csvWriter.append("#" + csvSep + "#\n");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return true;
//	}

	public boolean zloadKymographs() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		List<ImageFileDescriptor> myList = seqKymos.loadListOfPotentialKymographsFromSpots(getKymosBinFullDirectory(),
				cagesArray);
		ImageFileDescriptor.getExistingFileNames(myList);
		return seqKymos.loadKymographImagesFromList(myList, true);
	}

	// ------------------------------------------------

	public Experiment getFirstChainedExperiment(boolean globalValue) {
		Experiment exp = this;
		if (globalValue && chainToPreviousExperiment != null)
			exp = chainToPreviousExperiment.getFirstChainedExperiment(globalValue);
		return exp;
	}

	public Experiment getLastChainedExperiment(boolean globalValue) {
		Experiment exp = this;
		if (globalValue && chainToNextExperiment != null)
			exp = chainToNextExperiment.getLastChainedExperiment(globalValue);
		return exp;
	}

	public void setFileTimeImageFirst(FileTime fileTimeImageFirst) {
		this.firstImage_FileTime = fileTimeImageFirst;
	}

	public void setFileTimeImageLast(FileTime fileTimeImageLast) {
		this.lastImage_FileTime = fileTimeImageLast;
	}

	public void getFieldValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		switch (fieldEnumCode) {
		case EXP_STIM:
		case EXP_CONC:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_COND1:
		case EXP_COND2:
			addValue(expProperties.getExperimentField(fieldEnumCode), textList);
			break;
		case SPOT_STIM:
		case SPOT_CONC:
			addSpotsValues(fieldEnumCode, textList);
			break;
		default:
			break;
		}
	}

	public boolean replaceExperimentFieldIfEqualOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		boolean flag = expProperties.getExperimentField(fieldEnumCode).equals(oldValue);
		if (flag) {
			expProperties.setExperimentFieldNoTest(fieldEnumCode, newValue);
		}
		return flag;
	}

	public void replaceFieldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue, String newValue) {
		switch (fieldEnumCode) {
		case EXP_STIM:
		case EXP_CONC:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
		case EXP_COND1:
		case EXP_COND2:
			replaceExperimentFieldIfEqualOld(fieldEnumCode, oldValue, newValue);
			break;
		case SPOT_STIM:
		case SPOT_CONC:
			if (replaceSpotsFieldValueWithNewValueIfOld(fieldEnumCode, oldValue, newValue))
				save_MS96_cages();
			break;
		default:
			break;
		}
	}

	// --------------------------------------------

	public boolean loadReferenceImage() {
		BufferedImage image = null;
		File inputfile = new File(getReferenceImageFullName());
		boolean exists = inputfile.exists();
		if (!exists)
			return false;
		image = ImageUtil.load(inputfile, true);
		if (image == null) {
			System.out.println("Experiment:loadReferenceImage() image not loaded / not found");
			return false;
		}
		seqCamData.refImage = IcyBufferedImage.createFrom(image);
		seqReference = new Sequence(seqCamData.refImage);
		seqReference.setName("referenceImage");
		return true;
	}

	public boolean saveReferenceImage(IcyBufferedImage referenceImage) {
		File outputfile = new File(getReferenceImageFullName());
		RenderedImage image = ImageUtil.toRGBImage(referenceImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}

	public void cleanPreviousDetectedFliesROIs() {
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : list) {
			if (roi.getName().contains("det"))
				seqCamData.seq.removeROI(roi);
		}
	}

	public String zgetMCDrosoTrackFullName() {
		return resultsDirectory + File.separator + ID_MCDROSOTRACK_XML;
	}

	public void updateROIsAt(int t) {
		seqCamData.seq.beginUpdate();
		List<ROI2D> rois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi : rois) {
			if (roi.getName().contains("det"))
				seqCamData.seq.removeROI(roi);
		}
		seqCamData.seq.addROIs(cagesArray.getPositionsAsListOfROI2DRectanglesAtT(t), false);
		seqCamData.seq.endUpdate();
	}

	public void saveDetRoisToPositions() {
		List<ROI2D> detectedROIsList = seqCamData.seq.getROI2Ds();
		for (Cage cage : cagesArray.cagesList) {
			cage.transferRoisToPositions(detectedROIsList);
		}
	}

	// ----------------------------------

	private int getBinStepFromDirectoryName(String resultsPath) {
		int step = -1;
		if (resultsPath.contains(BIN)) {
			if (resultsPath.length() < (BIN.length() + 1))
				step = (int) seqCamData.binDuration_ms;
			else
				step = Integer.valueOf(resultsPath.substring(BIN.length())) * 1000;
		}
		return step;
	}

	private boolean zxmlReadDrosoTrack(String filename) {
		if (filename == null) {
			filename = getXML_MS96_cages_Location(cagesArray.ID_MS96_cages_XML);
			if (filename == null)
				return false;
		}
		return cagesArray.xmlReadCagesFromFileNoQuestion(filename, this);
	}

	private String findFile_3Locations(String xmlFileName, int first, int second, int third) {
		// current directory
		String xmlFullFileName = findFile_1Location(xmlFileName, first);
		if (xmlFullFileName == null)
			xmlFullFileName = findFile_1Location(xmlFileName, second);
		if (xmlFullFileName == null)
			xmlFullFileName = findFile_1Location(xmlFileName, third);
		return xmlFullFileName;
	}

	private String findFile_1Location(String xmlFileName, int item) {
		String xmlFullFileName = File.separator + xmlFileName;
		switch (item) {
		case IMG_DIRECTORY:
			camDataImagesDirectory = getRootWithNoResultNorBinString(resultsDirectory);
			xmlFullFileName = camDataImagesDirectory + File.separator + xmlFileName;
			break;

		case BIN_DIRECTORY:
			// any directory (below)
			Path dirPath = Paths.get(resultsDirectory);
			List<Path> subFolders = Directories.getAllSubPathsOfDirectory(resultsDirectory, 1);
			if (subFolders == null)
				return null;
			List<String> resultsDirList = Directories.getPathsContainingString(subFolders, RESULTS);
			List<String> binDirList = Directories.getPathsContainingString(subFolders, BIN);
			resultsDirList.addAll(binDirList);
			for (String resultsSub : resultsDirList) {
				Path dir = dirPath.resolve(resultsSub + File.separator + xmlFileName);
				if (Files.notExists(dir))
					continue;
				xmlFullFileName = dir.toAbsolutePath().toString();
				break;
			}
			break;

		case EXPT_DIRECTORY:
		default:
			xmlFullFileName = resultsDirectory + xmlFullFileName;
			break;
		}

		// current directory
		if (xmlFullFileName != null && fileExists(xmlFullFileName)) {
			if (item == IMG_DIRECTORY) {
				camDataImagesDirectory = getRootWithNoResultNorBinString(resultsDirectory);
				ExperimentDirectories.moveAndRename(xmlFileName, camDataImagesDirectory, xmlFileName, resultsDirectory);
				xmlFullFileName = resultsDirectory + xmlFullFileName;
			}
			return xmlFullFileName;
		}
		return null;
	}

	private boolean fileExists(String fileName) {
		File f = new File(fileName);
		return (f.exists() && !f.isDirectory());
	}

	private boolean replaceSpotsFieldValueWithNewValueIfOld(EnumXLSColumnHeader fieldEnumCode, String oldValue,
			String newValue) {
		if (cagesArray.cagesList.size() == 0)
			load_MS96_cages();
		boolean flag = false;
		for (Cage cage : cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getSpotField(fieldEnumCode).equals(oldValue)) {
					spot.setSpotField(fieldEnumCode, newValue);
					flag = true;
				}
			}
		}
		return flag;
	}

	private String concatenateExptDirectoryWithSubpathAndName(String subpath, String name) {
		if (subpath != null)
			return resultsDirectory + File.separator + subpath + File.separator + name;
		else
			return resultsDirectory + File.separator + name;
	}

	private void addSpotsValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList) {
		if (cagesArray.cagesList.size() == 0)
			load_MS96_cages();
		for (Cage cage : cagesArray.cagesList)
			for (Spot spot : cage.spotsArray.spotsList)
				addValue(spot.getSpotField(fieldEnumCode), textList);
	}

	private void addValue(String text, List<String> textList) {
		if (!isFound(text, textList))
			textList.add(text);
	}

	private boolean isFound(String pattern, List<String> names) {
		boolean found = false;
		if (names.size() > 0) {
			for (String name : names) {
				found = name.equals(pattern);
				if (found)
					break;
			}
		}
		return found;
	}

	private String getReferenceImageFullName() {
		return resultsDirectory + File.separator + "referenceImage.jpg";
	}

}
