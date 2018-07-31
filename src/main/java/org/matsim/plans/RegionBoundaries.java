package org.matsim.plans;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTSFactoryFinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ekaterina on 03.09.2016.
 */
public class RegionBoundaries {
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final MultiPolygon multiPolygon;

    // this is the constructor
    public RegionBoundaries(double minX, double minY, double maxX, double maxY, MultiPolygon multiPolygon) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.multiPolygon = multiPolygon;
    }

    // this sets up the minimum and maximum X and Y coordinates of the region
    public static RegionBoundaries obtainFromMultipolygon(MultiPolygon multiPolygon){
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Coordinate coordinate : multiPolygon.getBoundary().getCoordinates()){
            if (coordinate.x > maxX){
                maxX = coordinate.x;
            }
            if (coordinate.x < minX){
                minX = coordinate.x;
            }
            if (coordinate.y > maxY){
                maxY = coordinate.y;
            }
            if (coordinate.y < minY){
                minY = coordinate.y;
            }
        }
        // returns the Boundaries given the multiPolygon
        return new RegionBoundaries(minX, minY, maxX, maxY, multiPolygon);
    }


    // this methods creates the number of the points in the region which corresponds
    // with the number of trips, which are starting (ending) at this region
    List<Point> createNumberOfPointsInRegion(int numberOfPoint){
        MultiPolygon polygon = multiPolygon;
        List<Point> returnElements = new ArrayList<>(numberOfPoint);
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        while (returnElements.size() < numberOfPoint){
            // Here we create a random coordinates inside the sqare (xmin, xmax, ymin, ymax)
            double x = minX + Math.random() * (maxX - minX);
            double y = minY + Math.random() * (maxY - minY);
            // here we store the point with this coordinates
            Point point = geometryFactory.createPoint(new Coordinate(x, y));
            // here we check if the point is inside of the polygon using the standard method of the library
            if (polygon.contains(point)){
                // if it is inside the
                returnElements.add(point);
            }
        }
        return returnElements;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }
}
