package cz.cuni.mff.java.flightplanner;

import java.io.OutputStream;

public class CreateFlightPlanPlugin implements Plugin {

    @Override
    public String name() { return "CreateFlightPlanModule"; }

    @Override
    public String description() { return "Complete flight plan creation."; }

    @Override
    public String keyword() { return "flight plan"; }

    @Override
    public Integer pluginID() { return 3; }

    @Override
    public void action() {
        OutputStream outStream = null;
        //outStream = DialogCenter.chooseOutputForm("", true, "");
        // TODO: 15/03/2020 remember to check method arguments. The setting should be default
    }
}
