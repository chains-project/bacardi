import org.tinspin.index.PointEntryDist;
import org.tinspin.index.PointDistanceFunction;
final PointEntryDist<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR }); // find nearest-neighbour circle
private static final PointDistanceFunction circleDistanceMetric = (p1, p2) -> {