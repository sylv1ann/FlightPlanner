package cz.cuni.mff.java.flightplanner;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class Airport {

    /**
     * The database containing all the important information about airports from
     * the .csv source.
     */
    private static final Map<String, Airport> aptDatabase = new HashMap<>();
    private static boolean aptDatabaseIsSet = false;

    public String[] runways;
    public String icaoCode, name, countryCode, municipality;
    public Double elevation, geoLat, geoLong;
    public APTCategory cat;


    Airport (String icao, String name, String country, String municipality,
             APTCategory cat, Double geoLat, Double geoLong, Double elev, String[] rwys) {
        this.icaoCode = icao;
        this.name = name;
        this.countryCode = country;
        this.municipality = municipality;
        this.cat = cat;
        this.geoLat = geoLat;
        this.geoLong = geoLong;
        this.elevation = elev;
        this.runways = rwys;
    }

    Airport(String icao, String Name, String municipality) {
        this.icaoCode = icao;
        this.name = Name;
        this.municipality = municipality;
    }


    /**
     * @return Returns Airport.aptDatabase.
     *
     * @see #aptDatabase
     */
    private static Map<String, Airport> getAptDatabase() {
        try {
            if (!aptDatabaseIsSet) setAirportsDatabase();
        } catch (FileNotFoundException e) {
            System.err.println("Database file was not found.");
        }
        return aptDatabase;
    }

    /**
     * The method prompts the user to enter all the airports to be searched for
     * and creates a list of provided strings which are separated by any non-letter
     * character.
     *
     * @param initMsg The message to be printed prior to the prompt. If null,
     *                nothing is printed.
     *
     * @return Returns the {@code non-null} list of strings supposed to be airports'
     *         ICAO codes, municipalities or airports names, which will be searched
     *         for in the database.
     */
    private static @NotNull List<String> enterAirports(@Nullable String initMsg) {
        List<String> result = new LinkedList<>();
        String[] fields;

        if (initMsg != null) {
            System.out.println(initMsg);
        }
        do {
            System.out.print("Please enter all the airports you wish to search and separate them with any non-letter character: ");
            fields = DialogCenter.getInput(false, false).split("[^A-Za-z]+");
            result.addAll(Arrays.asList(fields));
        } while (DialogCenter.getResponse(null,"Do you wish to enter more airports? %OPT: ", "Y", true));
        return result;
    }

    /**
     * This method takes a database of airports (either a complete database or
     * its part) and asks the user to enter all the airports he wishes to search
     * for. Then iterates through the list and tries to match every input entry.
     * The method also handles cases, when any or multiple airports match the
     * input and asks user to correct them.
     *
     * @param airportsList The list of the airports to be searched in. If {@code null},
     *                then the stored database is retrieved and used in the method.
     *
     * @param repeatedSearch Re-launches this method in case of an incorrect
     *                       input.
     *
     * @return Returns a {@code NotNull} list of airports which match user's requests.
     */
    static @NotNull List<Airport> searchAirports(@Nullable Collection<Airport> airportsList,
                                                 @Nullable List<String> predefinedApt,
                                                 boolean repeatedSearch) {

        List<Airport> result = new LinkedList<>();
        try {
            if (!aptDatabaseIsSet) setAirportsDatabase();
            if (airportsList == null) airportsList = getAptDatabase().values();

            if (predefinedApt == null) {
                if (repeatedSearch) {
                    showAirportsList(airportsList, "icaoCode, aptName, municipality", airportsList.size() <= 10);
                }

                List<String> aptsToSearch = enterAirports(null);
                aptsToSearch.removeIf(String::isBlank);

                for (String apt : aptsToSearch) {                                       //iterates through all entries typed by user supposing them being airport codes or names
                    List<Airport> matchedApts = new LinkedList<>();                     //creates new list of airports that match current entry of the list

                    for (Airport airport : airportsList) {
                        if (airport.icaoCode.equalsIgnoreCase(apt) ||
                                airport.name.toLowerCase().contains(apt.toLowerCase()) ||
                                airport.municipality.toLowerCase().contains(apt.toLowerCase())) {
                            matchedApts.add(airport);
                        }
                    }
                    List<Airport> intermediateResult;
                    switch (matchedApts.size()) {
                        case 0:
                            System.out.printf("Error, no airport matched \"%s\" entry.%n", apt);
                            if (DialogCenter.getResponse(null,
                                                         "Do you wish to retype this entry? %OPT: ",
                                                         "Y",
                                                         false)) {
                                intermediateResult = searchAirports(airportsList,
                                                                    null,
                                                                    true);
                                if (intermediateResult.size() == 1) {
                                    result.addAll(intermediateResult);
                                }
                            }
                            break;
                        case 1:
                            result.addAll(matchedApts); //adds the !only! matching airport to the result
                            break;
                        default:
                            System.out.printf("There were multiple matches for entry: \"%s\".%n", apt);
                            if (DialogCenter.getResponse("Do you wish to precise more this entry? ",
                                    "You will be only able to search among the airports that matched \"%APT\". %OPT: "
                                            .replace("%APT", apt),
                                    "Y",
                                    false)
                            ) {
                                System.out.println("Enter 4-letter ICAO code for best precision.");
                                intermediateResult = searchAirports(matchedApts, null,true);
                                result.addAll(intermediateResult);
                            }
                            break;
                    }
                }
            } else {
                for (String predefined : predefinedApt) {
                    Airport foundAirport = aptDatabase.get(predefined);
                    if (foundAirport != null) result.add(foundAirport);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Database file was not found.");
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
    static void showAirportsList(Collection<Airport> aptsToShow, @NotNull String fields, boolean autoProceed) {

        try {
            if (!aptDatabaseIsSet) setAirportsDatabase(); //ensures that the database is not empty
            if (aptsToShow == null) aptsToShow = getAptDatabase().values();

            if (autoProceed ||
                    DialogCenter.getResponse(null,
                            "Do you want to show all %COUNT entries? %OPT: "
                                    .replace("%COUNT", String.valueOf(aptsToShow.size())),
                            "Y",
                            true)
            ) {
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
        } catch (FileNotFoundException e) {
            System.err.println("Database file was not found.");
        }
    }

    /**
     * Reads the file which contains the information about all available airports
     * and creates the list of these airports which is a {@code static} field of
     * Airport class.
     *
     * @see #aptDatabase
     */
    static void setAirportsDatabase() throws FileNotFoundException {

        String line;
        File src = FilesHandler.findResource("MRpLairportsDatabase.csv");
        String[] csvFields;
        double lat, longit, elev;
        int linesRead = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            while ((line = br.readLine()) != null) {
                ++linesRead;
                csvFields = line.split(",", 11);
                elev = Utilities.parseNum(csvFields[9]);
                lat = Utilities.parseNum(csvFields[7]);
                longit = Utilities.parseNum(csvFields[8]);
                APTCategory cat = APTCategory.valueOf(csvFields[6].trim());
                String[] runways = csvFields[10].split("RUNWAY,");
                runways = Arrays.stream(runways)
                                .filter(x -> !x.isBlank())
                                .toArray(String[]::new);
                aptDatabase.put(csvFields[0], new Airport(csvFields[0],
                                                          csvFields[1],
                                                          csvFields[2],
                                                          csvFields[3],
                                                          cat,
                                                          lat,
                                                          longit,
                                                          elev,
                                                          runways));
            }
            if (linesRead != aptDatabase.size()) throw new IOException();
            else aptDatabaseIsSet = true;
        } catch (FileNotFoundException e) {
            System.err.println("Something went wrong while creating airport database.");
            throw new FileNotFoundException();
        }
        catch (IOException ex) {
            System.err.println("One or more database lines were not imported.");
        }
    }

}
