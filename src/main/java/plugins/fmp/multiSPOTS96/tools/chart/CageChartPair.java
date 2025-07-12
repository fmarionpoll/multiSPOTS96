package plugins.fmp.multiSPOTS96.tools.chart;

import org.jfree.chart.ChartPanel;

import plugins.fmp.multiSPOTS96.experiment.cages.Cage;

public class CageChartPair {
	ChartPanel chartPanel = null;
	Cage cage = null;

	public CageChartPair(ChartPanel chart, Cage cage) {
		this.chartPanel = chart;
		this.cage = cage;
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	public void setChartPanel(ChartPanel chart) {
		this.chartPanel = chart;
	}

	public Cage getCage() {
		return cage;
	}

	public void setCage(Cage cage) {
		this.cage = cage;
	}

}
