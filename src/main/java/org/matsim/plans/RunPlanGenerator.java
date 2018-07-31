package org.matsim.plans;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RunPlanGenerator {
    String zeroDate = "2016.04.16 00:00";
    Set<Integer> nullZones = new HashSet<>();
    String modelScenario = "_horizon2036_1";



    public static void main(String [] args) throws IOException, ParseException, TransformException, FactoryException {
        // Factory, which can create geometric objects
        // Setting the system-wide default at startup time
        System.setProperty("org.geotools.referencing.forceXY", "true");
        GeometryFactory geometryFactory = new GeometryFactory();
        // Creating the point and checking, whether it transforms well
        Point point = geometryFactory.createPoint(new Coordinate(30.359388780101, 59.936114726278));
        CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32635");
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCrs);
        Point point1 = (Point) JTS.transform(point, transform);
        System.out.println(point);
        System.out.println(point1);

        //if(true)return;

        RunPlanGenerator runner = new RunPlanGenerator();
        runner.runParsing();
        runner.readMatrix();
    }

    private void readMatrix() throws ParseException, FactoryException, TransformException {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        Date zeroDate = simpleDateFormat.parse(this.zeroDate);
        try (BufferedReader reader = new BufferedReader(new FileReader("input\\inputForPlans\\trips" + modelScenario + ".csv"));
             PrintWriter printWriter = new PrintWriter("population_" + modelScenario + ".csv")) {
            String line = null;
            long personId = 0;
            long lineNumber = 0;
            while((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.startsWith("ts")) continue;
                if (lineNumber % 1000 == 0){
                    System.out.println(lineNumber);
                }
                String[] items = line.split(";");

                String dateString = items[0];
                System.out.println(dateString);
                if (!dateString.startsWith("2016.04.16")){
                    System.out.println("our date");
                    continue;
                } else {
                    //System.out.println(dateString);
                }
                // here we are parsing the csv file with the
                Date stopDepartureTime = simpleDateFormat.parse(dateString);
                Integer fromZone = Integer.parseInt(items[1]);
                Integer toZone = Integer.parseInt(items[2]);
                Integer count = Integer.parseInt(items[3]);
                String fromZoneName = items[4].toString();
                String modeDefault = items[6];
                System.out.println(fromZoneName);
                RegionBoundaries fromBoundaries = regionBoundariesMap.get(fromZone);
                if (fromBoundaries == null){
                    nullZones.add(fromZone);
                    continue;
                }
                List<Point> fromCoordinates = fromBoundaries.createNumberOfPointsInRegion(count);
                RegionBoundaries toBoundaries = regionBoundariesMap.get(toZone);
                if (toBoundaries == null){
                    nullZones.add(toZone);
                    continue;
                }
                List<Point> toCoordinates = toBoundaries.createNumberOfPointsInRegion(count);
                CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
                CoordinateReferenceSystem crs = CRS.decode("EPSG:32635");
                MathTransform transform = CRS.findMathTransform(sourceCRS, crs);

                for (int i = 0; i < count; i++) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(stopDepartureTime);
                    calendar.add(Calendar.MINUTE, (int) Math.round(60 * Math.random()));
                    Date departureTime = calendar.getTime();
                    Point departurePoint = fromCoordinates.get(i);
                    Point arrivalPoint = toCoordinates.get(i);
                    Point departure = (Point) JTS.transform( departurePoint, transform);
                    Point arrival = (Point) JTS.transform(arrivalPoint, transform);

                    double deltaX = departure.getX() - arrival.getX();
                    double deltaY = departure.getY() - arrival.getY();
                    double distance =  Math.sqrt(deltaX*deltaX + deltaY*deltaY);
                    // принимаемая средняя скорость перемещения по городу для задания времени старта и финиша (14 км/ч)
                    double beelineSpeed = 14/3.6;
                    double beelineSeconds = distance / beelineSpeed;
                    calendar.add(Calendar.SECOND, (int)Math.round(beelineSeconds));
                    Date arrivalTime = calendar.getTime();


                    double matsimDepartureTime = (departureTime.getTime() - zeroDate.getTime()) / 1000;
                    double matsimArrivalTime = (arrivalTime.getTime() - zeroDate.getTime()) / 1000;

                    printWriter.println(String.format(Locale.US, "%d;%f;%f;%f;%f;%f;%f", personId++, matsimDepartureTime,
                            departurePoint.getX(), departurePoint.getY(), matsimArrivalTime, arrivalPoint.getX(),
                            arrivalPoint.getY()));
                    Person person = populationFactory.createPerson(Id.createPersonId(personId++));
                    Plan plan = populationFactory.createPlan();
                    person.addPlan(plan);
                    Activity fromActivity = populationFactory.createActivityFromCoord("w", new Coord(departure.getX(), departure.getY()));
                    fromActivity.setEndTime(matsimDepartureTime);
                    Leg carLeg = populationFactory.createLeg("car");
                    Leg ptLeg = populationFactory.createLeg("pt");
                    Activity toActivity = populationFactory.createActivityFromCoord("w", new Coord(arrival.getX(), arrival.getY()));
                    toActivity.setStartTime(matsimArrivalTime);
                    plan.addActivity(fromActivity);
                    if (modeDefault.equals("car")) {
                        plan.addLeg(carLeg);
                    }
                    else if (modeDefault.equals("pt")) {
                        plan.addLeg(ptLeg);
                    }
                    plan.addActivity(toActivity);
                    scenario.getPopulation().addPerson(person);

                }

            }
            PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
            populationWriter.writeV5("population"  + modelScenario + ".xml");
          } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        nullZones.stream().forEach(zone-> System.out.println(zone));
    }

    // creating the map, where we will store all the Regions (id, its geometrical properties - RegionBoundaries)
    private final Map<Integer, RegionBoundaries> regionBoundariesMap = new HashMap<>();

    // parsing the shape file
    public void runParsing() throws IOException {
        // input file
        File file = new File("input/inputForPlans/shape" + modelScenario + ".shp");
        // smth??
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];


        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                .getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        GeometryFactory geometryFactory = new GeometryFactory();

        // collect objects
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

        // here we are parsing all the objects in the shape-file
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                //System.out.print(": ");
                GeometryAttribute defaultGeometryProperty = feature.getDefaultGeometryProperty();
                System.out.println(defaultGeometryProperty.getValue());

                // if the collected feature is MultiPolygon, the we:
                if (defaultGeometryProperty.getValue() instanceof MultiPolygon){
                    //get its ID
                    Integer featureIDInt = ((Long) feature.getAttribute("id")).intValue();
                    String featureID = featureIDInt.toString();
                    System.out.println(featureID);

                    //Remove unnecessary prefix from it
                    featureID = featureID.replaceAll("station_2.", "");
                    System.out.println(featureID);
                    // set the remaining part (integer) as a regionID
                    Integer districtId = Integer.parseInt(featureID);
                    // set the polygon from this feature
                    MultiPolygon polygon = (MultiPolygon) defaultGeometryProperty.getValue();
                    // getting its boundaries
                    RegionBoundaries regionBoundaries = RegionBoundaries.obtainFromMultipolygon(polygon);

                    regionBoundariesMap.put(districtId, regionBoundaries);
                }
            }
        }
    }
}