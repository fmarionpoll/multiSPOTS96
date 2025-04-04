package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;

public class SpotProperties {
	public int version = 1;

	public String sourceName = null;
	public int cageID = -1;
	public int cagePosition = 0;
	public int spotArrayIndex = -1;
	public Color spotColor = spotColors[0];

	// public int spotNFlies = 1;
	public String spotStim = new String("..");
	public String spotConc = new String("..");

	public double spotVolume = 1;
	public int spotNPixels = 1;
	public int spotRadius = 30;
	public int spotXCoord = -1;
	public int spotYCoord = -1;
	public boolean descriptionOK = false;
	public int versionInfos = 0;

	private final static String IDS_SPOTPROPS = "spotProperties";
	private final String ID_DESCOK = "descriptionOK";
	private final String ID_VERSIONINFOS = "versionInfos";
	private final String ID_SPOTVOLUME = "volume";
	private final String ID_PIXELS = "pixels";
	private final String ID_RADIUS = "radius";
	private final String ID_XCOORD = "spotXCoord";
	private final String ID_YCOORD = "spotYCoord";
	private final String ID_SPOTSTIMULUS = "stimulus";
	private final String ID_CONCENTRATION = "concentration";
	private final String ID_CAGEID = "cageID";
	private final String ID_CAGEPOSITION = "cagePosition";
	private final String ID_SPOTARRAYINDEX = "spotArrayIndex";
	private final String ID_COLOR_R = "spotColor_R";
	private final String ID_COLOR_G = "spotColor_G";
	private final String ID_COLOR_B = "spotColor_B";

	static final Color[] spotColors = new Color[] { Color.GREEN, Color.RED, Color.CYAN, Color.YELLOW, Color.MAGENTA,
			Color.ORANGE, Color.PINK, Color.GRAY };

	public void copy(SpotProperties propFrom) {
		spotVolume = propFrom.spotVolume;
		spotStim = propFrom.spotStim;
		spotConc = propFrom.spotConc;
		spotColor = propFrom.spotColor;
		spotArrayIndex = propFrom.spotArrayIndex;
		cageID = propFrom.cageID;
		cagePosition = propFrom.cagePosition;
	}

	public void paste(SpotProperties propTo) {
		propTo.spotVolume = spotVolume;
		propTo.spotStim = spotStim;
		propTo.spotConc = spotConc;
		propTo.spotColor = spotColor;
		propTo.spotArrayIndex = spotArrayIndex;
		propTo.cageID = cageID;
		propTo.cagePosition = cagePosition;
	}

	public boolean isChanged(SpotProperties desc) {
		boolean flag = false;
		return flag;
	}

	public boolean xmlSaveSpotsDescription(Node nodedoc) {
		Node node = XMLUtil.addElement(nodedoc, IDS_SPOTPROPS);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 1);

		return true;
	}

	public boolean xmlLoadSpotsDescription(Document doc) {
		boolean flag = false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), IDS_SPOTPROPS);
		if (node == null)
			return flag;

		version = XMLUtil.getElementIntValue(node, "version", 0);
		flag = loadFromXML(node);
		return flag;
	}

	public boolean loadFromXML(Node node) {
		Element nodeParameters = XMLUtil.getElement(node, IDS_SPOTPROPS);
		if (nodeParameters == null)
			return false;
		spotArrayIndex = XMLUtil.getElementIntValue(nodeParameters, ID_SPOTARRAYINDEX, spotArrayIndex);
		cageID = XMLUtil.getElementIntValue(nodeParameters, ID_CAGEID, cageID);
		cagePosition = XMLUtil.getElementIntValue(nodeParameters, ID_CAGEPOSITION, cagePosition);
//		int version = XMLUtil.getElementIntValue(nodeParameters, ID_VERSIONINFOS, versionInfos);

		descriptionOK = XMLUtil.getElementBooleanValue(nodeParameters, ID_DESCOK, false);
		spotVolume = XMLUtil.getElementDoubleValue(nodeParameters, ID_SPOTVOLUME, Double.NaN);
		spotNPixels = XMLUtil.getElementIntValue(nodeParameters, ID_PIXELS, 5);
		spotRadius = XMLUtil.getElementIntValue(nodeParameters, ID_RADIUS, 30);
		spotXCoord = XMLUtil.getElementIntValue(nodeParameters, ID_XCOORD, -1);
		spotYCoord = XMLUtil.getElementIntValue(nodeParameters, ID_YCOORD, -1);
		spotStim = XMLUtil.getElementValue(nodeParameters, ID_SPOTSTIMULUS, ID_SPOTSTIMULUS);
		spotConc = XMLUtil.getElementValue(nodeParameters, ID_CONCENTRATION, ID_CONCENTRATION);
		int r = XMLUtil.getElementIntValue(nodeParameters, ID_COLOR_R, spotColor.getRed());
		int g = XMLUtil.getElementIntValue(nodeParameters, ID_COLOR_G, spotColor.getGreen());
		int b = XMLUtil.getElementIntValue(nodeParameters, ID_COLOR_B, spotColor.getBlue());
		spotColor = new Color(r, g, b);
		return true;
	}

	public boolean saveToXML(Node node) {
		final Node nodeParameters = XMLUtil.setElement(node, IDS_SPOTPROPS);
		if (nodeParameters == null)
			return false;
		XMLUtil.setElementIntValue(nodeParameters, ID_SPOTARRAYINDEX, spotArrayIndex);
		XMLUtil.setElementIntValue(nodeParameters, ID_CAGEID, cageID);
		XMLUtil.setElementIntValue(nodeParameters, ID_CAGEPOSITION, cagePosition);
		XMLUtil.setElementIntValue(nodeParameters, ID_VERSIONINFOS, versionInfos);

		XMLUtil.setElementBooleanValue(nodeParameters, ID_DESCOK, descriptionOK);
		XMLUtil.setElementDoubleValue(nodeParameters, ID_SPOTVOLUME, spotVolume);
		XMLUtil.setElementIntValue(nodeParameters, ID_PIXELS, spotNPixels);
		XMLUtil.setElementIntValue(nodeParameters, ID_RADIUS, spotRadius);
		XMLUtil.setElementIntValue(nodeParameters, ID_XCOORD, spotXCoord);
		XMLUtil.setElementIntValue(nodeParameters, ID_YCOORD, spotYCoord);
		XMLUtil.setElementValue(nodeParameters, ID_SPOTSTIMULUS, spotStim);
		XMLUtil.setElementValue(nodeParameters, ID_CONCENTRATION, spotConc);
		XMLUtil.setElementIntValue(nodeParameters, ID_COLOR_R, spotColor.getRed());
		XMLUtil.setElementIntValue(nodeParameters, ID_COLOR_G, spotColor.getGreen());
		XMLUtil.setElementIntValue(nodeParameters, ID_COLOR_B, spotColor.getBlue());
		return true;
	}

	// --------------------------------------

	public void csvImportProperties(String[] data) {
		int i = 0;
		sourceName = data[i];
		i++;
		spotArrayIndex = Integer.valueOf(data[i]);
		i++;
		cageID = Integer.valueOf(data[i]);
		if (cageID < 0) {
			cageID = SpotString.getCageIDFromSpotName(sourceName);
		}
		i++;
		cagePosition = Integer.valueOf(data[i]);
		i++;
		spotVolume = Double.valueOf(data[i]);
		i++;
		spotNPixels = Integer.valueOf(data[i]);
		i++;
		spotRadius = Integer.valueOf(data[i]);
		i++;
		spotStim = data[i];
		i++;
		spotConc = data[i];
	}

	static public String csvExportSpotPropertiesHeader(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append("#" + csvSep + "#\n");
		sbf.append("#" + csvSep + "SPOTS" + csvSep + "multiSPOTS96 data\n");
		List<String> row2 = Arrays.asList("name", "index", "cageID", "cagePos", "volume", "npixels", "radius", "stim",
				"conc");
		sbf.append(String.join(csvSep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExportSpotProperties(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row = Arrays.asList(sourceName, String.valueOf(spotArrayIndex), String.valueOf(cageID),
				String.valueOf(cagePosition), String.valueOf(spotVolume), String.valueOf(spotNPixels),
				String.valueOf(spotRadius), spotStim.replace(",", "."), spotConc.replace(",", "."));
		sbf.append(String.join(csvSep, row));
		sbf.append("\n");
		return sbf.toString();
	}

}
