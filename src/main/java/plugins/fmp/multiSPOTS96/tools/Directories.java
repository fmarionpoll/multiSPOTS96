package plugins.fmp.multiSPOTS96.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class for directory and file operations. This class provides various
 * methods for working with directories, filtering files by type, and managing
 * file system operations.
 * 
 * <p>
 * Directories is used throughout the MultiSPOTS96 plugin for managing
 * experiment data, finding files with specific extensions, and organizing
 * directory structures.
 * </p>
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * // Get all directories containing TIFF files
 * List&lt;String&gt; tiffDirs = Directories.getSortedListOfSubDirectoriesWithTIFF("/path/to/experiments");
 * 
 * // Find directories with specific files
 * HashSet&lt;String&gt; dirs = Directories.getDirectoriesWithFilesType("/path", ".txt");
 * </pre>
 * 
 * @author MultiSPOTS96
 */
public class Directories {

	/** Logger for this class */
	private static final Logger LOGGER = Logger.getLogger(Directories.class.getName());

	/** Default depth for directory traversal */
	private static final int DEFAULT_DEPTH = 1;

	/** Minimum length for directory name parsing */
	private static final int MIN_DIR_NAME_LENGTH = 4;

	/** Default TIFF file extension */
	private static final String TIFF_EXTENSION = ".tiff";

	/** Case-insensitive string comparator for sorting */
	private static final Comparator<String> CASE_INSENSITIVE_COMPARATOR = String.CASE_INSENSITIVE_ORDER;

	/**
	 * Reduces full directory paths to just the last directory name.
	 * 
	 * @param dirList the list of full directory paths
	 * @return a list of short directory names, sorted case-insensitively
	 * @throws IllegalArgumentException if dirList is null
	 */
	public static List<String> reduceFullNameToLastDirectory(List<String> dirList) {
		if (dirList == null) {
			throw new IllegalArgumentException("Directory list cannot be null");
		}

		List<String> shortList = new ArrayList<String>(dirList.size());
		for (String name : dirList) {
			if (name != null && !name.trim().isEmpty()) {
				Path pathName = Paths.get(name);
				if (pathName.getNameCount() > 0) {
					shortList.add(pathName.getName(pathName.getNameCount() - 1).toString());
				}
			}
		}
		Collections.sort(shortList, CASE_INSENSITIVE_COMPARATOR);

		LOGGER.fine("Reduced " + dirList.size() + " full paths to " + shortList.size() + " short names");
		return shortList;
	}

	/**
	 * Gets all directories that contain files with the specified extension.
	 * 
	 * @param rootDirectory the root directory to search
	 * @param filter        the file extension to filter by (e.g., ".txt")
	 * @return a set of directory paths containing files with the specified
	 *         extension
	 * @throws IllegalArgumentException if rootDirectory or filter is null
	 */
	public static HashSet<String> getDirectoriesWithFilesType(String rootDirectory, String filter) {
		if (rootDirectory == null) {
			throw new IllegalArgumentException("Root directory cannot be null");
		}
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}

		HashSet<String> hSet = new HashSet<String>();
		try {
			Path rootPath = Paths.get(rootDirectory);
			if (Files.exists(rootPath)) {
				Files.walk(rootPath).filter(Files::isRegularFile)
						.filter(p -> p.getFileName().toString().toLowerCase().endsWith(filter.toLowerCase()))
						.forEach(p -> hSet.add(p.toFile().getParent().toString()));

//				LOGGER.fine("Found " + hSet.size() + " directories with files matching '" + filter + "' in "
//						+ rootDirectory);
			} else {
				LOGGER.warning("Root directory does not exist: " + rootDirectory);
			}
		} catch (IOException e) {
			LOGGER.severe("Error accessing directory " + rootDirectory + ": " + e.getMessage());
		}
		return hSet;
	}

	/**
	 * Fetches subdirectories matching a filter string.
	 * 
	 * @param directory the parent directory to search
	 * @param filter    the filter string to match
	 * @return a sorted list of matching directory paths, or null if no matches
	 * @throws IllegalArgumentException if directory or filter is null
	 */
	public static List<String> fetchSubDirectoriesMatchingFilter(String directory, String filter) {
		if (directory == null) {
			throw new IllegalArgumentException("Directory cannot be null");
		}
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}

		List<Path> subfolders = getAllSubPathsOfDirectory(directory, DEFAULT_DEPTH);
		if (subfolders == null) {
			LOGGER.warning("No subdirectories found in " + directory);
			return null;
		}

		List<String> dirList = getPathsContainingString(subfolders, filter);
		if (dirList != null) {
			Collections.sort(dirList, CASE_INSENSITIVE_COMPARATOR);
//			LOGGER.fine("Found " + dirList.size() + " subdirectories matching '" + filter + "' in " + directory);
		}
		return dirList;
	}

	/**
	 * Gets paths containing a specific string.
	 * 
	 * @param subfolders the list of paths to search
	 * @param filter     the string to search for
	 * @return a list of paths containing the filter string, or null if subfolders
	 *         is null
	 * @throws IllegalArgumentException if filter is null
	 */
	public static List<String> getPathsContainingString(List<Path> subfolders, String filter) {
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}
		if (subfolders == null) {
			LOGGER.warning("Subfolders list is null");
			return null;
		}

		HashSet<String> dirList = new HashSet<String>();
		for (Path dirPath : subfolders) {
			if (dirPath != null) {
				String subString = dirPath.toString();
				if (subString.contains(filter)) {
					dirList.add(subString);
				}
			}
		}

		List<String> result = new ArrayList<String>(dirList);
//		LOGGER.fine("Found " + result.size() + " paths containing '" + filter + "'");
		return result;
	}

	/**
	 * Gets all subdirectories of a directory up to a specified depth.
	 * 
	 * @param directory the directory to search
	 * @param depth     the maximum depth to search
	 * @return a list of subdirectory paths, or null if directory doesn't exist
	 * @throws IllegalArgumentException if directory is null or depth is negative
	 */
	public static List<Path> getAllSubPathsOfDirectory(String directory, int depth) {
		if (directory == null) {
			throw new IllegalArgumentException("Directory cannot be null");
		}
		if (depth < 0) {
			throw new IllegalArgumentException("Depth cannot be negative: " + depth);
		}

		Path pathExperimentDir = Paths.get(directory);
		List<Path> subfolders = null;
		try {
			if (Files.exists(pathExperimentDir)) {
				subfolders = Files.walk(pathExperimentDir, depth).filter(Files::isDirectory)
						.collect(Collectors.toList());

				// Remove the root directory from the results
				if (subfolders != null && !subfolders.isEmpty()) {
					subfolders.remove(0);
				}

				LOGGER.fine(
						"Found " + (subfolders != null ? subfolders.size() : 0) + " subdirectories in " + directory);
			} else {
				LOGGER.warning("Directory does not exist: " + directory);
			}
		} catch (IOException e) {
			LOGGER.severe("Error accessing directory " + directory + ": " + e.getMessage());
		}
		return subfolders;
	}

	/**
	 * Gets the directory path from a file name.
	 * 
	 * @param fileName the file name or path
	 * @return the directory path
	 * @throws IllegalArgumentException if fileName is null
	 */
	public static String getDirectoryFromName(String fileName) {
		if (fileName == null) {
			throw new IllegalArgumentException("File name cannot be null");
		}

		File filepath = new File(fileName);
		String strDirectory = filepath.isDirectory() ? filepath.getAbsolutePath()
				: filepath.getParentFile().getAbsolutePath();

//		LOGGER.fine("Extracted directory from '" + fileName + "': " + strDirectory);
		return strDirectory;
	}

	/**
	 * Gets a sorted list of subdirectories containing TIFF files.
	 * 
	 * @param parentDirectory the parent directory to search
	 * @return a sorted list of directory names containing TIFF files
	 * @throws IllegalArgumentException if parentDirectory is null
	 */
	public static List<String> getSortedListOfSubDirectoriesWithTIFF(String parentDirectory) {
		if (parentDirectory == null) {
			throw new IllegalArgumentException("Parent directory cannot be null");
		}

		HashSet<String> hSet = getDirectoriesWithFilesType(parentDirectory, TIFF_EXTENSION);
		List<String> list = reduceFullNameToLastDirectory(new ArrayList<String>(hSet));

		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				if (s1 == null && s2 == null) {
					return 0;
				}
				if (s1 == null) {
					return 1;
				}
				if (s2 == null) {
					return -1;
				}

				if (s1.equalsIgnoreCase(s2)) {
					return 0;
				}

				// Extract numeric part after first 4 characters
				if (s1.length() < MIN_DIR_NAME_LENGTH || s2.length() < MIN_DIR_NAME_LENGTH) {
					return s1.compareToIgnoreCase(s2);
				}

				String tokens1 = s1.substring(MIN_DIR_NAME_LENGTH);
				String tokens2 = s2.substring(MIN_DIR_NAME_LENGTH);

				if (!isInteger(tokens1) || !isInteger(tokens2)) {
					return tokens1.compareToIgnoreCase(tokens2);
				}

				int number1 = Integer.parseInt(tokens1);
				int number2 = Integer.parseInt(tokens2);

				if (number1 != number2) {
					return number1 - number2;
				}

				return tokens1.compareToIgnoreCase(tokens2);
			}
		});

//		LOGGER.fine("Found " + list.size() + " TIFF directories in " + parentDirectory);
		return list;
	}

	/**
	 * Checks if a string represents a valid integer.
	 * 
	 * @param str the string to check
	 * @return true if the string is a valid integer, false otherwise
	 */
	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Deletes all files with a specific extension in a directory.
	 * 
	 * @param directory the directory to search
	 * @param filter    the file extension to match (e.g., ".tmp")
	 * @throws IllegalArgumentException if directory or filter is null
	 */
	public static void deleteFilesWithExtension(String directory, String filter) {
		if (directory == null) {
			throw new IllegalArgumentException("Directory cannot be null");
		}
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}

		File folder = new File(directory);
		if (!folder.exists() || !folder.isDirectory()) {
			LOGGER.warning("Directory does not exist or is not a directory: " + directory);
			return;
		}

		File[] files = folder.listFiles();
		if (files == null) {
			LOGGER.warning("Cannot list files in directory: " + directory);
			return;
		}

		int deletedCount = 0;
		for (File file : files) {
			if (file != null && file.isFile()) {
				String name = file.getName();
				if (name.toLowerCase().endsWith(filter.toLowerCase())) {
					if (file.delete()) {
						deletedCount++;
//						LOGGER.fine("Deleted file: " + file.getAbsolutePath());
					} else {
						LOGGER.warning("Failed to delete file: " + file.getAbsolutePath());
					}
				}
			}
		}

//        LOGGER.info("Deleted " + deletedCount + " files with extension '" + filter + "' in " + directory);
	}
}
