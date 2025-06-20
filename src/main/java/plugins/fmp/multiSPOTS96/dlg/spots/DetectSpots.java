package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import icy.util.StringUtil;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.series.DetectSpotsOutline;
import plugins.fmp.multiSPOTS96.tools.canvas2D.Canvas2D_3Transforms;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformEnums;
import plugins.fmp.multiSPOTS96.tools.overlay.OverlayThreshold;

public class DetectSpots extends JPanel implements ChangeListener, PropertyChangeListener, PopupMenuListener {
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiSPOTS96 parent0 = null;

	private String detectString = "Detect...";
	private JButton startComputationButton = new JButton(detectString);
	private JComboBox<String> allCellsComboBox = new JComboBox<String>(new String[] { "all cages" });
	private JCheckBox allCheckBox = new JCheckBox("ALL (current to last)", false);

	private JLabel spotsFilterLabel = new JLabel("Filter");
	ImageTransformEnums[] transforms = new ImageTransformEnums[] { ImageTransformEnums.R_RGB, ImageTransformEnums.G_RGB,
			ImageTransformEnums.B_RGB, ImageTransformEnums.R2MINUS_GB, ImageTransformEnums.G2MINUS_RB,
			ImageTransformEnums.B2MINUS_RG, ImageTransformEnums.RGB, ImageTransformEnums.GBMINUS_2R,
			ImageTransformEnums.RBMINUS_2G, ImageTransformEnums.RGMINUS_2B, ImageTransformEnums.RGB_DIFFS,
			ImageTransformEnums.H_HSB, ImageTransformEnums.S_HSB, ImageTransformEnums.B_HSB };
	private JComboBox<ImageTransformEnums> spotsTransformsComboBox = new JComboBox<ImageTransformEnums>(transforms);
	private String[] directions = new String[] { "threshold >", "threshold <" };
	private JComboBox<String> spotsDirectionComboBox = new JComboBox<String>(directions);
	private JSpinner spotsThresholdSpinner = new JSpinner(new SpinnerNumberModel(35, 0, 255, 1));
	private JToggleButton spotsViewButton = new JToggleButton("View");
	private JCheckBox spotsOverlayCheckBox = new JCheckBox("overlay");

	private JCheckBox objectLowsizeCheckBox = new JCheckBox("size >");
	private JSpinner objectLowsizeSpinner = new JSpinner(new SpinnerNumberModel(50, 0, 9999, 1));

	private JCheckBox objectUpsizeCheckBox = new JCheckBox("<");
	private JSpinner objectUpsizeSpinner = new JSpinner(new SpinnerNumberModel(500, 0, 9999, 1));
	private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 255, 1));

	private JSpinner jitterTextField = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JSpinner limitRatioSpinner = new JSpinner(new SpinnerNumberModel(4, 0, 1000, 1));

	private DetectSpotsOutline detectSpots = null;
	private OverlayThreshold overlayThreshold = null;

	// ----------------------------------------------------

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT);
		layoutLeft.setVgap(0);

		JPanel panel0 = new JPanel(layoutLeft);
		panel0.add(startComputationButton);
		panel0.add(allCellsComboBox);
		panel0.add(allCheckBox);
		add(panel0);

		allCellsComboBox.addPopupMenuListener(this);

		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add(spotsFilterLabel);
		panel1.add(spotsTransformsComboBox);
		panel1.add(spotsDirectionComboBox);
		panel1.add(spotsThresholdSpinner);
		panel1.add(spotsViewButton);
		panel1.add(spotsOverlayCheckBox);
		add(panel1);
		add(panel1);

		JPanel panel2 = new JPanel(layoutLeft);
		panel2.add(objectLowsizeCheckBox);
		panel2.add(objectLowsizeSpinner);
		objectLowsizeSpinner.setPreferredSize(new Dimension(80, 20));
		panel2.add(objectUpsizeCheckBox);
		panel2.add(objectUpsizeSpinner);
		objectUpsizeSpinner.setPreferredSize(new Dimension(80, 20));
		add(panel2);

		JPanel panel3 = new JPanel(layoutLeft);
		panel3.add(new JLabel("length/width<"));
		panel3.add(limitRatioSpinner);
		limitRatioSpinner.setPreferredSize(new Dimension(40, 20));
		panel3.add(new JLabel("jitter <="));
		panel3.add(jitterTextField);
		jitterTextField.setPreferredSize(new Dimension(40, 20));
		add(panel3);

		spotsTransformsComboBox.setSelectedItem(ImageTransformEnums.RGB_DIFFS);
		spotsDirectionComboBox.setSelectedIndex(1);

		defineActionListeners();
		defineItemListeners();
	}

	private void defineItemListeners() {

		thresholdSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateOverlayThreshold();
			}
		});
	}

	private void defineActionListeners() {
		startComputationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (startComputationButton.getText().equals(detectString))
					startComputation();
				else
					stopComputation();
			}
		});

		allCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Color color = Color.BLACK;
				if (allCheckBox.isSelected())
					color = Color.RED;
				allCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
			}
		});

		spotsViewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					displayTransform(exp);
			}
		});

		spotsOverlayCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (spotsOverlayCheckBox.isSelected()) {
						updateOverlay(exp);
						updateOverlayThreshold();
					} else {
						removeOverlay(exp);
						overlayThreshold = null;
					}
				}
			}
		});

		spotsDirectionComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				updateOverlayThreshold();
			}
		});

		spotsThresholdSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				updateOverlayThreshold();
			}
		});

	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				exp.cagesArray.detect_threshold = (int) thresholdSpinner.getValue();
		}
	}

	public void updateOverlay(Experiment exp) {
		if (overlayThreshold == null) {
			overlayThreshold = new OverlayThreshold(exp.seqCamData.seq);
		} else {
			exp.seqCamData.seq.removeOverlay(overlayThreshold);
			overlayThreshold.setSequence(exp.seqCamData.seq);
		}
		overlayThreshold.setReferenceImage(exp.seqCamData.refImage);
		exp.seqCamData.seq.addOverlay(overlayThreshold);
	}

	private void displayTransform(Experiment exp) {
		boolean displayCheckOverlay = false;
		if (spotsViewButton.isSelected()) {
			updateTransformFunctionsOfCanvas(exp);
			displayCheckOverlay = true;
		} else {
			removeOverlay(exp);
			spotsOverlayCheckBox.setSelected(false);
			Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) exp.seqCamData.seq.getFirstViewer().getCanvas();
			canvas.transformsComboStep1.setSelectedIndex(0);
		}
		spotsOverlayCheckBox.setEnabled(displayCheckOverlay);
	}

	private void updateTransformFunctionsOfCanvas(Experiment exp) {
		Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) exp.seqCamData.seq.getFirstViewer().getCanvas();
		if (canvas.transformsComboStep1.getItemCount() < (spotsTransformsComboBox.getItemCount() + 1)) {
			canvas.updateTransformsComboStep1(transforms);
		}
		int index = spotsTransformsComboBox.getSelectedIndex();
		canvas.selectImageTransformFunctionStep1(index + 1, null);
	}

	void updateOverlayThreshold() {
		if (!spotsOverlayCheckBox.isSelected())
			return;

		if (overlayThreshold == null) {
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				updateOverlay(exp);
		}

		boolean ifGreater = (spotsDirectionComboBox.getSelectedIndex() == 0);
		int threshold = (int) spotsThresholdSpinner.getValue();
		ImageTransformEnums transform = (ImageTransformEnums) spotsTransformsComboBox.getSelectedItem();
		overlayThreshold.setThresholdSingle(threshold, transform, ifGreater);
		overlayThreshold.painterChanged();
	}

	private BuildSeriesOptions initTrackParameters(Experiment exp) {
		BuildSeriesOptions options = detectSpots.options;
		options.expList = parent0.expListCombo;
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allCheckBox.isSelected())
			options.expList.index1 = options.expList.getItemCount() - 1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();

		options.btrackWhite = (spotsDirectionComboBox.getSelectedIndex() == 1);
		options.blimitLow = objectLowsizeCheckBox.isSelected();
		options.blimitUp = objectUpsizeCheckBox.isSelected();
		options.limitLow = (int) objectLowsizeSpinner.getValue();
		options.limitUp = (int) objectUpsizeSpinner.getValue();
		options.limitRatio = (int) limitRatioSpinner.getValue();
		options.jitter = (int) jitterTextField.getValue();
		options.thresholdDiff = (int) thresholdSpinner.getValue();
		options.detectFlies = true;

		options.parent0Rect = parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = exp.getBinSubDirectory();

		options.fromFrame = exp.seqCamData.currentFrame;
		return options;
	}

	void startComputation() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		parent0.dlgBrowse.panelLoadSave.closeViewsForCurrentExperiment(exp);

		detectSpots = new DetectSpotsOutline();
		detectSpots.options = initTrackParameters(exp);
		detectSpots.stopFlag = false;
		detectSpots.addPropertyChangeListener(this);
		detectSpots.execute();
		startComputationButton.setText("STOP");
	}

	private void stopComputation() {
		if (detectSpots != null && !detectSpots.stopFlag)
			detectSpots.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			startComputationButton.setText(detectString);
			parent0.dlgKymos.tabDisplay.selectKymographImage(parent0.dlgKymos.tabDisplay.indexImagesCombo);
			parent0.dlgKymos.tabDisplay.indexImagesCombo = -1;
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		int nitems = 1;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			nitems = exp.cagesArray.cagesList.size() + 1;
		if (allCellsComboBox.getItemCount() != nitems) {
			allCellsComboBox.removeAllItems();
			allCellsComboBox.addItem("all cages");
			for (Cage cage : exp.cagesArray.cagesList) {
				allCellsComboBox.addItem(Integer.toString(cage.prop.cageID));
			}
		}
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// TODO Auto-generated method stub

	}

	void removeOverlay(Experiment exp) {
		if (exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(overlayThreshold);
	}

}
