package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.sequence.ImageLoader;
import plugins.fmp.multiSPOTS96.experiment.sequence.TimeManager;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;

/**
 * Legacy Excel export class providing core functionality for Excel data export.
 * This class is being refactored to use the new XLSExportBase template method
 * pattern.
 * 
 * <p>
 * Provides functionality for:
 * <ul>
 * <li>Workbook initialization and styling</li>
 * <li>Sheet creation and management</li>
 * <li>Data writing and formatting</li>
 * <li>Time interval calculations</li>
 * <li>Experiment data processing</li>
 * </ul>
 * 
 * <p>
 * This class uses Apache POI for Excel file generation and supports both
 * standard and streaming workbook formats for large datasets.
 * 
 * @author MultiSPOTS96 Team
 * @version 2.0
 * @since 1.0
 * @deprecated Use {@link XLSExportBase} and its subclasses for new
 *             implementations
 */
@Deprecated
public class XLSExport {
	protected XLSExportOptions options = null;
	protected Experiment expAll = null;

	CellStyle xssfCellStyle_red = null;
	CellStyle xssfCellStyle_blue = null;
	Font font_red = null;
	Font font_blue = null;
	SXSSFWorkbook workbook = null;

	JComboBoxExperiment expList = null;

	// ------------------------------------------------

	/**
	 * Writes the top row descriptors to the Excel sheet. Creates column headers for
	 * all experiment metadata fields.
	 * 
	 * @param sheet The Excel sheet to write to
	 * @return The next available row number after descriptors
	 */
	int writeTopRow_descriptors(SXSSFSheet sheet) {
		Point pt = new Point(0, 0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLSColumnHeader dumb : EnumXLSColumnHeader.values()) {
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol + 1;
		return pt.y;
	}

	void writeTopRow_timeIntervals(SXSSFSheet sheet, int row, EnumXLSExport xlsExport) {
		writeTopRow_timeIntervals_Default(sheet, row);
	}

	void writeTopRow_timeIntervals_Default(SXSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long duration = expAll.seqCamData.getLastImageMs() - expAll.seqCamData.getFirstImageMs();
		long interval = 0;
		while (interval < duration) {
			int i = (int) (interval / options.buildExcelUnitMs);
			XLSUtils.setValue(sheet, pt, transpose, "t" + i);
			pt.y++;
			interval += options.buildExcelStepMs;
		}
	}

	/**
	 * Initializes an Excel workbook with default settings and styles. Creates
	 * standard cell styles for red and blue text formatting.
	 * 
	 * @return A configured SXSSFWorkbook instance ready for data writing
	 */
	SXSSFWorkbook xlsInitWorkbook() {
		SXSSFWorkbook workbook = new SXSSFWorkbook();
		workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
		xssfCellStyle_red = workbook.createCellStyle();
		font_red = workbook.createFont();
		font_red.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
		xssfCellStyle_red.setFont(font_red);

		xssfCellStyle_blue = workbook.createCellStyle();
		font_blue = workbook.createFont();
		font_blue.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
		xssfCellStyle_blue.setFont(font_blue);
		return workbook;
	}

	SXSSFSheet xlsGetSheet(String title, EnumXLSExport xlsExport) {
		SXSSFSheet sheet = workbook.getSheet(title);
		if (sheet == null) {
			sheet = workbook.createSheet(title);
			int row = writeTopRow_descriptors(sheet);
			writeTopRow_timeIntervals(sheet, row, xlsExport);
		}
		return sheet;
	}

	private void exportError(Experiment exp, int nOutputFrames) {
		String error = "XLSExport:ExportError() ERROR in " + exp.getResultsDirectory() + "\n nOutputFrames="
				+ nOutputFrames + " kymoFirstCol_Ms=" + exp.seqCamData.getTimeManager().getBinFirst_ms()
				+ " kymoLastCol_Ms=" + exp.seqCamData.getTimeManager().getBinLast_ms();
		System.out.println(error);
	}

	/**
	 * Calculates the number of output frames for an experiment. This method handles
	 * cases where timing data might be incomplete or invalid.
	 * 
	 * <p>
	 * The calculation follows this logic:
	 * <ol>
	 * <li>Calculate based on time duration and step size</li>
	 * <li>If result is invalid, load kymographs and recalculate</li>
	 * <li>If still invalid, fall back to total frame count</li>
	 * </ol>
	 * 
	 * @param exp The experiment to analyze
	 * @return The number of output frames, always > 0
	 */
	protected int getNOutputFrames(Experiment exp) {
		TimeManager timeManager = exp.seqCamData.getTimeManager();
		ImageLoader imgLoader = exp.seqCamData.getImageLoader();
		long durationMs = timeManager.getBinLast_ms() - timeManager.getBinFirst_ms();
		int nOutputFrames = (int) (durationMs / options.buildExcelStepMs + 1);
		if (nOutputFrames <= 1) {
			if (exp.seqKymos != null && exp.seqKymos.getKymographInfo().getMaxWidth() == 0)
				exp.zloadKymographs();

			long binLastMs = timeManager.getBinFirst_ms()
					+ imgLoader.getNTotalFrames() * timeManager.getBinDurationMs();
			timeManager.setBinLast_ms(binLastMs);
			if (binLastMs <= 0)
				exportError(exp, -1);
			nOutputFrames = (int) ((binLastMs - timeManager.getBinFirst_ms()) / options.buildExcelStepMs + 1);
			if (nOutputFrames <= 1) {
				nOutputFrames = imgLoader.getNTotalFrames();
				exportError(exp, nOutputFrames);
			}
		}
		return nOutputFrames;
	}

	XLSResults getSpotResults(Experiment exp, Cage cage, Spot spot, EnumXLSExport xlsExportType) {
		int nOutputFrames = getNOutputFrames(exp);
		XLSResults xlsResults = new XLSResults(cage.getProperties(), spot.getProperties(), nOutputFrames);
		xlsResults.dataValues = (ArrayList<Double>) spot.getMeasuresForExcelPass1(xlsExportType,
				exp.seqCamData.getTimeManager().getBinDurationMs(), options.buildExcelStepMs);
		if (options.relativeToT0 && xlsExportType != EnumXLSExport.AREA_FLYPRESENT)
			xlsResults.relativeToMaximum(); // relativeToT0();
		return xlsResults;
	}

	void writeXLSResult(SXSSFSheet sheet, Point pt, XLSResults xlsResult) {
		boolean transpose = options.transpose;
		if (xlsResult.valuesOut == null)
			return;

		for (long coltime = expAll.seqCamData.getFirstImageMs(); coltime < expAll.seqCamData
				.getLastImageMs(); coltime += options.buildExcelStepMs, pt.y++) {
			int i_from = (int) ((coltime - expAll.seqCamData.getFirstImageMs()) / options.buildExcelStepMs);
			if (i_from >= xlsResult.valuesOut.length)
				break;
			double value = xlsResult.valuesOut[i_from];
			if (!Double.isNaN(value)) {
				XLSUtils.setValue(sheet, pt, transpose, value);
				if (i_from < xlsResult.padded_out.length && xlsResult.padded_out[i_from])
					XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
			}
		}
	}

	protected Point writeExperiment_separator(SXSSFSheet sheet, Point pt) {
		boolean transpose = options.transpose;
		XLSUtils.setValue(sheet, pt, transpose, "--");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "--");
		pt.x++;
		return pt;
	}

	/**
	 * Writes comprehensive experiment and spot information to the Excel sheet. This
	 * method writes all metadata fields for a given experiment, cage, and spot
	 * combination.
	 * 
	 * <p>
	 * The information written includes:
	 * <ul>
	 * <li>File path and date information</li>
	 * <li>Camera identifier extracted from filename</li>
	 * <li>Experiment properties (box ID, experiment type, stimuli, conditions)</li>
	 * <li>Spot properties (volume, pixels, position, stimulus, concentration)</li>
	 * <li>Cage properties (ID, row, column, fly count, strain, sex, age,
	 * comments)</li>
	 * </ul>
	 * 
	 * @param sheet         The Excel sheet to write to
	 * @param pt            The starting point for writing data
	 * @param exp           The experiment containing the data
	 * @param charSeries    The series character identifier for this experiment
	 * @param cage          The cage containing the spot
	 * @param spot          The spot data to write
	 * @param xlsExportType The type of export being performed
	 * @return The updated point after writing all information
	 */
	protected Point writeExperiment_spot_infos(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries, Cage cage,
			Spot spot, EnumXLSExport xlsExportType) {
		int x = pt.x;
		int y = pt.y;
		boolean transpose = options.transpose;
		String filename = exp.getResultsDirectory();
		if (filename == null)
			filename = exp.seqCamData.getImagesDirectory();
		Path path = Paths.get(filename);
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String date = df.format(exp.chainImageFirst_ms);
		String name0 = path.toString();

		int pos = name0.indexOf("cam");
		String cam = "-";
		if (pos > 0) {
			int pos5 = pos + 5;
			if (pos5 >= name0.length())
				pos5 = name0.length() - 1;
			cam = name0.substring(pos, pos5);
		}

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.PATH.getValue(), transpose, name0);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DATE.getValue(), transpose, date);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAM.getValue(), transpose, cam);

		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_BOXID);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_EXPT);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_STIM);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_CONC);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_STRAIN);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_SEX);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_COND1);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.getProperties(), EnumXLSColumnHeader.EXP_COND2);

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_VOLUME.getValue(), transpose,
				spot.getProperties().getSpotVolume());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_PIXELS.getValue(), transpose,
				spot.getProperties().getSpotNPixels());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEPOS.getValue(), transpose,
				spot.getCagePosition(xlsExportType));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_STIM.getValue(), transpose,
				spot.getProperties().getStimulus());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CONC.getValue(), transpose,
				spot.getProperties().getConcentration());

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CAGEID.getValue(), transpose,
				spot.getProperties().getCageID());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CAGEROW.getValue(), transpose,
				spot.getProperties().getCageRow());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CAGECOL.getValue(), transpose,
				spot.getProperties().getCageColumn());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose,
				charSeries + spot.getProperties().getCageID());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_NFLIES.getValue(), transpose,
				cage.getProperties().getCageNFlies());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CHOICE_NOCHOICE.getValue(), transpose, "");
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose,
				cage.getProperties().getFlyStrain());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose,
				cage.getProperties().getFlySex());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose,
				cage.getProperties().getFlyAge());
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose,
				cage.getProperties().getComment());
//		String sheetName = sheet.getSheetName();
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose,
				spot.getProperties().getStimulusI());

		pt.y = y + EnumXLSColumnHeader.DUM4.getValue() + 1;
		return pt;
	}

}
