package plugins.fmp.multiSPOTS96.tools.ROI2D;

/**
 * Exception thrown when processing operations fail.
 */
public class ROI2DProcessingException extends ROI2DException {

	private static final long serialVersionUID = 1L;

	private final String processingStep;

	public ROI2DProcessingException(String processingStep, String message) {
		super(String.format("Processing step '%s' failed: %s", processingStep, message));
		this.processingStep = processingStep;
	}

	public ROI2DProcessingException(String processingStep, String message, Throwable cause) {
		super(String.format("Processing step '%s' failed: %s", processingStep, message), cause);
		this.processingStep = processingStep;
	}

	public String getProcessingStep() {
		return processingStep;
	}
}
