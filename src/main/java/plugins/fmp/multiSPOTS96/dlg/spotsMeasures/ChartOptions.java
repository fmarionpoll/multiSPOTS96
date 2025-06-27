package plugins.fmp.multiSPOTS96.dlg.spotsMeasures;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.chart.ChartSpots;

public class ChartOptions extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	IcyFrame dialogFrame = null;
	private MultiSPOTS96 parent0 = null;
	private ChartSpots chartSpots = null;
	private JSpinner lowerXSpinner = new JSpinner(new SpinnerNumberModel(0., 0., 255., 1.));
	private JSpinner upperXSpinner = new JSpinner(new SpinnerNumberModel(120., 0., 255., 1.));
	private JSpinner lowerYSpinner = new JSpinner(new SpinnerNumberModel(0., 0., 255., 1.));
	private JSpinner upperYSpinner = new JSpinner(new SpinnerNumberModel(80., 0., 255., 1.));
	private JButton setYaxis = new JButton("set Y axis values");
	private JButton setXaxis = new JButton("set X axis values");

	public void initialize(MultiSPOTS96 parent0, ChartSpots chartSpots) {
		this.parent0 = parent0;
		this.chartSpots = chartSpots;

		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(new JLabel("x axis values:"));
		panel1.add(lowerXSpinner);
		panel1.add(upperXSpinner);
		panel1.add(setXaxis);
		topPanel.add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel("y axis values:"));
		panel2.add(lowerYSpinner);
		panel2.add(upperYSpinner);
		panel2.add(setYaxis);
		topPanel.add(panel2);

		dialogFrame = new IcyFrame("Chart options", true, true);
		dialogFrame.add(topPanel, BorderLayout.NORTH);

		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);

		collectValuesFromAllCharts();
		defineActionListeners();
	}

	public void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			exp.saveSpotsArray_file();
		}
	}

	private void defineActionListeners() {
		setXaxis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					updateXAxis();
				}
			}
		});

		setYaxis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					updateYAxis();
				}
			}
		});
	}

	private void collectValuesFromAllCharts() {
		int nrows = chartSpots.chartPanelArray.length;
		int ncolumns = chartSpots.chartPanelArray[0].length;
		chartSpots.xRange = null;
		chartSpots.yRange = null;

		for (int column = 0; column < ncolumns; column++) {
			for (int row = 0; row < nrows; row++) {
				ChartPanel chartPanel = chartSpots.chartPanelArray[row][column];
				if (chartPanel == null)
					continue;
				XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
				if (plot == null)
					continue;
				ValueAxis xAxis = plot.getDomainAxis();
				ValueAxis yAxis = plot.getRangeAxis();

				if (xAxis != null)
					chartSpots.xRange = Range.combine(chartSpots.xRange, xAxis.getRange());
				if (yAxis != null)
					chartSpots.yRange = Range.combine(chartSpots.yRange, yAxis.getRange());
			}
		}

		if (chartSpots.xRange != null) {
			lowerXSpinner.setValue(chartSpots.xRange.getLowerBound());
			upperXSpinner.setValue(chartSpots.xRange.getUpperBound());
		}
		if (chartSpots.yRange != null) {
			lowerYSpinner.setValue(chartSpots.yRange.getLowerBound());
			upperYSpinner.setValue(chartSpots.yRange.getUpperBound());
		}
	}

	private void updateXAxis() {
		int nrows = chartSpots.chartPanelArray.length;
		int ncolumns = chartSpots.chartPanelArray[0].length;

		double upper = (double) upperXSpinner.getValue();
		double lower = (double) lowerXSpinner.getValue();
		for (int column = 0; column < ncolumns; column++) {
			for (int row = 0; row < nrows; row++) {
				ChartPanel chartPanel = chartSpots.chartPanelArray[row][column];
				if (chartPanel == null)
					continue;
				XYPlot xyPlot = (XYPlot) chartPanel.getChart().getPlot();
				NumberAxis xAxis = (NumberAxis) xyPlot.getDomainAxis();
				xAxis.setAutoRange(false);
				xAxis.setRange(lower, upper);
			}
		}
	}

	private void updateYAxis() {
		int nrows = chartSpots.chartPanelArray.length;
		int ncolumns = chartSpots.chartPanelArray[0].length;

		double upper = (double) upperYSpinner.getValue();
		double lower = (double) lowerYSpinner.getValue();
		for (int column = 0; column < ncolumns; column++) {
			for (int row = 0; row < nrows; row++) {
				ChartPanel chartPanel = chartSpots.chartPanelArray[row][column];
				if (chartPanel == null)
					continue;
				XYPlot xyPlot = (XYPlot) chartPanel.getChart().getPlot();
				NumberAxis yAxis = (NumberAxis) xyPlot.getRangeAxis();
				yAxis.setAutoRange(false);
				yAxis.setRange(lower, upper);
			}
		}
	}

}
