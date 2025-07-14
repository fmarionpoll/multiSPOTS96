package plugins.fmp.multiSPOTS96.tools.ROI2D;

import java.awt.geom.Point2D;
import java.util.List;

import plugins.kernel.roi.roi2d.ROI2DPolygon;

/**
 * Enhanced ROI2DPolygon with additional cage-specific properties. Provides
 * proper encapsulation and validation for cage positioning and selection state.
 * 
 * @author MultiSPOTS96 Team
 * @version 2.0
 */
public class ROI2DPolygonPlus extends ROI2DPolygon {

	// Private fields with proper encapsulation
	private int cageRow = -1;
	private int cageColumn = -1;
	private int cagePosition = -1;
	private boolean selected = false;

	/**
	 * Creates a new ROI2DPolygonPlus with default values.
	 */
	public ROI2DPolygonPlus() {
		super();
	}

	/**
	 * Creates a new ROI2DPolygonPlus with specified cage coordinates.
	 * 
	 * @param cageRow    The row position of the cage
	 * @param cageColumn The column position of the cage
	 * @throws ROI2DValidationException If coordinates are invalid
	 */
	public ROI2DPolygonPlus(int cageRow, int cageColumn) throws ROI2DValidationException {
		super();
		setCageRow(cageRow);
		setCageColumn(cageColumn);
	}

	/**
	 * Creates a new ROI2DPolygonPlus with points and cage coordinates.
	 * 
	 * @param points     The polygon points
	 * @param cageRow    The row position of the cage
	 * @param cageColumn The column position of the cage
	 * @throws ROI2DValidationException If parameters are invalid
	 */
	public ROI2DPolygonPlus(List<Point2D> points, int cageRow, int cageColumn) throws ROI2DValidationException {
		super(validateAndGetPoints(points));
		setCageRow(cageRow);
		setCageColumn(cageColumn);
	}

	/**
	 * Validates points list and returns it if valid.
	 * 
	 * @param points The points to validate
	 * @return The validated points list
	 * @throws ROI2DValidationException If points are invalid
	 */
	private static List<Point2D> validateAndGetPoints(List<Point2D> points) throws ROI2DValidationException {
		ROI2DValidator.validateNotNullOrEmpty(points, "points");
		if (points.size() < ROI2DConstants.Geometry.MIN_POLYGON_POINTS) {
			throw new ROI2DValidationException("points", points.size(),
					String.format("Polygon requires at least %d points, but only %d provided",
							ROI2DConstants.Geometry.MIN_POLYGON_POINTS, points.size()));
		}
		return points;
	}

	/**
	 * Gets the cage row position.
	 * 
	 * @return The cage row position, or -1 if not set
	 */
	public int getCageRow() {
		return cageRow;
	}

	/**
	 * Sets the cage row position.
	 * 
	 * @param cageRow The cage row position (must be non-negative or -1 for unset)
	 * @throws ROI2DValidationException If the row position is invalid
	 */
	public void setCageRow(int cageRow) throws ROI2DValidationException {
		if (cageRow < -1) {
			throw new ROI2DValidationException("cageRow", cageRow, "Row must be non-negative or -1 for unset");
		}
		this.cageRow = cageRow;
	}

	/**
	 * Gets the cage column position.
	 * 
	 * @return The cage column position, or -1 if not set
	 */
	public int getCageColumn() {
		return cageColumn;
	}

	/**
	 * Sets the cage column position.
	 * 
	 * @param cageColumn The cage column position (must be non-negative or -1 for
	 *                   unset)
	 * @throws ROI2DValidationException If the column position is invalid
	 */
	public void setCageColumn(int cageColumn) throws ROI2DValidationException {
		if (cageColumn < -1) {
			throw new ROI2DValidationException("cageColumn", cageColumn, "Column must be non-negative or -1 for unset");
		}
		this.cageColumn = cageColumn;
	}

	/**
	 * Gets the cage position index.
	 * 
	 * @return The cage position index, or -1 if not set
	 */
	public int getCagePosition() {
		return cagePosition;
	}

	/**
	 * Sets the cage position index.
	 * 
	 * @param cagePosition The cage position index (must be non-negative or -1 for
	 *                     unset)
	 * @throws ROI2DValidationException If the position index is invalid
	 */
	public void setCagePosition(int cagePosition) throws ROI2DValidationException {
		if (cagePosition < -1) {
			throw new ROI2DValidationException("cagePosition", cagePosition,
					"Position must be non-negative or -1 for unset");
		}
		this.cagePosition = cagePosition;
	}

	/**
	 * Checks if this cage is selected.
	 * 
	 * @return true if the cage is selected, false otherwise
	 */
	public boolean getSelected() {
		return selected;
	}

	/**
	 * Sets the selection state of this cage.
	 * 
	 * @param selected true to select the cage, false to deselect
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Checks if this cage has valid coordinates set.
	 * 
	 * @return true if both row and column are non-negative, false otherwise
	 */
	public boolean hasValidCoordinates() {
		return cageRow >= 0 && cageColumn >= 0;
	}

	/**
	 * Checks if this cage has a valid position index set.
	 * 
	 * @return true if the position is non-negative, false otherwise
	 */
	public boolean hasValidPosition() {
		return cagePosition >= 0;
	}

	/**
	 * Resets the cage coordinates and position to unset state.
	 */
	public void resetCageProperties() {
		this.cageRow = -1;
		this.cageColumn = -1;
		this.cagePosition = -1;
		this.selected = false;
	}

	/**
	 * Creates a formatted string representation of the cage properties.
	 * 
	 * @return A string describing the cage properties
	 */
	public String getCagePropertiesString() {
		return String.format("Cage[row=%d, col=%d, pos=%d, selected=%s]", cageRow, cageColumn, cagePosition, selected);
	}

	/**
	 * Creates a copy of this ROI2DPolygonPlus with the same cage properties.
	 * 
	 * @return A copy of this polygon with the same cage properties
	 */
	@Override
	public ROI2DPolygonPlus getCopy() {
		ROI2DPolygonPlus copy = new ROI2DPolygonPlus();
		copy.setPolygon2D(getPolygon2D());
		copy.setName(getName());
		copy.setColor(getColor());
		copy.setStroke(getStroke());

		// Copy cage properties
		try {
			copy.setCageRow(this.cageRow);
			copy.setCageColumn(this.cageColumn);
			copy.setCagePosition(this.cagePosition);
			copy.setSelected(this.selected);
		} catch (ROI2DValidationException e) {
			// This should not happen as we're copying valid values
			throw new RuntimeException("Failed to copy cage properties", e);
		}

		return copy;
	}

	@Override
	public String toString() {
		return String.format("%s - %s", super.toString(), getCagePropertiesString());
	}
}
