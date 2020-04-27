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
class METARDecoder {

    private static final Map<String, String> metarDict = new HashMap<>();
    private static final String wndPattern      = "((VRB)[0-9]{2}|[0-9]{5})(G[0-9]{2})?(KT|MPS)",
                                vartnPattern    = "[0-9]{3,4}V[0-9]{3,4}",
                                vsbltyPattern   = "[0-9 /]{1,5}(SM)?",
                                rvrPattern      = "R[0-9]{2}[LCR]?/(P|M|[0-9]+V)?[0-9]+(FT/)?[DNU]?",
                                weatherPattern  = "[+-]?[a-zA-Z]{2,}",
                                tempPattern     = "M?[0-9]{2}/M?[0-9]{2}";
    private static final double knotsToKmH = 1.852,
                                ftToM      = 0.3048;


    /**
     * translator
     *
     * @param metarToDecode param
     * @param printer param
     */
    void fileDecode(@NotNull File metarToDecode, @NotNull PrintStream printer) {
        setMetarDict();
        boolean inSection = false;
        int     sectionCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(metarToDecode))) {
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
                         ? line.substring(2) //cuts off the initial "# " on the line
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
                         printer.println(sectionSeparator(line.substring(0, 4)));
                         printer.printf("Airport location: %s. %n", line);
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
                             decode(metarEntry.toString().replace("=",""), printer);
                         } while (!line.contains(sectionBound));
                         break;
                     case 4:
                         break;
                     default:
                         throw new IOException();
                 }
             }
             if (line != null) {
                 //then the only reason for the while loop to have ended is that the line ~= "# No METAR/SPECI reports ..."
                 //which means that there's no METAR report in the database
                 printer.println(line.substring(line.indexOf("No ")));
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void decode(@NotNull String metarEntry, @NotNull PrintStream printer) {
        boolean autoMetar = metarEntry.contains("AUTO");
        if (autoMetar)
            metarEntry = metarEntry.replaceFirst("AUTO", "");

        String[] tokens = metarEntry.split("\\s+");
        String  timeAndDate = tokens[0],
                metarType   = tokens[1];

        LocalDateTime metarIssuedAt =
                LocalDateTime.parse(tokens[0],
                                    DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        printer.printf("The %s was issued %son %s-%s-%s at %s:%s UTC time.%n",
                       metarType,
                       autoMetar ? "automatically, with no human intervention or oversight " : "",
                       metarIssuedAt.getDayOfMonth(),  metarIssuedAt.getMonth(),
                       metarIssuedAt.getYear(),        metarIssuedAt.getHour(),
                       metarIssuedAt.getMinute());

        for (int i = 4; i < tokens.length; i++) { //0 -> dateTime, 1 -> metar type, 2 -> airport (already noted), 3 -> day of (unspecified) month and time in zulu
            if (tokens[i].matches(wndPattern)) {    //wind direction and speed information
                printer.println(windDirSpd(tokens[i], wndPattern));
                continue;
            }
            if (tokens[i].matches(vartnPattern)) {
                printer.println(windVariation(tokens[i], vartnPattern));
                continue;
            }
            if (tokens[i].matches(vsbltyPattern)) {
                printer.println(visibility(tokens[i], vsbltyPattern));
                continue;
            }
            if (tokens[i].matches("[0-9]+") && (i + 1 < tokens.length) &&
                tokens[i + 1].matches(vsbltyPattern)) {
                printer.println(visibility(tokens[i] + " " + tokens[i + 1], vsbltyPattern));
                i++;
                continue;
            }
            if (tokens[i].matches(rvrPattern)) {
                printer.println(rvrVisibility(tokens[i], rvrPattern));
                continue;
            }
            if (tokens[i].matches(weatherPattern)) {
                printer.println(weatherPhenomena(tokens[i], weatherPattern));
            }
            if (tokens[i].matches(tempPattern)) {
                printer.println(temperature(tokens[i], tempPattern));
            }
            if (tokens[i].matches("[a-zA-Z]+")) {
                String result = metarDict.get(tokens[i]);
                if (result != null) {
                    printer.printf("%s: %s.%n", tokens[i], result);
                }
            }
            // TODO: 26/04/2020 Doplň parsovanie metaru podla struktury na stranke http://meteocentre.com/doc/metar.html
        }
    }

    // TODO: 27/04/2020 Documentation TBD 
    /**
     * TBD
     * @param token TBD
     * @param tempPattern TBD
     * @return TBD
     */
    static @NotNull String temperature(@NotNull String token, @NotNull String tempPattern) {
        initDecode(token, tempPattern);

        String[] temps   = token.split("/");
        String  temp     = temps[0].startsWith("M")
                                ? temps[0].replace("M", "-")
                                : temps[0],
                dewPoint = temps[1].startsWith("M")
                                ? temps[1].replace("M","-")
                                : temps[1];
        String  unit = "degrees";
        return "%TOKEN\nTemperature: %TEMP %UNIT.\nDewpoint   : %DEWPOINT %UNIT."
                .replace("%TOKEN", sectionSeparator(token))
                .replace("%TEMP", temp)
                .replace("%DEWPOINT", dewPoint)
                .replaceAll("%UNIT", unit);
    }

    // TODO: 27/04/2020 finish the documentation
    /**
     * TBD
     * @param token TBD
     * @param weatherPattern TBD
     * @return TBD
     */
    static @NotNull String weatherPhenomena(@NotNull String token, String weatherPattern) {
        initDecode(token, weatherPattern);

        String modifier, tokenBUp = token;
        switch (token.charAt(0)) {
            case '+':
                modifier = metarDict.get("+");
                token = token.substring(1);
                break;
            case '-':
                modifier = metarDict.get("-");
                token = token.substring(1);
                break;
            default:
                modifier = "moderate";
                break;
        }

        String  phenomenon = metarDict.get(token);
        StringBuilder sb = new StringBuilder();

        if (phenomenon == null) {
            for (int i = 0; i < token.length() / 2; i++) {
                String  phenomenKey   = token.substring(2*i, 2*(i+1)),
                        phenomenValue = metarDict.get(phenomenKey);
                if (phenomenValue != null) {
                    sb.append(phenomenValue).append(" ");
                }
            }
            phenomenon = sb.toString().strip();
        }
        return "Weather: %TOKEN = %MODIFIER %PHENOMENON."
                .replace("%TOKEN", tokenBUp)
                .replace("%MODIFIER", modifier)
                .replace("%PHENOMENON",phenomenon);
    }

    /**
     * This method translates the visibility measured at runway level using while
     * recognizing the different formats and modifiers (Europe/US, Canada) for
     * specified airport.
     *
     * @param token the runway visibility string to be translated
     * @param rvrPattern regular expression to be matched with {@code token}
     * @return the translation of at-runway level visibility information
     */
    static @NotNull String rvrVisibility(@NotNull String token, String rvrPattern) {
        initDecode(token, rvrPattern);

        int    slash      = token.indexOf('/');
        String rwyID      = token.substring(1, slash),
               units      = token.contains("FT") ? "feet" : "meters";
        String modifier   = "",
               tokenBUp   = token;

        switch (token.substring(slash, slash + 2)) {
            case "/P":
                modifier = "more than ";
                token = token.replace("P", "");
                break;
            case "/M":
                modifier = "less than ";
                token = token.replace("M","");
                break;
            default:
                if (token.substring(slash)
                         .matches("/" + vartnPattern + ".*")) {
                    int _V = token.indexOf('V');
                    String fVis = token.substring(slash + 1, _V),    // the variable visibility value is expected
                           sVis = token.substring(_V + 1, _V + 5);   // to have format nnnnVnnnn where n = [0-9]

                    modifier = "variable between %FVIS %UNIT and %SVIS %UNIT "
                                .replace("%FVIS", fVis)
                                .replace("%SVIS", sVis);
                }
                break;
        }
        String trend    = token.contains("U")
                            ?   "rising"
                            :   token.contains("D")
                                    ? "falling" : "no change";
        String value    = !modifier.startsWith("variable")
                                ? token.substring(slash + 1, slash + 5)
                                : "";

        String conversion = token.contains("FT")
                ? conversion(true, value, ftToM, "meters")
                : "";

        return "%SECTION\nRunway %RUNWAY, touchdown zone visual range is %MODIFIER%VALUE %UNIT%CONVERSION and %TREND is expected."
               .replace("%SECTION",sectionSeparator(tokenBUp))
               .replace("%RUNWAY",rwyID)
               .replace("%MODIFIER", modifier)
               .replace("%VALUE", value)
               .replaceAll("%UNIT", units)
               .replace("%CONVERSION", conversion)
               .replace("%TREND", trend);
    }

    /**
     * The visibility translator which looks at the visibility unit and returns
     * correct value.
     *
     * @param token the visibility string to be translated
     * @param vsbltyPattern regular expression to be matched with {@code token}
     * @return the information about visibility using correct units.
     */
    static @NotNull String visibility(@NotNull String token, String vsbltyPattern) {
        initDecode(token, vsbltyPattern);

        if (token.equals("9999"))
            return token + ": The visibility is 10 km or more.";
        if (token.equals("0000"))
            return token + ": The visibility is 50 meters or less.";

        int smIndex = token.indexOf("SM");
        String visibility = smIndex == -1
                ? token + " meters"
                : "%VISIB %UNIT"
                  .replace("%VISIB",token.substring(0, smIndex))
                  .replace("%UNIT",metarDict.get("SM"));

        return "%TOKEN\nMaximum horizontal visibility: %VISIBILITY."
                .replace("%TOKEN", sectionSeparator(token))
                .replace("%VISIBILITY", visibility);
    }

    /**
     * This method takes {@code windStr} parameter and the regular expression
     * this parameter matches and explains the numbers in the appropriate way.
     * The method should function correctly as-is even called in unit tests with
     * provided example and pattern.
     *
     * @param windStr is the {@code String} to translate. It has to match provided
     *                {@code pattern} expression.
     *
     * @param pattern regular expression to be matched with {@code windStr}
     *
     * @return {@code windStr} with added explanation.
     */
    static @NotNull String windDirSpd(@NotNull String windStr, String pattern) {
        initDecode(windStr, pattern);                              //normally the pattern is dddssUU(U) or dddssGssUU(U) where d -> direction, s -> speed and U -> unit char

        if (windStr.equalsIgnoreCase("00000KT"))
            return "%TOKEN: The wind is calm.".replace("%TOKEN", windStr);

        String spdUnit = windStr.contains("KT")
                ? metarDict.get("KT")
                : "meters per second";
        boolean conversionNeeded = "knots".equals(spdUnit);
        String  windDirection = windStr.startsWith("VRB")
                ? "is variable"
                : "blows from " + windStr.substring(0, 3) + " degrees";         // the substring is the direction 000 - 360
        String  windSpeed   = windStr.substring(3, 5),                          // the substring is the wind speed
                gusts = windStr.contains("G")
                        ? " with gusts of %GUSTS %UNIT%CONVERSION"
                          .replace("%GUSTS",windStr.substring(6, 8))
                          .replace("%UNIT", spdUnit)
                          .replace("%CONVERSION", conversion(conversionNeeded,windStr.substring(6, 8), knotsToKmH, "km/h"))
                        : "";
        String conversion =
                conversionNeeded
                    ? conversion(true,windSpeed, knotsToKmH, "km/h")
                    : "";

        return "%TOKEN\nWind: The wind %DIRECTION at %SPEED %UNIT%CONVERSION%GUSTS."
                .replace("%TOKEN",  sectionSeparator(windStr))
                .replace("%DIRECTION", windDirection)
                .replace("%SPEED", windSpeed)
                .replace("%UNIT", spdUnit)
                .replace("%CONVERSION", conversion)
                .replace("%GUSTS", gusts);
    }

    /**
     * This method takes {@code windStr} parameter which represents the wind
     * variation and translates it accordingly. The method should function as-is
     * even when called in unit tests.
     *
     * @param windStr is the {@code String} to translate. It has to match provided
     *                {@code pattern} expression.
     * @param pattern regular expression to be matched with {@code windStr}
     * @return the {@code String} which explains the wind variation
     */
    static @NotNull String windVariation(@NotNull String windStr, String pattern) {
        initDecode(windStr, pattern);

        int vPos = windStr.indexOf("V");
        String  firstWind   = windStr.substring(0, vPos),
                secondWind  = windStr.substring(vPos + 1);

        return "%TOKEN\nVariable wind: The wind direction varies between %FWIND degrees and %SWIND degrees.\n"
                .replace("%TOKEN", sectionSeparator(windStr))
                .replace("%FWIND", firstWind)
                .replace("%SWIND", secondWind) +
               "The wind direction has varied by 60 degrees or more in last 10 minutes with the mean speed exceeding 3 knots.";
    }

    /**
     * Reads the .txt file with the dictionary used for METAR/TAF terminology
     * and creates {@code Map<String,String> metarDict} map.
     *
     * @see #metarDict
     */
    static void setMetarDict() {
        try (BufferedReader br = new BufferedReader(new FileReader("metarDictionary.txt"))) {
            String dictEntry;
            while ((dictEntry = br.readLine()) != null) {
                String[] tokens = dictEntry.split("=");
                metarDict.put(tokens[0], tokens[1]);
            }
        } catch (IOException ignored) { }
    }

    static @NotNull String sectionSeparator(String sectionName) {
        return "----------------------------------------------- %s ------------------------------------------------"
               .replace("%s", sectionName);
    }

    private static void initDecode(@NotNull String token, @NotNull String pattern) {
        assert token.matches(pattern);
        if (metarDict.isEmpty()) setMetarDict();
    }

    private static @NotNull String conversion(boolean conversionNeeded, String valueToConvert, double constant, String finalUnit) {
        if (conversionNeeded)
            return  " (%s %UNIT)"
                    .replace("%s", Airport.constantConverter(valueToConvert, constant))
                    .replace("%UNIT", finalUnit);
        else return "";
    }
}
