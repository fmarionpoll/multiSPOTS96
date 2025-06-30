package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Color;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;

public class SpotTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6325792669154093747L;
	private JComboBoxExperiment expList = null;
	String columnNames[] = { "Spot name", "pixels", "uL", "IDCage", "Pos", "Row", "Col", "Stimulus", "Concentration",
			"Color" };

	public SpotTableModel(JComboBoxExperiment expList) {
		super();
		this.expList = expList;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			return exp.cagesArray.getTotalNumberOfSpots();
		}
		return 0;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
		case 7:
		case 8:
			return String.class;
		case 2:
			return Double.class;
		case 9:
			return Color.class;
		default:
			return Integer.class;
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex > 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Spot spot = getSpotAt(rowIndex);
		if (spot != null) { // && spot.prop != null
			switch (columnIndex) {
			case 0:
				return spot.getRoi().getName(); // string
			case 1:
				return spot.prop.spotNPixels;
			case 2:
				return spot.prop.spotVolume;
			case 3:
				return spot.prop.cageID;
			case 4:
				return spot.prop.cagePosition;
			case 5:
				return spot.prop.cageRow;
			case 6:
				return spot.prop.cageColumn;
			case 7:
				return spot.prop.stimulus; // string
			case 8:
				return spot.prop.concentration; // string
			case 9:
				return spot.prop.color;
			}
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Spot spot = getSpotAt(rowIndex);
		if (spot != null && spot.prop != null) {
			switch (columnIndex) {

			case 0:
				spot.getRoi().setName(aValue.toString());
				break;
			case 1:
				spot.prop.spotNPixels = (int) aValue;
				break;
			case 2:
				spot.prop.spotVolume = (double) aValue;
				break;
			case 3:
				spot.prop.cageID = (int) aValue;
				break;
			case 4:
				spot.prop.cagePosition = (int) aValue;
				break;
			case 5:
				spot.prop.cageRow = (int) aValue;
				break;
			case 6:
				spot.prop.cageColumn = (int) aValue;
				break;
			case 7:
				spot.prop.stimulus = aValue.toString();
				break;
			case 8:
				spot.prop.concentration = aValue.toString();
				break;
			case 9:
				spot.prop.color = (Color) aValue;
				spot.getRoi().setColor(spot.prop.color);
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
