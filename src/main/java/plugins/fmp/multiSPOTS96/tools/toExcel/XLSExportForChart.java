package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Point;
import java.util.Collections;

import org.apache.poi.xssf.streaming.SXSSFSheet;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.Comparators;

public class XLSExportForChart extends XLSExport {

	// ------------------------------------------------

	void writeTopRow_timeIntervals_Correl(SXSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long interval = -options.nBinsCorrelation;
		while (interval < options.nBinsCorrelation) {
			int i = (int) interval;
			XLSUtils.setValue(sheet, pt, transpose, "t" + i);
			pt.y++;
			interval += 1;
		}
	}

	void writeTopRow_timeIntervals_Default(SXSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long duration = expAll.seqCamData.lastImage_ms - expAll.seqCamData.firstImage_ms;
		long interval = 0;
		while (interval < duration) {
			int i = (int) (interval / options.buildExcelUnitMs);
			XLSUtils.setValue(sheet, pt, transpose, "t" + i);
			pt.y++;
			interval += options.buildExcelStepMs;
		}
	}

//	private XLSResultsArray getSingleSpotDescriptorsForOneExperiment(Experiment exp, EnumXLSExportType xlsOption) {
//		if (expAll == null)
//			return null;
//
//		// loop to get all spots into expAll and init rows for this experiment
//		expAll.cagesArray.copyCages(exp.cagesArray.cagesList, true);
//		expAll.chainImageFirst_ms = exp.chainImageFirst_ms;
//		expAll.expProperties.copyExperimentFieldsFrom(exp.expProperties);
//		expAll.setResultsDirectory(exp.getResultsDirectory());
//
//		Experiment expi = exp.chainToNextExperiment;
//		while (expi != null) {
//			expAll.cagesArray.mergeSpotsLists(expi.cagesArray);
//			expi = expi.chainToNextExperiment;
//		}
//
//		int nFrames = (int) ((expAll.seqCamData.lastImage_ms - expAll.seqCamData.firstImage_ms)
//				/ options.buildExcelStepMs + 1);
//		int nspots = expAll.cagesArray.getTotalNumberOfSpots();
//		XLSResultsArray rowListForOneExp = new XLSResultsArray(nspots);
//		for (Cage cage : expAll.cagesArray.cagesList) {
//			for (Spot spot : cage.spotsArray.spotsList) {
//				XLSResults rowResults = new XLSResults(cage, spot, xlsOption, nFrames);
//				rowResults.stimulus = spot.prop.stimulus;
//				rowResults.concentration = spot.prop.concentration;
//				rowResults.cageID = spot.prop.cageID;
//				rowListForOneExp.resultsList.add(rowResults);
//			}
//		}
//		Collections.sort(rowListForOneExp.resultsList, new Comparators.XLSResults_Name());
//		return rowListForOneExp;
//	}

	private XLSResultsArray getSpotsDescriptorsForOneExperiment(Experiment exp, EnumXLSExport xlsOption) {
		if (expAll == null)
			return null;

		// loop to get all spots into expAll and init rows for this experiment
		expAll.cagesArray.copyCages(exp.cagesArray.cagesList, true);
		expAll.chainImageFirst_ms = exp.chainImageFirst_ms;
		expAll.prop.copyExperimentFieldsFrom(exp.prop);
		expAll.setResultsDirectory(exp.getResultsDirectory());

		Experiment expi = exp.chainToNextExperiment;
		while (expi != null) {
			expAll.cagesArray.mergeSpotsLists(expi.cagesArray);
			expi = expi.chainToNextExperiment;
		}

		int nFrames = (int) ((expAll.seqCamData.lastImage_ms - expAll.seqCamData.firstImage_ms)
				/ options.buildExcelStepMs + 1);
		int nspots = expAll.cagesArray.getTotalNumberOfSpots();
		XLSResultsArray rowListForOneExp = new XLSResultsArray(nspots);
		for (Cage cage : expAll.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				XLSResults rowResults = new XLSResults(cage, spot, xlsOption, nFrames);
				rowResults.stimulus = spot.prop.stimulus;
				rowResults.concentration = spot.prop.concentration;
				rowResults.cageID = spot.prop.cageID;
				rowResults.name = spot.getName();
				rowListForOneExp.resultsList.add(rowResults);
			}
		}
		Collections.sort(rowListForOneExp.resultsList, new Comparators.XLSResults_Name());
		return rowListForOneExp;
	}
//
//	public XLSResultsArray getSpotDataFromOneExperiment_v3parms(Experiment exp, EnumXLSExportType exportType,
//			XLSExportOptions options) {
//		this.options = options;
//		expAll = new Experiment();
//		expAll.seqCamData.lastImage_ms = exp.seqCamData.lastImage_ms;
//		expAll.seqCamData.firstImage_ms = exp.seqCamData.firstImage_ms;
//		return getSpotDataFromOneExperimentSeries_v3parms(exp, exportType);
//	}

	public XLSResultsArray getSpotsDataFromOneExperiment_v2parms(Experiment exp, XLSExportOptions options) {
		this.options = options;
		expAll = new Experiment();
		expAll.seqCamData.lastImage_ms = exp.seqCamData.lastImage_ms;
		expAll.seqCamData.firstImage_ms = exp.seqCamData.firstImage_ms;
		return getSpotsDataFromOneExperimentSeries_v2parms(exp, options);
	}

//
//	private XLSResultsArray getSpotDataFromOneExperimentSeries_v3parms(Experiment exp,
//			EnumXLSExportType xlsExportType) {
//		XLSResultsArray rowListForOneExp = getSingleSpotDescriptorsForOneExperiment(exp, xlsExportType);
//		Experiment expi = exp.getFirstChainedExperiment(true);
//
//		while (expi != null) {
//			int nOutputFrames = getNOutputFrames(expi);
//			if (nOutputFrames > 1) {
//				XLSResultsArray resultsArrayList = new XLSResultsArray(expi.cagesArray.getTotalNumberOfSpots());
//				options.compensateEvaporation = false;
//				resultsArrayList.getSpotsArrayResults1(expi.cagesArray, nOutputFrames, exp.seqCamData.binDuration_ms,
//						options);
//				addResultsTo_rowsForOneExp(rowListForOneExp, expi, resultsArrayList);
//			}
//			expi = expi.chainToNextExperiment;
//		}
//		return rowListForOneExp;
//	}

	private XLSResultsArray getSpotsDataFromOneExperimentSeries_v2parms(Experiment exp, XLSExportOptions options) {
		XLSResultsArray rowListForOneExp = getSpotsDescriptorsForOneExperiment(exp, options.exportType);
		Experiment expi = exp.getFirstChainedExperiment(true);
		while (expi != null) {
			int nOutputFrames = getNOutputFrames(expi);
			if (nOutputFrames > 1) {
				XLSResultsArray resultsArrayList = new XLSResultsArray(expi.cagesArray.getTotalNumberOfSpots());
				options.compensateEvaporation = false;
				resultsArrayList.getSpotsArrayResults1(expi.cagesArray, nOutputFrames, exp.seqCamData.binDuration_ms,
						options);
				addResultsTo_rowsForOneExp(rowListForOneExp, expi, resultsArrayList);
			}
			expi = expi.chainToNextExperiment;
		}
		return rowListForOneExp;
	}

	private XLSResults getResultsArrayWithThatName(String testname, XLSResultsArray resultsArrayList) {
		XLSResults resultsFound = null;
		for (XLSResults results : resultsArrayList.resultsList) {
			if (results.name.equals(testname)) {
				resultsFound = results;
				break;
			}
		}
		return resultsFound;
	}

	private void addResultsTo_rowsForOneExp(XLSResultsArray rowListForOneExp, Experiment expi,
			XLSResultsArray resultsArrayList) {
		if (resultsArrayList.resultsList.size() < 1)
			return;

//		EnumXLSExportType xlsoption = resultsArrayList.getRow(0).exportType;

		long offsetChain = expi.seqCamData.firstImage_ms - expi.chainImageFirst_ms;
		long start_Ms = expi.seqCamData.binFirst_ms + offsetChain; // TODO check when collate?
		long end_Ms = expi.seqCamData.binLast_ms + offsetChain;
		if (options.fixedIntervals) {
			if (start_Ms < options.startAll_Ms)
				start_Ms = options.startAll_Ms;
			if (start_Ms > expi.seqCamData.lastImage_ms)
				return;
			if (end_Ms > options.endAll_Ms)
				end_Ms = options.endAll_Ms;
			if (end_Ms > expi.seqCamData.firstImage_ms)
				return;
		}

		// TODO check this
		final long from_first_Ms = start_Ms - offsetChain;
		final long from_lastMs = end_Ms - offsetChain;
		final int to_first_index = (int) (start_Ms / options.buildExcelStepMs);
		final int to_nvalues = (int) ((end_Ms - start_Ms) / options.buildExcelStepMs) + 1;

		for (int iRow = 0; iRow < rowListForOneExp.size(); iRow++) {
			XLSResults row = rowListForOneExp.getRow(iRow);
			XLSResults results = getResultsArrayWithThatName(row.name, resultsArrayList);
			if (results != null && results.valuesOut != null) {
				double dvalue = 0.;
//				switch (xlsoption) {
//				case TOPLEVEL:
//				case TOPLEVEL_LR:
//				case TOPLEVELDELTA:
//				case TOPLEVELDELTA_LR:
//					if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null)
//						dvalue = padWithLastPreviousValue(row, to_first_index);
//					break;
//				default:
//					break;
//				}

				int icolTo = 0;
				if (options.collateSeries || options.absoluteTime)
					icolTo = to_first_index;
				for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs, icolTo++) {
					int from_i = (int) Math
							.round(((double) (fromTime - from_first_Ms)) / ((double) options.buildExcelStepMs));
					if (from_i >= results.valuesOut.length)
						break;
					// TODO check how this can happen
					if (from_i < 0)
						continue;
					double value = results.valuesOut[from_i] + dvalue;
					if (icolTo >= row.valuesOut.length)
						break;
					row.valuesOut[icolTo] = value;
				}

			} else {
				if (options.collateSeries && options.padIntervals && expi.chainToPreviousExperiment != null) {
					double dvalue = padWithLastPreviousValue(row, to_first_index);
					int tofirst = (int) to_first_index;
					int tolast = (int) (tofirst + to_nvalues);
					if (tolast > row.valuesOut.length)
						tolast = row.valuesOut.length;
					for (int toi = tofirst; toi < tolast; toi++)
						row.valuesOut[toi] = dvalue;
				}
			}
		}
	}

	private double padWithLastPreviousValue(XLSResults row, long to_first_index) {
		double dvalue = 0;
		if (to_first_index >= row.valuesOut.length)
			return dvalue;

		int index = getIndexOfFirstNonEmptyValueBackwards(row, to_first_index);
		if (index >= 0) {
			dvalue = row.valuesOut[index];
			for (int i = index + 1; i < to_first_index; i++) {
				row.valuesOut[i] = dvalue;
				row.padded_out[i] = true;
			}
		}
		return dvalue;
	}

	private int getIndexOfFirstNonEmptyValueBackwards(XLSResults row, long fromindex) {
		int index = -1;
		int ifrom = (int) fromindex;
		for (int i = ifrom; i >= 0; i--) {
			if (!Double.isNaN(row.valuesOut[i])) {
				index = i;
				break;
			}
		}
		return index;
	}

}
