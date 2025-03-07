package micycle.pgs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.vecmath.Point3d;
import javax.vecmath.Point4d;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.spanning.PrimMinimumSpanningTree;
import org.jgrapht.graph.SimpleGraph;
import org.tinfour.common.IIncrementalTin;
import org.tinspin.index.kdtree.KDTree;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import it.unimi.dsi.util.XoRoShiRo128PlusRandomGenerator;
import micycle.pgs.commons.GeometricMedian;
import micycle.pgs.commons.PEdge;
import micycle.pgs.commons.PoissonDistributionJRUS;
import processing.core.PShape;
import processing.core.PVector;

public final class PGS_PointSet {

    private static final float SQRT_3 = (float) Math.sqrt(3);
    private static final float GOLDEN_ANGLE = (float) (Math.PI * (3 - Math.sqrt(5)));

    private PGS_PointSet() {
    }

    public static List<PVector> prunePointsWithinDistance(List<PVector> points, double distanceTolerance) {
        final KDTree<PVector> tree = KDTree.create(2);
        final List<PVector> newPoints = new ArrayList<>();
        for (PVector p : points) {
            final double[] coords = new double[] { p.x, p.y };
            if (tree.size() == 0 || tree.query(coords).dist() > distanceTolerance) {
                tree.insert(coords, p);
                newPoints.add(p);
            }
        }
        return newPoints;
    }

    // ... (rest of the class remains unchanged)
    
    private static class CPVector implements Clusterable {
        final PVector p;
        final double[] point;

        CPVector(PVector p) {
            this.p = p;
            point = new double[] { p.x, p.y };
        }

        @Override
        public double[] getPoint() {
            return point;
        }
    }
}