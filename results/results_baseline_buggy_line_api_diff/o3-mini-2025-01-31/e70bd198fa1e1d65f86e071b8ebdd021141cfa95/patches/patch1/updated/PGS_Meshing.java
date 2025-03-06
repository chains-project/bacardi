package micycle.pgs;

import static micycle.pgs.PGS_Conversion.getChildren;

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
import org.tinspin.index.phtree.PHTreeMMP;
import it.unimi.dsi.util.XoRoShiRo128PlusRandomGenerator;
import micycle.pgs.PGS_Conversion.PShapeData;
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

public class PGS_Meshing {

	private PGS_Meshing() {
	}

	/**
	 * Generates a shape consisting of polygonal faces of an <i>Urquhart graph</i>.
	 */
	public static PShape urquhartFaces(final IIncrementalTin triangulation, final boolean preservePerimeter) {
		final HashSet<IQuadEdge> edges = PGS.makeHashSet(triangulation.getMaximumEdgeAllocationIndex());
		final HashSet<IQuadEdge> uniqueLongestEdges = PGS.makeHashSet(triangulation.getMaximumEdgeAllocationIndex());

		final boolean notConstrained = triangulation.getConstraints().isEmpty();

		TriangleCollector.visitSimpleTriangles(triangulation, t -> {
			final IConstraint constraint = t.getContainingRegion();
			if (notConstrained || (constraint != null && constraint.definesConstrainedRegion())) {
				edges.add(t.getEdgeA().getBaseReference());
				edges.add(t.getEdgeB().getBaseReference());
				edges.add(t.getEdgeC().getBaseReference());
				final IQuadEdge longestEdge = findLongestEdge(t).getBaseReference();
				if (!preservePerimeter || (preservePerimeter && !longestEdge.isConstrainedRegionBorder())) {
					uniqueLongestEdges.add(longestEdge);
				}
			}
		});

		edges.removeAll(uniqueLongestEdges);

		final Collection<PEdge> meshEdges = new ArrayList<>(edges.size());
		edges.forEach(edge -> meshEdges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y)));

		return PGS.polygonizeEdges(meshEdges);
	}

	/**
	 * Generates a shape consisting of polygonal faces of a <i>Gabriel graph</i>.
	 */
	public static PShape gabrielFaces(final IIncrementalTin triangulation, final boolean preservePerimeter) {
		final HashSet<IQuadEdge> edges = new HashSet<>();
		final HashSet<Vertex> vertices = new HashSet<>();

		final boolean notConstrained = triangulation.getConstraints().isEmpty();
		TriangleCollector.visitSimpleTriangles(triangulation, t -> {
			final IConstraint constraint = t.getContainingRegion();
			if (notConstrained || (constraint != null && constraint.definesConstrainedRegion())) {
				edges.add(t.getEdgeA().getBaseReference());
				edges.add(t.getEdgeB().getBaseReference());
				edges.add(t.getEdgeC().getBaseReference());
				vertices.add(t.getVertexA());
				vertices.add(t.getVertexB());
				vertices.add(t.getVertexC());
			}
		});

		// Use the new PHTreeMMP API in place of the removed PointIndex/KDTree
		final PHTreeMMP<Vertex> tree = PHTreeMMP.create(2);
		vertices.forEach(v -> tree.insert(new double[] { v.x, v.y }, v));

		final HashSet<IQuadEdge> nonGabrielEdges = new HashSet<>();
		edges.forEach(edge -> {
			final double[] midpoint = midpoint(edge);
			final Vertex near = tree.query1NN(midpoint).value();
			if (near != edge.getA() && near != edge.getB()) {
				if (!preservePerimeter || (preservePerimeter && !edge.isConstrainedRegionBorder())) {
					nonGabrielEdges.add(edge);
				}
			}
		});
		edges.removeAll(nonGabrielEdges);

		final Collection<PEdge> meshEdges = new ArrayList<>(edges.size());
		edges.forEach(edge -> meshEdges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y)));

		return PGS.polygonizeEdges(meshEdges);
	}

	/**
	 * Generates a shape consisting of polygonal faces of a <i>Relative neighborhood
	 * graph</i> (RNG).
	 */
	public static PShape relativeNeighborFaces(final IIncrementalTin triangulation, final boolean preservePerimeter) {
		SimpleGraph<Vertex, IQuadEdge> graph = PGS_Triangulation.toTinfourGraph(triangulation);
		NeighborCache<Vertex, IQuadEdge> cache = new NeighborCache<>(graph);

		Set<IQuadEdge> edges = new HashSet<>(graph.edgeSet());

		/*
		 * If any vertex is nearer to both vertices of an edge, than the length of the edge, this edge does not belong in the RNG.
		 */
		graph.edgeSet().forEach(e -> {
			double l = e.getLength();
			cache.neighborsOf(e.getA()).forEach(n -> {
				if (Math.max(n.getDistance(e.getA()), n.getDistance(e.getB())) < l) {
					if (!preservePerimeter || (preservePerimeter && !e.isConstrainedRegionBorder())) {
						edges.remove(e);
					}
				}
			});
			cache.neighborsOf(e.getB()).forEach(n -> {
				if (Math.max(n.getDistance(e.getA()), n.getDistance(e.getB())) < l) {
					if (!preservePerimeter || (preservePerimeter && !e.isConstrainedRegionBorder())) {
						edges.remove(e);
					}
				}
			});
		});

		List<PEdge> edgesOut = edges.stream().map(PGS_Triangulation::toPEdge).collect(Collectors.toList());

		if (preservePerimeter) {
			return PGS.polygonizeEdgesRobust(edgesOut);
		} else {
			return PGS.polygonizeEdges(edgesOut);
		}

	}

	/**
	 * Generates a shape consisting of polygonal faces formed by edges returned by a
	 * greedy sparse spanner applied to a triangulation.
	 */
	public static PShape spannerFaces(final IIncrementalTin triangulation, int k, final boolean preservePerimeter) {
		SimpleGraph<PVector, PEdge> graph = PGS_Triangulation.toGraph(triangulation);
		if (graph.edgeSet().isEmpty()) {
			return new PShape();
		}

		k = Math.max(2, k);
		GreedyMultiplicativeSpanner<PVector, PEdge> spanner = new GreedyMultiplicativeSpanner<>(graph, k);
		List<PEdge> spannerEdges = spanner.getSpanner().stream().collect(Collectors.toList());
		if (preservePerimeter) {
			if (triangulation.getConstraints().isEmpty()) {
				spannerEdges.addAll(triangulation.getPerimeter().stream().map(PGS_Triangulation::toPEdge).collect(Collectors.toList()));
			} else {
				spannerEdges.addAll(triangulation.getEdges().stream().filter(IQuadEdge::isConstrainedRegionBorder)
						.map(PGS_Triangulation::toPEdge).collect(Collectors.toList()));
			}
		}

		return PGS.polygonizeEdgesRobust(spannerEdges);
	}

	/**
	 * Generates a shape consisting of polygonal faces of the dual graph
	 * of the given triangulation.
	 */
	public static PShape dualFaces(final IIncrementalTin triangulation) {
		final IncrementalTinDual dual = new IncrementalTinDual(triangulation);
		final PShape dualMesh = dual.getMesh();
		PGS_Conversion.setAllFillColor(dualMesh, Colors.WHITE);
		PGS_Conversion.setAllStrokeColor(dualMesh, Colors.PINK, 2);
		return dualMesh;
	}

	/**
	 * Produces a quadrangulation from a triangulation, by splitting each triangle
	 * into three quadrangles (using the <i>Catmull and Clark</i> technique).
	 */
	public static PShape splitQuadrangulation(final IIncrementalTin triangulation) {
		final PShape quads = new PShape(PConstants.GROUP);

		final boolean unconstrained = triangulation.getConstraints().isEmpty();

		TriangleCollector.visitSimpleTriangles(triangulation, t -> {
			final IConstraint constraint = t.getContainingRegion();
			if (unconstrained || (constraint != null && constraint.definesConstrainedRegion())) {
				final PVector p1 = PGS_Triangulation.toPVector(t.getVertexA());
				final PVector p2 = PGS_Triangulation.toPVector(t.getVertexB());
				final PVector p3 = PGS_Triangulation.toPVector(t.getVertexC());
				final PVector sA = PVector.add(p1, p2).div(2);
				final PVector sB = PVector.add(p2, p3).div(2);
				final PVector sC = PVector.add(p3, p1).div(2);

				final PVector cSeg = PVector.add(sA, sB).div(2);
				final PVector sI = PVector.add(cSeg, sC).div(2);

				quads.addChild(PGS_Conversion.fromPVector(p1, sC, sI, sA, p1));
				quads.addChild(PGS_Conversion.fromPVector(p2, sA, sI, sB, p2));
				quads.addChild(PGS_Conversion.fromPVector(p3, sB, sI, sC, p3));
			}
		});

		PGS_Conversion.setAllFillColor(quads, Colors.WHITE);
		PGS_Conversion.setAllStrokeColor(quads, Colors.PINK, 2);

		return quads;
	}

	/**
	 * Generates a quadrangulation from a triangulation by selectively removing (or
	 * "collapsing") the edges shared by neighboring triangles (via edge coloring).
	 */
	public static PShape edgeCollapseQuadrangulation(final IIncrementalTin triangulation, final boolean preservePerimeter) {
		final boolean unconstrained = triangulation.getConstraints().isEmpty();
		final AbstractBaseGraph<IQuadEdge, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
		TriangleCollector.visitSimpleTriangles(triangulation, t -> {
			final IConstraint constraint = t.getContainingRegion();
			if (unconstrained || (constraint != null && constraint.definesConstrainedRegion())) {
				graph.addVertex(t.getEdgeA().getBaseReference());
				graph.addVertex(t.getEdgeB().getBaseReference());
				graph.addVertex(t.getEdgeC().getBaseReference());

				graph.addEdge(t.getEdgeA().getBaseReference(), t.getEdgeB().getBaseReference());
				graph.addEdge(t.getEdgeA().getBaseReference(), t.getEdgeC().getBaseReference());
				graph.addEdge(t.getEdgeB().getBaseReference(), t.getEdgeC().getBaseReference());
			}
		});

		Coloring<IQuadEdge> coloring = new RLFColoring<>(graph, 1337).getColoring();

		final HashSet<IQuadEdge> perimeter = new HashSet<>(triangulation.getPerimeter());
		if (!unconstrained) {
			perimeter.clear();
		}

		final Collection<PEdge> meshEdges = new ArrayList<>();
		coloring.getColors().forEach((edge, color) -> {
			if ((color < 2) || (preservePerimeter && (edge.isConstrainedRegionBorder() || perimeter.contains(edge)))) {
				meshEdges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y));
			}
		});

		PShape quads = PGS.polygonizeEdges(meshEdges);
		if (triangulation.getConstraints().size() < 2) {
			return quads;
		} else {
			return removeHoles(quads, triangulation);
		}
	}

	/**
	 * Generates a quadrangulation from a triangulation by "inverting" triangles.
	 */
	public static PShape centroidQuadrangulation(final IIncrementalTin triangulation, final boolean preservePerimeter) {
		final boolean unconstrained = triangulation.getConstraints().isEmpty();
		final HashSet<PEdge> edges = new HashSet<>();
		TriangleCollector.visitSimpleTriangles(triangulation, t -> {
			final IConstraint constraint = t.getContainingRegion();
			if (unconstrained || (constraint != null && constraint.definesConstrainedRegion())) {
				Vertex centroid = centroid(t);
				edges.add(new PEdge(centroid.getX(), centroid.getY(), t.getVertexA().x, t.getVertexA().y));
				edges.add(new PEdge(centroid.getX(), centroid.getY(), t.getVertexB().x, t.getVertexB().y));
				edges.add(new PEdge(centroid.getX(), centroid.getY(), t.getVertexC().x, t.getVertexC().y));
			}
		});

		if (preservePerimeter) {
			List<IQuadEdge> perimeter = triangulation.getPerimeter();
			triangulation.edges().forEach(edge -> {
				if (edge.isConstrainedRegionBorder() || (unconstrained && perimeter.contains(edge))) {
					edges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y));
				}
			});
		}

		final PShape quads = PGS.polygonizeEdges(edges);
		if (triangulation.getConstraints().size() < 2) {
			return quads;
		} else {
			return removeHoles(quads, triangulation);
		}
	}

	/**
	 * Removes (what should be) holes from a polygonized quadrangulation.
	 */
	private static PShape removeHoles(PShape faces, IIncrementalTin triangulation) {
		List<IConstraint> holes = new ArrayList<>(triangulation.getConstraints());
		holes = holes.subList(1, holes.size());

		STRtree tree = new STRtree();
		holes.stream().map(constraint -> constraint.getVertices()).iterator().forEachRemaining(vertices -> {
			CoordinateList coords = new CoordinateList();
			vertices.forEach(v -> coords.add(new Coordinate(v.x, v.y)));
			coords.closeRing();

			if (!Orientation.isCCWArea(coords.toCoordinateArray())) {
				Polygon polygon = PGS.GEOM_FACTORY.createPolygon(coords.toCoordinateArray());
				tree.insert(polygon.getEnvelopeInternal(), polygon);
			}
		});

		List<PShape> nonHoles = PGS_Conversion.getChildren(faces).parallelStream().filter(quad -> {
			final Geometry g = PGS_Conversion.fromPShape(quad);

			@SuppressWarnings("unchecked")
			List<Polygon> matches = tree.query(g.getEnvelopeInternal());

			for (Polygon m : matches) {
				try {
					Geometry overlap = OverlayNG.overlay(m, g, OverlayNG.INTERSECTION);
					double a1 = g.getArea();
					double a2 = m.getArea();
					double total = a1 + a2;
					double aOverlap = overlap.getArea();
					double w1 = a1 / total;
					double w2 = a2 / total;

					double similarity = w1 * (aOverlap / a1) + w2 * (aOverlap / a2);
					if (similarity > 0.2) {
						return false;
					}
				} catch (Exception e) {
					continue;
				}

			}
			return true;
		}).collect(Collectors.toList());

		return PGS_Conversion.flatten(nonHoles);
	}

	/**
	 * Produces a quadrangulation from a point set. The resulting quadrangulation
	 * has a characteristic spiral pattern.
	 */
	public static PShape spiralQuadrangulation(List<PVector> points) {
		SpiralQuadrangulation sq = new SpiralQuadrangulation(points);
		return PGS.polygonizeEdges(sq.getQuadrangulationEdges());
	}

	/**
	 * Transforms a non-conforming mesh shape into a <i>conforming mesh</i> by
	 * performing a "noding" operation.
	 */
	public static PShape nodeNonMesh(PShape shape) {
		final List<SegmentString> segmentStrings = new ArrayList<>(shape.getChildCount() * 3);

		for (PShape face : shape.getChildren()) {
			for (int i = 0; i < face.getVertexCount(); i++) {
				final PVector a = face.getVertex(i);
				final PVector b = face.getVertex((i + 1) % face.getVertexCount());
				if (!a.equals(b)) {
					segmentStrings.add(PGS.createSegmentString(a, b));
				}
			}
		}
		return PGS.polygonizeSegments(segmentStrings, true);
	}

	/**
	 * Randomly merges together / dissolves adjacent faces of a mesh.
	 */
	public static PShape stochasticMerge(PShape mesh, int nClasses, long seed) {
		final RandomGenerator random = new XoRoShiRo128PlusRandomGenerator(seed);
		SimpleGraph<PShape, DefaultEdge> graph = PGS_Conversion.toDualGraph(mesh);
		Map<PShape, Integer> classes = new HashMap<>();
		graph.vertexSet().forEach(v -> classes.put(v, random.nextInt(Math.max(nClasses, 1))));

		NeighborCache<PShape, DefaultEdge> cache = new NeighborCache<>(graph);
		graph.vertexSet().forEach(v -> {
			final int vClass = classes.get(v);
			List<PShape> neighbours = cache.neighborListOf(v);
			final int nClass1 = classes.get(neighbours.get(0));
			if (vClass == nClass1) {
				return;
			}

			neighbours.removeIf(n -> classes.get(n) == nClass1);
			if (neighbours.isEmpty()) {
				classes.put(v, nClass1);
			}
		});

		List<DefaultEdge> toRemove = new ArrayList<>();
		graph.edgeSet().forEach(e -> {
			PShape a = graph.getEdgeSource(e);
			PShape b = graph.getEdgeTarget(e);
			if (!classes.get(a).equals(classes.get(b))) {
				toRemove.add(e);
			}
		});
		graph.removeAllEdges(toRemove);
		ConnectivityInspector<PShape, DefaultEdge> ci = new ConnectivityInspector<>(graph);

		List<PShape> blobs = ci.connectedSets().stream().map(group -> PGS_ShapeBoolean.unionMesh(PGS_Conversion.flatten(group)))
				.collect(Collectors.toList());

		return applyOriginalStyling(PGS_Conversion.flatten(blobs), mesh);
	}

	/**
	 * Smoothes a mesh via iterative weighted <i>Laplacian smoothing</i>.
	 */
	public static PShape smoothMesh(PShape mesh, int iterations, boolean preservePerimeter, double taubin, double t2) {
		PMesh m = new PMesh(mesh);
		for (int i = 0; i < iterations; i++) {
			m.smoothTaubin(0.25, -0.251, preservePerimeter);
		}
		return m.getMesh();
	}

	/**
	 * Smoothes a mesh via iterative weighted <i>Laplacian smoothing</i>.
	 */
	public static PShape smoothMesh(PShape mesh, double displacementCutoff, boolean preservePerimeter) {
		displacementCutoff = Math.max(displacementCutoff, 1e-3);
		PMesh m = new PMesh(mesh);

		double displacement;
		do {
			displacement = m.smoothTaubin(0.25, -0.251, preservePerimeter);
		} while (displacement > displacementCutoff);
		return m.getMesh();
	}

	/**
	 * Simplifies the boundaries of the faces in a mesh while preserving the original mesh topology.
	 */
	public static PShape simplifyMesh(PShape mesh, double tolerance, boolean preservePerimeter) {
		Geometry[] geometries = PGS_Conversion.getChildren(mesh).stream().map(s -> PGS_Conversion.fromPShape(s)).toArray(Geometry[]::new);
		CoverageSimplifier simplifier = new CoverageSimplifier(geometries);
		Geometry[] output;
		if (preservePerimeter) {
			output = simplifier.simplifyInner(tolerance);
		} else {
			output = simplifier.simplify(tolerance);
		}
		return applyOriginalStyling(PGS_Conversion.toPShape(Arrays.asList(output)), mesh);
	}

	/**
	 * Subdivides the faces of a mesh using the Catmull-Clark split approach.
	 */
	public static PShape subdivideMesh(PShape mesh, double edgeSplitRatio) {
		edgeSplitRatio %= 1;
		PShape newMesh = new PShape(PShape.GROUP);
		for (PShape face : getChildren(mesh)) {
			List<PVector> vertices = PGS_Conversion.toPVector(face);
			List<PVector> midPoints = new ArrayList<>();
			PVector centroid = new PVector();
			for (int i = 0; i < vertices.size(); i++) {
				PVector a = vertices.get(i);
				PVector b = vertices.get((i + 1) % vertices.size());
				midPoints.add(PVector.lerp(a, b, (float) edgeSplitRatio));
				centroid.add(a);
			}
			centroid.div(vertices.size());

			for (int i = 0; i < vertices.size(); i++) {
				PVector a = vertices.get(i);
				PVector b = midPoints.get(i);
				PVector c = centroid.copy();
				PVector d = midPoints.get(i - 1 < 0 ? vertices.size() - 1 : i - 1);
				newMesh.addChild(PGS_Conversion.fromPVector(a, b, c, d, a));
			}
		}
		return newMesh;
	}

	/**
	 * Extracts all inner edges from a mesh.
	 */
	public static PShape extractInnerEdges(PShape mesh) {
		List<PEdge> edges = PGS_SegmentSet.fromPShape(mesh);
		Map<PEdge, Integer> bag = new HashMap<>(edges.size());
		edges.forEach(edge -> {
			bag.merge(edge, 1, Integer::sum);
		});

		List<PEdge> innerEdges = bag.entrySet().stream().filter(e -> e.getValue() > 1).map(e -> e.getKey()).collect(Collectors.toList());
		return PGS_SegmentSet.dissolve(innerEdges);
	}

	/**
	 * Recursively merges smaller faces of a mesh into their adjacent faces.
	 */
	public static PShape areaMerge(PShape mesh, double areaThreshold) {
		PShape merged = AreaMerge.areaMerge(mesh, areaThreshold);
		return applyOriginalStyling(merged, mesh);
	}

	/**
	 * Splits each edge of a given mesh shape into a specified number of equal-length parts.
	 */
	public static PShape splitEdges(PShape split, int parts) {
		parts = Math.max(1, parts);
		HashSet<PEdge> edges = new HashSet<>(PGS_SegmentSet.fromPShape(split));
		List<PEdge> splitEdges = new ArrayList<>(edges.size() * parts);

		for (PEdge edge : edges) {
			double from = 0;
			double step = 1.0 / parts;
			for (int i = 0; i < parts; i++) {
				double to = from + step;
				PEdge subEdge = edge.slice(from, to);
				splitEdges.add(subEdge);
				from = to;
			}
		}

		return PGS.polygonizeEdges(splitEdges);
	}

	private static PShape applyOriginalStyling(final PShape newMesh, final PShape oldMesh) {
		final PShapeData data = new PShapeData(oldMesh.getChild(0));
		for (int i = 0; i < newMesh.getChildCount(); i++) {
			data.applyTo(newMesh.getChild(i));
		}
		return newMesh;
	}

	/**
	 * Calculate the longest edge of a given triangle.
	 */
	private static IQuadEdge findLongestEdge(final SimpleTriangle t) {
		if (t.getEdgeA().getLength() > t.getEdgeB().getLength()) {
			if (t.getEdgeC().getLength() > t.getEdgeA().getLength()) {
				return t.getEdgeC();
			} else {
				return t.getEdgeA();
			}
		} else {
			if (t.getEdgeC().getLength() > t.getEdgeB().getLength()) {
				return t.getEdgeC();
			} else {
				return t.getEdgeB();
			}
		}
	}

	private static double[] midpoint(final IQuadEdge edge) {
		final Vertex a = edge.getA();
		final Vertex b = edge.getB();
		return new double[] { (a.x + b.x) / 2d, (a.y + b.y) / 2d };
	}

	private static Vertex centroid(final SimpleTriangle t) {
		final Vertex a = t.getVertexA();
		final Vertex b = t.getVertexB();
		final Vertex c = t.getVertexC();
		double x = a.x + b.x + c.x;
		x /= 3;
		double y = a.y + b.y + c.y;
		y /= 3;
		return new Vertex(x, y, 0);
	}

}