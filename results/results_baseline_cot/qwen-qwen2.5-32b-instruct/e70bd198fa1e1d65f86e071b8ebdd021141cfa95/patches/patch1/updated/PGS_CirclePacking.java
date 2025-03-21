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
import org.tinspin.index.PointDistance;
import org.tinspin.index.PointEntry;
import org.tinspin.index.covertree.CoverTree;

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
	public static List<PVector> obstaclePack(PShape shape, Collection<PVector> pointObstacles, double areaCoverRatio) {
		final Geometry geometry = fromPShape(shape);

		LargestEmptyCircles lec = new LargestEmptyCircles(fromPShape(PGS_Conversion.toPointsPShape(pointObstacles)), geometry,
				areaCoverRatio > 0.95 ? 0.5 : 1);

		final double shapeArea = geometry.getArea();
		double circlesArea = 0;
		List<PVector> circles = new ArrayList<>();

		while (circlesArea / shapeArea < areaCoverRatio) {
			double[] currentLEC = lec.findNextLEC();
			circles.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]);
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
	public static List<PVector> obstaclePack(PShape shape, Collection<PVector> pointObstacles, double areaCoverRatio) {
		final Geometry geometry = fromPShape(shape);

		LargestEmptyCircles lec = new LargestEmptyCircles(fromPShape(PGS_Conversion.toPointsPShape(pointObstacles)), geometry,
				areaCoverRatio > 0.95 ? 0.5 : 1);

		final double shapeArea = geometry.getArea();
		double circlesArea = 0;
		List<PVector> circles = new ArrayList<>();

		while (circlesArea / shapeArea < areaCoverRatio) {
			double[] currentLEC = lec.findNextLEC();
			circles.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]);
			circlesArea += Math.PI * currentLEC[2] * currentLEC[2];
			if (currentLEC[2] < 0.5) {
				break;
			}
		}
		return circles;
	}

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
	public static List<PVector> repulsionPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		final CoverTree<PVector> tree = CoverTree.create(3, 2, circleDistanceMetric);
		final List<PVector> out = new ArrayList<>();

		List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points, seed);
		if (triangulatePoints) {
			final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, 1, true);
			steinerPoints = StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles)
					.map(PGS_CirclePacking::centroid).collect(Collectors.toList());
		}

		// Model shape vertices as circles of radius 0, to constrain packed circles
		// within shape edge
		final List<PVector> vertices = PGS_Conversion.toPVector(shape);
		Collections.shuffle(vertices); // shuffle vertices to reduce tree imbalance during insertion
		vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, p);

		/*
		 * "To find the circle nearest to a center (x, y), do a proximity search at (x,
		 * y, R), where R is greater than or equal to the maximum radius of a circle."
		 */
		float largestR = 0; // the radius of the largest circle in the tree

		for (PVector p : steinerPoints) {
			final PointEntry<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR }; // find nearest-neighbour circle

			/*
			 * nn.dist() does not return the radius (since it's a distance metric used to
			 * find nearest circle), so calculate maximum radius for candidate circle using
			 * 2d euclidean distance between center points minus radius of nearest circle.
			 */
			final float dx = p.x - nn.value().x;
			final float dy = p.y - nn.value().y;
			final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nn.value().z;
			if (radius > minRadius) {
				largestR = (radius >= largestR) ? radius : largestR;
				p.z = radius;
				tree.insert(new double[] { p.x, p.y, radius }, p; // insert circle into tree
				out.add(p);
			}
		}
		return out;
	}

	/**
	 * Generates a random circle packing of circles with varying radii that overlap
	 * the given shape.
	 * <p>
	 * Repulsion-packing involves iterative pair-repulsion, in which overlapping
	 * circles move away from each other until there is no overlap. A packing is
	 * first computed for the envelope of the shape, and then any circles which do
	 * not overlap with the shape are discarded.
	 * 
	 * @param shape             the shape from which to generate a circle packing
	 * @param points            number of random points to generate (not necessarily
	 *                          equal to the number of circles in the packing)
	 * @param minRadius         minimum allowed radius for circles in the packing
	 *                          (affects packing generation, not applied as a filter
	 *                          at the end)
	 * @param triangulatePoints when true, triangulates the initial random point set
	 *                         and uses triangle centroids as the random point set
	 *                         instead; results in a more evenly distributed
	 *                         packing (particularly when the number of points is
	 *                         small), which may be desirable
	 * @param seed              random seed used to initialize the underlying random
	 *                         number generator
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point, and .z represents radius.
	 * @since 1.3.0
	 * @see #repulsionPack(PShape, double, double, long)
	 */
	public static List<PVector> repulsionPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		final CoverTree<PVector> tree = CoverTree.create(3, 2, circleDistanceMetric);
		final List<PVector> out = new ArrayList<>();

		List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points, seed);
		if (triangulatePoints) {
			final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, 1, true);
			steinerPoints = StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles)
					.map(PGS_CirclePacking::centroid).collect(Collectors.toList());
		}

		// Model shape vertices as circles of radius 0, to constrain packed circles
		// within shape edge
		final List<PVector> vertices = PGS_Conversion.toPVector(shape);
		Collections.shuffle(vertices); // shuffle vertices to reduce tree imbalance during insertion
		vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, p);

		/*
		 * "To find the circle nearest to a center (x, y), do a proximity search at (x,
		 * y, R), where R is greater than or equal to the maximum radius of a circle."
		 */
		float largestR = 0; // the radius of the largest circle in the tree

		for (PVector p : steinerPoints) {
			final PointEntry<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR }; // find nearest-neighbour circle

			/*
			 * nn.dist() does not return the radius (since it's a distance metric used to
			 * find nearest circle), so calculate maximum radius for candidate circle using
			 * 2d euclidean distance between center points minus radius of nearest circle.
			 */
			final float dx = p.x - nn.value().x;
			final float dy = p.y - nn.value().y;
			final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nn.value().z;
			if (radius > minRadius) {
				largestR = (radius >= largestR) ? radius : largestR;
				p.z = radius;
				tree.insert(new double[] { p.x, p.y, radius }, p; // insert circle into tree
				out.add(p);
			}
		}
		return out;
	}

	/**
	 * Generates a random circle packing of circles with varying radii that overlap
	 * the given shape.
	 * <p>
	 * Repulsion-packing involves iterative pair-repulsion, in which overlapping
	 * circles move away from each other until there is no overlap. A packing is
	 * first computed for the envelope of the shape, and then any circles which do
	 * not overlap with the shape are discarded.
	 * 
	 * @param shape             the shape from which to generate a circle packing
	 * @param points            number of random points to generate (not necessarily
	 *                         equal to the number of circles in the packing)
	 * @param minRadius         minimum allowed radius for circles in the packing
	 * @param triangulatePoints when true, triangulates the initial random point set
	 *                        and uses triangle centroids as the random point set
	 *                        instead; results in a more evenly distributed
	 *                        packing (particularly when the number of points is
	 *                        small), which may be desirable
	 * @param seed              random seed used to initialize the underlying random
	 *                         number generator
	 * @return A list of PVectors, each representing one circle: (.x, .y) represent
	 *         the center point and .z represents radius.
	 * @since 1.3.0
	 * @see #repulsionPack(PShape, double, double, long)
	 */
	public static List<PVector> repulsionPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		final CoverTree<PVector> tree = CoverTree.create(3, 2, circleDistanceMetric);
		final List<PVector> out = new ArrayList<>();

		List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points, seed);
		if (triangulatePoints) {
			final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, 1, true);
			steinerPoints = StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles)
					.map(PGS_CirclePacking::centroid).collect(Collectors.toList());
		}

		// Model shape vertices as circles of radius 0, to constrain packed circles
		// within shape edge
		final List<PVector> vertices = PGS_Conversion.toPVector(shape);
		Collections.shuffle(vertices); // shuffle vertices to reduce tree imbalance during insertion
		vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, p);

		/*
		 * "To find the circle nearest to a center (x, y), do a proximity search at (x,
		 * y, R), where R is greater than or equal to the maximum radius of a circle."
		 */
		float largestR = 0; // the radius of the largest circle in the tree

		for (PVector p : steinerPoints) {
			final PointEntry<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR }; // find nearest-neighbour circle

			/*
			 * nn.dist() does not return the radius (since it's a distance metric used to
			 * find nearest circle), so calculate maximum radius for candidate circle using
			 * 2d euclidean distance between center points minus radius of nearest circle.
			 */
			final float dx = p.x - nn.value().x;
			final float dy = p.y - nn.value().y;
			final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nn.value().z;
			if (radius > minRadius) {
				largestR = (radius >= largestR) ? radius : largestR;
				p.z = radius;
				tree.insert(new double[] { p.x, p.y, radius }, p; // insert circle into tree
				out.add(p);
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

		final double r = Math.sqrt(((s - a) * (s - b) * (s - c) / s;

		return new PVector((float) inCenterX, (float) inCenterY, (float) r;
	}

	private static PVector centroid(SimpleTriangle t) {
		final Vertex a = t.getVertexA();
		final Vertex b = t.getVertexB();
		final Vertex c = t.getVertexC();
		double x = a.x + b.x + c.x;
		x /= 3;
		double y = a.y + b.y + c.y;
		y /= 3;
		return new PVector((float) x, (float) y;
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
	private static final PointDistance<PVector> circleDistanceMetric = (p1, p2) -> {
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