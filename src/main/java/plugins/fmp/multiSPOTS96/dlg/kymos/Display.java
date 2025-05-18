package plugins.fmp.multiSPOTS96.dlg.kymos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.SequenceKymos;
import plugins.fmp.multiSPOTS96.experiment.cages.Cage;
import plugins.fmp.multiSPOTS96.experiment.spots.Spot;
import plugins.fmp.multiSPOTS96.tools.Directories;
import plugins.fmp.multiSPOTS96.tools.ViewerFMP;
import plugins.fmp.multiSPOTS96.tools.canvas2D.Canvas2D_3Transforms;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformEnums;

public class Display extends JPanel implements ViewerListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;

	public int indexImagesCombo = -1;
	public JComboBox<String> kymographsCombo = new JComboBox<String>(new String[] { "none" });
	private JComboBox<String> viewsCombo = new JComboBox<String>();
	private JButton previousButton = new JButton("<");
	private JButton nextButton = new JButton(">");
	private JCheckBox sumCheckbox = new JCheckBox("sum (black)", true);
	private JCheckBox sumCleanCheckbox = new JCheckBox("sumCLEAN (green)", true);
	private JCheckBox flyPresentCheckbox = new JCheckBox("fly present (blue)", true);
	private ImageTransformEnums[] transforms = new ImageTransformEnums[] { ImageTransformEnums.SORT_SUMDIFFCOLS,
			ImageTransformEnums.SORT_CHAN0COLS };
	JComboBox<ImageTransformEnums> spotsTransformsComboBox = new JComboBox<ImageTransformEnums>(transforms);
	JToggleButton spotsViewButton = new JToggleButton("View");
	private MultiSPOTS96 parent0 = null;
	private boolean isActionEnabled = true;

	void init(GridLayout capLayout, MultiSPOTS96 parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);

		JPanel panel1 = new JPanel(layout);
		panel1.add(new JLabel("views"));
		panel1.add(viewsCombo);
		panel1.add(new JLabel(" kymograph"));
		int bWidth = 30;
		int bHeight = 21;
		panel1.add(previousButton, BorderLayout.WEST);
		previousButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(kymographsCombo, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(nextButton, BorderLayout.EAST);
		add(panel1);

		JPanel panel2 = new JPanel(layout);
		panel2.add(sumCheckbox);
		panel2.add(sumCleanCheckbox);
		panel2.add(flyPresentCheckbox);
		add(panel2);

		JPanel panel3 = new JPanel(layout);
		panel3.add(new JLabel("transform image"));
		panel3.add(spotsTransformsComboBox);
		panel3.add(spotsViewButton);
		add(panel3);

		defineActionListeners();
	}

	private void defineActionListeners() {
		kymographsCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (isActionEnabled)
					displayUpdateOnSwingThread();
			}
		});

		sumCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayROIs("sum", sumCheckbox.isSelected());
			}
		});

		sumCleanCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayROIs("clean", sumCleanCheckbox.isSelected());
			}
		});

		flyPresentCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				displayROIs("flyPresent", flyPresentCheckbox.isSelected());
			}
		});

		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				int isel = kymographsCombo.getSelectedIndex() + 1;
				if (isel < kymographsCombo.getItemCount()) {
					isel = selectKymographImage(isel);
					selectKymographComboItem(isel);
				}
			}
		});

		previousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				int isel = kymographsCombo.getSelectedIndex() - 1;
				if (isel < kymographsCombo.getItemCount()) {
					isel = selectKymographImage(isel);
					selectKymographComboItem(isel);
				}
			}
		});

		viewsCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				String localString = (String) viewsCombo.getSelectedItem();
				if (localString != null && localString.contains("."))
					localString = null;
				if (isActionEnabled)
					changeBinSubdirectory(localString);
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
	}

	public void transferSpotNamesToComboBox(Experiment exp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				kymographsCombo.removeAllItems();
				for (Cage cage : exp.cagesArray.cagesList) {
					Collections.sort(cage.spotsArray.spotsList);
					int nspotsArray = cage.spotsArray.spotsList.size();
					for (int i = 0; i < nspotsArray; i++) {
						Spot spot = cage.spotsArray.spotsList.get(i);
						String name = spot.getRoi().getName();
						kymographsCombo.addItem(name);
					}
				}
			}
		});
	}

	public void displayROIsAccordingToUserSelection() {
		displayROIs("level", sumCheckbox.isSelected());
	}

	private void displayROIs(String filter, boolean visible) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		Viewer v = exp.seqKymos.seq.getFirstViewer();
		if (v == null)
			return;
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers != null) {
			for (Layer layer : layers) {
				ROI roi = layer.getAttachedROI();
				if (roi != null) {
					String cs = roi.getName();
					if (cs.contains(filter))
						layer.setVisible(visible);
				}
			}
		}
	}

	void displayON() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null) {
			SequenceKymos seqKymographs = exp.seqKymos;
			if (seqKymographs == null || seqKymographs.seq == null)
				return;

			ArrayList<Viewer> vList = seqKymographs.seq.getViewers();
			if (vList.size() == 0) {
				ViewerFMP v = new ViewerFMP(seqKymographs.seq, true, true);
				List<String> list = IcyCanvas.getCanvasPluginNames();
				String pluginName = list.stream().filter(s -> s.contains("Canvas2D_3Transforms")).findFirst()
						.orElse(null);
				v.setCanvas(pluginName);
				v.setRepeat(false);
				v.addListener(this);

				JToolBar toolBar = v.getToolBar();
				Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) v.getCanvas();
				canvas.customizeToolbarStep2(toolBar);

				placeKymoViewerNextToCamViewer(exp);

				int isel = seqKymographs.currentFrame;
				isel = selectKymographImage(isel);
				selectKymographComboItem(isel);
				// canvas.selectImageTransformFunctionStep2(2);

				String title = kymographsCombo.getItemAt(0);
				v.setTitle(title);
			}
		}
	}

	void placeKymoViewerNextToCamViewer(Experiment exp) {
		Sequence seqCamData = exp.seqCamData.seq;
		Viewer viewerCamData = seqCamData.getFirstViewer();
		if (viewerCamData == null)
			return;

		Rectangle rectViewerCamData = viewerCamData.getBounds();
		Sequence seqKymograph = exp.seqKymos.seq;

		Rectangle rectViewerKymograph = (Rectangle) rectViewerCamData.clone();
		Rectangle rectImageKymograph = seqKymograph.getBounds2D();
		int desktopwidth = Icy.getMainInterface().getMainFrame().getDesktopWidth();

		rectViewerKymograph.width = (int) rectImageKymograph.getWidth();

		if ((rectViewerKymograph.width + rectViewerKymograph.x) > desktopwidth) {
			rectViewerKymograph.x = 0;
			rectViewerKymograph.y = rectViewerCamData.y + rectViewerCamData.height + 5;
			rectViewerKymograph.width = desktopwidth;
			rectViewerKymograph.height = rectImageKymograph.height;
		} else
			rectViewerKymograph.translate(5 + rectViewerCamData.width, 0);

		Viewer viewerKymograph = seqKymograph.getFirstViewer();
		if (viewerKymograph == null)
			return;
		viewerKymograph.setBounds(rectViewerKymograph);
		((Canvas2D) viewerKymograph.getCanvas()).setFitToCanvas(false);
	}

	void displayOFF() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null || exp.seqKymos == null)
			return;
		ArrayList<Viewer> vList = exp.seqKymos.seq.getViewers();
		if (vList.size() > 0) {
			for (Viewer v : vList)
				v.close();
			vList.clear();
		}
	}

	public void displayUpdateOnSwingThread() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int isel = selectKymographImage(displayUpdate());
				selectKymographComboItem(isel);
			}
		});
	}

	public void displayUpdateOnSwingThread2(int isel, int imethod) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				selectKymographImage(isel);
				selectKymographComboItem(isel);
				spotsTransformsComboBox.setSelectedIndex(imethod);
				parent0.dlgKymos.tabDisplay.spotsTransformsComboBox.setSelectedIndex(1);
				parent0.dlgKymos.tabDisplay.spotsViewButton.setSelected(true);
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					updateTransformFunctionsOfCanvas(exp);
			}
		});
	}

	int displayUpdate() {
		int item = -1;
		if (kymographsCombo.getItemCount() < 1)
			return item;

		displayON();

		item = kymographsCombo.getSelectedIndex();
		if (item < 0) {
			item = indexImagesCombo >= 0 ? indexImagesCombo : 0;
			indexImagesCombo = -1;
		}
		return item;
	}

	private void selectKymographComboItem(int isel) {
		int icurrent = kymographsCombo.getSelectedIndex();
		if (isel >= 0 && isel != icurrent)
			kymographsCombo.setSelectedIndex(isel);
	}

	public int selectKymographImage(int isel) {
		int selectedImageIndex = -1;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return selectedImageIndex;

		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || seqKymos.seq == null)
			return selectedImageIndex;
		if (seqKymos.seq.isUpdating())
			return selectedImageIndex;

		if (isel < 0)
			isel = 0;
		if (isel >= seqKymos.getImagesList().size())
			isel = seqKymos.getImagesList().size() - 1;

		seqKymos.seq.beginUpdate();
		Viewer v = seqKymos.seq.getFirstViewer();
		if (v != null) {
			int icurrent = v.getPositionT();
			if (icurrent != isel)
				v.setPositionT(isel);
			seqKymos.currentFrame = isel;
		}
		seqKymos.seq.endUpdate();

		selectedImageIndex = seqKymos.currentFrame;
		displayROIsAccordingToUserSelection();
		selectSpot(exp, selectedImageIndex);
		return selectedImageIndex;
	}

	private void selectSpot(Experiment exp, int isel) {
//		int i = 0;
//		for (Cage cage : exp.cagesArray.cagesList) {
//			SpotsArray spotsArray = cage.spotsArray;
//			for (Spot spot : spotsArray.spotsList) {
//				spot.getRoi().setSelected(isel == i);
//				spot.getRoi().setFocused(isel == i);
//				i++;
//			}
//		}
		String spotName = (String) kymographsCombo.getSelectedItem();
		Spot spot = exp.cagesArray.getSpotFromROIName(spotName);
		if (spot != null) {
			exp.seqCamData.centerOnRoi(spot.getRoi());
			exp.seqCamData.seq.setSelectedROI(spot.getRoi());
		}
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T)) {
			Viewer v = event.getSource();
			int t = v.getPositionT();
			if (t >= 0)
				selectKymographComboItem(t);

			String title = kymographsCombo.getItemAt(t);
			v.setTitle(title);
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	public void updateResultsAvailable(Experiment exp) {
		isActionEnabled = false;
		// isActionEnabled: hack to select the right directory and then add subsequent
		// available dir without calling actionListener
		// see
		// https://stackoverflow.com/questions/13434688/calling-additem-on-an-empty-jcombobox-triggers-actionperformed-event
		// when JComboBox is empty, adding the first item will trigger setSelected(0)
		viewsCombo.removeAllItems();
		List<String> list = Directories.getSortedListOfSubDirectoriesWithTIFF(exp.getResultsDirectory());
		for (int i = 0; i < list.size(); i++) {
			String dirName = list.get(i);
			if (dirName == null || dirName.contains(Experiment.RESULTS))
				dirName = ".";
			viewsCombo.addItem(dirName);
		}
		isActionEnabled = true;

		String select = exp.getBinSubDirectory();
		if (select == null)
			select = ".";
		viewsCombo.setSelectedItem(select);
	}

	public String getBinSubdirectory() {
		String name = (String) viewsCombo.getSelectedItem();
		if (name != null && !name.contains("bin_"))
			name = null;
		return name;
	}

	private void changeBinSubdirectory(String localString) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null || localString == null || exp.getBinSubDirectory().contains(localString))
			return;

		parent0.expListCombo.stringExpBinSubDirectory = localString;
		exp.setBinSubDirectory(localString);
		exp.seqKymos.seq.close();
		exp.zloadKymographs();
		parent0.dlgKymos.updateDialogs(exp);
	}

	private void displayTransform(Experiment exp) {
		if (spotsViewButton.isSelected()) {
			updateTransformFunctionsOfCanvas(exp);
		} else {
			Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) exp.seqKymos.seq.getFirstViewer().getCanvas();
			canvas.transformsComboStep2.setSelectedIndex(0);
		}
	}

	private void updateTransformFunctionsOfCanvas(Experiment exp) {
		Viewer viewer = exp.seqKymos.seq.getFirstViewer();
		if (viewer != null) {
			Canvas2D_3Transforms canvas = (Canvas2D_3Transforms) viewer.getCanvas();
			canvas.updateTransformsComboStep2(transforms);
			int index = spotsTransformsComboBox.getSelectedIndex();
			canvas.selectImageTransformFunctionStep2(index + 1, null);
		}
	}

}
