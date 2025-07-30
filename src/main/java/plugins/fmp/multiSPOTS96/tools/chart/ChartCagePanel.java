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

	private void updateFlyCountDisplay(int flyCount) {
		XYPlot xyPlot = getChart().getXYPlot();
		setXYPlotBackGroundAccordingToNFlies(xyPlot, flyCount);
	}

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

CageProperties cageProps = new CageProperties();

//Add a listener for all property changes
cageProps.addPropertyChangeListener(new PropertyChangeListener() {
 @Override
 public void propertyChange(PropertyChangeEvent evt) {
     System.out.println("Property " + evt.getPropertyName() + 
                       " changed from " + evt.getOldValue() + 
                       " to " + evt.getNewValue());
 }
});

//Add a listener for specific property changes
cageProps.addPropertyChangeListener(CageProperties.PROPERTY_COLOR, 
 new PropertyChangeListener() {
     @Override
     public void propertyChange(PropertyChangeEvent evt) {
         // Handle color changes specifically
     }
 });

//Now when you change properties, listeners will be notified
cageProps.setColor(Color.RED);  // This will fire a PropertyChange event
