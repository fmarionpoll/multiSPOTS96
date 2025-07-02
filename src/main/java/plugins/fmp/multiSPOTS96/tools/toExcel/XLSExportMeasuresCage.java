package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;

public class XLSExportMeasuresCage extends XLSExport {
	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLSExpoportSpotAreas:exportToFile() - start output");
		options = opt;
		expList = options.expList;

		expList.loadListOfMeasuresFromAllExperiments(true, options.onlyalive);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.get_MsTime_of_StartAndEnd_AllExperiments(options);

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
				if (exp.chainToPreviousExperiment != null)
					continue;
				progress.setMessage("Export experiment " + (index + 1) + " of " + nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);

				int collast = column;
				if (options.spotAreas) {
					collast = getCageDataAndExport(exp, column, charSeries, EnumXLSExport.AREA_SUM);
					getCageDataAndExport(exp, column, charSeries, EnumXLSExport.AREA_FLYPRESENT);
					getCageDataAndExport(exp, column, charSeries, EnumXLSExport.AREA_SUMCLEAN);
				}
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
		SXSSFSheet sheet = xlsGetSheet(exportType.toString(), exportType);
		int colmax = xlsExportExperimentCageDataToSheet(exp, sheet, exportType, col0, charSeries);
		if (options.onlyalive) {
			sheet = xlsGetSheet(exportType.toString() + "_alive", exportType);
			xlsExportExperimentCageDataToSheet(exp, sheet, exportType, col0, charSeries);
		}
		return colmax;
	}

	int xlsExportExperimentCageDataToSheet(Experiment exp, SXSSFSheet sheet, EnumXLSExport xlsExportType, int col0,
			String charSeries) {
		Point pt = new Point(col0, 0);
		pt = writeExperiment_separator(sheet, pt);

		for (Cage cage : exp.cagesArray.cagesList) {
			ArrayList<Spot> spotsList = cage.combineSpotsWithSameStimulusConcentration();
			if (spotsList.size() < 2) {
				System.out.println("Only 1 stimulus in cage " + cage.getRoi().getName() + " - file "
						+ exp.getCameraImagesDirectory());
				continue;
			}
			Spot spotPI = cage.createSpotPI(spotsList.get(0), spotsList.get(1));
			spotsList.add(spotPI);
			Spot spotSUM = cage.createSpotSUM(spotsList.get(0), spotsList.get(1));
			spotsList.add(spotSUM);

			double scalingFactorToPhysicalUnits = cage.spotsArray.getScalingFactorToPhysicalUnits(xlsExportType);

			for (Spot spot : spotsList) {
				pt.y = 0;
				pt = writeExperiment_spot_infos(sheet, pt, exp, charSeries, cage, spot, xlsExportType);
				XLSResults xlsResults = getSpotResults(exp, cage, spot, xlsExportType);
				xlsResults.transferMeasuresToValuesOut(scalingFactorToPhysicalUnits, xlsExportType);
				writeXLSResult(sheet, pt, xlsResults);
				pt.x++;
			}
		}
		return pt.x;
	}

}
