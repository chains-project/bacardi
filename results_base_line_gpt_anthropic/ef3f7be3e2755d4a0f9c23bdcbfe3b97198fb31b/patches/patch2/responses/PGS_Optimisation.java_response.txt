package micycle.pgs;

import static micycle.pgs.PGS_Conversion.fromPShape;
import static micycle.pgs.PGS_Conversion.toPShape;
import static processing.core.PConstants.GROUP;
import static micycle.pgs.PGS_Construction.createEllipse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.algorithm.MinimumAreaRectangle;
import org.locationtech.jts.algorithm.MinimumBoundingCircle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.algorithm.construct.LargestEmptyCircle;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.util.GeometricShapeFactory;

import almadina.rectpacking.RBPSolution;
import almadina.rectpacking.Rect;
import almadina.rectpacking.RectPacking.PackingHeuristic;
import micycle.pgs.color.Colors;
import micycle.pgs.commons.ClosestPointPair;
import micycle.pgs.commons.FarthestPointPair;
import micycle.pgs.commons.LargestEmptyCircles;
import micycle.pgs.commons.MaximumInscribedAARectangle;
import micycle.pgs.commons.MaximumInscribedRectangle;
import micycle.pgs.commons.MinimumBoundingEllipse;
import micycle.pgs.commons.MinimumBoundingTriangle;
import micycle.pgs.commons.Nullable;
import micycle.pgs.commons.VisibilityPolygon;
import processing.core.PShape;
import processing.core.PVector;
import whitegreen.dalsoo.DalsooPack;

/**
 * Solve geometric optimisation problems, such as bounding volumes, inscribed
 * areas, optimal distances, etc.
 * 
 * @author Michael Carleton
 *
 */
public final class PGS_Optimisation {

	private PGS_Optimisation() {
	}

	// Other methods omitted for brevity...

	/**
	 * Sorts the faces/child shapes of a GROUP shape according to hilbert curve
	 * index of each face's centroid coordinate. This ensures that nearby faces have
	 * a similar index in the list of children.
	 * 
	 * @param mesh group shape
	 * @return a copy of the input shape, having the same faces/child shapes in a
	 *         different order
	 * @since 1.3.0
	 */
	public static PShape hilbertSortFaces(PShape mesh) {
		Map<PVector, PShape> map = new HashMap<>(mesh.getChildCount());
		PGS_Conversion.getChildren(mesh).forEach(child -> {
			PVector centroid = PGS_ShapePredicates.centroid(child);
			map.put(centroid, child);
		});

		List<PVector> points = new ArrayList<>(map.keySet());
		// Updated method call to reflect the new method signature in the updated PGS_PointSet class
		return PGS_Conversion.flatten(PGS_PointSet.hilbertSortFaces(points).stream().map(map::get).collect(Collectors.toList()));
	}

	// Other methods omitted for brevity...

	/**
	 * Packs a collection of rectangles, according to the given packing heuristic,
	 * into rectangular 2D bin(s). Within each bin rectangles are packed flush with
	 * each other, having no overlap. Each rectangle is packed parallel to the edges
	 * of the plane.
	 * <p>
	 * When packed rectangles fill one bin, any remaining rectangles will be packed
	 * into additional bin(s).
	 * 
	 * @param rectangles a collection of rectangles (represented by PVectors),
	 *                   specifying their width (.x) and height (.y)
	 * @param binWidth   the width of each bin's area in which to pack the
	 *                   rectangles
	 * @param binHeight  the height of each bin's area in which to pack the
	 *                   rectangles
	 * @param heuristic  the packing heuristic to use. The heuristic determines
	 *                   rules for how every subsequent rectangle is placed
	 * @since 1.4.0
	 * @return a GROUP PShape, where each immediate child is a GROUP shape
	 *         corresponding to a bin; the child shapes of each bin are rectangles.
	 *         Bins are positioned at (0, 0).
	 */
	public static PShape rectPack(List<PVector> rectangles, int binWidth, int binHeight, RectPackHeuristic heuristic) {
		RBPSolution packer = new RBPSolution(binWidth, binHeight);
		List<Rect> rects = rectangles.stream().map(p -> Rect.of(Math.round(p.x), Math.round(p.y))).collect(Collectors.toList());

		packer.pack(rects, heuristic.h);

		PShape bins = new PShape(GROUP);
		packer.getBins().forEach(bin -> {
			PShape binGroup = new PShape(GROUP);
			bin.getPackedRects().forEach(r -> {
				binGroup.addChild(PGS.createRect(r.x, r.y, r.width, r.height));
			});
			bins.addChild(binGroup);
		});
		return bins;
	}

	// Other methods omitted for brevity...
}