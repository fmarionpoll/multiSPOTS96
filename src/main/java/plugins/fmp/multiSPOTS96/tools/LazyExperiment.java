package plugins.fmp.multiSPOTS96.tools;

import java.io.File;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentDirectories;
import plugins.fmp.multiSPOTS96.experiment.ExperimentProperties;
import plugins.fmp.multiSPOTS96.tools.toExcel.EnumXLSColumnHeader;

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
 * <p>
 * <strong>Performance Optimization:</strong> This class now caches experiment
 * properties to avoid repeated XML file reads when retrieving field values for
 * combo boxes.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.0.0
 */
public class LazyExperiment extends Experiment {
	
	private static final Logger LOGGER = Logger.getLogger(LazyExperiment.class.getName());
	
	private final ExperimentMetadata metadata;
	private boolean isLoaded = false;
	private boolean propertiesLoaded = false;
	private ExperimentProperties cachedProperties = null;
	
	// XML file constants for properties loading
	private final static String ID_MCEXPERIMENT = "MCexperiment";
	private final static String ID_MS96_experiment_XML = "MS96_experiment.xml";

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
	 * Loads only the experiment properties from XML file for efficient field value
	 * retrieval. This method avoids loading the full experiment data.
	 * 
	 * @return true if properties were loaded successfully, false otherwise
	 */
	public boolean loadPropertiesIfNeeded() {
		if (!propertiesLoaded) {
			try {
				String resultsDir = metadata.getResultsDirectory();
				if (resultsDir == null) {
					resultsDir = metadata.getCameraDirectory() + File.separator + "results";
				}
				
				String xmlFileName = resultsDir + File.separator + ID_MS96_experiment_XML;
				File xmlFile = new File(xmlFileName);
				
				if (!xmlFile.exists()) {
					LOGGER.warning("XML file not found: " + xmlFileName);
					return false;
				}
				
				Document doc = XMLUtil.loadDocument(xmlFileName);
				if (doc == null) {
					LOGGER.warning("Could not load XML document from " + xmlFileName);
					return false;
				}
				
				Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
				if (node == null) {
					LOGGER.warning("Could not find MCexperiment node in XML");
					return false;
				}
				
				cachedProperties = new ExperimentProperties();
				cachedProperties.loadXML_Properties(node);
				propertiesLoaded = true;
				
				return true;
			} catch (Exception e) {
				LOGGER.warning("Error loading properties for experiment " + metadata.getCameraDirectory() + ": " + e.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets a field value from the cached properties without loading the full experiment.
	 * This is much more efficient than loading the entire experiment just for field values.
	 * 
	 * @param field The field to retrieve
	 * @return The field value, or ".." if not available
	 */
	public String getFieldValue(EnumXLSColumnHeader field) {
		if (loadPropertiesIfNeeded() && cachedProperties != null) {
			return cachedProperties.getExperimentField(field);
		}
		return "..";
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
	 * Checks if the experiment properties have been loaded.
	 * 
	 * @return true if the properties are loaded, false otherwise
	 */
	public boolean isPropertiesLoaded() {
		return propertiesLoaded;
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
	 * Gets the cached properties. This method will load properties if needed.
	 * 
	 * @return The cached experiment properties, or null if loading failed
	 */
	public ExperimentProperties getCachedProperties() {
		loadPropertiesIfNeeded();
		return cachedProperties;
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