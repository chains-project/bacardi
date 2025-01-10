package micycle.pgs;

import static micycle.pgs.PGS_Conversion.fromPShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.tinspin.index.DistanceFunction;  
import org.tinspin.index.Entry;  
import org.tinspin.index.covertree.CoverTree;

// The following imports are updated to resolve the missing classes
import org.tinspin.index.PointEntryDist;
import org.tinspin.index.PointDistanceFunction;

import micycle.pgs.commons.LargestEmptyCircles;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Circle packings of shapes, subject to varying constraints and patterns of
 * tangencies.
 * <p>
 * Each method produces a circle packing with different characteristics using a
 * different technique; for this reason input arguments vary across the methods.
 * <p>
 * The output of each method is a list of PVectors, each representing one
 * circle: (.x, .y) represent the center point and .z represents radius.
 * <p>
 * Where applicable, packings will include circles that overlap with the shape,
 * rather than only including those circles whose center point lies inside the
 * shape.
 * 
 * @author Michael Carleton
 * @since 1.1.0
 *
 */
public final class PGS_CirclePacking {

	private PGS_CirclePacking() {
	}

	/**
	 * Packs circles of varying radii within a given shape, whilst respecting
	 * pointal obstacles using the Largest Empty Circle (LEC) algorithm. The method
	 * continues to generate circles until the sum of the areas of the circles
	 * exceeds a specified proportion of the area of the given shape.
	 * 
	 * @param shape          The shape within which circles will be packed. The
	 *                       shape should be in the form of PShape.
	 * @param pointObstacles A collection of PVector points representing obstacles,
	 *                       around which circles are packed. Only points contained
	 *                       within the shape are relevant.
	 * @param areaCoverRatio The target ratio of the total area of the circles to
	 *                       the area of the shape. This parameter should be a
	 *                       double between 0 and 1. Circle generation will stop
	 *                       when this ratio is reached.
	 * @return A list of PVectors, where each PVector represents a circle. The x and
	 *         y components of the PVector represent the center of the circle, and
	 *         the z component represents the radius of the circle.
	 * @since 1.4.0
	 */
	public static List<PVector> obstaclePack(PShape shape, Collection<PVector> pointObstacles, double areaCoverRatio) {
		final Geometry geometry = fromPShape(shape);

		LargestEmptyCircles lec = new LargestEmptyCircles(fromPShape(PGS_Conversion.toPointsPShape(pointObstacles)), geometry,
				areaCoverRatio > 0.95 ? 0.5 : 1);

		final double shapeArea = geometry.getArea();
		double circlesArea = 0;
		List<PVector> circles = new ArrayList<>();

		while (circlesArea / shapeArea < areaCoverRatio) {
			double[] currentLEC = lec.findNextLEC();
			circles.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]));
			circlesArea += Math.PI * currentLEC[2] * currentLEC[2];
			if (currentLEC[2] < 0.5) {
				break;
			}
		}
		return circles;
	}

	/**
	 * Calculate the distance between two points in 3D space, where each point
	 * represents a circle with (x, y, r) coordinates. This custom metric considers
	 * both the Euclidean distance between the centers of the circles and the
	 * absolute difference of their radii.
	 * <p>
	 * The metric is defined as follows: Given two points A and B, representing
	 * circles centered at (x1, y1) and (x2, y2) with radii r1 and r2 respectively,
	 * the distance is calculated as sqrt((x1 - x2)^2 + (y1 - y2)^2) + |r1 - r2|.
	 * <p>
	 * This metric can be used to find the nearest circle to a given center (x, y)
	 * in a proximity search. To perform the search, use a point (x, y, R) where R
	 * is greater than or equal to the maximum radius of a circle in the proximity
	 * structure.
	 *
	 * @param p1 3D point representing the first circle (x1, y1, r1)
	 * @param p2 3D point representing the second circle (x2, y2, r2)
	 * @return the distance between the two points based on the custom metric
	 */
	private static final DistanceFunction<double[]> circleDistanceMetric = new DistanceFunction<double[]>() {
		@Override
		public double distance(double[] p1, double[] p2) {
			final double dx = p1[0] - p2[0];
			final double dy = p1[1] - p2[1];
			final double dz = p1[2] - p2[2];

			double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
			double absZDifference = Math.abs(dz);
			return euclideanDistance + absZDifference; // negative if inside
		}
	};

}