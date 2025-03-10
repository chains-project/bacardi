double radius = distanceTolerance;
Object[] result = new Object[1];
while (true) {
    double[] min = new double[] { coords[0] - radius, coords[1] - radius };
    double[] max = new double[] { coords[0] + radius, coords[1] + radius };
    int n = tree.queryOverlap(min, max, result, 1);
    if (n > 0) {
        PVector nearest = (PVector) result[0];
        double dist = Math.sqrt(Math.pow(nearest.x - coords[0], 2) + Math.pow(nearest.y - coords[1], 2));
        if (tree.size() == 0 || dist > distanceTolerance) {
            tree.insert(coords, p);
            newPoints.add(p);
        }
        break;
    } else {
        radius *= 2; // Exponentially increase the search radius
        if (radius > 1e10) {
            tree.insert(coords, p);
            newPoints.add(p);
            break; // Prevent infinite loop if no points are found
        }
    }
}
