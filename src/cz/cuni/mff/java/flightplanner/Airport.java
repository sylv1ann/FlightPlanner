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
    //LinkedList<String> runways = new LinkedList<>();
    public String icaoCode, aptName, countryCode, municipality;
    public Double elevation;
    public Double geoLat, geoLong;
    public APTCategory cat;


    Airport (String icao, String name, String country, String municipality, APTCategory cat, Double geoLat, Double geoLong, Double elev) {
        this.icaoCode = icao;
        this.aptName = name;
        this.countryCode = country;
        this.municipality = municipality;
        this.cat = cat;
        this.geoLat = geoLat;
        this.geoLong = geoLong;
        this.elevation = elev;
    }

    public static LinkedList<Airport> getAptDatabase() {
        if (!aptDatabaseIsSet) setAirportsDatabase();
        return aptDatabase;
    }

    @NotNull
    public static LinkedList<Airport> searchAirports(@Nullable LinkedList<Airport> allApts, boolean repeatedSearch) {

        LinkedList<Airport> result = new LinkedList<>();
        if (!aptDatabaseIsSet) setAirportsDatabase();

        if (allApts == null) allApts = getAptDatabase();

        if(repeatedSearch && allApts.size() <= 40) showAirportsList(allApts, "icaoCode, aptName, municipality");

        List<String> aptToSearch = DialogCenter.enterAirports(null);

        for (String apt : aptToSearch) { //iterates through all entries typed by user supposing them being airport codes or names
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
                    System.out.println("There were multiple matches for entry: \"" + apt + "\".");
                    if (DialogCenter.getResponse("Do you wish to precise more this entry? (Y/n): ", "Y")) {
                        System.out.println("Enter 4-letter ICAO code for best precision.");
                        intermediateResult = searchAirports(matchedApts, true);
                        if (intermediateResult.size() == 1) result.addAll(intermediateResult);
                    }
                    break;
            }
        }
        if (!repeatedSearch) result.forEach(x -> System.out.println(x.icaoCode + " " + x.municipality + " " + x.aptName));
        return result;
    }

    public static void showAirportsList(LinkedList<Airport> aptsToShow, @NotNull String fields) {

        if (!aptDatabaseIsSet) setAirportsDatabase();
        if (aptsToShow == null) aptsToShow = getAptDatabase();

        if (DialogCenter.getResponse("The list of all airports contains " + aptsToShow.size() + " entries.\n" +
                "Are you sure you want to proceed? (Y/n): ", "Y")) {

            StringBuilder sb = new StringBuilder();
            for (Airport apt : aptsToShow) {
                for (Field fld : Airport.class.getFields()) {
                    if ("".equals(fields) || fields.contains(fld.getName())) {
                        try {
                            sb.append(fld.get(apt)).append(" : ");
                        } catch (IllegalAccessException ignored) { }
                    }
                }
                System.out.println(sb.toString());
                sb.delete(0, sb.length());
            }
        } else System.out.println("Abort Action.");
    }

    public static void setAirportsDatabase() {
        String currentDirPath = new File("").getAbsolutePath(), line;
        File src = new File(currentDirPath + File.separator + "airports1.csv");
        String[] csvFields;
        double lat = 0.0, longit = 0.0, elev = 0.0;
        int linesRead = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            while ((line = br.readLine()) != null) {
                ++linesRead;
                csvFields = line.split(",", -1);
                elev = parseNum(csvFields[9]);
                lat = parseNum(csvFields[7]);
                longit = parseNum(csvFields[8]);
                APTCategory cat = APTCategory.valueOf(csvFields[6].trim());
                aptDatabase.add(new Airport(csvFields[0], csvFields[1], csvFields[2], csvFields[3], cat, lat, longit, elev));
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