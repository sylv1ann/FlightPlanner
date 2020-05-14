package cz.cuni.mff.java.flightplanner;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * The WeatherInfoPlugin class is responsible for gathering data and creating the
 * output text concerning the weather at specified airport(s).
 */
public class WeatherInfoPlugin implements Plugin {

    final static String dateTimeStrFormat = "yyyy-MM-dd HH:mm";
                 OutputStream outStream = System.out;

    @Override
    public String name() { return this.getClass().getName(); }

    @Override
    public String description() { return "Write information about the weather at chosen airports."; }

    @Override
    public String keyword() { return "weather info"; }

    @Override
    public Integer pluginID() { return 1; }

    /**
     * Handles the action for airport weather data gathering. This method lets the
     * user enter all the required information about the output, then searches
     * for and downloads all the relevant information, processes the downloaded
     * data and finally outputs the desired information in the way precised by the
     * user.
     *
     * @return The exit code of the action. Any non-zero code means that an issue
     *         has occurred.
     */
    @Override
    public int action() {
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
                          Airport.searchAirports(null,
                                                 null,
                                                 false,
                                                 false);
        if (foundAirports == null) return 1;

        if (autoOutputManagement) {
            outStream =
                    DialogCenter.chooseOutputForm("", false,
                                                  null);
        }
        Map<String, File> aptMETARs_raw =
                dwnldr.downloadMETARs(utcFromTime, utcToTime,
                                      foundAirports);

        for (String icaoCode : aptMETARs_raw.keySet()) {
            int exit;
            File keyCorresFile = aptMETARs_raw.get(icaoCode);
            if (keyCorresFile == null) return 1;

            if (!autoOutputManagement) {
                outStream =
                    DialogCenter.chooseOutputForm(" for %ICAO airport"
                                                      .replace("%ICAO", icaoCode),
                                                  true,
                                                  icaoCode + "_METAR");
            }else {
                if (outStream.getClass()
                             .isAssignableFrom(FileOutputStream.class)) {
                    outStream = DialogCenter.setFileOutputStream(false,
                                                                 icaoCode + "_METAR");
                }
            }
            PrintStream pr = new PrintStream(outStream);
            boolean fileOutput = outStream.getClass()
                                          .isAssignableFrom(FileOutputStream.class);

            if (DialogCenter.getResponse(null,
                                         "Do you want to print the raw data using the output form chosen previously? %OPT: ",
                                         "Y",
                                         true)) {
                pr.println(Utilities.sectionSeparator(
                    "RAW %ICAO FILE"
                               .replace("%ICAO", icaoCode))
                );
                String fileOutPath;
                if (fileOutput) {
                    fileOutPath = keyCorresFile.getAbsolutePath();
                } else fileOutPath = null;
                if ((exit = printRawDataFile(keyCorresFile, pr, fileOutPath)) != 0) {
                    return exit;
                }
                pr.println(Utilities.sectionSeparator("END RAW FILE"));
                if (fileOutput) {
                    System.out.printf("The raw data have been successfully written to the file: %s%n%n",
                                      keyCorresFile.getAbsolutePath());
                }
            }

            // executes the fileDecode method and returns its exitCode
            exit = weatherProcessor.fileDecode(keyCorresFile,
                                               pr,
                                               fileOutput);
            if (exit != 0) return exit;
        }
        return 0;
    }

    /**
     * This method asks the user for time precision using specified format.
     * Incorrect input format or the date in the future causes the method to take
     * the current time - 1 day.
     * If the date is too far in the past, the database might not be able to
     * provide enough data. Therefore, an input from more than 3 months ago results
     * in taking weather reports from the last 1 day.
     *
     * @param format The format to be used for the date/time specification.
     *
     * @return The date and time from which the weather information will be
     *         gathered.
     */
    @NotNull LocalDateTime getFromDateTime(DateTimeFormatter format) {
        LocalDateTime now = LocalDateTime.now(),
                      resultTime,
                      chosenDateTime;

        int defMonthsValue = 3, defDaysValue = 1;

        try {
            System.out.println("Incorrect date/time format or the date from more than %MON months ago will result in taking the current time - %DAY day."
                               .replace("%MON", String.valueOf(defMonthsValue))
                               .replace("%DAY", String.valueOf(defDaysValue)));
            System.out.printf("Please enter the \"from\" date in the following format: %s : ", dateTimeStrFormat);
            chosenDateTime =
                    LocalDateTime.parse(DialogCenter.getInput(false, false),
                                        format);
            if (chosenDateTime.isAfter  (now) ||
                chosenDateTime.isBefore (now.minusMonths(defMonthsValue))) {
                System.err.println("Selected date/time is either in the future or too far in the past.");
                System.err.println("Therefore, %DEF_DATE will be used."
                                   .replace("%DEF_DATE",
                                            normalizeDateTime(now.minusDays(defDaysValue))));
                return now.minusDays(defDaysValue);
            } else {
                System.out.println("%DATE is correct and will be used."
                                   .replace("%DATE",
                                           normalizeDateTime(chosenDateTime)));
                return chosenDateTime;
            }
        }
        catch (DateTimeParseException e) {
            System.err.println("Incorrect date/time format detected.");
            System.err.println("Default \"%DEF_DATE\" will be used."
                               .replace("%DEF_DATE",
                                       normalizeDateTime(now.minusDays(defDaysValue))));
            return now.minusDays(defDaysValue);
        }
    }

    /**
     * This method asks the user to precise the time until which the weather
     * information will be gathered. If the given date is in the future, or the
     * time format String is incorrect, the date is corrected to the current local
     * date and time. If provided time is before the {@code fromTime} parameter,
     * then {@code fromTime + 1} day is taken.
     *
     * @param fromTime This time information will be compared to the given time
     *                 for correctness ({@code fromtime} needs to be before the
     *                 {@code to} time).
     *
     * @param format   The format to be used for the date/time specification.
     *
     * @return Date and time up until which weather data will be downloaded and
     *         processed.
     */
    @NotNull LocalDateTime getToDateTime(@NotNull LocalDateTime fromTime, DateTimeFormatter format) {
        LocalDateTime now = LocalDateTime.now(),
                      resultTime;

        try {
            System.out.println("Incorrect date/time format will result in taking the current time or the \"from time + 1\".");
            System.out.printf("Please enter the \"to\" date in the following format: %s: ", dateTimeStrFormat);
            LocalDateTime chosenDateTime =
                    LocalDateTime.parse(DialogCenter.getInput(false, false),
                                        format);

            if (chosenDateTime.isAfter(now)) {
                System.out.println("There are no weather reports provided for the given date in the future." +
                                   "Current time %DEF_DATE will be used."
                                   .replace("%DEF_DATE",
                                           normalizeDateTime(now)));
                return now;
            }
            if (chosenDateTime.isBefore(fromTime)) {
                LocalDateTime correctToTime =
                        fromTime.plusDays(1).isBefore(now)
                            ? fromTime.plusDays(1)
                            : now;
                System.out.println("This time specification is not correct, because given \"from\" time is after the \"to\" time. " +
                                   "Therefore, %DATE will be used."
                                   .replace("%DATE",
                                           normalizeDateTime(correctToTime)));
                return correctToTime;
            } else {
                System.out.println("%DATE is correct and will be used."
                                   .replace("%DATE",
                                           normalizeDateTime(chosenDateTime)));
                return chosenDateTime;
            }
        } catch (DateTimeParseException e) {
            LocalDateTime correctToTime =
                    fromTime.plusDays(1).isBefore(now)
                            ? fromTime.plusDays(1)
                            : now;
            System.err.println("Incorrect date/time format detected.");
            System.err.println("Therefore, \"%DEF_DATE\" will be used."
                               .replace("%DEF_DATE",
                                       normalizeDateTime(correctToTime)));
            return correctToTime;
        }
    }

    /**
     * The method which only prints the specified {@code file} using the
     * {@code printer}.
     * @param   file    The file to be printed.
     * @param   printer The printer used to print the {@code file}.
     * @return  The exit code of the action. Any non-zero code means that an issue
     *          has occurred.
     */
    int printRawDataFile(@NotNull File file, PrintStream printer, @Nullable String fileOutputPath) {
        try (BufferedReader br =
                     new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) !=  null) {
                printer.println(line);
            }
            if (fileOutputPath != null) {
                System.out.printf("Raw METARs were successfully written to the %s file.%n%n",
                                  fileOutputPath);
                fileOutputPath = null; // setting the value to null in order to avoid repeated messages for the same file
            }
        } catch (IOException ignored) {
            return 1;
        }
        return 0;
    }

    /**
     * The method used for formatting the ISO {@code LocalDateTime} parameter
     * into the String.
     * @param dateTime The date/time to be formatted.
     * @return The formatted String of dateTime parameter.
     */
    private static String normalizeDateTime(LocalDateTime dateTime) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);
        return dateTime.format(format);
    }
}
