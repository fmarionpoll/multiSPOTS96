package plugins.fmp.multiSPOTS96.experiment.cages;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.w3c.dom.Element;

import icy.util.XMLUtil;

/**
 * Observable version of CageProperties that notifies observers when properties change.
 * This class implements the Observer pattern to allow components to subscribe to
 * property changes, particularly the cageNFlies property.
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public class ObservableCageProperties extends CageProperties {
    
    /**
     * Observer interface for property change notifications.
     */
    public interface PropertyChangeObserver {
        /**
         * Called when a property changes.
         * 
         * @param propertyName the name of the changed property
         * @param oldValue the previous value
         * @param newValue the new value
         * @param source the ObservableCageProperties instance that changed
         */
        void onPropertyChanged(String propertyName, Object oldValue, Object newValue, ObservableCageProperties source);
    }
    
    private final List<PropertyChangeObserver> observers = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();
    
    /**
     * Adds an observer to receive property change notifications.
     * 
     * @param observer the observer to add
     */
    public void addObserver(PropertyChangeObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }
    
    /**
     * Removes an observer from receiving property change notifications.
     * 
     * @param observer the observer to remove
     */
    public void removeObserver(PropertyChangeObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }
    
    /**
     * Notifies all observers of a property change.
     * 
     * @param propertyName the name of the changed property
     * @param oldValue the previous value
     * @param newValue the new value
     */
    private void notifyObservers(String propertyName, Object oldValue, Object newValue) {
        for (PropertyChangeObserver observer : observers) {
            try {
                observer.onPropertyChanged(propertyName, oldValue, newValue, this);
            } catch (Exception e) {
                // Log error but don't break the notification chain
                System.err.println("Error notifying observer: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sets the number of flies in the cage and notifies observers.
     * 
     * @param nFlies the new number of flies
     */
    @Override
    public void setCageNFlies(int nFlies) {
        synchronized (lock) {
            int oldValue = this.cageNFlies;
            super.setCageNFlies(nFlies);
            if (oldValue != nFlies) {
                notifyObservers("cageNFlies", oldValue, nFlies);
            }
        }
    }
    
    /**
     * Sets the selected state and notifies observers.
     * 
     * @param selected the new selected state
     */
    @Override
    public void setSelected(boolean selected) {
        synchronized (lock) {
            int oldValue = this.cageNFlies;
            super.setSelected(selected);
            if (oldValue != this.cageNFlies) {
                notifyObservers("cageNFlies", oldValue, this.cageNFlies);
            }
        }
    }
    
    /**
     * Sets the cage ID and notifies observers.
     * 
     * @param cageID the new cage ID
     */
    @Override
    public void setCageID(int cageID) {
        synchronized (lock) {
            int oldValue = this.cageID;
            super.setCageID(cageID);
            if (oldValue != cageID) {
                notifyObservers("cageID", oldValue, cageID);
            }
        }
    }
    
    /**
     * Sets the cage position and notifies observers.
     * 
     * @param pos the new cage position
     */
    @Override
    public void setCagePosition(int pos) {
        synchronized (lock) {
            int oldValue = this.cagePosition;
            super.setCagePosition(pos);
            if (oldValue != pos) {
                notifyObservers("cagePosition", oldValue, pos);
            }
        }
    }
    
    /**
     * Sets the color and notifies observers.
     * 
     * @param color the new color
     */
    @Override
    public void setColor(Color color) {
        synchronized (lock) {
            Color oldValue = this.color;
            super.setColor(color);
            if (!oldValue.equals(color)) {
                notifyObservers("color", oldValue, color);
            }
        }
    }
    
    /**
     * Sets the array index and notifies observers.
     * 
     * @param arrayIndex the new array index
     */
    @Override
    public void setArrayIndex(int arrayIndex) {
        synchronized (lock) {
            int oldValue = this.arrayIndex;
            super.setArrayIndex(arrayIndex);
            if (oldValue != arrayIndex) {
                notifyObservers("arrayIndex", oldValue, arrayIndex);
            }
        }
    }
    
    /**
     * Sets the array column and notifies observers.
     * 
     * @param arrayColumn the new array column
     */
    @Override
    public void setArrayColumn(int arrayColumn) {
        synchronized (lock) {
            int oldValue = this.arrayColumn;
            super.setArrayColumn(arrayColumn);
            if (oldValue != arrayColumn) {
                notifyObservers("arrayColumn", oldValue, arrayColumn);
            }
        }
    }
    
    /**
     * Sets the array row and notifies observers.
     * 
     * @param arrayRow the new array row
     */
    @Override
    public void setArrayRow(int arrayRow) {
        synchronized (lock) {
            int oldValue = this.arrayRow;
            super.setArrayRow(arrayRow);
            if (oldValue != arrayRow) {
                notifyObservers("arrayRow", oldValue, arrayRow);
            }
        }
    }
    
    /**
     * Sets the fly age and notifies observers.
     * 
     * @param flyAge the new fly age
     */
    @Override
    public void setFlyAge(int flyAge) {
        synchronized (lock) {
            int oldValue = this.flyAge;
            super.setFlyAge(flyAge);
            if (oldValue != flyAge) {
                notifyObservers("flyAge", oldValue, flyAge);
            }
        }
    }
    
    /**
     * Sets the checked state and notifies observers.
     * 
     * @param checked the new checked state
     */
    @Override
    public void setChecked(boolean checked) {
        synchronized (lock) {
            boolean oldValue = this.checked;
            super.setChecked(checked);
            if (oldValue != checked) {
                notifyObservers("checked", oldValue, checked);
            }
        }
    }
    
    /**
     * Sets the comment and notifies observers.
     * 
     * @param comment the new comment
     */
    @Override
    public void setComment(String comment) {
        synchronized (lock) {
            String oldValue = this.comment;
            super.setComment(comment);
            if (!oldValue.equals(comment)) {
                notifyObservers("comment", oldValue, comment);
            }
        }
    }
    
    /**
     * Sets the fly sex and notifies observers.
     * 
     * @param flySex the new fly sex
     */
    @Override
    public void setFlySex(String flySex) {
        synchronized (lock) {
            String oldValue = this.flySex;
            super.setFlySex(flySex);
            if (!oldValue.equals(flySex)) {
                notifyObservers("flySex", oldValue, flySex);
            }
        }
    }
    
    /**
     * Sets the fly strain and notifies observers.
     * 
     * @param flyStrain the new fly strain
     */
    @Override
    public void setFlyStrain(String flyStrain) {
        synchronized (lock) {
            String oldValue = this.flyStrain;
            super.setFlyStrain(flyStrain);
            if (!oldValue.equals(flyStrain)) {
                notifyObservers("flyStrain", oldValue, flyStrain);
            }
        }
    }
    
    /**
     * Sets the cage number string and notifies observers.
     * 
     * @param strCageNumber the new cage number string
     */
    @Override
    public void setStrCageNumber(String strCageNumber) {
        synchronized (lock) {
            String oldValue = this.strCageNumber;
            super.setStrCageNumber(strCageNumber);
            if (!oldValue.equals(strCageNumber)) {
                notifyObservers("strCageNumber", oldValue, strCageNumber);
            }
        }
    }
    
    /**
     * Sets the version and notifies observers.
     * 
     * @param version the new version
     */
    @Override
    public void setVersion(int version) {
        synchronized (lock) {
            int oldValue = this.version;
            super.setVersion(version);
            if (oldValue != version) {
                notifyObservers("version", oldValue, version);
            }
        }
    }
    
    /**
     * Copies properties from another CageProperties object and notifies observers
     * of all changes.
     * 
     * @param propFrom the source properties
     */
    @Override
    public void copy(CageProperties propFrom) {
        synchronized (lock) {
            // Store old values for notification
            int oldCageNFlies = this.cageNFlies;
            int oldCageID = this.cageID;
            int oldCagePosition = this.cagePosition;
            Color oldColor = this.color;
            int oldArrayIndex = this.arrayIndex;
            int oldArrayColumn = this.arrayColumn;
            int oldArrayRow = this.arrayRow;
            int oldFlyAge = this.flyAge;
            String oldComment = this.comment;
            String oldFlySex = this.flySex;
            String oldFlyStrain = this.flyStrain;
            String oldStrCageNumber = this.strCageNumber;
            
            super.copy(propFrom);
            
            // Notify observers of all changes
            if (oldCageNFlies != this.cageNFlies) {
                notifyObservers("cageNFlies", oldCageNFlies, this.cageNFlies);
            }
            if (oldCageID != this.cageID) {
                notifyObservers("cageID", oldCageID, this.cageID);
            }
            if (oldCagePosition != this.cagePosition) {
                notifyObservers("cagePosition", oldCagePosition, this.cagePosition);
            }
            if (!oldColor.equals(this.color)) {
                notifyObservers("color", oldColor, this.color);
            }
            if (oldArrayIndex != this.arrayIndex) {
                notifyObservers("arrayIndex", oldArrayIndex, this.arrayIndex);
            }
            if (oldArrayColumn != this.arrayColumn) {
                notifyObservers("arrayColumn", oldArrayColumn, this.arrayColumn);
            }
            if (oldArrayRow != this.arrayRow) {
                notifyObservers("arrayRow", oldArrayRow, this.arrayRow);
            }
            if (oldFlyAge != this.flyAge) {
                notifyObservers("flyAge", oldFlyAge, this.flyAge);
            }
            if (!oldComment.equals(this.comment)) {
                notifyObservers("comment", oldComment, this.comment);
            }
            if (!oldFlySex.equals(this.flySex)) {
                notifyObservers("flySex", oldFlySex, this.flySex);
            }
            if (!oldFlyStrain.equals(this.flyStrain)) {
                notifyObservers("flyStrain", oldFlyStrain, this.flyStrain);
            }
            if (!oldStrCageNumber.equals(this.strCageNumber)) {
                notifyObservers("strCageNumber", oldStrCageNumber, this.strCageNumber);
            }
        }
    }
    
    /**
     * Gets the number of registered observers.
     * 
     * @return the number of observers
     */
    public int getObserverCount() {
        return observers.size();
    }
    
    /**
     * Clears all observers.
     */
    public void clearObservers() {
        observers.clear();
    }
} 