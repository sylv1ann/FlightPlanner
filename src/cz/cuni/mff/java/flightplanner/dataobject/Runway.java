package cz.cuni.mff.java.flightplanner.dataobject;

import cz.cuni.mff.java.flightplanner.util.NotNull;
import cz.cuni.mff.java.flightplanner.util.Utilities;

import java.util.*;

/**
 * The class which represents a Runway object of the specified airport for better
 * understanding of its components.
 */
public class Runway {

    private Double[] thr1Coordinates, thr2Coordinates;
    private final String   icaoCode;
    private String identification, coverage,
                   truehdgs, elevations;
    private Double length,   width;
    private boolean isDetailed;

    private Runway(String icao, String rwy) {
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
     * @return The surface type of the runway.
     */
    public String getCoverage() {
        return coverage;
    }

    /**
     * @return The length of the runway.
     */
    public Double getLength() {
        return length;
    }

    /**
     * @return The width of the runway.
     */
    public Double getWidth() {
        return width;
    }

    /**
     * @return The elevations of both ends of the runway.
     */
    public String getElevations() {
        return elevations;
    }

    /**
     * @return The runway identification.
     */
    public String getIdentification() {
        return identification;
    }

    /**
     * @return The coordinates of the first threshold of the runway.
     */
    public Double[] getThr1Coordinates() {
        return thr1Coordinates;
    }

    /**
     * @return The coordinates of the second threshold of the runway.
     */
    public Double[] getThr2Coordinates() {
        return thr2Coordinates;
    }

    /**
     * @return The flag indicating the (in)sufficient amount of information about
     *         the runway.
     */
    public boolean isDetailed() {
        return isDetailed;
    }

    /**
     * Checks for the relevance of {@code Runway} object and replaces invalid
     * fields with default "unknown" values.
     * @param runway The runway to check and normalize.
     * @return The flag indicating that enough data about runway are available.
     */
    private boolean normalize(Runway runway) {
        int countUnknownInfo = 0;
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
    static @NotNull
    List<Runway> setRunways(@NotNull String icaoCode, @NotNull String[] runways) {
        List<Runway> result = new LinkedList<>();
        for(String rwyStr : runways) {
            result.add(new Runway(icaoCode,rwyStr));
        }
        return result;
    }

}
