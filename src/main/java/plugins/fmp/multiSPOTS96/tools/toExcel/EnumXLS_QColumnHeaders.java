package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLS_QColumnHeaders {
	DATE("Date", 0, EnumXLSMeasure.COMMON), EXP_BOXID("Box_ID", 1, EnumXLSMeasure.COMMON),
	CAGEID("Cage_ID", 2, EnumXLSMeasure.COMMON), EXP_EXPT("Expmt", 3, EnumXLSMeasure.COMMON),
	EXP_STRAIN("Strain", 4, EnumXLSMeasure.COMMON), EXP_SEX("Sex", 5, EnumXLSMeasure.COMMON),
	EXP_STIM1("Stim1", 6, EnumXLSMeasure.COMMON), EXP_CONC1("Conc1", 7, EnumXLSMeasure.COMMON),
	EXP_STIM2("Stim2", 8, EnumXLSMeasure.COMMON), EXP_CONC2("Conc2", 9, EnumXLSMeasure.COMMON),
	CAGE_POS("Position", 10, EnumXLSMeasure.COMMON), CAGE_NFLIES("Position", 11, EnumXLSMeasure.COMMON),
	CAGE_STRAIN("Cage_strain", 12, EnumXLSMeasure.COMMON), CAGE_SEX("Cage_sex", 13, EnumXLSMeasure.COMMON),
	CAGE_AGE("Cage_age", 14, EnumXLSMeasure.COMMON), CAGE_COMMENT("Cage_comment", 15, EnumXLSMeasure.COMMON),
	DUM4("Dum4", 16, EnumXLSMeasure.COMMON), TIME("time", 16, EnumXLSMeasure.COMMON),
	STIM1("stim1", 17, EnumXLSMeasure.COMMON), STIM2("stim2", 18, EnumXLSMeasure.COMMON),
	SUM("sum", 19, EnumXLSMeasure.COMMON), PI("PI", 20, EnumXLSMeasure.COMMON);

	private final String name;
	private final int value;
	private final EnumXLSMeasure type;

	EnumXLS_QColumnHeaders(String label, int value, EnumXLSMeasure type) {
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

	static final Map<String, EnumXLS_QColumnHeaders> names = Arrays.stream(EnumXLS_QColumnHeaders.values())
			.collect(Collectors.toMap(EnumXLS_QColumnHeaders::getName, Function.identity()));

	static final Map<Integer, EnumXLS_QColumnHeaders> values = Arrays.stream(EnumXLS_QColumnHeaders.values())
			.collect(Collectors.toMap(EnumXLS_QColumnHeaders::getValue, Function.identity()));

	public static EnumXLS_QColumnHeaders fromName(final String name) {
		return names.get(name);
	}

	public static EnumXLS_QColumnHeaders fromValue(final int value) {
		return values.get(value);
	}

	public String toString() {
		return name;
	}

	public EnumXLSMeasure toType() {
		return type;
	}

	public static EnumXLS_QColumnHeaders findByText(String abbr) {
		for (EnumXLS_QColumnHeaders v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
