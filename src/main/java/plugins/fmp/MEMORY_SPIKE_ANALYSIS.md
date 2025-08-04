# MEMORY SPIKE ANALYSIS - POST-PROCESSING LEAK

## Problem Description
- **During Processing**: Memory usage stabilizes at ~5.2GB
- **After Processing**: Memory spikes to 9.5GB when returning from `ThresholdSimpleAdvanced` dialog
- **Timing**: Memory leak occurs during cleanup/return phase, not during processing

## Root Cause Analysis

### 1. Dialog Return Memory Leak
**Problem**: The `ThresholdSimpleAdvanced` dialog may be holding references to large objects
**Evidence**: Memory spike occurs specifically when returning from the dialog
**Likely Causes**:
- Dialog retains references to `BuildSpotsMeasuresAdvanced` instance
- Large objects not properly cleaned up before dialog return
- Icy framework may be caching data in the dialog context

### 2. Post-Processing Cleanup Issues
**Problem**: Cleanup methods may not be releasing all memory
**Evidence**: Memory usage doubles after processing completes
**Potential Issues**:
- `cleanupResources()` may not be comprehensive enough
- Icy internal caches not cleared
- Dialog references to processing objects

### 3. Icy Framework Memory Retention
**Problem**: Icy may be retaining image data in internal caches
**Evidence**: Memory spike suggests framework-level retention
**Common Issues**:
- Image caches not cleared
- Viewer references not released
- Sequence data retained

## Immediate Solutions

### 1. Enhanced Post-Processing Cleanup
```java
// Add to BuildSpotsMeasuresAdvanced
private void enhancedPostProcessingCleanup() {
    System.out.println("=== ENHANCED POST-PROCESSING CLEANUP ===");
    
    // Force multiple GC passes
    for (int i = 0; i < 5; i++) {
        System.gc();
        try {
            Thread.sleep(200); // Longer delays for post-processing cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Clear all caches and references
    clearAllCaches();
    clearAllReferences();
    forceIcyCleanup();
    
    // Final memory check
    logMemoryUsage("After Enhanced Cleanup");
}

private void clearAllCaches() {
    // Clear compressed mask cache
    if (compressedMasks != null) {
        compressedMasks.clear();
        System.out.println("Cleared compressed mask cache");
    }
    
    // Clear any other caches
    totalTransformedImagesCreated = 0;
    totalCursorsCreated = 0;
    totalImagesProcessed = 0;
}

private void clearAllReferences() {
    // Clear all image references
    if (seqData != null) {
        try {
            seqData.clear();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    // Clear viewer references
    if (vData != null) {
        try {
            vData.dispose();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        vData = null;
    }
    
    // Clear transform references
    transformToMeasureArea = null;
    transformToDetectFly = null;
    transformOptions01 = null;
    transformOptions02 = null;
    transformFunctionSpot = null;
    transformFunctionFly = null;
}

private void forceIcyCleanup() {
    // Try to clear Icy's internal caches
    try {
        // Clear Icy image cache
        Class<?> icyImageCacheClass = Class.forName("icy.image.ImageCache");
        java.lang.reflect.Method clearCacheMethod = icyImageCacheClass.getDeclaredMethod("clearCache");
        if (clearCacheMethod != null) {
            clearCacheMethod.setAccessible(true);
            clearCacheMethod.invoke(null);
            System.out.println("Cleared Icy image cache");
        }
    } catch (Exception e) {
        System.out.println("Could not clear Icy image cache: " + e.getMessage());
    }
    
    // Try to clear Icy sequence cache
    try {
        Class<?> icySequenceClass = Class.forName("icy.sequence.Sequence");
        java.lang.reflect.Method disposeMethod = icySequenceClass.getDeclaredMethod("dispose");
        if (disposeMethod != null) {
            disposeMethod.setAccessible(true);
            disposeMethod.invoke(seqData);
            System.out.println("Disposed Icy sequence");
        }
    } catch (Exception e) {
        System.out.println("Could not dispose Icy sequence: " + e.getMessage());
    }
}
```

### 2. Modify analyzeExperiment Method
```java
void analyzeExperiment(Experiment exp) {
    try {
        getTimeLimitsOfSequence(exp);
        loadExperimentDataToMeasureSpots(exp);
        exp.cagesArray.setReadyToAnalyze(true, options);
        openViewers(exp);

        if (measureSpotsAdvanced(exp))
            saveComputation(exp);

        exp.cagesArray.setReadyToAnalyze(false, options);
        closeViewers();
        cleanupResources();
        
        // ENHANCED POST-PROCESSING CLEANUP
        enhancedPostProcessingCleanup();
        
    } finally {
        // Ensure cleanup happens even if exceptions occur
        try {
            exp.cagesArray.setReadyToAnalyze(false, options);
            closeViewers();
            cleanupResources();
            enhancedPostProcessingCleanup();
        } catch (Exception e) {
            System.err.println("Error during final cleanup: " + e.getMessage());
        }
    }
}
```

### 3. Add Memory Monitoring to Dialog Return
```java
// Add to ThresholdSimpleAdvanced dialog class
private void monitorMemoryOnReturn() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
    
    System.out.println("=== DIALOG RETURN MEMORY CHECK ===");
    System.out.println("Memory Usage: " + usagePercent + "%");
    System.out.println("Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
    System.out.println("Max Memory: " + (runtime.maxMemory() / 1024 / 1024) + " MB");
    
    if (usagePercent > 70) {
        System.err.println("WARNING: High memory usage on dialog return!");
        System.err.println("Consider forcing cleanup before dialog return");
        
        // Force cleanup if memory is high
        forceDialogReturnCleanup();
    }
}

private void forceDialogReturnCleanup() {
    // Force multiple GC passes
    for (int i = 0; i < 3; i++) {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Clear any dialog-specific caches
    clearDialogCaches();
}

private void clearDialogCaches() {
    // Clear any caches specific to the dialog
    // This will depend on what the dialog is caching
}
```

### 4. Implement Weak References for Dialog References
```java
// In ThresholdSimpleAdvanced dialog
private WeakReference<BuildSpotsMeasuresAdvanced> advancedProcessorRef;

public void setAdvancedProcessor(BuildSpotsMeasuresAdvanced processor) {
    this.advancedProcessorRef = new WeakReference<>(processor);
}

// Use weak reference to avoid holding strong references
private BuildSpotsMeasuresAdvanced getAdvancedProcessor() {
    if (advancedProcessorRef != null) {
        return advancedProcessorRef.get();
    }
    return null;
}
```

## Long-term Solutions

### 1. Implement Disposable Pattern
```java
// Add to BuildSpotsMeasuresAdvanced
public void dispose() {
    System.out.println("Disposing BuildSpotsMeasuresAdvanced...");
    
    // Stop any running processes
    threadRunning = false;
    stopFlag = true;
    
    // Clear all references
    clearAllReferences();
    clearAllCaches();
    
    // Force cleanup
    enhancedPostProcessingCleanup();
    
    System.out.println("BuildSpotsMeasuresAdvanced disposed");
}

// Call dispose before dialog return
public void prepareForDialogReturn() {
    dispose();
}
```

### 2. Add Memory Pressure Monitoring to Dialog
```java
// In ThresholdSimpleAdvanced
private void checkMemoryBeforeReturn() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
    
    if (usagePercent > 60) {
        System.err.println("WARNING: High memory before dialog return: " + usagePercent + "%");
        
        // Force cleanup
        forceDialogReturnCleanup();
        
        // Check again
        usedMemory = runtime.totalMemory() - runtime.freeMemory();
        usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
        
        System.out.println("After cleanup: " + usagePercent + "%");
    }
}
```

### 3. Implement Memory-Efficient Dialog Pattern
```java
// In ThresholdSimpleAdvanced
@Override
public void dispose() {
    // Clear all dialog references
    clearDialogReferences();
    
    // Force cleanup
    forceDialogReturnCleanup();
    
    // Call super dispose
    super.dispose();
}

private void clearDialogReferences() {
    // Clear any references to processing objects
    // This will depend on what the dialog is holding
}
```

## Monitoring and Verification

### 1. Add Memory Tracking Points
```java
// Add to BuildSpotsMeasuresAdvanced.analyzeExperiment
void analyzeExperiment(Experiment exp) {
    logMemoryUsage("Before Analysis");
    
    try {
        // ... existing code ...
        
        if (measureSpotsAdvanced(exp))
            saveComputation(exp);
            
        logMemoryUsage("After Processing");
        
        // ... cleanup code ...
        
        enhancedPostProcessingCleanup();
        logMemoryUsage("After Enhanced Cleanup");
        
    } finally {
        // ... final cleanup ...
        logMemoryUsage("After Final Cleanup");
    }
}
```

### 2. Add Dialog Return Monitoring
```java
// In ThresholdSimpleAdvanced
public void onDialogReturn() {
    logMemoryUsage("Before Dialog Return");
    
    // Force cleanup
    forceDialogReturnCleanup();
    
    logMemoryUsage("After Dialog Return Cleanup");
}
```

## Expected Results

### Memory Usage Pattern (Expected)
| Stage | Memory Usage | Notes |
|-------|--------------|-------|
| Before Analysis | ~2-3GB | Initial state |
| During Processing | ~5.2GB | Stable during processing |
| After Processing | ~5.5GB | Slight increase |
| After Enhanced Cleanup | ~3-4GB | Significant reduction |
| After Dialog Return | ~3-4GB | No spike |

### Key Improvements
1. **No Memory Spike**: Dialog return should not cause memory increase
2. **Proper Cleanup**: All references properly cleared
3. **Memory Monitoring**: Clear visibility into memory usage at each stage
4. **Weak References**: Dialog doesn't hold strong references to processing objects

## Implementation Priority

### High Priority (Immediate)
1. Add `enhancedPostProcessingCleanup()` to `analyzeExperiment`
2. Implement memory monitoring in dialog return
3. Add weak references for dialog-object relationships

### Medium Priority (Next Iteration)
1. Implement disposable pattern for all processing objects
2. Add comprehensive Icy framework cleanup
3. Implement memory-efficient dialog patterns

### Low Priority (Future)
1. Add memory leak detection to dialog framework
2. Implement automatic cleanup triggers
3. Add memory usage alerts

## Conclusion

The memory spike during dialog return suggests that the dialog is holding references to large processing objects or that cleanup is not comprehensive enough. The immediate solutions focus on enhanced cleanup and memory monitoring, while long-term solutions involve implementing proper disposal patterns and weak references.

The goal is to eliminate the memory spike and ensure that memory usage returns to baseline levels after processing completes. 