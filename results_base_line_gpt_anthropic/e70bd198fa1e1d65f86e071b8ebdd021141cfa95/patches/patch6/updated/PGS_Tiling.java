package micycle.pgs;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import micycle.pgs.color.Colors;
import micycle.pgs.commons.DoyleSpiral;
import micycle.pgs.commons.HatchTiling;
import micycle.pgs.commons.PenroseTiling;
import micycle.pgs.commons.RectangularSubdivision;
import micycle.pgs.commons.SquareTriangleTiling;
import micycle.pgs.commons.TriangleSubdivision;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

/**
 * Tiling, tessellation and subdivision of the plane using periodic or
 * non-periodic geometric shapes.
 * <p>
 * A tiling is created when a collection of plane figures (tileCount) fills a
 * plane such that no gaps occur between the tileCount and no two tileCount
 * overlap each other.
 * 
 * @author Michael Carleton
 * @since 1.2.0
 */
public final class PGS_Tiling {

    private static final double ROOT3 = Math.sqrt(3);

    private PGS_Tiling() {
    }

    // Other methods...

    public static PShape quadSubdivision(double width, double height, int depth, long seed) {
        // https://openprocessing.org/sketch/1045334
        final float w = (float) width;
        final float h = (float) height;
        final float off = 20;
        final SplittableRandom r = new SplittableRandom(seed);

        final PVector p1 = new PVector(off, off);
        final PVector p2 = new PVector(w - off, off);
        final PVector p3 = new PVector(w - off, h - off);
        final PVector p4 = new PVector(off, h - off);

        final PShape divisions = new PShape(PConstants.GROUP);
        divideRect(p1, p2, p3, p4, depth, divisions, r);
        // Updated method calls to match the new PGS_Conversion methods
        // Fixing method calls for missing methods in the updated dependencies
        PGS_Conversion.setAllFillColor(divisions, Colors.WHITE);
        PGS_Conversion.setAllStrokeColor(divisions, Colors.PINK, 2);
        return divisions;
    }

    public static PShape hatchSubdivision(double width, double height, int gridCountX, int gridCountY, long seed) {
        final HatchTiling ht = new HatchTiling((int) width, (int) height, gridCountX, gridCountY);
        PShape tiling = ht.getTiling(seed);
        // Fixing the missing method call for setting stroke color
        tiling.setStroke(true);
        tiling.setStroke(Colors.PINK);
        tiling.setStrokeWeight(4);
        return tiling;
    }

    // Other methods...

    public static PShape islamicTiling(double width, double height, double w, double h) {
        final double[] vector = { -w, 0, w, -h, w, 0, -w, h };
        final ArrayList<PVector> segments = new ArrayList<>();
        for (int x = 0; x < width; x += w * 2) {
            for (int y = 0; y < height; y += h * 2) {
                for (int i = 0; i < vector.length; i++) {
                    segments.add(
                            new PVector((float) (vector[i % vector.length] + x + w), (float) (vector[(i + 6) % vector.length] + y + h)));
                    segments.add(new PVector((float) (vector[(i + 1) % vector.length] + x + w),
                            (float) (vector[(i + 1 + 6) % vector.length] + y + h)));
                }
            }
        }
        return PGS_Processing.polygonizeLines(segments);
    }

    // Other methods...

}