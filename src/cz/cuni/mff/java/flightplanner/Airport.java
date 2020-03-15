package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class Airport {

    private static LinkedList<Airport> aptDatabase = new LinkedList<>();
    private static boolean aptDatabaseIsSet = false;


    public String[] runways;
    public String icaoCode, aptName, countryCode, municipality;
    public Double elevation;
    public Double geoLat, geoLong;
    public APTCategory cat;


    Airport (String icao, String name, String country, String municipality, APTCategory cat, Double geoLat, Double geoLong, Double elev, String[] rwys) {
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

    public static LinkedList<Airport> getAptDatabase() {
        if (!aptDatabaseIsSet) setAirportsDatabase();
        return aptDatabase;
    }

    public static double ftTomConverter(double arg) {
        return arg * 0.3048;
    }

    /**
     * This method takes a database of airports (either a complete database or
     * its part) and asks the user to enter all the airports he wishes to search
     * for. Then iterates through the list and tries to match every input entry.
     * The method also handles cases, when any or multiple airports match the
     * input and asks user to correct them.
     * @param allApts = The list of the airports to be searched among.
     * @param repeatedSearch = Re-launches this method in case of an incorrect input
     * @return Returns a list of airports which match user's requests.
     */
    public static @NotNull LinkedList<Airport> searchAirports(@Nullable LinkedList<Airport> allApts, boolean repeatedSearch) {

        LinkedList<Airport> result = new LinkedList<>();
        if (!aptDatabaseIsSet) setAirportsDatabase();
        if (allApts == null) allApts = getAptDatabase();

        if(repeatedSearch) {
            if (allApts.size() <= 10) showAirportsList(allApts, "icaoCode, aptName, municipality", true);
            else showAirportsList(allApts, "icaoCode, aptName, municipality", false);
        }

        List<String> aptsToSearch = DialogCenter.enterAirports(null);
        aptsToSearch.removeIf(String::isBlank);

        for (String apt : aptsToSearch) { //iterates through all entries typed by user supposing them being airport codes or names
            LinkedList<Airport> matchedApts = new LinkedList<>(); //creates new list of airports that match current entry of the list

            for (Airport airport : allApts) {
                if (airport.icaoCode.equals(apt.toUpperCase())  ||
                    airport.aptName.toLowerCase().contains(apt.toLowerCase()) ||
                    airport.municipality.toLowerCase().contains(apt.toLowerCase())) {
                    //System.out.println(airport.icaoCode + " " + airport.municipality + " " + airport.aptName);
                    matchedApts.add(airport);
                }
            }
            LinkedList<Airport> intermediateResult;
            switch (matchedApts.size()) {
                case 0:
                    System.out.println("Error, no airport matched \"" + apt + "\" entry.");
                    if (DialogCenter.getResponse("Do you wish to retype this entry? (Y/n): ", "Y")) {
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
                    if (DialogCenter.getResponse("Do you wish to precise more this entry? " +
                                                  "You will be only able to search among the airports that matched \"" + apt + "\". (Y/n): ", "Y")) {
                        System.out.println("Enter 4-letter ICAO code for best precision:");
                        intermediateResult = searchAirports(matchedApts, true);
                        if (intermediateResult.size() == 1) result.addAll(intermediateResult);
                    }
                    break;
            }
        }
        //if (!repeatedSearch) result.forEach(x -> System.out.println(x.icaoCode + " " + x.municipality + " " + x.aptName));
        return result;
    }

    public static void showAirportsList(LinkedList<Airport> aptsToShow, @NotNull String fields, boolean proceed) {

        if (!aptDatabaseIsSet) setAirportsDatabase(); //ensures that the database is not empty
        if (aptsToShow == null) aptsToShow = getAptDatabase();

        //proceed arg -> direct listing of airport entries in the list, if false -> the user decides whether to show the entries
        if (proceed || DialogCenter.getResponse("The list of all airports contains " + aptsToShow.size() + " entries.\n" +
                "Are you sure you want to list all these airports? (Y/n): ", "Y")) {

            StringBuilder sb = new StringBuilder();
            for (Airport apt : aptsToShow) {
                for (Field fld  : Airport.class.getFields()) {
                    if ("".equals(fields) || fields.contains(fld.getName())) { //filters all the fields, if "" -> shows every field
                        try {
                            sb.append(fld.get(apt)).append(",");
                        } catch (IllegalAccessException ignored) { }
                    }
                }
                System.out.println(sb.toString());
                sb.delete(0, sb.length());
            }
        } else System.out.println("Action aborted.");
    }

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

    private static double parseNum(String strNum) {
        double num;
        try { num = Double.parseDouble(strNum); }
        catch (NumberFormatException e) { num = -1.0; }

        return num;
    }
}