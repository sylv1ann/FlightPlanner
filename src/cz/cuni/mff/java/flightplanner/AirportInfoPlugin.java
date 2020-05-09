package cz.cuni.mff.java.flightplanner;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

public class AirportInfoPlugin implements Plugin {

    OutputStream outStream = null;
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
        if (foundAirports == null) return 1;

        if (autoOutputManagement)
            outStream = DialogCenter.chooseOutputForm("",
                                                      false,
                                                      null);
        for (Airport apt : foundAirports) {

            if (!autoOutputManagement) {
                outStream =
                        DialogCenter.chooseOutputForm(" for %ICAO airport"
                                                          .replace("%ICAO", apt.icaoCode),
                                                      true,
                                                      apt.icaoCode + "_INFO");
            }
            else {
                if (outStream.getClass().isAssignableFrom(FileOutputStream.class))
                    outStream =
                        DialogCenter.setFileOutputStream(false,
                                                         apt.icaoCode + "_INFO");
            }
            PrintStream pr = new PrintStream(outStream);
            pr.println(Utilities.sectionSeparator("Information about %s airport"
                                                  .replace("%s",apt.icaoCode)));
            pr.printf("The ICAO (International civil aviation organization) code of this airport is: %s%n" +
                      "The %s airport is situated in: %s, %s.%n" +
                      "It is a %s.%n",  apt.icaoCode,       apt.name,
                                        apt.municipality,   apt.countryCode,
                                        apt.cat.name().replace("_", " size ")
                     );
            pr.printf("The coordinates of %s are: %.4f, %.4f and its elevation is: %.0f feet (%.1f meters above sea level).%n",
                        apt.name,       apt.geoLat,
                        apt.geoLong,    apt.elevation,
                        Utilities.unitsConverter(apt.elevation, ftToM)
                     );
            String append_S = apt.runways.size() > 1 ? "s" : "";

            pr.printf("This airport has %d runway%s available:%n", apt.runways.size(), append_S);
            for (Runway rwy : apt.runways) {
                pr.printf("Runway identification is: %s.%n", rwy.identification);
                if (rwy.isDetailed) {
                    pr.println(lengthAndWidth(rwy));
                    pr.println(elevation(rwy));
                    pr.println("\tThe geographic location of the thresholds is:");
                    pr.println(thresholdGeoLoc(rwy.identification, rwy.thr1Coordinates));
                    pr.println(thresholdGeoLoc(rwy.identification, rwy.thr2Coordinates));
                } else {
                    pr.println("The runway %ID is in the database, but no further data about it are provided."
                               .replace("%ID", rwy.identification));
                }
            }
            pr.println(Utilities.sectionSeparator("End of information about %ICAO airport."
                                                   .replace("%ICAO", apt.icaoCode)));
            pr.printf("%n");
            if (outStream.getClass().isAssignableFrom(FileOutputStream.class)) {
                pr = System.out; // changes the output stream to its default value
                pr.println("\nInformation about %ICAO airport has been successfully written to the file.\n"
                           .replace("%ICAO",apt.icaoCode));
            }
        }
        return 0;
    }

    /**
     * Sets the geographic loaction {@code String} result based on the information
     * about given {@code Runway} in the rwy parameter.
     * @param icaoID The airport's ICAO identification code.
     * @param coordinates The coordinates to inspect.
     * @return The final {@code String} to be printed.
     */
    @NotNull String thresholdGeoLoc(@NotNull String icaoID, @NotNull Double[] coordinates) {
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
     * @param rwy The runway whose length and width will be inspected.
     * @return The final {@code String} to be printed
     */
    @NotNull String lengthAndWidth(@NotNull Runway rwy) {
        if ("NaN".equals(rwy.length.toString()) ||
            "NaN".equals(rwy.width.toString())) {
            return "\tEither the length or the width of the %ID runway is unknown."
                    .replace("%ID", rwy.identification);
        } else {
            String trLength = decForm.format(Utilities.unitsConverter(rwy.length, ftToM));
            String trWidth  = decForm.format(Utilities.unitsConverter(rwy.width,ftToM));
            return "\tIt's length is: %LENGTH feet (%LCONV meters) and width: %WIDTH feet (%WCONV meters)."
                    .replace("%LENGTH",String.valueOf(rwy.length))
                    .replace("%WIDTH", String.valueOf(rwy.width))
                    .replace("%LCONV", trLength)
                    .replace("%WCONV", trWidth);
        }
    }


    /**
     * Sets the elevation {@code String} result based on the information about
     * given {@code Runway} in the rwy parameter.
     * @param rwy The runway whose length and width will be inspected.
     * @return The final {@code String} to be printed
     */
    @NotNull String elevation(@NotNull Runway rwy) {
        int elevSlashIndex = rwy.elevations.indexOf("/");
        if(!rwy.elevations.contains("UNKNOWN")) {
            return  "\tThe elevation at the thresholds is %ELEV feet %FCONV/%SCONV (respectively to the runway identification %ID)."
                    .replace("%ELEV", rwy.elevations)
                    .replace("%FCONV",
                             Utilities.conversion(true,
                                                            rwy.elevations.substring(0, elevSlashIndex),
                                                            ftToM,
                                                            "meters"))
                    .replace("%SCONV",
                             Utilities.conversion(true,
                                                             rwy.elevations.substring(elevSlashIndex + 1),
                                                             ftToM,
                                                             "meters"))
                    .replace("%ID",rwy.identification);
        } else {
            return "\tThe elevation of the threshold(s) of the runway %ID is unknown."
                   .replace("%ID",rwy.identification);
        }
    }

}
