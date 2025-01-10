package micycle.pgs;

import static micycle.pgs.PGS_Conversion.toPShape; // Adjusted import to use a valid method

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.HilbertSort;
import org.tinfour.voronoi.BoundedVoronoiBuildOptions;
import org.tinfour.voronoi.BoundedVoronoiDiagram;
import org.tinfour.voronoi.ThiessenPolygon;

import micycle.pgs.color.Colors;
import micycle.pgs.commons.Nullable;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Voronoi Diagrams of shapes and point sets. Supports polygonal constraining
 * and relaxation to generate centroidal Voronoi.
 * 
 * @author Michael Carleton
 *
 */
@SuppressWarnings("squid:S3776")
public final class PGS_Voronoi {

	private PGS_Voronoi() {
	}

	public static PShape innerVoronoi(final PShape shape, final boolean constrain) {
		return innerVoronoi(shape, constrain, null, null, 0);
	}

	public static PShape innerVoronoi(final PShape shape, final int relaxations) {
		return innerVoronoi(shape, true, null, null, relaxations);
	}

	public static PShape innerVoronoi(final PShape shape, Collection<PVector> additionalSites) {
		return innerVoronoi(shape, true, null, additionalSites, 0);
	}

	public static PShape innerVoronoi(final PShape shape, Collection<PVector> additionalSites, int relaxations) {
		return innerVoronoi(shape, true, null, additionalSites, relaxations);
	}

	public static PShape innerVoronoi(final PShape shape, final boolean constrain, @Nullable final double[] bounds,
			@Nullable final Collection<PVector> steinerPoints, final int relaxations) {
		final Geometry g = toPShape(shape); // Corrected fromPShape to toPShape
		final List<Vertex> vertices = new ArrayList<>();
		final Coordinate[] coords = g.getCoordinates();
		if (coords.length < 3) {
			return new PShape();
		}

		final BoundedVoronoiBuildOptions options = new BoundedVoronoiBuildOptions();
		final Rectangle2D boundsRect;
		if (bounds == null) {
			final Envelope e = g.getEnvelopeInternal();
			boundsRect = new Rectangle2D.Double(e.getMinX(), e.getMinY(), e.getWidth(), e.getHeight());
		} else {
			boundsRect = new Rectangle2D.Double(bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1]);
		}
		options.setBounds(boundsRect);

		for (int i = 0; i < coords.length; i++) {
			Coordinate p = coords[i];
			if (boundsRect.contains(p.x, p.y)) {
				vertices.add(new Vertex(p.x, p.y, Double.NaN, i));
			}
		}
		if (steinerPoints != null) {
			steinerPoints.forEach(p -> {
				if (boundsRect.contains(p.x, p.y)) {
					vertices.add(new Vertex(p.x, p.y, vertices.size()));
				}
			});
		}

		BoundedVoronoiDiagram v = new BoundedVoronoiDiagram(vertices, options);

		for (int i = 0; i < relaxations; i++) {
			double maxDistDelta = 0;
			List<Vertex> newSites = new ArrayList<>(vertices.size());
			for (ThiessenPolygon p : v.getPolygons()) {
				final Vertex newSite;
				if (p.getVertex().getIndex() == 0 || steinerPoints == null) {
					PVector centroid = computeCentroid(p);
					PVector site = new PVector((float) p.getVertex().x, (float) p.getVertex().y);
					site.add(PVector.sub(centroid, site).mult(1.5f));
					newSite = new Vertex(site.x, site.y, p.getIndex());
					maxDistDelta = Math.max(maxDistDelta, p.getVertex().getDistance(centroid.x, centroid.y));
				} else {
					newSite = p.getVertex();
				}
				if (boundsRect.contains(newSite.x, newSite.y)) {
					newSites.add(newSite);
				}
			}
			if (maxDistDelta < 0.05) {
				break;
			}
			v = new BoundedVoronoiDiagram(newSites, options);
		}

		List<Geometry> faces = v.getPolygons().stream().filter(p -> p.getEdges().size() > 1).map(PGS_Voronoi::toPolygon)
				.collect(Collectors.toList());
		if (constrain && g instanceof Polygonal) {
			faces = faces.parallelStream().map(f -> OverlayNG.overlay(f, g, OverlayNG.INTERSECTION)).collect(Collectors.toList());
			faces.removeIf(f -> f.getNumPoints() == 0);
		}

		PShape facesShape = PGS_Conversion.toPShape(faces);
		for (int i = 0; i < faces.size(); i++) {
			if (faces.get(i).getUserData() != null) {
				facesShape.getChild(i).setName(Integer.toString((int) faces.get(i).getUserData()));
			}
		}
		return facesShape;
	}

	public static PShape innerVoronoi(Collection<PVector> points) {
		return innerVoronoi(PGS_Conversion.toPointsPShape(points), false);
	}

	public static PShape innerVoronoi(Collection<PVector> points, int relaxations) {
		return innerVoronoi(PGS_Conversion.toPointsPShape(points), false, null, null, relaxations);
	}

	public static PShape innerVoronoi(Collection<PVector> points, double[] bounds, int relaxations) {
		return innerVoronoi(PGS_Conversion.toPointsPShape(points), false, bounds, null, relaxations);
	}

	public static PShape innerVoronoi(Collection<PVector> points, double[] bounds) {
		return innerVoronoi(PGS_Conversion.toPointsPShape(points), false, bounds, null, 0);
	}

	public static PShape compoundVoronoi(PShape shape) {
		return compoundVoronoi(shape, null);
	}

	public static PShape compoundVoronoi(PShape shape, double[] bounds) {
		Geometry g = toPShape(shape);
		Geometry densified = Densifier.densify(g, 2);

		List<Vertex> vertices = new ArrayList<>();
		final List<List<Vertex>> segmentVertexGroups = new ArrayList<>();

		for (int i = 0; i < densified.getNumGeometries(); i++) {
			Geometry geometry = densified.getGeometryN(i);
			List<Vertex> featureVertices;
			switch (geometry.getGeometryType()) {
				case Geometry.TYPENAME_LINEARRING :
				case Geometry.TYPENAME_POLYGON :
				case Geometry.TYPENAME_LINESTRING :
				case Geometry.TYPENAME_POINT :
					featureVertices = toVertex(geometry.getCoordinates());
					if (!featureVertices.isEmpty()) {
						segmentVertexGroups.add(featureVertices);
						vertices.addAll(featureVertices);
					}
					break;
				case Geometry.TYPENAME_MULTILINESTRING :
				case Geometry.TYPENAME_MULTIPOINT :
				case Geometry.TYPENAME_MULTIPOLYGON :
					for (int j = 0; j < geometry.getNumGeometries(); j++) {
						featureVertices = toVertex(geometry.getGeometryN(j).getCoordinates());
						if (!featureVertices.isEmpty()) {
							segmentVertexGroups.add(featureVertices);
							vertices.addAll(featureVertices);
						}
					}
					break;
				default :
					break;
			}
		}

		if (vertices.size() > 2500) {
			HilbertSort hs = new HilbertSort();
			hs.sort(vertices);
		}
		final IncrementalTin tin = new IncrementalTin(2);
		tin.add(vertices, null);
		if (!tin.isBootstrapped()) {
			return new PShape();
		}

		final BoundedVoronoiBuildOptions options = new BoundedVoronoiBuildOptions();
		final double x, y, w, h;
		if (bounds == null) {
			final Envelope envelope = g.getEnvelopeInternal();
			x = envelope.getMinX();
			y = envelope.getMinY();
			w = envelope.getMaxX() - envelope.getMinX();
			h = envelope.getMaxY() - envelope.getMinY();
		} else {
			x = bounds[0];
			y = bounds[1];
			w = bounds[2] - bounds[0];
			h = bounds[3] - bounds[1];
		}
		options.setBounds(new Rectangle2D.Double(x, y, w, h));

		final BoundedVoronoiDiagram voronoi = new BoundedVoronoiDiagram(tin);

		final HashMap<Vertex, ThiessenPolygon> vertexCellMap = new HashMap<>();
		voronoi.getPolygons().forEach(p -> vertexCellMap.put(p.getVertex(), p));

		final List<PShape> faces = segmentVertexGroups.parallelStream().map(vertexGroup -> {
			PShape cellSegments = new PShape(PConstants.GROUP);
			vertexGroup.forEach(segmentVertex -> {
				ThiessenPolygon thiessenCell = vertexCellMap.get(segmentVertex);
				if (thiessenCell != null) {
					PShape cellSegment = new PShape(PShape.PATH);
					cellSegment.beginShape();
					for (IQuadEdge e : thiessenCell.getEdges()) {
						cellSegment.vertex((float) e.getA().x, (float) e.getA().y);
					}
					cellSegment.endShape(PConstants.CLOSE);
					cellSegments.addChild(cellSegment);
				}
			});
			return PGS_ShapeBoolean.unionMesh(cellSegments);
		}).collect(Collectors.toList());

		PShape voronoiCells = PGS_Conversion.flatten(faces);
		PGS_Conversion.setAllFillColor(voronoiCells, Colors.WHITE);
		PGS_Conversion.setAllStrokeColor(voronoiCells, Colors.PINK, 2);

		return voronoiCells;
	}

	static Polygon toPolygon(ThiessenPolygon polygon) {
		Coordinate[] coords = new Coordinate[polygon.getEdges().size() + 1];
		int i = 0;
		for (IQuadEdge e : polygon.getEdges()) {
			coords[i++] = new Coordinate(e.getA().x, e.getA().y);
		}
		coords[i] = new Coordinate(polygon.getEdges().get(0).getA().x, polygon.getEdges().get(0).getA().y);
		Polygon p = PGS.GEOM_FACTORY.createPolygon(coords);
		p.setUserData(polygon.getIndex());
		return p;
	}

	private static PVector computeCentroid(ThiessenPolygon polygon) {
		double xSum = 0;
		double ySum = 0;
		int n = 0;
		for (IQuadEdge e : polygon.getEdges()) {
			xSum += e.getA().x;
			ySum += e.getA().y;
			n++;
		}
		return new PVector((float) xSum / n, (float) ySum / n);
	}

	private static List<Vertex> toVertex(Coordinate[] coords) {
		final boolean closed = coords[0].equals2D(coords[coords.length - 1]) && coords.length > 1;
		List<Vertex> vertices = new ArrayList<>(coords.length - (closed ? 1 : 0));
		for (int i = 0; i < coords.length - (closed ? 1 : 0); i++) {
			Coordinate coord = coords[i];
			vertices.add(new Vertex(coord.x, coord.y, 0));
		}
		return vertices;
	}
}