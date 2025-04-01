package plugins.fmp.multiSPOTS96.tools.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResults;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResultsArray;

public class ChartCageSpots {
	private boolean flagMaxMinSet = false;
	private double globalYMax = 0;
	private double globalYMin = 0;
	private double globalXMax = 0;

	private double ymax = 0;
	private double ymin = 0;
	private double xmax = 0;

	void initMaxMin() {
		ymax = 0;
		ymin = 0;
		flagMaxMinSet = false;
	}

	XYSeriesCollection combineCageCurves(Cage cage, XLSResultsArray xlsResultsArray, XLSResultsArray xlsResultsArray2) {
		XYSeriesCollection xyDataSetList = getSpotDataFromOneCage(xlsResultsArray, cage, "");
		if (xlsResultsArray2 != null)
			addXYSeriesCollection(xyDataSetList, getSpotDataFromOneCage(xlsResultsArray2, cage, "*"));
		return xyDataSetList;
	}

	XYPlot buildCageXYPlot(XYSeriesCollection xySeriesCollection, Paint[] chartColor) {
		XYLineAndShapeRenderer subPlotRenderer = getSubPlotRenderer(xySeriesCollection, chartColor);
		NumberAxis xAxis = new NumberAxis(); // description[1]);
		XYPlot subplot = new XYPlot(xySeriesCollection, xAxis, null, subPlotRenderer);
		updatePlotBackgroundAccordingToNFlies(xySeriesCollection, subplot);
		return subplot;
	}

	// ---------------------------

	private void updatePlotBackgroundAccordingToNFlies(XYSeriesCollection xySeriesCollection, XYPlot subplot) {
		String[] description = xySeriesCollection.getSeries(0).getDescription().split(":");
		int nflies = Integer.valueOf(description[5]);
		if (nflies > 0) {
			subplot.setBackgroundPaint(Color.WHITE);
			subplot.setDomainGridlinePaint(Color.GRAY);
			subplot.setRangeGridlinePaint(Color.GRAY);
		} else {
			subplot.setBackgroundPaint(Color.LIGHT_GRAY);
			subplot.setDomainGridlinePaint(Color.WHITE);
			subplot.setRangeGridlinePaint(Color.WHITE);
		}
	}

	private XYSeriesCollection getSpotDataFromOneCage(XLSResultsArray xlsResultsArray, Cage cage, String token) {
		XYSeriesCollection xySeriesCollection = null;
		for (int i = 0; i < xlsResultsArray.size(); i++) {
			XLSResults xlsResults = xlsResultsArray.getRow(i);
			if (cage.prop.cageID != xlsResults.cageID)
				continue;

			if (xySeriesCollection == null) {
				xySeriesCollection = new XYSeriesCollection();
			}
			XYSeries seriesXY = getXYSeries(xlsResults, xlsResults.name + token);
			// System.out.println(xlsResults.name + token);
			seriesXY.setDescription(
					"ID:" + xlsResults.cageID + ":Pos:" + xlsResults.cagePosition + ":nflies:" + cage.prop.cageNFlies);
			xySeriesCollection.addSeries(seriesXY);
			updateGlobalMaxMin();
		}
		return xySeriesCollection;
	}

	private void addXYSeriesCollection(XYSeriesCollection destination, XYSeriesCollection source) {
		if (source == null)
			return;
		for (int j = 0; j < source.getSeriesCount(); j++) {
			XYSeries xySeries = source.getSeries(j);
			destination.addSeries(xySeries);
		}
	}

	private XYSeries getXYSeries(XLSResults results, String name) {
		XYSeries seriesXY = new XYSeries(name, false);
		if (results.valuesOut != null && results.valuesOut.length > 0) {
			xmax = results.valuesOut.length;
			ymax = results.valuesOut[0];
			ymin = ymax;
			addPointsAndUpdateExtrema(seriesXY, results, 0);
		}
		return seriesXY;
	}

	private void updateGlobalMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
		} else {
			if (globalYMax < ymax)
				globalYMax = ymax;
			if (globalYMin >= ymin)
				globalYMin = ymin;
			if (globalXMax < xmax)
				globalXMax = xmax;
		}
	}

	private void addPointsAndUpdateExtrema(XYSeries seriesXY, XLSResults results, int startFrame) {
		int x = 0;
		int npoints = results.valuesOut.length;
		for (int j = 0; j < npoints; j++) {
			double y = results.valuesOut[j];
			seriesXY.add(x + startFrame, y);
			if (ymax < y)
				ymax = y;
			if (ymin > y)
				ymin = y;
			x++;
		}
	}

	private XYLineAndShapeRenderer getSubPlotRenderer(XYSeriesCollection xySeriesCollection, Paint[] chartColor) {
		XYLineAndShapeRenderer subPlotRenderer = new XYLineAndShapeRenderer(true, false);
		int maxcolor = chartColor.length;
		Stroke stroke = new BasicStroke(0.5f, // width = width of the stroke
				BasicStroke.CAP_ROUND, // cap = decoration of the ends of the stroke
				BasicStroke.JOIN_ROUND, // join = decoration applied where paths segments meet
				1.0f, // miterlimit = limit to trim the miter join (>= 1)
				new float[] { 2.0f, 4.0f }, // dash = array representing dashing pattern
				0.0f); // dash phase = offset to start dashing pattern

		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			String[] description = xySeriesCollection.getSeries(i).getDescription().split(":");
			int icolor = Integer.valueOf(description[3]);
			String key = (String) xySeriesCollection.getSeriesKey(i);
			// get description to get
			if (key.contains("*"))
				subPlotRenderer.setSeriesStroke(i, stroke);
			icolor = icolor % maxcolor;
			subPlotRenderer.setSeriesPaint(i, chartColor[icolor]);
		}
		return subPlotRenderer;
	}
}
