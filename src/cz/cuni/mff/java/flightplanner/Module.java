package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

abstract class Module {

    protected String name;
    protected String description;
    protected String keyword;
    protected OutputStream outStream;
    protected Integer processNum;
    protected File targetFile;

    abstract void action();

    /*
    @NotNull String getName() {
        return name;
    }

    void setName(@NotNull String newName) {
        name = newName;
    }

    @NotNull String getDescription() {
        return description;
    }

    void setDescription(@NotNull String newDescription) {
        description = newDescription;
    }

    @NotNull OutputStream getOs() {
        return outStream;
    }

    void setOs(@NotNull OutputStream newOutputStream) {
        outStream = newOutputStream;
    }

    @Nullable File getTargetFile() {
        return targetFile;
    }

    void setTargetFile(@Nullable File newFile) {
        targetFile = newFile;
    }

    @NotNull Integer getProcessNum() { return processNum; }

    void setProcessNum(Integer newProcessNum) { processNum = newProcessNum; }

    @Nullable MethodInvocator getAction() { return action; }

    void setAction(MethodInvocator m) { action = m; }

     */

    /**
     * setAllModules method searches for all Module class subclasses defined in
     * package given by package name and adds all these subclasses to
     * DialogCenter.modules list which will determine all possible actions for
     * the Flight Planner.
     *
     * @param packageName = Represents the package from which all Module subclasses will be added to "modules" list.
     */
    @NotNull
    public static ArrayList<Module> setAllModules(@NotNull String packageName) {
        ArrayList<Module> modules = new ArrayList<>();

        String path = packageName.replaceAll("[.]", Matcher.quoteReplacement(File.separator)), name;            //steps to create the absolute path
        List<Class<?>> classes = new ArrayList<>();
        String[] classPathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));        // takes all class paths in which it will search for classes
        for (String classpathEntry : classPathEntries) {
            if (!classpathEntry.endsWith(".jar")) {
                try {
                    File base = new File(classpathEntry + File.separatorChar + path);
                    for (File file : Objects.requireNonNull(base.listFiles())) {
                        name = file.getName();
                        if (name.endsWith(".class")) {
                            name = name.substring(0, name.length() - 6); //removes ".class" suffix
                            Class<?> clazz = Class.forName(packageName + "." + name); //recognises whether the clazz is a subclass of Module class
                            if (clazz.getSuperclass() != null && clazz.getSuperclass().getName().equals(packageName + ".Module")) //if so, it's a module so it takes full name of class which will be added
                                classes.add(Class.forName(packageName + "." + name));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        for (Class<?> cl : classes) { //creates a new instance of every Module subclass
            try {
                Constructor<?>[] constructorsArr = cl.getDeclaredConstructors();
                for (Constructor<?> constructor : constructorsArr) {
                    modules.add((Module) constructor.newInstance());
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
            }
        }
        modules.sort(Comparator.comparingInt(m -> m.processNum)); //sorts by their hardwired processNumbers in order to appear in correct order

        return modules;
    }

    public static void startModules(@NotNull List<Module> active) {
        for (Module mod : active) {
            mod.action();
        }
    }

}

class GetWeatherInfoModule extends Module {

    GetWeatherInfoModule() {
        this.targetFile = null;
        this.name = "GetWeatherInfoModule";
        this.outStream = System.out;
        this.description = "Write information about the weather at chosen airports.";
        this.keyword = "weather";
        this.processNum = 1;
    }

    @Override
    void action() {
        this.outStream = DialogCenter.chooseOutputForm("", true, ""); // TODO: 15/03/2020 remember to check method arguments. Maybe shouhld be default.
        DialogCenter.enterAirports(null); //vloží letiská na ktorých by chcel získať počasie
        // TODO: 06/03/2020 na ziskaných letiskách potom zavolám Downloader a už začnem samotné sťahovanie dát a následne parsovanie
    }

}

class GetAirportInfoModule extends Module {

    GetAirportInfoModule() {
        this.targetFile = null;
        this.name = "GetAirportInfoModule";
        this.outStream = System.out;
        this.description = "Write information about chosen airports.";
        this.keyword = "airport";
        this.processNum = 2;
    }

    /**
     * Handles the action for airport information listing. This method lets the
     * user enter all the required information about the output, then searches
     * for all the relevant information and finally outputs the desired information
     * in the way precised by the user.
     */
    @Override
    void action() {
        boolean auto = DialogCenter.getResponse(null,"Do you want the output to be managed automatically? (Y/n): ", "Y");
        PrintStream pr;
        LinkedList<Airport> found = Airport.searchAirports(null, false);
        this.outStream = null;
        if (auto)
            this.outStream = DialogCenter.chooseOutputForm("", false, null);
        for (Airport apt : found) {
            if (!auto)
                this.outStream = DialogCenter.chooseOutputForm(" for " + apt.icaoCode + " airport", true, apt.icaoCode);
            else {
                if (outStream instanceof FileOutputStream)
                    this.outStream = DialogCenter.setFileOutputStream(false, apt.icaoCode);
            }
            pr = new PrintStream(outStream);
            pr.println("------------ Information about " + apt.icaoCode + " airport. ------------\n" +
                       "The ICAO (International civil aviation organization) code of this airport is: " + apt.icaoCode + "\n" +
                       "The " + apt.aptName + " airport is situated in: " + apt.municipality + ", " + apt.countryCode + ".\n" +
                       "It is a " + apt.cat.name().replace("_", " size ") + ".");
            pr.printf("The coordinates of %s are: %.4f, %.4f and its elevation is: %.0f feet (%.1f meters above sea level).\n",
                      apt.aptName, apt.geoLat, apt.geoLong, apt.elevation, Airport.ftTomConverter(apt.elevation));
            String appendChar = apt.runways.length > 1 ? "s" : "";
            pr.println("This airport has " + apt.runways.length + " runway" + appendChar + ":");
            for (String rwy : apt.runways) {
                String[] fields = rwy.split(",", -1);
                pr.println("Runway identification is: " + fields[5] + "/" + fields[11]);
                pr.printf("    It's length is: %s feet (%.1f meters) and width: %s feet (%.1f meters).\n", fields[0], Airport.ftTomConverter(Double.parseDouble(fields[0])), fields[1], Airport.ftTomConverter(Double.parseDouble(fields[1])));
            }
            pr.println("------------ End of information about " + apt.icaoCode + " airport.------------\n");
            if (outStream instanceof FileOutputStream) {
                pr = System.out;
                pr.println("Information about " + apt.icaoCode + " airport has been successfully written to the file.");
            }
        }
    }
}

class CreateFlightPlanModule extends Module {

    CreateFlightPlanModule() {
        this.targetFile = null;
        this.name = "CreateFlightPlanModule";
        this.outStream = System.out;
        this.description = "Complete flight plan creation.";
        this.keyword = "flight plan";
        this.processNum = 3;
    }

    @Override
    void action() {
        this.outStream = DialogCenter.chooseOutputForm("", true, "");
        // TODO: 15/03/2020 remember to check method arguments. The setting should be default
    }
}

class ExitFlightPlannerModule extends Module {

    ExitFlightPlannerModule() {
        this.targetFile = null;
        this.name = "ExitFlightPlannerModule";
        this.outStream = System.out;
        this.description = "Exit the Flight Planner.";
        this.keyword = "exit";
        this.processNum = 4;
    }

    /**
     * The action implementation for this module is not needed as the program
     * will be terminated before the invocation.
     */
    @Override
    void action() {
    }
}
