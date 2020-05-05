package cz.cuni.mff.java.flightplanner;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class GetWeatherInfoPlugin implements Plugin {

    final String dateTimeStrFormat = "yyyy-MM-dd HH:mm";
          OutputStream outStream;

    @Override
    public String name() { return this.getClass().getName(); }

    @Override
    public String description() { return "Write information about the weather at chosen airports."; }

    @Override
    public String keyword() { return "weather info"; }

    @Override
    public Integer pluginID() { return 1; }

    @Override
    public void action() {
        LocalDateTime fromTime, toTime;
        Downloader dwnldr = new Downloader();
        METARDecoder weatherProcessor = new METARDecoder();

        boolean autoOutputManagement =
                DialogCenter.getResponse(null,
                                         "Do you want the %KEYWORD output to be managed automatically? %OPT: "
                                                 .replace("%KEYWORD", this.keyword()),
                                         "Y",
                                         true
                                        );
        if (DialogCenter.getResponse(null,
                                     "Do you want to precise the date and time for the output? %OPT: ",
                                     "Y",
                                     true
                                    )
           ) {
            DateTimeFormatter format =
                                DateTimeFormatter.ofPattern(dateTimeStrFormat);
            fromTime = getFromDateTime(format);
            toTime   = getToDateTime(fromTime, format);
        } else {
            fromTime = LocalDateTime.now().minusDays(1);
            toTime   = LocalDateTime.now();
        }

        ZonedDateTime utcFromTime =
                fromTime.atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(ZoneId.of("UTC"));
        ZonedDateTime utcToTime   =
                toTime  .atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(ZoneId.of("UTC"));
        List<File>      downloadedMETARs = new ArrayList<>();
        List<Airport>   foundAirports =
                          Airport.searchAirports(null,null,false);
        if (autoOutputManagement) {
            outStream =
                    DialogCenter.chooseOutputForm("", false,
                                                  null);
        }

        boolean deleteFilesOnExit = false;
        for (Airport apt : foundAirports) {

            if (DialogCenter.getResponse(null,
                    "Do you want to delete the RAW data file(s)? %OPT: ",
                    "Y",
                    true)
            ) {
                deleteFilesOnExit = true;
            }

            try {
                File aptMETAR_raw;
                if (DialogCenter.getResponse(null, "Do you debug? %OPT: ","Y", false)) {
                    aptMETAR_raw = dwnldr.noDownloadMETAR(utcFromTime, utcToTime,apt);
                } else {
                    aptMETAR_raw =
                            dwnldr.downloadMETAR(utcFromTime, utcToTime,
                                    apt);
                }
                if (deleteFilesOnExit || aptMETAR_raw.getName().startsWith("empty_"))
                    aptMETAR_raw.deleteOnExit();

                if (!autoOutputManagement) {
                    outStream =
                        DialogCenter.chooseOutputForm(" for %ICAO airport"
                                                          .replace("%ICAO", apt.icaoCode),
                                                      true,
                                                      apt.icaoCode + "_METAR");
                }
                else {
                    if (outStream.getClass().isAssignableFrom(FileOutputStream.class)) {
                        outStream =
                                DialogCenter.setFileOutputStream(
                                        true,
                                        "%ICAO_METAR".replace("%ICAO",apt.icaoCode)
                                );
                    }
                }
                PrintStream pr = new PrintStream(outStream);

                if (DialogCenter.getResponse(null,
                                             "Do you want to print the raw data using the output form chosen previously? %OPT: ",
                                             "Y",
                                             true)) {
                    pr.println(Utilities.sectionSeparator(
                            "RAW %ICAO FILE"
                                       .replace("%ICAO",apt.icaoCode))
                              );
                    printRawDataFile(aptMETAR_raw, pr);
                    pr.println(Utilities.sectionSeparator("END RAW FILE"));
                }

                weatherProcessor.fileDecode(aptMETAR_raw, pr);
            } catch (IOException e) {
                System.err.println("An error occured while creating a file with %ICAO METAR data."
                                   .replace("%ICAO", apt.icaoCode));
            }
        }
    }

    /**
     * This method asks the user for time precision using specified format.
     * Incorrect input format causes the method to take the current time - 1 day.
     * If the date is too far in the past, the weather report would be uselessly
     * long. Therefore, an input from more than a year ago results in taking
     * weather reports from the last 7 days.
     *
     * @return The date and time from which the weather information will be
     *         gathered.
     */
    @NotNull LocalDateTime getFromDateTime(DateTimeFormatter format) {
        LocalDateTime now = LocalDateTime.now(),
                      resultTime,
                      chosenDateTime;

        try {
            System.out.println("Incorrect date/time format will result in taking the current time - 24 hours.");
            System.out.printf("Please enter the \"from\" date in the following format: %s : ", dateTimeStrFormat);
            chosenDateTime =
                    LocalDateTime.parse(DialogCenter.getInput(false),
                                        format
                                       );
            if (now.minusYears(1).isAfter(chosenDateTime)) {
                System.out.printf("The weather history would be very long since %s%n." +
                                  "Therefore, last 24 hours weather reports will be processed.%n",
                                  chosenDateTime
                                 );
                resultTime = now.minusWeeks(1);
            } else
                resultTime = chosenDateTime;
        }
        catch (DateTimeParseException e) {
                resultTime = now.minusDays(1);
        }

        return resultTime;
    }

    /**
     * This method asks the user to precise the time until which the weather
     * information will be gathered. If the given date is in the future, there
     * will certainly not be any METAR reports. The date is therefore corrected
     * to the current local date and time. If provided time is before the
     * {@code fromTime} parameter, then {@code fromTime + 1} day is taken.
     *
     * @param fromTime This time information will be compared to the given time
     *                 for correctness ({@code fromtime} needs to be before the
     *                 {@code to} time).
     *
     * @return Returns date and time up until which weather data will be
     *         downloaded and processed.
     */
    @NotNull LocalDateTime getToDateTime(@NotNull LocalDateTime fromTime, DateTimeFormatter format) {
        LocalDateTime now = LocalDateTime.now(),
                      resultTime;

        try {
            System.out.println("Incorrect date/time format will result in taking the current time.");
            System.out.printf("Please enter the \"to\" date in the following format: %s: ", dateTimeStrFormat);
            LocalDateTime chosenDateTime =
                    LocalDateTime.parse(DialogCenter.getInput(false),
                                        format
                                       );

            if (chosenDateTime.isAfter(now)) {
                System.out.println("There are no weather reports provided for the given date in the future." +
                                   "Current time will be taken.");
                resultTime = now;
            } else {
                if (chosenDateTime.isBefore(fromTime)) {
                    System.out.println("This time specification is not correct, because given \"from\" time is after the \"to\" time. " +
                                       "Therefore, weather reports in 24 hours period since the \"from\" time will be taken.");
                    resultTime = fromTime.plusDays(1);
                } else
                    resultTime = chosenDateTime;
            }
        } catch (DateTimeParseException e) {
            resultTime = now;
        }

        return resultTime;
    }

    void printRawDataFile(@NotNull File file, PrintStream printer) {
        try (BufferedReader br =
                     new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) !=  null) {
                printer.println(line);
            }
            printer.printf("%n");
        } catch (IOException ignored) { }
    }

}
