package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;

public class XLSResults {
	public String name = null;
	String stimulus = null;
	String concentration = null;
	int nadded = 1;
	boolean[] padded_out = null;

	public int dimension = 0;
	public int nflies = 1;
	public int cageID = 0;
	public int cagePosition = 0;
	public Color color;
	public EnumXLSExport exportType = null;
	public ArrayList<Double> dataValues = null;
	public double[] valuesOut = null;

	public XLSResults(String name, int nflies, int cageID, int cagePos, EnumXLSExport exportType) {
		this.name = name;
		this.nflies = nflies;
		this.cageID = cageID;
		this.cagePosition = cagePos;
		this.exportType = exportType;
	}

	public XLSResults(Cage cage, Spot spot, EnumXLSExport exportType, int nFrames) {
		this.name = spot.getName();
		this.color = spot.getProperties().getColor();
		this.nflies = cage.getProperties().getCageNFlies();
		this.cageID = cage.getProperties().getCageID();
		this.cagePosition = spot.getProperties().getCagePosition();
		this.exportType = exportType;
		initValuesArray(nFrames);
	}

	void initValuesOutArray(int dimension, Double val) {
		this.dimension = dimension;
		valuesOut = new double[dimension];
		Arrays.fill(valuesOut, val);
	}

	private void initValuesArray(int dimension) {
		this.dimension = dimension;
		valuesOut = new double[dimension];
		Arrays.fill(valuesOut, Double.NaN);
		padded_out = new boolean[dimension];
		Arrays.fill(padded_out, false);
	}

	void clearValues(int fromindex) {
		int toindex = valuesOut.length;
		if (fromindex > 0 && fromindex < toindex) {
			Arrays.fill(valuesOut, fromindex, toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex, toindex, false);
		}
	}

	void clearAll() {
		dataValues = null;
		valuesOut = null;
		nflies = 0;
	}

	public void transferMeasuresToValuesOut(double scalingFactorToPhysicalUnits, EnumXLSExport xlsExport) {
		if (dimension == 0 || dataValues == null || dataValues.size() < 1)
			return;

		boolean removeZeros = false;
		int len = Math.min(dimension, dataValues.size());
		if (removeZeros) {
			for (int i = 0; i < len; i++) {
				double ivalue = dataValues.get(i);
				valuesOut[i] = (ivalue == 0 ? Double.NaN : ivalue) * scalingFactorToPhysicalUnits;
			}
		} else {
			for (int i = 0; i < len; i++)
				valuesOut[i] = dataValues.get(i) * scalingFactorToPhysicalUnits;
		}
	}

	public void copyValuesOut(XLSResults sourceRow) {
		if (sourceRow.valuesOut.length != valuesOut.length) {
			this.dimension = sourceRow.dimension;
			valuesOut = new double[dimension];
		}
		for (int i = 0; i < dimension; i++)
			valuesOut[i] = sourceRow.valuesOut[i];
	}

	public List<Double> relativeToMaximum() {
		if (dataValues == null || dataValues.size() < 1)
			return null;

		double value0 = getMaximum();
		relativeToValue(value0);
		return dataValues;
	}

	public double getMaximum() {
		double maximum = 0.;
		if (dataValues == null || dataValues.size() < 1)
			return maximum;

		maximum = dataValues.get(0);
		;
		for (int index = 0; index < dataValues.size(); index++) {
			double value = dataValues.get(index);
			maximum = Math.max(maximum, value);
		}

		return maximum;
	}

	private void relativeToValue(double value0) {
		for (int index = 0; index < dataValues.size(); index++) {
			double value = dataValues.get(index);
			// dataValues.set(index, ((value0 - value) / value0));
			dataValues.set(index, value / value0);
		}
	}

	boolean subtractDeltaT(int arrayStep, int binStep) {
		if (valuesOut == null || valuesOut.length < 2)
			return false;
		for (int index = 0; index < valuesOut.length; index++) {
			int timeIndex = index * arrayStep + binStep;
			int indexDelta = (int) (timeIndex / arrayStep);
			if (indexDelta < valuesOut.length)
				valuesOut[index] = valuesOut[indexDelta] - valuesOut[index];
			else
				valuesOut[index] = Double.NaN;
		}
		return true;
	}

}
