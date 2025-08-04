# MEMORY PROFILING GUIDE FOR BUILDSPOTSMEASURESADVANCED

## Overview
This guide provides comprehensive techniques for profiling memory usage in the `BuildSpotsMeasuresAdvanced` class to identify and optimize memory bottlenecks.

## 1. Built-in Java Memory Monitoring

### Runtime Memory Statistics
The `BuildSpotsMeasuresAdvanced` class now includes built-in memory logging:

```java
private void logMemoryUsage(String stage) {
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
}
```

### Memory Leak Detection
```java
private void checkForMemoryLeaks() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // Check if memory usage is growing abnormally
    if (usedMemory > runtime.maxMemory() * 0.8) {
        System.err.println("WARNING: High memory usage detected!");
        System.err.println("Consider reducing batch size or enabling more aggressive GC");
    }
}
```

### Object Counters
The class tracks key object creation:
- `totalImagesProcessed`: Number of images loaded
- `totalTransformedImagesCreated`: Number of transformed images created
- `totalCursorsCreated`: Number of cursors created
- `compressedMasks.size()`: Number of cached compressed masks

## 2. JVM Memory Profiling Tools

### VisualVM (Free)
```bash
# Download VisualVM from: https://visualvm.github.io/
# Connect to your Java process
# Monitor memory usage in real-time
# Features:
# - Real-time memory monitoring
# - Thread analysis
# - CPU profiling
# - Memory heap analysis
```

### JProfiler (Commercial)
```bash
# Professional memory profiler
# Features:
# - Detailed object allocation tracking
# - Memory leak detection
# - Performance analysis
# - Database monitoring
# Download from: https://www.ej-technologies.com/products/jprofiler/overview.html
```

### YourKit (Commercial)
```bash
# Another professional profiler
# Features:
# - Excellent memory leak detection
# - CPU profiling
# - Thread analysis
# Download from: https://www.yourkit.com/
```

## 3. Command Line Memory Monitoring

### JConsole
```bash
# Start JConsole
jconsole

# Connect to your Java process
# Monitor:
# - Memory usage
# - GC activity
# - Thread states
# - Class loading
```

### JStack for Thread Analysis
```bash
# Get thread dump to see what's consuming memory
jstack <pid>

# Save thread dump to file
jstack <pid> > thread_dump.txt
```

### JMap for Heap Analysis
```bash
# Generate heap dump
jmap -dump:format=b,file=heap_dump.hprof <pid>

# Show heap summary
jmap -histo <pid>
```

## 4. Heap Dump Analysis

### Generate Heap Dumps Programmatically
```java
// Add this method to your class
private void generateHeapDump(String filename) {
    try {
        java.lang.management.ManagementFactory.getMemoryMXBean().dumpHeap(filename, true);
        System.out.println("Heap dump saved to: " + filename);
    } catch (Exception e) {
        System.err.println("Failed to generate heap dump: " + e.getMessage());
    }
}
```

### MAT (Memory Analyzer Tool)
```bash
# Download Eclipse MAT from: https://www.eclipse.org/mat/
# Analyze heap dumps for:
# - Memory leaks
# - Largest objects
# - Object retention paths
# - Duplicate strings
# - Collection analysis
```

## 5. JVM Options for Memory Profiling

### Enable GC Logging
```bash
# Add these JVM options when running your application
-verbose:gc
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCDateStamps
-Xloggc:gc.log
```

### Enable Memory Allocation Tracking
```bash
# Track object allocations
-XX:+TraceClassLoading
-XX:+TraceClassUnloading

# Enable allocation profiling
-XX:+PrintCompilation
-XX:+PrintInlining
```

### Heap Size and GC Options
```bash
# Set heap size
-Xms2g
-Xmx8g

# Use G1GC for better performance
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# Enable GC logging
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
```

## 6. Integration Points in BuildSpotsMeasuresAdvanced

### Memory Profiling at Key Points
The class now includes memory logging at critical points:

1. **Before Batch Processing**: Log memory before each batch
2. **After Batch Processing**: Log memory after each batch
3. **Final Summary**: Complete memory profiling summary

### Enable Memory Profiling
Add to `BuildSeriesOptions.java`:
```java
public boolean enableMemoryProfiling = false;
```

### Usage
```java
// Enable memory profiling
options.enableMemoryProfiling = true;

// Run your analysis
BuildSpotsMeasuresAdvanced analyzer = new BuildSpotsMeasuresAdvanced(options);
analyzer.analyzeExperiment(experiment);
```

## 7. Key Areas to Monitor

### Image Processing Objects
- **IcyBufferedImage**: Source images loaded from files
- **Transformed Images**: `transformToMeasureArea` and `transformToDetectFly`
- **Cursors**: `IcyBufferedImageCursor` objects for pixel access

### Memory-Intensive Operations
- **Image Loading**: Each 270KB image becomes ~2-3MB in memory
- **Image Transformation**: Creates additional copies of images
- **Cursor Creation**: Creates access objects for each transformed image
- **Compressed Mask Caching**: Stores mask coordinates for reuse

### Batch Processing Memory
- **Concurrent Processing**: Multiple images in memory simultaneously
- **GC Between Batches**: Critical for memory cleanup
- **Batch Size**: Affects peak memory usage

## 8. Memory Optimization Strategies

### Reduce Object Creation
```java
// Reuse objects where possible
private final IcyBufferedImageCursor cursorPool = new LinkedBlockingQueue<>();

// Use object pooling for expensive objects
private IcyBufferedImageCursor getCursorFromPool(IcyBufferedImage image) {
    // Implementation
}
```

### Optimize Batch Sizes
```java
// Adaptive batch sizing based on memory
adaptiveBatchSizer.updateBatchSize(memoryMonitor.getMemoryUsagePercent());
```

### Force Garbage Collection
```java
// Between batches (like original)
System.gc();

// When memory pressure is high
if (memoryMonitor.getMemoryUsagePercent() > 80.0) {
    System.gc();
    Thread.yield();
}
```

## 9. Recommended Profiling Workflow

### Step 1: Enable Built-in Monitoring
```java
options.enableMemoryProfiling = true;
```

### Step 2: Run with VisualVM
1. Start your application
2. Connect VisualVM to the process
3. Monitor memory usage in real-time
4. Look for memory growth patterns

### Step 3: Generate Heap Dumps
```java
// At peak memory usage
generateHeapDump("peak_memory.hprof");
```

### Step 4: Analyze with MAT
1. Open heap dump in MAT
2. Look for largest objects
3. Check for memory leaks
4. Analyze object retention paths

### Step 5: Optimize Based on Findings
- Reduce object creation
- Implement object pooling
- Adjust batch sizes
- Optimize GC frequency

## 10. Memory Profiling Checklist

### Before Profiling
- [ ] Set appropriate heap size (-Xmx)
- [ ] Enable GC logging
- [ ] Prepare test dataset
- [ ] Set up monitoring tools

### During Profiling
- [ ] Monitor memory usage patterns
- [ ] Check for memory leaks
- [ ] Analyze GC behavior
- [ ] Track object creation rates

### After Profiling
- [ ] Generate heap dumps at peak usage
- [ ] Analyze with MAT
- [ ] Identify optimization opportunities
- [ ] Implement memory optimizations

## 11. Common Memory Issues and Solutions

### High Memory Usage
**Symptoms**: Memory usage > 80% of heap
**Solutions**:
- Reduce batch size
- Enable more frequent GC
- Implement object pooling
- Optimize image loading

### Memory Leaks
**Symptoms**: Memory usage grows over time
**Solutions**:
- Check for unclosed resources
- Verify proper cleanup in finally blocks
- Use weak references for caches
- Monitor object retention

### GC Pressure
**Symptoms**: Frequent GC, poor performance
**Solutions**:
- Increase heap size
- Optimize object creation
- Use appropriate GC algorithm
- Reduce object lifetime

## 12. Performance vs Memory Trade-offs

### Current Performance
- **BuildSpotsMeasures**: 13.0s, 6.1GB memory
- **BuildSpotsMeasuresAdvanced**: 13.4s, 6.8GB memory
- **Overhead**: 3% slower, 11% more memory

### Optimization Targets
- Reduce memory overhead to < 5%
- Maintain performance within 5% of original
- Improve memory efficiency for large datasets

## 13. Advanced Profiling Techniques

### Custom Memory Meters
```java
// Track specific object types
private final AtomicLong totalImageMemory = new AtomicLong(0);
private final AtomicLong totalCursorMemory = new AtomicLong(0);

// Update in processing methods
totalImageMemory.addAndGet(estimateImageMemory(image));
```

### Memory Pressure Monitoring
```java
// Monitor memory pressure in real-time
if (memoryMonitor.getMemoryUsagePercent() > 90) {
    // Take corrective action
    reduceBatchSize();
    forceGC();
    pauseProcessing();
}
```

### Profiling in Production
```java
// Lightweight production monitoring
if (options.enableProductionMonitoring) {
    logMemoryUsage("Production Check");
    checkForMemoryLeaks();
}
```

## 14. Tools Comparison

| Tool | Cost | Features | Best For |
|------|------|----------|----------|
| **VisualVM** | Free | Basic monitoring, heap analysis | Initial profiling |
| **JProfiler** | Commercial | Advanced profiling, leak detection | Detailed analysis |
| **YourKit** | Commercial | Comprehensive profiling | Production monitoring |
| **MAT** | Free | Heap dump analysis | Memory leak detection |
| **JConsole** | Free | Basic monitoring | Quick checks |

## 15. Conclusion

Memory profiling is essential for optimizing `BuildSpotsMeasuresAdvanced`. Use the built-in monitoring first, then progress to professional tools for detailed analysis. Focus on:

1. **Object Creation**: Minimize expensive object creation
2. **Memory Cleanup**: Ensure proper resource cleanup
3. **Batch Optimization**: Balance memory usage with performance
4. **GC Tuning**: Optimize garbage collection behavior

The goal is to reduce the 11% memory overhead while maintaining the 3% performance penalty, achieving near-parity with the original `BuildSpotsMeasures` while providing the benefits of modular architecture. 