package plugins.fmp.multiSPOTS96;

import plugins.fmp.multiSPOTS96.dlg.a_browse.LoadSaveExperimentOptimizedTest;
import plugins.fmp.multiSPOTS96.series.BuildSpotsMeasuresMemoryTest;
import plugins.fmp.multiSPOTS96.tools.toExcel.MemoryMonitoringTest;

/**
 * Comprehensive test suite for MultiSPOTS96 plugin.
 * 
 * <p>
 * This test suite organizes and runs all tests for the MultiSPOTS96 plugin,
 * including file loading tests, memory optimization tests, and Excel export tests.
 * </p>
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class MultiSPOTS96TestSuite {
    
    /**
     * Runs all tests in the suite.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("MultiSPOTS96 Test Suite");
        System.out.println("========================");
        System.out.println();
        
        int totalTests = 0;
        int passedTests = 0;
        
        // Test 1: File Loading Tests
        System.out.println("1. Testing File Loading Optimizations...");
        totalTests++;
        if (runFileLoadingTests()) {
            passedTests++;
            System.out.println("   ✓ File loading tests passed");
        } else {
            System.out.println("   ✗ File loading tests failed");
        }
        System.out.println();
        
        // Test 2: Memory Optimization Tests
        System.out.println("2. Testing Memory Optimizations...");
        totalTests++;
        if (runMemoryOptimizationTests()) {
            passedTests++;
            System.out.println("   ✓ Memory optimization tests passed");
        } else {
            System.out.println("   ✗ Memory optimization tests failed");
        }
        System.out.println();
        
        // Test 3: Excel Export Tests
        System.out.println("3. Testing Excel Export Functionality...");
        totalTests++;
        if (runExcelExportTests()) {
            passedTests++;
            System.out.println("   ✓ Excel export tests passed");
        } else {
            System.out.println("   ✗ Excel export tests failed");
        }
        System.out.println();
        
        // Summary
        System.out.println("Test Summary");
        System.out.println("============");
        System.out.println("Total tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success rate: " + (passedTests * 100 / totalTests) + "%");
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 All tests passed! The plugin is ready for use.");
        } else {
            System.out.println("\n⚠️  Some tests failed. Please review the output above.");
        }
    }
    
    /**
     * Runs file loading optimization tests.
     * 
     * @return true if all tests pass, false otherwise
     */
    private static boolean runFileLoadingTests() {
        try {
            System.out.println("   - Testing with 5 files...");
            boolean test5 = LoadSaveExperimentOptimizedTest.testFileLoading(5);
            
            System.out.println("   - Testing with 10 files...");
            boolean test10 = LoadSaveExperimentOptimizedTest.testFileLoading(10);
            
            System.out.println("   - Testing with 15 files...");
            boolean test15 = LoadSaveExperimentOptimizedTest.testFileLoading(15);
            
            System.out.println("   - Testing with 20 files...");
            boolean test20 = LoadSaveExperimentOptimizedTest.testFileLoading(20);
            
            return test5 && test10 && test15 && test20;
            
        } catch (Exception e) {
            System.err.println("   Error in file loading tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Runs memory optimization tests.
     * 
     * @return true if all tests pass, false otherwise
     */
    private static boolean runMemoryOptimizationTests() {
        try {
            System.out.println("   - Testing memory monitoring...");
            MemoryMonitoringTest.testMemoryMonitoring();
            
            System.out.println("   - Testing factory integration...");
            MemoryMonitoringTest.testFactoryIntegration();
            
            System.out.println("   - Demonstrating memory monitoring...");
            MemoryMonitoringTest.demonstrateMemoryMonitoring();
            
            System.out.println("   - Testing memory optimization configurations...");
            BuildSpotsMeasuresMemoryTest.exampleUsage();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("   Error in memory optimization tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Runs Excel export tests.
     * 
     * @return true if all tests pass, false otherwise
     */
    private static boolean runExcelExportTests() {
        try {
            System.out.println("   - Testing memory monitoring for Excel export...");
            MemoryMonitoringTest.testMemoryMonitoring();
            
            System.out.println("   - Testing Excel export factory...");
            MemoryMonitoringTest.testFactoryIntegration();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("   Error in Excel export tests: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Runs a specific test category.
     * 
     * @param testCategory The test category to run
     * @return true if tests pass, false otherwise
     */
    public static boolean runSpecificTests(String testCategory) {
        switch (testCategory.toLowerCase()) {
            case "fileloading":
            case "file":
                return runFileLoadingTests();
                
            case "memory":
            case "memoryoptimization":
                return runMemoryOptimizationTests();
                
            case "excel":
            case "export":
                return runExcelExportTests();
                
            default:
                System.err.println("Unknown test category: " + testCategory);
                System.err.println("Available categories: fileloading, memory, excel");
                return false;
        }
    }
    
    /**
     * Prints test configuration information.
     */
    public static void printTestConfiguration() {
        System.out.println("Test Configuration");
        System.out.println("==================");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Available memory: " + 
            (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        System.out.println("Number of processors: " + 
            Runtime.getRuntime().availableProcessors());
        System.out.println();
    }
} 