import org.jetbrains.annotations.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.io.*;
import java.time.*;

public class Downloader implements Corrector {

    @Override
    public String correct() {
        System.out.println("Please re-type your choices.");
        return null;
    }

    /**
     * Creates an absolute path for the new file in the current directory.
     * @param fileName = Name of the new file.
     * @return = Returns new file with path to current directory and fileName.
     */
    @NotNull
    public static File filePathCreator(@NotNull String fileName) {
        File currentDir = new File("");
        String part = currentDir.getAbsolutePath() + File.separator + fileName;
        return new File(part);
    }

    /**
     * Method to download METARs without specified timestamps (optional).
     * @see  #downloadMETAR(LocalDateTime, LocalDateTime, String[])
     * @param airfields = List of airfields to download METAR from.
     */
    public static void downloadMETAR(String[] airfields) {
        if (airfields == null) {
          System.out.println("Nothing to download.");
          return;
        }
        downloadMETAR(null, null, airfields); //calls the main downloader method
    }

    /**
     * Equivalent to Downloader.download(String[]) without specified timestamps.
     * @see #downloadMETAR(String[]) method
     * @param list = list of airfields to gather METARs from.
     */
    public static void downloadMETAR(List<String> list) {
        if (list == null) {
            System.out.println("Nothing to download.");
            return;
        }
        String[] array = new String[0];
        downloadMETAR(null, null,list.toArray(array));
    }

    /**
     * This method iterates through every given airport and downloads its METAR weather information.
     * @param timeFrom = Describes the timestamp from when the data will be fetched. If null, then corresponds to the current time in UTC minus one day.
     * @param timeTo = Describes the timestamp until when the data will be fetched. If null, then corresponds to the current time in UTC.
     * @param airfields = The array of all the airfields, from which the METAR data will be gathered.
     */
    @NotNull
    public static ArrayList<File> downloadMETAR(LocalDateTime timeFrom, LocalDateTime timeTo, @NotNull String[] airfields) {
        ArrayList<File> filesToParse = new ArrayList<>();

        if (timeTo == null) timeTo = LocalDateTime.now(ZoneOffset.UTC);
        if (timeFrom == null || timeFrom.isAfter(timeTo)) {
            timeFrom = timeTo.minusDays(1);
        }

        for (String airfield : airfields) {
            /*
                TODO: 22/02/2020 if airfield.length > 4 then search in airports database for the icao code
                 * if icao code not found, then throw exception which will call correction method
            */
            URL page = buildURLforMETAR(timeFrom.toString(), timeTo.toString(), airfield);
            if (page != null) {
                try {
                    String line;
                    boolean websiteBody = false;//, debug = true;
                    File targetFile = File.createTempFile(airfield, ".txt", new File("."));
                    //if (!debug) targetFile.deleteOnExit(); //deletes created files after program ends

                    try (BufferedWriter wr = new BufferedWriter(new FileWriter(targetFile));
                         BufferedInputStream bis = new BufferedInputStream(page.openStream());
                         BufferedReader br = new BufferedReader(new InputStreamReader(bis))) {

                        Thread t = new Thread(() -> System.out.println(airfield.toUpperCase() + " METAR download in process"));
                        t.start();                                          //Another thread informs user about currently downloaded webpage.

                        while ((line = br.readLine()) != null) {            //while loop which ensures that only correct part of the website is
                            if (!websiteBody && "<pre>".equals(line)) {     //written to the tmp file.
                                websiteBody = true;
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
                        //when the download is finished, add the current document to the list to process.
                        filesToParse.add(targetFile);
                    } catch (IOException ignore) { System.out.println("An error occured."); }
                } catch (IOException ignored) { System.out.println("The file couldn't be created."); }
            }
        }
        return filesToParse;
    }

    /**
     * This method creates a URL by modifying current airport name
     * and boundary date and time of the website from which the data
     * to parse will be downloaded.
     * @param timeFrom = Describes the time from when the data will be downloaded in LocalDateTime format (YYYY-MM-DDTHH:MM:SS).
     * @param timeTo = Describes the time until which the data will be downloaded in LocalDateTime format (YYYY-MM-DDTHH:MM:SS).
     * @param icao = The ICAO code for a given airport converted to lower case 4 letter code.
     */
    @Nullable
    private static URL buildURLforMETAR(@NotNull String timeFrom, @NotNull String timeTo, String icao) {
        icao = icao.toLowerCase();
        String yearFrom = timeFrom.substring(0, 4),
               monthFrom = timeFrom.substring(5, 7),
               dayFrom = timeFrom.substring(8, 10),
               hourFrom = timeFrom.substring(11, 13),
               minFrom = timeFrom.substring(14, 16);

        String yearTo = timeTo.substring(0, 4),
               monthTo = timeTo.substring(5, 7),
               dayTo = timeTo.substring(8, 10),
               hourTo = timeTo.substring(11, 13),
               minTo = timeTo.substring(14, 16);

        String sURL = "https://www.ogimet.com/display_metars2.php?lang=en&lugar=&tipo=ALL&ord=REV&nil=NO&fmt=txt&ano=&mes=&day=&hora=&min=&anof=&mesf=&dayf=&horaf=&minf=&send=send";
        sURL = sURL.replaceFirst("lugar=", "lugar=" + icao).replaceFirst("ano=","ano=" + yearFrom).
                    replaceFirst("mes=", "mes=" + monthFrom).replaceFirst("day=", "day=" + dayFrom).
                    replaceFirst("hora=", "hora=" + hourFrom).replaceFirst("min=", "min=" + minFrom).
                    replaceFirst("anof=", "anof=" + yearTo).replaceFirst("mesf=", "mesf=" + monthTo).
                    replaceFirst("dayf=", "dayf=" + dayTo).replaceFirst("horaf=", "horaf=" + hourTo).
                    replaceFirst("minf=", "minf=" + minTo);

        try {
            return new URL(sURL);
        }
        catch (MalformedURLException e) {
            return null;
        }
    }
}