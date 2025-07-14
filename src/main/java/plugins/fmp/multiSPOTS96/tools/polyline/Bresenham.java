package plugins.fmp.multiSPOTS96.tools.polyline;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bresenham's line drawing algorithm implementation for pixel-perfect line generation.
 * This class provides static methods to generate pixel coordinates along lines between
 * two points or along polylines.
 * 
 * <p>The algorithm is based on Bresenham's line algorithm which efficiently determines
 * the points of an n-dimensional raster that should be selected to form a close
 * approximation to a straight line between two endpoints.</p>
 * 
 * <p>References:
 * <ul>
 * <li>Michael ABRASH (1992) "The Good, the Bad and the Run-sliced", Dr Dobb's Journal 194: 171-191</li>
 * <li><a href="https://www.javatpoint.com/computer-graphics-bresenhams-line-algorithm">JavaTpoint Tutorial</a></li>
 * <li><a href="https://gist.github.com/0x414c/3bbd1122a50e4be229ce">Implementation by Alexey Gorishny</a></li>
 * <li><a href="https://github.com/ArminJo/STMF3-Discovery-Demos/blob/master/lib/graphics/src/thickLine.cpp">Fat line implementation</a></li>
 * <li><a href="http://members.chello.at/~easyfilter/bresenham.html">Bresenham description</a></li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * // Get pixels for a line from (0,0) to (10,5)
 * ArrayList&lt;int[]&gt; pixels = Bresenham.getPixelsBetween2Points(0, 0, 10, 5);
 * 
 * // Get pixels along a polyline defined by points
 * ArrayList&lt;Point2D&gt; points = Arrays.asList(new Point2D.Double(0,0), new Point2D.Double(5,3), new Point2D.Double(10,1));
 * ArrayList&lt;int[]&gt; linePixels = Bresenham.getPixelsAlongLineFromROI2D(points);
 * </pre>
 * 
 * @author MultiSPOTS96
 * @see java.awt.geom.Point2D
 */
public final class Bresenham {
    
    /** Logger for this class */
    private static final Logger LOGGER = Logger.getLogger(Bresenham.class.getName());
    
    /** Array index for x-coordinate */
    private static final int X_INDEX = 0;
    
    /** Array index for y-coordinate */
    private static final int Y_INDEX = 1;
    
    /** Bit shift value for doubling (equivalent to multiplication by 2) */
    private static final int DOUBLE_SHIFT = 1;
    
    /** Minimum list size for polyline operations */
    private static final int MIN_POLYLINE_SIZE = 2;
    
    /** Maximum coordinate value to prevent overflow */
    private static final int MAX_COORDINATE = Integer.MAX_VALUE / 4;
    
    /** Minimum coordinate value to prevent overflow */
    private static final int MIN_COORDINATE = Integer.MIN_VALUE / 4;
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Bresenham() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates pixel coordinates along a line between two points using Bresenham's algorithm.
     * 
     * <p>This method implements the classic Bresenham line algorithm which efficiently
     * determines which pixels should be selected to form a close approximation to a
     * straight line between two endpoints.</p>
     * 
     * @param x1 x-coordinate of the first point
     * @param y1 y-coordinate of the first point
     * @param x2 x-coordinate of the second point
     * @param y2 y-coordinate of the second point
     * @return an ArrayList of int arrays, where each array contains [x, y] coordinates
     * @throws IllegalArgumentException if coordinates are out of valid range
     */
    public static ArrayList<int[]> getPixelsBetween2Points(int x1, int y1, int x2, int y2) {
        // Validate input coordinates
        validateCoordinates(x1, y1, x2, y2);
        
        try {
            ArrayList<int[]> linePixels = new ArrayList<>();
            
            // Calculate deltas and increments
            int deltaX = Math.abs(x2 - x1);
            int deltaY = Math.abs(y2 - y1);
            
            int incrementX = (x2 >= x1) ? 1 : -1;
            int incrementY = (y2 >= y1) ? 1 : -1;
            
            int currentX = x1;
            int currentY = y1;
            
            // Determine primary direction based on which delta is larger
            if (deltaX >= deltaY) {
                // X is the primary direction
                generateLineXPrimary(linePixels, currentX, currentY, x2, deltaX, deltaY, incrementX, incrementY);
            } else {
                // Y is the primary direction
                generateLineYPrimary(linePixels, currentX, currentY, y2, deltaX, deltaY, incrementX, incrementY);
            }
            
            return linePixels;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating line pixels", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generates pixel coordinates along a polyline defined by a list of points.
     * 
     * <p>This method connects consecutive points in the list using straight lines
     * generated by Bresenham's algorithm. The resulting pixel list represents
     * the complete path along the polyline.</p>
     * 
     * @param pointsList list of Point2D objects defining the polyline vertices
     * @return an ArrayList of int arrays, where each array contains [x, y] coordinates
     * @throws IllegalArgumentException if pointsList is null, empty, or has fewer than 2 points
     */
    public static ArrayList<int[]> getPixelsAlongLineFromROI2D(ArrayList<Point2D> pointsList) {
        if (pointsList == null) {
            throw new IllegalArgumentException("Points list cannot be null");
        }
        
        if (pointsList.size() < MIN_POLYLINE_SIZE) {
            throw new IllegalArgumentException("Points list must contain at least 2 points, got: " + pointsList.size());
        }
        
        try {
            ArrayList<int[]> allLinePixels = new ArrayList<>();
            
            // Process each line segment
            for (int i = 1; i < pointsList.size(); i++) {
                Point2D currentPoint = pointsList.get(i - 1);
                Point2D nextPoint = pointsList.get(i);
                
                if (currentPoint == null || nextPoint == null) {
                    LOGGER.warning("Skipping null point at index " + (currentPoint == null ? i - 1 : i));
                    continue;
                }
                
                // Generate pixels for this line segment
                ArrayList<int[]> segmentPixels = getPixelsBetween2Points(
                    (int) currentPoint.getX(),
                    (int) currentPoint.getY(),
                    (int) nextPoint.getX(),
                    (int) nextPoint.getY()
                );
                
                // Add segment pixels to the result
                allLinePixels.addAll(segmentPixels);
            }
            
            return allLinePixels;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating polyline pixels", e);
            return new ArrayList<>();
        }
    }

    /**
     * Convenience method that accepts a list of Point2D objects (not necessarily ArrayList).
     * 
     * @param pointsList list of Point2D objects defining the polyline vertices
     * @return an ArrayList of int arrays, where each array contains [x, y] coordinates
     * @throws IllegalArgumentException if pointsList is null, empty, or has fewer than 2 points
     */
    public static ArrayList<int[]> getPixelsAlongPolyline(java.util.List<Point2D> pointsList) {
        if (pointsList == null) {
            throw new IllegalArgumentException("Points list cannot be null");
        }
        
        // Convert to ArrayList if necessary
        ArrayList<Point2D> arrayList = pointsList instanceof ArrayList ? 
            (ArrayList<Point2D>) pointsList : new ArrayList<>(pointsList);
        
        return getPixelsAlongLineFromROI2D(arrayList);
    }

    /**
     * Gets the total number of pixels that would be generated for a line between two points.
     * This method is useful for pre-allocating collections or estimating memory requirements.
     * 
     * @param x1 x-coordinate of the first point
     * @param y1 y-coordinate of the first point
     * @param x2 x-coordinate of the second point
     * @param y2 y-coordinate of the second point
     * @return the number of pixels in the line
     * @throws IllegalArgumentException if coordinates are out of valid range
     */
    public static int getLinePixelCount(int x1, int y1, int x2, int y2) {
        validateCoordinates(x1, y1, x2, y2);
        
        int deltaX = Math.abs(x2 - x1);
        int deltaY = Math.abs(y2 - y1);
        
        // The number of pixels is the maximum of the two deltas plus 1
        return Math.max(deltaX, deltaY) + 1;
    }

    // Private helper methods
    
    /**
     * Validates that coordinates are within acceptable ranges.
     */
    private static void validateCoordinates(int x1, int y1, int x2, int y2) {
        if (x1 < MIN_COORDINATE || x1 > MAX_COORDINATE) {
            throw new IllegalArgumentException("x1 coordinate out of range: " + x1);
        }
        if (y1 < MIN_COORDINATE || y1 > MAX_COORDINATE) {
            throw new IllegalArgumentException("y1 coordinate out of range: " + y1);
        }
        if (x2 < MIN_COORDINATE || x2 > MAX_COORDINATE) {
            throw new IllegalArgumentException("x2 coordinate out of range: " + x2);
        }
        if (y2 < MIN_COORDINATE || y2 > MAX_COORDINATE) {
            throw new IllegalArgumentException("y2 coordinate out of range: " + y2);
        }
    }
    
    /**
     * Generates line pixels when X is the primary direction (deltaX >= deltaY).
     */
    private static void generateLineXPrimary(ArrayList<int[]> linePixels, int currentX, int currentY, 
                                           int targetX, int deltaX, int deltaY, int incrementX, int incrementY) {
        int doubleDeltaY = deltaY << DOUBLE_SHIFT;
        int balance = doubleDeltaY - deltaX;
        int doubleDeltaX = deltaX << DOUBLE_SHIFT;
        
        while (currentX != targetX) {
            linePixels.add(new int[]{currentX, currentY});
            
            if (balance >= 0) {
                currentY += incrementY;
                balance -= doubleDeltaX;
            }
            
            balance += doubleDeltaY;
            currentX += incrementX;
        }
        
        // Add the final point
        linePixels.add(new int[]{currentX, currentY});
    }
    
    /**
     * Generates line pixels when Y is the primary direction (deltaY > deltaX).
     */
    private static void generateLineYPrimary(ArrayList<int[]> linePixels, int currentX, int currentY, 
                                           int targetY, int deltaX, int deltaY, int incrementX, int incrementY) {
        int doubleDeltaX = deltaX << DOUBLE_SHIFT;
        int balance = doubleDeltaX - deltaY;
        int doubleDeltaY = deltaY << DOUBLE_SHIFT;
        
        while (currentY != targetY) {
            linePixels.add(new int[]{currentX, currentY});
            
            if (balance >= 0) {
                currentX += incrementX;
                balance -= doubleDeltaY;
            }
            
            balance += doubleDeltaX;
            currentY += incrementY;
        }
        
        // Add the final point
        linePixels.add(new int[]{currentX, currentY});
    }
}
