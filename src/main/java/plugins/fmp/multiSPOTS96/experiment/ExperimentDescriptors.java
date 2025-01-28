package plugins.fmp.multiSPOTS96.experiment;

import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;

public class ExperimentDescriptors {

	public String ffield_boxID = new String("..");
	public String ffield_experiment = new String("..");
	public String ffield_stim = new String("..");
	public String ffield_conc = new String("..");
	public String field_comment1 = new String("..");
	public String field_comment2 = new String("..");
	public String field_strain = new String("..");
	public String field_sex = new String("..");
	public String field_cond1 = new String("..");
	public String field_cond2 = new String("..");
	
	private final static String ID_BOXID = "boxID";
	private final static String ID_EXPERIMENT = "experiment";
	private final static String ID_STIM = "stim";
	private final static String ID_CONC = "conc";
	
	private final static String ID_COMMENT1 = "comment";
	private final static String ID_COMMENT2 = "comment2";
	private final static String ID_STRAIN = "strain";
	private final static String ID_SEX = "sex";
	private final static String ID_COND1 = "cond1";
	private final static String ID_COND2 = "cond2";

	public void saveXML_Descriptors(Node node) {
		XMLUtil.setElementValue(node, ID_BOXID, ffield_boxID);
		XMLUtil.setElementValue(node, ID_EXPERIMENT, ffield_experiment);
		XMLUtil.setElementValue(node, ID_STIM, ffield_stim);
		XMLUtil.setElementValue(node, ID_CONC, ffield_conc);
		
		XMLUtil.setElementValue(node, ID_COMMENT1, field_comment1);
		XMLUtil.setElementValue(node, ID_COMMENT2, field_comment2);
		XMLUtil.setElementValue(node, ID_STRAIN, field_strain);
		XMLUtil.setElementValue(node, ID_SEX, field_sex);
		XMLUtil.setElementValue(node, ID_COND1, field_cond1);
		XMLUtil.setElementValue(node, ID_COND2, field_cond2);
	}

	public void loadXML_Descriptors(Node node) {
//		if (field_boxID != null && field_boxID.contentEquals("..")) {
		ffield_boxID = XMLUtil.getElementValue(node, ID_BOXID, "..");
		ffield_experiment = XMLUtil.getElementValue(node, ID_EXPERIMENT, "..");
		ffield_stim = XMLUtil.getElementValue(node, ID_STIM, "..");
		ffield_conc = XMLUtil.getElementValue(node, ID_CONC, "..");
		
		field_comment1 = XMLUtil.getElementValue(node, ID_COMMENT1, "..");
		field_comment2 = XMLUtil.getElementValue(node, ID_COMMENT2, "..");
		field_strain = XMLUtil.getElementValue(node, ID_STRAIN, "..");
		field_sex = XMLUtil.getElementValue(node, ID_SEX, "..");
		field_cond1 = XMLUtil.getElementValue(node, ID_COND1, "..");
		field_cond2 = XMLUtil.getElementValue(node, ID_COND2, "..");
//	}

	}

	public String getExperimentField(EnumXLSColumnHeader fieldEnumCode) {
		String strField = null;
		switch (fieldEnumCode) {
		case EXP_STIM:
			strField = ffield_stim;
			break;
		case EXP_CONC:
			strField = ffield_conc;
			break;
		case EXP_EXPT:
			strField = ffield_experiment;
			break;
		case EXP_BOXID:
			strField = ffield_boxID;
			break;
		case EXP_STRAIN:
			strField = field_strain;
			break;
		case EXP_SEX:
			strField = field_sex;
			break;
		case EXP_COND1:
			strField = field_cond1;
			break;
		case EXP_COND2:
			strField = field_cond2;
			break;
		default:
			break;
		}
		return strField;
	}

	public void setExperimentFieldNoTest(EnumXLSColumnHeader fieldEnumCode, String newValue) {
		switch (fieldEnumCode) {
		case EXP_STIM:
			ffield_stim = newValue;
			break;
		case EXP_CONC:
			ffield_conc = newValue;
			break;
		case EXP_EXPT:
			ffield_experiment = newValue;
			break;
		case EXP_BOXID:
			ffield_boxID = newValue;
			break;
		case EXP_STRAIN:
			field_strain = newValue;
			break;
		case EXP_SEX:
			field_sex = newValue;
			break;
		case EXP_COND1:
			field_cond1 = newValue;
			break;
		case EXP_COND2:
			field_cond2 = newValue;
			break;
		default:
			break;
		}
	}

	public void copyExperimentFields(ExperimentDescriptors expSource) {
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_BOXID,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_EXPT,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STIM));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_CONC));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STRAIN,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_SEX,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND1,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_COND1));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_COND2,
				expSource.getExperimentField(EnumXLSColumnHeader.EXP_COND2));
	}

	public boolean isSameDescriptors(ExperimentDescriptors expi) {
		boolean flag = true;
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_EXPT)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_BOXID)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_STIM)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_STIM));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_CONC)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_CONC));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_SEX)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_SEX));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_COND1)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_COND1));
		flag &= expi.getExperimentField(EnumXLSColumnHeader.EXP_COND2)
				.equals(getExperimentField(EnumXLSColumnHeader.EXP_COND2));
		return flag;
	}
}
