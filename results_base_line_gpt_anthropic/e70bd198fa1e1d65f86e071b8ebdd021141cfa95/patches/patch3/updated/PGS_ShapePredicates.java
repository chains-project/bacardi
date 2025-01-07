package micycle.pgs;

import static micycle.pgs.PGS_Conversion.fromPShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Point4d;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.MinimumBoundingCircle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.algorithm.match.HausdorffSimilarityMeasure;
import org.locationtech.jts.coverage.CoverageUnion;
import org.locationtech.jts.coverage.CoverageValidator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.valid.IsValidOp;

import micycle.pgs.commons.EllipticFourierDesc;
import micycle.pgs.commons.GeometricMedian;
import micycle.trapmap.TrapMap;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Various shape metrics, predicates and descriptors.
 * 
 * @author Michael Carleton
 *
 */
public final class PGS_ShapePredicates {

	private PGS_ShapePredicates() {
	}

	// Other methods remain unchanged...

	/**
	 * Returns the diameter of a shape. Diameter is the maximum distance between any
	 * 2 coordinates on the shape perimeter; this is equal to the diameter of the
	 * circumscribed circle.
	 * 
	 * @param shape
	 * @return
	 * @since 1.1.3
	 */
	public static double diameter(PShape shape) {
		List<PVector> farPoints = PGS_Optimisation.findFarthestPointPair(PGS_Conversion.toPVector(shape)); // Updated method call
		return farPoints.get(0).dist(farPoints.get(1));
	}

	// Other methods remain unchanged...
}