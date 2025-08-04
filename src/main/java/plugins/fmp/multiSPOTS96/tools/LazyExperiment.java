package plugins.fmp.multiSPOTS96.tools;

import java.util.logging.Logger;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentDirectories;

/**
 * Shared LazyExperiment implementation that can be used across different components
 * to provide memory-efficient experiment loading.
 * 
 * <p>
 * This class implements the lazy loading pattern for Experiment objects, allowing
 * components to store lightweight experiment references and only load full data
 * when needed. This dramatically reduces memory usage when handling large numbers
 * of experiments.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 1.0.0
 */
public class LazyExperiment extends Experiment {
	
	private static final Logger LOGGER = Logger.getLogger(LazyExperiment.class.getName());
	
	private final ExperimentMetadata metadata;
	private boolean isLoaded = false;

	/**
	 * Creates a new LazyExperiment with the specified metadata.
	 * 
	 * @param metadata The metadata containing experiment information
	 */
	public LazyExperiment(ExperimentMetadata metadata) {
		this.metadata = metadata;
		// Set the results directory to provide a meaningful display name
		this.setResultsDirectory(metadata.getResultsDirectory());
	}

	@Override
	public String toString() {
		return metadata.getCameraDirectory();
	}

	/**
	 * Loads the full experiment data only when this method is called. This
	 * implements the lazy loading pattern.
	 */
	public void loadIfNeeded() {
		if (!isLoaded) {
			try {
				ExperimentDirectories expDirectories = new ExperimentDirectories();
				if (expDirectories.getDirectoriesFromExptPath(metadata.getBinDirectory(),
						metadata.getCameraDirectory())) {
					Experiment fullExp = new Experiment(expDirectories);
					// Copy essential public properties from the fully loaded experiment
					this.seqCamData = fullExp.seqCamData;
					this.cagesArray = fullExp.cagesArray;
					this.firstImage_FileTime = fullExp.firstImage_FileTime;
					this.lastImage_FileTime = fullExp.lastImage_FileTime;
					this.col = fullExp.col;
					this.chainToPreviousExperiment = fullExp.chainToPreviousExperiment;
					this.chainToNextExperiment = fullExp.chainToNextExperiment;
					this.chainImageFirst_ms = fullExp.chainImageFirst_ms;
					this.experimentID = fullExp.experimentID;
					this.isLoaded = true;
				}
			} catch (Exception e) {
				LOGGER.warning("Error loading experiment " + metadata.getCameraDirectory() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Checks if the experiment has been fully loaded.
	 * 
	 * @return true if the experiment is loaded, false otherwise
	 */
	public boolean isLoaded() {
		return isLoaded;
	}

	/**
	 * Gets the metadata associated with this lazy experiment.
	 * 
	 * @return The experiment metadata
	 */
	public ExperimentMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Lightweight metadata class for experiment information. Contains only
	 * essential information needed for the dropdown and lazy loading.
	 */
	public static class ExperimentMetadata {
		private final String cameraDirectory;
		private final String resultsDirectory;
		private final String binDirectory;

		/**
		 * Creates a new ExperimentMetadata object.
		 * 
		 * @param cameraDirectory The camera directory path
		 * @param resultsDirectory The results directory path
		 * @param binDirectory The bin directory path
		 */
		public ExperimentMetadata(String cameraDirectory, String resultsDirectory, String binDirectory) {
			this.cameraDirectory = cameraDirectory;
			this.resultsDirectory = resultsDirectory;
			this.binDirectory = binDirectory;
		}

		/**
		 * Gets the camera directory path.
		 * 
		 * @return The camera directory path
		 */
		public String getCameraDirectory() {
			return cameraDirectory;
		}

		/**
		 * Gets the results directory path.
		 * 
		 * @return The results directory path
		 */
		public String getResultsDirectory() {
			return resultsDirectory;
		}

		/**
		 * Gets the bin directory path.
		 * 
		 * @return The bin directory path
		 */
		public String getBinDirectory() {
			return binDirectory;
		}

		@Override
		public String toString() {
			return cameraDirectory; // Used for dropdown display
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			ExperimentMetadata that = (ExperimentMetadata) obj;
			return cameraDirectory.equals(that.cameraDirectory) &&
				   resultsDirectory.equals(that.resultsDirectory) &&
				   binDirectory.equals(that.binDirectory);
		}

		@Override
		public int hashCode() {
			int result = cameraDirectory.hashCode();
			result = 31 * result + resultsDirectory.hashCode();
			result = 31 * result + binDirectory.hashCode();
			return result;
		}
	}
} 