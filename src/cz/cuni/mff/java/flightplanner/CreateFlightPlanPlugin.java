package cz.cuni.mff.java.flightplanner;

import java.io.*;

/**
 * The CreateFlightPlanPlugin class is the class that uses both AirportInfoPlugin
 * and WeatherInfoPlugin on top of which uses the waypoints database in order to
 * create the route plan between two specified airports.
 */
public class CreateFlightPlanPlugin implements Plugin {

    OutputStream outStream;

    @Override
    public String name() { return this.getClass().getName(); }

    @Override
    public String description() { return "Complete flight plan creation - UNFINISHED; activate for further details."; }

    @Override
    public String keyword() { return "flight plan"; }

    @Override
    public Integer pluginID() { return 3; }

    @Override
    public int action() {
        System.out.println(Utilities.sectionSeparator("UNFINISHED - MISSING DATABASE"));
        System.out.println("Unfortunately, this plugin could not be finished at this time.");
        System.out.println("Despite searching thoroughly multiple times, I could only find several websites able to provide appropriate waypoint databases:");
        System.out.println("Navigraph, Garmin, Jeppesen or FAA (federal aviation administration) webpages.");
        System.out.println("However, all of these websites are the providers of the official, real world aviation charts");
        System.out.println("and applications for pilots.");
        System.out.println("Therefore, a regular paid subscription and registration are required to at least get access");
        System.out.println("to the aviation charts without even being able to know whether the format would be appropriate");
        System.out.println("for this program purposes.");
        System.out.println(Utilities.sectionSeparator(""));
        System.exit(0);
        return 0;
    }
}
