package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLS_QColumnHeader {
	DATE("Date", 0, EnumXLSMeasure.COMMON), EXP_BOXID("Box_ID", 1, EnumXLSMeasure.COMMON),
	CAGEID("Cage_ID", 2, EnumXLSMeasure.COMMON), EXP_EXPT("Expmt", 3, EnumXLSMeasure.COMMON),
	EXP_STRAIN("Strain", 4, EnumXLSMeasure.COMMON), EXP_SEX("Sex", 5, EnumXLSMeasure.COMMON),
	EXP_STIM1("Stim1", 6, EnumXLSMeasure.COMMON), EXP_CONC1("Conc1", 7, EnumXLSMeasure.COMMON),
	EXP_STIM2("Stim2", 8, EnumXLSMeasure.COMMON), EXP_CONC2("Conc2", 9, EnumXLSMeasure.COMMON),
	CAGE_POS("Position", 10, EnumXLSMeasure.COMMON), CAGE_NFLIES("NFlies", 11, EnumXLSMeasure.COMMON),
	CAGE_STRAIN("Cage_strain", 12, EnumXLSMeasure.COMMON), CAGE_SEX("Cage_sex", 13, EnumXLSMeasure.COMMON),
	CAGE_AGE("Cage_age", 14, EnumXLSMeasure.COMMON), CAGE_COMMENT("Cage_comment", 15, EnumXLSMeasure.COMMON),
	DUM4("Dum4", 16, EnumXLSMeasure.COMMON), VAL_TIME("time", 17, EnumXLSMeasure.COMMON),
	VAL_STIM1("stim1_conc1", 18, EnumXLSMeasure.COMMON), VAL_STIM2("stim2_conc2", 19, EnumXLSMeasure.COMMON),
	VAL_SUM("sum", 20, EnumXLSMeasure.COMMON), VAL_PI("PI", 21, EnumXLSMeasure.COMMON);

	private final String name;
	private int value;
	private final EnumXLSMeasure type;

	EnumXLS_QColumnHeader(String label, int value, EnumXLSMeasure type) {
		this.name = label;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int val) {
		this.value = val;
	}

	static final Map<String, EnumXLS_QColumnHeader> names = Arrays.stream(EnumXLS_QColumnHeader.values())
			.collect(Collectors.toMap(EnumXLS_QColumnHeader::getName, Function.identity()));

	static final Map<Integer, EnumXLS_QColumnHeader> values = Arrays.stream(EnumXLS_QColumnHeader.values())
			.collect(Collectors.toMap(EnumXLS_QColumnHeader::getValue, Function.identity()));

	public static EnumXLS_QColumnHeader fromName(final String name) {
		return names.get(name);
	}

	public static EnumXLS_QColumnHeader fromValue(final int value) {
		return values.get(value);
	}

	public String toString() {
		return name;
	}

	public EnumXLSMeasure toType() {
		return type;
	}

	public static EnumXLS_QColumnHeader findByText(String abbr) {
		for (EnumXLS_QColumnHeader v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
