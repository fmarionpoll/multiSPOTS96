package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotTable;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;

public class InfosSpotTable extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8611587540329642259L;
	IcyFrame dialogFrame = null;
	private SpotTable spotTable = null;
	private JButton copyButton = new JButton("Copy table");
	private JButton pasteButton = new JButton("Paste");
	private JButton nPixelsButton = new JButton("Get n pixels");

	private JButton duplicateRowAtCagePositionButton = new JButton("Row at cage pos");
	private JButton duplicatePreviousButton = new JButton("Row above");
	private JButton duplicateCageButton = new JButton("Cage");
	private JButton duplicateAllButton = new JButton("Cell to all");

	private MultiSPOTS96 parent0 = null;
	private SpotsArray allSpotsCopy = null;

	public void initialize(MultiSPOTS96 parent0) {
		this.parent0 = parent0;

		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(copyButton);
		panel1.add(pasteButton);
		panel1.add(nPixelsButton);
		topPanel.add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel("Duplicate:"));
		panel2.add(duplicateRowAtCagePositionButton);
		panel2.add(duplicateAllButton);
		panel2.add(duplicateCageButton);
		panel2.add(duplicatePreviousButton);

		topPanel.add(panel2);

		JPanel tablePanel = new JPanel();
		spotTable = new SpotTable(parent0);
		tablePanel.add(new JScrollPane(spotTable));

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
			}
		});

		nPixelsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					measureNPixelsForAllSpots(exp);
			}
		});

		duplicateRowAtCagePositionButton.addActionListener(new ActionListener() {
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

		duplicatePreviousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					duplicatePreviousRow(exp);
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
		spotTable.spotTableModel.fireTableDataChanged();
	}

	void close() {
		dialogFrame.close();
	}

	private void measureNPixelsForAllSpots(Experiment exp) {
		int columnIndex = 3;
		for (Cage cage : exp.cagesArray.cagesList)
			for (Spot spot : cage.spotsArray.spotsList) {
				try {
					int value = (int) spot.getRoi().getNumberOfPoints();
					int iID = exp.cagesArray.getSpotGlobalPosition(spot);
					spotTable.spotTableModel.setValueAt(value, iID, columnIndex);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}

	private void duplicatePos(Experiment exp) {
		int rowIndex = spotTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		String spotName = (String) spotTable.getValueAt(rowIndex, 0);
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
				int iID = exp.cagesArray.getSpotGlobalPosition(spot);
				int columnIndex = 4;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.spotVolume, iID, columnIndex);
				columnIndex++;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.stimulus, iID, columnIndex);
				columnIndex++;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.concentration, iID, columnIndex);
				columnIndex++;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.color, iID, columnIndex);
			}
		}
	}

	private void duplicatePreviousRow(Experiment exp) {
		int rowTo = spotTable.getSelectedRow();
		if (rowTo < 0)
			return;

		int rowFrom = rowTo - 1;
		if (rowFrom < 0)
			return;

		String spotName = (String) spotTable.getValueAt(rowFrom, 0);
		Spot spotFrom = exp.cagesArray.getSpotFromROIName(spotName);
		if (spotFrom == null) {
			System.out.println("spot not found or invalid: " + spotName);
			return;
		}

		spotName = (String) spotTable.getValueAt(rowTo, 0);
		Spot spotTo = exp.cagesArray.getSpotFromROIName(spotName);
		if (spotTo == null) {
			System.out.println("spot not found or invalid: " + spotName);
			return;
		}
		int iID = exp.cagesArray.getSpotGlobalPosition(spotTo);
		int columnIndex = 4;
		spotTable.spotTableModel.setValueAt(spotFrom.prop.spotVolume, iID, columnIndex);
		columnIndex++;
		spotTable.spotTableModel.setValueAt(spotFrom.prop.stimulus, iID, columnIndex);
		columnIndex++;
		spotTable.spotTableModel.setValueAt(spotFrom.prop.concentration, iID, columnIndex);
		columnIndex++;
		spotTable.spotTableModel.setValueAt(spotFrom.prop.color, iID, columnIndex);
	}

	private void duplicateAll(Experiment exp) {
		int columnIndex = spotTable.getSelectedColumn();
		int rowIndex = spotTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		Object value = spotTable.spotTableModel.getValueAt(rowIndex, columnIndex);
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				int iID = exp.cagesArray.getSpotGlobalPosition(spot);
				spotTable.spotTableModel.setValueAt(value, iID, columnIndex);
			}
		}
	}

	private void duplicateCage(Experiment exp) {
		int rowIndex = spotTable.getSelectedRow();
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
				if (i >= cageFrom.spotsArray.spotsList.size())
					continue;
				Spot spotFrom = cageFrom.spotsArray.spotsList.get(i);
				int iID = exp.cagesArray.getSpotGlobalPosition(spot);
				int columnIndex = 4;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.spotVolume, iID, columnIndex);
				columnIndex++;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.stimulus, iID, columnIndex);
				columnIndex++;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.concentration, iID, columnIndex);
				columnIndex++;
				spotTable.spotTableModel.setValueAt(spotFrom.prop.color, iID, columnIndex);
			}
		}
	}

	public void selectRowFromSpot(Spot spot) {
		String spotName = spot.getRoi().getName();
		int nrows = spotTable.getRowCount();
		int selectedRow = -1;
		for (int i = 0; i < nrows; i++) {
			String name = (String) spotTable.getValueAt(i, 0);
			if (name.equals(spotName)) {
				selectedRow = i;
				break;
			}
		}
		if (selectedRow >= 0) {
			spotTable.setRowSelectionInterval(selectedRow, selectedRow);
			Rectangle rect = new Rectangle(spotTable.getCellRect(selectedRow, 0, true));
			rect.height = rect.height * 2;
			spotTable.scrollRectToVisible(rect);
		}
	}
}
