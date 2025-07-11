package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;

public class XLSExportMeasuresCagesAsQuery extends XLSExport {
	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportSpotAreas:exportToFile() - start output");
		options = opt;
		expList = options.expList;

		expList.loadListOfMeasuresFromAllExperiments(true, options.onlyalive);
//		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
//		expList.setFirstImageForAllExperiments(options.collateSeries);
//		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);

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

	protected int getCageDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExport exportType) {
		options.exportType = exportType;
		SXSSFSheet sheet = xlsGetQSheet(exportType.toString(), exportType);
		int colmax = xlsExportExperimentCageDataToSheet(exp, sheet, exportType, col0, charSeries);
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
			int row = writeTopRow_Qdescriptors(sheet);
		}
		return sheet;
	}

	int writeTopRow_Qdescriptors(SXSSFSheet sheet) {
		Point pt = new Point(0, 0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLS_QColumnHeaders dumb : EnumXLS_QColumnHeaders.values()) {
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol + 1;
		return pt.y;
	}

	int xlsExportExperimentCageDataToSheet(Experiment exp, SXSSFSheet sheet, EnumXLSExport xlsExportType, int col0,
			String charSeries) {
		Point pt = new Point(col0, 0);
		String stim1 = exp.prop.field_stim1;
		String conc1 = exp.prop.field_conc1;
		String stim2 = exp.prop.field_stim2;
		String conc2 = exp.prop.field_conc2;

		for (Cage cage : exp.cagesArray.cagesList) {

			Spot spot1 = cage.combineSpotsWith(stim1, conc1);
			Spot spot2 = cage.combineSpotsWith(stim2, conc2);
			Spot spotPI = cage.createSpotPI(spot1, spot2);
			Spot spotSUM = cage.createSpotSUM(spot1, spot2);

			double scalingFactorToPhysicalUnits = cage.spotsArray.getScalingFactorToPhysicalUnits(xlsExportType);
			/*
			 * for (int t = 0; t < duration; t++) { pt.y = 0; writeCageInfosToXLS(sheet, pt,
			 * exp, charSeries, cage, xlsExportType); writeDataToXLS(t, spot1, spot2,
			 * spotPI, spotSUM, xlsExportType); pt.x++; }
			 */
//			for (Spot spot : spotsList) {
//				pt.y = 0;
//				pt = writeExperiment_spot_infos(sheet, pt, exp, charSeries, cage, spot, xlsExportType);
//				XLSResults xlsResults = getSpotResults(exp, cage, spot, xlsExportType);
//				xlsResults.transferMeasuresToValuesOut(scalingFactorToPhysicalUnits, xlsExportType);
//				writeXLSResult(sheet, pt, xlsResults);
//				pt.x++;
//			}
		}
		return pt.x;
	}

}
