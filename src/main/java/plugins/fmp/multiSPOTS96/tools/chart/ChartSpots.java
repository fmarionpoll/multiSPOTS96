package plugins.fmp.multiSPOTS96.tools.chart;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

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
	public JPanel mainChartPanel = null;
	public IcyFrame mainChartFrame = null;

	public Range yRange = null;
	public Range xRange = null;

	private Point ptGraphLocationn = new Point(0, 0);

	int nPanelsAlongX = 1;
	int nPanelsAlongY = 1;

	public ChartPanel[][] chartPanelArray = null;
	Experiment exp = null;
	private MultiSPOTS96 parent0 = null;

	// ----------------------------------------

	public void createPanel(String title, Experiment exp, XLSExportOptions xlsExportOptions, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
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
		row = row * exp.cagesArray.nRowsPerCage;
		col = col * exp.cagesArray.nColumnsPerCage;
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
		this.exp = exp;
		ChartCageSpots chartCage = new ChartCageSpots();
		chartCage.initMaxMin();

		XLSResultsArray xlsResultsArray = getDataAsResultsArray(exp, xlsExportOptions);
		XLSResultsArray xlsResultsArray2 = null;
		if (xlsExportOptions.exportType == EnumXLSExport.AREA_SUMCLEAN) {
			xlsExportOptions.exportType = EnumXLSExport.AREA_SUM;
			xlsResultsArray2 = getDataAsResultsArray(exp, xlsExportOptions);
			xlsExportOptions.exportType = EnumXLSExport.AREA_SUMCLEAN;
		}

		// ---------------------------
		int index_cage = 0;
		for (int row = 0; row < exp.cagesArray.nCagesAlongY; row++) {
			for (int col = 0; col < exp.cagesArray.nCagesAlongX; col++) {
				if (index_cage < xlsExportOptions.cageIndexFirst || index_cage > xlsExportOptions.cageIndexLast) {
					index_cage++;
					continue;
				}
				Cage cage = exp.cagesArray.getCageFromRowColCoordinates(row, col);
				if (cage == null || cage.spotsArray.spotsList.size() < 1)
					continue;

				int cageID = cage.prop.cageID;
				if (xlsExportOptions.cageIndexFirst >= 0
						&& (cageID < xlsExportOptions.cageIndexFirst || cageID > xlsExportOptions.cageIndexLast)) {
					cageID++;
					continue;
				}

				XYSeriesCollection xyDataSetList = chartCage.combineResults(cage, xlsResultsArray, xlsResultsArray2);
				XYPlot cageXYPlot = chartCage.buildXYPlot(xyDataSetList,
						setYaxis(cage.getRoi().getName(), row, col, xlsExportOptions));

				JFreeChart chart = new JFreeChart(null, // xlsExportOptions.exportType.toTitle()
						null, // titleFont
						cageXYPlot, // cageXYPlot, // combinedXYPlot, // plot
						false); // true);

				// create legend
				chart.setID("row:" + row + ":col:" + col + ":cageID:" + cageID);

				ChartPanel panel = new ChartPanel(chart, // chart
						200, 100, // preferred width and height of panel
						50, 25, // min width and height of panel
						1200, 600, // max width and height of panel
						true, // use memory buffer to improve performance
						true, // chart property editor available via popup menu
						true, // copy option available via popup menu
						true, // print option available via popup menu
						false, // zoom options added to the popup menu
						true); // tooltips enabled for the chart

				panel.addChartMouseListener(new ChartMouseListener() {
					public void chartMouseClicked(ChartMouseEvent e) {
						Spot clickedSpot = getSpotFromClickedChart(e);
						if (clickedSpot != null) {
							chartSelectClickedSpot(exp, xlsExportOptions, clickedSpot);
							Cage cage = exp.cagesArray.getCageFromCageID(clickedSpot.prop.cageID);
							parent0.dlgSpots.tabInfos.selectCage(cage);
							parent0.dlgSpots.tabInfos.selectSpot(clickedSpot);
						}
					}

					public void chartMouseMoved(ChartMouseEvent e) {
					}
				});

				chartPanelArray[row][col] = panel;
				index_cage++;
			}
		}

		if (xlsExportOptions.cageIndexFirst == xlsExportOptions.cageIndexLast) {
			int indexCage = xlsExportOptions.cageIndexFirst;
			int irow = indexCage / exp.cagesArray.nCagesAlongX;
			int icol = indexCage % exp.cagesArray.nCagesAlongX;
			mainChartPanel.add(chartPanelArray[irow][icol]);
		} else {
			for (int row = 0; row < nPanelsAlongY; row++) {
				for (int col = 0; col < nPanelsAlongX; col++) {
					JPanel chartPanel = chartPanelArray[row][col];
					if (chartPanel == null)
						chartPanel = new JPanel();
					mainChartPanel.add(chartPanel);
				}
			}
		}

		// -----------------------------------
		mainChartFrame.pack();
		mainChartFrame.setLocation(ptGraphLocationn);
		mainChartFrame.addToDesktopPane();
		mainChartFrame.setVisible(true);
	}

	public void setChartSpotUpperLeftLocation(Rectangle rectv) {
		ptGraphLocationn = new Point(rectv.x, rectv.y);
	}

	private XLSResultsArray getDataAsResultsArray(Experiment exp, XLSExportOptions xlsExportOptions) {
		XLSExportForChart xlsExport = new XLSExportForChart();
		return xlsExport.getSpotsDataFromOneExperiment_v2parms(exp, xlsExportOptions);
	}

	private Spot getSpotFromClickedChart(ChartMouseEvent e) {
		final MouseEvent trigger = e.getTrigger();
		if (trigger.getButton() != MouseEvent.BUTTON1)
			return null;

		JFreeChart chart = e.getChart();
		MouseEvent mouseEvent = e.getTrigger();
		String[] chartID = chart.getID().split(":");
		int row = Integer.valueOf(chartID[1]);
		int col = Integer.valueOf(chartID[3]);

		ChartPanel panel = chartPanelArray[row][col];
		PlotRenderingInfo plotInfo = panel.getChartRenderingInfo().getPlotInfo();
		Point2D pointClicked = panel.translateScreenToJava2D(mouseEvent.getPoint());

		// get chart
		int subplotindex = plotInfo.getSubplotIndex(pointClicked);
		XYPlot xyPlot = (XYPlot) chart.getPlot();

		// get item in the chart
		Spot spotFound = null;
		String description = null;
		ChartEntity chartEntity = e.getEntity();

		if (chartEntity != null && chartEntity instanceof XYItemEntity) {
			XYItemEntity xyItemEntity = (XYItemEntity) chartEntity;
			int isel = xyItemEntity.getSeriesIndex();
			XYDataset xyDataset = xyItemEntity.getDataset();
			description = (String) xyDataset.getSeriesKey(isel);
			description = description.substring(0, Math.min(description.length(), 16));

			spotFound = exp.cagesArray.getSpotFromROIName(description);
			if (spotFound == null) {
				System.out.println("Graph clicked but source not found - description (roiName)=" + description);
				return null;
			} else
				spotFound.spotCamData_T = xyItemEntity.getItem();

		} else if (subplotindex >= 0) {
			XYDataset xyDataset = xyPlot.getDataset(0);
			description = (String) xyDataset.getSeriesKey(0);

			spotFound = exp.cagesArray.getSpotFromROIName(description);
		} else {
			System.out.println("Graph clicked but source not found");
			return null;
		}

		int index = exp.cagesArray.getSpotGlobalPosition(spotFound);
		spotFound.spotKymograph_T = index;
		return spotFound;
	}

	private void chartSelectSpot(Experiment exp, Spot spot) {
		ROI2D roi = spot.getRoi();
		exp.seqCamData.seq.setFocusedROI(roi);
		exp.seqCamData.centerOnRoi(roi);
	}

	private void selectT(Experiment exp, XLSExportOptions xlsExportOptions, Spot spot) {
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		if (v != null && spot != null && spot.spotCamData_T > 0) {
			int ii = (int) (spot.spotCamData_T * xlsExportOptions.buildExcelStepMs / exp.seqCamData.binDuration_ms);
			v.setPositionT(ii);
		}
	}

	private void chartSelectKymograph(Experiment exp, Spot spot) {
		if (exp.seqKymos != null) {
			Viewer v = exp.seqKymos.seq.getFirstViewer();
			if (v != null && spot != null) {
				v.setPositionT(spot.spotKymograph_T);
			}
		}
	}

	private void chartSelectClickedSpot(Experiment exp, XLSExportOptions xlsExportOptions, Spot clickedSpot) {
		chartSelectSpot(exp, clickedSpot);
		selectT(exp, xlsExportOptions, clickedSpot);
		chartSelectKymograph(exp, clickedSpot);
		exp.seqCamData.seq.setSelectedROI(clickedSpot.getRoi());

		String spotName = clickedSpot.getRoi().getName();
		Cage cage = exp.cagesArray.getCageFromSpotROIName(spotName);
		ROI2D cageRoi = cage.getRoi();
		exp.seqCamData.centerOnRoi(cageRoi);
	}

}
