package plugins.fmp.multiSPOTS96.tools.JComponents;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;

public class TableModelSpot extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6325792669154093747L;
	private JComboBoxExperiment expList = null;
	String columnNames[] = { "Name", "IDCage", "PosCage", "N flies", "N pixels", "Volume", "Stimulus",
			"Concentration" };

	// "O-Name", "1-CageID", "2-CageIndex", "3-N flies", "4-N pixels", "5-Volume",
	// "6-Stimulus",
//	"7-Concentration"
	public TableModelSpot(JComboBoxExperiment expList) {
		super();
		this.expList = expList;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return Integer.class;
		case 2:
			return Integer.class;
		case 3:
			return Integer.class;
		case 4:
			return Integer.class;
		case 5:
			return Double.class;
		case 6:
			return String.class;
		case 7:
			return String.class;
		}
		return String.class;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			return exp.cagesArray.cagesList.size() * (exp.cagesArray.nColumnsPerCage * exp.cagesArray.nRowsPerCage);
		}
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Spot spot = getSpotAt(rowIndex);
		if (spot != null) {
			switch (columnIndex) {
			case 0:
				return spot.getRoi().getName();
			case 1:
				return spot.prop.cageID;
			case 2:
				return spot.prop.cagePosition;
			case 3:
				return spot.prop.spotNFlies;
			case 4:
				return spot.prop.spotNPixels;
			case 5:
				return spot.prop.spotVolume;
			case 6:
				return spot.prop.spotStim;
			case 7:
				return spot.prop.spotConc;
			}
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return false;
		default:
			return true;
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Spot spot = getSpotAt(rowIndex);
		if (spot != null) {
			switch (columnIndex) {
			case 0:
				spot.getRoi().setName(aValue.toString());
				break;
			case 1:
				spot.prop.cageID = (int) aValue;
				break;
			case 2:
				spot.prop.cagePosition = (int) aValue;
				break;
			case 3:
				spot.prop.spotNFlies = (int) aValue;
				break;
			case 4:
				spot.prop.spotNPixels = (int) aValue;
				break;
			case 5:
				spot.prop.spotVolume = (double) aValue;
				break;
			case 6:
				spot.prop.spotStim = aValue.toString();
				break;
			case 7:
				spot.prop.spotConc = aValue.toString();
				break;
			}
		}
	}

	private Spot getSpotAt(int rowIndex) {
		Spot spot = null;
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			spot = exp.cagesArray.getSpotAtGlobalIndex(rowIndex);
		}
		return spot;
	}
}
