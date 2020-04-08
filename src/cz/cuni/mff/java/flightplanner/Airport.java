package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class Airport {

    /**
     * The database containing all the important information about airports from
     * the .csv source.
     */
    private static final LinkedList<Airport> aptDatabase = new LinkedList<>();
    private static boolean aptDatabaseIsSet = false;

    public String[] runways;
    public String icaoCode, aptName, countryCode, municipality;
    public Double elevation;
    public Double geoLat, geoLong;
    public APTCategory cat;


    Airport (String icao, String name, String country, String municipality,
             APTCategory cat, Double geoLat, Double geoLong, Double elev, String[] rwys) {
        this.icaoCode = icao;
        this.aptName = name;
        this.countryCode = country;
        this.municipality = municipality;
        this.cat = cat;
        this.geoLat = geoLat;
        this.geoLong = geoLong;
        this.elevation = elev;
        this.runways = rwys;
    }

    Airport(String icao, String aptName, String municipality) {
        this.icaoCode = icao;
        this.aptName = aptName;
        this.municipality = municipality;
    }

    /**
     * @return Returns Airport.aptDatabase.
     *
     * @see #aptDatabase
     */
    public static LinkedList<Airport> getAptDatabase() {
        if (!aptDatabaseIsSet) setAirportsDatabase();
        return aptDatabase;
    }

    /**
     * Feet to meters converter.
     * @param arg The value in feet to be converted.
     * @return Returns the {@code arg} parameter value in meters.
     */
    public static double ftTomConverter(double arg) {
        return arg * 0.3048;
    }

    /**
     * This method takes a database of airports (either a complete database or
     * its part) and asks the user to enter all the airports he wishes to search
     * for. Then iterates through the list and tries to match every input entry.
     * The method also handles cases, when any or multiple airports match the
     * input and asks user to correct them.
     *
     * @param allApts The list of the airports to be searched in.
     *
     * @param repeatedSearch Re-launches this method in case of an incorrect
     *                       input.
     *
     * @return Returns the list of airports which match user's requests.
     */
    public static @NotNull LinkedList<Airport> searchAirports(@Nullable List<Airport> allApts, boolean repeatedSearch) {

        LinkedList<Airport> result = new LinkedList<>();
        if (!aptDatabaseIsSet) setAirportsDatabase();
        if (allApts == null) allApts = getAptDatabase();

        if(repeatedSearch) {
            if (allApts.size() <= 10) showAirportsList(allApts, "icaoCode, aptName, municipality", true);
            else showAirportsList(allApts, "icaoCode, aptName, municipality", false);
        }

        List<String> aptsToSearch = DialogCenter.enterAirports(null);
        aptsToSearch.removeIf(String::isBlank);

        for (String apt : aptsToSearch) {                                       //iterates through all entries typed by user supposing them being airport codes or names
            LinkedList<Airport> matchedApts = new LinkedList<>();               //creates new list of airports that match current entry of the list

            for (Airport airport : allApts) {
                if (airport.icaoCode.equalsIgnoreCase(apt)        ||
                    airport.aptName.toLowerCase().contains(apt.toLowerCase()) ||
                    airport.municipality.toLowerCase().contains(apt.toLowerCase())) {
                    matchedApts.add(airport);
                }
            }
            LinkedList<Airport> intermediateResult;
            switch (matchedApts.size()) {
                case 0:
                    System.out.println("Error, no airport matched \"" + apt + "\" entry.");
                    if (DialogCenter.getResponse(null,"Do you wish to retype this entry? (Y/n): ", "Y", false)) {
                        if ((intermediateResult = searchAirports(allApts, true)).size() == 1) {
                            result.addAll(intermediateResult);
                        }
                    }
                    break;
                case 1:
                    result.addAll(matchedApts); //adds the !only! matching airport to the result
                    break;
                default:
                    System.out.println("\nThere were multiple matches for entry: \"" + apt + "\".");
                    if (DialogCenter.getResponse("Do you wish to precise more this entry? ",
                                                 "You will be only able to search among the airports that matched \"" + apt + "\". (Y/n): ",
                                                 "Y", false)) {
                        System.out.println("\nEnter 4-letter ICAO code for best precision.");
                        intermediateResult = searchAirports(matchedApts, true);
                        result.addAll(intermediateResult);
                    }
                    break;
            }
        }

        return result;
    }

    /**
     * Prints the list of specified fields in {@code fields} parameter of chosen
     * airports specified in {@code aptsToShow} parameter.
     *
     * @param aptsToShow The list of all airports to be listed.
     *
     * @param fields All the fields of the airports to be printed on stdout.
     *
     * @param autoProceed Either directly lists all the specified fields of chosen
     *                    airports or lets the user decide whether all the airports
     *                    should be shown (used with large number of airports).
     */
    public static void showAirportsList(List<Airport> aptsToShow, @NotNull String fields, boolean autoProceed) {

        if (!aptDatabaseIsSet) setAirportsDatabase(); //ensures that the database is not empty
        if (aptsToShow == null) aptsToShow = getAptDatabase();

        if (autoProceed ||
            DialogCenter.getResponse(null,"Do you want to show all " + aptsToShow.size() + " entries? (Y/n): ", "Y", true)) {
            StringBuilder sb = new StringBuilder();
            for (Airport apt : aptsToShow) {
                for (Field fld  : Airport.class.getFields()) {
                    if ("".equals(fields) || fields.contains(fld.getName())) { //filters all the fields, if "" -> shows every field
                        if (sb.length() > 0) sb.append(", ");
                        try {
                            sb.append(fld.get(apt));
                        } catch (IllegalAccessException ignored) { }
                    }
                }
                System.out.println(sb.toString());
                sb.delete(0, sb.length());
            }
        } else System.out.println("Action aborted.");
    }

    /**
     * Reads the file which contains the information about all available airports
     * and creates the list of these airports which is a {@code static} field of
     * Airport class.
     *
     * @see #aptDatabase
     */
    public static void setAirportsDatabase() {
        String currentDirPath = new File("").getAbsolutePath(), line;
        File src = new File(currentDirPath + File.separator + "MRpLairportsDatabase.csv");
        String[] csvFields;
        double lat, longit, elev;
        int linesRead = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            while ((line = br.readLine()) != null) {
                ++linesRead;
                csvFields = line.split(",", 11);
                elev = parseNum(csvFields[9]);
                lat = parseNum(csvFields[7]);
                longit = parseNum(csvFields[8]);
                APTCategory cat = APTCategory.valueOf(csvFields[6].trim());
                String[] runways = csvFields[10].split("RUNWAY,");
                runways = Arrays.stream(runways).filter(x -> !x.isBlank()).toArray(String[]::new);
                aptDatabase.add(new Airport(csvFields[0], csvFields[1], csvFields[2], csvFields[3], cat, lat, longit, elev, runways));
            }
            if (linesRead != aptDatabase.size()) throw new IOException();
            else aptDatabaseIsSet = true;
        } catch (IOException e) {
            System.out.println("Something went wrong while creating airport database.");
        }
    }

    /**
     * Parses the {@code String} parameter supposed to be double number. Used
     * primarily in order to avoid code repetition. The value is expected to be
     * positive.
     * @param strNum The string to be parsed.
     * @return Double number value of {@code strNum} parameter or -1.0 if something
     *         if something goes wrong.
     */
    private static double parseNum(String strNum) {
        double num;
        try { num = Double.parseDouble(strNum); }
        catch (NumberFormatException e) { num = -1.0; }

        return num;
    }
}