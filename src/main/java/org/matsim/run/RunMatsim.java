/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.run;

import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.IOException;
import java.util.Map;

/**
 * @author nagel
 *
 */

abstract public class RunMatsim {
	/**CUSTOM VARIABLES*/
	private static String WRD_link_forward_direction = "1";
	private static String WRD_link_opposite_direction = "2";
	private static String SHPCoordSystem = "EPSG:4326"; //If WGS84 is used, then specify EPSG:4326
	private static String NetworkCoordSystem = "EPSG:32635";
	/**----------------*/

	private static Scenario scenario;
	private static Config config;
	private static Map<Integer, MultiPolygon> MultiPolygonsMap;
	private static ParserSHP runParserSHP = new ParserSHP();

	public static String getWRD_link_forward_direction() {
		return WRD_link_forward_direction;
	}

	public static String getWRD_link_opposite_direction() {
		return WRD_link_opposite_direction;
	}

	public static String getSHPCoordSystem() {
		return SHPCoordSystem;
	}

	public static String getNetworkCoordSystem() {
		return NetworkCoordSystem;
	}

	public static void main(String[] args) throws IOException, FactoryException {

		if ( args.length==0 || args[0]=="" ) {
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" ) ;
			config.controler().setLastIteration(10);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		} else {
			config = ConfigUtils.loadConfig(args[0]) ;
		}

		scenario = ScenarioUtils.loadScenario(config);

		CoordinateReferenceSystem sourceCRS = CRS.decode(NetworkCoordSystem);
		CoordinateReferenceSystem targetCrs = CRS.decode(SHPCoordSystem);
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCrs);

		//Create an instance of the Controler
		Controler controler = new Controler(scenario);
		MultiPolygonsMap = runParserSHP.run("input/inputForPlans/shapedistricts.shp");
		AgentsStat agentsStat = new AgentsStat(scenario, MultiPolygonsMap, transform);
		System.out.println("Agents under consideration:\n"+agentsStat.firstCheckCoordPersons());

		//Create an instance of the HandlersCollection
		final Handlers my_handlers = new Handlers(agentsStat);

        //Add collection of the events handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(my_handlers);
			}
		});

		//Start simulation
		controler.run();
	}



}