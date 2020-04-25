package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * The METARProcessor is the class responsible for parsing, modifying
 * and processing all the temporary .txt files with METARs.
 * This class processes, parses and translates all the files into the
 * comprehensible language.
 */
class METARProcessor {

    private static final Map<String, String> metarDict = new HashMap<>();

    void metarTranslate(@NotNull File metarToTranslate, @NotNull PrintStream printer) {
        setMetarDict();
        boolean inSection = false;
        int     sectionCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(metarToTranslate))) {
             String line, sectionBound = null;

             while ((line = br.readLine()) != null && !line.startsWith("# No METAR/SPECI reports")) {
                 if (line.isEmpty()) continue;
                 if (sectionBound == null)
                     sectionBound = String.valueOf(line.charAt(0)).repeat(3);
                 if (line.contains(sectionBound)) {
                     inSection = !inSection;
                     if (inSection) sectionCount++;
                     continue;
                 }
                 line = line.startsWith("#")
                         ? line.substring(2)
                         : line;

                 switch (sectionCount) {
                     case 1:
                         do {
                             if (line.contains("Query")) {
                                 printer.printf("The weather information has been downloaded at: %s.%n", line.substring(line.indexOf("at") + 3));
                             } else {
                                 printer.printf("The weather information ranges %s.%n", line.substring(line.indexOf("from")));
                             }
                             line = br.readLine();
                         } while (!line.contains(sectionBound));
                         inSection = !inSection;
                         break;
                     case 2:
                         printer.printf("Airport in question: %s. %n", line);
                         do {
                            line = br.readLine();
                         } while (!line.contains(sectionBound));
                         inSection = !inSection;
                         break;
                     case 3:
                         printer.println(sectionBound.repeat(5));
                         do {
                             line = br.readLine();                          //don't care about text in the section
                         } while (!line.contains(sectionBound));
                         inSection = !inSection;
                         do {
                             StringBuilder metarEntry = new StringBuilder();
                             do {
                                 line = br.readLine();
                                 metarEntry.append(line.stripLeading()).append(" ");
                             } while (!line.contains("="));
                             translate(metarEntry.toString().replaceAll("=",""), printer);
                         } while (!line.contains(sectionBound));
                         break;
                     case 4:
                         break;
                     default:
                         throw new IOException();
                 }
             }
             if (line != null) {
                 //then the only reason for the while loop to end is that the line ~= "# No METAR/SPECI reports ..."
                 //which means that there's no METAR report in the database
                 printer.println(line.substring(line.indexOf("No ")));
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void translate(@NotNull String metarEntry, @NotNull PrintStream printer) {
        boolean autoMetar = metarEntry.contains("AUTO");
        if (autoMetar)
            metarEntry = metarEntry.replaceFirst("AUTO", "");

        String[] tokens = metarEntry.split(" ");
        String  timeAndDate = tokens[0],
                metarType   = tokens[1];

        LocalDateTime metarIssuedAt =
                LocalDateTime.parse(tokens[0],
                                    DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        printer.printf("The %s was issued %son %s-%s-%s at %s:%s UTC time.%n",
                       metarType,
                       autoMetar ? "automatically without human intervention " : "",
                       metarIssuedAt.getDayOfMonth(),  metarIssuedAt.getMonth(),
                       metarIssuedAt.getYear(),        metarIssuedAt.getHour(),
                       metarIssuedAt.getMinute());

        for (int i = 4; i < tokens.length; i++) { //0 -> dateTime, 1 -> metar type, 2 -> airport (already noted), 3 -> day of (unspecified) month and time in zulu
            if (tokens[i].matches("[VRB0-9]{3,}(G[0-9]{2})?(KT|MPS)")) {
                String spdUnit = tokens[i].contains("KT")
                            ? "knots"
                            : "meters per second";
                String  windDirection = tokens[i].startsWith("VRB")
                            ? "is variable"
                            : "blows from " + tokens[i].substring(0, 3);        //the substring is the direction 000 - 360
                String  windSpeed   = tokens[i].substring(3, 5),
                        gusts = tokens[i].contains("G")
                            ? " with gusts of " + tokens[i].substring(6, 8) + " " + spdUnit
                            : "";

                printer.printf("%s: The wind %s at %s %s%s.%n",
                                tokens[i], windDirection,
                                windSpeed, spdUnit,
                                gusts
                              );
            }
            if (tokens[i].matches("[0-9]{3}V[0-9]{3}")) {
                printer.printf("%s: The wind direction is varying between %s degrees and %s degrees.",
                               tokens[i], tokens[i].substring(0, 3), tokens[i].substring(4, 6));
            }
            // TODO: 26/04/2020 Doplň parsovanie metaru podla struktury na stranke http://meteocentre.com/doc/metar.html 
        }
    }

    void setMetarDict() {
        try (BufferedReader br = new BufferedReader(new FileReader("metarDictionary.txt"))) {
            String dictEntry;
            while ((dictEntry = br.readLine()) != null) {
                String[] tokens = dictEntry.split("=");
                metarDict.put(tokens[0], tokens[1]);
            }
        } catch (IOException ignored) { }
    }
}
