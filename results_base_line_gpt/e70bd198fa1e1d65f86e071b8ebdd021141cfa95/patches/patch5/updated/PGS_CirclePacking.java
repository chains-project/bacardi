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
import org.tinspin.index.covertree.CoverTree;
import org.tinspin.index.Index; // Added import
import org.tinspin.index.Index.PointEntry; // Updated import

import micycle.pgs.commons.FrontChainPacker;
import micycle.pgs.commons.LargestEmptyCircles;
import micycle.pgs.commons.RepulsionCirclePack;
import micycle.pgs.commons.TangencyPack;
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

	/*-
	 * Roadmap (see/implement): 'A LINEARIZED CIRCLE PACKING ALGORITHM'? 
	 * 'A note on circle packing' Young Joon AHN.
	 */

	private PGS_CirclePacking() {
	}

	// ... (rest of the methods remain unchanged)

	/**
	 * Generates a random circle packing of circles with varying radii that overlap
	 * the given shape.
	 * <p>
	 * Repulsion-packing involves iterative pair-repulsion, in which overlapping
	 * circles move away from each other until there is no overlap. A packing is
	 * first computed for the envelope of the shape, and then any circles which do
	 * not overlap with the shape are discarded.
	 * 
	 * @param shape     the shape from which to generate a circle packing
	 * @param radiusMin minimum radius of circles in the packing. the radii
	 *                  parameters can be the same.
	 * @param radiusMax maximum radius of circles in the packing. the radii
	 *                  parameters can be the same.
	 * @param seed      for initial circle positions and radii
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 * @since 1.3.0
	 * @see #repulsionPack(PShape, List)
	 */
	public static List<PVector> repulsionPack(PShape shape, double radiusMin, double radiusMax, long seed) {
		final double rMinA = Math.max(1f, Math.min(radiusMin, radiusMax)); // actual min
		final double rMaxA = Math.max(1f, Math.max(radiusMin, radiusMax)); // actual max
		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();

		/*
		 * We want spawn N circles, such that there are enough to (theoretically) cover
		 * the envelope exactly without any overlap, assuming a packing efficiency of
		 * ~85% (close to optimum).
		 */
		double totalArea = e.getArea() * 0.85;
		/*
		 * Average area is not a simple mean since circle area is quadratic with regards
		 * to radius. The actual average area of circles with radii a...b is an integral
		 * of: pi*r^2 dr from r=a to b.
		 */
		double avgCircleArea = ((rMaxA * rMaxA * rMaxA) - (rMinA * rMinA * rMinA));
		avgCircleArea *= (Math.PI / (3 * (rMaxA - rMinA)));
		int n = (int) (totalArea / avgCircleArea);

		List<PVector> points = PGS_PointSet.poissonN(e.getMinX() + rMaxA, e.getMinY() + rMaxA, e.getMaxX() - rMaxA, e.getMaxY() - rMaxA, n,
				seed);
		SplittableRandom r = new SplittableRandom(seed);
		points.forEach(p -> p.z = rMaxA == rMinA ? (float) rMaxA : (float) r.nextDouble(rMinA, rMaxA));

		return repulsionPack(shape, points);
	}

	// ... (rest of the methods remain unchanged)

	private static PVector centroid(SimpleTriangle t) {
		final Vertex a = t.getVertexA();
		final Vertex b = t.getVertexB();
		final Vertex c = t.getVertexC();
		double x = a.x + b.x + c.x;
		x /= 3;
		double y = a.y + b.y + c.y;
		y /= 3;
		return new PVector((float) x, (float) y);
	}

	/**
	 * A streams filter to remove triangulation triangles that share at least one
	 * edge with the shape edge.
	 */
	private static final Predicate<SimpleTriangle> filterBorderTriangles = t -> t.getContainingRegion() != null
			&& !t.getEdgeA().isConstrainedRegionBorder() && !t.getEdgeB().isConstrainedRegionBorder()
			&& !t.getEdgeC().isConstrainedRegionBorder();

}