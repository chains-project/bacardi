final KDTree<PVector> tree = KDTree.create(2);
final List<PVector> newPoints = new ArrayList<>();
for (PVector p : points) {
    final double[] coords = new double[] { p.x, p.y };
    if (tree.size() == 0) {
        tree.insert(coords, p);
        newPoints.add(p);
    } else {
        org.tinspin.index.PointEntry<PVector> nearest = tree.nearestNeighbour(coords);
        if (nearest == null || nearest.dist() > distanceTolerance) {
            tree.insert(coords, p);
            newPoints.add(p);
        }
    }
}
return newPoints;
