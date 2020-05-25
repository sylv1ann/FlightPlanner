package cz.cuni.mff.java.flightplanner.plugin;

import java.util.*;

import cz.cuni.mff.java.flightplanner.util.NotNull;
import cz.cuni.mff.java.flightplanner.util.Utilities;

/**
 * {@code Plugin} interface represents an independent part of the program performing
 * its own {@link #action()}. This interface provides all the important attributes
 * of a plugin object. The interface also includes several static methods which
 * do not have to be tied with the implementation of the interface.
 *
 */
public interface Plugin {

    /**
     * loadAllPlugins method searches for all Plugin implementations defined in
     * the plugin package and adds new class instance for each of the implementations
     * to the list which will determine all possible actions for the Flight Planner.
     *
     * @see cz.cuni.mff.java.flightplanner.DialogCenter
     *
     * @return The list of all available plugins in the given package.
     */
    static @NotNull List<Plugin> loadAllPlugins() {
        ArrayList<Plugin> plugins = new ArrayList<>();

        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class);

        for (Plugin plugin : serviceLoader) {
            plugins.add(plugin);
        }
        plugins.sort(Comparator.comparingInt(Plugin::pluginID));

        return plugins;
    }

    /**
     * Launches every active plugin and prints the concluding message output
     * specified by the exit code of currently performed action.
     *
     * @param active List of all plugins to be launched.
     */
    static void startPlugins(@NotNull List<Plugin> active) {
        boolean fine = true;
        for (Plugin mod : active) {
            System.out.println(Utilities.sectionSeparator(mod.keyword()));
            if (mod.action() != 0) {
                System.err.println("An error occured during the execution of the %ACTION"
                                   .replace("%ACTION",mod.name()));
                fine = false;
            }
            System.out.println(Utilities.sectionSeparator("End of %MOD"
                                                                     .replace("%MOD",mod.keyword()))
                                                         );
        }
        if (fine) System.out.println("Every action finished just fine.");
        active.clear();
    }

    /**
     * Returns the deleted plugins into the available plugins list.
     *
     * @param fromList  The plugins to be added to the {@code toList} parameter.
     * @param toList    The destination list for the {@code fromList} parameter.
     */
    static void returnActivePlugins(List<Plugin> fromList, List<Plugin> toList) {
        toList.addAll(fromList);
        toList.sort(Comparator.comparingInt(Plugin::pluginID));
    }

    /**
     * This method checks whether "exit" plugin is included in the list of plugins
     * to perform. If so, then this plugin is activated immediately.
     * Otherwise, the user is informed about the plugins that are going to be
     * performed.
     * @param active The list of modules which have been activated.
     */
    static void checkActivePlugins(@NotNull List<Plugin> active) {
        if (active.stream()
                  .anyMatch(x -> x.getClass().isAssignableFrom(ExitFlightPlannerPlugin.class))) {
            if (new ExitFlightPlannerPlugin().action() == 0)
                active.removeIf(x -> x.getClass().isAssignableFrom(ExitFlightPlannerPlugin.class));
        }

        if (active.size() > 0) {
            System.out.println("The list of operations which will be performed: ");
            for (Plugin mod : active) {
                System.out.printf("(%d) : %s%n", mod.pluginID(), mod.description());
            }
        }
        System.out.printf("%n");
    }

    /**
     * @return The name of the class implementing this interface.
     */
    String name();

    /**
     * @return A brief description of the class which implements this interface.
     */
    String description();

    /**
     * @return The keyword of the class which implements this interface.
     */
    String keyword();

    /**
     * @return A unique ID number of the class which implements this interface.
     */
    Integer pluginID();

    /**
     * The main method of the implementation of the {@code Plugin} interface.
     * Represents the operation to be performed in order to create a part of the
     * flight plan.
     *
     * @return The exit code of the action. Any non-zero number means that an issue
     *         was encountered during the execution of the action.
     */
    int action();
}
