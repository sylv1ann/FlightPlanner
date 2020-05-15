package cz.cuni.mff.java.flightplanner.plugin;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import cz.cuni.mff.java.flightplanner.dataobject.Airport;
import cz.cuni.mff.java.flightplanner.DialogCenter;
import cz.cuni.mff.java.flightplanner.util.Utilities;
import cz.cuni.mff.java.flightplanner.dataobject.Runway;
import cz.cuni.mff.java.flightplanner.util.NotNull;

/**
 * The AirportInfoPlugin class is the class that gathers data about the
 * airports available in the .csv "database" file and shows it to the user.
 */
public class AirportInfoPlugin implements Plugin {

    private OutputStream outStream = null;
    private static final double ftToM = 0.3048;
    private static final DecimalFormat decForm = new DecimalFormat("#.###");

    @Override
    public String name() { return this.getClass().getName(); }

    @Override
    public String description() { return "Write information about chosen airports."; }

    @Override
    public String keyword() { return "airport info"; }

    @Override
    public Integer pluginID() { return 2; }

    /**
     * Handles the action for airport information listing. This method lets the
     * user enter all the required information about the output, then searches
     * for all the relevant information and finally outputs the desired information
     * in the way precised by the user.
     *
     * @return The exit code of the action. Any non-zero code means that an issue
     *         has occurred.
     */
    @Override
    public int action() {
        boolean autoOutputManagement =
                DialogCenter.getResponse(null,
                                         "Do you want the %KEYWORD output to be managed automatically? %OPT: "
                                                 .replace("%KEYWORD", this.keyword()),
                                         "Y",
                                         true);
        List<Airport> foundAirports =
                    Airport.searchAirports(null,
                                           null,
                                           false,
                                           false);
        if (foundAirports == null) return 1;                         // returns the exit code if searchAirports fails

        if (autoOutputManagement)
            outStream = DialogCenter.chooseOutputForm("",
                                                      false,
                                                      null);
        for (Airport apt : foundAirports) {
            String icao = apt.getIcaoCode();

            if (!autoOutputManagement) {
                outStream =
                        DialogCenter.chooseOutputForm(" for %ICAO airport"
                                                          .replace("%ICAO", icao),
                                                      true,
                                                      icao + "_INFO");
            }
            else {
                if (outStream.getClass().isAssignableFrom(FileOutputStream.class))
                    outStream =
                        DialogCenter.setFileOutputStream(false,
                                                         icao + "_INFO");
            }
            PrintStream pr = new PrintStream(outStream);
            pr.println(Utilities.sectionSeparator("Information about %s airport"
                                                  .replace("%s",icao)));
            pr.printf("The ICAO (International civil aviation organization) code of this airport is: %s%n" +
                      "The %s airport is situated in: %s, %s.%n" +
                      "It is a %s.%n",  icao,      apt.getName(),
                                        apt.getMunicipality(),  apt.getCountryCode(),
                                        apt.getCat().name().replace("_", " size ")
                     );
            pr.printf("The coordinates of %s are: %.4f, %.4f and its elevation is: %.0f feet (%.1f meters above sea level).%n",
                        apt.getName(),      apt.getGeoLat(),
                        apt.getGeoLong(),   apt.getElevation(),
                        Utilities.unitsConverter(apt.getElevation(), ftToM)
                     );
            String append_S = apt.getRunways().size() > 1 ? "s" : "";

            pr.printf("This airport has %d runway%s available:%n", apt.getRunways().size(), append_S);
            for (Runway rwy : apt.getRunways()) {
                String ident = rwy.getIdentification();
                pr.printf("Runway identification is: %s.%n", ident);
                if (!rwy.isDetailed()) {
                    pr.println("ATTENTION! The runway %ID is in the database, but no enough data about it are provided."
                               .replace("%ID", ident));
                }
                pr.println(lengthAndWidth(rwy));
                pr.println(elevation(rwy));
                pr.println(coverage(rwy));
                pr.println("\tThe geographic location of the thresholds is:");
                pr.println(thresholdGeoLoc(ident, rwy.getThr1Coordinates()));
                pr.println(thresholdGeoLoc(ident, rwy.getThr2Coordinates()));
            }
            pr.println(Utilities.sectionSeparator("End of information about %ICAO airport."
                                                   .replace("%ICAO", icao)));
            pr.printf("%n");
            if (outStream.getClass().isAssignableFrom(FileOutputStream.class)) {
                pr = System.out; // changes the output stream to its default value
                pr.println("\nInformation about %ICAO airport has been successfully written to the file.\n"
                           .replace("%ICAO",icao));
            }
        }
        return 0;
    }

    /**
     * Sets the runway coverage {@code String} result based on the information
     * about given {@code Runway} in the rwy parameter.
     *
     * @param rwy The runway object whose coverage will be inspected.
     * @return The final {@code String} to be printed.
     */
    private String coverage(Runway rwy) {
        String coverage = rwy.getCoverage();

        if (coverage.length() > 1)
            return "\tThe runway is made of: %COVERAGE"
                   .replace("%COVERAGE", coverage);
        else return "\tThe material used for the runway construction is not specified or unknown";
    }

    /**
     * Sets the geographic location {@code String} result based on the information
     * about given {@code Runway} in the rwy parameter.
     *
     * @param icaoID      The airport's ICAO identification code.
     * @param coordinates The coordinates to inspect.
     * @return The final {@code String} to be printed.
     */
    private @NotNull String thresholdGeoLoc(@NotNull String icaoID, @NotNull Double[] coordinates) {
        int idSlashIndex   = icaoID.indexOf("/");
        String latitude, longitude;
        latitude = "NaN".equals(String.valueOf(coordinates[0]))
            ? "UNKNOWN"
            : String.valueOf(coordinates[0]);
        longitude = "NaN".equals(String.valueOf(coordinates[1]))
            ? "UNKNOWN"
            : String.valueOf(coordinates[1]);

        return "\t\tLatitude  threshold %ID: %LAT\n\t\tLongitude threshold %ID: %LONG"
               .replaceAll("%ID",icaoID.substring(0,idSlashIndex))
               .replace("%LAT", latitude)
               .replace("%LONG",longitude);
    }

    /**
     * Sets the length and width {@code String} result based on the information
     * about given {@code Runway} in the rwy parameter.
     *
     * @param rwy The runway whose length and width will be inspected.
     * @return The final {@code String} to be printed
     */
    private @NotNull String lengthAndWidth(@NotNull Runway rwy) {
        Double length = rwy.getLength(),
               width = rwy.getWidth();
        String trLength = decForm.format(Utilities.unitsConverter(length, ftToM));
        String trWidth  = decForm.format(Utilities.unitsConverter(width,ftToM));
        return "\tThe length: %LENGTH feet (%LCONV meters)\n\tThe width : %WIDTH feet (%WCONV meters)."
               .replace("%LENGTH",String.valueOf(length))
               .replace("%WIDTH", String.valueOf(width))
               .replace("%LCONV", trLength)
               .replace("%WCONV", trWidth);
    }


    /**
     * Sets the elevation {@code String} result based on the information about
     * given {@code Runway} in the rwy parameter.
     *
     * @param  rwy The runway whose length and width will be inspected.
     * @return The final {@code String} to be printed.
     */
    private @NotNull String elevation(@NotNull Runway rwy) {
        String elev  = rwy.getElevations(),
               ident = rwy.getIdentification();

        int elevSlashIndex = elev.indexOf("/");
        if(!elev.contains("UNKNOWN")) {
            return  "\tThe elevation at the thresholds is %ELEV feet %FCONV/%SCONV (respectively to the runway identification %ID)."
                    .replace("%ELEV", elev)
                    .replace("%FCONV",
                             Utilities.conversion(true,
                                                            elev.substring(0, elevSlashIndex),
                                                            ftToM,
                                                            "meters"))
                    .replace("%SCONV",
                             Utilities.conversion(true,
                                                             elev.substring(elevSlashIndex + 1),
                                                             ftToM,
                                                             "meters"))
                    .replace("%ID",ident);
        } else {
            return "\tThe elevation of the threshold(s) of the runway %ID is unknown."
                   .replace("%ID",ident);
        }
    }

}
