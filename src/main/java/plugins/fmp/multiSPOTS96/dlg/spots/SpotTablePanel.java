package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;
import plugins.fmp.multiSPOTS96.tools.JComponents.TableModelSpot;


// look at these pages:
// https://www.codejava.net/java-se/swing/how-to-create-jcombobox-cell-editor-for-jtable
// https://stackoverflow.com/questions/14355712/adding-jcombobox-to-a-jtable-cell
// https://forums.oracle.com/ords/apexds/post/make-a-combobox-appear-in-just-one-cell-in-a-jtable-column-9798


public class SpotTablePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8611587540329642259L;
	IcyFrame dialogFrame = null;
	private JTable jTable = new JTable();
	private TableModelSpot spotTableModel = null;
	private JButton copyButton = new JButton("Copy table");
	private JButton pasteButton = new JButton("Paste");
	private JButton duplicatePosButton = new JButton("Duplicate row at cage pos");
	private JButton duplicateCageButton = new JButton("Duplicate cage");

	private JButton duplicateAllButton = new JButton("Duplicate cell to all");
	private JButton getNfliesButton = new JButton("Get n flies from cage");
	private JButton nPixelsButton = new JButton("Get n pixels");
	private MultiSPOTS96 parent0 = null;
	private SpotsArray allSpotsCopy = null;

	public void initialize(MultiSPOTS96 parent0) {
		this.parent0 = parent0;

		spotTableModel = new TableModelSpot(parent0.expListCombo);
		jTable.setModel(spotTableModel);
		jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jTable.setPreferredScrollableViewportSize(new Dimension(500, 400));
		jTable.setFillsViewportHeight(true);
		TableColumnModel columnModel = jTable.getColumnModel();

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 0; i < spotTableModel.getColumnCount(); i++) {
			TableColumn col = columnModel.getColumn(i);
			col.setCellRenderer(centerRenderer);
		}
		columnModel.getColumn(0).setPreferredWidth(25);
		columnModel.getColumn(1).setPreferredWidth(15);
		columnModel.getColumn(2).setPreferredWidth(15);
		columnModel.getColumn(3).setPreferredWidth(15);
		columnModel.getColumn(4).setPreferredWidth(25);
		columnModel.getColumn(5).setPreferredWidth(15);

		JScrollPane scrollPane = new JScrollPane(jTable);

		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(copyButton);
		panel1.add(pasteButton);
		panel1.add(duplicatePosButton);
		panel1.add(duplicateAllButton);
		panel1.add(duplicateCageButton);
		topPanel.add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(getNfliesButton);
		panel2.add(nPixelsButton);

		topPanel.add(panel2);

		JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);

		dialogFrame = new IcyFrame("Spots properties", true, true);
		dialogFrame.add(topPanel, BorderLayout.NORTH);
		dialogFrame.add(tablePanel, BorderLayout.CENTER);

		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.setLocation(new Point(5, 5));
		dialogFrame.setVisible(true);
		defineActionListeners();

		pasteButton.setEnabled(false);
	}

	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					allSpotsCopy = exp.cagesArray.getAllSpotsArray();
					pasteButton.setEnabled(true);
				}
			}
		});

		pasteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					SpotsArray spotsArray = exp.cagesArray.getAllSpotsArray();
					allSpotsCopy.pasteSpotsInfos(spotsArray);
				}
				spotTableModel.fireTableDataChanged();
			}
		});

		nPixelsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					setSpotsNPixels(exp);
					spotTableModel.fireTableDataChanged();
				}
			}
		});

		duplicatePosButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					duplicatePos(exp);
			}
		});

		duplicateCageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					duplicateCage(exp);
			}
		});

		duplicateAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					duplicateAll(exp);
				}
			}
		});

		getNfliesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.cagesArray.cagesList.size() > 0) {
					exp.cagesArray.transferNFliesFromCagesToSpots();
					spotTableModel.fireTableDataChanged();
				}
			}
		});

	}

	void close() {
		dialogFrame.close();
	}

	private void setSpotsNPixels(Experiment exp) {
		for (Cage cage : exp.cagesArray.cagesList)
			for (Spot spot : cage.spotsArray.spotsList) {
				try {
					spot.prop.spotNPixels = (int) spot.getRoi().getNumberOfPoints();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}

	private void duplicatePos(Experiment exp) {
		int rowIndex = jTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		String spotName = (String) jTable.getValueAt(rowIndex, 0);
		Spot spotFrom = exp.cagesArray.getSpotFromROIName(spotName);
		if (spotFrom == null) {
			System.out.println("spot not found: " + spotName);
			return;
		}
		int cagePosition = spotFrom.prop.cagePosition;
		int cageID = spotFrom.prop.cageID;

		for (Cage cage : exp.cagesArray.cagesList) {
			if (cage.prop.cageID == cageID)
				continue;
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.prop.cagePosition != cagePosition)
					continue;
				spot.prop.spotNFlies = spotFrom.prop.spotNFlies;
				spot.prop.spotVolume = spotFrom.prop.spotVolume;
				spot.prop.spotStim = spotFrom.prop.spotStim;
				spot.prop.spotConc = spotFrom.prop.spotConc;
			}
		}
	}

	private void duplicateAll(Experiment exp) {
		int columnIndex = jTable.getSelectedColumn();
		int rowIndex = jTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		Spot spotFrom = exp.cagesArray.getSpotAtGlobalIndex(rowIndex);
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().getName().equals(spotFrom.getRoi().getName()))
					continue;
				switch (columnIndex) {
				case 3:
					spot.prop.spotNFlies = spotFrom.prop.spotNFlies;
					break;
				case 4:
					spot.prop.spotNPixels = spotFrom.prop.spotNPixels;
					break;
				case 5:
					spot.prop.spotVolume = spotFrom.prop.spotVolume;
					break;
				case 6:
					spot.prop.spotStim = spotFrom.prop.spotStim;
					break;
				case 7:
					spot.prop.spotConc = spotFrom.prop.spotConc;
					break;
				default:
					break;
				}
			}
		}
	}

	private void duplicateCage(Experiment exp) {
		int rowIndex = jTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		Spot spotFromSelectedRow = exp.cagesArray.getSpotAtGlobalIndex(rowIndex);
		int cageIDFrom = spotFromSelectedRow.prop.cageID;
		Cage cageFrom = exp.cagesArray.getCageFromSpotRoiName(spotFromSelectedRow.getRoi().getName());

		for (Cage cage : exp.cagesArray.cagesList) {
			if (cage.prop.cageID == cageIDFrom)
				continue;

			for (int i = 0; i < cage.spotsArray.spotsList.size(); i++) {
				Spot spot = cage.spotsArray.spotsList.get(i);
				Spot spotFrom = cageFrom.spotsArray.spotsList.get(i);
				spot.prop.spotNFlies = spotFrom.prop.spotNFlies;
				spot.prop.spotVolume = spotFrom.prop.spotVolume;
				spot.prop.spotStim = spotFrom.prop.spotStim;
				spot.prop.spotConc = spotFrom.prop.spotConc;
			}
		}

	}

}
