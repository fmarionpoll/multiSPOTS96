package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import icy.gui.dialog.ConfirmDialog;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DAlongT;
import plugins.fmp.multiSPOTS96.tools.ROI2D.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Edit extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7582410775062671523L;

	private JButton editSpotsButton = new JButton("Change spots position with time");
	private JCheckBox selectSpotsButton = new JCheckBox("Select spots within frame");
	private JToggleButton displaySnakeButton = new JToggleButton("Display snake over spots");
	private JButton updateSpotsFromSnakeButton = new JButton("Center spots to snake");
	private MultiSPOTS96 parent0 = null;
	private PositionWithTimePanel editPositionWithTime = null;

	private final String dummyname = "perimeter_enclosing";
	private ROI2DPolygon spotsFrame = null;
	private ArrayList<Spot> enclosedSpots = null;
	private ROI2DPolyLine snakeRoi = null;

	private JButton erodeButton = new JButton("Contract spots");
	private JButton dilateButton = new JButton("Dilate spots");

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);

		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(selectSpotsButton);
		panel0.add(displaySnakeButton);
		panel0.add(updateSpotsFromSnakeButton);
		add(panel0);

		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(dilateButton);
		panel1.add(erodeButton);
		add(panel1);

		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(editSpotsButton);
		add(panel2);

		defineActionListeners();
		updateButtonsState(false);
	}

	private void updateButtonsState(boolean isFrameSelected) {
		displaySnakeButton.setEnabled(isFrameSelected);
		updateSpotsFromSnakeButton.setEnabled((displaySnakeButton.isSelected()) ? isFrameSelected : false);
	}

	private void defineActionListeners() {
		editSpotsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				openDialog();
			}
		});

		selectSpotsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				updateButtonsState(selectSpotsButton.isSelected());
				showFrame(selectSpotsButton.isSelected());
			}
		});

		displaySnakeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				if (displaySnakeButton.isSelected())
					displaySnake(exp);
				else
					removeSnake(exp);
				updateButtonsState(selectSpotsButton.isSelected());
			}
		});

		updateSpotsFromSnakeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp == null)
					return;
				updateSpotsFromSnake(exp);
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

	private void showFrame(boolean show) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;

		if (show) {
			int t = exp.seqCamData.seq.getFirstViewer().getPositionT();
			setSpotsFrame(t, exp);
		} else {
			removeSpotsFrame(exp);
			removeSnake(exp);
		}
	}

	private void setSpotsFrame(int t, Experiment exp) {
		if (spotsFrame == null) {
			ArrayList<ROI2D> listRoisAtT = new ArrayList<ROI2D>();
			for (Cage cage : exp.cagesArray.cagesList) {
				for (Spot spot : cage.spotsArray.spotsList) {

					ROI2DAlongT kymoROI2D = spot.getROIAtT(t);
					listRoisAtT.add(kymoROI2D.getRoi_in());
				}
				Polygon2D polygon = ROI2DUtilities.getPolygonEnclosingROI2Ds(listRoisAtT);
				spotsFrame = new ROI2DPolygon(polygon);
				spotsFrame.setName(dummyname);
				spotsFrame.setColor(Color.YELLOW);
			}
		}

		exp.seqCamData.seq.removeROI(spotsFrame);
		exp.seqCamData.seq.addROI(spotsFrame);
		exp.seqCamData.seq.setSelectedROI(spotsFrame);
		removeSnake(exp);
	}

	private void displaySnake(Experiment exp) {
		enclosedSpots = exp.cagesArray.getSpotsEnclosed(spotsFrame);
		if (enclosedSpots.size() > 0) {
			ArrayList<Point2D> listPoint = new ArrayList<Point2D>();
			for (Spot spot : enclosedSpots) {
				listPoint.add(new Point2D.Double(spot.spotXCoord + spot.spotRadius, spot.spotYCoord + spot.spotRadius));
			}
			snakeRoi = new ROI2DPolyLine(listPoint);
			exp.seqCamData.seq.addROI(snakeRoi);
			exp.seqCamData.seq.setSelectedROI(snakeRoi);
		}
	}

	private void removeSnake(Experiment exp) {
		if (snakeRoi != null)
			exp.seqCamData.seq.removeROI(snakeRoi);
		snakeRoi = null;
	}

	private void removeSpotsFrame(Experiment exp) {
		if (spotsFrame != null)
			exp.seqCamData.seq.removeROI(spotsFrame);
	}

	private void updateSpotsFromSnake(Experiment exp) {
		if (enclosedSpots != null && enclosedSpots.size() > 0 && snakeRoi != null) {
			Polyline2D snake = snakeRoi.getPolyline2D();
			int i = 0;
			for (Spot spot : enclosedSpots) {
				spot.spotXCoord = (int) snake.xpoints[i];
				spot.spotYCoord = (int) snake.ypoints[i];
				spot.getRoi().setPosition2D(
						new Point2D.Double(snake.xpoints[i] - spot.spotRadius, snake.ypoints[i] - spot.spotRadius));
				i++;
			}
		}
	}

	private void resizeSpots(Experiment exp, int delta) {
		enclosedSpots = exp.cagesArray.getSpotsEnclosed(spotsFrame);
		if (enclosedSpots.size() > 0) {
			for (Spot spot : enclosedSpots) {
				ROI2DShape roi = (ROI2DShape) spot.getRoi();
				exp.seqCamData.seq.removeROI(roi);
				roi = (ROI2DShape) ROI2DUtilities.resizeROI(roi, delta);
				spot.setRoi(roi);
				exp.seqCamData.seq.addROI(roi);
			}
		} else {
			ConfirmDialog.confirm("At least one spot must be selected");
		}
	}

}
