package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.Color;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;

public class CageTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3501225818220221949L;
	private JComboBoxExperiment expList = null;
	String columnNames[] = { "Name", "N flies", "Strain", "Sex", "Age", "Comment", "Color" };
	public Color colorTable[] = { Color.GRAY, Color.WHITE };

	public CageTableModel(JComboBoxExperiment expList) {
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
			return exp.cagesArray.cagesList.size();
		}
		return 0;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Cage cage = null;
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			cage = exp.cagesArray.cagesList.get(rowIndex);
		}
		if (cage != null) {
			switch (columnIndex) {
			case 0:
				return cage.getRoi().getName();
			case 1:
				return cage.prop.cageNFlies;
			case 2:
				return cage.prop.flyStrain;
			case 3:
				return cage.prop.flySex;
			case 4:
				return cage.prop.flyAge;
			case 5:
				return cage.prop.comment;
			case 6:
				return cage.prop.color;
			}
		}
		return null;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return String.class;
		case 1:
			return Integer.class;
		case 2:
			return String.class;
		case 3:
			return String.class;
		case 4:
			return Integer.class;
		case 5:
			return String.class;
		case 6:
			return Color.class;
		}
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex < 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Cage cage = null;
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			cage = exp.cagesArray.cagesList.get(rowIndex);
		}
		if (cage != null) {
			switch (columnIndex) {
			case 0:
				cage.getRoi().setName(aValue.toString());
				break;
			case 1:
				cage.prop.cageNFlies = (int) aValue;
				cage.prop.color = colorTable[((int) aValue) % 2];
				cage.getRoi().setColor(cage.prop.color);
				break;
			case 2:
				cage.prop.flyStrain = aValue.toString();
				break;
			case 3:
				cage.prop.flySex = aValue.toString();
				break;
			case 4:
				cage.prop.flyAge = (int) aValue;
				break;
			case 5:
				cage.prop.comment = aValue.toString();
				break;
			case 6:
				cage.prop.color = (Color) aValue;
				break;
			}
		}
	}

}
