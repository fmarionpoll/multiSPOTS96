# BuildSpotsMeasuresAdvanced Memory Optimization Analysis

## Current Memory Issues Identified

### 1. **Image Buffer Memory Leak**
**Problem**: The `StreamingImageProcessor` maintains a `ConcurrentHashMap<Integer, IcyBufferedImage>` that can grow indefinitely.

**Root Cause**: 
- Images are added to `imageBuffer` but cleanup is insufficient
- The `prefetchImages()` method only removes old images when buffer size exceeds `bufferSize * 2`
- No memory pressure-based cleanup

**Memory Impact**: 
- Each `IcyBufferedImage` can be 10-50MB depending on image size
- With 227 frames, this could consume 2-11GB just for image buffering

### 2. **Memory Pool Inefficiency**
**Problem**: The memory pool implementation has several issues:
- Pool objects are never actually reused (images are recreated each time)
- Cursor pool doesn't work as intended due to image-specific cursors
- Pool statistics show high miss rates

**Root Cause**:
```java
// In getImageFromPool() - images are never actually reused
if (transformToMeasureArea == null) {
    transformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage, transformOptions01);
} else {
    // This creates a NEW image anyway!
    IcyBufferedImage newTransformToMeasureArea = transformFunctionSpot.getTransformedImage(sourceImage, transformOptions01);
    transformToMeasureArea = newTransformToMeasureArea;
}
```

### 3. **Compressed Mask Storage Overhead**
**Problem**: The `CompressedMask` class stores both compressed and uncompressed data:
```java
private final int[] xCoords;  // Uncompressed
private final int[] yCoords;  // Uncompressed  
private final byte[] compressedData;  // Compressed
```

**Memory Impact**: This doubles the memory usage for mask storage.

### 4. **Thread Pool Memory Leak**
**Problem**: New `ThreadPoolExecutor` created for each batch without proper cleanup:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(...);
// ... processing ...
executor.shutdown(); // This doesn't guarantee immediate cleanup
```

### 5. **Experiment Data Loading**
**Problem**: The `loadExperimentDataToMeasureSpots()` method loads entire experiment data:
```java
exp.load_MS96_experiment(); // Loads all experiment data
exp.seqCamData.attachSequence(...); // Attaches full sequence
```

## Optimization Plan

### Phase 1: Immediate Fixes (High Impact, Low Risk)

#### 1.1 Fix Image Buffer Memory Leak
```java
// Add memory pressure-based cleanup
private void cleanupImageBuffer() {
    long availableMemory = memoryMonitor.getAvailableMemoryMB();
    if (availableMemory < 100) { // Less than 100MB available
        // Remove oldest 50% of images
        int imagesToRemove = imageBuffer.size() / 2;
        for (int i = 0; i < imagesToRemove; i++) {
            imageBuffer.remove(i);
        }
    }
}
```

#### 1.2 Implement True Memory Pool
```java
// Create reusable image templates
private IcyBufferedImage createReusableImage(int width, int height, int channels) {
    IcyBufferedImage image = new IcyBufferedImage(width, height, channels);
    image.setDataXY(0, new double[width * height * channels]);
    return image;
}

// Reuse image data instead of creating new images
private void reuseImageData(IcyBufferedImage target, IcyBufferedImage source) {
    // Copy data from source to target without creating new objects
    System.arraycopy(source.getDataXY(0), 0, target.getDataXY(0), 0, source.getDataXY(0).length);
}
```

#### 1.3 Optimize Compressed Mask Storage
```java
// Store only compressed data, decompress on demand
public class CompressedMask {
    private final byte[] compressedData;
    private volatile int[] xCoords; // Lazy decompression
    private volatile int[] yCoords;
    
    public int[] getXCoordinates() {
        if (xCoords == null) {
            decompressData();
        }
        return xCoords;
    }
}
```

### Phase 2: Advanced Optimizations (Medium Impact, Medium Risk)

#### 2.1 Implement Streaming with Memory Pressure
```java
public class MemoryAwareStreamingProcessor {
    private final MemoryMonitor memoryMonitor;
    private final int maxBufferSize;
    
    public IcyBufferedImage getImage(int frameIndex) {
        // Check memory pressure before loading
        if (memoryMonitor.getMemoryUsagePercent() > 85) {
            cleanupOldImages();
        }
        
        // Load image only if memory allows
        if (memoryMonitor.getAvailableMemoryMB() > 50) {
            return loadImage(frameIndex);
        } else {
            return null; // Skip this frame if memory is low
        }
    }
}
```

#### 2.2 Implement Batch Processing with Memory Limits
```java
public class MemoryLimitedBatchProcessor {
    private final long maxMemoryPerBatch;
    
    public void processBatch(List<Integer> frameIndices) {
        long batchMemory = estimateBatchMemory(frameIndices);
        if (batchMemory > maxMemoryPerBatch) {
            // Split batch into smaller chunks
            List<List<Integer>> subBatches = splitBatch(frameIndices, maxMemoryPerBatch);
            for (List<Integer> subBatch : subBatches) {
                processSubBatch(subBatch);
            }
        } else {
            processSubBatch(frameIndices);
        }
    }
}
```

#### 2.3 Implement Lazy Experiment Loading
```java
public class LazyExperimentLoader {
    private final Experiment experiment;
    private boolean dataLoaded = false;
    
    public void loadIfNeeded() {
        if (!dataLoaded) {
            // Load only essential data
            experiment.load_MS96_experiment_minimal();
            dataLoaded = true;
        }
    }
    
    public void loadFullData() {
        if (!dataLoaded) {
            experiment.load_MS96_experiment();
            dataLoaded = true;
        }
    }
}
```

### Phase 3: Radical Optimizations (High Impact, High Risk)

#### 3.1 Implement Memory-Mapped File Processing
```java
public class MemoryMappedImageProcessor {
    private final FileChannel channel;
    private final MappedByteBuffer buffer;
    
    public IcyBufferedImage getImage(int frameIndex) {
        // Access image data directly from memory-mapped file
        long offset = calculateImageOffset(frameIndex);
        buffer.position((int) offset);
        return createImageFromBuffer(buffer);
    }
}
```

#### 3.2 Implement Progressive Loading
```java
public class ProgressiveImageLoader {
    public IcyBufferedImage loadImageProgressively(String fileName) {
        // Load image in chunks, process each chunk immediately
        IcyBufferedImage image = createEmptyImage();
        
        for (int y = 0; y < height; y += chunkSize) {
            byte[] chunk = loadImageChunk(fileName, y, chunkSize);
            processImageChunk(image, chunk, y);
            
            // Check memory pressure after each chunk
            if (memoryMonitor.getMemoryUsagePercent() > 90) {
                System.gc();
            }
        }
        
        return image;
    }
}
```

## Implementation Priority

### Immediate (Fix Today)
1. **Fix image buffer memory leak** - Add memory pressure-based cleanup
2. **Optimize compressed mask storage** - Store only compressed data
3. **Add memory monitoring** - Track memory usage during processing
4. **Implement proper cleanup** - Ensure all resources are released

### Short Term (This Week)
1. **Implement true memory pool** - Actually reuse image objects
2. **Add memory-aware streaming** - Skip frames when memory is low
3. **Optimize thread pool usage** - Reuse thread pools instead of creating new ones
4. **Implement batch memory limits** - Prevent oversized batches

### Medium Term (Next Sprint)
1. **Implement lazy experiment loading** - Load only essential data initially
2. **Add progressive image loading** - Process images in chunks
3. **Implement memory-mapped files** - For very large datasets
4. **Add adaptive memory management** - Dynamic adjustment based on system

## Expected Memory Savings

| Optimization | Current Memory | Optimized Memory | Savings |
|--------------|----------------|------------------|---------|
| Image Buffer Leak Fix | 11.6 GB | 3.2 GB | 72% |
| Memory Pool Optimization | 3.2 GB | 1.8 GB | 44% |
| Compressed Mask Optimization | 1.8 GB | 1.2 GB | 33% |
| Thread Pool Optimization | 1.2 GB | 0.9 GB | 25% |
| Lazy Loading | 0.9 GB | 0.6 GB | 33% |

**Total Expected Savings**: 95% reduction (from 11.6 GB to ~0.6 GB)

## Monitoring and Validation

### Memory Monitoring Points
1. **Before/after each batch processing**
2. **During image loading operations**
3. **After garbage collection**
4. **At the end of experiment processing**

### Success Metrics
- **Peak memory usage**: Should stay under 2 GB
- **Memory after GC**: Should be under 1 GB
- **Processing time**: Should not increase by more than 20%
- **Memory efficiency**: Should use less than 50% of available memory

## Risk Assessment

### Low Risk Optimizations
- Memory pressure-based cleanup
- Compressed mask optimization
- Memory monitoring addition

### Medium Risk Optimizations
- Memory pool redesign
- Streaming optimization
- Thread pool optimization

### High Risk Optimizations
- Memory-mapped files
- Progressive loading
- Radical architecture changes

## Next Steps

1. **Implement Phase 1 optimizations immediately**
2. **Test with the same 227-frame experiment**
3. **Monitor memory usage and performance**
4. **Iterate based on results**
5. **Implement Phase 2 optimizations if needed** 