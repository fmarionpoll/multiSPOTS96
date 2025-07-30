package plugins.fmp.multiSPOTS96.tools.chart;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import icy.util.StringUtil;

public class ChartCagePanel extends ChartPanel implements PropertyChangeListener, AutoCloseable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Background color for charts with data */
	private static final Color BACKGROUND_WITH_DATA = Color.WHITE;

	/** Background color for charts without data */
	private static final Color BACKGROUND_WITHOUT_DATA = Color.LIGHT_GRAY;

	/** Grid color for charts with data */
	private static final Color GRID_WITH_DATA = Color.GRAY;

	/** Grid color for charts without data */
	private static final Color GRID_WITHOUT_DATA = Color.WHITE;

	public ChartCagePanel(JFreeChart chart, int width, int height, int minimumDrawWidth, int minimumDrawHeight,
			int maximumDrawWidth, int maximumDrawHeight, boolean useBuffer, boolean properties, boolean copy,
			boolean save, boolean print, boolean zoom) {
		super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, useBuffer,
				properties, copy, save, print, zoom);
		// TODO Auto-generated constructor stub
	}

//	public ChartPanelObserver(JFreeChart chart) {
//		super(chart);
//		// TODO Auto-generated constructor stub
//	}

	private void updateFlyCountDisplay(int flyCount) {
		XYPlot xyPlot = getChart().getXYPlot();
		setXYPlotBackGroundAccordingToNFlies(xyPlot, flyCount);
	}

//	private void updateCageColorDisplay(int cageID, java.awt.Color color) {
//		XYPlot xyPlot = getChart().getXYPlot();
//		xyPlot.setBackgroundPaint(color);
//	}

	private void setXYPlotBackGroundAccordingToNFlies(XYPlot xyPlot, int flyCount) {
		if (flyCount > 0) {
			xyPlot.setBackgroundPaint(BACKGROUND_WITH_DATA);
			xyPlot.setDomainGridlinePaint(GRID_WITH_DATA);
			xyPlot.setRangeGridlinePaint(GRID_WITH_DATA);
			// LOGGER.fine("Set background for chart with " + nflies + " flies");
		} else {
			xyPlot.setBackgroundPaint(BACKGROUND_WITHOUT_DATA);
			xyPlot.setDomainGridlinePaint(GRID_WITHOUT_DATA);
			xyPlot.setRangeGridlinePaint(GRID_WITHOUT_DATA);
			// LOGGER.fine("Set background for chart with no flies");
		}
	}

//	@Override
//	public void onPropertyChanged(String propertyName, Object oldValue, Object newValue,
//			ObservableCageProperties source) {
//		switch (propertyName) {
//		case "cageNFlies":
//			updateFlyCountDisplay(source.getCageID(), (Integer) newValue);
//			break;
//		case "color":
//			updateCageColorDisplay(source.getCageID(), (java.awt.Color) newValue);
//			break;
////         case "comment":
////             updateCageCommentDisplay(source.getCageID(), (String) newValue);
////             break;
//		default:
//			// Handle other property changes
//			break;
//		}
//
//	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (StringUtil.equals("cageNFlies", evt.getPropertyName())) {
			int flyCount = (int) evt.getNewValue();
			updateFlyCountDisplay(flyCount);
		}

	}

}
