package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

public interface Plugin {

    /**
     * loadAllPlugins method searches for all Plugin subclasses defined in
     * package given by package name and adds all these subclasses to
     * DialogCenter.allPlugs list which will determine all possible actions for
     * the Flight Planner.
     *
     * @param packageName Represents the package from which all Plugins will
     *                    be added to "DialogCenter.allPlugs" list.
     * @see DialogCenter
     */
    @NotNull
    static List<Plugin> loadAllPlugins(@NotNull String packageName) {
        ArrayList<Plugin> plugins = new ArrayList<>();

        String path = packageName.replaceAll("[.]", Matcher.quoteReplacement(File.separator)), name; //steps to create the absolute path
        List<Class<?>> allPlugins = new ArrayList<>();
        String[] classPathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator")); // takes all class paths in which it will search for classes
        for (String classpathEntry : classPathEntries) {
            if (!classpathEntry.endsWith(".jar")) {
                try {
                    File base = new File(classpathEntry + File.separatorChar + path);
                    for (File file : Objects.requireNonNull(base.listFiles())) {
                        name = file.getName();
                        if (name.endsWith(".class")) {
                            name = name.substring(0, name.length() - 6); //removes ".class" suffix
                            Class<?> clazz = Class.forName(packageName + "." + name); //recognises whether the clazz is a subclass of Module class
                            if (Plugin.class.isAssignableFrom(clazz))
                              allPlugins.add(Class.forName(packageName + "." + name));
                        }
                    }
                } catch (Exception ignored) { }
            }
        }

        for (Class<?> cl : allPlugins) { //creates a new instance of every Mode subclass
            try {
                Constructor<?>[] constructorsArr = cl.getDeclaredConstructors();
                for (Constructor<?> constructor : constructorsArr) {
                    plugins.add((Plugin) constructor.newInstance());
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ignored) { }
        }
        plugins.sort(Comparator.comparingInt(Plugin::pluginID)); //sorts by their hardwired processNumbers in order to appear in correct order

        return plugins;
    }

    /**
     * Launches every active plugin.
     *
     * @param active List of all plugins to be launched.
     */
    static void startPlugins(@NotNull List<Plugin> active) {
        for (Plugin mod : active) {
            mod.action();
        }
        System.out.println("Everything finished just fine.");
    }

    String name();

    String description();

    String keyword();

    Integer pluginID();

    void action();
}
