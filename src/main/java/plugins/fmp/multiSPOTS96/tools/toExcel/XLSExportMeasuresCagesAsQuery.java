package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.toExcel.exceptions.ExcelDataException;
import plugins.fmp.multiSPOTS96.tools.toExcel.exceptions.ExcelExportException;
import plugins.fmp.multiSPOTS96.tools.toExcel.exceptions.ExcelResourceException;

public class XLSExportMeasuresCagesAsQuery extends XLSExport {
	ArrayList<EnumXLS_QColumnHeader> headers = new ArrayList<EnumXLS_QColumnHeader>();

	public void exportQToFile(String filename, XLSExportOptions options) throws ExcelExportException {
		System.out.println("XLSExportBase:exportQToFile() - " + ExcelExportConstants.EXPORT_START_MESSAGE);

		this.options = options;
		this.expList = options.expList;

		try (ExcelResourceManager resourceManager = new ExcelResourceManager(filename)) {
			this.resourceManager = resourceManager;
			this.redCellStyle = resourceManager.getRedCellStyle();
			this.blueCellStyle = resourceManager.getBlueCellStyle();

			// Execute method steps
			prepareQExperiments();
			validateExportParameters();
			executeExportQ();

			// Save and close
			resourceManager.saveAndClose();

		} catch (ExcelResourceException e) {
			throw new ExcelExportException("Resource management failed during export", "export_to_file", filename, e);
		} catch (Exception e) {
			throw new ExcelExportException("Unexpected error during export", "export_to_file", filename, e);
		} finally {
			cleanup();
		}

		System.out.println("XLSExport:exportToFile() - " + ExcelExportConstants.EXPORT_FINISH_MESSAGE);
	}

	/**
	 * Executes the export process with progress tracking.
	 * 
	 * @throws ExcelExportException If export execution fails
	 */
	protected void executeExportQ() throws ExcelExportException {
		int nbexpts = expList.getItemCount();
		initHeadersArray();
		ProgressFrame progress = new ProgressFrame(ExcelExportConstants.DEFAULT_PROGRESS_TITLE);

		try {
			progress.setLength(nbexpts);
			int column = 1;
			int iSeries = 0;

			for (int index = options.experimentIndexFirst; index <= options.experimentIndexLast; index++) {
				Experiment exp = expList.getItemAt(index);
				exp.load_MS96_spotsMeasures();
				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);

				String seriesIdentifier = CellReference.convertNumToColString(iSeries);
				column = exportExperimentData(exp, options, column, seriesIdentifier);

				iSeries++;
				progress.incPosition();
			}

			progress.setMessage(ExcelExportConstants.SAVE_PROGRESS_MESSAGE);

		} catch (Exception e) {
			throw new ExcelExportException("Export execution failed", "execute_export", "export_loop", e);
		} finally {
			// Ensure progress frame is properly closed
			if (progress != null) {
				progress.close();
			}
		}
	}

	/**
	 * Prepares experiments for export by loading data and setting up chains.
	 * 
	 * @throws ExcelDataException If experiment preparation fails
	 */
	protected void prepareQExperiments() throws ExcelDataException {
		try {
			expList.loadListOfMeasuresFromAllExperiments(true, options.onlyalive);
//			expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
//			expList.setFirstImageForAllExperiments(options.collateSeries);
//			expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);
		} catch (Exception e) {
			throw new ExcelDataException("Failed to prepare experiments for export", "prepare_experiments",
					"experiment_loading", e);
		}
	}

	private void initHeadersArray() {
		headers.add(EnumXLS_QColumnHeader.DATE);
		headers.add(EnumXLS_QColumnHeader.EXP_BOXID);
		headers.add(EnumXLS_QColumnHeader.EXP_EXPT);
		headers.add(EnumXLS_QColumnHeader.EXP_STIM1);
		headers.add(EnumXLS_QColumnHeader.EXP_CONC1);
		headers.add(EnumXLS_QColumnHeader.EXP_STIM2);
		headers.add(EnumXLS_QColumnHeader.EXP_CONC2);
		headers.add(EnumXLS_QColumnHeader.CAGE_STRAIN);
		headers.add(EnumXLS_QColumnHeader.CAGE_NFLIES);
		headers.add(EnumXLS_QColumnHeader.CAGE_POS);
		headers.add(EnumXLS_QColumnHeader.VAL_TIME);
		headers.add(EnumXLS_QColumnHeader.VAL_STIM1);
		headers.add(EnumXLS_QColumnHeader.VAL_STIM2);
		headers.add(EnumXLS_QColumnHeader.VAL_SUM);
		headers.add(EnumXLS_QColumnHeader.VAL_PI);
		for (int i = 0; i < headers.size(); i++)
			headers.get(i).setValue(i);
	}

	@Override
	protected int exportExperimentData(Experiment exp, XLSExportOptions xlsExportOptions, int startColumn,
			String charSeries) throws ExcelExportException {
		int column = getCageDataAndExport(exp, startColumn, charSeries, xlsExportOptions, EnumXLSExport.AREA_SUMCLEAN);
		return column;
	}

	protected int getCageDataAndExport(Experiment exp, int col0, String charSeries, XLSExportOptions xlsExportOptions,
			EnumXLSExport exportType) throws ExcelDataException {
		options.exportType = exportType;
		int colmax = 0;
		try {
			SXSSFSheet sheet = xlsGetQSheet(exportType.toString(), exportType);
			colmax = xlsExportExperimentCageDataToSheet(sheet, exp, xlsExportOptions, exportType, col0, charSeries);
		} catch (Exception e) {
			throw new ExcelDataException("Failed to get access to sheet or to export", "getCageDataAndExport",
					"experiment_export", e);
		}
		return colmax;
	}

	SXSSFSheet xlsGetQSheet(String title, EnumXLSExport xlsExport) throws ExcelResourceException {
		SXSSFWorkbook workbook = resourceManager.getWorkbook();
		SXSSFSheet sheet = workbook.getSheet(title);

		if (sheet == null) {
			sheet = resourceManager.getWorkbook().createSheet(title);
			writeTopRow_Qdescriptors(sheet);
		}
		return sheet;
	}

	int writeTopRow_Qdescriptors(SXSSFSheet sheet) {
		Point pt = new Point(0, 0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLS_QColumnHeader dumb : headers) {
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol + 1;
		return pt.y;
	}

	int xlsExportExperimentCageDataToSheet(SXSSFSheet sheet, Experiment exp, XLSExportOptions xlsExportOptions,
			EnumXLSExport xlsExportType, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		String stim1 = exp.getProperties().getField_stim1();
		String conc1 = exp.getProperties().getField_conc1();
		String stim2 = exp.getProperties().getField_stim2();
		String conc2 = exp.getProperties().getField_conc2();

		for (Cage cage : exp.cagesArray.cagesList) {
			double scalingFactorToPhysicalUnits = cage.spotsArray.getScalingFactorToPhysicalUnits(xlsExportType);
			Spot spot1 = cage.combineSpotsWith(stim1, conc1);
			Spot spot2 = cage.combineSpotsWith(stim2, conc2);
			Spot spotSUM = cage.createSpotSUM(spot1, spot2);
			Spot spotPI = cage.createSpotPI(spot1, spot2);

			XLSResults xlsStim1 = getResultForCage(exp, cage, spot1, scalingFactorToPhysicalUnits, xlsExportOptions,
					xlsExportType);
			XLSResults xlsStim2 = getResultForCage(exp, cage, spot2, scalingFactorToPhysicalUnits, xlsExportOptions,
					xlsExportType);
			XLSResults xlsSUM = getResultForCage(exp, cage, spotSUM, scalingFactorToPhysicalUnits, xlsExportOptions,
					xlsExportType);
			XLSResults xlsPI = getResultForCage(exp, cage, spotPI, scalingFactorToPhysicalUnits, xlsExportOptions,
					xlsExportType);

			int duration = 0;
			if (xlsStim1 != null)
				duration = xlsStim1.dimension;
			else if (xlsStim2 != null)
				duration = xlsStim2.dimension;

			for (int t = 0; t < duration; t++) {
				pt.y = 0;
				writeCageInfosToXLS(sheet, pt, exp, charSeries, cage, xlsExportType);
				pt.y -= 4;
				writeDataAtTToXLS(sheet, pt, t, xlsStim1, xlsStim2, xlsPI, xlsSUM, xlsExportType);
				pt.x++;
			}
		}
		pt.x++;
		return pt.x;
	}

	XLSResults getResultForCage(Experiment exp, Cage cage, Spot spot, double scaling, XLSExportOptions xlsExportOptions,
			EnumXLSExport xlsExportType) {
		XLSResults xlsResults = null;
		if (spot != null) {
			xlsResults = getSpotResults(exp, cage, spot, xlsExportOptions);
			xlsResults.transferMeasuresToValuesOut(scaling, xlsExportType);
		}
		return xlsResults;
	}

	void writeCageInfosToXLS(SXSSFSheet sheet, Point pt, Experiment exp, String charSeries, Cage cage,
			EnumXLSExport xlsExportType) {
		boolean transpose = options.transpose;
		for (int i = 0; i < headers.size(); i++) {
			String dummy = getDescriptor(exp, cage, headers.get(i));
			pt.y = headers.get(i).getValue();
			XLSUtils.setValue(sheet, pt, transpose, dummy);
		}
	}

	void writeDataAtTToXLS(SXSSFSheet sheet, Point pt, int t, XLSResults xlsStim1, XLSResults xlsStim2,
			XLSResults xlsPI, XLSResults xlsSUM, EnumXLSExport xlsExportType) {
		pt.y = EnumXLS_QColumnHeader.VAL_TIME.getValue();
		XLSUtils.setValue(sheet, pt, options.transpose, t);
		pt.y = EnumXLS_QColumnHeader.VAL_STIM1.getValue();
		writeDataToXLS(sheet, pt, t, xlsStim1);
		pt.y = EnumXLS_QColumnHeader.VAL_STIM2.getValue();
		writeDataToXLS(sheet, pt, t, xlsStim2);
		pt.y = EnumXLS_QColumnHeader.VAL_SUM.getValue();
		writeDataToXLS(sheet, pt, t, xlsSUM);
		pt.y = EnumXLS_QColumnHeader.VAL_PI.getValue();
		writeDataToXLS(sheet, pt, t, xlsPI);

		pt.y++;
	}

	void writeDataToXLS(SXSSFSheet sheet, Point pt, int t, XLSResults xlsResult) {
		if (xlsResult == null)
			return;
		double value = xlsResult.valuesOut[t];
		boolean transpose = options.transpose;
		if (!Double.isNaN(value)) {
			XLSUtils.setValue(sheet, pt, transpose, value);
		}
	}

	String getDescriptor(Experiment exp, Cage cage, EnumXLS_QColumnHeader col) {
		String dummy = null;
		switch (col) {
		case DATE:
			SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
			return df.format(exp.seqCamData.getTimeManager().getFirstImageMs());
		case EXP_BOXID:
			return exp.getProperties().getFfield_boxID();
		case CAGEID:
			return Integer.toString(cage.getProperties().getCageID());
		case EXP_EXPT:
			return exp.getProperties().getFfield_experiment();
		case EXP_STRAIN:
			return exp.getProperties().getField_strain();
		case EXP_SEX:
			return exp.getProperties().getField_sex();
		case EXP_STIM1:
			return exp.getProperties().getField_stim1();
		case EXP_CONC1:
			return exp.getProperties().getField_conc1();
		case EXP_STIM2:
			return exp.getProperties().getField_stim2();
		case EXP_CONC2:
			return exp.getProperties().getField_conc2();

		case CAGE_POS:
			return Integer.toString(cage.getProperties().getArrayIndex());
		case CAGE_NFLIES:
			return Integer.toString(cage.getProperties().getCageNFlies());
		case CAGE_STRAIN:
			return cage.getProperties().getFlyStrain();
		case CAGE_SEX:
			return cage.getProperties().getFlySex();
		case CAGE_AGE:
			return Integer.toString(cage.getProperties().getFlyAge());
		case CAGE_COMMENT:
			return cage.getProperties().getComment();
//		case DUM4:
//			break;
//		case VAL_TIME:
//			break;
//		case VAL_STIM1:
//			break;
//		case VAL_STIM2:
//			break;
//		case VAL_SUM:
//			break;
//		case VAL_PI:
//			break;
		default:
			break;
		}
		return dummy;
	}

}
