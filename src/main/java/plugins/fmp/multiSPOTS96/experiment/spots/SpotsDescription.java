package plugins.fmp.multiSPOTS96.experiment.spots;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.ExperimentDescriptors;

public class SpotsDescription {
	public int version = 1;

	public double volume = 5.;
	public int pixels = 5;
	public String sourceName = null;
	public ExperimentDescriptors expDesc = new ExperimentDescriptors();

	public int grouping2 = 2;
	public String stimulusR = new String("..");
	public String concentrationR = new String("..");
	public String stimulusL = new String("..");
	public String concentrationL = new String("..");

	private final static String IDS_SPOTTRACK = "spotTrack";
	private final static String IDS_PARAMETERS = "Parameters";
	private final static String IDS_FILE = "file";
	private final static String IDS_ID = "ID";
	private final static String IDS_DESCGROUPING = "Grouping";
	private final static String IDS_DESCN = "n";
	private final static String IDS_DESCCAPVOLUME = "capillaryVolume";
	private final static String IDS_DESCVOLUMEUL = "volume_ul";
	private final static String IDS_DESCCAPILLARYPIX = "capillaryPixels";
	private final static String IDS_DESCNPIXELS = "npixels";

	private final static String IDS_LRSTIMULUS = "LRstimulus";
	private final static String IDS_STIMR = "stimR";
	private final static String IDS_CONCR = "concR";
	private final static String IDS_STIML = "stimL";
	private final static String IDS_CONCL = "concL";
	private final static String IDS_EXPERIMENT = "Experiment";
	private final static String IDS_BOXID = "boxID";
	private final static String IDS_EXPT = "expt";
	private final static String IDS_COMMENT1 = "comment";
	private final static String IDS_COMMENT2 = "comment2";
	private final static String IDS_STRAIN = "strain";
	private final static String IDS_SEX = "sex";
	private final static String IDS_COND1 = "cond1";
	private final static String IDS_COND2 = "cond2";

	public void copy(SpotsDescription desc) {
		volume = desc.volume;
		pixels = desc.pixels;
		grouping2 = desc.grouping2;
		stimulusR = desc.stimulusR;
		stimulusL = desc.stimulusL;
		concentrationR = desc.concentrationR;
		concentrationL = desc.concentrationL;
	}

	public boolean isChanged(SpotsDescription desc) {
		boolean flag = false;
		flag |= (volume != desc.volume);
		flag |= (pixels != desc.pixels);
		flag |= (grouping2 != desc.grouping2);
		flag |= (stimulusR != null && !stimulusR.equals(desc.stimulusR));
		flag |= (concentrationR != null && !concentrationR.equals(desc.concentrationR));
		flag |= (stimulusL != null && !stimulusL.equals(desc.stimulusL));
		flag |= (concentrationL != null && !concentrationL.equals(desc.concentrationL));
		return flag;
	}

	public boolean xmlSaveSpotsDescription(Document doc) {
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), IDS_SPOTTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);

		Element xmlElement = XMLUtil.addElement(node, IDS_PARAMETERS);

		XMLUtil.addElement(xmlElement, IDS_FILE, sourceName);
		Element xmlVal = XMLUtil.addElement(xmlElement, "capillaries");
		XMLUtil.setElementIntValue(xmlVal, IDS_DESCGROUPING, grouping2);
		XMLUtil.setElementDoubleValue(xmlVal, IDS_DESCVOLUMEUL, volume);
		XMLUtil.setElementIntValue(xmlVal, IDS_DESCNPIXELS, pixels);

		xmlVal = XMLUtil.addElement(xmlElement, IDS_EXPERIMENT);
		expDesc.saveXML_Descriptors(xmlVal);
		return true;
	}

	public boolean xmlLoadSpotsDescription(Document doc) {
		boolean flag = false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), IDS_SPOTTRACK);
		if (node == null)
			return flag;
		version = XMLUtil.getElementIntValue(node, "version", 0);
		switch (version) {
		case 0:
			flag = xmlLoadSpotsDescriptionv0(node);
			break;
		case 1:
		default:
			flag = xmlLoadCSpotsDescriptionv1(node);
			break;
		}
		return flag;
	}

	private boolean xmlLoadSpotsDescriptionv0(Node node) {
		Element xmlElement = XMLUtil.getElement(node, IDS_PARAMETERS);
		if (xmlElement == null)
			return false;

		Element xmlVal = XMLUtil.getElement(xmlElement, IDS_FILE);
		sourceName = XMLUtil.getAttributeValue(xmlVal, IDS_ID, null);

		xmlVal = XMLUtil.getElement(xmlElement, IDS_DESCGROUPING);
		grouping2 = XMLUtil.getAttributeIntValue(xmlVal, IDS_DESCN, 2);

		xmlVal = XMLUtil.getElement(xmlElement, IDS_DESCCAPVOLUME);
		volume = XMLUtil.getAttributeDoubleValue(xmlVal, IDS_DESCVOLUMEUL, Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, IDS_DESCCAPILLARYPIX);
		pixels = (int) XMLUtil.getAttributeDoubleValue(xmlVal, IDS_DESCNPIXELS, Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, IDS_LRSTIMULUS);
		if (xmlVal != null) {
			stimulusR = XMLUtil.getAttributeValue(xmlVal, IDS_STIMR, IDS_STIMR);
			concentrationR = XMLUtil.getAttributeValue(xmlVal, IDS_CONCR, IDS_CONCR);
			stimulusL = XMLUtil.getAttributeValue(xmlVal, IDS_STIML, IDS_STIML);
			concentrationL = XMLUtil.getAttributeValue(xmlVal, IDS_CONCL, IDS_CONCL);
		}

		expDesc.loadXML_Descriptors(node);
		return true;
	}

	private boolean xmlLoadCSpotsDescriptionv1(Node node) {
		Element xmlElement = XMLUtil.getElement(node, IDS_PARAMETERS);
		if (xmlElement == null)
			return false;

		sourceName = XMLUtil.getElementValue(xmlElement, IDS_FILE, null);
		Element xmlVal = XMLUtil.getElement(xmlElement, "capillaries");
		if (xmlVal != null) {
			grouping2 = XMLUtil.getElementIntValue(xmlVal, IDS_DESCGROUPING, 2);
			volume = XMLUtil.getElementDoubleValue(xmlVal, IDS_DESCVOLUMEUL, Double.NaN);
			pixels = XMLUtil.getElementIntValue(xmlVal, IDS_DESCNPIXELS, 5);
		}

		xmlVal = XMLUtil.getElement(xmlElement, IDS_LRSTIMULUS);
		if (xmlVal != null) {
			stimulusR = XMLUtil.getElementValue(xmlVal, IDS_STIMR, IDS_STIMR);
			concentrationR = XMLUtil.getElementValue(xmlVal, IDS_CONCR, IDS_CONCR);
			stimulusL = XMLUtil.getElementValue(xmlVal, IDS_STIML, IDS_STIML);
			concentrationL = XMLUtil.getElementValue(xmlVal, IDS_CONCL, IDS_CONCL);
		}

//		expDesc.saveXML_Descriptors(node);

		return true;
	}

	// --------------------------------------

	public String csvExportSectionHeader(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		sbf.append("#" + csvSep + "DESCRIPTION" + csvSep + "multiSPOTS96 data\n");
		List<String> row2 = Arrays.asList(
				IDS_DESCGROUPING, 
				IDS_DESCVOLUMEUL, 
				IDS_DESCNPIXELS, 
				IDS_STIMR, 
				IDS_CONCR,
				IDS_STIML, 
				IDS_CONCL, 
				IDS_BOXID, 
				IDS_EXPT, 
				IDS_COMMENT1, 
				IDS_COMMENT2, 
				IDS_STRAIN, 
				IDS_SEX, 
				IDS_COND1, 
				IDS_COND2);
		sbf.append(String.join(csvSep, row2));
		sbf.append("\n");
		return sbf.toString();
	}

	public String csvExportExperimentDescriptors(String csvSep) {
		StringBuffer sbf = new StringBuffer();
		List<String> row3 = Arrays.asList(
				Integer.toString(grouping2), 
				Double.toString(volume), 
				Integer.toString(pixels),
				stimulusR, 
				concentrationR.replace(",", "."), 
				stimulusL, 
				concentrationL.replace(",", "."),
				expDesc.ffield_boxID, 
				expDesc.ffield_experiment, 
				expDesc.field_comment1, 
				expDesc.field_comment2,
				expDesc.field_strain, 
				expDesc.field_sex, 
				expDesc.field_cond1, 
				expDesc.field_cond2);
		sbf.append(String.join(csvSep, row3));
		sbf.append("\n");
		return sbf.toString();
	}

	public void csvImportSpotsDescriptionData(String[] data) {
		int i = 0;
		grouping2 = Integer.valueOf(data[i]);
		i++;
		volume = Double.valueOf(data[i]);
		i++;
		pixels = Integer.valueOf(data[i]);
		i++;
		stimulusR = data[i];
		i++;
		concentrationR = data[i];
		i++;
		stimulusL = data[i];
		i++;
		concentrationL = data[i];
		i++;
		expDesc.ffield_boxID = data[i];
		i++;
		expDesc.ffield_experiment = data[i];
		i++;
		expDesc.field_comment1 = data[i];
		i++;
		expDesc.field_comment2 = data[i];
		i++;
		expDesc.field_strain = data[i];
		i++;
		expDesc.field_sex = data[i];
		int nitems = data.length;
		if (i < nitems)
			expDesc.field_cond1 = data[i];
		i++;
		if (i < nitems)
			expDesc.field_cond2 = data[i];
	}

}
