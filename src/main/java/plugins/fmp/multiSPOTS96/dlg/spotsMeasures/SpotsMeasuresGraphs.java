package plugins.fmp.multiSPOTS96.dlg.spotsMeasures;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.CageString;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.chart.ChartSpots;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExportType;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSExportOptions;

public class SpotsMeasuresGraphs extends JPanel implements SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private ChartSpots plotAreaPixels = null;
	private MultiSPOTS96 parent0 = null;
	private JButton displayResultsButton = new JButton("Display results");
	private EnumXLSExportType[] measures = new EnumXLSExportType[] { EnumXLSExportType.AREA_SUM,
			EnumXLSExportType.AREA_SUMCLEAN // , EnumXLSExportType.AREA_DIFF
	};
	private JComboBox<EnumXLSExportType> exportTypeComboBox = new JComboBox<EnumXLSExportType>(measures);
	private JCheckBox relativeToCheckbox = new JCheckBox("relative to t0", false);
//	private JRadioButton t0Button = new JRadioButton("t0", false);
//	private JRadioButton medianT0Button = new JRadioButton("relative to median of first", true);
//	private JSpinner medianT0FromNPointsSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 50, 1));
//	private JLabel medianT0Legend = new JLabel("points");

	private JRadioButton displayAllButton = new JRadioButton("all cages");
	private JRadioButton displaySelectedButton = new JRadioButton("cage selected");

	// ----------------------------------------

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel01 = new JPanel(layout);
		panel01.add(new JLabel("Measure"));
		panel01.add(exportTypeComboBox);
		panel01.add(new JLabel(" display"));
		panel01.add(displayAllButton);
		panel01.add(displaySelectedButton);
		add(panel01);

		JPanel panel02 = new JPanel(layout);
		panel02.add(relativeToCheckbox);
		add(panel02);

		JPanel panel04 = new JPanel(layout);
		panel04.add(displayResultsButton);
		add(panel04);

		ButtonGroup group1 = new ButtonGroup();
		group1.add(displayAllButton);
		group1.add(displaySelectedButton);
		displayAllButton.setSelected(true);

		exportTypeComboBox.setSelectedIndex(1);
		defineActionListeners();
	}

	private void defineActionListeners() {

		exportTypeComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					displayGraphsPanels(exp);
			}
		});

		displayResultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					displayGraphsPanels(exp);
			}
		});

		relativeToCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					displayGraphsPanels(exp);
			}
		});
	}

	private Rectangle getInitialUpperLeftPosition(Experiment exp) {
		Rectangle rectv = new Rectangle(50, 500, 10, 10);
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		if (v != null) {
			rectv = v.getBounds();
			rectv.translate(0, rectv.height);
		} else {
			rectv = parent0.mainFrame.getBounds();
			rectv.translate(rectv.width, rectv.height + 100);
		}
		return rectv;
	}

	public void displayGraphsPanels(Experiment exp) {
		Rectangle rectv = getInitialUpperLeftPosition(exp);
		int dx = 5;
		int dy = 10;
		exp.seqCamData.seq.addListener(this);
		EnumXLSExportType exportType = (EnumXLSExportType) exportTypeComboBox.getSelectedItem();
		if (isThereAnyDataToDisplay(exp, exportType)) {
			rectv.translate(dx, dy);
			plotAreaPixels = plotToChart(exp, exportType, plotAreaPixels, rectv);
		}
	}

	private ChartSpots plotToChart(Experiment exp, EnumXLSExportType exportType, ChartSpots iChart, Rectangle rectv) {
		if (iChart != null)
			iChart.mainChartFrame.dispose();

		XLSExportOptions xlsExportOptions = new XLSExportOptions();
		xlsExportOptions.buildExcelStepMs = 60000;

		boolean bRelative = relativeToCheckbox.isSelected();
		xlsExportOptions.relativeToT0 = bRelative;
		xlsExportOptions.exportType = exportType;

		if (displayAllButton.isSelected()) {
			xlsExportOptions.cageIndexFirst = -1;
		} else {
			String cageName = findSelectedCage(exp);
			if (cageName == null)
				return null;
			String cageNumber = CageString.getCageNumberFromCageRoiName(cageName);
			xlsExportOptions.cageIndexFirst = Integer.parseInt(cageNumber);
			xlsExportOptions.cageIndexLast = xlsExportOptions.cageIndexFirst;
		}

		iChart = new ChartSpots();
		iChart.createSpotsChartPanel2("Spots measures", exp, xlsExportOptions);
		iChart.setUpperLeftLocation(rectv);

		iChart.displayData(exp, xlsExportOptions);
		iChart.mainChartFrame.toFront();
		iChart.mainChartFrame.requestFocus();
		return iChart;
	}

	private String findSelectedCage(Experiment exp) {
		for (Cage cage : exp.cagesArray.cagesList) {
			ROI2D roi = cage.getRoi();
			if (roi.isSelected())
				return roi.getName();
		}
		return null;
	}

	public void closeAllCharts() {
		plotAreaPixels = closeChart(plotAreaPixels);
	}

	private ChartSpots closeChart(ChartSpots chart) {
		if (chart != null)
			chart.mainChartFrame.dispose();
		chart = null;
		return chart;
	}

	private boolean isThereAnyDataToDisplay(Experiment exp, EnumXLSExportType option) {
		boolean flag = false;
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				flag = spot.isThereAnyMeasuresDone(option);
				if (flag)
					break;
			}
			if (flag)
				break;
		}
		return flag;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);
		closeAllCharts();
	}

}
