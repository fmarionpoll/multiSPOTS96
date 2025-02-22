package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentUtils;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.cages.CagesArray;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DGrid;
import plugins.fmp.multiSPOTS96.tools.polyline.PolygonUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class Cages extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private JButton createFrameButton = new JButton("(1) Frame");
	private JButton createGridButton = new JButton("(2) Grid (from 1)");
	private JButton createCagesButton = new JButton("(3) Cages (from 2)");

	private JSpinner nCagesPerPlateAlongXJSpinner = new JSpinner(new SpinnerNumberModel(6, 0, 10000, 1));
	private JSpinner nCagesPerPlateAlongYJSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 10000, 1));

	private JSpinner width_intervalTextField = new JSpinner(new SpinnerNumberModel(4, 0, 10000, 1));
	private JSpinner height_intervalTextField = new JSpinner(new SpinnerNumberModel(4, 0, 10000, 1));

	private final String cages_perimeter = "cages_perimeter";
	private int width_interval = 1;
	private int height_interval = 1;
	private Polygon2D polygon2D = null;
	private ROI2DGrid roiGrid = null;
	private MultiSPOTS96 parent0;

	// -------------------------------------------------

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(createFrameButton);
		panel0.add(createGridButton);
		panel0.add(createCagesButton);
		add(panel0);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(new JLabel("N columns "));
		panel1.add(nCagesPerPlateAlongXJSpinner);
		nCagesPerPlateAlongXJSpinner.setPreferredSize(new Dimension(40, 20));

		panel1.add(new JLabel("space"));
		panel1.add(width_intervalTextField);
		width_intervalTextField.setPreferredSize(new Dimension(40, 20));
		add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel("N rows "));
		panel2.add(nCagesPerPlateAlongYJSpinner);
		nCagesPerPlateAlongYJSpinner.setPreferredSize(new Dimension(40, 20));

		panel2.add(new JLabel("space"));
		panel2.add(height_intervalTextField);
		height_intervalTextField.setPreferredSize(new Dimension(40, 20));
		add(panel2);

		defineActionListeners();
	}

	private void defineActionListeners() {
		createFrameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					selectRoiPerimeterEnclosingCages(exp);
					removeGrid(exp);
				}
			}
		});

		createGridButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					createGrid(exp);
				}
			}
		});

		createCagesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					createCages(exp);
					removeGrid(exp);
				}
			}
		});

	}

	private void removeGrid(Experiment exp) {
		if (roiGrid == null)
			return;
		exp.seqCamData.seq.removeROIs(roiGrid.getHorizontalRois(), false);
		exp.seqCamData.seq.removeROIs(roiGrid.getVerticalRois(), false);
	}

	private void createCages(Experiment exp) {
		polygon2D = getPolygonFromCagesPerimeterRoi(exp);
		if (polygon2D != null) {
			if (roiGrid == null)
				createGrid(exp);
			createCagesFromGrid(exp, roiGrid);
			ExperimentUtils.transferCagesToCamDataSequence(exp);
		}
	}

	private Polygon2D getPolygonFromCagesPerimeterRoi(Experiment exp) {
		selectRoiPerimeterEnclosingCages(exp);
		ROI2D roi = (ROI2D) exp.seqCamData.seq.getSelectedROI();
		polygon2D = PolygonUtilities.orderVerticesOf4CornersPolygon(((ROI2DPolygon) roi).getPolygon());
		exp.seqCamData.seq.removeROI(roi);
		return polygon2D;
	}

	void updateNColumnsFieldFromSequence() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			int nrois = exp.cagesArray.cagesList.size();
			if (nrois > 0) {
				nCagesPerPlateAlongXJSpinner.setValue(exp.cagesArray.nCagesAlongX);
				nCagesPerPlateAlongYJSpinner.setValue(exp.cagesArray.nCagesAlongY);
			}
		}
	}

	private void selectRoiPerimeterEnclosingCages(Experiment exp) {
		Sequence seq = exp.seqCamData.seq;
		List<ROI2D> listrois = exp.seqCamData.getROIsContainingString(cages_perimeter);
		ROI2DPolygon roi = null;
		if (listrois != null && listrois.size() > 0)
			roi = (ROI2DPolygon) listrois.get(0);
		else {
			roi = new ROI2DPolygon(getPolygonEnclosingAllCages(exp));
			roi.setName(cages_perimeter);
			seq.addROI(roi);
		}
		roi.setColor(Color.orange);
		seq.setSelectedROI(roi);
	}

	private Polygon2D getPolygonEnclosingAllCages(Experiment exp) {
		if (exp.cagesArray.cagesList.size() > 0) {
			polygon2D = exp.cagesArray.getPolygon2DEnclosingAllCages();
		} else {
			Rectangle rect = exp.seqCamData.seq.getBounds2D();
			List<Point2D> points = new ArrayList<Point2D>();
			points.add(new Point2D.Double(rect.x + rect.width / 5, rect.y + rect.height / 5));
			points.add(new Point2D.Double(rect.x + rect.width * 4 / 5, rect.y + rect.height / 5));
			points.add(new Point2D.Double(rect.x + rect.width * 4 / 5, rect.y + rect.height * 2 / 3));
			points.add(new Point2D.Double(rect.x + rect.width / 5, rect.y + rect.height * 2 / 3));
			polygon2D = new Polygon2D(points);
		}
		return polygon2D;
	}

	private void createCagesFromGrid(Experiment exp, ROI2DGrid roiGrid) {
		int n_columns = 10;
		int n_rows = 1;
		try {
			n_columns = (int) nCagesPerPlateAlongXJSpinner.getValue();
			n_rows = (int) nCagesPerPlateAlongYJSpinner.getValue();
			width_interval = (int) width_intervalTextField.getValue();
			height_interval = (int) height_intervalTextField.getValue();
		} catch (Exception e) {
			new AnnounceFrame("Can't interpret ROI parameters value");
		}

		// erase existing cages
		exp.seqCamData.removeROIsContainingString("cage");
		exp.cagesArray.cagesList.clear();
		exp.cagesArray = new CagesArray(n_columns, n_rows);
		createCagesArrayFromGrid(exp, roiGrid, n_columns, n_rows, width_interval, height_interval);
	}

	private void createCagesArrayFromGrid(Experiment exp, ROI2DGrid roiGrid, int ncolumns, int nrows,
			int width_interval, int height_interval) {
		Point2D.Double[][] grid = roiGrid.getGridPoints();
		// test if dimensions are ok
		if (grid.length != (ncolumns + 1) || grid[0].length != (nrows + 1)) {
			System.out.println("error in the dimensions of grid");
		}
		// generate cage frames
		String cageRoot = "cage";
		int index = 0;

		for (int row = 0; row < nrows; row++) {
			for (int column = 0; column < ncolumns; column++) {
				ROI2DPolygon roiP = createRoiPolygon(grid, column, row, width_interval, height_interval);
				roiP.setName(cageRoot + String.format("%03d", index));
				roiP.setColor(Color.yellow);

				Cage cage = new Cage(roiP);
				cage.cageID = index;
				cage.arrayIndex = index;
				cage.arrayColumn = column;
				cage.arrayRow = row;

				index++;
				exp.seqCamData.seq.addROI(roiP);
				exp.cagesArray.cagesList.add(cage);
			}
		}
	}

	private ROI2DPolygon createRoiPolygon(Point2D.Double[][] grid, int icol, int irow, int width, int height) {
		List<Point2D> points = new ArrayList<>();
		Point2D.Double pt = (Double) grid[icol][irow].clone();
		pt.x += width;
		pt.y += height;
		points.add(pt);

		pt = (Double) grid[icol][irow + 1].clone();
		pt.x += width;
		pt.y -= height;
		points.add(pt);

		pt = (Double) grid[icol + 1][irow + 1].clone();
		pt.x -= width;
		pt.y -= height;
		points.add(pt);

		pt = (Double) grid[icol + 1][irow].clone();
		pt.x -= width;
		pt.y += height;
		points.add(pt);

		ROI2DPolygon roiP = new ROI2DPolygon(points);
		return roiP;
	}

	private void createGrid(Experiment exp) {
		exp.seqCamData.removeROIsContainingString("cage");
		exp.seqCamData.removeROIsContainingString("row");
		exp.seqCamData.removeROIsContainingString("col");
		int n_columns = 6;
		int n_rows = 8;
		try {
			n_columns = (int) nCagesPerPlateAlongXJSpinner.getValue();
			n_rows = (int) nCagesPerPlateAlongYJSpinner.getValue();
		} catch (Exception e) {
			new AnnounceFrame("Can't interpret one of the ROI parameters value");
		}

		Polygon2D polyGon = getPolygonFromCagesPerimeterRoi(exp);
		roiGrid = new ROI2DGrid();
		roiGrid.createGridFromFrame(polyGon, n_columns, n_rows);
		exp.seqCamData.seq.addROIs(roiGrid.getHorizontalRois(), false);
		exp.seqCamData.seq.addROIs(roiGrid.getVerticalRois(), false);
	}

}
