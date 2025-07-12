package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;

public class XLSExportMeasuresCagesAsQuery extends XLSExport {
	ArrayList<EnumXLS_QColumnHeader> headers = new ArrayList<EnumXLS_QColumnHeader>();

	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportSpotAreas:exportToFile() - start output");
		options = opt;
		expList = options.expList;

		expList.loadListOfMeasuresFromAllExperiments(true, options.onlyalive);
//		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
//		expList.setFirstImageForAllExperiments(options.collateSeries);
//		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);
		initHeadersArray();

		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getItemCount();
		progress.setLength(nbexpts);

		try {
			int column = 1;
			int iSeries = 0;
			workbook = xlsInitWorkbook();
			for (int index = options.experimentIndexFirst; index <= options.experimentIndexLast; index++) {
				Experiment exp = expList.getItemAt(index);
				exp.load_MS96_spotsMeasures();
//				if (exp.chainToPreviousExperiment != null)
//					continue;
				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				int collast = column;

//					collast = getCageDataAndExport(exp, column, charSeries, EnumXLSExport.AREA_SUM);
//					getCageDataAndExport(exp, column, charSeries, EnumXLSExport.AREA_FLYPRESENT);
				collast = getCageDataAndExport(exp, column, charSeries, EnumXLSExport.AREA_SUMCLEAN);

				column = collast;
				iSeries++;
				progress.incPosition();
			}
			progress.setMessage("Save Excel file to disk... ");
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
			progress.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLSExpoportSpotAreas:exportToFile() XLS output finished");
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

	protected int getCageDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExport exportType) {
		options.exportType = exportType;
		SXSSFSheet sheet = xlsGetQSheet(exportType.toString(), exportType);
		int colmax = xlsExportExperimentCageDataToSheet(sheet, exp, exportType, col0, charSeries);
//		if (options.onlyalive) {
//			sheet = xlsGetSheet(exportType.toString() + "_alive", exportType);
//			xlsExportExperimentCageDataToSheet(exp, sheet, exportType, col0, charSeries);
//		}
		return colmax;
	}

	SXSSFSheet xlsGetQSheet(String title, EnumXLSExport xlsExport) {
		SXSSFSheet sheet = workbook.getSheet(title);
		if (sheet == null) {
			sheet = workbook.createSheet(title);
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

	int xlsExportExperimentCageDataToSheet(SXSSFSheet sheet, Experiment exp, EnumXLSExport xlsExportType, int col0,
			String charSeries) {
		Point pt = new Point(col0, 0);
		String stim1 = exp.prop.field_stim1;
		String conc1 = exp.prop.field_conc1;
		String stim2 = exp.prop.field_stim2;
		String conc2 = exp.prop.field_conc2;

		for (Cage cage : exp.cagesArray.cagesList) {
			double scalingFactorToPhysicalUnits = cage.spotsArray.getScalingFactorToPhysicalUnits(xlsExportType);
			Spot spot1 = cage.combineSpotsWith(stim1, conc1);
			Spot spot2 = cage.combineSpotsWith(stim2, conc2);
			Spot spotSUM = cage.createSpotSUM(spot1, spot2);
			Spot spotPI = cage.createSpotPI(spot1, spot2);

			XLSResults xlsStim1 = getResultForCage(exp, cage, spot1, scalingFactorToPhysicalUnits, xlsExportType);
			XLSResults xlsStim2 = getResultForCage(exp, cage, spot2, scalingFactorToPhysicalUnits, xlsExportType);
			XLSResults xlsSUM = getResultForCage(exp, cage, spotSUM, scalingFactorToPhysicalUnits, xlsExportType);
			XLSResults xlsPI = getResultForCage(exp, cage, spotPI, scalingFactorToPhysicalUnits, xlsExportType);

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

	XLSResults getResultForCage(Experiment exp, Cage cage, Spot spot, double scaling, EnumXLSExport xlsExportType) {
		XLSResults xlsResults = null;
		if (spot != null) {
			xlsResults = getSpotResults(exp, cage, spot, xlsExportType);
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
			return df.format(exp.chainImageFirst_ms);

		case EXP_BOXID:
			return exp.prop.ffield_boxID;
		case CAGEID:
			return Integer.toString(cage.prop.cageID);
		case EXP_EXPT:
			return exp.prop.ffield_experiment;
		case EXP_STRAIN:
			return exp.prop.field_strain;
		case EXP_SEX:
			return exp.prop.field_sex;
		case EXP_STIM1:
			return exp.prop.field_stim1;
		case EXP_CONC1:
			return exp.prop.field_conc1;
		case EXP_STIM2:
			return exp.prop.field_stim2;
		case EXP_CONC2:
			return exp.prop.field_conc2;

		case CAGE_POS:
			return Integer.toString(cage.prop.cagePosition);
		case CAGE_NFLIES:
			return Integer.toString(cage.prop.cageNFlies);
		case CAGE_STRAIN:
			return cage.prop.flyStrain;
		case CAGE_SEX:
			return cage.prop.flySex;
		case CAGE_AGE:
			return Integer.toString(cage.prop.flyAge);
		case CAGE_COMMENT:
			return cage.prop.comment;
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
