package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.Color;

import org.w3c.dom.Element;

import icy.util.XMLUtil;

public class CageProperties {
	public int version = 1;

	public int cageID = -1;
	public int cagePosition = 0;
	public Color cageColor = Color.MAGENTA;

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
	private final String ID_COLOR_R = "color_R";
	private final String ID_COLOR_G = "color_G";
	private final String ID_COLOR_B = "color_B";

	public void copy(CageProperties propFrom) {
		arrayIndex = propFrom.arrayIndex;
		arrayColumn = propFrom.arrayColumn;
		arrayRow = propFrom.arrayRow;
		cageID = propFrom.cageID;
		cagePosition = propFrom.cagePosition;
		cageColor = propFrom.cageColor;

		cageNFlies = propFrom.cageNFlies;
		cageAge = propFrom.cageAge;
		strCageComment = propFrom.strCageComment;
		strCageSex = propFrom.strCageSex;
		strCageNumber = propFrom.strCageNumber;
		strCageStrain = propFrom.strCageStrain;
	}

	public void paste(CageProperties propTo) {
		propTo.arrayIndex = arrayIndex;
		propTo.arrayColumn = arrayColumn;
		propTo.arrayRow = arrayRow;
		propTo.cageID = cageID;
		propTo.cagePosition = cagePosition;
		propTo.cageColor = cageColor;

		propTo.cageNFlies = cageNFlies;
		propTo.cageAge = cageAge;
		propTo.strCageComment = strCageComment;
		propTo.strCageSex = strCageSex;
		propTo.strCageNumber = strCageNumber;
		propTo.strCageStrain = strCageStrain;
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
		int r = XMLUtil.getElementIntValue(xmlVal, ID_COLOR_R, cageColor.getRed());
		int g = XMLUtil.getElementIntValue(xmlVal, ID_COLOR_G, cageColor.getGreen());
		int b = XMLUtil.getElementIntValue(xmlVal, ID_COLOR_B, cageColor.getBlue());
		cageColor = new Color(r, g, b);
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
		XMLUtil.setElementIntValue(xmlVal, ID_COLOR_R, cageColor.getRed());
		XMLUtil.setElementIntValue(xmlVal, ID_COLOR_G, cageColor.getGreen());
		XMLUtil.setElementIntValue(xmlVal, ID_COLOR_B, cageColor.getBlue());
		return true;
	}

}
