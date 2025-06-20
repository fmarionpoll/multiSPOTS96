package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
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
	private JButton duplicatePosButton = new JButton("Duplicate row at cage pos");
	private JButton duplicatePreviousButton = new JButton("Duplicate previous row");
	private JButton duplicateCageButton = new JButton("Duplicate cage");

	private JButton duplicateAllButton = new JButton("Duplicate cell to all");
	private JButton nPixelsButton = new JButton("Get n pixels");
	private MultiSPOTS96 parent0 = null;
	private SpotsArray allSpotsCopy = null;

	public void initialize(MultiSPOTS96 parent0) {
		this.parent0 = parent0;

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
		panel2.add(nPixelsButton);
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
					setSpotsNPixels(exp);
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
				spot.prop.spotVolume = spotFrom.prop.spotVolume;
				spot.prop.spotStim = spotFrom.prop.spotStim;
				spot.prop.spotConc = spotFrom.prop.spotConc;
				spot.prop.spotColor = spotFrom.prop.spotColor;
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

		spotTo.prop.spotVolume = spotFrom.prop.spotVolume;
		spotTo.prop.spotStim = spotFrom.prop.spotStim;
		spotTo.prop.spotConc = spotFrom.prop.spotConc;
		spotTo.prop.spotColor = spotFrom.prop.spotColor;
	}

	private void duplicateAll(Experiment exp) {
		int columnIndex = spotTable.getSelectedColumn();
		int rowIndex = spotTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		Spot spotFrom = exp.cagesArray.getSpotAtGlobalIndex(rowIndex);
		for (Cage cage : exp.cagesArray.cagesList) {
			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().getName().equals(spotFrom.getRoi().getName()))
					continue;
				switch (columnIndex) {
				case 3:
					spot.prop.spotNPixels = spotFrom.prop.spotNPixels;
					break;
				case 4:
					spot.prop.spotVolume = spotFrom.prop.spotVolume;
					break;
				case 5:
					spot.prop.spotStim = spotFrom.prop.spotStim;
					break;
				case 6:
					spot.prop.spotConc = spotFrom.prop.spotConc;
					break;
				case 7:
					spot.prop.spotColor = spotFrom.prop.spotColor;
					break;
				default:
					break;
				}
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
				Spot spotFrom = cageFrom.spotsArray.spotsList.get(i);
				spot.prop.spotVolume = spotFrom.prop.spotVolume;
				spot.prop.spotStim = spotFrom.prop.spotStim;
				spot.prop.spotConc = spotFrom.prop.spotConc;
				spot.prop.spotColor = spotFrom.prop.spotColor;
			}
		}

	}

}
