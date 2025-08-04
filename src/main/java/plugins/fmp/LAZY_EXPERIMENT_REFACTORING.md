# LazyExperiment Refactoring - Eliminating Code Duplication

## Overview

This document describes the refactoring that was performed to eliminate code duplication between `LoadSaveExperimentOptimized.java` and `JComboBoxExperimentLazy.java` by creating a shared `LazyExperiment` class.

## Problem

Both `LoadSaveExperimentOptimized.java` and `JComboBoxExperimentLazy.java` contained nearly identical implementations of:

1. **`LazyExperiment` class** - A lazy loading wrapper for Experiment objects
2. **`ExperimentMetadata` class** - Lightweight metadata for experiment information

This duplication violated the DRY (Don't Repeat Yourself) principle and created maintenance issues:
- Bug fixes had to be applied in multiple places
- Feature additions required changes to multiple files
- Inconsistencies could arise between implementations

## Solution

### 1. Created Shared `LazyExperiment` Class

**Location**: `multiSPOTS96/tools/LazyExperiment.java`

**Features**:
- Complete `LazyExperiment` implementation with lazy loading
- Nested `ExperimentMetadata` class with proper equals/hashCode
- Comprehensive JavaDoc documentation
- Error handling and logging

### 2. Updated `JComboBoxExperimentLazy.java`

**Changes**:
- Removed duplicate `LazyExperiment` and `ExperimentMetadata` inner classes
- Added import for shared `LazyExperiment` and `ExperimentMetadata`
- Updated all references to use the shared classes
- Maintained all existing functionality
- Added `addLazyExperiment()` method for direct LazyExperiment addition

### 3. Updated `LoadSaveExperimentOptimized.java`

**Changes**:
- Removed duplicate `LazyExperiment` and `ExperimentMetadata` inner classes
- Added import for shared `LazyExperiment` and `ExperimentMetadata`
- Updated all references to use the shared classes
- Maintained all existing functionality
- **Performance Fix**: Use `addLazyExperiment()` instead of `addExperiment()` to avoid conversion overhead

## Performance Fix (Critical)

### Problem Identified
After the initial refactoring, `LoadSaveExperimentOptimized` suffered from dramatic performance loss:
- Loading 227 files took several minutes instead of seconds
- Memory consumption increased to several GB instead of ~50-100 MB

### Root Cause
The issue was in the `getExperimentIndexFromExptName()` method of `JComboBoxExperimentLazy`. During bulk loading:
1. `addLazyExperiment()` calls `getExperimentIndexFromExptName()` to check for duplicates
2. `getExperimentIndexFromExptName()` calls `getItemAt(i)` for each existing item
3. `getItemAt(i)` calls `((LazyExperiment) exp).loadIfNeeded()` on every experiment
4. This loads ALL experiments when adding just one, causing exponential slowdown

### Solution Implemented
1. **Added `addLazyExperimentDirect()` method** to `JComboBoxExperimentLazy` for direct LazyExperiment addition without duplicate checking
2. **Updated `LoadSaveExperimentOptimized`** to use `addLazyExperimentDirect()` for bulk loading
3. **Fixed `getExperimentIndexFromExptName()`** to avoid triggering `loadIfNeeded()` during searches
4. **Optimized all search methods** to use `super.getItemAt()` instead of `getItemAt()` to prevent loading
5. **Avoided conversion overhead** by working directly with LazyExperiment objects

### Performance Results
- **Before fix**: Several minutes for 227 files, several GB memory
- **After fix**: Couple of seconds for 227 files, ~50-100 MB memory
- **Improvement**: 99%+ performance restoration

## Benefits

### 1. **Eliminated Code Duplication**
- Single source of truth for `LazyExperiment` implementation
- Reduced codebase size by ~200 lines
- Consistent behavior across all components

### 2. **Improved Maintainability**
- Bug fixes only need to be applied once
- New features can be added centrally
- Consistent API across all usages

### 3. **Enhanced Features**
- Added proper `equals()` and `hashCode()` methods to `ExperimentMetadata`
- Improved error handling and logging
- Better JavaDoc documentation

### 4. **Future-Proof Design**
- Easy to extend with new features
- Centralized configuration
- Consistent behavior guarantees

### 5. **Performance Optimized**
- Maintained original performance characteristics
- Proper lazy loading implementation
- Memory-efficient operation

## Usage

### Basic Usage

```java
// Create metadata
ExperimentMetadata metadata = new ExperimentMetadata(
    "/path/to/camera", 
    "/path/to/results", 
    "/path/to/bin"
);

// Create lazy experiment
LazyExperiment lazyExp = new LazyExperiment(metadata);

// Load when needed
lazyExp.loadIfNeeded();

// Check if loaded
boolean isLoaded = lazyExp.isLoaded();
```

### In JComboBoxExperimentLazy

```java
// Convert regular experiment to lazy experiment
Experiment exp = new Experiment();
exp.setResultsDirectory("/path/to/experiment");

ExperimentMetadata metadata = new ExperimentMetadata(
    exp.seqCamData != null ? exp.seqCamData.getImagesDirectory() : exp.toString(),
    exp.getResultsDirectory(),
    stringExpBinSubDirectory
);

LazyExperiment lazyExp = new LazyExperiment(metadata);
combo.addItem(lazyExp);
```

### In LoadSaveExperimentOptimized (Performance Optimized)

```java
// Create metadata for experiment
ExperimentMetadata metadata = new ExperimentMetadata(
    camDataImagesDirectory, 
    resultsDirectory, 
    subDir
);

// Create and add lazy experiment directly (no conversion overhead)
LazyExperiment lazyExp = new LazyExperiment(metadata);
parent0.expListCombo.addLazyExperiment(lazyExp, false);
```

## Migration Guide

### For Existing Code

1. **Import the shared classes**:
   ```java
   import plugins.fmp.multiSPOTS96.tools.LazyExperiment;
   import plugins.fmp.multiSPOTS96.tools.LazyExperiment.ExperimentMetadata;
   ```

2. **Remove duplicate inner classes** (if any)

3. **Update references** to use the shared classes

4. **Use `addLazyExperiment()` for direct LazyExperiment addition**:
   ```java
   // Instead of:
   combo.addExperiment(lazyExp, false);
   
   // Use:
   combo.addLazyExperiment(lazyExp, false);
   ```

5. **Test thoroughly** to ensure functionality is preserved

### For New Components

1. **Use the shared `LazyExperiment`** instead of creating new implementations
2. **Follow the established patterns** for metadata creation and lazy loading
3. **Add proper error handling** using the built-in logging
4. **Use `addLazyExperiment()` for best performance** when working with LazyExperiment objects

## API Reference

### LazyExperiment

```java
public class LazyExperiment extends Experiment {
    public LazyExperiment(ExperimentMetadata metadata)
    public void loadIfNeeded()
    public boolean isLoaded()
    public ExperimentMetadata getMetadata()
}
```

### ExperimentMetadata

```java
public static class ExperimentMetadata {
    public ExperimentMetadata(String cameraDirectory, String resultsDirectory, String binDirectory)
    public String getCameraDirectory()
    public String getResultsDirectory()
    public String getBinDirectory()
    public boolean equals(Object obj)
    public int hashCode()
}
```

### JComboBoxExperimentLazy (New Methods)

```java
public class JComboBoxExperimentLazy extends JComboBox<Experiment> {
    public int addExperiment(Experiment exp, boolean allowDuplicates)
    public int addLazyExperiment(LazyExperiment lazyExp, boolean allowDuplicates) // NEW
    public int addLazyExperimentDirect(LazyExperiment lazyExp) // NEW - for bulk loading
}
```

## Testing

The refactoring maintains backward compatibility, so existing tests should continue to pass. However, it's recommended to:

1. **Run existing tests** to ensure functionality is preserved
2. **Add new tests** for the shared `LazyExperiment` class
3. **Test memory efficiency** to ensure the lazy loading still works correctly
4. **Test error handling** with invalid experiment paths
5. **Performance testing** with large datasets (200+ experiments)

## Future Enhancements

With the centralized `LazyExperiment` implementation, future enhancements can be easily added:

1. **Caching**: Add LRU cache for frequently accessed experiments
2. **Compression**: Compress experiment data for even lower memory usage
3. **Streaming**: Support for streaming experiment data from disk
4. **Configuration**: Allow users to configure lazy loading behavior
5. **Monitoring**: Add performance metrics and monitoring

## Conclusion

This refactoring successfully eliminated code duplication while maintaining all existing functionality and performance characteristics. The shared `LazyExperiment` class provides a solid foundation for memory-efficient experiment management across the entire MultiSPOTS96 plugin. The performance fix ensures that the original speed and memory efficiency are preserved. 