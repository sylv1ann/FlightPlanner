package cz.cuni.mff.java.flightplanner;

import java.util.List;
import cz.cuni.mff.java.flightplanner.plugin.Plugin;

/**
 * The entry point to the program. Launches the application.
 */
public class FlightPlanner {

    /**
     * The entry point to the program.
     * @param args Program parameters.
     */
    public static void main(String[] args) {
        List<Plugin> allPlugins, activePlugins;

        allPlugins = Plugin.loadAllPlugins();
        DialogCenter.welcomeMenu();
        while (true) {
            activePlugins = DialogCenter.choosePlugins(allPlugins);
            if (activePlugins.size() > 0) {
                Plugin.returnActivePlugins(activePlugins, allPlugins);
                Plugin.checkActivePlugins(activePlugins);
                Plugin.startPlugins(activePlugins);
                activePlugins.clear();
            } else break;
        }
    }
}
