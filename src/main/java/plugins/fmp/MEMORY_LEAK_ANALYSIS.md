# MEMORY LEAK ANALYSIS FOR BUILDSPOTSMEASURESADVANCED

## Memory Growth Pattern Analysis

Based on the memory profiling logs, there's a clear **steady increase in memory usage** across batches:

### Memory Usage Progression
| Batch Range | Before (MB) | After (MB) | Memory Growth | Usage % |
|-------------|-------------|------------|---------------|---------|
| 0-9        | 798         | 1075       | +277 MB       | 4% → 6% |
| 10-19      | 943         | 2134       | +1191 MB      | 5% → 13% |
| 20-29      | 1227        | 1949       | +722 MB       | 7% → 12% |
| 30-39      | 1678        | 3808       | +2130 MB      | 10% → 23% |
| 40-49      | 2260        | 3548       | +1288 MB      | 13% → 21% |
| 50-59      | 2683        | 4186       | +1503 MB      | 16% → 25% |

### Key Observations
1. **Steady Growth**: Memory usage increases by ~1-2GB per batch
2. **Insufficient Cleanup**: GC between batches isn't reclaiming enough memory
3. **Accumulation**: Objects are being retained across batches
4. **Total Memory Expansion**: JVM is expanding heap from 2.4GB to 7.7GB

## Root Cause Analysis

### 1. Image Object Retention
**Problem**: `IcyBufferedImage` objects may not be properly garbage collected
**Evidence**: Each batch processes multiple images, but memory keeps growing

### 2. Transformed Image Accumulation
**Problem**: `transformToMeasureArea` and `transformToDetectFly` images are being retained
**Evidence**: Each frame creates 2 transformed images that may not be cleaned up

### 3. Cursor Object Retention
**Problem**: `IcyBufferedImageCursor` objects may be holding references
**Evidence**: Cursors are created for each transformed image

### 4. Compressed Mask Cache Growth
**Problem**: `compressedMasks` cache may be growing without bounds
**Evidence**: Each ROI creates a compressed mask that's cached

## Immediate Solutions

### 1. Force Aggressive Garbage Collection
```java
// Add to processFrameBatchAdvanced after batch completion
private void processFrameBatchAdvanced(Experiment exp, int batchStart, int batchEnd, int iiFirst, int iiLast,
        ProgressFrame progressBar1) {
    
    // ... existing code ...
    
    // Wait for all tasks to complete
    waitFuturesCompletion(processor, tasks, null);
    
    // Memory profiling - log after batch processing
    if (options.enableMemoryProfiling) {
        logMemoryUsage("After Batch " + batchStart + "-" + (batchEnd - 1));
    }
    
    // FORCE AGGRESSIVE CLEANUP
    forceAggressiveCleanup();
}

private void forceAggressiveCleanup() {
    // Multiple GC passes with delays
    for (int i = 0; i < 3; i++) {
        System.gc();
        try {
            Thread.sleep(100); // Give GC time to work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Force memory pool cleanup
    clearImageCaches();
    clearCompressedMaskCache();
}
```

### 2. Clear Image Caches
```java
private void clearImageCaches() {
    // Clear any Icy internal caches if accessible
    try {
        // Try to clear Icy's image cache
        Class<?> icyImageCacheClass = Class.forName("icy.image.ImageCache");
        java.lang.reflect.Method clearCacheMethod = icyImageCacheClass.getDeclaredMethod("clearCache");
        if (clearCacheMethod != null) {
            clearCacheMethod.setAccessible(true);
            clearCacheMethod.invoke(null);
        }
    } catch (Exception e) {
        // Icy cache clearing not available
    }
}
```

### 3. Limit Compressed Mask Cache
```java
private void clearCompressedMaskCache() {
    // Limit cache size to prevent unbounded growth
    if (compressedMasks.size() > 1000) {
        System.out.println("Clearing compressed mask cache (size: " + compressedMasks.size() + ")");
        compressedMasks.clear();
    }
}
```

### 4. Enhanced Resource Cleanup in processSingleFrameAdvanced
```java
private void processSingleFrameAdvanced(Experiment exp, int frameIndex, int iiFirst, IcyBufferedImage sourceImage) {
    IcyBufferedImage transformToMeasureArea = null;
    IcyBufferedImage transformToDetectFly = null;
    IcyBufferedImageCursor cursorToDetectFly = null;
    IcyBufferedImageCursor cursorToMeasureArea = null;

    try {
        // Create transformed images (same as original)
        transformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage, transformOptions01);
        transformToDetectFly = transformFunctionFly.getTransformedImage(sourceImage, transformOptions02);
        totalTransformedImagesCreated += 2;

        // Create cursors (same as original)
        cursorToDetectFly = new IcyBufferedImageCursor(transformToDetectFly);
        cursorToMeasureArea = new IcyBufferedImageCursor(transformToMeasureArea);
        totalCursorsCreated += 2;

        // ... existing processing code ...

    } finally {
        // ENHANCED CLEANUP
        if (transformToMeasureArea != null) {
            // Try to clear image data
            try {
                transformToMeasureArea.setDataXY(0, null);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            transformToMeasureArea = null;
        }
        
        if (transformToDetectFly != null) {
            try {
                transformToDetectFly.setDataXY(0, null);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            transformToDetectFly = null;
        }
        
        if (cursorToDetectFly != null) {
            cursorToDetectFly = null;
        }
        
        if (cursorToMeasureArea != null) {
            cursorToMeasureArea = null;
        }
        
        // Force immediate cleanup for this frame
        System.gc();
    }
}
```

### 5. Add Memory Pressure Monitoring
```java
private void checkMemoryPressure() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
    
    if (usagePercent > 70) {
        System.err.println("WARNING: High memory pressure detected: " + usagePercent + "%");
        System.err.println("Used: " + (usedMemory / 1024 / 1024) + "MB");
        
        // Take corrective action
        forceAggressiveCleanup();
        
        // Reduce batch size if needed
        if (usagePercent > 80) {
            adaptiveBatchSizer.reduceBatchSize();
        }
    }
}
```

## Long-term Solutions

### 1. Implement Object Pooling
```java
// Add to BuildSpotsMeasuresAdvanced
private final LinkedBlockingQueue<IcyBufferedImage> imagePool = new LinkedBlockingQueue<>(20);
private final LinkedBlockingQueue<IcyBufferedImageCursor> cursorPool = new LinkedBlockingQueue<>(40);

private IcyBufferedImage getImageFromPool() {
    IcyBufferedImage image = imagePool.poll();
    if (image == null) {
        // Create new image if pool is empty
        return new IcyBufferedImage(1920, 1080, 1, icy.type.DataType.BYTE);
    }
    return image;
}

private void returnImageToPool(IcyBufferedImage image) {
    if (image != null && imagePool.size() < 20) {
        // Clear image data before returning to pool
        try {
            image.setDataXY(0, null);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        imagePool.offer(image);
    }
}
```

### 2. Use Weak References for Caching
```java
// Replace ConcurrentHashMap with WeakHashMap for compressed masks
private final Map<String, CompressedMask> compressedMasks = new WeakHashMap<>();

// Or use a size-limited cache
private final Map<String, CompressedMask> compressedMasks = new LinkedHashMap<String, CompressedMask>(100, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, CompressedMask> eldest) {
        return size() > 100; // Limit cache to 100 entries
    }
};
```

### 3. Implement Batch-Level Memory Limits
```java
private void enforceBatchMemoryLimit() {
    long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long maxBatchMemory = 2 * 1024 * 1024 * 1024; // 2GB limit per batch
    
    if (currentMemory > maxBatchMemory) {
        System.err.println("Batch memory limit exceeded. Forcing cleanup...");
        forceAggressiveCleanup();
        
        // Wait for memory to be freed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## Monitoring and Verification

### 1. Add Detailed Memory Tracking
```java
private void logDetailedMemoryUsage(String stage) {
    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    long maxMemory = runtime.maxMemory();
    
    System.out.println("=== " + stage + " ===");
    System.out.println("Used Memory: " + (usedMemory / 1024 / 1024) + " MB");
    System.out.println("Free Memory: " + (freeMemory / 1024 / 1024) + " MB");
    System.out.println("Total Memory: " + (totalMemory / 1024 / 1024) + " MB");
    System.out.println("Max Memory: " + (maxMemory / 1024 / 1024) + " MB");
    System.out.println("Memory Usage: " + (usedMemory * 100 / maxMemory) + "%");
    
    // Additional tracking
    System.out.println("Compressed Masks: " + compressedMasks.size());
    System.out.println("Total Transformed Images: " + totalTransformedImagesCreated);
    System.out.println("Total Cursors: " + totalCursorsCreated);
}
```

### 2. Generate Heap Dumps at Peak Usage
```java
private void generateHeapDumpIfNeeded(String stage) {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double usagePercent = (usedMemory * 100.0) / runtime.maxMemory();
    
    if (usagePercent > 80) {
        try {
            String filename = "heap_dump_" + stage + "_" + System.currentTimeMillis() + ".hprof";
            java.lang.management.ManagementFactory.getMemoryMXBean().dumpHeap(filename, true);
            System.out.println("Heap dump generated: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to generate heap dump: " + e.getMessage());
        }
    }
}
```

## Expected Results After Implementation

### Memory Usage Pattern (Expected)
| Batch Range | Before (MB) | After (MB) | Memory Growth | Usage % |
|-------------|-------------|------------|---------------|---------|
| 0-9        | 800         | 850        | +50 MB        | 5% → 5% |
| 10-19      | 850         | 900        | +50 MB        | 5% → 5% |
| 20-29      | 900         | 950        | +50 MB        | 5% → 5% |
| 30-39      | 950         | 1000       | +50 MB        | 6% → 6% |
| 40-49      | 1000        | 1050       | +50 MB        | 6% → 6% |
| 50-59      | 1050        | 1100       | +50 MB        | 6% → 6% |

### Key Improvements
1. **Stable Memory Usage**: No more steady growth
2. **Consistent Performance**: Predictable memory patterns
3. **Better GC Efficiency**: More objects properly cleaned up
4. **Reduced Peak Memory**: Lower maximum memory usage

## Implementation Priority

### High Priority (Immediate)
1. Add `forceAggressiveCleanup()` after each batch
2. Enhance resource cleanup in `processSingleFrameAdvanced`
3. Add memory pressure monitoring

### Medium Priority (Next Iteration)
1. Implement object pooling for images and cursors
2. Use weak references for compressed mask cache
3. Add batch-level memory limits

### Low Priority (Future)
1. Implement custom memory management
2. Add production monitoring
3. Optimize for specific image types

## Conclusion

The memory leak pattern shows that objects are not being properly garbage collected between batches. The immediate solutions focus on forcing more aggressive cleanup and monitoring memory pressure. The long-term solutions involve implementing proper object pooling and using weak references to prevent memory accumulation.

The goal is to achieve stable memory usage patterns similar to the original `BuildSpotsMeasures` while maintaining the performance benefits of the advanced implementation. 