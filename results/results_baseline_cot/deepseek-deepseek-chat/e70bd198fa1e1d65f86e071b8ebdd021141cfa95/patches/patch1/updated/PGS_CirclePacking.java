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

import micycle.pgs.commons.FrontChainPacker;
import micycle.pgs.commons.LargestEmptyCircles;
import micycle.pgs.commons.RepulsionCirclePack;
import micycle.pgs.commons.TangencyPack;
import processing.core.PShape;
import processing.core.PVector;

public final class PGS_CirclePacking {

    private static class CircleEntry {
        final double[] point;
        final PVector circle;

        CircleEntry(double[] point, PVector circle) {
            this.point = point;
            this.circle = circle;
        }
    }

    private PGS_CirclePacking() {
    }

    // ... (rest of the methods remain unchanged until stochasticPack)

    public static List<PVector> stochasticPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
            long seed) {

        final CoverTree<CircleEntry> tree = CoverTree.create(3, 2);
        final List<PVector> out = new ArrayList<>();

        List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points, seed);
        if (triangulatePoints) {
            final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, 1, true);
            steinerPoints = StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles)
                    .map(PGS_CirclePacking::centroid).collect(Collectors.toList());
        }

        final List<PVector> vertices = PGS_Conversion.toPVector(shape);
        Collections.shuffle(vertices);
        vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, new CircleEntry(new double[] { p.x, p.y, 0 }, p)));

        float largestR = 0;

        for (PVector p : steinerPoints) {
            final CircleEntry nn = tree.query1NN(new double[] { p.x, p.y, largestR });

            final float dx = p.x - nn.circle.x;
            final float dy = p.y - nn.circle.y;
            final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nn.circle.z);
            if (radius > minRadius) {
                largestR = (radius >= largestR) ? radius : largestR;
                p.z = radius;
                tree.insert(new double[] { p.x, p.y, radius }, new CircleEntry(new double[] { p.x, p.y, radius }, p));
                out.add(p);
            }
        }
        return out;
    }

    // ... (rest of the methods remain unchanged)

    private static final class CircleDistanceFunction implements CoverTree.DistanceFunction {
        @Override
        public double distance(double[] p1, double[] p2) {
            final double dx = p1[0] - p2[0];
            final double dy = p1[1] - p2[1];
            final double dz = p1[2] - p2[2];

            double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
            double absZDifference = Math.abs(dz);
            return euclideanDistance + absZDifference;
        }
    }

    private static final CircleDistanceFunction circleDistanceMetric = new CircleDistanceFunction();

    private static final Predicate<SimpleTriangle> filterBorderTriangles = t -> t.getContainingRegion() != null
            && !t.getEdgeA().isConstrainedRegionBorder() && !t.getEdgeB().isConstrainedRegionBorder()
            && !t.getEdgeC().isConstrainedRegionBorder();
}