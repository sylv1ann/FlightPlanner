package cz.cuni.mff.java.flightplanner;

import java.util.*;

/**
 * The class which represents a Runway object of the specified airport for better
 * understanding of its components.
 */
public class Runway {

    Double[] thr1Coordinates, thr2Coordinates;
    String   icaoCode,   identification, coverage,
             truehdgs,   elevations;
    Double   length  ,   width;
    boolean isDetailed;

    Runway(String icao, String rwy) {
        this.icaoCode = icao;

        String[] items = rwy.split(",",-1);
        try {
            this.identification = "%F/%S".replace("%F", items[5])
                                         .replace("%S", items[11]);
            this.truehdgs = "%F/%S".replace("%F", items[9])
                                   .replace("%S", items[15]);
            this.thr1Coordinates = new Double[]{
                    Utilities.parseDouble(items[6]),
                    Utilities.parseDouble(items[7])
            };
            this.thr2Coordinates = new Double[]{
                    Utilities.parseDouble(items[12]),
                    Utilities.parseDouble(items[13])
            };
            this.elevations = "%F/%S".replace("%F", items[8])
                                     .replace("%S", items[14]);
            this.coverage   = items[2].toLowerCase();
            this.length     = Utilities.parseDouble(items[0]);
            this.width      = Utilities.parseDouble(items[1]);
            this.isDetailed = normalize(this);
        } catch (NullPointerException ignored) { }
    }

    /**
     * Checks for the relevance of {@code Runway} object and replaces invalid
     * fields with default "unknown" values.
     * @param runway The runway to check and normalize.
     * @return The flag indicating that enough data about runway are available.
     */
    private boolean normalize(Runway runway) {
        int countUnknownInfo = 0;
        boolean unkLength = false, unkWidth = false,
                unkHDGs   = false, unkElevs = false,
                unkCover  = false;
        if ("NaN".equals(String.valueOf(runway.length))) {
            countUnknownInfo++;
        }
        if ("NaN".equals(String.valueOf(runway.width))) {
            countUnknownInfo++;
        }
        if ("/".equals(runway.truehdgs)) {
            runway.truehdgs = "UNKNOWN/UNKNOWN";
            countUnknownInfo++;
        }
        if ("/".equals(runway.elevations)) {
            runway.elevations = "UNKNOWN/UNKNOWN";
            countUnknownInfo++;
        }
        if (runway.coverage.length() <= 2) {
            runway.coverage = "UNKNOWN/NOT SPECIFIED";
            countUnknownInfo++;
        }
        return countUnknownInfo < 3;
    }

    /**
     * Creates a {@code Runway} object for each item of {@code String array}
     * specified in {@code runways} parameter. It also assigns the airport the
     * runways belong to.
     * @param icaoCode The ICAO code of the airport.
     * @param runways  The array of runways in .csv format.
     * @return The list of runway objects for corresponding airport.
     */
    static @NotNull List<Runway> setRunways(@NotNull String icaoCode, @NotNull String[] runways) {
        List<Runway> result = new LinkedList<>();
        for(String rwyStr : runways) {
            result.add(new Runway(icaoCode,rwyStr));
        }
        return result;
    }

}
