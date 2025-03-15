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
import org.tinspin.index.PointIndex;
import org.tinspin.index.kdtree.KDTree;
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

/**
 * Mesh generation (excluding triangulation) and processing.
 * <p>
 * Many of the methods within this class process an existing Delaunay
 * triangulation. You may first generate such a triangulation from a shape using
 * {@link PGS_Triangulation#delaunayTriangulationMesh(PShape, Collection, boolean, int, boolean)
 * delaunayTriangulationMesh()} and then feed it to this method.
 */
public class PGS_Meshing {

    private PGS_Meshing() {
    }

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

    public static PShape relativeNeighborFaces(final IIncrementalTin triangulation, final boolean preservePerimeter) {
        SimpleGraph<Vertex, IQuadEdge> graph = PGS_Triangulation.toTinfourGraph(triangulation);
        NeighborCache<Vertex, IQuadEdge> cache = new NeighborCache<>(graph);

        Set<IQuadEdge> edges = new HashSet<>(graph.edgeSet());

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

    public static PShape spannerFaces(final IIncrementalTin triangulation, int k, final boolean preservePerimeter) {
        SimpleGraph<PVector, PEdge> graph = PGS_Triangulation.toGraph(triangulation);
        if (graph.edgeSet().isEmpty()) {
            return new PShape();
        }

        k = Math.max(2, k); // min(2) since k=1 returns triangulation
        GreedyMultiplicativeSpanner<PVector, PEdge> spanner = new GreedyMultiplicativeSpanner<>(graph, k);
        List<PEdge> spannerEdges = spanner.getSpanner().stream().collect(Collectors.toList());
        if (preservePerimeter) {
            if (triangulation.getConstraints().isEmpty()) { // does not have constraints
                spannerEdges.addAll(triangulation.getPerimeter().stream().map(PGS_Triangulation::toPEdge).collect(Collectors.toList()));
            } else { // has constraints
                spannerEdges.addAll(triangulation.getEdges().stream().filter(IQuadEdge::isConstrainedRegionBorder)
                        .map(PGS_Triangulation::toPEdge).collect(Collectors.toList()));
            }
        }

        return PGS.polygonizeEdgesRobust(spannerEdges);
    }

    public static PShape dualFaces(final IIncrementalTin triangulation) {
        // TODO SEE HOT: Hodge-Optimized Triangulations - use voronoi dual / HOT
        final IncrementalTinDual dual = new IncrementalTinDual(triangulation);
        final PShape dualMesh = dual.getMesh();
        PGS_Conversion.setAllFillColor(dualMesh, Colors.WHITE);
        PGS_Conversion.setAllStrokeColor(dualMesh, Colors.PINK, 2);
        return dualMesh;
    }

    public static PShape splitQuadrangulation(final IIncrementalTin triangulation) {
        // https://www.cs.mcgill.ca/~cs507/projects/1998/rachelp/welcome.html
        final PShape quads = new PShape(PConstants.GROUP);

        final boolean unconstrained = triangulation.getConstraints().isEmpty();

        TriangleCollector.visitSimpleTriangles(triangulation, t -> {
            final IConstraint constraint = t.getContainingRegion();
            if (unconstrained || (constraint != null && constraint.definesConstrainedRegion())) {
                final PVector p1 = PGS_Triangulation.toPVector(t.getVertexA());
                final PVector p2 = PGS_Triangulation.toPVector(t.getVertexB());
                final PVector p3 = PGS_Triangulation.toPVector(t.getVertexC());
                final PVector sA = PVector.add(p1, p2).div(2); // steiner point p1-p2
                final PVector sB = PVector.add(p2, p3).div(2); // steiner point p2-p3
                final PVector sC = PVector.add(p3, p1).div(2); // steiner point p3-p1

                // compute ?barycenter? of triangle = interior steiner point
                final PVector cSeg = PVector.add(sA, sB).div(2);
                final PVector sI = PVector.add(cSeg, sC).div(2); // interior steiner point

                // anti-clockwise, starting at original vertex
                quads.addChild(PGS_Conversion.fromPVector(p1, sC, sI, sA, p1);
                quads.addChild(PGS_Conversion.fromPVector(p2, sA, sI, sB, p2);
                quads.addChild(PGS_Conversion.fromPVector(p3, sB, sI, sC, p3);
            }
        });

        PGS_Conversion.setAllFillColor(quads, Colors.WHITE);
        PGS_Conversion.setAllStrokeColor(quads, Colors.PINK, 2);

        return quads;
    }

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
            perimeter.clear(); // clear, the perimeter of constrained tin is unaffected by the constraint
        }

        final Collection<PEdge> meshEdges = new ArrayList<>();
        coloring.getColors().forEach((edge, color) -> {
            if ((color < 2) || (preservePerimeter && (edge.isConstrainedRegionBorder() || perimeter.contains(edge))) {
                meshEdges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y);
            }
        });

        PShape quads = PGS.polygonizeEdges(meshEdges);
        if (triangulation.getConstraints().size() < 2) { // assume constraint 1 is the boundary (not a hole)
            return quads;
        } else {
            return removeHoles(quads, triangulation);
        }
    }

    public static PShape centroidQuadrangulation(final IIncrementalTin triangulation, final boolean preservePerimeter) {
        final boolean unconstrained = triangulation.getConstraints().isEmpty();
        final HashSet<PEdge> edges = new HashSet<>();
        TriangleCollector.visitSimpleTriangles(triangulation, t -> {
            final IConstraint constraint = t.getContainingRegion();
            if (unconstrained || (constraint != null && constraint.definesConstrainedRegion())) {
                edges.add(t.getEdgeA().getBaseReference());
                edges.add(t.getEdgeB().getBaseReference());
                edges.add(t.getEdgeC().getBaseReference());
                Vertex centroid = centroid(t);
                edges.add(new PEdge(centroid.getX, centroid.getY, t.getVertexA().x, t.getVertexA().y);
                edges.add(new PEdge(centroid.getX, centroid.getY, t.getVertexB().x, t.getVertexB().y);
                edges.add(new PEdge(centroid.getX, centroid.getY, t.getVertexC().x, t.getVertexC().y);
            }
        });

        if (preservePerimeter) {
            List<IQuadEdge> perimeter = triangulation.getPerimeter();
            triangulation.edges().forEach(edge -> {
                if (edge.isConstrainedRegionBorder() || (unconstrained && perimeter.contains(edge))) {
                    edges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y);
                }
            });
        }

        PShape quads = PGS.polygonizeEdges(edges);
        if (triangulation.getConstraints().size() < 2) { // assume constraint 1 is the boundary (not a hole)
            return quads;
        } else {
            return removeHoles(quads, triangulation);
        }
    }

    public static PShape removeHoles(PShape faces, IIncrementalTin triangulation) {
        List<IConstraint> holes = new ArrayList<>(triangulation.getConstraints()); // copy list
        holes = holes.subList(1, holes.size()); // slice off perimeter constraint (not a hole)

        STRtree tree = new STRtree();
        holes.stream().map(constraint -> constraint.getVertices()).iterator().forEachRemaining(vertices -> {
            CoordinateList coords = new CoordinateList(); // coords of constraint
            vertices.forEach(v -> coords.add(new Coordinate(v.x, v.y));
            coords.closeRing();

            if (!Orientation.isCCWArea(coords.toCoordinateArray()) { // triangulation holes are CW
                Polygon polygon = PGS.GEOM_FACTORY.createPolygon(coords.toCoordinateArray());
                tree.insert(polygon.getEnvelopeInternal(), polygon);
            }
        });

        List<PEdge> meshEdges = new ArrayList<>();
        edges.forEach(edge -> {
            double[] midpoint = midpoint(edge);
            Vertex near = tree.query1NN(midpoint).value();
            if (near != edge.getA() && near != edge.getB()) {
                if (!preservePerimeter || (preservePerimeter && !edge.isConstrainedRegionBorder())) {
                    nonGabrielEdges.add(edge); // base reference
                }
            }
        });
        edges.removeAll(nonGabrielEdges);

        Collection<PEdge> meshEdges = new ArrayList<>(edges.size());
        edges.forEach(edge -> meshEdges.add(new PEdge(edge.getA().x, edge.getA().y, edge.getB().x, edge.getB().y));

        return PGS.polygonizeEdges(meshEdges);
    }

    public static PShape smoothMesh(PShape mesh, int iterations, boolean preservePerimeter, double taubin, double t2) {
        PMesh m = new PMesh(mesh);
        for (int i = 0; i < iterations; i++) {
            m.smoothTaubin(0.25, -0.251, preservePerimeter);
        }
        return m.getMesh();
    }

    public static PShape splitEdges(PShape split, int parts) {
        parts = Math.max(1, parts);
        Set<PEdge> edges = new HashSet<>(PGS_SegmentSet.fromPShape(split);
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
        final PShapeData data = new PShapeData(oldMesh.getChild(0); // use first child; assume global.
        for (int i = 0; i < newMesh.getChildCount(); i++) {
            data.applyTo(newMesh.getChild(i);
        }
        return newMesh;
    }

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