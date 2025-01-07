package micycle.pgs;

import static micycle.pgs.PGS_Conversion.toPShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.overlayng.CoverageUnion;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.util.GeometricShapeFactory;

import micycle.pgs.commons.PEdge;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Boolean set-operations for 2D shapes.
 * 
 * @author Michael Carleton
 *
 */
public final class PGS_ShapeBoolean {

	private PGS_ShapeBoolean() {
	}

	// Other methods remain unchanged...

	public static PShape intersect(final PShape a, final PShape b) {
		Geometry shapeA = PGS_Conversion.fromPShape(a); // Ensure PGS_Conversion is updated
		Geometry result = OverlayNG.overlay(shapeA, PGS_Conversion.fromPShape(b), OverlayNG.INTERSECTION);
		result.setUserData(shapeA.getUserData()); // preserve shape style (if any)
		return toPShape(result);
	}

	// ... other methods ...

	public static PShape union(final Collection<PShape> shapes) {
		Collection<Geometry> polygons = new ArrayList<>();
		shapes.forEach(s -> polygons.add(PGS_Conversion.fromPShape(s))); // Ensure fromPShape is correctly invoked
		return toPShape(UnaryUnionOp.union(polygons));
	}

	// ... other methods ...

	public static PShape unionMesh(final PShape mesh) {
		if (mesh.getChildCount() < 2 || mesh.getKind() != PConstants.GROUP) {
			if (mesh.getChildCount() == 1) {
				return mesh.getChild(0);
			}
			return mesh;
		}

		return unionMeshWithHoles(mesh);
	}

	private static PShape unionMeshWithHoles(final PShape mesh) {
		return toPShape(CoverageUnion.union(PGS_Conversion.fromPShape(mesh))); // Ensure fromPShape is correctly invoked
	}

	// ... other methods ...

	public static PShape subtractMesh(PShape mesh, PShape area) {
		final Geometry g = PGS_Conversion.fromPShape(area);
		final PreparedGeometry cache = PreparedGeometryFactory.prepare(g);

		List<Geometry> faces = PGS_Conversion.getChildren(mesh).parallelStream().map(s -> {
			final Geometry f = PGS_Conversion.fromPShape(s); // Ensure fromPShape is correctly invoked
			if (cache.containsProperly(f)) {
				return null; // inside -- remove
			} else {
				if (cache.disjoint(f)) {
					return f; // outside -- keep
				}
				// preserve the fill etc of the PShape during subtraction
				Geometry boundarySubtract = OverlayNG.overlay(f, g, OverlayNG.DIFFERENCE);
				boundarySubtract.setUserData(f.getUserData());
				return boundarySubtract;
			}
		}).filter(geometry -> geometry != null).collect(Collectors.toList());

		return PGS_Conversion.toPShape(faces);
	}

	// ... other methods ...
}