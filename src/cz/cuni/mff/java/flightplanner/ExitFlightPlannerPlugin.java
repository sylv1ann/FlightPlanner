package cz.cuni.mff.java.flightplanner;

public class ExitFlightPlannerPlugin implements Plugin{

    @Override
    public String name() { return this.getClass().getName(); }

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
    public int action() {
        try {
            if (DialogCenter.getResponse(null,
                                         "Are you sure to exit the planner? %OPT: ",
                                         "Y",
                                         true)) {
                System.out.println("Goodbye. See you next time in Flight Planner.");
                Thread.sleep(300);
                System.exit(0);
            }
        } catch (InterruptedException ex) {
            return 1;
        }
        return 0;
    }
}
