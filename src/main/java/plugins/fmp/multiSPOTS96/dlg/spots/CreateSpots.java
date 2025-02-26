package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
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

import icy.canvas.Canvas2D;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentUtils;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DGrid;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DPolygonPlus;
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
					int cagenb = findSelectedCage(exp);
					zoomCage(exp, cagenb);
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
			roi.isSelected = roi.isSelected();
			if (!roi.isSelected)
				exp.seqCamData.seq.removeROI(roi);
		}
	}

	private void createSpotsForAllCages(Experiment exp, ROI2DGrid roiGrid, Point2D.Double referenceCagePosition) {
		ArrayList<ROI2DPolygonPlus> listSelectedAreas = roiGrid.getSelectedAreaRois();
		int spotIndex = 0;
		for (Cage cage : exp.cagesArray.cagesList) {
			ROI2D cageRoi = cage.getRoi();
			ROI2DGrid cageGrid = createGrid(cageRoi);
			cage.spotsArray.spotsList.clear();
			for (ROI2DPolygonPlus roi : listSelectedAreas) {
				ROI2DPolygonPlus roiP = cageGrid.getAreaAt(roi.cagePosition);
				Rectangle2D rect = roiP.getBounds2D();
				Point2D.Double center = (Double) roiP.getPosition2D();
				int radius = (int) (rect.getHeight() / 2);
				cage.addEllipseSpot(spotIndex, center, radius);
				spotIndex++;
			}
			cage.getRoi().setSelected(false);
		}
	}

	int findSelectedCage(Experiment exp) {
		int selectedCage = 0;
		for (Cage cage : exp.cagesArray.cagesList) {
			ROI2D roi = cage.getRoi();
			if (roi.isSelected()) {
				selectedCage = cage.getCageNumberInteger();
				break;
			}
		}
		return selectedCage;
	}

	void zoomCage(Experiment exp, int cagenb) {
		Cage cage = exp.cagesArray.getCageFromNumber(cagenb);
		if (cage == null)
			return;

		ROI2D roiCage = cage.getRoi();
		referencePosition = (Double) roiCage.getPosition2D();
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		Canvas2D canvas = (Canvas2D) v.getCanvas();
		Rectangle rect = roiCage.getBounds();
		canvas.centerOn(rect);

		if (roiGrid != null)
			roiGrid.clearGridRois(exp.seqCamData.seq);
		roiGrid = createGrid(roiCage);
		exp.seqCamData.seq.addROIs(roiGrid.getAreaRois(), false);
	}

	private ROI2DGrid createGrid(ROI2D roi) {
		ROI2DGrid grid = null;
		Polygon2D polygon = ((ROI2DPolygon) roi).getPolygon2D();
		if (polygon != null) {
			int n_columns = (int) nColumnsCombo.getSelectedItem();
			int n_rows = (int) nRowsCombo.getSelectedItem();
			grid = new ROI2DGrid();
			grid.createGridFromFrame(polygon, n_columns, n_rows);
			grid.gridToRois("carre", Color.RED, 1, 1);
		}
		return grid;
	}

}
