package plugins.fmp.multiSPOTS96.dlg.a_experiment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperimentLazy;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;

public class Edit extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<EnumXLSColumnHeader> fieldNamesCombo = new JComboBox<EnumXLSColumnHeader>(
			new EnumXLSColumnHeader[] { EnumXLSColumnHeader.EXP_EXPT, EnumXLSColumnHeader.EXP_BOXID,
					EnumXLSColumnHeader.EXP_STIM1, EnumXLSColumnHeader.EXP_CONC1, EnumXLSColumnHeader.EXP_STRAIN,
					EnumXLSColumnHeader.EXP_SEX, EnumXLSColumnHeader.EXP_STIM2, EnumXLSColumnHeader.EXP_CONC2,
					EnumXLSColumnHeader.SPOT_STIM, EnumXLSColumnHeader.SPOT_CONC, EnumXLSColumnHeader.SPOT_VOLUME,
					EnumXLSColumnHeader.CAGE_SEX, EnumXLSColumnHeader.CAGE_STRAIN, EnumXLSColumnHeader.CAGE_AGE });

	private JComboBox<String> fieldOldValuesCombo = new JComboBox<String>();
	private JButton refreshButton = new JButton("Refresh");
	private JTextField newValueTextField = new JTextField(10);
	private JButton applyButton = new JButton("Apply");

	private MultiSPOTS96 parent0 = null;
	JComboBoxExperimentLazy editExpList = new JComboBoxExperimentLazy();

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(1);

		int bWidth = 100;
		int bHeight = 21;

		JPanel panel0 = new JPanel(flowlayout);
		panel0.add(new JLabel("Field name "));
		panel0.add(fieldNamesCombo);
		fieldNamesCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel0.add(refreshButton);
		add(panel0);

		bWidth = 200;
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(new JLabel("Field value "));
		panel1.add(fieldOldValuesCombo);
		fieldOldValuesCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		add(panel1);

		JPanel panel2 = new JPanel(flowlayout);
		panel2.add(new JLabel("replace with"));
		panel2.add(newValueTextField);
		newValueTextField.setPreferredSize(new Dimension(bWidth, bHeight));
		panel2.add(applyButton);
		add(panel2);

		defineActionListeners();
	}

	public void initEditCombos() {
		editExpList.setExperimentsFromList(parent0.expListCombo.getExperimentsAsListNoLoad());
		EnumXLSColumnHeader field = (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem();
		fieldOldValuesCombo.removeAllItems();
		java.util.List<String> values;
		if (parent0.descriptorIndex != null && parent0.descriptorIndex.isReady()) {
			values = parent0.descriptorIndex.getDistinctValues(field);
		} else {
			// fallback: use existing lightweight scan
			editExpList.getFieldValuesToComboLightweight(fieldOldValuesCombo, field);
			return;
		}
		java.util.Collections.sort(values);
		for (String v : values)
			fieldOldValuesCombo.addItem(v);
	}

	private void defineActionListeners() {
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				applyChange();
				newValueTextField.setText("");
				initEditCombos();
			}
		});

		fieldNamesCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				EnumXLSColumnHeader field = (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem();
				fieldOldValuesCombo.removeAllItems();
				java.util.List<String> values;
				if (parent0.descriptorIndex != null && parent0.descriptorIndex.isReady()) {
					values = parent0.descriptorIndex.getDistinctValues(field);
					java.util.Collections.sort(values);
					for (String v : values)
						fieldOldValuesCombo.addItem(v);
				} else {
					editExpList.getFieldValuesToComboLightweight(fieldOldValuesCombo, field);
				}
			}
		});

		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				initEditCombos();
			}
		});

	}

	void applyChange() {
		int nExperiments = editExpList.getItemCount();
		EnumXLSColumnHeader fieldEnumCode = (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem();
		String oldValue = (String) fieldOldValuesCombo.getSelectedItem();
		String newValue = newValueTextField.getText();

		for (int i = 0; i < nExperiments; i++) {
			Experiment exp = editExpList.getItemAt(i);
			exp.load_MS96_experiment();
			exp.load_MS96_cages();

			exp.replaceFieldValue(fieldEnumCode, oldValue, newValue);

			exp.save_MS96_experiment();
			exp.save_MS96_cages();
		}

		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			exp.load_MS96_spotsMeasures();
			parent0.dlgMeasure.tabCharts.displayChartPanels(exp);
		}

	}

}
