package plugins.fmp.multiSPOTS96.dlg.c_kymos;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.util.StringUtil;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.sequence.KymographConfiguration;
import plugins.fmp.multiSPOTS96.experiment.sequence.SequenceKymos;
import plugins.fmp.multiSPOTS96.series.BuildSeriesOptions;
import plugins.fmp.multiSPOTS96.series.BuildSpotsKymos;

public class Create extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	private String detectString = "Start";

	JButton startComputationButton = new JButton("Start");
	JCheckBox allSeriesCheckBox = new JCheckBox("ALL series (current to last)", false);
	JCheckBox concurrentDisplayCheckBox = new JCheckBox("concurrent display", false);

	Long val = 0L; // set your own value, I used to check if it works
	Long min = 0L;
	Long max = 10000L;
	Long step = 1L;
	Long maxLast = 99999999L;
	JSpinner kymosFrameFirstJSpinner = new JSpinner(new SpinnerNumberModel(val, min, max, step));
	JSpinner kymosFrameLastJSpinner = new JSpinner(new SpinnerNumberModel(maxLast, step, maxLast, step));
	JSpinner kymosFrameDeltaJSpinner = new JSpinner(
			new SpinnerNumberModel((Long) 1L, (Long) 1L, (Long) 100L, (Long) 1L));

	EnumStatusComputation sComputation = EnumStatusComputation.START_COMPUTATION;
	private MultiSPOTS96 parent0 = null;
	private BuildSpotsKymos threadBuildKymo = null;

	// -----------------------------------------------------

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT);

		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout) panel0.getLayout()).setVgap(1);
		panel0.add(startComputationButton);
		panel0.add(allSeriesCheckBox);
		panel0.add(concurrentDisplayCheckBox);
		add(panel0);

		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add(new JLabel("Frame ", SwingConstants.RIGHT));
		panel1.add(kymosFrameFirstJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(kymosFrameLastJSpinner);
		add(panel1);

		JPanel panel2 = new JPanel(layoutLeft);
		panel2.add(new JLabel("Ratio: 1 to ", SwingConstants.RIGHT));
		panel2.add(kymosFrameDeltaJSpinner);
		panel2.add(new JLabel(" image(s)"));
		add(panel2);

		defineActionListeners();
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

		allSeriesCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected())
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
			}
		});

	}

	private void setExptParms(Experiment exp) {
		long bin_ms = exp.seqCamData.getTimeManager().getBinImage_ms();
		if (exp.seqKymos == null) {
			// Use builder pattern with quality processing for dialog operations
			exp.seqKymos = SequenceKymos.kymographBuilder()
					.withConfiguration(KymographConfiguration.qualityProcessing()).build();
		}
		exp.seqKymos.getImageLoader().setAbsoluteIndexFirstImage((long) kymosFrameFirstJSpinner.getValue());
		exp.seqKymos.getTimeManager().setDeltaImage((long) kymosFrameDeltaJSpinner.getValue());
		exp.seqKymos.getTimeManager()
				.setBinFirst_ms(exp.seqCamData.getImageLoader().getAbsoluteIndexFirstImage() * bin_ms);
		exp.seqKymos.getTimeManager().setBinLast_ms(((long) kymosFrameLastJSpinner.getValue()) * bin_ms);
	}

	public void getExptParms(Experiment exp) {
		long bin_ms = exp.seqCamData.getTimeManager().getBinImage_ms();
		if (bin_ms == 0) {
			exp.loadFileIntervalsFromSeqCamData();
			bin_ms = exp.seqCamData.getTimeManager().getBinImage_ms();
		}
		if (exp.seqKymos == null) {
			// Use builder pattern with default configuration for parameter retrieval
			exp.seqKymos = SequenceKymos.kymographBuilder()
					.withConfiguration(KymographConfiguration.defaultConfiguration()).build();
		}
		long dFirst = exp.seqKymos.getImageLoader().getAbsoluteIndexFirstImage();
		kymosFrameFirstJSpinner.setValue(dFirst);
		kymosFrameDeltaJSpinner.setValue(exp.seqCamData.getTimeManager().getDeltaImage());
		if (exp.seqCamData.getTimeManager().getBinLast_ms() <= 0)
			exp.seqCamData.getTimeManager()
					.setBinLast_ms((long) (exp.seqCamData.getImageLoader().getNTotalFrames()) * bin_ms);
		long dLast = (long) exp.seqCamData.getTimeManager().getBinLast_ms() / bin_ms;
		kymosFrameLastJSpinner.setValue(dLast);
		exp.getFileIntervalsFromSeqCamData();
	}

	private BuildSeriesOptions initBuildParameters(Experiment exp) {
		setExptParms(exp);
		BuildSeriesOptions options = new BuildSeriesOptions();
		options.expList = parent0.expListCombo;
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allSeriesCheckBox.isSelected())
			options.expList.index1 = parent0.expListCombo.getItemCount() - 1;
		else
			options.expList.index1 = options.expList.index0;
		options.isFrameFixed = false;
		exp.loadFileIntervalsFromSeqCamData();
		options.t_Ms_First = exp.seqCamData.getFirstImageMs();
		options.t_Ms_Last = exp.seqCamData.getLastImageMs();
		options.t_Ms_BinDuration = exp.seqCamData.getTimeManager().getBinImage_ms();
		options.doCreateBinDir = true;
		options.parent0Rect = parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = Experiment.BIN + options.t_Ms_BinDuration / 1000;
		options.concurrentDisplay = concurrentDisplayCheckBox.isSelected();
		return options;
	}

	private void startComputation() {
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			exp.saveSpotsArray_file();

		threadBuildKymo = new BuildSpotsKymos();
		threadBuildKymo.options = initBuildParameters(exp);

		threadBuildKymo.addPropertyChangeListener(this);
		threadBuildKymo.execute();
		startComputationButton.setText("STOP");
	}

	private void stopComputation() {
		if (threadBuildKymo != null && !threadBuildKymo.stopFlag) {
			threadBuildKymo.stopFlag = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			startComputationButton.setText(detectString);
			parent0.dlgKymos.tabDisplay.displayUpdateOnSwingThread2(0, 1);
		}
	}

}