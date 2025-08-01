package plugins.fmp.multiSPOTS96.dlg.d_spotsMeasures;

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
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.CageString;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.chart.ChartCageArrayFrame;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExport;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSExportOptions;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSExportOptionsBuilder;

public class Charts extends JPanel implements SequenceListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private ChartCageArrayFrame chartCageArray = null;
	private MultiSPOTS96 parent0 = null;
	private JButton displayResultsButton = new JButton("Display results");
	private JButton axisOptionsButton = new JButton("Axis options");
	private ChartOptions graphOptions = null;
	private EnumXLSExport[] measures = new EnumXLSExport[] { //
			EnumXLSExport.AREA_SUM, //
			EnumXLSExport.AREA_SUMCLEAN // ,
			// EnumXLSExportType.AREA_DIFF
	};
	private JComboBox<EnumXLSExport> exportTypeComboBox = new JComboBox<EnumXLSExport>(measures);
	private JCheckBox relativeToCheckbox = new JCheckBox("relative to max", false);
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
		panel04.add(axisOptionsButton);
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
					displayChartPanels(exp);
			}
		});

		displayResultsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					displayChartPanels(exp);
				}
			}
		});

		axisOptionsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (graphOptions != null) {
						graphOptions.close();
					}
					graphOptions = new ChartOptions();
					graphOptions.initialize(parent0, chartCageArray);
					graphOptions.requestFocus();
				}
			}
		});

		relativeToCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					displayChartPanels(exp);
			}
		});
	}

	private Rectangle getInitialUpperLeftPosition(Experiment exp) {
		Rectangle rectv = new Rectangle(50, 500, 10, 10);
		Viewer v = exp.seqCamData.getSequence().getFirstViewer();
		if (v != null) {
			rectv = v.getBounds();
			rectv.translate(0, rectv.height);
		} else {
			rectv = parent0.mainFrame.getBounds();
			rectv.translate(rectv.width, rectv.height + 100);
		}

		int dx = 5;
		int dy = 10;
		rectv.translate(dx, dy);
		return rectv;
	}

	public void displayChartPanels(Experiment exp) {
		exp.seqCamData.getSequence().removeListener(this);
		EnumXLSExport exportType = (EnumXLSExport) exportTypeComboBox.getSelectedItem();
		if (isThereAnyDataToDisplay(exp, exportType))
			chartCageArray = plotSpotMeasuresToChart(exp, exportType, chartCageArray);
		exp.seqCamData.getSequence().addListener(this);
	}

	private ChartCageArrayFrame plotSpotMeasuresToChart(Experiment exp, EnumXLSExport exportType,
			ChartCageArrayFrame iChart) {
		if (iChart != null)
			iChart.getMainChartFrame().dispose();

		int first = 0;
		int last = exp.cagesArray.cagesList.size() - 1;
		if (!displayAllButton.isSelected()) {
			Cage cageFound = exp.cagesArray.findFirstCageWithSelectedSpot();
			if (cageFound == null)
				cageFound = exp.cagesArray.findFirstSelectedCage();
			if (cageFound == null)
				return null;
			exp.seqCamData.centerDisplayOnRoi(cageFound.getRoi());
			String cageNumber = CageString.getCageNumberFromCageRoiName(cageFound.getRoi().getName());
			first = Integer.parseInt(cageNumber);
			last = first;
		}

		XLSExportOptions options = XLSExportOptionsBuilder.forChart().withBuildExcelStepMs(60000)
				.withRelativeToT0(relativeToCheckbox.isSelected()).withExportType(exportType).withCageRange(first, last)
				.build();

		iChart = new ChartCageArrayFrame();
		iChart.createMainChartPanel("Spots measures", exp, options, parent0);
		iChart.setChartSpotUpperLeftLocation(getInitialUpperLeftPosition(exp));
		iChart.displayData(exp, options);
		iChart.getMainChartFrame().toFront();
		iChart.getMainChartFrame().requestFocus();
		return iChart;
	}

	public void closeAllCharts() {
		if (chartCageArray != null)
			chartCageArray.getMainChartFrame().dispose();
		chartCageArray = null;
	}

	private boolean isThereAnyDataToDisplay(Experiment exp, EnumXLSExport option) {
		boolean flag = false;
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.getSpotsList()) {
				flag = spot.hasMeasurements(option);
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
