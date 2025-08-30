package plugins.fmp.multiSPOTS96.dlg.a_experiment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentProperties;
import plugins.fmp.multiSPOTS96.tools.DialogTools;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperimentLazy;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxModelSorted;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;
import plugins.fmp.multiSPOTS96.tools.LazyExperiment;

public class Filter extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String> stim1Combo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> conc1Combo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> boxIDCombo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> exptCombo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> strainCombo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> sexCombo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> stim2Combo = new JComboBox<String>(new JComboBoxModelSorted());
	private JComboBox<String> conc2Combo = new JComboBox<String>(new JComboBoxModelSorted());

	private JCheckBox experimentCheck = new JCheckBox(EnumXLSColumnHeader.EXP_EXPT.toString());
	private JCheckBox boxIDCheck = new JCheckBox(EnumXLSColumnHeader.EXP_BOXID.toString());
	private JCheckBox stim1Check = new JCheckBox(EnumXLSColumnHeader.EXP_STIM1.toString());
	private JCheckBox conc1Check = new JCheckBox(EnumXLSColumnHeader.EXP_CONC1.toString());
	private JCheckBox strainCheck = new JCheckBox(EnumXLSColumnHeader.EXP_STRAIN.toString());
	private JCheckBox sexCheck = new JCheckBox(EnumXLSColumnHeader.EXP_SEX.toString());
	private JCheckBox stim2Check = new JCheckBox(EnumXLSColumnHeader.EXP_STIM2.toString());
	private JCheckBox conc2Check = new JCheckBox(EnumXLSColumnHeader.EXP_CONC2.toString());

	private JButton applyButton = new JButton("Apply");
	private JButton clearButton = new JButton("Clear");

	private MultiSPOTS96 parent0 = null;
	public JComboBoxExperimentLazy filterExpList = new JComboBoxExperimentLazy();

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		this.parent0 = parent0;
		GridBagLayout layoutThis = new GridBagLayout();
		setLayout(layoutThis);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.BASELINE;
		c.ipadx = 0;
		c.ipady = 0;
		c.insets = new Insets(1, 2, 1, 2);
		int delta1 = 1;
		int delta2 = 3;

		// line 0
		c.gridx = 0;
		c.gridy = 0;
		DialogTools.addFiveComponentOnARow(this, experimentCheck, exptCombo, boxIDCheck, boxIDCombo, applyButton, c,
				delta1, delta2);
		// line 2
		c.gridy = 1;
		c.gridx = 0;
		DialogTools.addFiveComponentOnARow(this, strainCheck, strainCombo, sexCheck, sexCombo, clearButton, c, delta1,
				delta2);
		// line 1
		c.gridy = 2;
		c.gridx = 0;
		DialogTools.addFiveComponentOnARow(this, stim1Check, stim1Combo, conc1Check, conc1Combo, null, c, delta1,
				delta2);
		// line 3
		c.gridy = 3;
		c.gridx = 0;
		DialogTools.addFiveComponentOnARow(this, stim2Check, stim2Combo, conc2Check, conc2Combo, null, c, delta1,
				delta2);

		defineActionListeners();
	}

	public void initCombos() {
		if (!parent0.dlgBrowse.loadSaveExperiment.filteredCheck.isSelected())
			filterExpList.setExperimentsFromList(parent0.expListCombo.getExperimentsAsListNoLoad());
		filterExpList.getFieldValuesToComboLightweight(exptCombo, EnumXLSColumnHeader.EXP_EXPT);
		filterExpList.getFieldValuesToComboLightweight(stim1Combo, EnumXLSColumnHeader.EXP_STIM1);
		filterExpList.getFieldValuesToComboLightweight(conc1Combo, EnumXLSColumnHeader.EXP_CONC1);
		filterExpList.getFieldValuesToComboLightweight(boxIDCombo, EnumXLSColumnHeader.EXP_BOXID);
		filterExpList.getFieldValuesToComboLightweight(sexCombo, EnumXLSColumnHeader.EXP_SEX);
		filterExpList.getFieldValuesToComboLightweight(strainCombo, EnumXLSColumnHeader.EXP_STRAIN);
		filterExpList.getFieldValuesToComboLightweight(stim2Combo, EnumXLSColumnHeader.EXP_STIM2);
		filterExpList.getFieldValuesToComboLightweight(conc2Combo, EnumXLSColumnHeader.EXP_CONC2);
	}

	private void defineActionListeners() {
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				filterExperimentList(true);
				parent0.dlgExperiment.tabsPane.setSelectedIndex(0);
			}
		});

		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				filterExperimentList(false);
			}
		});
	}

	public void filterExperimentList(boolean setFilter) {
		if (setFilter) {
			parent0.expListCombo.setExperimentsFromList(filterAllItems());
		} else {
			clearAllCheckBoxes();
			parent0.expListCombo.setExperimentsFromList(filterExpList.getExperimentsAsListNoLoad());
		}

		if (parent0.expListCombo.getItemCount() > 0)
			parent0.expListCombo.setSelectedIndex(0);
		if (setFilter != parent0.dlgBrowse.loadSaveExperiment.filteredCheck.isSelected())
			parent0.dlgBrowse.loadSaveExperiment.filteredCheck.setSelected(setFilter);
	}

	public void clearAllCheckBoxes() {
		boolean select = false;
		experimentCheck.setSelected(select);
		boxIDCheck.setSelected(select);
		stim1Check.setSelected(select);
		conc1Check.setSelected(select);
		strainCheck.setSelected(select);
		sexCheck.setSelected(select);
	}

	private List<Experiment> filterAllItems() {
		List<Experiment> filteredList = new ArrayList<Experiment>(filterExpList.getExperimentsAsListNoLoad());
		if (experimentCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_EXPT, (String) exptCombo.getSelectedItem());
		if (boxIDCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_BOXID, (String) boxIDCombo.getSelectedItem());
		if (stim1Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_STIM1, (String) stim1Combo.getSelectedItem());
		if (conc1Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_CONC1, (String) conc1Combo.getSelectedItem());
		if (sexCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_SEX, (String) sexCombo.getSelectedItem());
		if (strainCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_STRAIN, (String) strainCombo.getSelectedItem());
		if (stim2Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_STIM2, (String) stim2Combo.getSelectedItem());
		if (conc2Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXP_CONC2, (String) conc2Combo.getSelectedItem());
		return filteredList;
	}

	void filterItem(List<Experiment> filteredList, EnumXLSColumnHeader header, String filter) {
		Iterator<Experiment> iterator = filteredList.iterator();
		while (iterator.hasNext()) {
			Experiment exp = iterator.next();
			String value;
			if (exp instanceof LazyExperiment) {
				value = ((LazyExperiment) exp).getFieldValue(header);
			} else {
				ExperimentProperties prop = exp.getProperties();
				value = prop.getExperimentField(header);
			}
			int compare = value.compareTo(filter);
			if (compare != 0)
				iterator.remove();
		}
	}

}
