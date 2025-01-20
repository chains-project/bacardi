package micycle.pgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.interfaces.VertexColoringAlgorithm.Coloring;
import org.jgrapht.alg.spanning.GreedyMultiplicativeSpanner;
import org.jgrapht.alg.util.NeighborCache;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.coverage.CoverageSimplifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.noding.SegmentString;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.tinfour.common.IConstraint;
import org.tinfour.common.IIncrementalTin;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.utils.TriangleCollector;
import org.tinspin.index.kdtree.KDTree;
import org.tinspin.index.PointIndex; // Ensure this import is correct
import it.unimi.dsi.util.XoRoShiRo128PlusRandomGenerator;
import micycle.pgs.color.Colors;
import micycle.pgs.commons.AreaMerge;
import micycle.pgs.commons.IncrementalTinDual;
import micycle.pgs.commons.PEdge;
import micycle.pgs.commons.PMesh;
import micycle.pgs.commons.RLFColoring;
import micycle.pgs.commons.SpiralQuadrangulation;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Mesh generation (excluding triangulation) and processing.
 * <p>
 * Many of the methods within this class process an existing Delaunay
 * triangulation; you may first generate such a triangulation from a shape using
 * the
 * {@link PGS_Triangulation#delaunayTriangulationMesh(PShape, Collection, boolean, int, boolean)
 * delaunayTriangulationMesh()} method.
 * 
 * @author Michael Carleton
 * @since 1.2.0
 */
public class PGS_Meshing {

	private PGS_Meshing() {
	}

	// Other methods remain unchanged...

	/**
	 * Generates a shape consisting of polygonal faces of a <i>Gabriel graph</i>. A
	 * Gabriel graph is obtained by removing each edge E from a triangulation if a
	 * vertex lies within a circle of diameter = length(E), centered on the midpoint
	 * of E.
	 * <p>
	 * In practice this is a way to tessellate a shape into polygons (with the
	 * resulting tessellation being reminiscent of shattering the shape as if it
	 * were glass).
	 * <p>
	 * Note that this method processes a Delaunay triangulation. Process a shape
	 * using
	 * {@link PGS_Triangulation#delaunayTriangulationMesh(PShape, Collection, boolean, int, boolean)
	 * delaunayTriangulationMesh()} first and then feed it to this method.
	 * 
	 * @param triangulation     a triangulation mesh
	 * @param preservePerimeter whether to retain/preserve edges on the perimeter
	 *                          even if they should be removed according to the
	 *                          gabriel condition
	 * @return a GROUP PShape where each child shape is a single face
	 * @since 1.1.0
	 * @see #urquhartFaces(IIncrementalTin, boolean)
	 */
	public static PShape gabrielFaces(final IIncrementalTin triangulation, final boolean preservePerimeter) {
		final HashSet<IQuadEdge> edges = new HashSet<>();
		final HashSet<Vertex> vertices = new HashSet<>();

		final boolean notConstrained = triangulation.getConstraints().isEmpty();
		TriangleCollector.visitSimpleTriangles(triangulation, t -> {
			final IConstraint constraint = t.getContainingRegion();
			if (notConstrained || (constraint != null && constraint.definesConstrainedRegion())) {
				edges.add(t.getEdgeA().getBaseReference()); // add edge to set
				edges.add(t.getEdgeB().getBaseReference()); // add edge to set
				edges.add(t.getEdgeC().getBaseReference()); // add edge to set
				vertices.add(t.getVertexA());
				vertices.add(t.getVertexB());
				vertices.add(t.getVertexC());
			}
		});

		final PointIndex<Vertex> tree = KDTree.create(2, (p1, p2) -> {
			final double deltaX = p1[0] - p2[0];
			final double deltaY = p1[1] - p2[1];
			return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		});
		vertices.forEach(v -> tree.insert(new double[] { v.x, v.y }, v));

		final HashSet<IQuadEdge> nonGabrielEdges = new HashSet<>(); // base references to edges that should be removed
		edges.forEach(edge -> {
			final double[] midpoint = midpoint(edge);
			final Vertex near = tree.query1NN(midpoint).value();
			if (near != edge.getA() && near != edge.getB()) {
				if (!preservePerimeter || (preservePerimeter && !edge.isConstrainedRegionBorder())) {
					nonGabrielEdges.add(edge); // base reference
				}
			}
		});
		edges.removeAll(nonGabrielEdges);

		final Collection<PEdge> meshEdges = new ArrayList<>(edges.size());
		edges.forEach(edge -> meshEdges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y)));

		return PGS.polygonizeEdges(meshEdges);
	}

	// Other methods remain unchanged...
}