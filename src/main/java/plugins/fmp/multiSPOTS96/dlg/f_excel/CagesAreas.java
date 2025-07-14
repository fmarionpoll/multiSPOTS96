package plugins.fmp.multiSPOTS96.dlg.f_excel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class CagesAreas extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	JButton exportToXLSButton2 = new JButton("save XLS");

	JCheckBox sumCheckBox = new JCheckBox("sum", true);
	JCheckBox nPixelsCheckBox = new JCheckBox("pi", true);

	void init(GridLayout capLayout) {
		setLayout(capLayout);

		FlowLayout flowLayout0 = new FlowLayout(FlowLayout.LEFT);
		flowLayout0.setVgap(0);
		JPanel panel0 = new JPanel(flowLayout0);
		panel0.add(sumCheckBox);
		panel0.add(nPixelsCheckBox);
		add(panel0);

		JPanel panel1 = new JPanel(flowLayout0);

		add(panel1);

		FlowLayout flowLayout2 = new FlowLayout(FlowLayout.RIGHT);
		flowLayout2.setVgap(0);
		JPanel panel2 = new JPanel(flowLayout2);
		panel2.add(exportToXLSButton2);
		add(panel2);

		sumCheckBox.setEnabled(false);
		nPixelsCheckBox.setEnabled(false);

		defineActionListeners();
	}

	private void defineActionListeners() {
		exportToXLSButton2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				firePropertyChange("EXPORT_CAGESMEASURES", false, true);
			}
		});

	}

}
