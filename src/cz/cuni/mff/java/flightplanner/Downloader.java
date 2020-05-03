package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.*;
import java.net.*;
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
     * @return Returns the file which contains the METAR weather information for
     *         selected airport, date and time if available. Returns an empty file,
     *         if an error occurs.
     */
    @NotNull File downloadMETAR(@NotNull ZonedDateTime timeFrom,@NotNull ZonedDateTime timeTo, @NotNull Airport airportTarget) {

        URL page =  buildMETARURL(timeFrom,
                                  timeTo,
                                  airportTarget.icaoCode);
        if (page != null) {
            try {
                String  line;
                boolean websiteBody = false;
                File    targetFile = File.createTempFile(airportTarget.icaoCode,      //creates temporary file in current directory with icao code prefix in its name
                                                         ".txt",
                                                         new File(".")
                                                        );
                targetFile.deleteOnExit(); //deletes created files after program ends

                try (BufferedWriter wr = new BufferedWriter(new FileWriter(targetFile));
                     BufferedInputStream bis = new BufferedInputStream(page.openStream());
                     BufferedReader br = new BufferedReader(new InputStreamReader(bis))) {

                    String finalAirfieldICAO = airportTarget.icaoCode;
                    Thread t = new Thread(() -> System.out.printf("%n... %s METAR download in process ...%n%n", finalAirfieldICAO.toUpperCase()));

                    while ((line = br.readLine()) != null) {            //while loop which ensures that only correct part of the website is
                        if (!websiteBody && "<pre>".equals(line)) {     //written to the tmp file.
                            websiteBody = true;
                            t.start();                                          //Another thread informs the user about currently downloaded webpage.
                            continue;
                        }
                        if (websiteBody && "</pre>".equals(line)) {    //Skips the part before the <pre> tag and after the </pre> tag.
                            websiteBody = false;
                            continue;
                        }
                        if (websiteBody) {
                            wr.append(line).append("\n");
                        }
                    }
                    return targetFile;
                } catch (IOException ignore) {
                    System.out.println("An error occurred.");
                }
            } catch (IOException ignored) {
                System.out.println("The file couldn't be created.");
            }
        }

        try {
            return File.createTempFile("empty" + airportTarget.icaoCode, null, null);
        }
        catch (IOException e) {
            return new File(".");
        }
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

        airportCode = airportCode.toLowerCase();
        String  yearFrom = String.valueOf(timeFrom.getYear()),
                monthFrom= String.valueOf(timeFrom.getMonth()),
                dayFrom  = String.valueOf(timeFrom.getDayOfMonth()),
                hourFrom = String.valueOf(timeFrom.getHour()),
                minFrom  = String.valueOf(timeFrom.getMinute());

        String  yearTo   = String.valueOf(timeTo.getYear()),
                monthTo  = String.valueOf(timeTo.getMonth()),
                dayTo    = String.valueOf(timeTo.getDayOfMonth()),
                hourTo   = String.valueOf(timeTo.getHour()),
                minTo    = String.valueOf(timeTo.getMinute());

        String sURL = "https://www.ogimet.com/display_metars2.php?lang=en&lugar=&tipo=ALL&ord=REV&nil=NO&fmt=txt&ano=&mes=&day=&hora=&min=&anof=&mesf=&dayf=&horaf=&minf=&send=send";
        sURL = sURL
                .replaceFirst("lugar=", "lugar=" + airportCode) .replaceFirst("ano=",  "ano="   + yearFrom)
                .replaceFirst("mes=",   "mes=" + monthFrom)     .replaceFirst("day=",  "day="   + dayFrom )
                .replaceFirst("hora=",  "hora=" + hourFrom)     .replaceFirst("min=",  "min="   + minFrom )
                .replaceFirst("anof=",  "anof=" + yearTo)       .replaceFirst("mesf=", "mesf="  + monthTo )
                .replaceFirst("dayf=",  "dayf=" + dayTo)        .replaceFirst("horaf=","horaf=" + hourTo  )
                .replaceFirst("minf=",  "minf=" + minTo);
        try {
            return new URL(sURL);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Creates an absolute path for the new file in the current directory.
     *
     * @param fileName = Name of the new file.
     * @return = Returns new file with path to current directory and fileName.
     */
    static @NotNull File fileFromPathCreator(@NotNull String fileName) {
        File currentDir = new File("");
        String part = currentDir.getAbsolutePath() + File.separator + fileName;
        return new File(part);
    }
}
