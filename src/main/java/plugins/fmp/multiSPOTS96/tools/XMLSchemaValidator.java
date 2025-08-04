package plugins.fmp.multiSPOTS96.tools;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * XML Schema validation utility for multiSPOTS96 XML files.
 * 
 * Provides schema validation capabilities for the XML persistence layer.
 * 
 * @author MultiSPOTS96
 * @version 1.0.0
 */
public class XMLSchemaValidator {

	// Schema file paths - using classpath-based resource loading
	private static final String EXPERIMENT_SCHEMA_PATH = "plugins/fmp/multiSPOTS96/schemas/MS96_experiment.xsd";
	private static final String CAGES_SCHEMA_PATH = "plugins/fmp/multiSPOTS96/schemas/MCdrosotrack.xsd";

	// Schema validation flags
	private static boolean enableSchemaValidation = true;
	private static boolean enableStrictValidation = false;

	/**
	 * Validates an XML document against its schema.
	 * 
	 * @param doc        the XML document to validate
	 * @param schemaType the type of schema to validate against
	 * @return true if validation passes, false otherwise
	 */
	public static boolean validateXMLDocument(Document doc, SchemaType schemaType) {
		if (!enableSchemaValidation) {
			// System.out.println("Schema validation disabled");
			return true;
		}

		if (doc == null) {
			System.err.println("ERROR: Null document provided for validation");
			return false;
		}

		// Skip cages validation due to complex dynamic structure
		if (schemaType == SchemaType.CAGES) {
			// System.out.println("Skipping cages schema validation (complex dynamic
			// structure)");
			return true;
		}

		try {
			Schema schema = getSchema(schemaType);
			if (schema == null) {
				System.err.println("WARNING: Schema not found for " + schemaType + ", skipping validation");
				return true; // Skip validation if schema not available
			}

			Validator validator = schema.newValidator();

			// Convert Document to String for validation
			String xmlString = documentToString(doc);
			if (xmlString == null) {
				System.err.println("ERROR: Could not convert Document to String for validation");
				return false;
			}
			Source source = new StreamSource(new StringReader(xmlString));

			// Set validation error handler
			ValidationErrorHandler errorHandler = new ValidationErrorHandler();
			validator.setErrorHandler(errorHandler);

			// Perform validation
			validator.validate(source);

			if (errorHandler.hasErrors()) {
				System.err.println("XML Schema validation failed:");
				errorHandler.printErrors();
				return false;
			}

			// System.out.println("XML Schema validation passed for " + schemaType);
			return true;

		} catch (SAXException e) {
			System.err.println("ERROR during XML schema validation: " + e.getMessage());
			return false;
		} catch (IOException e) {
			System.err.println("ERROR reading XML for validation: " + e.getMessage());
			return false;
		} catch (Exception e) {
			System.err.println("ERROR during XML validation: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Gets the appropriate schema for the given schema type.
	 * 
	 * @param schemaType the type of schema to retrieve
	 * @return the Schema object, or null if not found
	 */
	private static Schema getSchema(SchemaType schemaType) {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

			String schemaPath = null;
			switch (schemaType) {
			case EXPERIMENT:
				schemaPath = EXPERIMENT_SCHEMA_PATH;
				break;
			case CAGES:
				schemaPath = CAGES_SCHEMA_PATH;
				break;
			default:
				System.err.println("ERROR: Unknown schema type: " + schemaType);
				return null;
			}

			// Try classpath-based loading first (preferred method)
			java.io.InputStream schemaStream = XMLSchemaValidator.class.getClassLoader()
					.getResourceAsStream(schemaPath);
			if (schemaStream != null) {
//				System.err.println("DEBUG: Schema loaded from classpath: " + schemaPath);
				return factory.newSchema(new StreamSource(schemaStream));
			}

			// Fallback to file system (for development/testing)
			File schemaFile = new File(schemaPath);
			if (schemaFile.exists()) {
//				System.err.println("DEBUG: Schema loaded from file system: " + schemaFile.getAbsolutePath());
				return factory.newSchema(schemaFile);
			}

			// Try relative to current working directory as last resort
			String currentDir = System.getProperty("user.dir");
			schemaFile = new File(currentDir, schemaPath);
			if (schemaFile.exists()) {
//				System.err.println("DEBUG: Schema loaded from working directory: " + schemaFile.getAbsolutePath());
				return factory.newSchema(schemaFile);
			}

			// Schema not found
			System.err.println("WARNING: Schema file not found: " + schemaPath);
			System.err.println("Current directory: " + currentDir);
			System.err.println("Tried classpath and file locations");
			return null;

		} catch (SAXException e) {
			System.err.println("ERROR loading schema: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Enables or disables schema validation.
	 * 
	 * @param enabled true to enable validation, false to disable
	 */
	public static void setSchemaValidationEnabled(boolean enabled) {
		enableSchemaValidation = enabled;
		// System.out.println("Schema validation " + (enabled ? "enabled" :
		// "disabled"));
	}

	/**
	 * Enables or disables strict validation mode.
	 * 
	 * @param strict true for strict validation, false for lenient
	 */
	public static void setStrictValidation(boolean strict) {
		enableStrictValidation = strict;
		// System.out.println("Strict validation " + (strict ? "enabled" : "disabled"));
	}

	/**
	 * Converts a Document to a String representation.
	 * 
	 * @param doc the Document to convert
	 * @return the XML as a String, or null if conversion fails
	 */
	private static String documentToString(Document doc) {
		try {
			javax.xml.transform.TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();
			javax.xml.transform.Transformer transformer = factory.newTransformer();
			javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
			java.io.StringWriter writer = new java.io.StringWriter();
			javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(writer);
			transformer.transform(source, result);
			return writer.toString();
		} catch (Exception e) {
			System.err.println("ERROR converting Document to String: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Schema types for different XML structures.
	 */
	public enum SchemaType {
		EXPERIMENT, CAGES
	}

	/**
	 * Custom error handler for XML validation.
	 */
	private static class ValidationErrorHandler extends org.xml.sax.helpers.DefaultHandler {
		private boolean hasErrors = false;
		private StringBuilder errorMessages = new StringBuilder();

		@Override
		public void error(org.xml.sax.SAXParseException e) throws SAXException {
			hasErrors = true;
			errorMessages.append("ERROR: ").append(e.getMessage()).append(" at line ").append(e.getLineNumber())
					.append("\n");
		}

		@Override
		public void fatalError(org.xml.sax.SAXParseException e) throws SAXException {
			hasErrors = true;
			errorMessages.append("FATAL ERROR: ").append(e.getMessage()).append(" at line ").append(e.getLineNumber())
					.append("\n");
		}

		@Override
		public void warning(org.xml.sax.SAXParseException e) throws SAXException {
			if (enableStrictValidation) {
				hasErrors = true;
				errorMessages.append("WARNING: ").append(e.getMessage()).append(" at line ").append(e.getLineNumber())
						.append("\n");
			} else {
				// System.out.println("XML Validation Warning: " + e.getMessage() + " at line "
				// + e.getLineNumber());
			}
		}

		public boolean hasErrors() {
			return hasErrors;
		}

		public void printErrors() {
			System.err.println(errorMessages.toString());
		}
	}
}