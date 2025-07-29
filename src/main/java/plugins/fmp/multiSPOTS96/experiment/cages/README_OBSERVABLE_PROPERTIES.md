# Observable Cage Properties

This document explains how to use the observable properties feature in the ModernCage class, which allows observers to subscribe to changes in cage properties, particularly the `cageNFlies` variable.

## Overview

The observable properties feature implements the Observer pattern to allow components to subscribe to property changes in cage objects. This is particularly useful for:

- Updating UI components when cage properties change
- Logging property changes for debugging
- Synchronizing data across different parts of the application
- Real-time monitoring of cage states

## Key Classes

### ObservableCageProperties
A subclass of `CageProperties` that adds observer functionality. It notifies observers whenever any property changes, including:
- `cageNFlies` - Number of flies in the cage
- `cageID` - Cage identifier
- `color` - Cage color
- `comment` - Cage comment
- And all other properties

### ModernCage
The main cage class that supports observable properties through:
- `addPropertyObserver()` - Add an observer to the cage
- `removePropertyObserver()` - Remove an observer from the cage
- `hasObservableProperties()` - Check if the cage has observable properties
- `getObservableProperties()` - Get the observable properties object

## Usage Examples

### Basic Observer Implementation

```java
// Create an observer that implements the PropertyChangeObserver interface
public class MyCageObserver implements ObservableCageProperties.PropertyChangeObserver {
    @Override
    public void onPropertyChanged(String propertyName, Object oldValue, Object newValue, ObservableCageProperties source) {
        if ("cageNFlies".equals(propertyName)) {
            System.out.println("Fly count changed from " + oldValue + " to " + newValue);
        }
    }
}
```

### Creating a Cage with Observable Properties

```java
// Create observable properties
ObservableCageProperties props = new ObservableCageProperties();
props.setCageID(1);
props.setCageNFlies(0);

// Create ROI for the cage
Ellipse2D ellipse = new Ellipse2D.Double(100, 100, 50, 50);
ROI2DEllipse roi = new ROI2DEllipse(ellipse);
roi.setName("Cage_1");

// Create the cage with observable properties
ModernCage cage = ModernCage.createValidWithObservableProperties(roi, props);

// Add an observer
MyCageObserver observer = new MyCageObserver();
cage.addPropertyObserver(observer);

// Now when you change properties, observers will be notified
props.setCageNFlies(3); // This will trigger the observer
```

### UI Update Observer Example

```java
public class CageUIUpdater implements ObservableCageProperties.PropertyChangeObserver {
    @Override
    public void onPropertyChanged(String propertyName, Object oldValue, Object newValue, ObservableCageProperties source) {
        switch (propertyName) {
            case "cageNFlies":
                updateFlyCountDisplay(source.getCageID(), (Integer) newValue);
                break;
            case "color":
                updateCageColorDisplay(source.getCageID(), (Color) newValue);
                break;
            case "comment":
                updateCageCommentDisplay(source.getCageID(), (String) newValue);
                break;
        }
    }
    
    private void updateFlyCountDisplay(int cageID, int flyCount) {
        // Update UI component for fly count
        // This could update a label, table, or other UI element
    }
}
```

### Multiple Observers

```java
// You can add multiple observers to the same cage
ObservableCageProperties props = new ObservableCageProperties();
ModernCage cage = ModernCage.createValidWithObservableProperties(roi, props);

// Add different types of observers
cage.addPropertyObserver(new CageUIUpdater());
cage.addPropertyObserver(new CagePropertyLogger("Logger"));
cage.addPropertyObserver(new DataSynchronizer());

// All observers will be notified when properties change
props.setCageNFlies(5);
```

## Thread Safety

The observable properties implementation is thread-safe:
- Uses `CopyOnWriteArrayList` for observer management
- Synchronized property setters to prevent race conditions
- Exception handling in observer notifications to prevent one observer from breaking others

## Performance Considerations

- Observers are notified synchronously on the calling thread
- Use `CopyOnWriteArrayList` for thread safety, which has some overhead
- Consider using background threads for heavy observer processing
- Remove observers when they're no longer needed to prevent memory leaks

## Migration from Regular CageProperties

To migrate existing code to use observable properties:

1. Replace `CageProperties` with `ObservableCageProperties`
2. Use `ModernCage.createValidWithObservableProperties()` instead of `createValid()`
3. Add observers as needed
4. The rest of your code can remain the same since `ObservableCageProperties` extends `CageProperties`

## Example: Complete Usage

See `CageObserverExample.java` for a complete demonstration of the observable properties feature, including:
- Basic observer implementation
- UI update observers
- Multiple observers
- Observer management (add/remove/clear)

## API Reference

### ObservableCageProperties Methods

- `addObserver(PropertyChangeObserver observer)` - Add an observer
- `removeObserver(PropertyChangeObserver observer)` - Remove an observer
- `getObserverCount()` - Get the number of registered observers
- `clearObservers()` - Remove all observers
- All setter methods notify observers when values change

### ModernCage Methods

- `addPropertyObserver(PropertyChangeObserver observer)` - Add observer to cage properties
- `removePropertyObserver(PropertyChangeObserver observer)` - Remove observer from cage properties
- `hasObservableProperties()` - Check if cage has observable properties
- `getObservableProperties()` - Get the observable properties object
- `createValidWithObservableProperties(ROI2D, ObservableCageProperties)` - Create cage with observable properties

### PropertyChangeObserver Interface

```java
public interface PropertyChangeObserver {
    void onPropertyChanged(String propertyName, Object oldValue, Object newValue, ObservableCageProperties source);
}
```

## Best Practices

1. **Always remove observers** when they're no longer needed to prevent memory leaks
2. **Handle exceptions** in observer implementations to prevent breaking other observers
3. **Use meaningful observer names** for debugging
4. **Consider performance** when adding many observers or doing heavy processing in observers
5. **Test observer behavior** thoroughly, especially in multi-threaded scenarios 