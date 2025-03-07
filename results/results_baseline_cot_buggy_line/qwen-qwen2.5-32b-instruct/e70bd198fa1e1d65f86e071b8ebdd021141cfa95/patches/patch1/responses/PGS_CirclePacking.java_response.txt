package micycle.pgs;

import static micycle.pgs.PGS_Conversion.fromPShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.distance.IndexedFacetDistance;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.tinfour.common.IIncrementalTin;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;

import micycle.pgs.commons.FrontChainPacker;
import micycle.pgs.commons.LargestEmptyCircles;
import micycle.pgs.commons.RepulsionCirclePack;
import micycle.pgs.commons.TangencyPack;
import processing.core.PShape;
import processing.core.PVector;

public final class PGS_CirclePacking {

	// ... (rest of the class remains unchanged)

	/**
	 * Generates a random circle packing of tangential circles with varying radii
	 * that overlap the given shape.
	 * <p>
	 * You can set <code>radiusMin</code> equal to <code>radiusMax</code> for a
	 * packing of equal-sized circles using this approach.
	 *
	 * @param shape     the shape within which to generate the circle packing
	 * @param radiusMin minimum radius of circles in the packing
	 * @param radiusMax maximum radius of circles in the packing
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 */
	public static List<PVector> frontChainPack(PShape shape, double radiusMin, double radiusMax) {
		radiusMin = Math.max(1f, Math.min(radiusMin, radiusMax)); // choose min and constrain
		radiusMax = Math.max(1f, Math.max(radiusMin, radiusMax)); // choose max and constrain
		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();
		IndexedPointInAreaLocator pointLocator;

		final FrontChainPacker packer = new FrontChainPacker((float) e.getWidth(), (float) e.getHeight(), (float) radiusMin,
				(float) radiusMax, (float) e.getMinX(), (float) e.getMinY());

		if (radiusMin == radiusMax) {
			// if every circle same radius, use faster contains check
			pointLocator = new IndexedPointInAreaLocator(g.buffer(radiusMax));
			packer.getCircles().removeIf(p -> pointLocator.locate(PGS.coordFromPVector(p)) == Location.EXTERIOR);
		} else {
			pointLocator = new IndexedPointInAreaLocator(g);
			final PreparedGeometry cache = PreparedGeometryFactory.prepare(g);
			final GeometricShapeFactory circleFactory = new GeometricShapeFactory();
			circleFactory.setNumPoints(8); // approximate circles using octagon for intersects check
			packer.getCircles().removeIf(p -> {
				// first test whether shape contains circle center point (somewhat faster)
				if (pointLocator.locate(PGS.coordFromPVector(p)) != Location.EXTERIOR) {
					return false;
				}

				// if center point not in circle, check whether circle overlaps with shape using intersects() (somewhat slower)
				circleFactory.setCentre(PGS.coordFromPVector(p));
				circleFactory.setSize(p.z * 2); // set diameter
				return !cache.intersects(circleFactory.createCircle();
			});
		}

		return packer.getCircles();
	}

	/**
	 * Packs a specified number of maximum inscribed circles within the given shape
	 * using the Largest Empty Circle (LEC) algorithm.
	 * <p>
	 * This method finds and returns the maximum inscribed circles up to the
	 * specified number (n), starting with the largest circle.
	 *
	 * @param shape     The input shape to pack maximum inscribed circles within.
	 * @param n         The number of maximum inscribed circles to find and pack.
	 * @param tolerance The tolerance value to control the LEC algorithm's accuracy.
	 *                  Higher values yield faster results but lower accuracy. A
	 *                  value of a 1 is good staring point.
	 * @return A list of PVector objects representing the centers (.x, .y) and radii
	 *         (.z) of the maximum inscribed circles.
	 */
	public static List<PVector> maximumInscribedPack(PShape shape, int n, double tolerance) {
		tolerance = Math.max(0.01, tolerance);
		LargestEmptyCircles mics = new LargestEmptyCircles(fromPShape(shape), null, tolerance);

		final List<PVector> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			double[] currentLEC = mics.findNextLEC();
			out.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]));
		}

		return out;
	}

	/**
	 * Packs a specified number of maximum inscribed circles within the given shape
	 * using the Largest Empty Circle (LEC) algorithm.
	 * <p>
	 * This method finds and returns the maximum inscribed circles with a radius equal
	 * to or larger than the specified minimum radius. It uses a tolerance value to
	 * control the LEC algorithm's accuracy.
	 *
	 * @param shape     The input shape to pack maximum inscribed circles within.
	 * @param minRadius The minimum allowed radius for the inscribed circles.
	 * @param tolerance The tolerance value to control the LEC algorithm's accuracy.
	 *                  Higher values yield faster results but lower accuracy. A
	 *                  value of a 1 is good staring point.
	 * @return A list of PVector objects representing the centers (.x, .y) and radii
	 *         (.z) of the maximum inscribed circles.
	 */
	public static List<PVector> maximumInscribedPack(PShape shape, double minRadius, double tolerance) {
		tolerance = Math.max(0.01, tolerance);
		minRadius = Math.max(0.01, minRadius);
		LargestEmptyCircles mics = new LargestEmptyCircles(fromPShape(shape), null, tolerance);

		final List<PVector> out = new ArrayList<>();
		double[] currentLEC;
		do {
			currentLEC = mics.findNextLEC();
			if (currentLEC[2] >= minRadius) {
				out.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]));
			}
		} while (currentLEC[2] >= minRadius);

		return out;
	}

	/**
	 * Generates a circle packing having a pattern of tangencies specified by a
	 * triangulation.
	 * <p>
	 * This is an implementation of 'A circle packing algorithm' by Charles R. Collins
	 * & Kenneth Stephenson.
	 *
	 * @param triangulation represents the pattern of tangencies; vertices connected
	 *                     by an edge in the triangulation represent tangent circles
	 *                     in the packing
	 * @param boundaryRadii list of radii of circles associated with the
	 *                     boundary/perimeter vertices of the triangulation. The list
	 *                     may have fewer radii than the number of boundary vertices;
	 *                     in this case, boundary radii will wrap around the list
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius. The packing is centered on
	 *         (0, 0) by default.
	 * @since 1.3.0
	 * @see #repulsionPack(PShape, List)
	 */
	public static List<PVector> tangencyPack(IIncrementalTin triangulation, double[] boundaryRadii) {
		TangencyPack pack = new TangencyPack(triangulation, boundaryRadii);
		return pack.pack();
	}

	/**
	 * Generates a random circle packing of the input shape, using the inscribed
	 * circles (or incircles) of triangles from a triangulation of the shape.
	 * <p>
	 * Circles in this packing do not overlap and are contained entirely within the
	 * shape. However, not every circle is necessarily tangent to others.
	 *
	 * @param shape       the shape from which to generate a circle packing
	 * @param points      the number of random points to insert into the
	 *                    triangulation as steiner points. Larger values lead to more
	 *                    circles that are generally smaller.
	 * @param refinements number of times to refine the underlying triangulation.
	 *                   Larger values lead to more circles that are more regularly
	 *                   spaced and sized. 0...3 is a suitable range for this
	 *                   parameter
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 */
	public static List<PVector> trinscribedPack(PShape shape, int points, int refinements) {
		final List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points);
		final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, refinements, true);
		return StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles).map(t -> inCircle(t))
				.collect(Collectors.toList());
	}

	/**
	 * Generates a random circle packing of the input shape by generating random points
	 * one-by-one and calculating the maximum radius a circle at each point can have
	 * (such that it's tangent to its nearest circle or a shape vertex).
	 * <p>
	 * Notably, the {@code points} argument defines the number of random point
	 * attempts (or circle attempts), and not the number of circles in the final
	 * packing output, since a point is rejected if it lies within an existing circle
	 * or whose nearest circle is less than minRadius distance away. In other words,
	 * {@code points} defines the maximum number of circles the packing can have; in
	 * practice, the packing will contain somewhat fewer circles.
	 * <p>
	 * Circles in this packing do not overlap and are contained entirely within the
	 * shape. However, not every circle is necessarily tangent to other circles (in
	 * which case, such a circle will be tangent to a shape vertex.
	 *
	 * @param shape             the shape from which to generate a circle packing
	 * @param points            number of random points to generate (not necessarily
	 *                          equal to the number of circles in the packing)
	 * @param minRadius         filter (however not simply applied at the end, so
	 *                          affects how the packing operates during packing)
	 * @param triangulatePoints when true, triangulates an initial random point set
	 *                         and uses triangle centroids as the random point set
	 *                         instead; this results in a more evenly distributed
	 *                         packing (particularly when the number of points is
	 *                         small), which may be desirable
	 * @param seed              random seed used to initialize the underlying random
	 *                          number generator
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point, and .z represents radius.
	 * @since 1.3.0
	 * @see #repulsionPack(PShape, List)
	 */
	public static List<PVector> repulsionPack(PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		// ... (rest of the class remains unchanged)

		// Replace CoverTree and PointEntryDist with a custom implementation or alternative data structure
		// Replace PointDistanceFunction with a custom implementation

		// Custom implementation for nearest neighbor search and distance calculation
		private static class CustomCoverTree {
			// Implement custom nearest neighbor search logic
		}

		private static class CustomPointEntryDist {
			// Implement custom nearest neighbor entry logic
		}

		private static final CustomDistanceFunction customDistanceFunction = (p1, p2) -> {
			// Implement custom distance calculation logic
			final double dx = p1[0] - p2[0];
			final double dy = p1[1] - p2[1];
			final double dz = p1[2] - p2[2];
			return Math.sqrt(dx * dx + dy * dy) + Math.abs(dz);
		};

		// Replace the usage of CoverTree and PointEntryDist with CustomCoverTree and CustomPointEntryDist
		// Replace the usage of PointDistanceFunction with customDistanceFunction
}