package plugins.fmp.multiSPOTS96.tools.toExcel;

import plugins.fmp.multiSPOTS96.tools.toExcel.exceptions.ExcelExportException;

/**
 * Example usage of the optimized Excel export implementations.
 * 
 * <p>
 * This class demonstrates how to use the different export implementations
 * and the factory for automatic selection.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class ExportUsageExample {

    /**
     * Example 1: Automatic selection using factory
     */
    public static void exampleAutomaticSelection() {
        try {
            // Get experiment count from your data
            int experimentCount = getExperimentCount();
            XLSExportOptions options = createExportOptions();
            
            // Factory automatically selects the best implementation
            XLSExport exporter = XLSExportFactory.createExporter(experimentCount, options);
            
            System.out.println("Using: " + exporter.getClass().getSimpleName());
            System.out.println(XLSExportFactory.getMemoryAnalysis(experimentCount));
            
            // Perform export
            exporter.exportToFile("output_automatic.xlsx", options);
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 2: Manual selection for specific use cases
     */
    public static void exampleManualSelection() {
        try {
            XLSExportOptions options = createExportOptions();
            
            // For small datasets with no memory constraints
            XLSExport originalExporter = XLSExportFactory.createOriginalExporter();
            originalExporter.exportToFile("output_original.xlsx", options);
            
            // For medium datasets with moderate memory constraints
            XLSExport optimizedExporter = XLSExportFactory.createOptimizedExporter();
            optimizedExporter.exportToFile("output_optimized.xlsx", options);
            
            // For large datasets with severe memory constraints
            XLSExport streamingExporter = XLSExportFactory.createStreamingExporter();
            streamingExporter.exportToFile("output_streaming.xlsx", options);
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 3: Memory-constrained environment
     */
    public static void exampleMemoryConstrained() {
        try {
            int experimentCount = getExperimentCount();
            XLSExportOptions options = createExportOptions();
            
            // Check if system can handle the export
            if (!XLSExportFactory.canHandleExport(experimentCount)) {
                System.err.println("Warning: Insufficient memory for export");
                System.err.println("Required: " + XLSExportFactory.getMinimumRequiredMemory(experimentCount) + " MB");
                System.err.println("Available: " + XLSExportFactory.getAvailableMemoryMB() + " MB");
                return;
            }
            
            // Use streaming implementation for memory-constrained environments
            XLSExport exporter = XLSExportFactory.createStreamingExporter();
            
            // Enable memory monitoring
            if (exporter instanceof XLSExportMeasuresFromSpotStreaming) {
                ((XLSExportMeasuresFromSpotStreaming) exporter).setMemoryMonitoringEnabled(true);
            }
            
            exporter.exportToFile("output_memory_constrained.xlsx", options);
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 4: Performance comparison
     */
    public static void examplePerformanceComparison() {
        try {
            int experimentCount = getExperimentCount();
            XLSExportOptions options = createExportOptions();
            
            System.out.println("Performance Comparison for " + experimentCount + " experiments:");
            System.out.println("==================================================");
            
            // Test original implementation
            long startTime = System.currentTimeMillis();
            long startMemory = getCurrentMemoryUsage();
            
            XLSExport originalExporter = XLSExportFactory.createOriginalExporter();
            originalExporter.exportToFile("output_perf_original.xlsx", options);
            
            long originalTime = System.currentTimeMillis() - startTime;
            long originalMemory = getCurrentMemoryUsage() - startMemory;
            
            // Test optimized implementation
            startTime = System.currentTimeMillis();
            startMemory = getCurrentMemoryUsage();
            
            XLSExport optimizedExporter = XLSExportFactory.createOptimizedExporter();
            optimizedExporter.exportToFile("output_perf_optimized.xlsx", options);
            
            long optimizedTime = System.currentTimeMillis() - startTime;
            long optimizedMemory = getCurrentMemoryUsage() - startMemory;
            
            // Test streaming implementation
            startTime = System.currentTimeMillis();
            startMemory = getCurrentMemoryUsage();
            
            XLSExport streamingExporter = XLSExportFactory.createStreamingExporter();
            streamingExporter.exportToFile("output_perf_streaming.xlsx", options);
            
            long streamingTime = System.currentTimeMillis() - startTime;
            long streamingMemory = getCurrentMemoryUsage() - startMemory;
            
            // Print results
            System.out.println("Original:    " + originalTime + " ms, " + originalMemory + " MB");
            System.out.println("Optimized:   " + optimizedTime + " ms, " + optimizedMemory + " MB");
            System.out.println("Streaming:   " + streamingTime + " ms, " + streamingMemory + " MB");
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 5: Custom thresholds
     */
    public static void exampleCustomThresholds() {
        try {
            int experimentCount = getExperimentCount();
            XLSExportOptions options = createExportOptions();
            
            // Use custom thresholds for specific requirements
            XLSExport exporter = XLSExportFactory.createExporterWithCustomThresholds(
                experimentCount, 
                options,
                5,    // Small dataset threshold
                50,   // Medium dataset threshold
                0.5   // Memory usage threshold (50%)
            );
            
            System.out.println("Using custom thresholds: " + exporter.getClass().getSimpleName());
            exporter.exportToFile("output_custom.xlsx", options);
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 6: Memory monitoring during export
     */
    public static void exampleMemoryMonitoring() {
        try {
            int experimentCount = getExperimentCount();
            XLSExportOptions options = createExportOptions();
            
            // Print initial memory state
            System.out.println("Initial memory usage: " + getCurrentMemoryUsage() + " MB");
            System.out.println("Available memory: " + XLSExportFactory.getAvailableMemoryMB() + " MB");
            
            // Create exporter with memory monitoring
            XLSExport exporter = XLSExportFactory.createExporter(experimentCount, options, true);
            
            // Monitor memory during export
            long startMemory = getCurrentMemoryUsage();
            long startTime = System.currentTimeMillis();
            
            exporter.exportToFile("output_monitored.xlsx", options);
            
            long endTime = System.currentTimeMillis();
            long endMemory = getCurrentMemoryUsage();
            
            System.out.println("Export completed:");
            System.out.println("- Time: " + (endTime - startTime) + " ms");
            System.out.println("- Memory used: " + (endMemory - startMemory) + " MB");
            System.out.println("- Final memory: " + endMemory + " MB");
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 7: Handling memory pressure
     */
    public static void exampleMemoryPressure() {
        try {
            int experimentCount = getExperimentCount();
            XLSExportOptions options = createExportOptions();
            
            // Check for memory pressure
            if (XLSExportFactory.isUnderMemoryPressure()) {
                System.out.println("Warning: System under memory pressure");
                System.out.println("Memory usage: " + (XLSExportFactory.getMemoryUsagePercentage() * 100) + "%");
                
                // Force garbage collection
                System.gc();
                
                if (XLSExportFactory.isUnderMemoryPressure()) {
                    System.err.println("Error: Insufficient memory for export");
                    return;
                }
            }
            
            // Use streaming implementation for memory pressure situations
            XLSExport exporter = XLSExportFactory.createStreamingExporter();
            exporter.exportToFile("output_pressure.xlsx", options);
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    /**
     * Example 8: Batch processing with different implementations
     */
    public static void exampleBatchProcessing() {
        try {
            XLSExportOptions options = createExportOptions();
            
            // Process different experiment sets with appropriate implementations
            int[] experimentCounts = {5, 25, 100, 500};
            
            for (int count : experimentCounts) {
                System.out.println("Processing " + count + " experiments...");
                
                XLSExport exporter = XLSExportFactory.createExporter(count, options);
                String filename = "output_batch_" + count + ".xlsx";
                
                exporter.exportToFile(filename, options);
                
                System.out.println("Completed: " + filename);
            }
            
        } catch (ExcelExportException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    // Helper methods

    /**
     * Gets the current experiment count (implement based on your data source).
     */
    private static int getExperimentCount() {
        // This should be implemented based on your actual data
        return 50; // Example value
    }

    /**
     * Creates export options (implement based on your requirements).
     */
    private static XLSExportOptions createExportOptions() {
        XLSExportOptions options = new XLSExportOptions();
        // Configure options based on your requirements
        options.spotAreas = true;
        options.onlyalive = false;
        options.relativeToT0 = false;
        options.buildExcelStepMs = 1000;
        return options;
    }

    /**
     * Gets current memory usage in MB.
     */
    private static long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }

    /**
     * Main method demonstrating all examples.
     */
    public static void main(String[] args) {
        System.out.println("MultiSPOTS96 Excel Export Examples");
        System.out.println("==================================");
        
        // Run examples
        exampleAutomaticSelection();
        exampleManualSelection();
        exampleMemoryConstrained();
        examplePerformanceComparison();
        exampleCustomThresholds();
        exampleMemoryMonitoring();
        exampleMemoryPressure();
        exampleBatchProcessing();
        
        System.out.println("All examples completed.");
    }
} 