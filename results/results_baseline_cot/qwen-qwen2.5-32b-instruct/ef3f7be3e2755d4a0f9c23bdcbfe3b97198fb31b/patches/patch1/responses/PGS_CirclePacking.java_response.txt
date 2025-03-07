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
import org.tinspin.index.CoverTree;
import org.tinspin.index.DistanceFunction;
import org.tinspin.index.Entry;
import org.tinspin.index.QueryResult;

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
	public static List<PVector> stochasticPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints) {
		return stochasticPack(shape, points, minRadius, triangulatePoints, System.nanoTime());
	}

	/**
	 * Generates a random circle packing within the input shape, using the inscribed
	 * circles (or incircles) of triangles from a triangulation of the shape.
	 * 
	 * @param shape             the shape from which to generate a circle packing
	 * @param points            number of random points to generate (not necessarily
	 *                          equal to the number of circles in the packing)
	 * @param minRadius         minimum allowed radius for circles in the packing
	 * @param triangulatePoints when true, triangulates the initial random point set
	 *                         and uses triangle centroids as the random point set
	 *                         instead; this results in a packing that covers the
	 *                         shape more evenly (particularly when points is
	 *                         small), which is sometimes desirable
	 * @param seed              random seed used to initialize the underlying random
	 *                          number generator
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 * @since 1.3.0
	 * @see #repulsionPack(PShape, List)
	 */
	public static List<PVector> stochasticPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		final CoverTree<PVector> tree = new CoverTree<>(3, 2, circleDistanceMetric);
		final List<PVector> out = new ArrayList<>();

		List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points, seed);
		if (triangulatePoints) {
			final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, 1, true);
			steinerPoints = StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles).map(PGS_CirclePacking::centroid).collect(Collectors.toList());
		}

		// Model shape vertices as circles of radius 0, to constrain packed circles
		// within the shape
		final List<PVector> vertices = PGS_Conversion.toPVector(shape);
		Collections.shuffle(vertices); // shuffle vertices to reduce tree imbalance during insertion
		vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, p);

		final double largestR = 0; // the radius of the largest circle in the tree

		for (PVector p : steinerPoints) {
			final QueryResult<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR }); // find nearest neighbor circle
			final PVector nearestCircle = nn.getEntry().getValue();

			// Calculate the maximum radius for the candidate circle
			final float dx = p.x - nearestCircle.x;
			final float dy = p.y - nearestCircle.y;
			final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nearestCircle.z);
			if (radius > minRadius) {
				largestR = (radius >= largestR) ? radius : largestR;
				p.z = radius;
				tree.insert(new double[] { p.x, p.y, radius }, p); // insert circle into tree
				out.add(p);
			}
		}
		return out;
	}

	/**
	 * Generates a random circle packing of the input shape by generating random
	 * points one-by-one and calculating the maximum radius a circle at each point
	 * can have (such that it's tangent to its nearest circle or a shape vertex).
	 * 
	 * @param shape             the shape from which to generate a circle packing
	 * @param points            number of random points to generate (this is not the
	 *                          number of circles in the packing)
	 * @param minRadius         filter (however not simply applied at the end, so
	 *                          affects how the packing operates during packing)
	 * @param triangulatePoints when true, triangulates an initial random point set
	 *                         and uses triangle centroids as the random point set
	 *                         instead; this results in a packing that covers the
	 *                         shape more evenly (particularly when points is
	 *                         small), which is sometimes desirable
	 * @param seed              random seed used to initialize the underlying random
	 *                          number generator
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 */
	public static List<PVector> repulsionPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();

		float radiusMin = Float.MAX_VALUE;
		float radiusMax = Float.MIN_VALUE;
		for (PVector circle : circles) {
			radiusMax = Math.max(1f, Math.max(radiusMax, circle.z));
			radiusMin = Math.max(1f, Math.min(radiusMin, circle.z);
		}

		final RepulsionCirclePack packer = new RepulsionCirclePack(circles, e.getMinX() + radiusMin, e.getMaxX() - radiusMin,
				e.getMinY() + radiusMin, e.getMaxY() - radiusMin, false);

		final List<PVector> packing = packer.getPacking(); // packing result

		IndexedPointInAreaLocator pointLocator;
		if (radiusMin == radiusMax) {
			// if every circle same radius, use faster contains check
			pointLocator = new IndexedPointInAreaLocator(g.buffer(radiusMax);
			packing.removeIf(p -> pointLocator.locate(PGS.coordFromPVector(p)) == Location.EXTERIOR);
		} else {
			pointLocator = new IndexedPointInAreaLocator(g);
			IndexedFacetDistance distIndex = new IndexedFacetDistance(g);
			packing.removeIf(p -> {
				// first test whether shape contains circle center point (somewhat faster)
				if (pointLocator.locate(PGS.coordFromPVector(p)) != Location.EXTERIOR) {
					return false;
				}
				return distIndex.distance(PGS.pointFromPVector(p)) > p.z * 0.5;
			});
		}

		return packing;
	}

	/**
	 * Generates a tiled circle packing consisting of equal-sized circles arranged
	 * in a square lattice (or grid) bounded by the input shape.
	 * 
	 * @param shape    the shape from which to generate a circle packing
	 * @param diameter diameter of every circle in the packing
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 * @see #hexLatticePack(PShape, double)
	 */
	public static List<PVector> squareLatticePack(PShape shape, double diameter) {
		diameter = Math.max(diameter, 0.1);
		final double radius = diameter / 2;

		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();
		// buffer the geometry to use InAreaLocator to test circles for overlap (this
		// works because all circles have the same diameter)
		final IndexedPointInAreaLocator pointLocator = new IndexedPointInAreaLocator(g.buffer(radius * 0.95);
		final double w = e.getWidth() + diameter + e.getMinX();
		final double h = e.getHeight() + diameter + e.getMinY();

		final List<PVector> out = new ArrayList<>();

		for (double x = e.getMinX(); x < w; x += diameter) {
			for (double y = e.getMinY(); y < h; y += diameter) {
				if (pointLocator.locate(new Coordinate(x, y)) != Location.EXTERIOR) {
					out.add(new PVector((float) x, (float) y, (float) radius);
				}
			}
		}
		return out;
	}

	/**
	 * Generates a tiled circle packing consisting of equal-sized circles arranged
	 * in a hexagonal lattice bounded by the input shape.
	 * 
	 * @param shape    the shape from which to generate a circle packing
	 * @param diameter diameter of every circle in the packing
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 * @see #squareLatticePack(PShape, double)
	 */
	public static List<PVector> hexLatticePack(PShape shape, double diameter) {
		diameter = Math.max(diameter, 0.1);
		final double radius = diameter / 2d;

		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();
		/*
		 * Buffer the geometry to use InAreaLocator to test circles for overlap (this
		 * works because all circles have the same diameter).
		 */
		final IndexedPointInAreaLocator pointLocator = new IndexedPointInAreaLocator(g.buffer(radius * 0.95);
		final double w = e.getWidth() + diameter + e.getMinX();
		final double h = e.getHeight() + diameter + e.getMinY();

		final List<PVector> out = new ArrayList<>();

		final double z = radius * Math.sqrt(3); // hex distance between successive columns
		double offset = 0;
		for (double x = e.getMinX(); x < w; x += z) {
			offset = (offset == radius) ? 0 : radius;
			for (double y = e.getMinY() - offset; y < h; y += diameter) {
				if (pointLocator.locate(new Coordinate(x, y)) != Location.EXTERIOR) {
					out.add(new PVector((float) x, (float) y, (float) radius);
				}
			}
		}
		return out;
	}

	/**
	 * Computes the incircle of a triangle; the largest circle contained in a given
	 * triangle.
	 * 
	 * @param t triangle
	 * @return PVector, where x & y represent incenter coordinates, and z represents
	 *         incircle radius.
	 */
	private static PVector inCircle(SimpleTriangle t) {
		final double a = t.getEdgeA().getLength();
		final double b = t.getEdgeB().getLength();
		final double c = t.getEdgeC().getLength();

		double inCenterX = t.getVertexA().x * a + t.getVertexB().x * b + t.getVertexC().x * c;
		inCenterX /= (a + b + c);
		double inCenterY = t.getVertexA().y * a + t.getVertexB().y * b + t.getVertexC().y * c;
		inCenterY /= (a + b + c);

		final double s = (a + b + c) / 2; // semiPerimeter

		final double r = Math.sqrt(((s - a) * (s - b) * (s - c)) / s);

		return new PVector((float) inCenterX, (float) inCenterY, (float) r);
	}

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
	 * Calculate the distance between two points in 3D space, where each point
	 * represents a circle with (x, y, r) coordinates. This custom metric considers
	 * both the Euclidean distance between the centers of the circles and the
	 * absolute difference of their radii.
	 * 
	 * @param p1 3D point representing the first circle (x1, y1, r1)
	 * @param p2 3D point representing the second circle (x2, y2, r2)
	 * @return the distance between the two points based on the custom metric
	 */
	private static final DistanceFunction circleDistanceMetric = (p1, p2) -> {
		// from https://stackoverflow.com/a/21975136/
		final double dx = p1[0] - p2[0];
		final double dy = p1[1] - p2[1];
		final double dz = p1[2] - p2[2];

		double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
		double absZDifference = Math.abs(dz);
		return euclideanDistance + absZDifference; // negative if inside
	};

	/**
	 * A streams filter to remove triangulation triangles that share at least one edge
	 * with the shape edge.
	 */
	private static final Predicate<SimpleTriangle> filterBorderTriangles = t -> t.getContainingRegion() != null
			&& !t.getEdgeA().isConstrainedRegionBorder() && !t.getEdgeB().isConstrainedRegionBorder()
			&& !t.getEdgeC().isConstrainedRegionBorder();
}