package plugins.fmp.multiSPOTS96.tools.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.logging.Logger;

import org.jfree.chart.ChartColor;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResults;
import plugins.fmp.multiSPOTS96.tools.toExcel.XLSResultsArray;

/**
 * Utility class for creating and managing cage charts. This class provides
 * functionality to build XY plots from cage data, including data extraction,
 * plot configuration, and rendering setup.
 * 
 * <p>
 * ChartCage handles the conversion of cage data into chart-ready formats,
 * manages global min/max values for axis scaling, and configures plot
 * appearance based on data characteristics.
 * </p>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * ChartCageSpots chartBuilder = new ChartCageSpots();
 * chartBuilder.initMaxMin();
 * 
 * XYSeriesCollection data = chartBuilder.combineResults(cage, resultsArray1, resultsArray2);
 * NumberAxis xAxis = new NumberAxis("Time");
 * NumberAxis yAxis = new NumberAxis("Value");
 * XYPlot plot = chartBuilder.buildXYPlot(data, xAxis, yAxis);
 * </pre>
 * 
 * @author MultiSPOTS96
 * @see org.jfree.chart.plot.XYPlot
 * @see org.jfree.data.xy.XYSeriesCollection
 * @see plugins.fmp.multiSPOTS96.experiment.cages.Cage
 */
public class ChartCage {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(ChartCage.class.getName());

	/** Default stroke width for chart lines */
	private static final float DEFAULT_STROKE_WIDTH = 0.5f;

	/** Default dash pattern for secondary data series */
	private static final float[] DASH_PATTERN = { 2.0f, 4.0f };

	/** Default dash phase for secondary data series */
	private static final float DASH_PHASE = 0.0f;

	/** Background color for charts with data */
	private static final Color BACKGROUND_WITH_DATA = Color.WHITE;

	/** Background color for charts without data */
	private static final Color BACKGROUND_WITHOUT_DATA = Color.LIGHT_GRAY;

	/** Grid color for charts with data */
	private static final Color GRID_WITH_DATA = Color.GRAY;

	/** Grid color for charts without data */
	private static final Color GRID_WITHOUT_DATA = Color.WHITE;

	/** Token used to mark secondary data series */
	private static final String SECONDARY_DATA_TOKEN = "*";

	/** Delimiter used in series descriptions */
	private static final String DESCRIPTION_DELIMITER = ":";

	/** Flag indicating if global min/max values have been set */
	private boolean flagMaxMinSet = false;

	/** Global maximum Y value across all series */
	private double globalYMax = 0;

	/** Global minimum Y value across all series */
	private double globalYMin = 0;

	/** Global maximum X value across all series */
	private double globalXMax = 0;

	/** Current maximum Y value for the current series */
	private double ymax = 0;

	/** Current minimum Y value for the current series */
	private double ymin = 0;

	/** Current maximum X value for the current series */
	private double xmax = 0;

	/**
	 * Initializes the global min/max tracking variables. This method should be
	 * called before processing new data to reset the global extrema tracking.
	 */
	public void initMaxMin() {
		ymax = 0;
		ymin = 0;
		xmax = 0;
		flagMaxMinSet = false;
		globalYMax = 0;
		globalYMin = 0;
		globalXMax = 0;

		LOGGER.fine("Initialized max/min tracking variables");
	}

	/**
	 * Builds an XY plot from the given dataset and axes.
	 * 
	 * @param xySeriesCollection the dataset to plot
	 * @param xAxis              the X-axis to use
	 * @param yAxis              the Y-axis to use
	 * @return configured XYPlot ready for chart creation
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public XYPlot buildXYPlot(XYSeriesCollection xySeriesCollection, NumberAxis xAxis, NumberAxis yAxis) {
		if (xySeriesCollection == null) {
			throw new IllegalArgumentException("XYSeriesCollection cannot be null");
		}
		if (xAxis == null) {
			throw new IllegalArgumentException("X-axis cannot be null");
		}
		if (yAxis == null) {
			throw new IllegalArgumentException("Y-axis cannot be null");
		}

		LOGGER.fine("Building XY plot with " + xySeriesCollection.getSeriesCount() + " series");

		XYLineAndShapeRenderer subPlotRenderer = getSubPlotRenderer(xySeriesCollection);
		XYPlot subplot = new XYPlot(xySeriesCollection, xAxis, yAxis, subPlotRenderer);
		updatePlotBackgroundAccordingToNFlies(xySeriesCollection, subplot);

		return subplot;
	}

	/**
	 * Updates the plot background and grid colors based on the number of flies in
	 * the data.
	 * 
	 * @param xySeriesCollection the dataset to analyze
	 * @param subplot            the plot to update
	 */
	private void updatePlotBackgroundAccordingToNFlies(XYSeriesCollection xySeriesCollection, XYPlot subplot) {
		if (xySeriesCollection == null || xySeriesCollection.getSeriesCount() == 0) {
			LOGGER.warning("Cannot update plot background: dataset is null or empty");
			return;
		}

		try {
			String[] description = xySeriesCollection.getSeries(0).getDescription().split(DESCRIPTION_DELIMITER);
			if (description.length < 6) {
				LOGGER.warning("Invalid series description format, using default background");
				setDefaultBackground(subplot);
				return;
			}

			int nflies = Integer.parseInt(description[5]);
			if (nflies > 0) {
				subplot.setBackgroundPaint(BACKGROUND_WITH_DATA);
				subplot.setDomainGridlinePaint(GRID_WITH_DATA);
				subplot.setRangeGridlinePaint(GRID_WITH_DATA);
				LOGGER.fine("Set background for chart with " + nflies + " flies");
			} else {
				subplot.setBackgroundPaint(BACKGROUND_WITHOUT_DATA);
				subplot.setDomainGridlinePaint(GRID_WITHOUT_DATA);
				subplot.setRangeGridlinePaint(GRID_WITHOUT_DATA);
				LOGGER.fine("Set background for chart with no flies");
			}
		} catch (NumberFormatException e) {
			LOGGER.warning("Could not parse number of flies from description: " + e.getMessage());
			setDefaultBackground(subplot);
		}
	}

	/**
	 * Sets the default background for a plot.
	 * 
	 * @param subplot the plot to update
	 */
	private void setDefaultBackground(XYPlot subplot) {
		subplot.setBackgroundPaint(BACKGROUND_WITHOUT_DATA);
		subplot.setDomainGridlinePaint(GRID_WITHOUT_DATA);
		subplot.setRangeGridlinePaint(GRID_WITHOUT_DATA);
	}

	XLSResultsArray xlsResultsArray = getXLSResultsFromCage(cage, xlsExportOptions);

	/**
	 * Extracts spot data from one cage in the results array.
	 * 
	 * @param xlsResultsArray the results array to search
	 * @param cage            the cage to get data for
	 * @param token           token to append to series names
	 * @return XYSeriesCollection containing the cage's data
	 */
	XYSeriesCollection getSpotDataFromOneCage(XLSResultsArray xlsResultsArray, Cage cage, String token) {
		if (xlsResultsArray == null || cage == null) {
			LOGGER.warning("Cannot get spot data: results array or cage is null");
			return new XYSeriesCollection();
		}

		XYSeriesCollection xySeriesCollection = null;
		int seriesCount = 0;

		for (int i = 0; i < xlsResultsArray.size(); i++) {
			XLSResults xlsResults = xlsResultsArray.getRow(i);
			if (xlsResults == null) {
				LOGGER.warning("Null result at index " + i + ", skipping");
				continue;
			}

			if (cage.prop.cageID != xlsResults.cageID) {
				continue;
			}

			if (xySeriesCollection == null) {
				xySeriesCollection = new XYSeriesCollection();
			}

			XYSeries seriesXY = getXYSeries(xlsResults, xlsResults.name + token);
			if (seriesXY != null) {
				seriesXY.setDescription(buildSeriesDescription(xlsResults, cage));
				xySeriesCollection.addSeries(seriesXY);
				seriesCount++;
				updateGlobalMaxMin();
			}
		}

		LOGGER.fine("Extracted " + seriesCount + " series for cage ID: " + cage.prop.cageID);
		return xySeriesCollection;
	}

	/**
	 * Builds a description string for a series.
	 * 
	 * @param xlsResults the results data
	 * @param cage       the cage data
	 * @return formatted description string
	 */
	private String buildSeriesDescription(XLSResults xlsResults, Cage cage) {
		return "ID:" + xlsResults.cageID + ":Pos:" + xlsResults.cagePosition + ":nflies:" + cage.prop.cageNFlies + ":R:"
				+ xlsResults.color.getRed() + ":G:" + xlsResults.color.getGreen() + ":B:" + xlsResults.color.getBlue();
	}

	/**
	 * Adds all series from source collection to destination collection.
	 * 
	 * @param destination the collection to add series to
	 * @param source      the collection to copy series from
	 */
	private void addXYSeriesCollection(XYSeriesCollection destination, XYSeriesCollection source) {
		if (source == null) {
			LOGGER.fine("Source collection is null, nothing to add");
			return;
		}

		int addedCount = 0;
		for (int j = 0; j < source.getSeriesCount(); j++) {
			XYSeries xySeries = source.getSeries(j);
			if (xySeries != null) {
				destination.addSeries(xySeries);
				addedCount++;
			}
		}

		LOGGER.fine("Added " + addedCount + " series from secondary collection");
	}

	/**
	 * Creates an XY series from results data.
	 * 
	 * @param xlsResults the results data
	 * @param name       the series name
	 * @return XYSeries containing the data points
	 */
	private XYSeries getXYSeries(XLSResults xlsResults, String name) {
		if (xlsResults == null) {
			LOGGER.warning("Cannot create XY series: results is null");
			return null;
		}

		XYSeries seriesXY = new XYSeries(name, false);

		if (xlsResults.valuesOut != null && xlsResults.valuesOut.length > 0) {
			xmax = xlsResults.valuesOut.length;
			ymax = xlsResults.valuesOut[0];
			ymin = ymax;
			addPointsAndUpdateExtrema(seriesXY, xlsResults, 0);
			LOGGER.fine("Created series '" + name + "' with " + xlsResults.valuesOut.length + " points");
		} else {
			LOGGER.warning("No data points in results for series '" + name + "'");
		}

		return seriesXY;
	}

	/**
	 * Updates the global min/max values based on current series extrema.
	 */
	private void updateGlobalMaxMin() {
		if (!flagMaxMinSet) {
			globalYMax = ymax;
			globalYMin = ymin;
			globalXMax = xmax;
			flagMaxMinSet = true;
			LOGGER.fine(
					"Set initial global extrema: Y[" + globalYMin + ", " + globalYMax + "], X[0, " + globalXMax + "]");
		} else {
			boolean updated = false;
			if (globalYMax < ymax) {
				globalYMax = ymax;
				updated = true;
			}
			if (globalYMin > ymin) {
				globalYMin = ymin;
				updated = true;
			}
			if (globalXMax < xmax) {
				globalXMax = xmax;
				updated = true;
			}

			if (updated) {
				LOGGER.fine(
						"Updated global extrema: Y[" + globalYMin + ", " + globalYMax + "], X[0, " + globalXMax + "]");
			}
		}
	}

	/**
	 * Adds data points to a series and updates local extrema.
	 * 
	 * @param seriesXY   the series to add points to
	 * @param xlsResults the results data
	 * @param startFrame the starting frame number
	 */
	private void addPointsAndUpdateExtrema(XYSeries seriesXY, XLSResults xlsResults, int startFrame) {
		if (seriesXY == null || xlsResults == null || xlsResults.valuesOut == null) {
			LOGGER.warning("Cannot add points: series, results, or values are null");
			return;
		}

		int x = 0;
		int npoints = xlsResults.valuesOut.length;

		for (int j = 0; j < npoints; j++) {
			double y = xlsResults.valuesOut[j];
			seriesXY.add(x + startFrame, y);

			if (ymax < y) {
				ymax = y;
			}
			if (ymin > y) {
				ymin = y;
			}
			x++;
		}

		LOGGER.fine("Added " + npoints + " points to series, local extrema: Y[" + ymin + ", " + ymax + "]");
	}

	/**
	 * Creates a renderer for the XY plot with appropriate styling.
	 * 
	 * @param xySeriesCollection the dataset to render
	 * @return configured XYLineAndShapeRenderer
	 */
	private XYLineAndShapeRenderer getSubPlotRenderer(XYSeriesCollection xySeriesCollection) {
		if (xySeriesCollection == null) {
			LOGGER.warning("Cannot create renderer: dataset is null");
			return null;
		}

		XYLineAndShapeRenderer subPlotRenderer = new XYLineAndShapeRenderer(true, false);
		Stroke stroke = new BasicStroke(DEFAULT_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				DASH_PATTERN, DASH_PHASE);

		for (int i = 0; i < xySeriesCollection.getSeriesCount(); i++) {
			try {
				String[] description = xySeriesCollection.getSeries(i).getDescription().split(DESCRIPTION_DELIMITER);
				if (description.length >= 12) {
					int r = Integer.parseInt(description[7]);
					int g = Integer.parseInt(description[9]);
					int b = Integer.parseInt(description[11]);
					subPlotRenderer.setSeriesPaint(i, new ChartColor(r, g, b));

					String key = (String) xySeriesCollection.getSeriesKey(i);
					if (key != null && key.contains(SECONDARY_DATA_TOKEN)) {
						subPlotRenderer.setSeriesStroke(i, stroke);
					}
				} else {
					LOGGER.warning("Invalid description format for series " + i + ", using default color");
					subPlotRenderer.setSeriesPaint(i, ChartColor.BLACK);
				}
			} catch (NumberFormatException e) {
				LOGGER.warning("Could not parse color values for series " + i + ": " + e.getMessage());
				subPlotRenderer.setSeriesPaint(i, ChartColor.BLACK);
			}
		}

		LOGGER.fine("Created renderer for " + xySeriesCollection.getSeriesCount() + " series");
		return subPlotRenderer;
	}

	/**
	 * Gets the global maximum Y value.
	 * 
	 * @return the global Y maximum
	 */
	public double getGlobalYMax() {
		return globalYMax;
	}

	/**
	 * Gets the global minimum Y value.
	 * 
	 * @return the global Y minimum
	 */
	public double getGlobalYMin() {
		return globalYMin;
	}

	/**
	 * Gets the global maximum X value.
	 * 
	 * @return the global X maximum
	 */
	public double getGlobalXMax() {
		return globalXMax;
	}

	/**
	 * Checks if global min/max values have been set.
	 * 
	 * @return true if global extrema are set, false otherwise
	 */
	public boolean isGlobalMaxMinSet() {
		return flagMaxMinSet;
	}
}
