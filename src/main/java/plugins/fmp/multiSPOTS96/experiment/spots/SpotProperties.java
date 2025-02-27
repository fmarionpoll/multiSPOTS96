package plugins.fmp.multiSPOTS96.experiment.spots;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;

public class SpotProperties {
	public int version = 1;

	public String sourceName = null;
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
	private final static String IDS_DESCVOLUMEUL = "volume_ul";
	private final static String IDS_DESCNPIXELS = "npixels";
	
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

	public String csvExportDescriptionSectionHeader(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append("#" + csvSep + "DESCRIPTION" + csvSep + "multiSPOTS96 data\n");
		List<String> row2 = Arrays.asList(IDS_DESCGROUPING, IDS_DESCVOLUMEUL, IDS_DESCNPIXELS);
		sbf.append(String.join(csvSep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public void csvImportSpotsDescriptionData(String row, String csvSep) {
		String[] data = row.split(csvSep);
		int i = 0;
		grouping2 = Integer.valueOf(data[i]);
		i++;

	}

	public String csvExportSpotsDescriptionData(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row3 = Arrays.asList(String.valueOf(grouping2), String.valueOf(volume), String.valueOf(pixels),
				stimulusR, concentrationR, stimulusL, concentrationL);
		sbf.append(String.join(csvSep, row3));
		sbf.append("\n");
		return sbf.toString();
	}

}
