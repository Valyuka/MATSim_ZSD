package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class Handlers implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler,
        PersonDepartureEventHandler {

    private AgentsStat agentsStat;
    private String WRD_link_forward_direction;
    private String WRD_link_opposite_direction;

    //Class constructor
    Handlers (AgentsStat agentsStat){
        this.agentsStat = agentsStat;
        this.WRD_link_forward_direction = RunMatsim.getWRD_link_forward_direction();
        this.WRD_link_opposite_direction = RunMatsim.getWRD_link_opposite_direction();
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (agentsStat.getListObservedAgents().contains(event.getPersonId())) {
            agentsStat.RecordToMapStatBook(
                    "Start",
                    event.getPersonId(),
                    agentsStat.getNetwork().getLinks().get(event.getLinkId()).getCoord()
            );
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (agentsStat.getListObservedAgents().contains(event.getPersonId())) {
            agentsStat.RecordToMapStatBook(
                    "End",
                    event.getPersonId(),
                    agentsStat.getNetwork().getLinks().get(event.getLinkId()).getCoord()
            );
        }
        agentsStat.reportConstructor(event.getPersonId(), event.getTime());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        //If the car is traveling along the link, it is added to the list
        if (event.getLinkId().equals(Id.create(WRD_link_forward_direction, Link.class))) {
            agentsStat.RecordToMapStatBook("ForwardAlongWRD", event.getVehicleId());
        } else if (event.getLinkId().equals(Id.create(WRD_link_opposite_direction, Link.class))) {
            agentsStat.RecordToMapStatBook("OppositeAlongWRD", event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        // Unfortunately necessary since vehicle departures are not uniformly registered
        Id<Vehicle> vehId = Id.create( event.getPersonId(), Vehicle.class );
    }

    @Override
    public void reset(int iteration) {

    }
}
