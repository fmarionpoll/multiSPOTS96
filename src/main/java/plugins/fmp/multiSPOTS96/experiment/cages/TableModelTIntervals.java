package plugins.fmp.multiSPOTS96.experiment.cages;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.TInterval;
import plugins.fmp.multiSPOTS96.experiment.TIntervalsArray;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;

public class TableModelTIntervals extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JComboBoxExperiment expList = null;
	private final String columnNames[] = { "Starting at frame" };
	private ArrayList<TInterval> intervals = null;

	public TableModelTIntervals(JComboBoxExperiment expList) {
		super();
		this.expList = expList;
	}

	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0) {
			Experiment exp = (Experiment) expList.getSelectedItem();
			TIntervalsArray tIntervals = exp.cagesArray.getCagesListTimeIntervals();
			if (tIntervals == null)
				return 0;
			intervals = exp.cagesArray.getCagesListTimeIntervals().intervals;
			return intervals.size();
		}
		return 0;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return Long.class;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TInterval interval = intervals.get(rowIndex);
		Long value;
		if (columnIndex == 0)
			value = interval.start;
		else
			value = interval.end;
		return value;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		TInterval interval = intervals.get(rowIndex);
		if (columnIndex == 0)
			interval.start = (long) aValue;
		else
			interval.end = (long) aValue;
	}

}