package cz.cuni.mff.java.flightplanner;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class GetAirportInfoPlugin implements Plugin {

    OutputStream outStream = null;
    private static final double ftToM = 0.3048;

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
     */
    @Override
    public void action() {
        boolean autoOutputManagement =
                DialogCenter.getResponse(null,
                                         "Do you want the %KEYWORD output to be managed automatically? %OPT: "
                                                 .replace("%KEYWORD", this.keyword()),
                                         "Y",
                                         true);
        List<Airport> foundAirports =
                    Airport.searchAirports(null, null, false);

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
            pr.printf("------------ Information about %s airport. ------------%n" +
                      "The ICAO (International civil aviation organization) code of this airport is: %s%n" +
                      "The %s airport is situated in: %s, %s.%n" +
                      "It is a %s.%n",  apt.icaoCode,       apt.icaoCode,
                                        apt.name,           apt.municipality,
                                        apt.countryCode,    apt.cat.name()
                                                                   .replace("_", " size ")
                     );
            pr.printf("The coordinates of %s are: %.4f, %.4f and its elevation is: %.0f feet (%.1f meters above sea level).%n",
                        apt.name,       apt.geoLat,
                        apt.geoLong,    apt.elevation,
                        Utilities.constantConverter(apt.elevation, ftToM)
                     );
            String append_S = apt.runways.length > 1 ? "s" : "";

            pr.printf("This airport has %d runway%s available:%n", apt.runways.length, append_S);
            for (String rwy : apt.runways) {
                String[] fields = rwy.split(",", -1);
                pr.printf("Runway identification is: %s/%s.%n", fields[5], fields[11]); // references correct fields in the csv file format
                pr.printf("\tIt's length is: %s feet (%.1f meters) and width: %s feet (%.1f meters).%n",
                          fields[0], Utilities.constantConverter(Double.parseDouble(fields[0]), ftToM),
                          fields[1], Utilities.constantConverter(Double.parseDouble(fields[1]), ftToM)
                         );
            }
            pr.printf("------------ End of information about %s airport.------------%n%n", apt.icaoCode);
            if (outStream.getClass().isAssignableFrom(FileOutputStream.class)) {
                pr = System.out; // changes the output stream to its default value
                pr.println("\nInformation about %ICAO airport has been successfully written to the file.\n"
                           .replace("%ICAO",apt.icaoCode));
            }
        }
    }
}
