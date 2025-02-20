package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
import plugins.fmp.multiSPOTS96.tools.JComponents.TableModelSpot;

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
	private JButton duplicateLRButton = new JButton("Duplicate cell to pos");
	private JButton duplicateCageStimButton = new JButton("Duplicate cage stim");

	private JButton exchangeLRButton = new JButton("Exchg L/R");

	private JButton duplicateAllButton = new JButton("Duplicate cell to all");
	private JButton getNfliesButton = new JButton("Get n flies from cage");
	private JButton nPixelsButton = new JButton("Get n pixels");
	private MultiSPOTS96 parent0 = null;
	private ArrayList<Cage> cagesArrayCopy = null;

	public void initialize(MultiSPOTS96 parent0, ArrayList<Cage> cageCopy) {
		this.parent0 = parent0;
		cagesArrayCopy = cageCopy;

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
		panel1.add(duplicateLRButton);
		panel1.add(duplicateAllButton);
		panel1.add(exchangeLRButton);
		topPanel.add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(getNfliesButton);
		panel2.add(nPixelsButton);
		panel2.add(duplicateCageStimButton);
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

		pasteButton.setEnabled(cagesArrayCopy.size() > 0);
	}

	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					copyInfos(exp);
			}
		});

		pasteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					pasteInfos(exp);
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

		duplicateLRButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					duplicateLR(exp);
			}
		});

		duplicateCageStimButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					duplicateCage(exp);
			}
		});

		exchangeLRButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null || (exp.cagesArray.nColumnsPerCage * exp.cagesArray.nRowsPerCage) != 2)
					return;
				exchangeLR(exp);
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

	private void exchangeLR(Experiment exp) {
		int columnIndex = jTable.getSelectedColumn();
		if (columnIndex < 0)
			columnIndex = 5;
		for (Cage cage : exp.cagesArray.cagesList) {
			int side0 = cage.spotsArray.spotsList.get(0).cagePosition;
			Spot spot0 = new Spot();
			spot0.copySpot(cage.spotsArray.spotsList.get(0), false);

			Spot spot1 = new Spot();
			spot1.copySpot(cage.spotsArray.spotsList.get(1), false);

			for (Spot spot : cage.spotsArray.spotsList) {
				if ((spot.cagePosition == side0))
					copySingleSpotValue(spot1, spot, columnIndex);
				else
					copySingleSpotValue(spot0, spot, columnIndex);
			}
		}
	}

	private void copySingleSpotValue(Spot spotFrom, Spot spotTo, int columnIndex) {
		switch (columnIndex) {
		case 3:
			spotTo.spotNFlies = spotFrom.spotNFlies;
			break;
		case 4:
			spotTo.spotNPixels = spotFrom.spotNPixels;
			break;
		case 5:
			spotTo.spotVolume = spotFrom.spotVolume;
			break;
		case 6:
			spotTo.spotStim = spotFrom.spotStim;
			break;
		case 7:
			spotTo.spotConc = spotFrom.spotConc;
			break;
		default:
			break;
		}

	}

	private void copyInfos(Experiment exp) {
		cagesArrayCopy.clear();
		for (Cage cage : exp.cagesArray.cagesList)
			cagesArrayCopy.add(cage);
		pasteButton.setEnabled(true);
	}

	private void pasteInfos(Experiment exp) {
		exp.cagesArray.copy(cagesArrayCopy, false);
	}

	private void setSpotsNPixels(Experiment exp) {
		for (Cage cage : exp.cagesArray.cagesList)
			for (Spot spot : cage.spotsArray.spotsList) {
				try {
					spot.spotNPixels = (int) spot.getRoi().getNumberOfPoints();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}

	private void duplicateLR(Experiment exp) {
		int rowIndex = jTable.getSelectedRow();
		int columnIndex = jTable.getSelectedColumn();
		if (rowIndex < 0)
			return;

		for (Cage cage : exp.cagesArray.cagesList) {
			Spot spot0 = cage.spotsArray.spotsList.get(rowIndex);
			int cageIndex = spot0.cagePosition;

			for (Spot spot : cage.spotsArray.spotsList) {
				if (spot.getRoi().getName().equals(spot0.getRoi().getName()))
					continue;

				if (spot.cagePosition != cageIndex)
					continue;

				switch (columnIndex) {
				case 3:
					spot.spotNFlies = spot0.spotNFlies;
					break;
				case 4:
					spot.spotNPixels = spot0.spotNPixels;
					break;
				case 5:
					spot.spotVolume = spot0.spotVolume;
					break;
				case 6:
					spot.spotStim = spot0.spotStim;
					break;
				case 7:
					spot.spotConc = spot0.spotConc;
					break;
				default:
					break;
				}
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
					spot.spotNFlies = spotFrom.spotNFlies;
					break;
				case 4:
					spot.spotNPixels = spotFrom.spotNPixels;
					break;
				case 5:
					spot.spotVolume = spotFrom.spotVolume;
					break;
				case 6:
					spot.spotStim = spotFrom.spotStim;
					break;
				case 7:
					spot.spotConc = spotFrom.spotConc;
					break;
				default:
					break;
				}
			}
		}
	}

	private void duplicateCage(Experiment exp) {
		int columnIndex = jTable.getSelectedColumn();
		int rowIndex = jTable.getSelectedRow();
		if (rowIndex < 0)
			return;

		Spot spotFrom = exp.cagesArray.getSpotAtGlobalIndex(rowIndex);
		int cageFrom = spotFrom.cageID;
		int cageTo = -1;

		int nSpotsPerCage = getCageNSpots(exp, cageFrom);
		int indexFirstSpotOfCageFrom = getIndexFirstSpotOfCage(exp, cageFrom);
		int indexFirstSpotOfCageTo = -1;

		for (Cage cage : exp.cagesArray.cagesList) {
			for (int i = 0; i < cage.spotsArray.spotsList.size(); i++) {
				Spot spot = cage.spotsArray.spotsList.get(i);
				if (spot.cageID == cageFrom)
					continue;

				if (spot.cageID != cageTo) {
					cageTo = spot.cageID;
					indexFirstSpotOfCageTo = getIndexFirstSpotOfCage(exp, cageTo);
				}

				if (getCageNSpots(exp, spot.cageID) != nSpotsPerCage)
					continue;

				int indexFrom = i - indexFirstSpotOfCageTo + indexFirstSpotOfCageFrom;
				Spot spot0 = cage.spotsArray.spotsList.get(indexFrom);

				switch (columnIndex) {
				case 3:
					spot.spotNFlies = spot0.spotNFlies;
					break;
				case 4:
					spot.spotNPixels = spot0.spotNPixels;
					break;
				case 5:
					spot.spotVolume = spot0.spotVolume;
					break;
				case 6:
					spot.spotStim = spot0.spotStim;
					break;
				case 7:
					spot.spotConc = spot0.spotConc;
					break;
				default:
					break;
				}
			}
		}

	}

	private int getCageNSpots(Experiment exp, int cageID) {
		int nSpots = 0;
		Cage cage = exp.cagesArray.getCageFromID(cageID);
		if (cage != null)
			nSpots = cage.spotsArray.spotsList.size();
		return nSpots;
	}

	private int getIndexFirstSpotOfCage(Experiment exp, int cageID) {
		int index = -1;
		Cage cage = exp.cagesArray.getCageFromID(cageID);
		if (cage != null) {
			Spot spot = cage.spotsArray.spotsList.get(0);
			index = spot.spotArrayIndex;
		}
		return index;
	}
}
