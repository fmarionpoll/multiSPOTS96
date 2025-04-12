package plugins.fmp.multiSPOTS96.dlg.spots;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;

public class Infos extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;

	private JButton editCagesButton = new JButton("Edit cages infos...");
	private JButton editSpotsButton = new JButton("Edit spots infos...");
	private InfosCageTable infosCageTable = null;
	private InfosSpotTable infosSpotTable = null;
//	private JSpinner nFliesPerCageJSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 500, 1));
//	private String[] flyString = new String[] { "fly", "flies" };
//	private JLabel flyLabel = new JLabel(flyString[0]);

	private MultiSPOTS96 parent0 = null;

	void init(GridLayout gridLayout, MultiSPOTS96 parent0) {
		setLayout(gridLayout);
		this.parent0 = parent0;

		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT);
		layoutLeft.setVgap(0);

		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(editCagesButton);
		add(panel01);

		JPanel panel02 = new JPanel(layoutLeft);
		panel02.add(editSpotsButton);
		add(panel02);

//		JPanel panel2 = new JPanel(layoutLeft);
//		panel2.add(nFliesPerCageJSpinner);
//		panel2.add(flyLabel);
//		nFliesPerCageJSpinner.setPreferredSize(new Dimension(40, 20));

		declareListeners();
		defineActionListeners();
	}

	private void defineActionListeners() {
//		nFliesPerCageJSpinner.addChangeListener(new ChangeListener() {
//			@Override
//			public void stateChanged(ChangeEvent e) {
//				int i = (int) nFliesPerCageJSpinner.getValue() > 1 ? 1 : 0;
//				flyLabel.setText(flyString[i]);
//				nFliesPerCageJSpinner.requestFocus();
//				int nbFliesPerCage = (int) nFliesPerCageJSpinner.getValue();
//				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
//				if (exp != null) {
//					exp.cagesArray.initCagesAndSpotsWithNFlies(nbFliesPerCage);
//				}
//			}
//		});
	}

	private void declareListeners() {
		editCagesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (infosCageTable != null) {
						infosCageTable.close();
					}
					infosCageTable = new InfosCageTable();
					infosCageTable.initialize(parent0);
					infosCageTable.requestFocus();
				}
			}
		});

		editSpotsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					if (infosSpotTable != null) {
						infosSpotTable.close();
					}
					infosSpotTable = new InfosSpotTable();
					infosSpotTable.initialize(parent0);
					infosSpotTable.requestFocus();
				}
			}
		});
	}

}