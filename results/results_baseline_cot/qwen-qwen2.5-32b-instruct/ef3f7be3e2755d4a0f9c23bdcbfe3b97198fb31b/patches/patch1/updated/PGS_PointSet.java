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
import org.tinspin.index.kdtree.KDTree;
import org.tinspin.index.kdtree.NNResult;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import it.unimi.dsi.util.XoRoShiRo128PlusRandomGenerator;
import micycle.pgs.commons.GeometricMedian;
import micycle.pgs.commons.PEdge;
import micycle.pgs.commons.PoissonDistributionJRUS;
import processing.core.PShape;
import processing.core.PVector;

public final class PGS_PointSet {

{
    private static final float SQRT_3 = (float) Math.sqrt(3);
    /** Golden angle (in radians) */
    private static final float GOLDEN_ANGLE = (float) (Math.PI * (3 - Math.sqrt(5)));

    private PGS_PointSet() {
    }

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

    public static List<PVector> hilbertSort(List<PVector> points) {
        double xMin, xMax, yMin, yMax;
        if (points.isEmpty()) {
            return points;
        }

        PVector v = points.get(0);
        xMin = v.x;
        xMax = v.x;
        yMin = v.y;
        yMax = v.y;

        for (PVector PVector : points) {
            if (PVector.x < xMin) {
                xMin = PVector.x;
            } else if (PVector.x > xMax) {
                xMax = PVector.x;
            }
            if (PVector.y < yMin) {
                yMin = PVector.y;
            } else if (PVector.y > yMax) {
                yMax = PVector.y;
            }
        }

        double xDelta = xMax - xMin;
        double yDelta = yMax - yMin;
        if (xDelta == 0 || yDelta == 0) {
            return points;
        }
        if (points.size() < 24) {
            return points;
        }

        double hn = Math.log(points.size()) / 0.693147180559945 / 2.0;
        int nHilbert = (int) Math.floor(hn + 0.5);
        if (nHilbert < 4) {
            nHilbert = 4;
        }

        List<Pair<Integer, PVector>> ranks = new ArrayList<>(points.size());
        double hScale = (1 << nHilbert) - 1.0;
        for (PVector vh : points) {
            int ix = (int) (hScale * (vh.x - xMin) / xDelta);
            int iy = (int) (hScale * (vh.y - yMin) / yDelta);
            ranks.add(new Pair<>(xy2Hilbert(ix, iy, nHilbert), vh));
        }

        ranks.sort((a, b) -> Integer.compare(a.getFirst(), b.getFirst()));

        return ranks.stream().map(Pair::getSecond).collect(Collectors.toList());
    }

    public static List<List<PVector>> cluster(Collection<PVector> points, int groups) {
        return cluster(points, groups, System.nanoTime());
    }

    public static List<List<PVector>> cluster(Collection<PVector> points, int groups, long seed) {
        RandomGenerator r = new XoRoShiRo128PlusRandomGenerator(seed);
        Clusterer<CPVector> kmeans = new KMeansPlusPlusClusterer<>(groups, 25, new EuclideanDistance(), r);
        List<CPVector> pointz = points.stream().map(p -> new CPVector(p)).collect(Collectors.toList());

        List<List<PVector>> clusters = new ArrayList<>(groups);
        kmeans.cluster(pointz).forEach(cluster -> {
            clusters.add(cluster.getPoints().stream().map(p -> p.p).collect(Collectors.toList()));
        });

        return clusters;
    }

    public static PVector weightedMedian(Collection<PVector> points) {
        boolean allZero = points.stream().allMatch(p -> p.z == 0);
        Point4d[] wp = points.stream().map(p -> new Point4d(p.x, p.y, 0, allZero ? 1 : p.z)).toArray(Point4d[]::new);
        Point3d median = GeometricMedian.median(wp, 1e-3, 50);
        return new PVector((float) median.x, (float) median.y);
    }

    public static List<PVector> random(double xMin, double yMin, double xMax, double yMax, int n) {
        return random(xMin, yMin, xMax, yMax, n, System.nanoTime());
    }

    public static List<PVector> random(double xMin, double yMin, double xMax, double yMax, int n, long seed) {
        final SplittableRandom random = new SplittableRandom(seed);
        final List<PVector> points = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final float x = (float) (xMin + (xMax - xMin) * random.nextDouble());
            final float y = (float) (yMin + (yMax - yMin) * random.nextDouble());
            points.add(new PVector(x, y));
        }
        return points;
    }

    public static List<PVector> gaussian(double centerX, double centerY, double sd, int n) {
        return gaussian(centerX, centerY, sd, n, System.nanoTime());
    }

    public static List<PVector> gaussian(double centerX, double centerY, double sd, int n, long seed) {
        final RandomGenerator random = new XoRoShiRo128PlusRandomGenerator(seed);
        final List<PVector> points = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final float x = (float) (sd * random.nextGaussian() + centerX);
            final float y = (float) (sd * random.nextGaussian() + centerY);
            points.add(new PVector(x, y);
        }
        return points;
    }

    public static List<PVector> squareGrid(final double xMin, final double yMin, final double xMax, final double yMax, final double pointDistance) {
        final double width = xMax - xMin;
        final double height = yMax - yMin;

        final List<PVector> points = new ArrayList<>();

        for (double x = 0; x < width; x += pointDistance) {
            for (double y = 0; y < height; y += pointDistance) {
                points.add(new PVector((float) (x + xMin), (float) (y + yMin));
            }
        }
        return points;
    }

    public static List<PVector> hexGrid(final double xMin, final double yMin, final double xMax, final double yMax, final int n) {
        final double width = xMax - xMin;
        final double height = yMax - yMin;

        final float h = (float) Math.sqrt((width * height * (Math.sqrt(5) / 2)) / n;
        final float v = (float) (h * (2 / Math.sqrt(5));
        final List<PVector> points = new ArrayList<>(n);

        for (int i = 0; i < width / h; i++) {
            for (int j = 0; j < height / v; j++) {
                points.add(new PVector((float) ((i - (j % 2) / 2f) * h + xMin, (float) (j * v + yMin));
            }
        }
        return points;
    }

    public static List<PVector> hexGrid(final double xMin, final double yMin, final double xMax, final double yMax, final double pointDistance) {
        final double width = xMax - xMin;
        final double height = yMax - yMin;

        final List<PVector> points = new ArrayList<>();

        for (int i = 0; i < width / pointDistance; i++) {
            for (int j = 0; j < height / pointDistance; j++) {
                points.add(new PVector((float) ((i - (j % 2) / 2f) * pointDistance + xMin, (float) (j * pointDistance + yMin);
            }
        }
        return points;
    }

    public static List<PVector> hexagon(double centerX, double centerY, int length, double distance) {
        final float xOffset = (float) centerX;
        final float yOffset = (float) centerY;
        final float d = (float) distance;

        final List<PVector> points = new ArrayList<>();

        for (int i = 0; i <= (length - 1); i++) {
            float y = (SQRT_3 * i * d) / 2.0f;
            for (int j = 0; j < (2 * length - 1 - i); j++) {
                float x = (-(2 * length - i - 2) * d) / 2.0f + j * d;
                points.add(new PVector(x + xOffset, y + yOffset, length);
                if (y != 0) {
                    points.add(new PVector(x + xOffset, -y + yOffset, length);
                }
            }
        }
        return points;
    }

    public static List<PVector> ring(double centerX, double centerY, double innerRadius, double outerRadius, double maxAngle, int n) {
        return ring(centerX, centerY, innerRadius, outerRadius, maxAngle, n, System.nanoTime());
    }

    public static List<PVector> ring(double centerX, double centerY, double innerRadius, double outerRadius, double maxAngle, int n, long seed) {
        final SplittableRandom random = new SplittableRandom(seed);
        final List<PVector> points = new ArrayList<>(n);
        if (maxAngle == 0) {
            maxAngle = Double.MIN_VALUE;
        }
        for (int i = 0; i < n; i++) {
            double randomAngle = (maxAngle < 0 ? -1 : 1) * random.nextDouble(Math.abs(maxAngle);
            double randomRadius = random.nextDouble(innerRadius, outerRadius);
            double x = -Math.sin(randomAngle) * randomRadius;
            double y = Math.cos(randomAngle) * randomRadius;

            points.add(new PVector((float) (x + centerX), (float) (y + centerY);
        }
        return points;
    }

    public static List<PVector> poisson(double xMin, double yMin, double xMax, double yMax, double minDist) {
        return poisson(xMin, yMin, xMax, yMax, minDist, System.nanoTime());
    }

    public static List<PVector> poisson(double xMin, double yMin, double xMax, double yMax, double minDist, long seed) {
        final PoissonDistributionJRUS pd = new PoissonDistributionJRUS(seed);
        return pd.generate(xMin, yMin, xMax, yMax, minDist);
    }

    public static List<PVector> poissonN(double xMin, double yMin, double xMax, double yMax, int n, long seed) {
        final PoissonDistributionJRUS pd = new PoissonDistributionJRUS(seed);
        return pd.generate(xMin, yMin, xMax, yMax, n);
    }

    public static List<PVector> nRooksLDS(double xMin, double yMin, double xMax, double yMax, int n) {
        return nRooksLDS(xMin, yMin, xMax, yMax, n, System.nanoTime());
    }

    public static List<PVector> nRooksLDS(double xMin, double yMin, double xMax, double yMax, int n, long seed) {
        final double w = xMax - xMin;
        final double h = yMax - yMin;

        final List<Integer> rookPositions = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(rookPositions, new XoRoShiRo128PlusRandom(seed);

        final float offset = 1.0f / (n * 2);
        final List<PVector> points = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            float x = offset + (rookPositions.get(i) / (float) n;
            x *= w;
            x += xMin;
            float y = offset + i / (float) n;
            y *= h;
            y += yMin;
            points.add(new PVector(x, y);
        }

        return points;
    }

    public static List<PVector> sobolLDS(double xMin, double yMin, double xMax, double yMax, int n) {
        final double w = xMax - xMin;
        final double h = yMax - yMin;
        final int dimension = 2;
        final int BITS = 52;
        final double SCALE = FastMath.pow(2, BITS);
        final long[][] direction = new long[dimension][BITS + 1];
        final long[] x = new long[dimension];
        final int[] m = new int[] { 0, 1 };
        final int a = 0;
        final int s = m.length - 1;

        for (int i = 1; i <= BITS; i++) {
            direction[0][i] = 1l << (BITS - i;
        }

        List<PVector> output = new ArrayList<>(n);
        for (int i = 1; i < n; i++) {

            int c = 1;
            int value = i - 1;
            while ((value & 1) == 1) {
                value >>= 1;
                c++;
            }

            x[0] ^= direction[0][c];
            x[1] ^= direction[1][c];
            double vX = x[0] / SCALE;
            vX *= w;
            vX += xMin;
            double vY = x[1] / SCALE;
            vY *= h;
            vY += yMin;
            output.add(new PVector((float) vX, (float) vY;
        }

        return output;
    }

    private static class CPVector implements Clusterable {
        final PVector p;
        final double[] point;

        CPVector(PVector p) {
            this.p = p;
            point = new double[] { p.x, p.y };
        }

        public double[] getPoint() {
            return point;
        }
    }
}