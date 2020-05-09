package cz.cuni.mff.java.flightplanner;

import java.io.*;

public class CreateFlightPlanPlugin implements Plugin {

    OutputStream outStream;

    @Override
    public String name() { return this.getClass().getName(); }

    @Override
    public String description() { return "Complete flight plan creation."; }

    @Override
    public String keyword() { return "flight plan"; }

    @Override
    public Integer pluginID() { return 3; }

    @Override
    public int action() {
        return 0;
        //outStream = DialogCenter.chooseOutputForm("", true, "");
        // TODO: 15/03/2020 remember to check method arguments. The setting should be default
    }
}
