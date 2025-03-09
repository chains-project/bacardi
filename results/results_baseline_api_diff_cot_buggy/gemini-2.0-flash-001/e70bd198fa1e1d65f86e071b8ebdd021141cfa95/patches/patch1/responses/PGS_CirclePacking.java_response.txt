import org.tinspin.index.PointDistanceFunction;
import org.tinspin.index.PointEntryDist;
import org.tinspin.index.covertree.CoverTree;

private static final PointDistanceFunction circleDistanceMetric = (p1, p2) -> { ... };
final PointEntryDist<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR });