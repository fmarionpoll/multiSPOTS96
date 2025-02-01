package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.canvas.Canvas2D;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentUtils;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.experiment.spots.SpotsArray;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DGrid;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROIUtilities;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class CreateSpots extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;

	private JButton zoomCageButton = new JButton("(1) Show grid over cage");
	private JComboBox<Integer> nRowsCombo = new JComboBox<Integer>(new Integer[] { 1, 2, 4 });
	private JComboBox<Integer> nColumnsCombo = new JComboBox<Integer>(new Integer[] { 1, 2, 4, 8 });

	private JButton keepAreasButton = new JButton("(2) Keep selected areas");
	private JButton restoreAreasButton = new JButton("restore areas");

	private JButton duplicateAllButton = new JButton("(3) Create spots / all cages");
	private JComboBox<String> spotShapeCombo = new JComboBox<String>(new String[] { "polygon", "ellipse" });

	private JSpinner nFliesPerCageJSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 500, 1));

	private String[] flyString = new String[] { "fly", "flies" };
	private JLabel flyLabel = new JLabel(flyString[0]);

	private MultiSPOTS96 parent0 = null;
	private ROI2DGrid roiGrid = null;
	private Point2D.Double referencePosition = null;

	// ----------------------------------------------------------

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(zoomCageButton);
		panel0.add(new JLabel("cols"));
		panel0.add(nColumnsCombo);
		panel0.add(new JLabel("rows"));
		panel0.add(nRowsCombo);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(keepAreasButton);
		panel1.add(restoreAreasButton);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(duplicateAllButton);
		panel2.add(spotShapeCombo);
		panel2.add(new JLabel("with"));

		panel2.add(nFliesPerCageJSpinner);
		nFliesPerCageJSpinner.setPreferredSize(new Dimension(40, 20));
		panel2.add(flyLabel);

		add(panel0);
		add(panel1);
		add(panel2);

		nRowsCombo.setSelectedItem(4);
		nColumnsCombo.setSelectedItem(8);

		defineActionListeners();
		this.parent0 = parent0;
	}

	private void defineActionListeners() {
		zoomCageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					int cagenb = findSelectedCage(exp);
					zoomCage(exp, cagenb);
				}
			}
		});

		keepAreasButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					keepSelectedAreas(exp);
				}
			}
		});

		restoreAreasButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					restoreAreas(exp);
				}
			}
		});

		duplicateAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					createSpotsForAllCages(exp, roiGrid, referencePosition);

					ExperimentUtils.transferSpotsToCamDataSequence(exp);
					int nbFliesPerCage = (int) nFliesPerCageJSpinner.getValue();
					exp.spotsArray.initSpotsWithNFlies(nbFliesPerCage);

				}
			}
		});

		nFliesPerCageJSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int i = (int) nFliesPerCageJSpinner.getValue() > 1 ? 1 : 0;
				flyLabel.setText(flyString[i]);
				nFliesPerCageJSpinner.requestFocus();
			}
		});

	}

	// ---------------------------------

	private void keepSelectedAreas(Experiment exp) {
		ArrayList<ROI2DPolygon> listCarres = roiGrid.getAreaRois();
		for (ROI2DPolygon roi : listCarres) {
			if (!roi.isSelected()) {
				exp.seqCamData.seq.removeROI(roi);
			}
		}
	}

	private void restoreAreas(Experiment exp) {
		exp.seqCamData.seq.removeROIs(ROIUtilities.getROIsContainingString("carre", exp.seqCamData.seq), false);
		ArrayList<ROI2DPolygon> listCarres = roiGrid.getAreaRois();
		for (ROI2DPolygon roi : listCarres)
			exp.seqCamData.seq.addROI(roi);
	}

//	private ArrayList<Point2D.Double> getRelativePositionsOfSelectedAreas() {
//		ArrayList<Point2D.Double> listOfPositionsRelativeToCage = new ArrayList<Point2D.Double> (1);
//		ArrayList<ROI2DPolygon> listCarres = roiGrid.getAreaRois();
//		for (ROI2DPolygon roi : listCarres) {
//			if (roi.isSelected()) {
//				Rectangle2D rect = roi.getBounds2D();
//				Point2D.Double center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
//				center.x = center.x - referencePosition.x;
//				center.y = center.x - referencePosition.y;
//				listOfPositionsRelativeToCage.add(center);
//			}
//		}
//		return listOfPositionsRelativeToCage;
//	}

	private void createSpotsForAllCages(Experiment exp, ROI2DGrid roiGrid, Point2D.Double referenceCagePosition) {
		exp.spotsArray.spotsList.clear();
		exp.spotsArray = new SpotsArray();

		ArrayList<ROI2DPolygon> listCarres = roiGrid.getAreaRois();
		int spotIndex = 0;
		for (Cage cage : exp.cagesArray.cagesList) {
			Point2D.Double cagePosition = (Double) cage.getRoi().getPosition2D();
			int carreIndex = 0;
			for (ROI2DPolygon roi : listCarres) {
				if (roi.isSelected()) {
					Rectangle2D rect = roi.getBounds2D();
					Point2D.Double center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					center.x = center.x - referenceCagePosition.x + cagePosition.x;
					center.y = center.y - referenceCagePosition.y + cagePosition.y;
					int radius = (int) (roi.getBounds().getHeight() / 2);
					exp.spotsArray.spotsList.add(createSpot(exp, cage, carreIndex, spotIndex, center, radius));
					spotIndex++;
				}
				carreIndex++;
			}
		}
	}

	private Spot createSpot(Experiment exp, Cage cage, int carreIndex, int spotIndex, Point2D.Double center,
			int radius) {
		Ellipse2D ellipse = new Ellipse2D.Double(center.x, center.y, 2 * radius, 2 * radius);
		ROI2DEllipse roiEllipse = new ROI2DEllipse(ellipse);
		roiEllipse.setName("spot_" + String.format("%03d", cage.cageID) + String.format("%03d", carreIndex)
				+ String.format("%03d", spotIndex));

		Spot spot = new Spot(roiEllipse);
		spot.plateIndex = spotIndex;

		spot.spotRadius = radius;
		spot.spotXCoord = (int) center.getX();
		spot.spotYCoord = (int) center.getY();
		try {
			spot.spotNPixels = (int) roiEllipse.getNumberOfPoints();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return spot;
	}

//	private void createCirclesFromSelectedAreas(Experiment exp) {
//		int radius = 3;// half of rectangle in which selected roi fits.
////		try {
////			radius = (int) pixelRadiusSpinner.getValue();
////		} catch (Exception e) {
////			new AnnounceFrame("Can't interpret one of the ROI parameters value");
////		}
//		// erase existing spots
//		exp.seqCamData.seq.removeROIs(ROIUtilities.getROIsContainingString("circle_", exp.seqCamData.seq), false);
//		ArrayList<ROI2DPolygon> listCarres = roiGrid.getAreaRois();
//
//		ArrayList<Point2D.Double> listPoints = new ArrayList<Point2D.Double>(1);
//		for (ROI2DPolygon roi : listCarres) {
//			if (roi.isSelected()) {
//				Rectangle2D rect = roi.getBounds2D();
//				Point2D.Double center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
//				listPoints.add(center);
//			}
//		}
//		ArrayList<ROI2DEllipse> listCircles = convertPoint2DArrayToCircles(exp, listPoints, radius);
//		exp.seqCamData.seq.addROIs(listCircles, false);
//	}

//	private ArrayList<ROI2DEllipse> convertPoint2DArrayToCircles(Experiment exp, ArrayList<Point2D.Double> arrayPoints,
//			int radius) {
//		int spotIndex = 0;
//		ArrayList<ROI2DEllipse> listCircles = new ArrayList<ROI2DEllipse>(1);
//		for (Point2D.Double point : arrayPoints) {
//			double x = point.getX() - radius;
//			double y = point.getY() - radius;
//			Ellipse2D ellipse = new Ellipse2D.Double(x, y, 2 * radius, 2 * radius);
//			ROI2DEllipse roiEllipse = new ROI2DEllipse(ellipse);
//			roiEllipse.setName("circle_" + String.format("%03d", spotIndex));
//			listCircles.add(roiEllipse);
//			spotIndex++;
//		}
//		return listCircles;
//	}

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

		createGrid(exp, roiCage);
	}

	private void createGrid(Experiment exp, ROI2D roi) {
		Polygon2D polygon = ((ROI2DPolygon) roi).getPolygon2D();
		if (polygon != null) {
			roiGrid = new ROI2DGrid();
			roiGrid.clearGridRois(exp.seqCamData.seq);

			int n_columns = (int) nColumnsCombo.getSelectedItem();
			int n_rows = (int) nRowsCombo.getSelectedItem();
			roiGrid.createGridFromFrame(polygon, n_columns, n_rows);
			ArrayList<ROI2DPolygon> listCarres = roiGrid.gridToRois("carre", Color.RED, 1, 1);
			exp.seqCamData.seq.addROIs(listCarres, false);
		}
	}

}
