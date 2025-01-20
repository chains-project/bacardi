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
import org.tinspin.index.PointDistanceFunction; // Ensure this is the correct import
import org.tinspin.index.PointEntry; // Updated import for PointEntryDist
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

	// ... (other methods remain unchanged)

	private static final PointDistanceFunction circleDistanceMetric = (p1, p2) -> {
		// from https://stackoverflow.com/a/21975136/
		final double dx = p1[0] - p2[0];
		final double dy = p1[1] - p2[1];
		final double dz = p1[2] - p2[2];

		double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
		double absZDifference = Math.abs(dz);
		return euclideanDistance + absZDifference; // negative if inside
	};

	// ... (other methods remain unchanged)

	public static List<PVector> stochasticPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
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
		vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, p));

		/*
		 * "To find the circle nearest to a center (x, y), do a proximity search at (x,
		 * y, R), where R is greater than or equal to the maximum radius of a circle."
		 */
		float largestR = 0; // the radius of the largest circle in the tree

		for (PVector p : steinerPoints) {
			final PointEntry<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR }); // find nearest-neighbour circle

			/*
			 * nn.dist() does not return the radius (since it's a distance metric used to
			 * find nearest circle), so calculate maximum radius for candidate circle using
			 * 2d euclidean distance between center points minus radius of nearest circle.
			 */
			final float dx = p.x - nn.value().x;
			final float dy = p.y - nn.value().y;
			final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nn.value().z);
			if (radius > minRadius) {
				largestR = (radius >= largestR) ? radius : largestR;
				p.z = radius;
				tree.insert(new double[] { p.x, p.y, radius }, p); // insert circle into tree
				out.add(p);
			}
		}
		return out;
	}

	// ... (other methods remain unchanged)

}