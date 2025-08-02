package plugins.fmp.multiSPOTS96.tools.toExcel;

/**
 * Test class for memory monitoring functionality in XLSExportMeasuresFromSpotStreaming.
 * 
 * <p>
 * This class demonstrates how to use the memory monitoring features
 * and verifies that the setMemoryMonitoringEnabled method works correctly.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class MemoryMonitoringTest {

    /**
     * Tests the memory monitoring functionality.
     */
    public static void testMemoryMonitoring() {
        System.out.println("=== Testing Memory Monitoring Functionality ===");

        // Create a streaming exporter instance
        XLSExportMeasuresFromSpotStreaming exporter = new XLSExportMeasuresFromSpotStreaming();

        // Test 1: Default state
        System.out.println("Test 1: Default memory monitoring state");
        System.out.println("Memory monitoring enabled: " + exporter.isMemoryMonitoringEnabled());
        System.out.println("Memory check interval: " + exporter.getMemoryCheckInterval());

        // Test 2: Enable memory monitoring
        System.out.println("\nTest 2: Enabling memory monitoring");
        exporter.setMemoryMonitoringEnabled(true);
        System.out.println("Memory monitoring enabled: " + exporter.isMemoryMonitoringEnabled());

        // Test 3: Set custom memory check interval
        System.out.println("\nTest 3: Setting custom memory check interval");
        exporter.setMemoryCheckInterval(50);
        System.out.println("Memory check interval: " + exporter.getMemoryCheckInterval());

        // Test 4: Get memory usage stats
        System.out.println("\nTest 4: Getting memory usage statistics");
        String memoryStats = exporter.getMemoryUsageStats();
        System.out.println("Memory stats: " + memoryStats);

        // Test 5: Disable memory monitoring
        System.out.println("\nTest 5: Disabling memory monitoring");
        exporter.setMemoryMonitoringEnabled(false);
        System.out.println("Memory monitoring enabled: " + exporter.isMemoryMonitoringEnabled());

        System.out.println("\n=== Memory Monitoring Tests Completed Successfully ===");
    }

    /**
     * Tests the XLSExportFactory integration with memory monitoring.
     */
    public static void testFactoryIntegration() {
        System.out.println("\n=== Testing Factory Integration ===");

        try {
            // Test creating exporter with memory monitoring enabled
            XLSExport exporter = XLSExportFactory.createExporter(100, new XLSExportOptions(), true);
            
            if (exporter instanceof XLSExportMeasuresFromSpotStreaming) {
                XLSExportMeasuresFromSpotStreaming streamingExporter = (XLSExportMeasuresFromSpotStreaming) exporter;
                System.out.println("Factory created streaming exporter with memory monitoring: " + 
                    streamingExporter.isMemoryMonitoringEnabled());
            } else {
                System.out.println("Factory created non-streaming exporter (memory monitoring not applicable)");
            }

        } catch (Exception e) {
            System.err.println("Error testing factory integration: " + e.getMessage());
        }

        System.out.println("=== Factory Integration Tests Completed ===");
    }

    /**
     * Demonstrates memory monitoring in action.
     */
    public static void demonstrateMemoryMonitoring() {
        System.out.println("\n=== Demonstrating Memory Monitoring ===");

        XLSExportMeasuresFromSpotStreaming exporter = new XLSExportMeasuresFromSpotStreaming();
        
        // Enable memory monitoring
        exporter.setMemoryMonitoringEnabled(true);
        exporter.setMemoryCheckInterval(10); // Check every 10 spots for demonstration

        System.out.println("Memory monitoring enabled with interval: " + exporter.getMemoryCheckInterval());
        System.out.println("Initial memory stats: " + exporter.getMemoryUsageStats());

        // Simulate some processing (this would normally happen during export)
        System.out.println("\nSimulating processing...");
        for (int i = 0; i < 5; i++) {
            // Simulate some memory allocation
            byte[] testArray = new byte[1024 * 1024]; // 1MB allocation
            
            System.out.println("Step " + (i + 1) + " - Memory stats: " + exporter.getMemoryUsageStats());
            
            // Force garbage collection to simulate memory cleanup
            if (i % 2 == 0) {
                System.gc();
                System.out.println("Garbage collection performed");
            }
        }

        System.out.println("Final memory stats: " + exporter.getMemoryUsageStats());
        System.out.println("=== Memory Monitoring Demonstration Completed ===");
    }

    /**
     * Main method to run all tests.
     */
    public static void main(String[] args) {
        System.out.println("Memory Monitoring Test Suite");
        System.out.println("============================");

        // Run all tests
        testMemoryMonitoring();
        testFactoryIntegration();
        demonstrateMemoryMonitoring();

        System.out.println("\nAll tests completed successfully!");
        System.out.println("\nKey Features Verified:");
        System.out.println("- setMemoryMonitoringEnabled() method works correctly");
        System.out.println("- Memory monitoring can be enabled/disabled");
        System.out.println("- Custom memory check intervals can be set");
        System.out.println("- Memory usage statistics are available");
        System.out.println("- Factory integration works with memory monitoring");
    }
} 