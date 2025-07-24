package plugins.fmp.multiSPOTS96.dlg.a_experiment;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector2d;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.sequence.DimensionId;
import plugins.fmp.multiSPOTS96.MultiSPOTS96;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.tools.GaspardRigidRegistration;
import plugins.fmp.multiSPOTS96.tools.JComponents.JComboBoxExperiment;

public class CorrectDrift extends JPanel implements ViewerListener {

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
	JButton runButton = new JButton("Run");

	private MultiSPOTS96 parent0 = null;
	JComboBoxExperiment editExpList = new JComboBoxExperiment();

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
					executeRegistration(exp);
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
		int refFrame = (int) referenceFrameJSpinner.getValue();
		correctDriftAndRotation(exp, 0, refFrame - 1, refFrame);

	}

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(CorrectDrift.class.getName());
	/** Minimum threshold for considering a translation significant */
	private static final double MIN_TRANSLATION_THRESHOLD = 0.001;
	/** Minimum threshold for considering a rotation significant */
	private static final double MIN_ROTATION_THRESHOLD = 0.001;

	private boolean correctDriftAndRotation(Experiment exp, int iiFirst, int iiLast, int referenceFrame) {
		ProgressFrame progressBar1 = new ProgressFrame("Analyze stack");

		String fileNameReference = exp.seqCamData.getFileNameFromImageList(referenceFrame);
		final IcyBufferedImage referenceImage = imageIORead(fileNameReference);

		for (int ii = iiFirst; ii < iiLast; ii++) {

			final int t = ii;
			progressBar1.setMessage("Analyze frame: " + t + "//" + iiLast);
			String fileName = exp.seqCamData.getFileNameFromImageList(t);
			if (fileName == null) {
				System.out.println("filename null at t=" + t);
				continue;
			}

			IcyBufferedImage workImage = imageIORead(fileName);
			int referenceChannel = 0;
			Vector2d translation = GaspardRigidRegistration.findTranslation2D(workImage, referenceChannel, referenceImage,
					referenceChannel);
			boolean change = false;
			if (translation.lengthSquared() > MIN_TRANSLATION_THRESHOLD) {
				change = true;
				workImage = GaspardRigidRegistration.applyTranslation2D(workImage, -1, translation, true);
				LOGGER.info("Applied translation correction: (" + translation.x + ", " + translation.y + ")");
			}

			boolean rotate = false;
			if (!change) 
				translation = null;
			double angle = GaspardRigidRegistration.findRotation2D(workImage, referenceChannel, referenceImage, referenceChannel, translation);
			if (Math.abs(angle) > MIN_ROTATION_THRESHOLD) {
				rotate = true;
				workImage = GaspardRigidRegistration.applyRotation2D(workImage, -1, angle, true);
				LOGGER.info("Applied rotation correction: " + Math.toDegrees(angle) + " degrees");
				Vector2d translation2 = GaspardRigidRegistration.getTranslation2D(workImage, referenceImage,
						referenceChannel);
				if (translation2.lengthSquared() > MIN_TRANSLATION_THRESHOLD) {
					workImage = GaspardRigidRegistration.applyTranslation2D(workImage, -1, translation2, true);
				}
			}

			System.out.println("image:" + t + "  change=" + change + "  rotation=" + rotate);
			if (rotate)
				GaspardRigidRegistration.getTranslation2D(workImage, referenceImage, referenceChannel);

			if (change || rotate) {
				File outputfile = new File(fileName);
				RenderedImage image = ImageUtil.toRGBImage(workImage);
				boolean success = ImageUtil.save(image, "jpg", outputfile);
				System.out.println("save file " + fileName + " --->" + success);
			}
		}

		progressBar1.close();
		return true;
	}

	public IcyBufferedImage imageIORead(String name) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File(name));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return IcyBufferedImage.createFrom(image);
	}

}
