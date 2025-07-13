package plugins.fmp.multiSPOTS96.experiment.cages;

import java.util.List;

/**
 * Immutable data class containing comprehensive cages array information.
 * 
 * @author MultiSPOTS96
 * @version 2.3.3
 */
public final class CagesArrayInfo {
    private final int totalCages;
    private final int validCages;
    private final int activeCages;
    private final int cagesWithSpots;
    private final int gridSize;
    private final List<String> cageNames;
    private final boolean hasTimeIntervals;
    private final int timeIntervalsCount;
    
    private CagesArrayInfo(Builder builder) {
        this.totalCages = builder.totalCages;
        this.validCages = builder.validCages;
        this.activeCages = builder.activeCages;
        this.cagesWithSpots = builder.cagesWithSpots;
        this.gridSize = builder.gridSize;
        this.cageNames = builder.cageNames != null ? List.copyOf(builder.cageNames) : List.of();
        this.hasTimeIntervals = builder.hasTimeIntervals;
        this.timeIntervalsCount = builder.timeIntervalsCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public int getTotalCages() { return totalCages; }
    public int getValidCages() { return validCages; }
    public int getActiveCages() { return activeCages; }
    public int getCagesWithSpots() { return cagesWithSpots; }
    public int getGridSize() { return gridSize; }
    public List<String> getCageNames() { return cageNames; }
    public boolean hasTimeIntervals() { return hasTimeIntervals; }
    public int getTimeIntervalsCount() { return timeIntervalsCount; }
    
    // Computed properties
    public boolean hasValidCages() {
        return validCages > 0;
    }
    
    public double getValidCagesRatio() {
        return totalCages > 0 ? (double) validCages / totalCages : 0.0;
    }
    
    public double getActiveCagesRatio() {
        return totalCages > 0 ? (double) activeCages / totalCages : 0.0;
    }
    
    public double getSpotsCompletionRatio() {
        return totalCages > 0 ? (double) cagesWithSpots / totalCages : 0.0;
    }
    
    public boolean isGridComplete() {
        return totalCages == gridSize;
    }
    
    public boolean isReadyForAnalysis() {
        return activeCages > 0 && validCages > 0;
    }
    
    public boolean hasAnySpots() {
        return cagesWithSpots > 0;
    }
    
    @Override
    public String toString() {
        return String.format("CagesArrayInfo{total=%d, valid=%d, active=%d, withSpots=%d, grid=%d, timeIntervals=%d}", 
                           totalCages, validCages, activeCages, cagesWithSpots, gridSize, timeIntervalsCount);
    }
    
    public static class Builder {
        private int totalCages;
        private int validCages;
        private int activeCages;
        private int cagesWithSpots;
        private int gridSize;
        private List<String> cageNames;
        private boolean hasTimeIntervals;
        private int timeIntervalsCount;
        
        public Builder totalCages(int totalCages) {
            this.totalCages = totalCages;
            return this;
        }
        
        public Builder validCages(int validCages) {
            this.validCages = validCages;
            return this;
        }
        
        public Builder activeCages(int activeCages) {
            this.activeCages = activeCages;
            return this;
        }
        
        public Builder cagesWithSpots(int cagesWithSpots) {
            this.cagesWithSpots = cagesWithSpots;
            return this;
        }
        
        public Builder gridSize(int gridSize) {
            this.gridSize = gridSize;
            return this;
        }
        
        public Builder cageNames(List<String> cageNames) {
            this.cageNames = cageNames;
            return this;
        }
        
        public Builder hasTimeIntervals(boolean hasTimeIntervals) {
            this.hasTimeIntervals = hasTimeIntervals;
            return this;
        }
        
        public Builder timeIntervalsCount(int timeIntervalsCount) {
            this.timeIntervalsCount = timeIntervalsCount;
            return this;
        }
        
        public CagesArrayInfo build() {
            return new CagesArrayInfo(this);
        }
    }
} 