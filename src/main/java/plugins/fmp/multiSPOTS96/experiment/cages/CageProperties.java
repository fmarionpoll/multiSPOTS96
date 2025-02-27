package plugins.fmp.multiSPOTS96.experiment.cages;

import org.w3c.dom.Element;

import icy.util.XMLUtil;

public class CageProperties {
	public int version = 1;

	public int cageID = -1;
	public int cagePosition = 0;

	public int arrayIndex = 0;
	public int arrayColumn = -1;
	public int arrayRow = -1;

	public int cageNFlies = 0;
	public int cageAge = 5;
	public String strCageComment = "..";
	public String strCageSex = "..";
	public String strCageStrain = "..";
	public String strCageNumber = "0";

	private final String ID_NFLIES = "nflies";
	private final String ID_AGE = "age";
	private final String ID_COMMENT = "comment";
	private final String ID_SEX = "sex";
	private final String ID_STRAIN = "strain";
	private final String ID_CAGEID = "ID";
	private final String ID_CAGEPOSITION = "Pos";
	private final String ID_ARRAYINDEX = "aIndex";
	private final String ID_ARRAYCOLUMN = "aCol";
	private final String ID_ARRAYROW = "aRow";

	public void copy(CageProperties prop) {
		arrayIndex = prop.arrayIndex;
		arrayColumn = prop.arrayColumn;
		arrayRow = prop.arrayRow;
		cageID = prop.cageID;
		cagePosition = prop.cagePosition;

		cageNFlies = prop.cageNFlies;
		cageAge = prop.cageAge;
		strCageComment = prop.strCageComment;
		strCageSex = prop.strCageSex;
		strCageNumber = prop.strCageNumber;
		strCageStrain = prop.strCageStrain;
		strCageNumber = prop.strCageNumber;
	}

	public boolean xmlLoadCageParameters(Element xmlVal) {
		cageID = XMLUtil.getElementIntValue(xmlVal, ID_CAGEID, cageID);
		cagePosition = XMLUtil.getElementIntValue(xmlVal, ID_CAGEPOSITION, cagePosition);
		arrayIndex = XMLUtil.getElementIntValue(xmlVal, ID_ARRAYINDEX, arrayIndex);
		arrayColumn = XMLUtil.getElementIntValue(xmlVal, ID_ARRAYCOLUMN, arrayColumn);
		arrayRow = XMLUtil.getElementIntValue(xmlVal, ID_ARRAYROW, arrayRow);
		cageNFlies = XMLUtil.getElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		cageAge = XMLUtil.getElementIntValue(xmlVal, ID_AGE, cageAge);

		strCageComment = XMLUtil.getElementValue(xmlVal, ID_COMMENT, strCageComment);
		strCageSex = XMLUtil.getElementValue(xmlVal, ID_SEX, strCageSex);
		strCageStrain = XMLUtil.getElementValue(xmlVal, ID_STRAIN, strCageStrain);
		return true;
	}

	public boolean xmlSaveCageParameters(Element xmlVal) {
		XMLUtil.setElementIntValue(xmlVal, ID_CAGEID, cageID);
		XMLUtil.setElementIntValue(xmlVal, ID_CAGEPOSITION, cagePosition);
		XMLUtil.setElementIntValue(xmlVal, ID_ARRAYINDEX, arrayIndex);
		XMLUtil.setElementIntValue(xmlVal, ID_ARRAYCOLUMN, arrayColumn);
		XMLUtil.setElementIntValue(xmlVal, ID_ARRAYROW, arrayRow);
		XMLUtil.setElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		XMLUtil.setElementIntValue(xmlVal, ID_AGE, cageAge);

		XMLUtil.setElementValue(xmlVal, ID_COMMENT, strCageComment);
		XMLUtil.setElementValue(xmlVal, ID_SEX, strCageSex);
		XMLUtil.setElementValue(xmlVal, ID_STRAIN, strCageStrain);
		return true;
	}

}
