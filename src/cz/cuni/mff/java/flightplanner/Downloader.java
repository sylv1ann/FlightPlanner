package cz.cuni.mff.java.flightplanner;

import java.net.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;
import java.time.*;

public class Downloader {

    private final List<String> airfieldsToDownload = new ArrayList<>();

    /**
     * This method builds the URL from given parameters and then downloads the
     * wanted part of html page that contains METAR weather information.
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
     * @throws IOException if an error occurs while creating a file
     */
    @NotNull File downloadMETAR(@NotNull ZonedDateTime timeFrom,@NotNull ZonedDateTime timeTo, @NotNull Airport airportTarget)
            throws IOException {
        URL page =  buildMETARURL(timeFrom,
                                  timeTo,
                                  airportTarget.icaoCode);
        if (page != null) {
            try {
                String  line;
                File    targetFile = File.createTempFile(airportTarget.icaoCode,      //creates temporary file in current directory with icao code prefix in its name
                                                         ".csv",
                                                         new File(".")
                                                        );
                targetFile.deleteOnExit(); // on-demand deletion of created file after program ends

                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile));
                     BufferedInputStream bis = new BufferedInputStream(page.openStream());
                     BufferedReader br = new BufferedReader(new InputStreamReader(bis))) {

                    String finalAirfieldICAO = airportTarget.icaoCode;
                    boolean threadStarted = false;
                    Thread t = new Thread(() ->
                            System.out.printf("%n... %s METAR download in process ...%n%n", finalAirfieldICAO.toUpperCase()));

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
                }
            } catch (IOException ignored) {
                System.err.println("The file couldn't be created.");
            }
        }

        File emptyFile =
                File.createTempFile("empty_%ICAO"
                                          .replace("%ICAO", airportTarget.icaoCode),
                                    null,
                                    null);
             emptyFile.deleteOnExit();
        return emptyFile;
    }

    @NotNull File noDownloadMETAR(@NotNull ZonedDateTime timeFrom,@NotNull ZonedDateTime timeTo, @NotNull Airport airportTarget) {
        return new File("C:\\Users\\vikto\\Documents\\MFF_Skola\\Java\\FlightPlanner\\LZIB_METAR_20200507230852.txt");
    }

    /**
     * This method creates a URL by modifying current airport name and boundary
     * date and time of the website from which the data to parse will be downloaded.
     *
     * @param timeFrom Describes the time from when the data will be downloaded
     *                 in LocalDateTime format (YYYY-MM-DDTHH:MM:SS).
     * @param timeTo   Describes the time until which the data will be downloaded
     *                 in LocalDateTime format (YYYY-MM-DDTHH:MM:SS).
     * @param airportCode     The ICAO code for a given airport converted to lower case 4
     *                 letter code.
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
            System.err.println("The URL is malformed. Null will be returned.");
            return null;
        }
    }

}
