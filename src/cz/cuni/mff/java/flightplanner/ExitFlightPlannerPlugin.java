package cz.cuni.mff.java.flightplanner;

public class ExitFlightPlannerPlugin implements Plugin{

    @Override
    public String name() { return "ExitFlightPlannerModule"; }

    @Override
    public String description() { return "Exit the Flight Planner."; }

    @Override
    public String keyword() { return "exit"; }

    @Override
    public Integer pluginID() { return 4; }

    /**
     * The action implementation for this module is not needed as the program
     * will be terminated before the invocation.
     */
    @Override
    public void action() { }
}
