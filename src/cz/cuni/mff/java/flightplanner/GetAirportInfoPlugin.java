package cz.cuni.mff.java.flightplanner;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;

public class GetAirportInfoPlugin implements Plugin {

    @Override
    public String name() { return "GetAirportInfoModule"; }

    @Override
    public String description() { return "Write information about chosen airports."; }

    @Override
    public String keyword() { return "airport"; }

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
        OutputStream outStream = null;
        boolean auto = DialogCenter.getResponse(null,"Do you want the output to be managed automatically? (Y/n): ", "Y", true);
        boolean autoPrint = false; //the option to print the output automatically or require Keypress to continue; not implemented yet
        PrintStream pr;
        LinkedList<Airport> found = Airport.searchAirports(null, false);
        //this.outStream = null;
        if (auto)
            outStream = DialogCenter.chooseOutputForm("", false, null);
        for (Airport apt : found) {
            if (!auto)
                outStream = DialogCenter.chooseOutputForm(" for " + apt.icaoCode + " airport", true, apt.icaoCode);
            else {
                if (outStream instanceof FileOutputStream)
                    outStream = DialogCenter.setFileOutputStream(false, apt.icaoCode);
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
            } else { //the outStream goes to the screen
                DialogCenter.getResponse(null, "Press Enter to continue.", "", true);
            }
        }
    }
}