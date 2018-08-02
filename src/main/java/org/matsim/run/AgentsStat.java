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

import java.util.*;

public class AgentsStat {
    private Network network;
    private Scenario scenario;
    private MathTransform transform;
    private Integer NumberOfPolygons;
    private static ArrayList<Id<Person>> list_observed_agents = new ArrayList<>();
    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    private Map<Integer, MultiPolygon> MultiPolygonsMap;



    private Table<String, Integer, Integer> ReportTable = HashBasedTable.create();
    private HashMap<String, HashSet<Id>> BookOfEvents = new HashMap<String, HashSet<Id>>();

    //Class constructor
    AgentsStat(Scenario scenario, Map<Integer, MultiPolygon> MultiPolygonsMap, MathTransform transform) {
        this.scenario = scenario;
        this.network = scenario.getNetwork();
        this.transform = transform;
        this.MultiPolygonsMap = MultiPolygonsMap;
        this.NumberOfPolygons = MultiPolygonsMap.size();

        //Initialization StatTable in the class constructor
        for (Integer i = 1; i <= NumberOfPolygons; i++) {
            BookOfEvents.put(String.format("StartPoint%d", i), new HashSet<Id>());
            BookOfEvents.put(String.format("EndPoint%d", i), new HashSet<Id>());
        }
        BookOfEvents.put(String.format("ForwardAlongWRD"), new HashSet<Id>());
        BookOfEvents.put(String.format("OppositeAlongWRD"), new HashSet<Id>());
    }

    //Full remove agent from BookOfEvents
    private void removeAgentFromBook(Id id){
        for (String key:BookOfEvents.keySet()) {
            BookOfEvents.get(key).remove(id);
        }
    }

    //Network Getter
    public Network getNetwork() {
        return network;
    }

    //List of Observed Agents Getter
    public ArrayList<Id<Person>> getListObservedAgents() {
        return list_observed_agents;
    }

    public Table<String, Integer, Integer> getReportTable() {
        return ReportTable;
    }

    private boolean checkAgentOnWRD(Id<Person> personId) {
        if (BookOfEvents.get("ForwardAlongWRD").contains(personId) | BookOfEvents.get("OppositeAlongWRD").contains(personId)) {
            return true;
        }
        return false;
    }

    private Integer checkZone(Coord coord) {
        Point point;
        //If SHP Coordinate System equals Network Coordinate System then just create point WITHOUT transformation
        if (RunMatsim.getSHPCoordSystem().equals(RunMatsim.getNetworkCoordSystem())) {
            point = geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
        //Else create point WITH transformation Network Coodinate System to SHP Coordinate System
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
            return (int) Time/3600;
        } else {
            return (int) (Time%(24*3600))/3600;
        }
    }

    private void RecordToReportTable(String keyName, Integer Time) {
        if(!ReportTable.containsRow(keyName) ){
            for (Integer i = 0; i<=23; i++) {
                ReportTable.put(keyName, i, 0);
            }
        }
        Integer value = ReportTable.get(keyName, Time);
        value++;
        ReportTable.put(keyName, Time, value);
    }

    public void RecordToMapStatBook(String eventType, Id<Person> personId, Coord coord) {
        Integer zone = checkZone(coord);
        if (zone !=0) {
            if (eventType.equals("Start")) {
                BookOfEvents.get(String.format("StartPoint%d", zone)).add(personId);
            } else if (eventType.equals("End")) {
                BookOfEvents.get(String.format("EndPoint%d", zone)).add(personId);
            }
        }
    }

    public void RecordToMapStatBook(String eventType, Id<Vehicle> vehicleId) {
        if (BookOfEvents.containsKey(eventType)){
            BookOfEvents.get(eventType).add(vehicleId);
        }
    }
    /**----
    This method allows you to create various reports from BookOfEvents
     --*/
    public void reportConstructor(Id id, double Time){
        //Подсчет числа агентов, которые стартовали в точке 1 + ЗСД
        if(BookOfEvents.get("StartPoint1").contains(id) & checkAgentOnWRD(id)) {
            RecordToReportTable("StartPoint1WithWRD", timeConverter(Time));
        }

        //Подсчет числа агентов, которые закончили в точке 2 + ЗСД
        if(BookOfEvents.get("EndPoint1").contains(id) & checkAgentOnWRD(id)) {
            RecordToReportTable("EndPoint1WithWRD", timeConverter(Time));
        }

        //Подсчет числа агентов, которые закончили в точке 1 или 2 + ЗСД
        if((BookOfEvents.get("EndPoint1").contains(id) | BookOfEvents.get("EndPoint2").contains(id)) & checkAgentOnWRD(id)) {
            RecordToReportTable("EndPoint1Or2WithWRD", timeConverter(Time));
        }

        //Необходимо вызывать в конце этот метод, чтобы удалять агентов занесенных в отчет
        removeAgentFromBook(id);
    }
}
