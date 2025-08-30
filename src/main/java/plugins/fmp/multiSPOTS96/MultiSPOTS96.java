package plugins.fmp.multiSPOTS96;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.GeneralPreferences;
import plugins.fmp.multiSPOTS96.dlg.a_browse._DlgBrowse_;
import plugins.fmp.multiSPOTS96.dlg.a_experiment._DlgExperiment_;
import plugins.fmp.multiSPOTS96.dlg.b_spots._DlgSpots_;
import plugins.fmp.multiSPOTS96.dlg.d_spotsMeasures._DlgSpotMeasure_;
import plugins.fmp.multiSPOTS96.dlg.f_excel._DlgExcel_;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperimentLazy;
import plugins.fmp.multiSPOTS96.tools.DescriptorIndex;
import plugins.fmp.multiSPOTS96.tools.DescriptorsIO;

public class MultiSPOTS96 extends PluginActionable {

	public IcyFrame mainFrame = new IcyFrame("multiSPOTS96 August 30, 2025", true, true, true, true);
	public JComboBoxExperimentLazy expListCombo = new JComboBoxExperimentLazy();
    public DescriptorIndex descriptorIndex = new DescriptorIndex();

	public _DlgBrowse_ dlgBrowse = new _DlgBrowse_();
	public _DlgExperiment_ dlgExperiment = new _DlgExperiment_();
	public _DlgSpots_ dlgSpots = new _DlgSpots_();
	public _DlgSpotMeasure_ dlgMeasure = new _DlgSpotMeasure_();
	public _DlgExcel_ dlgExcel = new _DlgExcel_();

	public JTabbedPane tabsPane = new JTabbedPane();

	// -------------------------------------------------------------------

	@Override
	public void run() {
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();

		dlgBrowse.init(mainPanel, "Browse", this);
		dlgExperiment.init(mainPanel, "Experiment", this);
		dlgSpots.init(mainPanel, "Spots", this);
		dlgMeasure.init(mainPanel, "Measure spots", this);
		dlgExcel.init(mainPanel, "Export", this);

		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.WEST);

		// Add a lightweight "Rebuild descriptors" button in the title bar menu area
		javax.swing.JButton rebuildBtn = new javax.swing.JButton("Rebuild descriptors");
		rebuildBtn.addActionListener(e -> {
			icy.gui.frame.progress.ProgressFrame pf = new icy.gui.frame.progress.ProgressFrame("Rebuild descriptors");
			pf.setLength(expListCombo.getItemCount());
			new javax.swing.SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					for (int i = 0; i < expListCombo.getItemCount(); i++) {
						pf.setMessage("Experiment " + (i + 1) + "/" + expListCombo.getItemCount());
						plugins.fmp.multiSPOTS96.experiment.Experiment exp = expListCombo.getItemAt(i);
						DescriptorsIO.buildFromExperiment(exp);
						pf.incPosition();
					}
					return null;
				}

				@Override
				protected void done() {
					pf.close();
					descriptorIndex.preloadFromCombo(expListCombo, new Runnable() {
						@Override
						public void run() {
							dlgExperiment.tabInfos.initCombos();
							dlgExperiment.tabFilter.initCombos();
						}
					});
				}
			}.execute();
		});
		javax.swing.JPanel actionsPanel = new javax.swing.JPanel();
		actionsPanel.add(rebuildBtn);
		mainFrame.add(actionsPanel, BorderLayout.NORTH);

		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	public static void main(String[] args) {
		Icy.main(args);
		GeneralPreferences.setSequencePersistence(false);
		PluginLauncher.start(PluginLoader.getPlugin(MultiSPOTS96.class.getName()));
	}

}
