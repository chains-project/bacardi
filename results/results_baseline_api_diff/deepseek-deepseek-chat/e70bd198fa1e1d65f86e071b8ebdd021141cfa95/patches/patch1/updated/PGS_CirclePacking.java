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
import org.tinspin.index.CoverTree;
import org.tinspin.index.PointEntry;
import org.tinspin.index.PointIndex;

import micycle.pgs.commons.FrontChainPacker;
import micycle.pgs.commons.LargestEmptyCircles;
import micycle.pgs.commons.RepulsionCirclePack;
import micycle.pgs.commons.TangencyPack;
import processing.core.PShape;
import processing.core.PVector;

public final class PGS_CirclePacking {

	private PGS_CirclePacking() {
	}

	public static List<PVector> obstaclePack(PShape shape, Collection<PVector> pointObstacles, double areaCoverRatio) {
		final Geometry geometry = fromPShape(shape);

		LargestEmptyCircles lec = new LargestEmptyCircles(fromPShape(PGS_Conversion.toPointsPShape(pointObstacles)), geometry,
				areaCoverRatio > 0.95 ? 0.5 : 1);

		final double shapeArea = geometry.getArea();
		double circlesArea = 0;
		List<PVector> circles = new ArrayList<>();

		while (circlesArea / shapeArea < areaCoverRatio) {
			double[] currentLEC = lec.findNextLEC();
			circles.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]));
			circlesArea += Math.PI * currentLEC[2] * currentLEC[2];
			if (currentLEC[2] < 0.5) {
				break;
			}
		}
		return circles;
	}

	public static List<PVector> trinscribedPack(PShape shape, int points, int refinements) {
		final List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points);
		final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, refinements, true);
		return StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles).map(t -> inCircle(t))
				.collect(Collectors.toList());
	}

	public static List<PVector> stochasticPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints) {
		return stochasticPack(shape, points, minRadius, triangulatePoints, System.nanoTime());
	}

	public static List<PVector> stochasticPack(final PShape shape, final int points, final double minRadius, boolean triangulatePoints,
			long seed) {

		final PointIndex<PVector> tree = CoverTree.create(3);
		final List<PVector> out = new ArrayList<>();

		List<PVector> steinerPoints = PGS_Processing.generateRandomPoints(shape, points, seed);
		if (triangulatePoints) {
			final IIncrementalTin tin = PGS_Triangulation.delaunayTriangulationMesh(shape, steinerPoints, true, 1, true);
			steinerPoints = StreamSupport.stream(tin.triangles().spliterator(), false).filter(filterBorderTriangles)
					.map(PGS_CirclePacking::centroid).collect(Collectors.toList());
		}

		final List<PVector> vertices = PGS_Conversion.toPVector(shape);
		Collections.shuffle(vertices);
		vertices.forEach(p -> tree.insert(new double[] { p.x, p.y, 0 }, p));

		float largestR = 0;

		for (PVector p : steinerPoints) {
			PointEntry<PVector> nn = tree.query1NN(new double[] { p.x, p.y, largestR });

			final float dx = p.x - nn.value().x;
			final float dy = p.y - nn.value().y;
			final float radius = (float) (Math.sqrt(dx * dx + dy * dy) - nn.value().z);
			if (radius > minRadius) {
				largestR = (radius >= largestR) ? radius : largestR;
				p.z = radius;
				tree.insert(new double[] { p.x, p.y, radius }, p);
				out.add(p);
			}
		}
		return out;
	}

	public static List<PVector> frontChainPack(PShape shape, double radiusMin, double radiusMax) {
		radiusMin = Math.max(1f, Math.min(radiusMin, radiusMax));
		radiusMax = Math.max(1f, Math.max(radiusMin, radiusMax));
		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();
		IndexedPointInAreaLocator pointLocator;

		final FrontChainPacker packer = new FrontChainPacker((float) e.getWidth(), (float) e.getHeight(), (float) radiusMin,
				(float) radiusMax, (float) e.getMinX(), (float) e.getMinY());

		if (radiusMin == radiusMax) {
			pointLocator = new IndexedPointInAreaLocator(g.buffer(radiusMax));
			packer.getCircles().removeIf(p -> pointLocator.locate(PGS.coordFromPVector(p)) == Location.EXTERIOR);
		} else {
			pointLocator = new IndexedPointInAreaLocator(g);
			final PreparedGeometry cache = PreparedGeometryFactory.prepare(g);
			final GeometricShapeFactory circleFactory = new GeometricShapeFactory();
			circleFactory.setNumPoints(8);
			packer.getCircles().removeIf(p -> {
				if (pointLocator.locate(PGS.coordFromPVector(p)) != Location.EXTERIOR) {
					return false;
				}
				circleFactory.setCentre(PGS.coordFromPVector(p));
				circleFactory.setSize(p.z * 2);
				return !cache.intersects(circleFactory.createCircle());
			});
		}

		return packer.getCircles();
	}

	public static List<PVector> maximumInscribedPack(PShape shape, int n, double tolerance) {
		tolerance = Math.max(0.01, tolerance);
		LargestEmptyCircles mics = new LargestEmptyCircles(fromPShape(shape), null, tolerance);

		final List<PVector> out = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			double[] c = mics.findNextLEC();
			out.add(new PVector((float) c[0], (float) c[1], (float) c[2]));
		}

		return out;
	}

	public static List<PVector> maximumInscribedPack(PShape shape, double minRadius, double tolerance) {
		tolerance = Math.max(0.01, tolerance);
		minRadius = Math.max(0.01, minRadius);
		LargestEmptyCircles mics = new LargestEmptyCircles(fromPShape(shape), null, tolerance);

		final List<PVector> out = new ArrayList<>();
		double[] currentLEC;
		do {
			currentLEC = mics.findNextLEC();
			if (currentLEC[2] >= minRadius) {
				out.add(new PVector((float) currentLEC[0], (float) currentLEC[1], (float) currentLEC[2]));
			}
		} while (currentLEC[2] >= minRadius);

		return out;
	}

	public static List<PVector> tangencyPack(IIncrementalTin triangulation, double boundaryRadii) {
		TangencyPack pack = new TangencyPack(triangulation, boundaryRadii);
		return pack.pack();
	}

	public static List<PVector> tangencyPack(IIncrementalTin triangulation, double[] boundaryRadii) {
		TangencyPack pack = new TangencyPack(triangulation, boundaryRadii);
		return pack.pack();
	}

	public static List<PVector> repulsionPack(PShape shape, double radiusMin, double radiusMax, long seed) {
		final double rMinA = Math.max(1f, Math.min(radiusMin, radiusMax));
		final double rMaxA = Math.max(1f, Math.max(radiusMin, radiusMax));
		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();

		double totalArea = e.getArea() * 0.85;
		double avgCircleArea = ((rMaxA * rMaxA * rMaxA) - (rMinA * rMinA * rMinA));
		avgCircleArea *= (Math.PI / (3 * (rMaxA - rMinA)));
		int n = (int) (totalArea / avgCircleArea);

		List<PVector> points = PGS_PointSet.poissonN(e.getMinX() + rMaxA, e.getMinY() + rMaxA, e.getMaxX() - rMaxA, e.getMaxY() - rMaxA, n,
				seed);
		SplittableRandom r = new SplittableRandom(seed);
		points.forEach(p -> p.z = rMaxA == rMinA ? (float) rMaxA : (float) r.nextDouble(rMinA, rMaxA));

		return repulsionPack(shape, points);
	}

	public static List<PVector> repulsionPack(PShape shape, List<PVector> circles) {
		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();

		float radiusMin = Float.MAX_VALUE;
		float radiusMax = Float.MIN_VALUE;
		for (PVector circle : circles) {
			radiusMax = Math.max(1f, Math.max(radiusMax, circle.z));
			radiusMin = Math.max(1f, Math.min(radiusMin, circle.z));
		}

		final RepulsionCirclePack packer = new RepulsionCirclePack(circles, e.getMinX() + radiusMin, e.getMaxX() - radiusMin,
				e.getMinY() + radiusMin, e.getMaxY() - radiusMin, false);

		final List<PVector> packing = packer.getPacking();

		IndexedPointInAreaLocator pointLocator;
		if (radiusMin == radiusMax) {
			pointLocator = new IndexedPointInAreaLocator(g.buffer(radiusMax));
			packing.removeIf(p -> pointLocator.locate(PGS.coordFromPVector(p)) == Location.EXTERIOR);
		} else {
			pointLocator = new IndexedPointInAreaLocator(g);
			IndexedFacetDistance distIndex = new IndexedFacetDistance(g);
			packing.removeIf(p -> {
				if (pointLocator.locate(PGS.coordFromPVector(p)) != Location.EXTERIOR) {
					return false;
				}
				return distIndex.distance(PGS.pointFromPVector(p)) > p.z * 0.5;
			});
		}

		return packing;
	}

	public static List<PVector> squareLatticePack(PShape shape, double diameter) {
		diameter = Math.max(diameter, 0.1);
		final double radius = diameter / 2;

		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();
		final IndexedPointInAreaLocator pointLocator = new IndexedPointInAreaLocator(g.buffer(radius * 0.95));
		final double w = e.getWidth() + diameter + e.getMinX();
		final double h = e.getHeight() + diameter + e.getMinY();

		final List<PVector> out = new ArrayList<>();

		for (double x = e.getMinX(); x < w; x += diameter) {
			for (double y = e.getMinY(); y < h; y += diameter) {
				if (pointLocator.locate(new Coordinate(x, y)) != Location.EXTERIOR) {
					out.add(new PVector((float) x, (float) y, (float) radius));
				}
			}
		}
		return out;
	}

	public static List<PVector> hexLatticePack(PShape shape, double diameter) {
		diameter = Math.max(diameter, 0.1);
		final double radius = diameter / 2d;

		final Geometry g = fromPShape(shape);
		final Envelope e = g.getEnvelopeInternal();
		final IndexedPointInAreaLocator pointLocator = new IndexedPointInAreaLocator(g.buffer(radius * 0.95));
		final double w = e.getWidth() + diameter + e.getMinX();
		final double h = e.getHeight() + diameter + e.getMinY();

		final List<PVector> out = new ArrayList<>();

		final double z = radius * Math.sqrt(3);
		double offset = 0;
		for (double x = e.getMinX(); x < w; x += z) {
			offset = (offset == radius) ? 0 : radius;
			for (double y = e.getMinY() - offset; y < h; y += diameter) {
				if (pointLocator.locate(new Coordinate(x, y)) != Location.EXTERIOR) {
					out.add(new PVector((float) x, (float) y, (float) radius));
				}
			}
		}
		return out;
	}

	private static PVector inCircle(SimpleTriangle t) {
		final double a = t.getEdgeA().getLength();
		final double b = t.getEdgeB().getLength();
		final double c = t.getEdgeC().getLength();

		double inCenterX = t.getVertexA().x * a + t.getVertexB().x * b + t.getVertexC().x * c;
		inCenterX /= (a + b + c);
		double inCenterY = t.getVertexA().y * a + t.getVertexB().y * b + t.getVertexC().y * c;
		inCenterY /= (a + b + c);

		final double s = (a + b + c) / 2;

		final double r = Math.sqrt(((s - a) * (s - b) * (s - c)) / s);

		return new PVector((float) inCenterX, (float) inCenterY, (float) r);
	}

	private static PVector centroid(SimpleTriangle t) {
		final Vertex a = t.getVertexA();
		final Vertex b = t.getVertexB();
		final Vertex c = t.getVertexC();
		double x = a.x + b.x + c.x;
		x /= 3;
		double y = a.y + b.y + c.y;
		y /= 3;
		return new PVector((float) x, (float) y);
	}

	private static final Predicate<SimpleTriangle> filterBorderTriangles = t -> t.getContainingRegion() != null
			&& !t.getEdgeA().isConstrainedRegionBorder() && !t.getEdgeB().isConstrainedRegionBorder()
			&& !t.getEdgeC().isConstrainedRegionBorder();

}