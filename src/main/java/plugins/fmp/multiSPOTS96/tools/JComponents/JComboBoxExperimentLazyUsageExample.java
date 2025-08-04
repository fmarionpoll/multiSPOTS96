package plugins.fmp.multiSPOTS96.tools.JComponents;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import plugins.fmp.multiSPOTS96.experiment.Experiment;
import plugins.fmp.multiSPOTS96.experiment.ExperimentDirectories;

/**
 * Example demonstrating how to use JComboBoxExperimentLazy for memory-efficient
 * experiment management.
 * 
 * <p>
 * This example shows:
 * <ul>
 * <li>How to create and populate a JComboBoxExperimentLazy</li>
 * <li>How lazy loading works automatically</li>
 * <li>Memory usage monitoring</li>
 * <li>Performance comparison with regular JComboBoxExperiment</li>
 * </ul>
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 1.0.0
 */
public class JComboBoxExperimentLazyUsageExample {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JComboBoxExperimentLazyUsageExample example = new JComboBoxExperimentLazyUsageExample();
			example.runExample();
		});
	}

	public void runExample() {
		// Create the lazy combo box
		JComboBoxExperimentLazy lazyCombo = new JComboBoxExperimentLazy();
		
		// Set the bin directory (required for experiment loading)
		lazyCombo.stringExpBinSubDirectory = "/path/to/your/bin/directory";
		
		// Example 1: Add experiments from a list of experiment names
		List<String> experimentNames = getExampleExperimentNames();
		addExperimentsFromNames(lazyCombo, experimentNames);
		
		// Example 2: Monitor memory usage
		System.out.println("Initial memory usage: " + lazyCombo.getMemoryUsageInfo());
		System.out.println("Loaded experiments: " + lazyCombo.getLoadedExperimentCount() + " / " + lazyCombo.getItemCount());
		
		// Example 3: Access experiments (triggers lazy loading)
		if (lazyCombo.getItemCount() > 0) {
			Experiment firstExp = lazyCombo.getItemAt(0);
			System.out.println("First experiment loaded: " + firstExp.toString());
			System.out.println("Memory after loading first experiment: " + lazyCombo.getMemoryUsageInfo());
		}
		
		// Example 4: Load all experiments (for processing)
		System.out.println("Loading all experiments for processing...");
		boolean success = lazyCombo.loadListOfMeasuresFromAllExperiments(true, false);
		System.out.println("All experiments loaded: " + success);
		System.out.println("Final memory usage: " + lazyCombo.getMemoryUsageInfo());
		
		// Example 5: Create a simple UI to demonstrate the combo box
		createDemoUI(lazyCombo);
	}

	/**
	 * Adds experiments to the combo box from a list of experiment names.
	 * This demonstrates the lazy loading approach.
	 */
	private void addExperimentsFromNames(JComboBoxExperimentLazy combo, List<String> experimentNames) {
		System.out.println("Adding " + experimentNames.size() + " experiments to combo box...");
		
		for (String expName : experimentNames) {
			// Create a minimal experiment object (will be converted to LazyExperiment)
			Experiment exp = new Experiment();
			exp.setResultsDirectory("/path/to/experiments/" + expName);
			
			// Add to combo box (automatically converted to LazyExperiment)
			int index = combo.addExperiment(exp, false);
			System.out.println("Added experiment " + expName + " at index " + index);
		}
		
		System.out.println("Total experiments in combo box: " + combo.getItemCount());
	}

	/**
	 * Creates a simple demo UI to show the combo box in action.
	 */
	private void createDemoUI(JComboBoxExperimentLazy combo) {
		JFrame frame = new JFrame("JComboBoxExperimentLazy Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 400);
		
		JPanel panel = new JPanel();
		panel.add(combo);
		
		// Add a button to show memory usage
		javax.swing.JButton memoryButton = new javax.swing.JButton("Show Memory Usage");
		memoryButton.addActionListener(e -> {
			System.out.println("Current memory usage: " + combo.getMemoryUsageInfo());
			System.out.println("Loaded experiments: " + combo.getLoadedExperimentCount() + " / " + combo.getItemCount());
		});
		panel.add(memoryButton);
		
		// Add a button to load selected experiment
		javax.swing.JButton loadButton = new javax.swing.JButton("Load Selected");
		loadButton.addActionListener(e -> {
			Experiment selected = combo.getSelectedItem();
			if (selected != null) {
				System.out.println("Selected experiment: " + selected.toString());
				System.out.println("Memory after selection: " + combo.getMemoryUsageInfo());
			}
		});
		panel.add(loadButton);
		
		frame.add(panel);
		frame.setVisible(true);
	}

	/**
	 * Returns a list of example experiment names for demonstration.
	 */
	private List<String> getExampleExperimentNames() {
		List<String> names = new ArrayList<>();
		names.add("experiment_001");
		names.add("experiment_002");
		names.add("experiment_003");
		names.add("experiment_004");
		names.add("experiment_005");
		return names;
	}

	/**
	 * Demonstrates the memory efficiency difference between regular and lazy combo boxes.
	 */
	public void compareMemoryUsage() {
		System.out.println("=== Memory Usage Comparison ===");
		
		// Test with regular JComboBoxExperiment
		JComboBoxExperiment regularCombo = new JComboBoxExperiment();
		regularCombo.stringExpBinSubDirectory = "/path/to/bin";
		
		// Test with lazy JComboBoxExperimentLazy
		JComboBoxExperimentLazy lazyCombo = new JComboBoxExperimentLazy();
		lazyCombo.stringExpBinSubDirectory = "/path/to/bin";
		
		List<String> experimentNames = getExampleExperimentNames();
		
		// Add experiments to both combo boxes
		for (String expName : experimentNames) {
			Experiment exp = new Experiment();
			exp.setResultsDirectory("/path/to/experiments/" + expName);
			
			regularCombo.addExperiment(exp, false);
			lazyCombo.addExperiment(exp, false);
		}
		
		// Compare memory usage
		System.out.println("Regular combo box experiments: " + regularCombo.getItemCount());
		System.out.println("Lazy combo box experiments: " + lazyCombo.getItemCount());
		System.out.println("Lazy combo box memory info: " + lazyCombo.getMemoryUsageInfo());
		System.out.println("Lazy combo box loaded experiments: " + lazyCombo.getLoadedExperimentCount());
		
		// Load one experiment in lazy combo box
		if (lazyCombo.getItemCount() > 0) {
			lazyCombo.getItemAt(0); // This triggers lazy loading
			System.out.println("After loading one experiment: " + lazyCombo.getMemoryUsageInfo());
			System.out.println("Loaded experiments: " + lazyCombo.getLoadedExperimentCount());
		}
	}
} 