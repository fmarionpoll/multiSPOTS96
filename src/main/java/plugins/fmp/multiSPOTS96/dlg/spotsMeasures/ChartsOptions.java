package plugins.fmp.multiSPOTS96.dlg.spotsMeasures;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.chart.ChartSpots;



public class ChartsOptions extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	IcyFrame dialogFrame = null;
	private MultiSPOTS96 parent0 = null;
	private ChartSpots chartSpots = null;
	private JSpinner lowerXSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	private JSpinner upperXSpinner = new JSpinner(new SpinnerNumberModel(120, 0, 255, 1));
	private JSpinner lowerYSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	private JSpinner upperYSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 255, 1));
	private JButton setYaxis = new JButton ("set Y axis values");
	private JButton setXaxis = new JButton("set X axis values");

	
	public void initialize(MultiSPOTS96 parent0, ChartSpots chartSpots) {
		this.parent0 = parent0;
		this.chartSpots = chartSpots;
		
		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(new JLabel("x axis values:"));
		panel1.add(lowerXSpinner);
		panel1.add(upperXSpinner);
		panel1.add(setXaxis);
		topPanel.add(panel1);
		
		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel("y axis values:"));
		panel2.add(lowerYSpinner);
		panel2.add(upperYSpinner);
		panel2.add(setYaxis);
		topPanel.add(panel2);
		
		dialogFrame = new IcyFrame("Chart options", true, true);
		dialogFrame.add(topPanel, BorderLayout.NORTH);

		defineActionListeners();
	}
	
	public void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			parent0.dlgSpots.tabFile.saveSpotsArray_file(exp);
		}
	}
	
	private void defineActionListeners() {
		setXaxis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
				}
			}
		});

		setYaxis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) {
				}
			}
		});


	}
}
