package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;

public class CageTablePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7599620793495187279L;
	IcyFrame dialogFrame = null;
	private CageTable jTable = null;
	private CageTableModel viewModel = null;
	private JButton copyButton = new JButton("Copy table");
	private JButton pasteButton = new JButton("Paste");
	private JButton duplicateAllButton = new JButton("Duplicate cell to all");
	private MultiSPOTS96 parent0 = null;
	private CagesArray cagesArrayCopy = null;

	// -------------------------

	public void initialize(MultiSPOTS96 parent0) {
		this.parent0 = parent0;

		viewModel = new CageTableModel(parent0.expListCombo);
		jTable = new CageTable(parent0);
		jTable.setModel(viewModel);
		jTable.setPreferredScrollableViewportSize(new Dimension(500, 400));
		jTable.setFillsViewportHeight(true);
		TableColumnModel columnModel = jTable.getColumnModel();
		for (int i = 0; i < 2; i++)
			setFixedColumnProperties(columnModel.getColumn(i));
		JScrollPane scrollPane = new JScrollPane(jTable);

		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(copyButton);
		panel1.add(pasteButton);
		topPanel.add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(duplicateAllButton);
		topPanel.add(panel2);

		JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);

		dialogFrame = new IcyFrame("Cages properties", true, true);
		dialogFrame.add(topPanel, BorderLayout.NORTH);
		dialogFrame.add(tablePanel, BorderLayout.CENTER);

		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);
		defineActionListeners();

		pasteButton.setEnabled(cagesArrayCopy != null);
	}

	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					cagesArrayCopy = exp.cagesArray;
					pasteButton.setEnabled(true);
				}
			}
		});

		pasteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					for (Cage cageFrom : cagesArrayCopy.cagesList) {
						cageFrom.valid = false;
						for (Cage cageTo : exp.cagesArray.cagesList) {
							if (!cageFrom.getCageRoi().getName().equals(cageTo.getCageRoi().getName()))
								continue;
							cageFrom.valid = true;
							cageTo.prop.copy(cageFrom.prop);
						}
					}
					viewModel.fireTableDataChanged();
				}
			}
		});

		duplicateAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					int rowIndex = jTable.getSelectedRow();
					int columnIndex = jTable.getSelectedColumn();
					if (rowIndex >= 0) {
						Cage cage0 = exp.cagesArray.cagesList.get(rowIndex);
						for (Cage cage : exp.cagesArray.cagesList) {
							if (cage.getCageRoi().getName().equals(cage0.getCageRoi().getName()))
								continue;
							switch (columnIndex) {
							case 1:
								cage.prop.cageNFlies = cage0.prop.cageNFlies;
								break;
							case 2:
								cage.prop.strCageStrain = cage0.prop.strCageStrain;
								break;
							case 3:
								cage.prop.strCageSex = cage0.prop.strCageSex;
								break;
							case 4:
								cage.prop.cageAge = cage0.prop.cageAge;
								break;
							case 5:
								cage.prop.strCageComment = cage0.prop.strCageComment;
								break;
							case 6:
								cage.prop.cageColor = cage0.prop.cageColor;
							default:
								break;
							}
						}
					}
				}
			}
		});
	}

	public void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			parent0.dlgSpots.tabFile.saveSpotsArray_file(exp);
		}
	}

	private void setFixedColumnProperties(TableColumn column) {
		column.setResizable(false);
		column.setPreferredWidth(50);
		column.setMaxWidth(50);
		column.setMinWidth(30);
	}

}
