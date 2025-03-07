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
import org.tinspin.index.kdtree.NearestNeighbourResult;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import it.unimi.dsi.util.XoRoShiRo128PlusRandomGenerator;
import micycle.pgs.commons.GeometricMedian;
import micycle.pgs.commons.PEdge;
import micycle.pgs.commons.PoissonDistributionJRUS;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Generation of random sets of 2D points having a variety of different
 * distributions and constraints (and associated functions).
 * 
 * @author Michael Carleton
 * @since 1.2.0
 *
 */
public final class PGS_PointSet {

{
    private static final float SQRT_3 = (float) Math.sqrt(3);
    /** Golden angle (in radians) */
    private static final float GOLDEN_ANGLE = (float) (Math.PI * (3 - Math.sqrt(5)));

    private PGS_PointSet() {
    }

    /**
     * Returns a filtered copy of the input, containing no points that are within
     * the <code>distanceTolerance</code> of each other.
     * <p>
     * This method can be used to convert a random point set into a blue-noise-like
     * (poisson) point set.
     * 
     * @param points            list of points to filter
     * @param distanceTolerance a point that is within this distance of a previously
     *                         included point is not included in the output
     * @return
     */
    public static List<PVector> prunePointsWithinDistance(List<PVector> points, double distanceTolerance) {
        final KDTree<PVector> tree = KDTree.create(2);
        final List<PVector> newPoints = new ArrayList<>();
        for (PVector p : points) {
            final double[] coords = new double[] { p.x, p.y };
            if (tree.size() == 0 || tree.nearestNeighbour(coords).getDistance() > distanceTolerance) {
                tree.insert(coords, p);
                newPoints.add(p);
            }
        }
        return newPoints;
    }

    // ... (rest of the class remains unchanged)
}