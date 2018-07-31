package org.matsim.run;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.vehicles.Vehicle;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AgentsStat {
    private Network network;
    private Scenario scenario;
    private MathTransform transform;
    private Integer NumberOfPolygons;
    private static ArrayList<Id<Person>> list_observed_agents = new ArrayList<>();
    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    private Map<Integer, MultiPolygon> MultiPolygonsMap;
    private Table<String, Integer, Integer> StatTable = HashBasedTable.create();

    private Map<String, ArrayList<Id<Vehicle>>> MapOfVehicleAlongWRD = new HashMap<String, ArrayList<Id<Vehicle>>>(){{
       put("forward", new ArrayList<Id<Vehicle>>());
       put("opposite", new ArrayList<Id<Vehicle>>());
    }};

    //Class constructor
    AgentsStat(Scenario scenario, Map<Integer, MultiPolygon> MultiPolygonsMap, MathTransform transform) {
        this.scenario = scenario;
        this.network = scenario.getNetwork();
        this.transform = transform;
        this.MultiPolygonsMap = MultiPolygonsMap;
        this.NumberOfPolygons = MultiPolygonsMap.size();

        //Initialization StatTable in the class constructor
        for (Integer i = 1; i <= NumberOfPolygons; i++) {
            for (Integer j = 1; j<=24; j++) {
                StatTable.put("StartPoint"+i+"WithWRD", j, 0);
                StatTable.put("StartPoint"+i+"WithoutWRD", j, 0);
                StatTable.put("EndPoint"+i+"WithWRD", j, 0);
                StatTable.put("EndPoint"+i+"WithoutWRD", j, 0);
            }
        }
    }

    //This method is responsible for fixing the agents who drove along the WRD
    public void addAgentOnWRD(boolean direction, Id<Vehicle> vehicleId) {
        if (direction) {
            MapOfVehicleAlongWRD.get("forward").add(vehicleId);
        } else {
            MapOfVehicleAlongWRD.get("opposite").add(vehicleId);
        }
    }

    //Clear the list of agents passing by the Western Rapid Diameter
    public void clearAgentsOnWRD(){
        MapOfVehicleAlongWRD.get("forward").clear();
        MapOfVehicleAlongWRD.get("opposite").clear();
    }

    //Network Getter
    public Network getNetwork() {
        return network;
    }

    //List of Observed Agents Getter
    public ArrayList<Id<Person>> getListObservedAgents() {
        return list_observed_agents;
    }

    private boolean checkAgentOnWRD(Id<Person> personId) {
        for (String key:MapOfVehicleAlongWRD.keySet()) {
            if (MapOfVehicleAlongWRD.get(key).contains(personId)) {
                return true;
            }
        }
        return false;
    }

    public Integer checkZone(Coord coord) {
        Point point;
        if (RunMatsim.getSHPCoordSystem().equals(RunMatsim.getNetworkCoordSystem())) {
            point = geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
        } else {
            point = geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
            try {
                point = (Point) JTS.transform(point, transform);
            } catch (TransformException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<Integer, MultiPolygon> entry : MultiPolygonsMap.entrySet()) {
            if (entry.getValue().contains(point)) {
                return entry.getKey();
            }
        }
        return 0;
    }

    public ArrayList<Id<Person>> firstCheckCoordPersons() {
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan plan = person.getPlans().stream().findAny().orElse(null);
            Activity activity = plan.getPlanElements()
                    .stream()
                    .filter(pl->pl instanceof Activity)
                    .map(pl->(Activity)pl)
                    .filter(act->checkZone(act.getCoord())!=0)
                    .findFirst().orElse(null);
            if (activity!=null) {
                list_observed_agents.add(person.getId());
            }
        }
        return list_observed_agents;
    }

    private Integer timeConverter(double Time){
        if (Time<(24*3600)) {
            return (int) Time /3600;
        } else {
            return (int) (Time%(24*3600))/3600;
        }
    }

    public void checkAndRecordAgent(String eventType, Id<Person> personId, Coord coord, double Time) {
        Integer zone = checkZone(coord);
        if (zone > 0 && checkAgentOnWRD(personId)){
            Integer timeconv = timeConverter(Time);
            if (eventType.equals("Start")) {
                Integer value = StatTable.get("StartPoint"+zone+"WithWRD", timeconv);
                value++;
                StatTable.put("StartPoint"+zone+"WithWRD", timeConverter(Time), value);
            } else if (eventType.equals("End")) {
                Integer value = StatTable.get("EndPoint"+zone+"WithWRD", timeconv);
                value++;
                StatTable.put("EndPoint"+zone+"WithWRD", timeConverter(Time), value);
            }
        } else if (zone > 0 && !checkAgentOnWRD(personId)){
            Integer timeconv = timeConverter(Time);
            if (eventType.equals("Start")) {
                Integer value = StatTable.get("StartPoint"+zone+"WithoutWRD", timeconv);
                value++;
                StatTable.put("StartPoint"+zone+"WithoutWRD", timeConverter(Time), value);
            } else if (eventType.equals("End")) {
                Integer value = StatTable.get("EndPoint"+zone+"WithoutWRD", timeconv);
                value++;
                StatTable.put("EndPoint"+zone+"WithoutWRD", timeConverter(Time), value);
            }
        }
    }
}
