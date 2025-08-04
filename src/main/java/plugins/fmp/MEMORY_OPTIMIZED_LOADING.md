# MEMORY OPTIMIZED LOADING - EXPERIMENT DATA SPIKE PREVENTION

## Problem Analysis

### Memory Spike Root Cause
- **Trigger**: `exp.load_MS96_spotsMeasures()` called in dialog after processing
- **Location**: `ThresholdSimpleAdvanced.propertyChange()` method
- **Memory Increase**: ~4GB spike (5GB → 9GB)
- **Cause**: Loading large CSV files with processed data back into memory

### Data Loading Chain
```
ThresholdSimpleAdvanced.propertyChange()
  → exp.load_MS96_spotsMeasures()
    → cagesArray.load_SpotsMeasures(directory)
      → spotsArray.loadSpotsMeasures(directory)
        → csvLoadSpots(directory, EnumSpotMeasures.SPOTS_MEASURES)
          → Creates large data structures in memory
```

## Immediate Solutions

### 1. Lazy Loading Implementation
```java
// In ThresholdSimpleAdvanced.java
@Override
public void propertyChange(PropertyChangeEvent evt) {
    if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
        detectButton.setText(detectString);
        
        // Check memory before loading experiment data
        checkMemoryBeforeLoading();
        
        Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
        if (exp != null) {
            // Use memory-optimized loading
            loadExperimentDataOptimized(exp);
            parent0.dlgMeasure.tabCharts.displayChartPanels(exp);
        }
        
        // Clear processor reference to allow GC
        processorRef = null;
        
        logMemoryUsage("After Detection Complete");
    }
}

private void checkMemoryBeforeLoading() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
    
    System.out.println("=== BEFORE EXPERIMENT LOADING ===");
    System.out.println("Memory Usage: " + usagePercent + "%");
    System.out.println("Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
    
    if (usagePercent > 50) {
        System.err.println("WARNING: High memory before loading experiment data!");
        System.err.println("Forcing cleanup before loading...");
        
        // Force cleanup before loading
        forcePreLoadingCleanup();
        
        // Check again
        usedMemory = runtime.totalMemory() - runtime.freeMemory();
        usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
        System.out.println("After cleanup: " + usagePercent + "%");
    }
}

private void forcePreLoadingCleanup() {
    System.out.println("=== FORCING PRE-LOADING CLEANUP ===");
    
    // Force multiple GC passes
    for (int i = 0; i < 3; i++) {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

private void loadExperimentDataOptimized(Experiment exp) {
    System.out.println("=== OPTIMIZED EXPERIMENT LOADING ===");
    
    try {
        // Load with memory monitoring
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Use optimized loading method
        boolean success = exp.load_MS96_spotsMeasuresOptimized();
        
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryIncrease = endMemory - startMemory;
        
        System.out.println("Experiment loading completed: " + (success ? "SUCCESS" : "FAILED"));
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        if (memoryIncrease > 1024 * 1024 * 1024) { // > 1GB
            System.err.println("WARNING: Large memory increase during loading: " + (memoryIncrease / 1024 / 1024) + " MB");
            System.err.println("Consider implementing streaming loading for large datasets");
        }
        
    } catch (Exception e) {
        System.err.println("Error during optimized experiment loading: " + e.getMessage());
        // Fallback to original method
        exp.load_MS96_spotsMeasures();
    }
}
```

### 2. Memory-Optimized Experiment Loading
```java
// In Experiment.java
public boolean load_MS96_spotsMeasuresOptimized() {
    System.out.println("=== MEMORY-OPTIMIZED SPOTS MEASURES LOADING ===");
    
    // Check available memory before loading
    Runtime runtime = Runtime.getRuntime();
    long availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    long requiredMemory = estimateRequiredMemory();
    
    System.out.println("Available memory: " + (availableMemory / 1024 / 1024) + " MB");
    System.out.println("Estimated required memory: " + (requiredMemory / 1024 / 1024) + " MB");
    
    if (availableMemory < requiredMemory * 1.5) { // Need 50% buffer
        System.err.println("WARNING: Insufficient memory for loading!");
        System.err.println("Available: " + (availableMemory / 1024 / 1024) + " MB");
        System.err.println("Required: " + (requiredMemory / 1024 / 1024) + " MB");
        
        // Force cleanup and try again
        forceMemoryCleanup();
        
        availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        if (availableMemory < requiredMemory) {
            System.err.println("Still insufficient memory after cleanup. Using streaming loading...");
            return load_MS96_spotsMeasuresStreaming();
        }
    }
    
    // Proceed with normal loading
    return cagesArray.load_SpotsMeasuresOptimized(getResultsDirectory());
}

private long estimateRequiredMemory() {
    // Estimate memory required based on file size and data structure overhead
    String resultsDir = getResultsDirectory();
    if (resultsDir == null) return 0;
    
    Path csvPath = Paths.get(resultsDir, "SpotsMeasures.csv");
    if (!Files.exists(csvPath)) return 0;
    
    try {
        long fileSize = Files.size(csvPath);
        // Estimate memory usage: file size * 3 (for data structures, objects, overhead)
        return fileSize * 3;
    } catch (IOException e) {
        return 1024 * 1024 * 100; // Default 100MB estimate
    }
}

private void forceMemoryCleanup() {
    System.out.println("=== FORCING MEMORY CLEANUP ===");
    
    // Force multiple GC passes
    for (int i = 0; i < 5; i++) {
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Clear any caches
    clearExperimentCaches();
}

private void clearExperimentCaches() {
    // Clear any experiment-specific caches
    if (seqCamData != null) {
        try {
            // Clear sequence data if possible
            seqCamData.clearCaches();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    // Clear cages array caches
    if (cagesArray != null) {
        cagesArray.clearCaches();
    }
}

public boolean load_MS96_spotsMeasuresStreaming() {
    System.out.println("=== STREAMING SPOTS MEASURES LOADING ===");
    
    // Implement streaming loading for large datasets
    // This would load data in chunks to avoid memory spikes
    
    try {
        // Load only essential data first
        boolean success = cagesArray.load_SpotsMeasuresEssential(getResultsDirectory());
        
        if (success) {
            // Load additional data in background if needed
            loadAdditionalDataInBackground();
        }
        
        return success;
    } catch (Exception e) {
        System.err.println("Error in streaming loading: " + e.getMessage());
        return false;
    }
}

private void loadAdditionalDataInBackground() {
    // Load non-essential data in background thread
    Thread backgroundLoader = new Thread(() -> {
        try {
            Thread.sleep(1000); // Delay to let UI update
            cagesArray.load_SpotsMeasuresAdditional(getResultsDirectory());
        } catch (Exception e) {
            System.err.println("Background loading error: " + e.getMessage());
        }
    });
    backgroundLoader.setDaemon(true);
    backgroundLoader.start();
}
```

### 3. Optimized CagesArray Loading
```java
// In CagesArray.java
public boolean load_SpotsMeasuresOptimized(String directory) {
    System.out.println("=== OPTIMIZED CAGES ARRAY LOADING ===");
    
    // Create temporary spots array to avoid memory leaks
    SpotsArray tempSpotsArray = new SpotsArray();
    
    try {
        boolean success = tempSpotsArray.loadSpotsMeasuresOptimized(directory);
        
        if (success) {
            // Transfer data to cages efficiently
            transferSpotsToCagesOptimized(tempSpotsArray);
        }
        
        return success;
    } finally {
        // Clear temporary array
        tempSpotsArray.clearSpots();
    }
}

public boolean load_SpotsMeasuresEssential(String directory) {
    System.out.println("=== LOADING ESSENTIAL SPOTS DATA ===");
    
    // Load only essential data (spot names, basic properties)
    SpotsArray spotsArray = getSpotsArrayFromAllCages();
    return spotsArray.loadSpotsMeasuresEssential(directory);
}

public boolean load_SpotsMeasuresAdditional(String directory) {
    System.out.println("=== LOADING ADDITIONAL SPOTS DATA ===");
    
    // Load additional data (measurements, statistics)
    SpotsArray spotsArray = getSpotsArrayFromAllCages();
    return spotsArray.loadSpotsMeasuresAdditional(directory);
}

private void transferSpotsToCagesOptimized(SpotsArray sourceArray) {
    // Efficiently transfer spots data to cages
    for (Cage cage : cagesList) {
        for (Spot sourceSpot : sourceArray.getSpotsList()) {
            // Find matching spot in cage
            Spot targetSpot = cage.getSpotsArray().findSpotByName(sourceSpot.getProperties().getName());
            if (targetSpot != null) {
                // Transfer data efficiently
                targetSpot.copyMeasuresFrom(sourceSpot);
            }
        }
    }
}

public void clearCaches() {
    // Clear any caches in cages array
    for (Cage cage : cagesList) {
        cage.clearCaches();
    }
}
```

### 4. Optimized SpotsArray Loading
```java
// In SpotsArray.java
public boolean loadSpotsMeasuresOptimized(String directory) {
    System.out.println("=== OPTIMIZED SPOTS ARRAY LOADING ===");
    
    // Use memory-efficient loading
    return loadSpotsOptimized(directory, EnumSpotMeasures.SPOTS_MEASURES);
}

public boolean loadSpotsMeasuresEssential(String directory) {
    System.out.println("=== LOADING ESSENTIAL SPOTS DATA ===");
    
    // Load only essential data (names, properties)
    return loadSpotsEssential(directory);
}

public boolean loadSpotsMeasuresAdditional(String directory) {
    System.out.println("=== LOADING ADDITIONAL SPOTS DATA ===");
    
    // Load additional data (measurements)
    return loadSpotsAdditional(directory);
}

private boolean loadSpotsOptimized(String directory, EnumSpotMeasures measureType) {
    if (directory == null) {
        return false;
    }

    try {
        // Use streaming CSV reader to avoid loading entire file into memory
        return csvLoadSpotsStreaming(directory, measureType);
    } catch (Exception e) {
        System.err.println("Error in optimized spots loading: " + e.getMessage());
        return false;
    }
}

private boolean csvLoadSpotsStreaming(String directory, EnumSpotMeasures measureType) throws Exception {
    Path csvPath = Paths.get(directory, CSV_FILENAME);
    if (!Files.exists(csvPath)) {
        return false;
    }

    // Use streaming approach to avoid memory spikes
    try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
        String line;
        String sep = CSV_SEPARATOR;
        
        // Process file in chunks
        int lineCount = 0;
        int chunkSize = 1000; // Process 1000 lines at a time
        
        while ((line = reader.readLine()) != null) {
            lineCount++;
            
            if (line.charAt(0) == '#')
                sep = String.valueOf(line.charAt(1));
            
            String[] data = line.split(sep);
            if (data[0].equals("#")) {
                switch (data[1]) {
                case "SPOTS_ARRAY":
                    csvLoadSpotsDescriptionStreaming(reader, sep);
                    break;
                case "SPOTS":
                    csvLoadSpotsArrayStreaming(reader, sep);
                    break;
                case "AREA_SUM":
                case "AREA_SUMCLEAN":
                case "AREA_FLYPRESENT":
                default:
                    EnumSpotMeasures measure = EnumSpotMeasures.findByText(data[1]);
                    if (measure != null)
                        csvLoadSpotsMeasuresStreaming(reader, measure, sep);
                    break;
                }
            }
            
            // Force GC every chunkSize lines to prevent memory buildup
            if (lineCount % chunkSize == 0) {
                System.gc();
                Thread.yield(); // Give GC time to work
            }
        }
        
        return true;
    }
}

private void csvLoadSpotsArrayStreaming(BufferedReader reader, String csvSeparator) throws IOException {
    String line = reader.readLine();
    int spotCount = 0;
    
    while ((line = reader.readLine()) != null) {
        String[] spotData = line.split(csvSeparator);
        if (spotData[0].equals("#"))
            return;

        Spot spot = findSpotByName(spotData[0]);
        if (spot == null) {
            spot = new Spot();
            spotsList.add(spot);
            spot.getProperties().importFromCsv(spotData);
            spotCount++;
        }
        
        // Force GC every 100 spots to prevent memory buildup
        if (spotCount % 100 == 0) {
            System.gc();
            Thread.yield();
        }
    }
}

private void csvLoadSpotsMeasuresStreaming(BufferedReader reader, EnumSpotMeasures measureType, String csvSeparator)
        throws IOException {
    String line = reader.readLine();
    boolean y = true;
    boolean x = line.contains("xi");
    int measureCount = 0;
    
    while ((line = reader.readLine()) != null) {
        String[] data = line.split(csvSeparator);
        if (data[0].equals("#"))
            return;

        Spot spot = findSpotByName(data[0]);
        if (spot == null) {
            spot = new Spot();
            spotsList.add(spot);
        }
        spot.importMeasuresOneType(measureType, data, x, y);
        measureCount++;
        
        // Force GC every 500 measures to prevent memory buildup
        if (measureCount % 500 == 0) {
            System.gc();
            Thread.yield();
        }
    }
}
```

## Long-term Solutions

### 1. Implement Data Streaming
- Load data in chunks rather than all at once
- Use memory-mapped files for large datasets
- Implement lazy loading for non-essential data

### 2. Add Memory Monitoring
- Monitor memory usage during loading
- Automatically trigger cleanup when memory pressure is high
- Provide user feedback about memory usage

### 3. Implement Caching Strategy
- Cache frequently accessed data
- Clear caches when memory pressure is high
- Use weak references for cached objects

### 4. Add Configuration Options
- Allow users to configure memory usage limits
- Provide options for streaming vs. full loading
- Add memory usage warnings and recommendations

## Expected Results

### Memory Usage Pattern (Expected)
| Stage | Memory Usage | Notes |
|-------|--------------|-------|
| After Processing | ~5GB | Processing complete |
| Before Loading | ~5GB | After cleanup |
| During Loading | ~6-7GB | Controlled increase |
| After Loading | ~6-7GB | Stable, no spike |

### Key Improvements
1. **No Memory Spike**: Loading should not cause >2GB increase
2. **Controlled Loading**: Memory usage should be predictable
3. **Automatic Cleanup**: Memory pressure triggers cleanup
4. **User Feedback**: Clear logging of memory usage

## Implementation Priority

### High Priority (Immediate)
1. Add memory monitoring before loading
2. Implement pre-loading cleanup
3. Add memory-optimized loading methods

### Medium Priority (Next Iteration)
1. Implement streaming loading for large datasets
2. Add configuration options for memory limits
3. Implement background loading for non-essential data

### Low Priority (Future)
1. Add memory usage analytics
2. Implement adaptive loading strategies
3. Add user-configurable memory management

## Conclusion

The memory spike during experiment loading can be prevented by implementing memory monitoring, pre-loading cleanup, and optimized loading methods. The key is to control the memory usage during the loading process and provide fallback options when memory pressure is high. 