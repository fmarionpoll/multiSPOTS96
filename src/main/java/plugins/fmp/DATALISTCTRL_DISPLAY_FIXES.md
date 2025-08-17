# DataListCtrl Display Issues - Analysis and Fixes

## Problem Description
The DataListCtrl class in dbWave64 is not displaying data, spikes, or red rectangles when users select different display options. The class is supposed to trap messages that display bitmaps and substitute calls to plot data, display spikes, or display a red rectangle.

## Root Cause Analysis

### 1. Image List Management Issues
- The image list is created with a fixed size but may not be properly synchronized with the actual number of database records
- Image replacement operations may fail silently
- The image list count may not match the cache size

### 2. Empty Bitmap Integration Problems
- The `build_empty_bitmap()` function creates a red rectangle but may not be properly integrated into the image list
- The empty bitmap may not be used consistently across all display modes

### 3. Display Mode Switching Issues
- The display mode switching logic may not properly trigger image updates
- Cache invalidation may not be working correctly

## Proposed Fixes

### Fix 1: Improve Image List Management
- Ensure image list count matches database record count
- Add proper error checking for image replacement operations
- Synchronize image list with cache operations

### Fix 2: Enhance Empty Bitmap Handling
- Ensure empty bitmap is properly created and integrated
- Add debug tracing to track bitmap operations
- Improve error handling in bitmap creation

### Fix 3: Fix Display Mode Switching
- Ensure proper cache invalidation when display mode changes
- Add forced refresh mechanisms
- Improve synchronization between display modes

### Fix 4: Add Debug Tracing
- Add comprehensive debug output to track the display process
- Monitor image list operations
- Track bitmap creation and replacement

## Implementation Plan
1. Fix the `build_empty_bitmap()` function to ensure proper red rectangle creation
2. Improve image list management in `update_cache()`
3. Enhance the `display_empty_wnd()` function
4. Add proper error checking and debug output
5. Test the red rectangle display functionality

## Expected Results
After implementing these fixes, the DataListCtrl should properly display:
- Red rectangles when "Display Nothing" is selected
- Data plots when "Display Data" is selected  
- Spike plots when "Display Spikes" is selected
