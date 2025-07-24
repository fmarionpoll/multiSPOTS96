package plugins.fmp.multiSPOTS96.dlg.a_experiment;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.sequence.DimensionId;
import icy.util.StringUtil;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.series.Registration;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;
import plugins.fmp.multiSPOTS96.tools.canvas2D.Canvas2D_3Transforms;
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
	JSpinner referenceFrameJSpinner = new JSpinner(new SpinnerNumberModel(val, min, max, step));
	public static final ImageTransformEnums[] TRANSFORMS = { ImageTransformEnums.NONE, ImageTransformEnums.R_RGB,
			ImageTransformEnums.G_RGB, ImageTransformEnums.B_RGB, ImageTransformEnums.R2MINUS_GB,
			ImageTransformEnums.G2MINUS_RB, ImageTransformEnums.B2MINUS_RG, ImageTransformEnums.RGB,
			ImageTransformEnums.GBMINUS_2R, ImageTransformEnums.RBMINUS_2G, ImageTransformEnums.RGMINUS_2B,
			ImageTransformEnums.RGB_DIFFS, ImageTransformEnums.H_HSB, ImageTransformEnums.S_HSB,
			ImageTransformEnums.B_HSB, ImageTransformEnums.DERICHE, ImageTransformEnums.DERICHE_COLOR };
	private JComboBox<ImageTransformEnums> transformsComboBox = new JComboBox<ImageTransformEnums>(TRANSFORMS);
	JButton runButton = new JButton("Run");

	private MultiSPOTS96 parent0 = null;
	JComboBoxExperiment editExpList = new JComboBoxExperiment();
	private Registration registration = null;

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(1);
		JPanel panel0 = new JPanel(flowlayout);

		panel0.add(new JLabel("Reference frame"));
		panel0.add(referenceFrameJSpinner);
		add(panel0);

		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(new JLabel("image transformation:"));
		panel1.add(transformsComboBox);
		add(panel1);

		JPanel panel2 = new JPanel(flowlayout);
		panel2.add(runButton);
		add(panel2);

		defineActionListeners();
	}

	private void defineActionListeners() {
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

		referenceFrameJSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData.getSequence() != null) {
					Viewer v = exp.seqCamData.getSequence().getFirstViewer();
					if (v != null) {
						int newValue = (int) referenceFrameJSpinner.getValue();
						if (v.getPositionT() != newValue)
							v.setPositionT((int) newValue);
					}
				}
			}
		});

		transformsComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqKymos != null) {
					int index = transformsComboBox.getSelectedIndex();
					Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) exp.seqCamData.getSequence().getFirstViewer()
							.getCanvas();
					updateTransformFunctionsOfCanvas(exp);
					canvas.setTransformStep1Index(index + 1);
				}
			}
		});

	}

	public void resetFrameIndex() {
		referenceFrameJSpinner.setValue(0);
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T)) {
			Viewer v = event.getSource();
			int t = v.getPositionT();
			if (t >= 0)
				referenceFrameJSpinner.setValue(t);
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

		options.fromFrame = 0;
		options.toFrame = referenceFrame - 1;
		options.referenceFrame = referenceFrame;
		options.expList = parent0.expListCombo;
		options.transformop = (ImageTransformEnums) transformsComboBox.getSelectedItem();
		return options;
	}

	private void stopComputation() {
		if (registration != null && !registration.stopFlag)
			registration.stopFlag = true;
		runButton.setText("Run");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			runButton.setText("Run");
			runButton.removePropertyChangeListener(this);
		}
	}

	private void updateTransformFunctionsOfCanvas(Experiment exp) {
		Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) exp.seqCamData.getSequence().getFirstViewer().getCanvas();
		if (canvas.getTransformStep1ItemCount() < (transformsComboBox.getItemCount() + 1)) {
			canvas.updateTransformsComboStep1(TRANSFORMS);
		}
		int index = transformsComboBox.getSelectedIndex();
		canvas.selectImageTransformFunctionStep1(index + 1, null);
	}

//	private void displayTransform(Experiment exp) {
//		updateTransformFunctionsOfCanvas(exp);
//	}

}
