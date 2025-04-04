package plugins.fmp.multiSPOTS96.experiment.spots;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.tools.JComponents.SpotColorEditor;
import plugins.fmp.multiSPOTS96.tools.JComponents.SpotColorRenderer;

public class SpotTable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public SpotTableModel tableModelSpot = null;
	// Table Default Colors
	Color cellsOrigBackColor;
	Color cellsOrigForeColor;

	public SpotTable(MultiSPOTS96 parent0) {
		cellsOrigBackColor = this.getBackground();
		cellsOrigForeColor = this.getForeground();
		tableModelSpot = new SpotTableModel(parent0.expListCombo);
		setModel(tableModelSpot);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setPreferredScrollableViewportSize(new Dimension(500, 400));
		setFillsViewportHeight(true);

		setDefaultRenderer(Color.class, new SpotColorRenderer(true));
		setDefaultEditor(Color.class, new SpotColorEditor());

		TableColumnModel columnModel = getColumnModel();

		// columnModel.getColumn(0).setPreferredWidth(65);
		columnModel.getColumn(1).setPreferredWidth(15);
		columnModel.getColumn(2).setPreferredWidth(15);
		columnModel.getColumn(3).setPreferredWidth(15);
		columnModel.getColumn(4).setPreferredWidth(25);
		columnModel.getColumn(5).setPreferredWidth(15);
		columnModel.getColumn(8).setPreferredWidth(15);
	}

}
