package cz.cuni.mff.java.flightplanner;

import java.util.*;

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
                    Utilities.parseNum(items[6]),
                    Utilities.parseNum(items[7])
            };
            this.thr2Coordinates = new Double[]{
                    Utilities.parseNum(items[12]),
                    Utilities.parseNum(items[13])
            };
            this.elevations = "%F/%S".replace("%F", items[8])
                                     .replace("%S", items[14]);
            this.coverage   = items[2].toLowerCase();
            this.length     = Utilities.parseNum(items[0]);
            this.width      = Utilities.parseNum(items[1]);
            this.isDetailed = normalize(this);
        } catch (NullPointerException ignored) { }
    }

    /**
     * Checks for the relevance of {@code Runway} object and replaces invalid
     * fields with default "unknown" values.
     * @param runway The runway to check and normalize.
     * @return The flag indicating that more data about runway are available.
     */
    private boolean normalize(Runway runway) {
        boolean unkLength = false, unkWidth = false,
                unkHDGs   = false, unkElevs = false;
        if ("NaN".equals(String.valueOf(runway.length))) {
            unkLength = true;
        }
        if ("NaN".equals(String.valueOf(runway.width))) {
            unkWidth = true;
        }
        if ("/".equals(runway.truehdgs)) {
            runway.truehdgs = "UNKNOWN/UNKNOWN";
            unkHDGs = true;
        }
        if ("/".equals(runway.elevations)) {
            runway.elevations = "UNKNOWN/UNKNOWN";
            unkElevs = true;
        }
        return !unkLength || !unkWidth || !unkHDGs || !unkElevs;
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
