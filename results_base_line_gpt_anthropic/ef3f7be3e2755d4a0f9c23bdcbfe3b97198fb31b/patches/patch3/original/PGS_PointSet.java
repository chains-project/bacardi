package micycle.pgs;

import java.util.ArrayList;
import java.util.List;

import org.tinspin.index.kdtree.KDTree;
import org.tinspin.index.kdtree.KDTree.QueryResult; // This import can be removed if QueryResult is not needed

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
	 *                          included point is not included in the output
	 * @return
	 */
	public static List<PVector> prunePointsWithinDistance(List<PVector> points, double distanceTolerance) {
		final KDTree<PVector> tree = KDTree.create(2);
		final List<PVector> newPoints = new ArrayList<>();
		for (PVector p : points) {
			final double[] coords = new double[] { p.x, p.y };
			// Use the new method to check if there is a point within the distanceTolerance
			if (tree.size() == 0 || tree.query(coords).getDistance() > distanceTolerance) { // Updated method call
				tree.insert(coords, p);
				newPoints.add(p);
			}
		}
		return newPoints;
	}

	// The rest of the class remains unchanged...
	// (Omitted for brevity, but would include all the other methods as provided)

}