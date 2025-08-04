# Externalized Classes from BuildSpotsMeasuresAdvanced

This document describes the classes that have been externalized from `BuildSpotsMeasuresAdvanced.java` to make them reusable by other classes in the multiSPOTS96 project.

## Overview

The following internal classes have been externalized:

1. **CompressedMask** - Compressed mask storage using run-length encoding
2. **StreamingImageProcessor** - Streaming image processing to avoid loading entire stack
3. **MemoryMonitor** - Memory usage monitoring and management
4. **AdaptiveBatchSizer** - Adaptive batch sizing based on available memory

## Classes

### CompressedMask

**File:** `CompressedMask.java`

**Purpose:** Provides efficient storage and retrieval of mask coordinates using run-length encoding to reduce memory footprint.

**Key Features:**
- Run-length encoding for consecutive coordinates
- Lazy decompression on demand
- Memory-efficient storage
- Thread-safe decompression

**Usage:**
```java
Point[] maskPoints = roiT.getMaskPoints();
CompressedMask compressedMask = new CompressedMask(maskPoints);
int[] xCoords = compressedMask.getXCoordinates();
int[] yCoords = compressedMask.getYCoordinates();
double compressionRatio = compressedMask.getCompressionRatio();
```

### StreamingImageProcessor

**File:** `StreamingImageProcessor.java`

**Purpose:** Provides efficient image loading and processing by loading images on-demand rather than loading the entire stack into memory.

**Key Features:**
- On-demand image loading
- Memory pressure monitoring
- Native Java I/O support
- Automatic fallback to Icy's image loading
- Memory cleanup after each frame

**Usage:**
```java
MemoryMonitor memoryMonitor = new MemoryMonitor();
StreamingImageProcessor processor = new StreamingImageProcessor(memoryMonitor);
processor.start(seqCamData, startFrame, endFrame);
IcyBufferedImage image = processor.getImage(frameIndex);
processor.stop();
```

### MemoryMonitor

**File:** `MemoryMonitor.java`

**Purpose:** Provides utilities for monitoring and managing memory usage during image processing operations.

**Key Features:**
- Real-time memory usage monitoring
- Memory footprint estimation for images
- Available memory calculation
- Memory usage percentage calculation

**Usage:**
```java
MemoryMonitor monitor = new MemoryMonitor();
long usedMemory = monitor.getUsedMemoryMB();
long maxMemory = monitor.getMaxMemoryMB();
double usagePercent = monitor.getMemoryUsagePercent();
long imageFootprint = monitor.estimateImageMemoryFootprint(image);
```

### AdaptiveBatchSizer

**File:** `AdaptiveBatchSizer.java`

**Purpose:** Provides dynamic batch sizing capabilities that adjust based on available memory to optimize processing performance while preventing memory overflow.

**Key Features:**
- Dynamic batch size adjustment
- Memory pressure-based optimization
- Configurable min/max batch sizes
- Automatic initialization based on available memory

**Usage:**
```java
MemoryMonitor memoryMonitor = new MemoryMonitor();
AdaptiveBatchSizer batchSizer = new AdaptiveBatchSizer(memoryMonitor);
batchSizer.initialize(totalFrames, availableMemoryMB);
int batchSize = batchSizer.getCurrentBatchSize();
batchSizer.updateBatchSize(memoryUsagePercent);
```

## Integration with BuildSpotsMeasuresAdvanced

The `BuildSpotsMeasuresAdvanced` class has been updated to use these external classes:

1. **Constructor updated** to create instances of external classes
2. **Inner classes removed** and replaced with external class references
3. **Method calls updated** to use the external class instances

## Benefits of Externalization

1. **Reusability:** These classes can now be used by other parts of the multiSPOTS96 project
2. **Maintainability:** Each class has a single responsibility and can be maintained independently
3. **Testability:** Individual classes can be unit tested in isolation
4. **Modularity:** Classes can be used independently without depending on the entire BuildSpotsMeasuresAdvanced class
5. **Documentation:** Each class has its own documentation and clear purpose

## Testing

A test class `ExternalClassesTest.java` has been created to verify that all externalized classes work correctly. Run this class to test the functionality of all externalized classes.

## Migration Notes

- All existing functionality in `BuildSpotsMeasuresAdvanced` remains unchanged
- The externalized classes maintain the same API as their inner class counterparts
- No changes are required to existing code that uses `BuildSpotsMeasuresAdvanced`
- The externalized classes can be used independently by other classes in the project 