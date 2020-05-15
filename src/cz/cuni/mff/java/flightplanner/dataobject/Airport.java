package cz.cuni.mff.java.flightplanner.dataobject;

import cz.cuni.mff.java.flightplanner.DialogCenter;
import cz.cuni.mff.java.flightplanner.util.*;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

/**
 * The class which represent an Airport object and all the important available data
 * about it.
 */
public class Airport {

    /**
     * The database containing all the important information about airports from
     * the .csv source.
     */
    private static final Map<String, Airport> aptDatabase = new HashMap<>();
    private static boolean aptDatabaseIsSet = false;

    private final List<Runway> runways;
    private final String icaoCode, name, municipality;
    private final String countryCode;
    private final Double elevation, geoLat, geoLong;
    private final APTCategory cat;

    private Airport(String icao, String name, String country, String municipality,
                    APTCategory cat, Double geoLat, Double geoLong, Double elev, List<Runway> rwys) {
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

    /**
     * @return The value of the elevation of the object.
     */
    public Double getElevation() {
        return elevation;
    }

    /**
     * @return The ICAO code of the object.
     */
    public String getIcaoCode() {
        return icaoCode;
    }

    /**
     * @return The geographic latitude.
     */
    public Double getGeoLat() {
        return geoLat;
    }

    /**
     * @return The geographic longitude.
     */
    public Double getGeoLong() {
        return geoLong;
    }

    /**
     * @return The country code of the airport.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @return The city which the airport belongs to.
     */
    public String getMunicipality() {
        return municipality;
    }

    /**
     * @return The name of the airport.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The list of the runways of the airport.
     */
    public List<Runway> getRunways() {
        return runways;
    }

    /**
     * @return The category of the airport.
     */
    public APTCategory getCat() {
        return cat;
    }

    /**
     * @return Returns the airports database dictionary.
     *
     * @see #aptDatabase
     */
    private static Map<String, Airport> getAptDatabase() {
        int exitCode;
        if (!aptDatabaseIsSet) {
            exitCode = setAirportsDatabase();
            if (exitCode == 0) return aptDatabase;
            else return null;
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
            System.out.println("Please enter all the airports you want to search for and separate them with any non-letter character.");
            System.out.print("You can search by ICAO code (e.g. LKPR), by city name (Prague) or the airport name (e.g \"Havel\" for Prague airport): ");
            fields = DialogCenter.getInput(false, false)
                                 .split("[^A-Za-z]+");
            result.addAll(Arrays.asList(fields));
        } while (DialogCenter.getResponse(null,
                                          "Do you wish to enter more airports? %OPT: ",
                                          "Y",
                                          true));
        return result;
    }

    /**
     * This method takes a database of airports (either a complete database or
     * its subset) and asks the user to enter all the airports he wishes to search
     * for. Then iterates through the list and tries to match every input entry.
     * The method also handles cases, when any or multiple airports match the
     * input and asks user to correct/precise them.
     *
     * @param airportsList The list of the airports to be searched in. If {@code null},
     *                     then the stored database is retrieved and used in the method.
     *
     * @param predefinedApts The fixed list of ICAO codes of the airports in the
     *                      database. Used only to get the data about airport(s)
     *                      for further processing by program.
     *
     * @param repeatedSearch Indicates whether the method was relaunched because
     *                       of an incorrect input.
     *
     * @param onlyICAO      If true, only the airports matching the ICAO code will
     *                      be accepted.
     *
     * @return Returns a list of airports which match user's requests, or
     *         {@code null} if a database issue is detected.
     */
    public static List<Airport> searchAirports(@Nullable List<Airport> airportsList,
                                               @Nullable List<String> predefinedApts,
                                               boolean repeatedSearch,
                                               boolean onlyICAO) {
        List<Airport> result = new LinkedList<>();
        if (!aptDatabaseIsSet && setAirportsDatabase() != 0) return null;

        if (airportsList == null)
            airportsList = new LinkedList<>(
                    Objects.requireNonNull(getAptDatabase()).values());
            // getAptDatabase() returns null only if setAirportsDatabase() fails
            // and returns non-zero exit code. However, in such a case this method
            // does not even get to this invocation and already returns null

        if (predefinedApts == null) {
            if (repeatedSearch) {
                int exitCode = showAirportsList(airportsList,
                                                "icaoCode,name,municipality",
                                                airportsList.size() <= 10);
                if (exitCode != 0) return null;
            }

            List<String> aptsToSearch = enterAirports(null);
            aptsToSearch.removeIf(String::isBlank);
            airportsList.sort(Comparator.comparing(o -> o.icaoCode));

            for (String apt : aptsToSearch) {                                   //iterates through all entries typed by user supposing them being airport codes or names
                List<Airport> matchedApts = new LinkedList<>();                 //creates new list of airports that match current entry of the list

                for (Airport airport : airportsList) {
                    if (airport.icaoCode.equalsIgnoreCase(apt)) {
                        matchedApts.add(airport);
                        break;
                    } else {
                        if (!onlyICAO &&
                            (airport .name        .toLowerCase().contains(apt.toLowerCase()) ||
                             airport .municipality.toLowerCase().contains(apt.toLowerCase()))
                           ) {
                            matchedApts.add(airport);
                        }
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
                                                                true,
                                                                false);
                            if (intermediateResult != null &&
                                intermediateResult.size() == 1)
                                result.addAll(intermediateResult);
                        }
                        break;
                    case 1:
                        //adds the !only! matching airport to the result
                        result.addAll(matchedApts);
                        break;
                    default:
                        System.out.printf("Multiple matches were found for entry: \"%s\".%n", apt);
                        if (DialogCenter.getResponse("Do you wish to precise more this entry? ",
                                "You will be only able to search among the airports that matched \"%APT\". %OPT: "
                                        .replace("%APT", apt),
                                "Y",
                                false)
                        ) {
                            System.out.println("Only 4-letter ICAO codes will be accepted.");
                            intermediateResult = searchAirports(matchedApts,
                                                                null,
                                                                true,
                                                                true);
                            result.addAll(Objects.requireNonNull(intermediateResult));
                        }
                        break;
                }
            }
        } else {
            for (String predefined : predefinedApts) {
                Airport foundAirport = aptDatabase.get(predefined);
                if (foundAirport != null) result.add(foundAirport);
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
     * @return The exit code of the action. Any non-zero code means that an issue
     *         has occurred.
     */
    private static int showAirportsList(Collection<Airport> aptsToShow, @NotNull String fields, boolean autoProceed) {
        int dbSetExitCode;
        if (!aptDatabaseIsSet && (dbSetExitCode = setAirportsDatabase()) != 0)
            return dbSetExitCode;

        if (aptsToShow == null) aptsToShow = Objects.requireNonNull(getAptDatabase()).values();
        // getAptDatabase() returns null only if setAirportsDatabase() fails
        // and returns non-zero exit code. However, in such a case this method
        // does not even get to this invocation and already return null

        if (autoProceed ||
            DialogCenter.getResponse(null,
                                     "Do you want to show all %COUNT entries? %OPT: "
                                             .replace("%COUNT", String.valueOf(aptsToShow.size())),
                                     "Y",
                                     true)
            ) {
            System.out.println(Utilities.sectionSeparator("Airports list"));
            for (Airport apt : aptsToShow) {
                StringBuilder sb = new StringBuilder();
                for (Field fld  : apt.getClass().getDeclaredFields()) {
                    try {
                        fld.setAccessible(true);
                        if ("".equals(fields) || fields.contains(fld.getName())) { //filters all the fields, if "" -> shows every field
                            if (sb.length() > 0)
                                sb.append(", ");
                            sb.append(fld.get(apt));
                        }
                    } catch (IllegalArgumentException |
                             IllegalAccessException   |
                             NullPointerException ignored) { }
                }
                System.out.println(sb.toString());
            }
            System.out.println(Utilities.sectionSeparator("End of the list"));
        }
        return 0;
    }

    /**
     * Reads the file which contains the information about all available airports
     * and creates the list of these airports which is a {@code static} field of
     * Airport class.
     *
     * @return The exit code of the action. Any non-zero code means that an issue
     *         has occurred.
     *
     * @see #aptDatabase
     */
    private static int setAirportsDatabase() {

        String line;
        File src = FilesHandler.findResource("MRpLairportsDatabase.csv");
        if (src == null) {
            System.err.println("The resource was not found.\n");
            return 1;
        }
        String[] csvFields;
        double lat, longit, elev;
        int linesRead = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(src))) {
            while ((line = br.readLine()) != null) {
                ++linesRead;
                csvFields = line.split(",", 11);
                elev = Utilities.parseDouble(csvFields[9]);
                lat = Utilities.parseDouble(csvFields[7]);
                longit = Utilities.parseDouble(csvFields[8]);
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
                                                          Runway.setRunways(csvFields[0],runways)));
            }
            if (linesRead != aptDatabase.size()) throw new IOException();
            else aptDatabaseIsSet = true;
            return 0;
        } catch (FileNotFoundException e) {
            System.err.println("The file with data was not found.");
            return 1;
        }
        catch (IOException ex) {
            System.err.println("At least one database line was not imported.");
            return 1;
        }
    }
}
