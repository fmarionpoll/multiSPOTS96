package plugins.fmp.multiSPOTS96.dlg.a_experiment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector2d;

import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.util.StringUtil;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.series.Registration;
import plugins.fmp.multiSPOTS96.tools.GaspardRigidRegistration;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformEnums;

public class CorrectDrift extends JPanel implements ViewerListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	int val = 0; // set your own value, I used to check if it works
	int min = 0;
	int max = 10000;
	int step = 1;
	int maxLast = 99999999;
	JSpinner startFrameJSpinner = new JSpinner(new SpinnerNumberModel(val, min, max, step));
	JSpinner referenceFrameJSpinner = new JSpinner(new SpinnerNumberModel(val, min, max, step));
	JButton runButton = new JButton("Run registration");

	JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(0, -500, 500, 1));
	JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(0, -500, 500, 1));
	JButton testTranslationButton = new JButton("Test");
	JButton applyTranslationButton = new JButton("Apply");
	JButton restoreTranslationButton = new JButton("Restore 1 step");
	int previousX = 0;
	int previousY = 0;
	int previousT = 0;
	double previousAngle = 0.;

	JSpinner oSpinner = new JSpinner(new SpinnerNumberModel(0., -180., 180., 1.));
	JButton testRotationButton = new JButton("Test");
	JButton applyRotationButton = new JButton("Apply");
	JButton restoreRotationButton = new JButton("Restore 1 step");

	JSpinner squareSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 500, 1));

	private MultiSPOTS96 parent0 = null;
	JComboBoxExperiment editExpList = new JComboBoxExperiment();
	private Registration registration = null;

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(1);

		JPanel referencePanel = new JPanel(flowlayout);
		referencePanel.add(new JLabel("Start frame"));
		referencePanel.add(startFrameJSpinner);
		startFrameJSpinner.setPreferredSize(new Dimension(50, 20));
		referencePanel.add(new JLabel("Ref. frame"));
		referencePanel.add(referenceFrameJSpinner);
		referenceFrameJSpinner.setPreferredSize(new Dimension(50, 20));
		referencePanel.add(runButton);

		add(referencePanel);

		JPanel translationPanel = new JPanel(flowlayout);
		translationPanel.add(new JLabel("Translate X"));
		translationPanel.add(xSpinner);
		xSpinner.setPreferredSize(new Dimension(50, 20));
		translationPanel.add(new JLabel("Y"));
		translationPanel.add(ySpinner);
		ySpinner.setPreferredSize(new Dimension(50, 20));
		translationPanel.add(testTranslationButton);
		translationPanel.add(applyTranslationButton);
		translationPanel.add(restoreTranslationButton);
		add(translationPanel);

		JPanel rotationPanel = new JPanel(flowlayout);
		rotationPanel.add(new JLabel("rotate (degrees)"));
		rotationPanel.add(oSpinner);
		rotationPanel.add(testRotationButton);
		rotationPanel.add(applyRotationButton);
		rotationPanel.add(restoreRotationButton);
		add(rotationPanel);

		JPanel chessPanel = new JPanel(flowlayout);
		chessPanel.add(new JLabel("Size of test square (pixels)"));
		chessPanel.add(squareSizeSpinner);
		add(chessPanel);

		restoreTranslationButton.setEnabled(false);
		restoreRotationButton.setEnabled(false);

		defineActionListeners();
	}

	private void defineActionListeners() {
		runButtonListener();
		startFrameJSpinnerListener();
		testTranslationButtonListener();
		applyTranslationButtonListener();
		restoreTranslationButtonListener();
		testRotationButtonListener();
		applyRotationButtonListener();
		restoreRotationButtonListener();
	}

	private void runButtonListener() {
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (runButton.getText().equals("Run"))
						executeRegistration(exp);
					else
						stopComputation();
				}
			}
		});
	}

	private void startFrameJSpinnerListener() {
		startFrameJSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData.getSequence() != null) {
					Viewer v = exp.seqCamData.getSequence().getFirstViewer();
					if (v != null) {
						int newValue = (int) startFrameJSpinner.getValue();
						if (v.getPositionT() != newValue)
							v.setPositionT((int) newValue);
					}
				}
			}
		});
	}

	private void applyTranslationButtonListener() {
		applyTranslationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {
					int x = (int) xSpinner.getValue();
					int y = (int) ySpinner.getValue();
					applyTranslation(exp, x, y);

					restoreTranslationButton.setEnabled(true);
					previousX = x;
					previousY = y;
				}
			}
		});
	}

	private void testTranslationButtonListener() {
		testTranslationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {
					int x = (int) xSpinner.getValue();
					int y = (int) ySpinner.getValue();
					testTranslation(exp, x, y);
				}
			}
		});
	}

	private void restoreTranslationButtonListener() {
		restoreTranslationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {
					applyTranslation(exp, -previousX, -previousY);

					restoreTranslationButton.setEnabled(false);
					previousX = 0;
					previousY = 0;
				}
			}
		});
	}

	private void applyRotationButtonListener() {
		applyRotationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {
					double angle = (double) oSpinner.getValue();
					applyRotation(exp, angle);

					restoreRotationButton.setEnabled(true);
					previousAngle = angle;
				}
			}
		});
	}

	private void restoreRotationButtonListener() {
		restoreRotationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {
					applyRotation(exp, previousAngle);

					restoreRotationButton.setEnabled(false);
					previousAngle = 0.;
				}
			}
		});
	}

	private void testRotationButtonListener() {
		testRotationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {

				}
			}
		});
	}

	public void resetFrameIndex() {
		startFrameJSpinner.setValue(0);
		referenceFrameJSpinner.setValue(0);
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T)) {
			Viewer v = event.getSource();
			int t = v.getPositionT();
			if (t >= 0) {
				if (t != previousT) {
					previousT = t;
					previousX = 0;
					previousY = 0;
					restoreTranslationButton.setEnabled(false);
				}
				startFrameJSpinner.setValue(t);
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	void executeRegistration(Experiment exp) {
		registration = new Registration();
		registration.options = initParameters(exp);
		registration.stopFlag = false;
		registration.addPropertyChangeListener(this);
		registration.execute();
		runButton.setText("STOP");
	}

	private BuildSeriesOptions initParameters(Experiment exp) {
		BuildSeriesOptions options = new BuildSeriesOptions();
		int referenceFrame = (int) referenceFrameJSpinner.getValue();

		options.fromFrame = (int) startFrameJSpinner.getValue();
		options.toFrame = referenceFrame - 1;
		options.referenceFrame = referenceFrame;
		options.expList = parent0.expListCombo;
		options.transformop = ImageTransformEnums.NONE;
		return options;
	}

	private void stopComputation() {
		if (registration != null && !registration.stopFlag)
			registration.stopFlag = true;
		runButton.setText("Run registration");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			runButton.setText("Run registration");
			runButton.removePropertyChangeListener(this);
		}
	}

//	private void displayTransform(Experiment exp) {
//		updateTransformFunctionsOfCanvas(exp);
//	}

	private boolean testTranslation(Experiment exp, int x, int y) {
		int squareSize = (int) squareSizeSpinner.getValue();

		int testFrame = (int) startFrameJSpinner.getValue();
		int referenceFrame = (int) referenceFrameJSpinner.getValue();
		Sequence seq = exp.seqCamData.getSequence();
		IcyBufferedImage imageTest = seq.getImage(testFrame, 0);
		IcyBufferedImage imageReference = seq.getImage(referenceFrame, 0);
		Sequence chessSeq = createChessboardImage(imageTest, imageReference, squareSize);
		Viewer v = new Viewer(chessSeq, true);
		if (v == null)
			return false;

//		Vector2d translation = new Vector2d(x, y);
//		int t = (int) referenceFrameJSpinner.getValue();
//		Sequence seq = exp.seqCamData.getSequence();
//		IcyBufferedImage workImage = seq.getImage(t, 0);
//		workImage = GaspardRigidRegistration.applyTranslation2D(workImage, -1, translation, true);
//		seq.setImage(t, 0, workImage);
//
//		String fileName = exp.seqCamData.getFileNameFromImageList(t);
//		File outputfile = new File(fileName);
//		RenderedImage image = ImageUtil.toRGBImage(workImage);
//		return ImageUtil.save(image, "jpg", outputfile);
		return true;
	}

	private boolean applyTranslation(Experiment exp, int x, int y) {
		Vector2d translation = new Vector2d(x, y);
		int t = (int) referenceFrameJSpinner.getValue();
		Sequence seq = exp.seqCamData.getSequence();
		IcyBufferedImage workImage = seq.getImage(t, 0);
		workImage = GaspardRigidRegistration.applyTranslation2D(workImage, -1, translation, true);
		seq.setImage(t, 0, workImage);

		String fileName = exp.seqCamData.getFileNameFromImageList(t);
		File outputfile = new File(fileName);
		RenderedImage image = ImageUtil.toRGBImage(workImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}

	private boolean applyRotation(Experiment exp, double angleDegrees) {
		int t = (int) referenceFrameJSpinner.getValue();
		double angleRadians = Math.toRadians(angleDegrees);
		Sequence seq = exp.seqCamData.getSequence();
		IcyBufferedImage workImage = seq.getImage(t, 0);
		workImage = GaspardRigidRegistration.applyRotation2D(workImage, -1, angleRadians, true);
		seq.setImage(t, 0, workImage);

		String fileName = exp.seqCamData.getFileNameFromImageList(t);
		File outputfile = new File(fileName);
		RenderedImage image = ImageUtil.toRGBImage(workImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}

	private Sequence createChessboardImage(IcyBufferedImage s1Image, IcyBufferedImage s2Image, int squareSize) {

		Dimension resultDim = new Dimension(Math.max(s1Image.getWidth(), s2Image.getWidth()),
				Math.max(s1Image.getHeight(), s2Image.getHeight()));
		IcyBufferedImage resultImage = new IcyBufferedImage(resultDim.width, resultDim.height, s1Image.getSizeC(),
				s1Image.getDataType_());
		resultImage.beginUpdate();

		for (int ch = 0; ch < resultImage.getSizeC(); ch++) {
			double[] resultData = Array1DUtil.arrayToDoubleArray(resultImage.getDataXY(ch),
					resultImage.isSignedDataType());
			double[] s1Data = Array1DUtil.arrayToDoubleArray(s1Image.getDataXY(ch), resultImage.isSignedDataType());
			double[] s2Data = Array1DUtil.arrayToDoubleArray(s2Image.getDataXY(ch), resultImage.isSignedDataType());

			for (int j = 0; j < resultImage.getHeight(); j++) {

				int jResultOffset = j * resultImage.getWidth();
				int js1Offset = j * s1Image.getWidth();
				int js2Offset = j * s2Image.getWidth();

				for (int i = 0; i < resultImage.getWidth(); i++) {
					boolean showS1 = (((i / squareSize) % 2) + ((j / squareSize) % 2)) % 2 == 0;
					if (showS1) {
						if (i < s1Image.getWidth() && j < s1Image.getHeight()) {
							resultData[jResultOffset + i] = s1Data[js1Offset + i];
						} else {
							resultData[jResultOffset + i] = 0;
						}
					} else {
						if (i < s2Image.getWidth() && j < s2Image.getHeight()) {
							resultData[jResultOffset + i] = s2Data[js2Offset + i];
						} else {
							resultData[jResultOffset + i] = 0;
						}
					}
				}
			}

			Array1DUtil.doubleArrayToArray(resultData, resultImage.getDataXY(ch));
		}

		resultImage.endUpdate();
		Sequence result = new Sequence("composite image", resultImage);
		return result;
	}

}
