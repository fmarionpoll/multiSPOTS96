package plugins.fmp.multiSPOTS96.experiment.spots;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;

public class SpotProperties {
	public int version = 1;

	public int cageID = -1;
	public int cagePosition = 0;
	public int spotArrayIndex = 0;

	public String sourceName = null;
	public int spotNFlies = 1;
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
	private final String ID_SPOTVOLUME = "volume";
	private final String ID_PIXELS = "pixels";
	private final String ID_RADIUS = "radius";
	private final String ID_XCOORD = "spotXCoord";
	private final String ID_YCOORD = "spotYCoord";
	private final String ID_STIMULUS = "stimulus";
	private final String ID_CONCENTRATION = "concentration";
	private final String ID_DESCOK = "descriptionOK";

	public void copy(SpotProperties desc) {
		spotVolume = desc.spotVolume;
		spotStim = desc.spotStim;
		spotConc = desc.spotConc;
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
		descriptionOK = XMLUtil.getElementBooleanValue(nodeParameters, ID_DESCOK, false);
		spotVolume = XMLUtil.getElementDoubleValue(nodeParameters, ID_SPOTVOLUME, Double.NaN);
		spotNPixels = XMLUtil.getElementIntValue(nodeParameters, ID_PIXELS, 5);
		spotRadius = XMLUtil.getElementIntValue(nodeParameters, ID_RADIUS, 30);
		spotXCoord = XMLUtil.getElementIntValue(nodeParameters, ID_XCOORD, -1);
		spotYCoord = XMLUtil.getElementIntValue(nodeParameters, ID_YCOORD, -1);
		spotStim = XMLUtil.getElementValue(nodeParameters, ID_STIMULUS, ID_STIMULUS);
		spotConc = XMLUtil.getElementValue(nodeParameters, ID_CONCENTRATION, ID_CONCENTRATION);
		return true;
	}

	public boolean saveToXML(Node node) {
		final Node nodeParameters = XMLUtil.setElement(node, IDS_SPOTPROPS);
		if (nodeParameters == null)
			return false;
		XMLUtil.setElementBooleanValue(nodeParameters, ID_DESCOK, descriptionOK);
		XMLUtil.setElementDoubleValue(nodeParameters, ID_SPOTVOLUME, spotVolume);
		XMLUtil.setElementIntValue(nodeParameters, ID_PIXELS, spotNPixels);
		XMLUtil.setElementIntValue(nodeParameters, ID_RADIUS, spotRadius);
		XMLUtil.setElementIntValue(nodeParameters, ID_XCOORD, spotXCoord);
		XMLUtil.setElementIntValue(nodeParameters, ID_YCOORD, spotYCoord);
		XMLUtil.setElementValue(nodeParameters, ID_STIMULUS, spotStim);
		XMLUtil.setElementValue(nodeParameters, ID_CONCENTRATION, spotConc);
		return true;
	}

	// --------------------------------------

	public void csvImportDescription(String[] data, boolean dummyColumn) {
		int i = dummyColumn ? 1 : 0;
		spotArrayIndex = Integer.valueOf(data[i]);
		i++;
		cageID = Integer.valueOf(data[i]);
		i++;
		cagePosition = Integer.valueOf(data[i]);
		i++;
		spotNFlies = Integer.valueOf(data[i]);
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

	static public String csvExportDescriptionHeader(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append("#" + csvSep + "DESCRIPTION" + csvSep + "multiSPOTS data\n");
		List<String> row2 = Arrays.asList("spotArrayIndex", "cageID", "cagePos", "nflies", "volume", "npixels",
				"radius", "stim", "conc");
		sbf.append(String.join(csvSep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExportDescription(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row = Arrays.asList(String.valueOf(spotArrayIndex), String.valueOf(cageID),
				String.valueOf(cagePosition), String.valueOf(spotNFlies), String.valueOf(spotVolume),
				String.valueOf(spotNPixels), String.valueOf(spotRadius), spotStim.replace(",", "."),
				spotConc.replace(",", "."));
		sbf.append(String.join(csvSep, row));
		sbf.append("\n");
		return sbf.toString();
	}

}
