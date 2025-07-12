package plugins.fmp.multiSPOTS96.experiment;

import java.awt.Rectangle;
import java.io.File;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.logging.Logger;

import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multiSPOTS96.tools.imageTransform.ImageTransformEnums;

public class SequenceCamData {
    private static final Logger LOGGER = Logger.getLogger(SequenceCamData.class.getName());
    
    private Sequence seq = null;
    private EnumStatus status = EnumStatus.REGULAR;
    private int currentFrame = 0;
    
    // Specialized managers
    private ImageLoader imageLoader = new ImageLoader();
    private TimeManager timeManager = new TimeManager();
    private ROIManager roiManager = new ROIManager();
    private ViewerManager viewerManager = new ViewerManager();
    
    public SequenceCamData() {
        seq = new Sequence();
        status = EnumStatus.FILESTACK;
    }

    public SequenceCamData(String name, IcyBufferedImage image) {
        seq = new Sequence(name, image);
        status = EnumStatus.FILESTACK;
    }
    
    // Image loading methods delegated to ImageLoader
    
    public String getImagesDirectory() {
        return imageLoader.getImagesDirectory();
    }

    public void setImagesDirectory(String directoryString) {
        imageLoader.setImagesDirectory(directoryString);
    }

    public List<String> getImagesList(boolean bsort) {
        return imageLoader.getImagesList(bsort);
    }
    
    public List<String> getImagesList() {
        return imageLoader.getImagesList();
    }

    public void setImagesList(List<String> extImagesList) {
        imageLoader.setImagesList(extImagesList);
    }

    public String getDecoratedImageName(int t) {
        currentFrame = t;
        if (seq != null) {
            return getCSCamFileName() + " [" + t + "/" + (seq.getSizeT() - 1) + "]";
        } else {
            return getCSCamFileName() + "[]";
        }
    }

    public String getCSCamFileName() {
        return imageLoader.getFileName();
    }

    public String getFileNameFromImageList(int t) {
        return imageLoader.getFileNameFromImageList(t);
    }

    public boolean loadImages() {
        return imageLoader.loadImages(this);
    }

    public boolean loadFirstImage() {
        return imageLoader.loadFirstImage(this);
    }

    public void loadImageList(List<String> imagesList) {
        imageLoader.loadImageList(imagesList, this);
    }

    public IcyBufferedImage getSeqImage(int t, int z) {
        if (seq == null) {
            LOGGER.warning("Sequence is null");
            return null;
        }
        if (t < 0 || t >= seq.getSizeT() || z < 0 || z >= seq.getSizeZ()) {
            LOGGER.warning("Invalid t or z index: " + t + ", " + z);
            return null;
        }
        currentFrame = t;
        return seq.getImage(t, z);
    }
    
    // Time methods delegated to TimeManager
    
    public FileTime getFileTimeFromStructuredName(int t) {
        return timeManager.getFileTimeFromStructuredName(imageLoader, t);
    }

    public FileTime getFileTimeFromFileAttributes(int t) {
        return timeManager.getFileTimeFromFileAttributes(imageLoader, t);
    }

    public FileTime getFileTimeFromJPEGMetaData(int t) {
        return timeManager.getFileTimeFromJPEGMetaData(imageLoader, t);
    }
    
    // ROI methods delegated to ROIManager
    
    public void displaySpecificROIs(boolean isVisible, String pattern) {
        roiManager.displaySpecificROIs(seq, isVisible, pattern);
    }

    public List<ROI2D> getROIsContainingString(String string) {
        return roiManager.getROIsContainingString(seq, string);
    }

    public void removeROIsContainingString(String string) {
        roiManager.removeROIsContainingString(seq, string);
    }

    public void centerOnRoi(ROI2D roi) {
        roiManager.centerOnRoi(seq, roi);
    }

    public void selectRoi(ROI2D roi, boolean select) {
        roiManager.selectRoi(seq, roi, select);
    }
    
    // Viewer and overlay methods delegated to ViewerManager
    
    public void displayViewerAtRectangle(Rectangle parent0Rect) {
        viewerManager.displayViewerAtRectangle(seq, parent0Rect);
    }
    
    public void updateOverlay() {
        viewerManager.updateOverlay(seq);
    }
    
    public void removeOverlay() {
        viewerManager.removeOverlay(seq);
    }
    
    public void updateOverlayThreshold(int threshold, ImageTransformEnums transform, boolean ifGreater) {
        viewerManager.updateOverlayThreshold(threshold, transform, ifGreater);
    }
    
    // Sequence management methods
    
    public void closeSequence() {
        if (seq == null) {
            return;
        }
        seq.removeAllROI();
        seq.close();
    }

    public void attachSequence(Sequence sequence) {
        this.seq = sequence;
        status = EnumStatus.FILESTACK;
    }
    
    // Accessors
    
    public Sequence getSequence() {
        return seq;
    }
    
    public EnumStatus getStatus() {
        return status;
    }
    
    public void setStatus(EnumStatus newStatus) {
        this.status = newStatus;
    }
    
    public int getCurrentFrame() {
        return currentFrame;
    }
    
    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }
    
    public ImageLoader getImageLoader() {
        return imageLoader;
    }
    
    public TimeManager getTimeManager() {
        return timeManager;
    }
    
    public ROIManager getRoiManager() {
        return roiManager;
    }
    
    public ViewerManager getViewerManager() {
        return viewerManager;
    }
    
    // Time-related delegations
    
    public long getFirstImageMs() {
        return timeManager.getFirstImageMs();
    }
    
    public void setFirstImageMs(long timeMs) {
        timeManager.setFirstImageMs(timeMs);
    }
    
    public long getLastImageMs() {
        return timeManager.getLastImageMs();
    }
    
    public void setLastImageMs(long timeMs) {
        timeManager.setLastImageMs(timeMs);
    }
    
    public long getBinDurationMs() {
        return timeManager.getBinDurationMs();
    }
    
    public void setBinDurationMs(long durationMs) {
        timeManager.setBinDurationMs(durationMs);
    }
}