package plugins.fmp.multiSPOTS96.tools.NHDistance;

import java.awt.Color;

/**
 * Class NH L1ColorDistance.
 */
public class NHDistanceColorL1 extends NHDistanceColor {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.image.feature.ColorDistance#computeDistance(double[],
	 * double[])
	 */
	@Override
	public double computeDistance(Color c1, Color c2) {
		double dr = c1.getRed() - c2.getRed();
		double dg = c1.getGreen() - c2.getGreen();
		double db = c1.getBlue() - c2.getBlue();
		return Math.abs(dr) + Math.abs(dg) + Math.abs(db);
	}

}
