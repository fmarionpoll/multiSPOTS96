package plugins.fmp.multiSPOTS96.tools.toExcel;

public enum EnumXLSMeasure {
	SPOT("spot"), MOVE("move"), COMMON("common");

	private String label;

	EnumXLSMeasure(String label) {
		this.label = label;
	}

	public String toString() {
		return label;
	}
}
