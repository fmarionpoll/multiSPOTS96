package plugins.fmp.multiSPOTS96.dlg.b_spots;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import icy.canvas.Canvas2D;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class EditSpots extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7582410775062671523L;

	private JCheckBox selectSpotsCheckBox = new JCheckBox("Select spots");
	private JCheckBox displaySnakeCheckBox = new JCheckBox("Display snake over spots");
	private JButton centerSpotsToSnakeButton = new JButton("Center spots to snake");
	private MultiSPOTS96 parent0 = null;
	private PositionWithTimePanel editPositionWithTime = null;

	public ROI2DPolygon roiPerimeter = null;
	public ROI2DPolyLine roiSnake = null;
	private ArrayList<Spot> enclosedSpots = null;

	private JButton erodeButton = new JButton("Contract spots");
	private JButton dilateButton = new JButton("Dilate spots");

	private JButton editSpotsWithTimeButton = new JButton("Change spots position with time");

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(selectSpotsCheckBox);
		panel0.add(displaySnakeCheckBox);
		panel0.add(centerSpotsToSnakeButton);
		add(panel0);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(dilateButton);
		panel1.add(erodeButton);
		add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(editSpotsWithTimeButton);
		add(panel2);

		defineActionListeners();
		updateButtonsState(false, false);
	}

	private void updateButtonsState(boolean enableSnake, boolean enableCenter) {
		displaySnakeCheckBox.setEnabled(enableSnake);
		centerSpotsToSnakeButton.setEnabled(enableCenter);
	}

	private void defineActionListeners() {

		selectSpotsCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				boolean isSelected = selectSpotsCheckBox.isSelected();
				updateButtonsState(isSelected, false);
				showFrame(exp, isSelected);
			}
		});

		displaySnakeCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				boolean isSelected = displaySnakeCheckBox.isSelected();
				showSnake(exp, isSelected);
				showFrame(exp, !isSelected);
				updateButtonsState(true, isSelected);
			}
		});

		centerSpotsToSnakeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				updateSpotsFromSnake(exp);
				// update colors
				// remove snake
			}
		});

		dilateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				resizeSpots(exp, +1);
			}
		});

		erodeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				resizeSpots(exp, -1);
			}
		});

		editSpotsWithTimeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				openDialog();
			}
		});

	}

	public void openDialog() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			if (editPositionWithTime == null)
				editPositionWithTime = new PositionWithTimePanel();
			editPositionWithTime.initialize(parent0);
		}
	}

	public void closeDialog() {
		editPositionWithTime.close();
	}

	// --------------------------------------

	private void showFrame(Experiment exp, boolean show) {
		if (show) {
			setSpotsFrame(exp);
		} else {
			exp.seqCamData.getSequence().removeROI(roiPerimeter);
		}
	}

	private void setSpotsFrame(Experiment exp) {
		exp.seqCamData.getSequence().removeROI(roiPerimeter);
		showSnake(exp, false);
		if (roiPerimeter == null)
			createRoiPerimeterEnclosingSpots(exp);
		exp.seqCamData.getSequence().addROI(roiPerimeter);
		exp.seqCamData.getSequence().setSelectedROI(roiPerimeter);

		makeSureRectangleIsVisible(exp, roiPerimeter.getBounds());
	}

	private void makeSureRectangleIsVisible(Experiment exp, Rectangle rect) {
		Viewer v = exp.seqCamData.getSequence().getFirstViewer();
		Canvas2D canvas = (Canvas2D) v.getCanvas();
		canvas.centerOn(rect);
	}

	private void showSnake(Experiment exp, boolean show) {
		exp.seqCamData.getSequence().removeROI(roiSnake);
		if (show) {
			exp.seqCamData.getSequence().removeROI(roiPerimeter);
			enclosedSpots = exp.cagesArray.getSpotsEnclosed(roiPerimeter);
			if (enclosedSpots.size() > 0) {
				ArrayList<Point2D> listPoint = new ArrayList<Point2D>();
				for (Spot spot : enclosedSpots) {
					Point2D.Double point = new Point2D.Double(spot.prop.spotXCoord + spot.prop.spotRadius,
							spot.prop.spotYCoord + spot.prop.spotRadius);
					listPoint.add(point);
				}
				roiSnake = new ROI2DPolyLine(listPoint);
				roiSnake.setName("snake");
				exp.seqCamData.getSequence().addROI(roiSnake);
				exp.seqCamData.getSequence().setSelectedROI(roiSnake);

				exp.seqCamData.displaySpecificROIs(false, "spot");
				makeSureRectangleIsVisible(exp, roiSnake.getBounds());
			}
		} else {
			roiSnake = null;
			exp.seqCamData.getSequence().addROI(roiPerimeter);
			exp.seqCamData.displaySpecificROIs(true, "spot");
		}
	}

	private void createRoiPerimeterEnclosingSpots(Experiment exp) {
		ArrayList<ROI2D> listSpotsRoisPresent = exp.seqCamData.getROIsContainingString("spot");
		ArrayList<ROI2D> listSpotsRoisSelected = new ArrayList<ROI2D>();
		for (ROI2D roi : listSpotsRoisPresent) {
			if (roi.isSelected())
				listSpotsRoisSelected.add(roi);
		}
		Polygon2D polygon = ROI2DUtilities.getPolygonEnclosingROI2Ds(
				listSpotsRoisSelected.size() > 0 ? listSpotsRoisSelected : listSpotsRoisPresent);
		roiPerimeter = new ROI2DPolygon(polygon);
		roiPerimeter.setName("perimeter");
		roiPerimeter.setColor(Color.YELLOW);
	}

	private void updateSpotsFromSnake(Experiment exp) {
		if (enclosedSpots == null || enclosedSpots.size() < 1 || roiSnake == null)
			return;

		ArrayList<ROI2D> listRoisSeq = exp.seqCamData.getROIsContainingString("spot");
		Polyline2D snake = roiSnake.getPolyline2D();
		int i = 0;
		for (Spot spot : enclosedSpots) {
			spot.prop.spotXCoord = (int) snake.xpoints[i];
			spot.prop.spotYCoord = (int) snake.ypoints[i];
			ROI2D roi = spot.getRoi();

			Point2D.Double point = new Point2D.Double(snake.xpoints[i] - spot.prop.spotRadius,
					snake.ypoints[i] - spot.prop.spotRadius);
			roi.setPosition2D(point);

			String name = roi.getName();
			for (ROI2D roiSeq : listRoisSeq) {
				if (roiSeq.getName().equals(name)) {
					listRoisSeq.remove(roiSeq);
					roiSeq.setPosition2D(point);
					break;
				}
			}
			i++;
		}

		exp.seqCamData.displaySpecificROIs(true, "spot");
	}

	private void resizeSpots(Experiment exp, int delta) {
		enclosedSpots = exp.cagesArray.getSpotsEnclosed(roiPerimeter);
		if (enclosedSpots.size() > 0) {
			for (Spot spot : enclosedSpots) {
				ROI2DShape roi = (ROI2DShape) spot.getRoi();
				exp.seqCamData.getSequence().removeROI(roi);
				roi = (ROI2DShape) ROI2DUtilities.resizeROI(roi, delta);
				spot.setRoi(roi);
				exp.seqCamData.getSequence().addROI(roi);
			}
		} else {
			ConfirmDialog.confirm("At least one spot must be selected");
		}
	}

	public void clearTemporaryROIs() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			exp.seqCamData.getSequence().removeROI(roiSnake);
			exp.seqCamData.getSequence().removeROI(roiPerimeter);
			roiSnake = null;
			roiPerimeter = null;
			displaySnakeCheckBox.setSelected(false);
			selectSpotsCheckBox.setSelected(false);
		}
	}

}
