package micycle.pgs;

import static micycle.pgs.PGS_Conversion.toPShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.shape.GeometricShapeBuilder;
import org.locationtech.jts.shape.fractal.HilbertCurveBuilder;
import org.locationtech.jts.shape.fractal.KochSnowflakeBuilder;
import org.locationtech.jts.shape.fractal.SierpinskiCarpetBuilder;
import org.locationtech.jts.util.GeometricShapeFactory;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import micycle.pgs.PGS_Contour.OffsetStyle;
import micycle.pgs.color.Colors;
import micycle.pgs.commons.BezierShapeGenerator;
import micycle.pgs.commons.PEdge;
import micycle.pgs.commons.RandomPolygon;
import micycle.pgs.commons.RandomSpaceFillingCurve;
import micycle.pgs.commons.Star;
import micycle.spacefillingcurves.SierpinskiFiveSteps;
import micycle.spacefillingcurves.SierpinskiFourSteps;
import micycle.spacefillingcurves.SierpinskiTenSteps;
import micycle.spacefillingcurves.SierpinskiThreeSteps;
import micycle.spacefillingcurves.SpaceFillingCurve;
import micycle.srpg.SRPolygonGenerator;
import net.jafama.FastMath;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Construct uncommon/interesting 2D geometries.
 * 
 * @author Michael Carleton
 *
 */
public class PGS_Construction {

	private PGS_Construction() {
	}

	private static final GeometricShapeFactory shapeFactory = new GeometricShapeFactory();

	static {
		shapeFactory.setNumPoints(PGS.SHAPE_SAMPLES);
	}

	// Other methods...

	/**
	 * Creates a sponge-like porous structure.
	 * 
	 * @param width      the width of the sponge bounds
	 * @param height     the height of the sponge bounds
	 * @param generators the number of generator points for the underlying Voronoi
	 *                   tessellation. Should be >5.
	 * @param thickness  thickness of sponge structure walls
	 * @param smoothing  the cell smoothing factor which determines how rounded the
	 *                   cells are. a value of 6 is a good starting point.
	 * @param classes    the number of classes to use for the cell merging process,
	 *                   where lower results in more merging (or larger "blob-like"
	 *                   shapes).
	 * @param seed       the seed for the random number generator
	 * @return the sponge shape
	 * @since 1.4.0
	 */
	public static PShape createSponge(double width, double height, int generators, double thickness, double smoothing, int classes,
			long seed) {
		// A Simple and Effective Geometric Representation for Irregular Porous
		// Structure Modeling
		List<PVector> points = PGS_PointSet.random(thickness, thickness / 2, width - thickness / 2, height - thickness / 2, generators,
				seed);
		if (points.size() < 6) {
			return new PShape();
		}
		PShape voro = PGS_Voronoi.innerVoronoi(points, 2);

		// Updated handling for the stochasticMerge method
		List<PShape> blobs = PGS_Conversion.getChildren(PGS_Meshing.stochasticMerge(voro, classes)).stream().map(c -> {
			c = PGS_Morphology.buffer(c, -thickness / 2, OffsetStyle.MITER);
			c = PGS_Morphology.smoothGaussian(c, smoothing);
			return c;
		}).collect(Collectors.toList());

		/*
		 * Although faster, can't use .simpleSubtract() here because holes (cell
		 * islands) are *sometimes* nested.
		 */
		PShape flattenedBlobs = PGS_Conversion.flatten(blobs); // Ensure flatten method is called correctly, might need to implement
		PShape s = PGS_ShapeBoolean.subtract(PGS.createRect(0, 0, width, height), flattenedBlobs);
		s.setStroke(false);
		return s;
	}

	// Other methods...
}