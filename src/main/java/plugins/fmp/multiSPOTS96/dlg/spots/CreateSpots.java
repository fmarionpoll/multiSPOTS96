package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentUtils;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DGeometryException;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DGrid;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DPolygonPlus;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DProcessingException;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DValidationException;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class CreateSpots extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;

	private JButton cageGridButton = new JButton("(1) Cage grid");
	private JComboBox<Integer> nRowsCombo = new JComboBox<Integer>(new Integer[] { 1, 2, 4 });
	private JComboBox<Integer> nColumnsCombo = new JComboBox<Integer>(new Integer[] { 1, 2, 4, 8 });
	private JButton selectButton = new JButton("(2) Select");

	private JButton createSpotsButton = new JButton("(3) Create spots");

	private MultiSPOTS96 parent0 = null;
	private ROI2DGrid roiGrid = null;
	private Point2D.Double referencePosition = null;

	private CreateSpotsArrayPanel spotsPanel = null;

	// ----------------------------------------------------------

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(cageGridButton);
		panel0.add(new JLabel("cols"));
		panel0.add(nColumnsCombo);
		panel0.add(new JLabel("rows"));
		panel0.add(nRowsCombo);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(selectButton);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(createSpotsButton);

		add(panel0);
		add(panel1);
		add(panel2);

		nRowsCombo.setSelectedItem(4);
		nColumnsCombo.setSelectedItem(8);

		defineActionListeners();
		this.parent0 = parent0;
	}

	private void defineActionListeners() {
		cageGridButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					exp.seqCamData.removeROIsContainingString("carre");
					exp.seqCamData.removeROIsContainingString("spot");
					Cage cageFound = exp.cagesArray.findFirstSelectedCage();
					if (cageFound == null)
						cageFound = exp.cagesArray.cagesList.get(0);

					if (cageFound != null) {
						exp.seqCamData.centerOnRoi(cageFound.getRoi());
						changeGrid(exp, cageFound);
					}

					if (spotsPanel == null) {
						spotsPanel = new CreateSpotsArrayPanel();
						int n_columns = (int) nColumnsCombo.getSelectedItem();
						int n_rows = (int) nRowsCombo.getSelectedItem();
						spotsPanel.initialize(parent0, n_columns, n_rows);
						spotsPanel.requestFocus();
					}
				}
			}
		});

		selectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					keepSelectedAreas(exp);
				}
			}
		});

		createSpotsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					exp.seqCamData.removeROIsContainingString("spot");
					createSpotsForAllCages(exp, roiGrid, referencePosition);
					ExperimentUtils.transferSpotsToCamDataSequence(exp);
					exp.seqCamData.removeROIsContainingString("carre");
				}
			}
		});

	}

	// ---------------------------------

	private void keepSelectedAreas(Experiment exp) {
		ArrayList<ROI2DPolygonPlus> listCarres = roiGrid.getAreaRois();
		for (ROI2DPolygonPlus roi : listCarres) {
			roi.setSelected(roi.isSelected());
			if (!roi.getSelected())
				exp.seqCamData.getSequence().removeROI(roi);
		}
	}

	private void createSpotsForAllCages(Experiment exp, ROI2DGrid roiGrid, Point2D.Double referenceCagePosition) {
		ArrayList<ROI2DPolygonPlus> listSelectedAreas = roiGrid.getSelectedAreaRois();
		for (Cage cage : exp.cagesArray.cagesList) {
			ROI2D cageRoi = cage.getRoi();
			ROI2DGrid cageGrid = createGrid(cageRoi);
			cage.spotsArray.spotsList.clear();
					for (ROI2DPolygonPlus roi : listSelectedAreas) {
			try {
				ROI2DPolygonPlus roiP = cageGrid.getAreaAt(roi.getCagePosition());
				Rectangle2D rect = roiP.getBounds2D();
				Point2D.Double center = (Double) roiP.getPosition2D();
				int radius = (int) (rect.getHeight() / 2);
				cage.addEllipseSpot(center, radius);
			} catch (ROI2DValidationException e) {
				System.err.println("Error getting area at position " + roi.getCagePosition() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
			cage.getRoi().setSelected(false);
		}
	}

	void changeGrid(Experiment exp, Cage cage) {
		if (roiGrid != null) {
			Sequence sequence = exp.seqCamData.getSequence();
			try {
				roiGrid.clearGridRois(sequence);
			} catch (ROI2DValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		roiGrid = createGrid(cage.getRoi());
		exp.seqCamData.getSequence().addROIs(roiGrid.getAreaRois(), false);
	}

	private ROI2DGrid createGrid(ROI2D roi) {
		ROI2DGrid grid = null;
		Polygon2D polygon = ((ROI2DPolygon) roi).getPolygon2D();
		if (polygon != null) {
			int n_columns = (int) nColumnsCombo.getSelectedItem();
			int n_rows = (int) nRowsCombo.getSelectedItem();
			grid = new ROI2DGrid();
			try {
				grid.createGridFromFrame(polygon, n_columns, n_rows);
			} catch (ROI2DValidationException | ROI2DGeometryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				grid.gridToRois("carre", Color.RED, 1, 1);
			} catch (ROI2DValidationException | ROI2DProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return grid;
	}

}
