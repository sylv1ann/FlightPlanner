package cz.cuni.mff.java.flightplanner;

import java.net.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;
import java.time.*;

/**
 * The Downloader class is responsible for the download preparation and the METAR
 * information download itself from the provider website.
 */
public class Downloader {

    private final List<String> airfieldsToDownload = new ArrayList<>();

    /**
     * This method uses {@link #buildMETARURL(ZonedDateTime, ZonedDateTime, String)}
     * to build the URL from given parameters and then downloads the .csv file
     * that contains METAR weather information.
     *
     * @param timeFrom      Describes the timestamp from when the data will be
     *                      fetched. If null, then corresponds to the current time
     *                      in UTC minus one day.
     *
     * @param timeTo        Describes the timestamp until when the data will be
     *                      fetched. If null, then corresponds to the current time
     *                      in UTC.
     *
     * @param airportTarget The airfield for which the METAR data will be gathered.
     *
     * @return The file which contains the METAR weather information for
     *         selected airport, date and time if available. Returns an empty file,
     *         if an error occurs.
     *
     * @throws IOException if an error occurs while creating a file.
     */
    @NotNull File downloadMETAR(@NotNull ZonedDateTime timeFrom, @NotNull ZonedDateTime timeTo,
                                @NotNull Airport airportTarget) throws IOException {

        URL page =  buildMETARURL(timeFrom, timeTo,
                                  airportTarget.icaoCode);
        if (page != null) {
            String line;
            File targetFile = File.createTempFile(airportTarget.icaoCode,      //creates temporary file in current directory with icao code prefix in its name
                                                  ".csv",
                                                  new File("output/")
            );
            targetFile.deleteOnExit(); // deletion of created file after program ends

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile));
                 BufferedInputStream bis = new BufferedInputStream(page.openStream());
                 BufferedReader br = new BufferedReader(new InputStreamReader(bis))) {

                String finalAirfieldICAO = airportTarget.icaoCode;
                boolean threadStarted = false;
                Thread t = new Thread(() ->
                        System.out.printf("%n... %s METAR download in process ...%n", finalAirfieldICAO.toUpperCase()));

                while ((line = br.readLine()) != null) {
                    if (!threadStarted) {
                        t.start();
                        threadStarted = true;
                    }
                    writer.write(line);
                    writer.append("\n");
                }
                bis.close();
                return targetFile;                              // the point where the method normally ends
            } catch (IOException ignore) {
                System.err.println("An error occurred.");
                throw new IOException();
            }
        } else throw new IOException();

    }

    /**
     * Wrapper aroung {@link #downloadMETAR(ZonedDateTime, ZonedDateTime, Airport)} method
     * which allows multiple files to downloaded and grouped together.
     *
     * @param timeFrom      Describes the timestamp from when the data will be
     *                      fetched. If null, then corresponds to the current time
     *                      in UTC minus one day.
     *
     * @param timeTo        Describes the timestamp until when the data will be
     *                      fetched. If null, then corresponds to the current time
     *                      in UTC.
     *
     * @param aptsToDwnld   The list of airfields for which the METAR data will
     *                      be downloaded.
     *
     * @return The map of pairs (icao String, non-empty file) which contain the
     *         METAR weather information for selected airports, date and time
     *         (if available).
     */
    @NotNull Map<String, File> downloadMETARs(@NotNull ZonedDateTime timeFrom, @NotNull ZonedDateTime timeTo,
                                              @NotNull List<Airport> aptsToDwnld) {
        Map<String, File> result = new HashMap<>();

        for(Airport apt : aptsToDwnld) {
            try {
                if (!result.containsKey(apt.icaoCode)) {
                    result.put(apt.icaoCode, downloadMETAR(timeFrom, timeTo, apt));
                }
            } catch (IOException e) {
                System.err.println("An error occured while creating a file with %ICAO METAR data."
                                   .replace("%ICAO", apt.icaoCode));
            }
        }
        return result;
    }


    /**
     * This method creates a URL of the METAR data provider website  by defining
     * the current target airport name, date and time boundaries.
     *
     * @param timeFrom Describes the time from when the data will be downloaded
     *                 in predefined LocalDateTime format.
     * @param timeTo   Describes the time until which the data will be downloaded
     *                 in predefined LocalDateTime format.
     * @param airportCode The ICAO code for a given airport converted to lower
     *                    case 4-letter code.
     * @return The URL which will be used for data gathering.
     */
    @Nullable URL buildMETARURL(@NotNull ZonedDateTime timeFrom, @NotNull ZonedDateTime timeTo, String airportCode) {

        airportCode = airportCode.toUpperCase();
        final String datePattern = "yyyyMMddHHmm";
              String  fromDate   = timeFrom.format(DateTimeFormatter.ofPattern(datePattern)),
                      toDate     = timeTo.format(DateTimeFormatter.ofPattern(datePattern));

        String sURL =  "http://www.ogimet.com/cgi-bin/getmetar?icao=%ICAO&begin=%FROM&end=%TO"
                        .replace("%ICAO", airportCode)
                        .replace("%FROM", fromDate)
                        .replace("%TO", toDate);
        try {
            return new URL(sURL);
        } catch (MalformedURLException e) {
            System.err.println("The URL is malformed or does not exist.");
            return null;
        }
    }
}
