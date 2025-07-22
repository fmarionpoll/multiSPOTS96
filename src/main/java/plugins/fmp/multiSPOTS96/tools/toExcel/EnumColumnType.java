package plugins.fmp.multiSPOTS96.tools.toExcel;

public enum EnumColumnType {
	SPOT("spot"), MOVE("move"), COMMON("common"), DESCRIPTOR("descriptor"), MEASURE("measure");

	private String label;

	EnumColumnType(String label) {
		this.label = label;
	}

	public String toString() {
		return label;
	}
}
