package plugins.fmp.multiSPOTS96.tools.chart;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSExport;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSExportForChart;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSExportOptions;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResultsArray;

public class ChartSpots extends IcyFrame {
	private static final Logger LOGGER = Logger.getLogger(ChartSpots.class.getName());

	private JPanel mainChartPanel = null;
	public IcyFrame mainChartFrame = null;

	private Range yRange = null;
	private Range xRange = null;

	private Point graphLocation = new Point(0, 0);

	private int nPanelsAlongX = 1;
	private int nPanelsAlongY = 1;

	private ChartPanel[][] chartPanelArray = null;
	private Experiment experiment = null;
	private MultiSPOTS96 parent = null;

	// ----------------------------------------

	public void createPanel(String title, Experiment exp, XLSExportOptions xlsExportOptions, MultiSPOTS96 parent0) {
		if (exp == null) {
			throw new NullPointerException("Experiment cannot be null");
		}

		this.parent = parent0;
		this.experiment = exp;

		mainChartPanel = new JPanel();
		nPanelsAlongX = exp.cagesArray.nCagesAlongX;
		nPanelsAlongY = exp.cagesArray.nCagesAlongY;
		if (xlsExportOptions.cageIndexFirst == xlsExportOptions.cageIndexLast) {
			nPanelsAlongX = 1;
			nPanelsAlongY = 1;
		}
		mainChartPanel.setLayout(new GridLayout(nPanelsAlongY, nPanelsAlongX));
		mainChartFrame = GuiUtil.generateTitleFrame(title, new JPanel(), new Dimension(300, 70), true, true, true,
				true);
		JScrollPane scrollPane = new JScrollPane(mainChartPanel);
		mainChartFrame.add(scrollPane);
		chartPanelArray = new ChartPanel[exp.cagesArray.nCagesAlongY][exp.cagesArray.nCagesAlongX];
	}

	private NumberAxis setYaxis(String title, int row, int col, XLSExportOptions xlsExportOptions) {
		NumberAxis yAxis = new NumberAxis();
		row = row * experiment.cagesArray.nRowsPerCage;
		col = col * experiment.cagesArray.nColumnsPerCage;
		String yLegend = title + " " + String.valueOf((char) (row + 'A')) + "_" + Integer.toString(col);
		yAxis.setLabel(yLegend);

		if (xlsExportOptions.relativeToT0 || xlsExportOptions.relativeToMedianT0) {
			yAxis.setAutoRange(false);
			yAxis.setRange(-0.2, 1.2);
		} else {
			yAxis.setAutoRange(true);
			yAxis.setAutoRangeIncludesZero(false);
		}
		return yAxis;
	}

	public void displayData(Experiment exp, XLSExportOptions xlsExportOptions) {
		this.experiment = exp;
		ChartCageSpots chartCage = new ChartCageSpots();
		chartCage.initMaxMin();

		// Prepare data
		XLSResultsArray xlsResultsArray = getDataAsResultsArray(exp, xlsExportOptions);
		XLSResultsArray xlsResultsArray2 = prepareSecondaryDataIfNeeded(exp, xlsExportOptions);

		// Create chart panels
		createChartPanels(chartCage, xlsResultsArray, xlsResultsArray2, xlsExportOptions);

		// Arrange panels in display
		arrangePanelsInDisplay(xlsExportOptions);

		// Setup and display chart frame
		displayChartFrame();
	}

	private XLSResultsArray prepareSecondaryDataIfNeeded(Experiment exp, XLSExportOptions xlsExportOptions) {
		if (xlsExportOptions.exportType != EnumXLSExport.AREA_SUMCLEAN) {
			return null;
		}

		XLSExportOptions tempOptions = new XLSExportOptions();
		tempOptions.copy(xlsExportOptions);
		tempOptions.exportType = EnumXLSExport.AREA_SUM;
		return getDataAsResultsArray(exp, tempOptions);
	}

	private void createChartPanels(ChartCageSpots chartCage, XLSResultsArray xlsResultsArray,
			XLSResultsArray xlsResultsArray2, XLSExportOptions xlsExportOptions) {
		int indexCage = 0;
		for (int row = 0; row < experiment.cagesArray.nCagesAlongY; row++) {
			for (int col = 0; col < experiment.cagesArray.nCagesAlongX; col++) {
				if (indexCage < xlsExportOptions.cageIndexFirst || indexCage > xlsExportOptions.cageIndexLast) {
					indexCage++;
					continue;
				}

				Cage cage = experiment.cagesArray.getCageFromRowColCoordinates(row, col);
				if (cage == null || cage.spotsArray.spotsList.size() < 1) {
					continue;
				}

				int cageID = cage.prop.cageID;
				if (xlsExportOptions.cageIndexFirst >= 0
						&& (cageID < xlsExportOptions.cageIndexFirst || cageID > xlsExportOptions.cageIndexLast)) {
					indexCage++;
					continue;
				}

				chartPanelArray[row][col] = createChartPanelForCage(chartCage, cage, xlsResultsArray, xlsResultsArray2,
						row, col, xlsExportOptions);
				indexCage++;
			}
		}
	}

	private ChartPanel createChartPanelForCage(ChartCageSpots chartCage, Cage cage, XLSResultsArray xlsResultsArray,
			XLSResultsArray xlsResultsArray2, int row, int col, XLSExportOptions xlsExportOptions) {
		XYSeriesCollection xyDataSetList = chartCage.combineResults(cage, xlsResultsArray, xlsResultsArray2);
		XYPlot cageXYPlot = chartCage.buildXYPlot(xyDataSetList,
				setYaxis(cage.getRoi().getName(), row, col, xlsExportOptions));

		JFreeChart chart = new JFreeChart(null, null, cageXYPlot, false);

		// Store cage data in chart properties instead of string ID
		chart.putClientProperty("cage", cage);
		chart.putClientProperty("row", row);
		chart.putClientProperty("col", col);

		ChartPanel panel = new ChartPanel(chart, 200, 100, 50, 25, 1200, 600, true, true, true, true, false, true);

		panel.addChartMouseListener(new SpotChartMouseListener(experiment, xlsExportOptions));

		return panel;
	}

	private void arrangePanelsInDisplay(XLSExportOptions xlsExportOptions) {
		if (xlsExportOptions.cageIndexFirst == xlsExportOptions.cageIndexLast) {
			int indexCage = xlsExportOptions.cageIndexFirst;
			int row = indexCage / experiment.cagesArray.nCagesAlongX;
			int col = indexCage % experiment.cagesArray.nCagesAlongX;
			mainChartPanel.add(chartPanelArray[row][col]);
		} else {
			for (int row = 0; row < nPanelsAlongY; row++) {
				for (int col = 0; col < nPanelsAlongX; col++) {
					JPanel chartPanel = chartPanelArray[row][col];
					if (chartPanel == null) {
						chartPanel = new JPanel();
					}
					mainChartPanel.add(chartPanel);
				}
			}
		}
	}

	private void displayChartFrame() {
		mainChartFrame.pack();
		mainChartFrame.setLocation(graphLocation);
		mainChartFrame.addToDesktopPane();
		mainChartFrame.setVisible(true);
	}

	public void setChartSpotUpperLeftLocation(Rectangle rectv) {
		graphLocation = new Point(rectv.x, rectv.y);
	}

	private XLSResultsArray getDataAsResultsArray(Experiment exp, XLSExportOptions xlsExportOptions) {
		XLSExportForChart xlsExport = new XLSExportForChart();
		return xlsExport.getSpotsDataFromOneExperiment_v2parms(exp, xlsExportOptions);
	}

	private Spot getSpotFromClickedChart(ChartMouseEvent e) {
		final MouseEvent trigger = e.getTrigger();
		if (trigger.getButton() != MouseEvent.BUTTON1) {
			return null;
		}

		JFreeChart chart = e.getChart();
		Cage cage = (Cage) chart.getClientProperty("cage");
		if (cage == null) {
			LOGGER.warning("Clicked chart has no associated cage");
			return null;
		}

		ChartPanel panel = (ChartPanel) e.getSource();
		PlotRenderingInfo plotInfo = panel.getChartRenderingInfo().getPlotInfo();
		Point2D pointClicked = panel.translateScreenToJava2D(trigger.getPoint());

		// Get chart
		int subplotIndex = plotInfo.getSubplotIndex(pointClicked);
		XYPlot xyPlot = (XYPlot) chart.getPlot();

		// Get item in the chart
		Spot spotFound = null;
		String description = null;
		ChartEntity chartEntity = e.getEntity();

		if (chartEntity != null && chartEntity instanceof XYItemEntity) {
			spotFound = getSpotFromXYItemEntity((XYItemEntity) chartEntity);
		} else if (subplotIndex >= 0) {
			XYDataset xyDataset = xyPlot.getDataset(0);
			description = (String) xyDataset.getSeriesKey(0);
			spotFound = experiment.cagesArray.getSpotFromROIName(description);
		} else {
			LOGGER.warning("Graph clicked but source not found");
			return null;
		}

		if (spotFound == null) {
			LOGGER.warning("Failed to find spot from clicked chart");
			return null;
		}

		int index = experiment.cagesArray.getSpotGlobalPosition(spotFound);
		spotFound.spotKymograph_T = index;
		return spotFound;
	}

	private Spot getSpotFromXYItemEntity(XYItemEntity xyItemEntity) {
		int seriesIndex = xyItemEntity.getSeriesIndex();
		XYDataset xyDataset = xyItemEntity.getDataset();
		String description = (String) xyDataset.getSeriesKey(seriesIndex);
		description = description.substring(0, Math.min(description.length(), 16));

		Spot spotFound = experiment.cagesArray.getSpotFromROIName(description);
		if (spotFound == null) {
			LOGGER.warning("Graph clicked but source not found - description (roiName)=" + description);
			return null;
		}

		spotFound.spotCamData_T = xyItemEntity.getItem();
		return spotFound;
	}

	private void chartSelectSpot(Experiment exp, Spot spot) {
		ROI2D roi = spot.getRoi();
		exp.seqCamData.getSequence().setFocusedROI(roi);
		exp.seqCamData.centerOnRoi(roi);
	}

	private void selectT(Experiment exp, XLSExportOptions xlsExportOptions, Spot spot) {
		Viewer v = exp.seqCamData.getSequence().getFirstViewer();
		if (v != null && spot != null && spot.spotCamData_T > 0) {
			int frameIndex = (int) (spot.spotCamData_T * xlsExportOptions.buildExcelStepMs
					/ exp.seqCamData.getTimeManager().getBinDurationMs());
			v.setPositionT(frameIndex);
		}
	}

	private void chartSelectKymograph(Experiment exp, Spot spot) {
		if (exp.seqKymos != null) {
			Viewer v = exp.seqKymos.getSequence().getFirstViewer();
			if (v != null && spot != null) {
				v.setPositionT(spot.spotKymograph_T);
			}
		}
	}

	private void chartSelectClickedSpot(Experiment exp, XLSExportOptions xlsExportOptions, Spot clickedSpot) {
		chartSelectSpot(exp, clickedSpot);
		selectT(exp, xlsExportOptions, clickedSpot);
		chartSelectKymograph(exp, clickedSpot);
		exp.seqCamData.getSequence().setSelectedROI(clickedSpot.getRoi());

		String spotName = clickedSpot.getRoi().getName();
		Cage cage = exp.cagesArray.getCageFromSpotROIName(spotName);
		if (cage != null) {
			ROI2D cageRoi = cage.getRoi();
			exp.seqCamData.centerOnRoi(cageRoi);
		} else {
			LOGGER.warning("Could not find cage for spot: " + spotName);
		}
	}

	// Accessors for testing and external use

	public JPanel getMainChartPanel() {
		return mainChartPanel;
	}

	public IcyFrame getMainChartFrame() {
		return mainChartFrame;
	}

	public ChartPanel[][] getChartPanelArray() {
		return chartPanelArray;
	}

	public int getPanelsAlongX() {
		return nPanelsAlongX;
	}

	public int getPanelsAlongY() {
		return nPanelsAlongY;
	}

	// Inner class for chart mouse handling

	private class SpotChartMouseListener implements ChartMouseListener {
		private final Experiment experiment;
		private final XLSExportOptions xlsOptions;

		public SpotChartMouseListener(Experiment exp, XLSExportOptions options) {
			this.experiment = exp;
			this.xlsOptions = options;
		}

		@Override
		public void chartMouseClicked(ChartMouseEvent e) {
			Spot clickedSpot = getSpotFromClickedChart(e);
			if (clickedSpot != null) {
				chartSelectClickedSpot(experiment, xlsOptions, clickedSpot);
				Cage cage = experiment.cagesArray.getCageFromID(clickedSpot.prop.cageID);
				if (cage != null && parent != null && parent.dlgSpots != null) {
					parent.dlgSpots.tabInfos.selectCage(cage);
					parent.dlgSpots.tabInfos.selectSpot(clickedSpot);
				}
			}
		}

		@Override
		public void chartMouseMoved(ChartMouseEvent e) {
			// No action needed
		}
	}
}
