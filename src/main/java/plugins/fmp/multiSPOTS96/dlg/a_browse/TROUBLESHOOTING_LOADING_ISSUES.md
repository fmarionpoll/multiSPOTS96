# Troubleshooting Guide: LoadSaveExperimentOptimized Loading Issues

## Problem Description
The optimized version of `LoadSaveExperiment` gets stuck when loading more than 10 files, preventing the loading process from completing.

## Root Causes Identified

### 1. Thread Safety Issues
**Problem**: The original optimized version was trying to update UI components from background threads, causing thread conflicts and deadlocks.

**Solution**: 
- All UI updates now use `SwingUtilities.invokeLater()` to ensure they run on the Event Dispatch Thread (EDT)
- Reduced concurrent processing to avoid thread conflicts

### 2. Excessive Concurrency
**Problem**: Processing too many files concurrently (4 threads) was causing resource contention and memory pressure.

**Solution**:
- Reduced `MAX_CONCURRENT_THREADS` from 4 to 2
- Reduced `BATCH_SIZE` from 10 to 5 files per batch
- Added sequential processing within batches to prevent conflicts

### 3. Memory Management Issues
**Problem**: Large batches and concurrent processing were causing memory pressure and garbage collection issues.

**Solution**:
- Reduced cache size from 100 to 50 entries
- Added explicit garbage collection after each batch
- Added small delays between file processing to prevent UI freezing

### 4. Progress Frame Thread Conflicts
**Problem**: Progress frame updates were happening from background threads, causing UI thread conflicts.

**Solution**:
- All progress frame updates now use `SwingUtilities.invokeLater()`
- Progress updates are properly synchronized with the EDT

## Key Improvements Made

### 1. Thread Safety Enhancements
```java
// Before: Direct UI updates from background threads
progressFrame.setMessage("Processing...");

// After: Proper EDT synchronization
SwingUtilities.invokeLater(() -> {
    progressFrame.setMessage("Processing...");
});
```

### 2. Reduced Concurrency
```java
// Before: High concurrency settings
private static final int BATCH_SIZE = 10;
private static final int MAX_CONCURRENT_THREADS = 4;

// After: Conservative settings for stability
private static final int BATCH_SIZE = 5;
private static final int MAX_CONCURRENT_THREADS = 2;
```

### 3. Sequential Processing
```java
// Before: Concurrent batch processing
List<CompletableFuture<Void>> futures = new ArrayList<>();
for (String fileName : batch) {
    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        processSingleFile(fileName, subDir, false);
    }, executorService);
    futures.add(future);
}
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

// After: Sequential processing with small delays
for (int j = i; j < endIndex; j++) {
    final String fileName = selectedNames.get(j);
    processSingleFile(fileName, subDir, false);
    processingCount.incrementAndGet();
    
    // Small delay to prevent UI freezing
    try {
        Thread.sleep(10);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
    }
}
```

### 4. Improved Error Handling
```java
// Before: Synchronous file processing with potential blocking
CompletableFuture<ExperimentDirectories> future = CompletableFuture.supplyAsync(() -> {
    return createExperimentDirectories(fileName, subDir);
}, executorService);
ExperimentDirectories expDirectories = future.get(); // Could block indefinitely

// After: Synchronous processing to avoid deadlocks
ExperimentDirectories expDirectories = createExperimentDirectories(fileName, subDir);
```

## Testing Recommendations

### 1. Gradual Testing
- Start with 5 files and gradually increase to 10, 15, 20
- Monitor memory usage and UI responsiveness
- Check for any error messages in the console

### 2. Performance Monitoring
```java
// Add timing measurements
long startTime = System.currentTimeMillis();
// ... processing ...
long endTime = System.currentTimeMillis();
LOGGER.info("Processed " + fileCount + " files in " + (endTime - startTime) + "ms");
```

### 3. Memory Monitoring
```java
// Monitor memory usage
Runtime runtime = Runtime.getRuntime();
long usedMemory = runtime.totalMemory() - runtime.freeMemory();
LOGGER.info("Memory used: " + (usedMemory / 1024 / 1024) + " MB");
```

## Configuration Tuning

### For Better Performance (if stable)
```java
private static final int BATCH_SIZE = 8; // Increase if stable
private static final int MAX_CONCURRENT_THREADS = 3; // Increase if stable
private static final int CACHE_SIZE = 75; // Increase if memory allows
```

### For Maximum Stability
```java
private static final int BATCH_SIZE = 3; // Very conservative
private static final int MAX_CONCURRENT_THREADS = 1; // Single thread
private static final int CACHE_SIZE = 25; // Minimal cache
```

## Debugging Steps

### 1. Enable Detailed Logging
```java
// Add to your logging configuration
LOGGER.setLevel(Level.FINE);
```

### 2. Monitor Thread States
```java
// Add thread monitoring
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
long[] threadIds = threadBean.getAllThreadIds();
for (long threadId : threadIds) {
    ThreadInfo threadInfo = threadBean.getThreadInfo(threadId);
    if (threadInfo.getThreadState() == Thread.State.BLOCKED) {
        LOGGER.warning("Blocked thread: " + threadInfo.getThreadName());
    }
}
```

### 3. Check for Deadlocks
```java
// Add deadlock detection
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
long[] deadlockedThreads = threadBean.findDeadlockedThreads();
if (deadlockedThreads != null) {
    LOGGER.severe("Deadlock detected!");
    for (long threadId : deadlockedThreads) {
        ThreadInfo threadInfo = threadBean.getThreadInfo(threadId);
        LOGGER.severe("Deadlocked thread: " + threadInfo.getThreadName());
    }
}
```

## Fallback Strategy

If the optimized version still has issues, consider using a hybrid approach:

1. **Use original sequential processing** for the first few files to establish UI state
2. **Switch to optimized processing** for remaining files
3. **Implement a timeout mechanism** to fall back to sequential processing if optimized processing takes too long

```java
// Hybrid approach example
if (selectedNames.size() <= 5) {
    // Use original sequential processing for small batches
    processFilesSequentiallyOriginal(progressFrame);
} else {
    // Use optimized processing for larger batches
    processFilesSequentially(progressFrame);
}
```

## Conclusion

The main issues were related to thread safety and excessive concurrency. The improved version addresses these by:

1. **Ensuring all UI updates happen on the EDT**
2. **Reducing concurrent processing to prevent resource contention**
3. **Adding proper error handling and timeouts**
4. **Implementing sequential processing within batches**

These changes should resolve the loading issues while maintaining performance improvements over the original implementation. 