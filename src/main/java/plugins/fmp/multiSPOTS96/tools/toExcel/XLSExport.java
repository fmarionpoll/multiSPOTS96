package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;

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

	private void exportError(Experiment expi, int nOutputFrames) {
		String error = "XLSExport:ExportError() ERROR in " + expi.getResultsDirectory() + "\n nOutputFrames="
				+ nOutputFrames + " kymoFirstCol_Ms=" + expi.seqCamData.getTimeManager().getBinFirst_ms()
				+ " kymoLastCol_Ms=" + expi.seqCamData.getTimeManager().getBinLast_ms();
		System.out.println(error);
	}

	protected int getNOutputFrames(Experiment expi) {
		int nOutputFrames = (int) ((expi.seqCamData.getTimeManager().getBinLast_ms()
				- expi.seqCamData.getTimeManager().getBinFirst_ms()) / options.buildExcelStepMs + 1);
		if (nOutputFrames <= 1) {
			if (expi.seqKymos.imageWidthMax == 0)
				expi.zloadKymographs();
			expi.seqCamData.getTimeManager().setBinLast_ms(expi.seqCamData.getTimeManager().getBinFirst_ms()
					+ expi.seqKymos.imageWidthMax * expi.seqCamData.getTimeManager().getBinDurationMs());
			if (expi.seqCamData.getTimeManager().getBinLast_ms() <= 0)
				exportError(expi, -1);
			nOutputFrames = (int) ((expi.seqCamData.getTimeManager().getBinLast_ms()
					- expi.seqCamData.getTimeManager().getBinFirst_ms()) / options.buildExcelStepMs + 1);
			if (nOutputFrames <= 1) {
				nOutputFrames = expi.seqCamData.getImageLoader().getNTotalFrames();
				exportError(expi, nOutputFrames);
			}
		}
		return nOutputFrames;
	}

	XLSResults getSpotResults(Experiment exp, Cage cage, Spot spot, EnumXLSExport xlsExportType) {
		int nOutputFrames = getNOutputFrames(exp);
		XLSResults xlsResults = new XLSResults(cage, spot, xlsExportType, nOutputFrames);
		xlsResults.dataValues = spot.getSpotMeasuresForXLSPass1(xlsExportType, exp.seqCamData.getTimeManager().getBinDurationMs(),
				options.buildExcelStepMs);
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

		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_BOXID);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_EXPT);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_STIM);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_CONC);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_STRAIN);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_SEX);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_COND1);
		XLSUtils.setFieldValue(sheet, x, y, transpose, exp.prop, EnumXLSColumnHeader.EXP_COND2);

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_VOLUME.getValue(), transpose, spot.prop.spotVolume);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_PIXELS.getValue(), transpose, spot.prop.spotNPixels);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEPOS.getValue(), transpose,
				spot.getCagePosition(xlsExportType));
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_STIM.getValue(), transpose, spot.prop.stimulus);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CONC.getValue(), transpose, spot.prop.concentration);

		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CAGEID.getValue(), transpose, spot.prop.cageID);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CAGEROW.getValue(), transpose, spot.prop.cageRow);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_CAGECOL.getValue(), transpose, spot.prop.cageColumn);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGEID.getValue(), transpose,
				charSeries + spot.prop.cageID);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.SPOT_NFLIES.getValue(), transpose, cage.prop.cageNFlies);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CHOICE_NOCHOICE.getValue(), transpose, "");
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_STRAIN.getValue(), transpose, cage.prop.flyStrain);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_SEX.getValue(), transpose, cage.prop.flySex);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_AGE.getValue(), transpose, cage.prop.flyAge);
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.CAGE_COMMENT.getValue(), transpose, cage.prop.comment);
//		String sheetName = sheet.getSheetName();
		XLSUtils.setValue(sheet, x, y + EnumXLSColumnHeader.DUM4.getValue(), transpose, spot.prop.stimulus_i);

		pt.y = y + EnumXLSColumnHeader.DUM4.getValue() + 1;
		return pt;
	}

}
