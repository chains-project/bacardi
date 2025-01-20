package micycle.pgs;

import static micycle.pgs.PGS_Conversion.fromPShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.triangulate.polygon.PolygonTriangulator;
import org.tinfour.common.IConstraint;
import org.tinfour.common.IIncrementalTin;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.PolygonConstraint;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.HilbertSort;
import org.tinfour.utils.TriangleCollector;

import micycle.pgs.PGS.LinearRingIterator;
import micycle.pgs.color.Colors;
import micycle.pgs.commons.Nullable;
import micycle.pgs.commons.PEdge;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Delaunay and earcut triangulation of shapes and point sets.
 * 
 * @author Michael Carleton
 *
 */
public final class PGS_Triangulation {

	private PGS_Triangulation() {
	}

	// ... (other methods remain unchanged)

	/**
	 * Creates a Delaunay triangulation of the shape where additional steiner
	 * points, populated by poisson sampling, are included.
	 * <p>
	 * This method returns the triangulation in its raw form: a
	 * TriangulatedIrregular Network (mesh).
	 * 
	 * @param shape
	 * @param spacing (Minimum) spacing between poisson points
	 * @return Triangulated Irregular Network object (mesh)
	 * @see #poissonTriangulation(PShape, double)
	 */
	public static IIncrementalTin poissonTriangulationMesh(PShape shape, double spacing) {
		final Envelope e = fromPShape(shape).getEnvelopeInternal();

		// Updated method call to match the new method signature
		final List<PVector> poissonPoints = PGS_PointSet.poisson(e.getMinX(), e.getMinY(), 
			e.getMinX() + e.getWidth(), e.getMinY() + e.getHeight(), spacing);

		final IIncrementalTin tin = delaunayTriangulationMesh(shape, poissonPoints, true, 0, false);
		return tin;
	}

	// ... (other methods remain unchanged)
}