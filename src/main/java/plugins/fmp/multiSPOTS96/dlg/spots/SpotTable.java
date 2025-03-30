package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.tools.JComponents.ButtonColumnRenderer;
import plugins.fmp.multiSPOTS96.tools.JComponents.SpotTableModel;

//look at these pages:
//https://www.codejava.net/java-se/swing/how-to-create-jcombobox-cell-editor-for-jtable
//https://stackoverflow.com/questions/14355712/adding-jcombobox-to-a-jtable-cell
//https://forums.oracle.com/ords/apexds/post/make-a-combobox-appear-in-just-one-cell-in-a-jtable-column-9798

public class SpotTable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public SpotTableModel spotTableModel = null;
	// Table Default Colors
	Color cellsOrigBackColor;
	Color cellsOrigForeColor;

	public SpotTable(MultiSPOTS96 parent0) {
		cellsOrigBackColor = this.getBackground();
		cellsOrigForeColor = this.getForeground();
		spotTableModel = new SpotTableModel(parent0.expListCombo);
		setModel(spotTableModel);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setPreferredScrollableViewportSize(new Dimension(500, 400));
		setFillsViewportHeight(true);

		TableColumnModel columnModel = getColumnModel();
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		ButtonColumnRenderer colorButton = new ButtonColumnRenderer();

		for (int i = 0; i < spotTableModel.getColumnCount(); i++) {
			TableColumn col = columnModel.getColumn(i);
			if (i != 8)
				col.setCellRenderer(centerRenderer);
			else
				col.setCellRenderer(colorButton);
		}

		// columnModel.getColumn(0).setPreferredWidth(65);
		columnModel.getColumn(1).setPreferredWidth(15);
		columnModel.getColumn(2).setPreferredWidth(15);
		columnModel.getColumn(3).setPreferredWidth(15);
		columnModel.getColumn(4).setPreferredWidth(25);
		columnModel.getColumn(5).setPreferredWidth(15);
		// columnModel.getColumn(6).setPreferredWidth(15);
		columnModel.getColumn(8).setPreferredWidth(15);
	}

//	@Override
//	public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
//		Color cellBackColor = cellsOrigBackColor;
//		Color cellForeColor = cellsOrigForeColor;
//		JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);
//
//		if (columnIndex == 8) {
//			cellBackColor = (Color) getValueAt(rowIndex, 8);
//			cellForeColor = properTextColor(cellBackColor);
//		}
//		component.setBackground(cellBackColor);
//		component.setForeground(cellForeColor);
//		return component;
//	}

	/**
	 * Returns either the Color WHITE or the Color BLACK dependent upon the
	 * brightness of what the supplied background color might be. If the background
	 * color is too dark then WHITE is returned. If the background color is too
	 * bright then BLACK is returned.<br>
	 * <br>
	 *
	 * @param currentBackgroundColor (Color Object) The background color text will
	 *                               reside on.<br>
	 *
	 * @return (Color Object) The color WHITE or the Color BLACK.
	 */

//	public static Color properTextColor(Color currentBackgroundColor) {
//		double L; // Holds the brightness value for the supplied color
//		Color determinedColor; // Default
//
//		// Calculate color brightness from supplied color.
//		int r = currentBackgroundColor.getRed();
//		int g = currentBackgroundColor.getGreen();
//		int b = currentBackgroundColor.getBlue();
//		L = (int) Math.sqrt((r * r * .241) + (g * g * .691) + (b * b * .068));
//
//		// Return the required text color to suit the
//		// supplied background color.
//		if (L > 129) {
//			determinedColor = Color.decode("#000000"); // White
//		} else {
//			determinedColor = Color.decode("#FFFFFF"); // Black
//		}
//		return determinedColor;
//	}

}
