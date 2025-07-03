package plugins.fmp.multiSPOTS96.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EnumXLSColumnHeader {
	PATH("Path", 0, EnumXLSMeasure.COMMON), DATE("Date", 1, EnumXLSMeasure.COMMON),
	EXP_BOXID("Box_ID", 2, EnumXLSMeasure.COMMON), CAM("Cam", 3, EnumXLSMeasure.COMMON),
	EXP_EXPT("Expmt", 4, EnumXLSMeasure.COMMON), CAGEID("Cage_ID", 5, EnumXLSMeasure.COMMON),
	EXP_STIM("Stim1", 6, EnumXLSMeasure.COMMON), EXP_CONC("Conc1", 7, EnumXLSMeasure.COMMON),
	EXP_STRAIN("Strain", 8, EnumXLSMeasure.COMMON), EXP_SEX("Sex", 9, EnumXLSMeasure.COMMON),
	EXP_COND1("Stim2", 10, EnumXLSMeasure.COMMON), EXP_COND2("Conc2", 11, EnumXLSMeasure.COMMON),
	SPOT_VOLUME("Spot_ul", 13, EnumXLSMeasure.COMMON), SPOT_PIXELS("Spot_npixels", 14, EnumXLSMeasure.COMMON),
	CHOICE_NOCHOICE("Choice", 15, EnumXLSMeasure.COMMON), SPOT_STIM("Spot_stimulus", 16, EnumXLSMeasure.COMMON),
	SPOT_CONC("Spot_concentration", 17, EnumXLSMeasure.COMMON), SPOT_NFLIES("Nflies", 18, EnumXLSMeasure.COMMON),
	SPOT_CAGEID("Cage", 19, EnumXLSMeasure.COMMON), CAGEPOS("Position", 12, EnumXLSMeasure.COMMON),
	SPOT_CAGEROW("cageRow", 20, EnumXLSMeasure.COMMON), SPOT_CAGECOL("cageCol", 21, EnumXLSMeasure.COMMON),
	CAGE_STRAIN("Cage_strain", 22, EnumXLSMeasure.COMMON), CAGE_SEX("Cage_sex", 23, EnumXLSMeasure.COMMON),
	CAGE_AGE("Cage_age", 24, EnumXLSMeasure.COMMON), CAGE_COMMENT("Cage_comment", 25, EnumXLSMeasure.COMMON),
	DUM4("Dum4", 26, EnumXLSMeasure.COMMON);

	private final String name;
	private final int value;
	private final EnumXLSMeasure type;

	EnumXLSColumnHeader(String label, int value, EnumXLSMeasure type) {
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

	static final Map<String, EnumXLSColumnHeader> names = Arrays.stream(EnumXLSColumnHeader.values())
			.collect(Collectors.toMap(EnumXLSColumnHeader::getName, Function.identity()));

	static final Map<Integer, EnumXLSColumnHeader> values = Arrays.stream(EnumXLSColumnHeader.values())
			.collect(Collectors.toMap(EnumXLSColumnHeader::getValue, Function.identity()));

	public static EnumXLSColumnHeader fromName(final String name) {
		return names.get(name);
	}

	public static EnumXLSColumnHeader fromValue(final int value) {
		return values.get(value);
	}

	public String toString() {
		return name;
	}

	public EnumXLSMeasure toType() {
		return type;
	}

	public static EnumXLSColumnHeader findByText(String abbr) {
		for (EnumXLSColumnHeader v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
