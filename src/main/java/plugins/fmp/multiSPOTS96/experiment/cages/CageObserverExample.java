package plugins.fmp.multiSPOTS96.experiment.cages;

import icy.roi.ROI2D;
import icy.roi.ROI2DEllipse;
import java.awt.geom.Ellipse2D;

/**
 * Example class demonstrating how to use observable cage properties.
 * This class shows how to subscribe to changes in the cageNFlies property
 * and other cage properties.
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class CageObserverExample {
    
    /**
     * Example observer that logs property changes.
     */
    public static class CagePropertyLogger implements ObservableCageProperties.PropertyChangeObserver {
        private final String observerName;
        
        public CagePropertyLogger(String observerName) {
            this.observerName = observerName;
        }
        
        @Override
        public void onPropertyChanged(String propertyName, Object oldValue, Object newValue, ObservableCageProperties source) {
            System.out.printf("[%s] Property '%s' changed from %s to %s in cage %d%n", 
                observerName, propertyName, oldValue, newValue, source.getCageID());
            
            // Special handling for cageNFlies changes
            if ("cageNFlies".equals(propertyName)) {
                handleCageNFliesChange((Integer) oldValue, (Integer) newValue, source);
            }
        }
        
        private void handleCageNFliesChange(Integer oldValue, Integer newValue, ObservableCageProperties source) {
            if (newValue > oldValue) {
                System.out.printf("[%s] Flies added to cage %d: %d -> %d%n", 
                    observerName, source.getCageID(), oldValue, newValue);
            } else if (newValue < oldValue) {
                System.out.printf("[%s] Flies removed from cage %d: %d -> %d%n", 
                    observerName, source.getCageID(), oldValue, newValue);
            }
        }
    }
    
    /**
     * Example observer that updates UI components when properties change.
     */
    public static class CageUIUpdater implements ObservableCageProperties.PropertyChangeObserver {
        @Override
        public void onPropertyChanged(String propertyName, Object oldValue, Object newValue, ObservableCageProperties source) {
            switch (propertyName) {
                case "cageNFlies":
                    updateFlyCountDisplay(source.getCageID(), (Integer) newValue);
                    break;
                case "color":
                    updateCageColorDisplay(source.getCageID(), (java.awt.Color) newValue);
                    break;
                case "comment":
                    updateCageCommentDisplay(source.getCageID(), (String) newValue);
                    break;
                default:
                    // Handle other property changes
                    break;
            }
        }
        
        private void updateFlyCountDisplay(int cageID, int flyCount) {
            // Example: Update UI display for fly count
            System.out.printf("UI: Updated fly count display for cage %d to %d%n", cageID, flyCount);
        }
        
        private void updateCageColorDisplay(int cageID, java.awt.Color color) {
            // Example: Update UI display for cage color
            System.out.printf("UI: Updated color display for cage %d to %s%n", cageID, color);
        }
        
        private void updateCageCommentDisplay(int cageID, String comment) {
            // Example: Update UI display for cage comment
            System.out.printf("UI: Updated comment display for cage %d to '%s'%n", cageID, comment);
        }
    }
    
    /**
     * Demonstrates how to create a cage with observable properties and subscribe to changes.
     */
    public static void demonstrateObservableCage() {
        // Create an observable properties object
        ObservableCageProperties observableProps = new ObservableCageProperties();
        observableProps.setCageID(1);
        observableProps.setCagePosition(0);
        observableProps.setCageNFlies(0);
        
        // Create observers
        CagePropertyLogger logger = new CagePropertyLogger("Logger");
        CageUIUpdater uiUpdater = new CageUIUpdater();
        
        // Add observers to the properties
        observableProps.addObserver(logger);
        observableProps.addObserver(uiUpdater);
        
        // Create a simple ROI for the cage
        Ellipse2D ellipse = new Ellipse2D.Double(100, 100, 50, 50);
        ROI2DEllipse roi = new ROI2DEllipse(ellipse);
        roi.setName("Cage_1");
        
        // Create the cage with observable properties
        ModernCage cage = ModernCage.createValidWithObservableProperties(roi, observableProps);
        
        // Demonstrate property changes
        System.out.println("=== Demonstrating Observable Cage Properties ===");
        
        // Change the number of flies
        System.out.println("\n1. Changing cageNFlies from 0 to 3:");
        observableProps.setCageNFlies(3);
        
        // Change the color
        System.out.println("\n2. Changing cage color:");
        observableProps.setColor(java.awt.Color.RED);
        
        // Change the comment
        System.out.println("\n3. Changing cage comment:");
        observableProps.setComment("Experimental cage with 3 flies");
        
        // Change the number of flies again
        System.out.println("\n4. Changing cageNFlies from 3 to 1:");
        observableProps.setCageNFlies(1);
        
        // Remove an observer
        System.out.println("\n5. Removing logger observer and changing properties:");
        observableProps.removeObserver(logger);
        observableProps.setCageNFlies(5);
        observableProps.setComment("Updated comment without logger");
        
        // Show observer count
        System.out.printf("\n6. Current observer count: %d%n", observableProps.getObserverCount());
        
        // Clear all observers
        observableProps.clearObservers();
        System.out.println("\n7. Cleared all observers and made final change:");
        observableProps.setCageNFlies(10);
        System.out.printf("Final observer count: %d%n", observableProps.getObserverCount());
    }
    
    /**
     * Demonstrates how to use the ModernCage class with observable properties.
     */
    public static void demonstrateModernCageWithObservers() {
        // Create observable properties
        ObservableCageProperties observableProps = new ObservableCageProperties();
        observableProps.setCageID(2);
        observableProps.setCagePosition(1);
        
        // Create a simple ROI
        Ellipse2D ellipse = new Ellipse2D.Double(200, 200, 60, 60);
        ROI2DEllipse roi = new ROI2DEllipse(ellipse);
        roi.setName("Cage_2");
        
        // Create the cage
        ModernCage cage = ModernCage.createValidWithObservableProperties(roi, observableProps);
        
        // Create an observer
        CagePropertyLogger cageLogger = new CagePropertyLogger("CageLogger");
        
        // Add observer through the ModernCage class
        boolean observerAdded = cage.addPropertyObserver(cageLogger);
        System.out.printf("Observer added to cage: %b%n", observerAdded);
        
        // Check if cage has observable properties
        System.out.printf("Cage has observable properties: %b%n", cage.hasObservableProperties());
        
        // Get the observable properties
        ObservableCageProperties props = cage.getObservableProperties();
        if (props != null) {
            System.out.println("Making changes to observable properties:");
            props.setCageNFlies(2);
            props.setColor(java.awt.Color.BLUE);
            props.setComment("Modern cage with observers");
        }
        
        // Remove observer
        boolean observerRemoved = cage.removePropertyObserver(cageLogger);
        System.out.printf("Observer removed from cage: %b%n", observerRemoved);
    }
    
    /**
     * Main method to run the demonstration.
     */
    public static void main(String[] args) {
        System.out.println("=== Cage Observer Pattern Demonstration ===\n");
        
        demonstrateObservableCage();
        System.out.println("\n" + "=".repeat(50) + "\n");
        demonstrateModernCageWithObservers();
        
        System.out.println("\n=== Demonstration Complete ===");
    }
} 