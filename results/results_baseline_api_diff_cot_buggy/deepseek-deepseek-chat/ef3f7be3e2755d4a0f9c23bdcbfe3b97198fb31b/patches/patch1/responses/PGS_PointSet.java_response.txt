if (tree.size() == 0 || tree.queryNearestNeighbor(coords) == null || 
         Math.sqrt(Math.pow(coords[0] - tree.queryNearestNeighbor(coords)[0], 2) + 
                   Math.pow(coords[1] - tree.queryNearestNeighbor(coords)[1], 2)) > distanceTolerance) {
     