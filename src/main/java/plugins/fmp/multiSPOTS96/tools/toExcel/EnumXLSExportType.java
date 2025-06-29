package plugins.fmp.multiSPOTS96.tools.toExcel;

public enum EnumXLSExportType {

	XYIMAGE("xy-image", "mm", "xy image"), XYTOPCAGE("xy-topcage", "mm", "xy top cage"),
	XYTIPCAPS("xy-tipcaps", "mm", "xy tip capillaries"), ELLIPSEAXES("ellipse-axes", "mm", "Ellipse of axes"),
	DISTANCE("distance", "mm", "Distance between consecutive points"), ISALIVE("_alive", "yes/no", "Fly alive or not"),
	SLEEP("sleep", "yes, no", "Fly sleeping"),

	AREA_SUM("AREA_SUM", "grey value", "Consumption (estimated/threshold)"),
	AREA_SUMCLEAN("AREA_SUMCLEAN", "grey value - no fly", "Consumption (estimated/threshold)"),
	AREA_OUT("AREA_OUT", "pixel grey value", "background"), AREA_DIFF("AREA_DIFF", "grey value - background", "diff"),

	AREA_FLYPRESENT("AREA_FLYPRESENT", "boolean value", "Fly is present or not over the spot");
//	AREA_CNTPIX("AREA_CNTPIX", "n pixels", "Spot n pixels over threshold"),
//	AREA_SUM_LR("area sum_L+R", "sum grey", "SUM and PI"),
//	AREA_SUMCLEAN_LR("area sumclean_L+R", "sumclean grey", "SUM and PI");
//	AREA_CNTPIX_LR ("area cntpix_L+R", "n_pixels", "SUM and PI");

	private String label;
	private String unit;
	private String title;

	EnumXLSExportType(String label, String unit, String title) {
		this.label = label;
		this.unit = unit;
		this.title = title;
	}

	public String toString() {
		return label;
	}

	public String toUnit() {
		return unit;
	}

	public String toTitle() {
		return title;
	}

	public static EnumXLSExportType findByText(String abbr) {
		for (EnumXLSExportType v : values()) {
			if (v.toString().equals(abbr))
				return v;
		}
		return null;
	}
}
