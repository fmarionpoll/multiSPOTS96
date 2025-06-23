package plugins.fmp.multiSPOTS96.experiment.spots;

public class SpotString {
	private final String underlyingString;

	public SpotString(String underlyingString) {
		this.underlyingString = underlyingString;
	}

	public String getUnderlyingString() {
		return underlyingString;
	}

	static public int getCageIDFromSpotName(String description) {
		int index = -1;
		String[] roiDescription = description.split("_");
		try {
			index = Integer.parseInt(roiDescription[1]);
		} catch (NumberFormatException e1) {
		}
		return index;
	}

	static public int getSpotCagePositionFromSpotName(String description) {
		int index = -1;
		String[] roiDescription = description.split("_");
		try {
			index = Integer.parseInt(roiDescription[2]);
		} catch (NumberFormatException e1) {
		}
		return index;
	}

	static public int getSpotArrayIndexFromSpotName(String description) {
		int index = -1;
		String[] roiDescription = description.split("_");
		try {
			index = Integer.parseInt(roiDescription[1])*Integer.parseInt(roiDescription[2]);
		} catch (NumberFormatException e1) {
		}
		return index;
	}

	static public String createSpotString(int cageID, int cagePosition, int spotArrayIndex) {
		return "spot_" + String.format("%03d", cageID) + "_" + String.format("%03d", cagePosition) + "_"
				+ String.format("%03d", spotArrayIndex);
	}

}
