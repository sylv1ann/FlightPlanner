package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * The METARProcessor is the class responsible for parsing, modifying
 * and processing all the .txt files with METARs.
 * This class processes and decodes all the files into the
 * comprehensible language.
 */
class METARDecoder {

    private static final Map<String, String> metarDict = new HashMap<>();
    private static final String windPttrn     = "((VRB)[0-9]{2}|[0-9]{5})(G[0-9]{2})?(KT|MPS)",
                                vartnPttrn    = "[0-9]{3,4}V[0-9]{3,4}",
                                vsbltyPttrn   = "[0-9]{4}|[0-9 /.]{1,5}SM",
                                rvrPttrn      = "R[0-9]{2}[LCR]?/(P|M|[0-9]+V)?[0-9]+(FT)?[/DNU]?",
                                weatherPttrn  = "(RE|[+-])?[a-zA-Z]{2,}",
                                tempPttrn     = "M?[0-9]{2}/M?[0-9]{2}",
                                cloudPttrn    = "(SKC|FEW|BKN|SCT|OVC|CLR)[0-9]{3}(CB|TCU|///)?",
                                vrtclVisPttrn = "VV[0-9]{3}",
                                pressurePttrn = "[AQ][0-9]{4}",
                                windshrPttrn  = "WS (ALL RWY|(RWY[0-9]{2}[LCR]?))",
                                slpPttrn      = "SLP[0-9]{3}",
                                rsgPttrn      = "[0-9]{2}[0-9/]{6}";
    private static final double knotsToKmH  = 1.852,
                                ftToM       = 0.3048,
                                inchTohPa   = 1/2.953,
                                hPaToInch   = 1/(100 * inchTohPa);

    /**
     * The method which takes the file denoted by {@code metarToDecode} and
     * progressively translates its different sections of the METAR.
     *
     * @param metarToDecode The fixed METAR file to be decoded.
     * @param printer       The printer used for printing.
     */
    void fileDecode(@NotNull File metarToDecode, @NotNull PrintStream printer) {
        setMetarDict();
        boolean inSection = false;
        int     sectionCount = 0;
        StringBuilder initInfo = new StringBuilder();

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
                                 initInfo.append("The weather information has been downloaded at: %s.\n"
                                                 .replace("%s", line.substring(line.indexOf("at") + 3)));
                             } else {
                                 initInfo.append("The weather information ranges %s.\n"
                                                .replace("%s",line.substring(line.indexOf("from"))));
                             }
                             line = br.readLine();
                         } while (!line.contains(sectionBound));
                         inSection = !inSection;
                         break;
                     case 2:
                         initInfo.append(sectionSeparator(line.substring(0, 4))).append("\n");
                         initInfo.append("Airport location: %s.\n".replace("%s",line));
                         do {
                            line = br.readLine();
                         } while (!line.contains(sectionBound));
                         inSection = !inSection;
                         break;
                     case 3:
                         printer.println(sectionBound.repeat(5));         //writes string "###############"
                         do {
                             line = br.readLine();                              //don't care about text in the section
                         } while (!line.contains(sectionBound));
                         inSection = !inSection;
                         boolean refuseDecode = false,
                                 metarSectionEnd = false,
                                 tokenPrint =
                                    DialogCenter.getResponse(null,
                                                             "Should the token names be printed too? %OPT: ",
                                                             "Y",
                                                             true);
                         do {
                             StringBuilder metarEntry = new StringBuilder();
                             do {
                                 line = br.readLine();
                                 if (line.startsWith("# ")) {
                                     metarSectionEnd = true;
                                     break;
                                 }
                                 metarEntry.append(line.stripLeading()).append(" ");
                             } while (!line.contains("="));
                             if (metarSectionEnd) break;
                             if (!refuseDecode &&
                                 DialogCenter.getResponse(null,
                                                          "Do you want \"%METAR\" to be decoded? %OPT: "
                                                                  .replace("%METAR",metarEntry.toString().strip()),
                                                          "Y",
                                                          false)) {
                                 decode(metarEntry.toString().replace("=", ""),
                                        printer,
                                        initInfo.toString(),
                                        tokenPrint);
                                 printer.printf("%n");
                             } else refuseDecode = true;
                         } while (!line.contains(sectionBound));
                         break;
                     case 4:
                         printer.println("FINISH THE 4th SECTION");
                         break;
                     default:
                         printer.println("Section num" + sectionCount);
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

    /**
     * Decode a METAR unit specified in the {@code metarEntry}. The fixed-structured
     * METAR is divided into tokens. Each token is treated based on the category it
     * belongs to.
     *
     * @param metarEntry The actual METAR unit which will be decoded.
     * @param printer The printer used for printing.
     * @param initInfo The initial information which will be printed before the
     *                 decoded METAR.
     * @param tokenPrint The flag which indicates whether each token should be
     *                   highlighted before its translation.
     */
    void decode(@NotNull String metarEntry, @NotNull PrintStream printer, String initInfo, boolean tokenPrint) {
        boolean autoMetar = metarEntry.contains("AUTO");
        if (autoMetar)
            metarEntry = metarEntry.replaceFirst("AUTO", "");

        String[] tokens = metarEntry.split("\\s+");
        String   timeAndDate = tokens[0],
                 metarType   = tokens[1];

        LocalDateTime metarIssuedAt =
                LocalDateTime.parse(tokens[0],
                                    DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        printer.println(initInfo);
        printer.printf("The %s was issued %son %s-%s-%s at %s:%s UTC time.%n",
                       metarType,
                       autoMetar ? "automatically, with no human intervention or oversight " : "",
                       metarIssuedAt.getDayOfMonth(),  metarIssuedAt.getMonth(),
                       metarIssuedAt.getYear(),        metarIssuedAt.getHour(),
                       metarIssuedAt.getMinute());

        for (int i = 4; i < tokens.length; i++) { //0 -> dateTime, 1 -> metar type, 2 -> airport (already noted), 3 -> day of (unspecified) month and time in zulu
            if (tokens[i].matches(windPttrn)) {    //wind direction and speed information
                printer.println(windDirSpd(tokens[i], windPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(vartnPttrn)) {
                printer.println(windVariation(tokens[i], vartnPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(vsbltyPttrn)) {
                printer.println(visibility(tokens[i], vsbltyPttrn, tokenPrint));
                continue;
            }
            //the following if condition treats the "1 1/4SM" example
            if (tokens[i].matches("[0-9]+") && (i + 1 < tokens.length) &&
                tokens[i + 1].matches(vsbltyPttrn)) {
                printer.println(visibility(tokens[i] + " " + tokens[i + 1], vsbltyPttrn, tokenPrint));
                i++;
                continue;
            }
            if (tokens[i].matches(rvrPttrn)) {
                printer.println(rvrVisibility(tokens[i], rvrPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(vrtclVisPttrn)) {
                printer.println(verticalVisibility(tokens[i], vrtclVisPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(weatherPttrn)) {
                printer.println(weatherPhenomena(tokens[i], weatherPttrn, tokens[i].startsWith("RE"), tokenPrint));
                continue;
            }
            if (tokens[i].matches(cloudPttrn)) {
                printer.println(cloudLayer(tokens[i], cloudPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(tempPttrn)) {
                printer.println(temperature(tokens[i], tempPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(pressurePttrn)) {
                printer.println(pressure(tokens[i], pressurePttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(windshrPttrn)) {
                printer.println(windshearWarning(tokens[i], windshrPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(slpPttrn)) {
                printer.println(seaLvlPressure(tokens[i], slpPttrn, tokenPrint));
                continue;
            }
            if (tokens[i].matches(rsgPttrn)) {
                printer.println(rwyStateGroup(tokens[i], rsgPttrn, tokenPrint));
            }
            if (tokens[i].matches("[a-zA-Z]+")) {
                String result = metarDict.get(tokens[i]);
                if (result != null) {
                    printer.printf("%s: %s.%n", tokens[i], result);
                }
            } else {
                printer.println(tokens[i] + ": Unknown token.");
            }
        }
    }

    /**
     *
     * @param token
     * @param rsgPttrn
     * @param tokenPrint
     * @return
     */
    static @NotNull String rwyStateGroup(@NotNull String token, @NotNull String rsgPttrn, boolean tokenPrint) {
        String init = initTokenDecoder(token,rsgPttrn,tokenPrint);
        // the token contains only numbers, "/" or substring "CLRD" on indexes 2 - 5

        boolean cleared  = token.contains("CLRD");
        String  rwyModif = getRwyModificator(token.substring(0,2)); // rwy modificator is always present, however it's value has to be parsed
        String  brkAction = getBrakingAction(token.substring(6));

        if (cleared) {
            return "%INITRunway state descriptor: Runway %RWY cleared, %BRKACT"
                    .replace("%INIT", init)
                    .replace("%RWY", rwyModif)
                    .replace("%BRKACT", brkAction);
        }

        char    contaminationType    = token.charAt(2),   // runway deposit position
                contaminationExtent = token.charAt(3);    // runway contamination extent position

        String  contaminationDepth = getContaminationDepth(token.substring(4, 6));

        return "%INITRunway state descriptor:\n\tRunway(s) concerned: %RWY\n\t %vCON: %CONTAM\n\t %vEXT: %EXTENT\n\t%vDEP: %DEPTH\n\t%vBR: %BRACT"
                .replace("%INIT", init)
                .replace("%RWY", rwyModif)
                .replace("%vCON", String.valueOf(contaminationType))
                .replace("%CONTAM", getRwyContamination(contaminationType))
                .replace("%vEXT", String.valueOf(contaminationExtent))
                .replace("%EXTENT", getContaminationExtent(contaminationExtent))
                .replace("%vDEP", token.substring(4,6))
                .replace("%DEPTH", contaminationDepth)
                .replace("%vBR", token.substring(6))
                .replace("%BRACT", brkAction);
    }

    /**
     *
     * @param arg
     * @return
     */
    static String getRwyContamination(char arg) {
        Map<Character, String> rwyDepositDict = new HashMap<>();
        rwyDepositDict.put('0', "Clear and dry");
        rwyDepositDict.put('1', "Damp");
        rwyDepositDict.put('2', "Wet or water patches");
        rwyDepositDict.put('3', "This frost cover");
        rwyDepositDict.put('4', "Dry snow");
        rwyDepositDict.put('5', "Wet snow");
        rwyDepositDict.put('6', "Slush");
        rwyDepositDict.put('7', "Ice");
        rwyDepositDict.put('8', "Compacted or rolled snow");
        rwyDepositDict.put('9', "Frozen ruts or ridges");
        rwyDepositDict.put('/', "Type of deposit not reported");

        return rwyDepositDict.get(arg);
    }

    /**
     *
     * @param arg
     * @return
     */
    static String getContaminationExtent(char arg) {
        Map<Character, String> rwyContamiDict = new HashMap<>();
        rwyContamiDict.put('1', "Less than 10%");
        rwyContamiDict.put('2', "11% to 25%");
        rwyContamiDict.put('5', "26% to 50%");
        rwyContamiDict.put('9', "51% to 100%");
        rwyContamiDict.put('/', "Not reported");

        return rwyContamiDict.get(arg);
    }

    /**
     *
     * @param arg
     * @return
     */
    static String getContaminationDepth(String arg) {
        Map<String, String> depositDepth   = new HashMap<>();
        depositDepth.put("00", "Less than 1mm");
        //values between 01 - 90 correspond to their value in millimeters
        depositDepth.put("91", "Not used/Incorrect value");
        depositDepth.put("98", "40cm or more");
        depositDepth.put("99", "Non-operational due to snow, slush, ice or rwy clearance, but depth not reported");
        depositDepth.put("//", "Depth of deposit operationally not significant or measurable.");

        try {
          int argInt = Integer.parseInt(arg);
          if (Utilities.isBetween(argInt, 1, 90)) {
              return "%VAL mm".replace("%VAL", arg);
          } else {
              if (Utilities.isBetween(argInt, 92, 97)) {
                  final int step = 5, baseKey = 92, baseValue = 10;
                  return "%VAL cm".replace("%VAL", String.valueOf(baseValue + (argInt - baseKey) * step ));
              } else
                    throw new NumberFormatException();
          }
        } catch (NumberFormatException ex) {
            // should be only the case when the argument is "//"
            return depositDepth.get(arg);
        }
    }

    /**
     * This method takes {@code token} parameter which represents the temperature
     * encoding and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param rwy the runway number
     * @return the runway identificator represented in the {@code rwy}
     */
    static @NotNull String getRwyModificator(String rwy) {
        int rwyID = Integer.parseInt(rwy); // when invoked from
        String rwyModif = "";
        if (rwyID == 88) {
            rwyModif = "All runways";
        } else {
            if (Utilities.isBetween(rwyID, 0, 36)) {
                rwyModif = "%RWY or %RWYL"
                        .replaceAll("%RWY", rwy);
            } else if (Utilities.isBetween(rwyID, 50, 86)) {
                rwyModif = "%RWYR".replace("%RWY", rwy);
            }
        }
        return rwyModif;
    }

    /**
     * The method which decodes the {@code arg} representing the braking coefficient
     *
     * @param arg the braking action to be explained
     * @return the value of the braking action
     */
    static @NotNull String getBrakingAction(String arg) {
        Map<String, String> brakingFricti = new HashMap<>();

        brakingFricti.put("91", "action poor");
        brakingFricti.put("92", "action medium/poor");
        brakingFricti.put("93", "action medium");
        brakingFricti.put("94", "action medium/good");
        brakingFricti.put("95", "action good");
        brakingFricti.put("99", "information unreliable");
        brakingFricti.put("//", "action not reported");

        try {
            int intArg = Integer.parseInt(arg);
            if (Utilities.isBetween(intArg,0,90)) {
                return "Braking friction coefficient: %VAL".replace("%VAL", arg);
            } else {
                // when arg value is > 90, it is special and mentioned in the dictionary
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            // if invoked correctly (which is the case always except for testing)
            // returns the value from the dictionary -> 91 - 95, 99 or //
            return "Braking %DICTVAL"
                   .replace("%DICTVAL", brakingFricti.get(arg));
        }
    }

    /**
     * This method takes {@code token} parameter which represents the sea-level
     * pressure (SLP) and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param token the SLP to be translated
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to highlight also the corresponding token name
     * @return the information about sea-level pressure
     */
    static @NotNull String seaLvlPressure(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

        String  seaLevelPressure = "",
                conversion       = "";
        try {
            double slPressure = Double.parseDouble(token.substring(3)) / 10;
            if (slPressure >= 50.0)
                seaLevelPressure = "9%f".replace("%f", String.valueOf(slPressure));
            else
                seaLevelPressure = "10%f".replace("%f", String.valueOf(slPressure));
            conversion = Utilities.conversion(true,
                                    seaLevelPressure.substring(0,seaLevelPressure.indexOf(".")),
                                    hPaToInch,
                                    "inches");
        }
        catch (NumberFormatException ignored) { }

        return "%INITSea-level pressure: %VALUE%CONVERSION Beware of the possible difference with QNH!"
                .replace("%INIT", init)
                .replace("%VALUE", seaLevelPressure)
                .replace("%CONVERSION", conversion);
    }

    /**
    * This method explains the warning of possible wind shear existence in the
    * vicinity of the airport.
    * The method should function as-is even when called in unit tests.
    *
    * @param token wind-shear token to be translated
    * @param pattern regular expression to be matched with {@code token}
    * @param tokenPrint user's decision to highlight also the corresponding token name
    * @return wind shear warning explanation
    */
    static @NotNull String windshearWarning(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

        String runway;
        if (token.contains("ALL"))
            runway = "all runways.";
        else
            runway = "runway %s".replace("%s", token.substring(token.indexOf("Y") + 1));

        return "%INITWARNING! WINDSHEAR was detected on %RUNWAY."
               .replace("%INIT", init)
               .replace("%RUNWAY", runway);
    }

    /**
     * This method takes {@code token} parameter which represents the pressure
     * information and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param token encoded pressure information to be translated
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to highlight also the corresponding token name
     * @return the information about pressure using correct units.
     */
    static @NotNull String pressure(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

        String  value = token.substring(1),
                unit  = token.startsWith("Q") ? "hPa" : "inches",
                conversion = "hPa".equals(unit)
                                ? Utilities.conversion(true,value,hPaToInch,"inches")
                                : Utilities.conversion(true,value,inchTohPa,"hPa");

        if ("inches".equals(unit)) {
            value = "%fP.%sP"
                   .replace("%fP", value.substring(0,2))
                   .replace("%sP", value.substring(2));
        }

        return "%INITSea level pressure (QNH): %VALUE %UNIT%CONVERSION."
                .replace("%INIT", init)
                .replace("%VALUE", value)
                .replace("%UNIT", unit)
                .replace("%CONVERSION", conversion);
    }

    /**
     * This method takes {@code token} parameter which represents the vertical
     * visibility and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param token the vertical visibility representation to be translated
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to highlight also the corresponding token name
     * @return the information about vertical visibility using correct units.
     */
    static @NotNull String verticalVisibility(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token,pattern, tokenPrint);

        String  value = token.substring(2,5),
                conversion = Utilities.conversion(true,value,100,"meters");


        return "%INITVertical visibility: %VALUE feet%CONVERSION."
                .replace("%INIT", init)
                .replace("%VALUE", value)
                .replace("%CONVERSION", conversion);
    }

    /**
     * This method takes {@code token} the cloud type and height representation
     * and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param token the clound representation to be translated
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to highlight also the corresponding token name
     * @return the cloud information explanation
     */
    static @NotNull String cloudLayer(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token,pattern, tokenPrint);

        assert token.length() >= 6;
        String layerType = metarDict.get(token.substring(0, 3));
        if (layerType == null) layerType = "unknown layer type";
        String  height      = token.substring(3,6),
                layerHeight = Utilities.constantConverter(height,100), //i.e. BKN030 means broken at 3000 feet
                conversion  = Utilities.conversion(true,layerHeight, ftToM, "meters");
        String appendix;
        switch (token.substring(6)) {
            case "///":
                appendix = ", cloud type convection is unknown.";
                break;
            case "TCU":
                appendix = ", towering cumulus.";
                break;
            case "CB":
                appendix = ", cumulonimbus.";
                break;
            default:
                appendix = ".";
                break;
        }

        return "%INITClouds: A %LAYER detected at %HEIGHT feet%CONVERSION above aerodrome level%APPENDIX"
                .replace("%INIT", init)
                .replace("%LAYER",layerType)
                .replace("%HEIGHT", layerHeight)
                .replace("%CONVERSION", conversion)
                .replace("%APPENDIX", appendix);
    }

    /**
     * This method takes {@code token} parameter which represents the temperature
     * encoding and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param token the temperature to be translated
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to highlight also the corresponding token name
     * @return the decoded temperature
     */
    static @NotNull String temperature(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

        String[] temps   = token.split("/");
        String  temp     = temps[0].startsWith("M")
                                ? temps[0].replace("M", "-")
                                : temps[0],
                dewPoint = temps[1].startsWith("M")
                                ? temps[1].replace("M","-")
                                : temps[1];
        String  unit = "degrees";
        return "%INITTemperature: %TEMP %UNIT.\nDewpoint   : %DEWPOINT %UNIT."
                .replace("%INIT", init)
                .replace("%TEMP", temp)
                .replace("%DEWPOINT", dewPoint)
                .replaceAll("%UNIT", unit);
    }

    /**
     * This method takes {@code token} parameter which represents the weather
     * phenomenon abbreviation and translates it accordingly.
     * The method should function as-is even when called in unit tests.
     *
     * @param token is the weather token to be decoded
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to highlight also the corresponding token name
     * @return the wind variation explanation
     */
    static @NotNull String weatherPhenomena(@NotNull String token, String pattern, boolean recentWeather, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

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
                //modifier = "moderate";
                modifier = "";
                break;
        }

        String  phenomenon = metarDict.get(token),
                recent     = recentWeather ? "Recent " : "";
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
        return "%INIT%RECENTWeather: %TOKENB = %MODIFIER %PHENOMENON."
                .replaceFirst("%INIT",init)
                .replace("%TOKENB",tokenBUp)
                .replace("%RECENT", recent)
                .replace("%MODIFIER", modifier)
                .replace("%PHENOMENON",phenomenon);
    }

    /**
     * This method translates the visibility measured at runway level using while
     * recognizing the different formats and modifiers (Europe/US, Canada) for
     * specified airport.
     *
     * @param token the runway visibility string to be translated
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to print also the corresponding token name
     * @return the translation of at-runway level visibility information
     */
    static @NotNull String rvrVisibility(@NotNull String token, String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

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
                         .matches("/" + vartnPttrn + ".*")) {
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
                ? Utilities.conversion(true, value, ftToM, "meters")
                : "";

        return "%INITRunway %RUNWAY, touchdown zone visual range is %MODIFIER%VALUE %UNIT%CONVERSION and %TREND is expected."
               .replace("%INIT", init)
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
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to print also the corresponding token name
     * @return the information about visibility using correct units.
     */
    static @NotNull String visibility(@NotNull String token, String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

        if (token.equals("9999"))
            return "%INIT: The visibility is 10 km or more."
                    .replace("%INIT",init);
        if (token.equals("0000"))
            return "%INIT: The visibility is 50 meters or less."
                    .replace("%INIT", init);

        int smIndex = token.indexOf("SM");
        String visibility;
        if (smIndex == -1) {
            // if the visibility is less than 1000 meters i.e. 600 meters
            // then the visibility format is 0600 -> the leading zero is to be removed
            if (token.startsWith("0")) {
                token = token.substring(1);
            }
            visibility = "%s meters".replace("%s",
                                             token);
        }
        else {
            visibility =
                "%VISIB %UNIT"
                .replace("%VISIB", token.substring(0, smIndex))
                .replace("%UNIT", metarDict.get("SM"));
        }

        return "%INITMaximum horizontal visibility: %VISIBILITY."
                .replace("%INIT", init)
                .replace("%VISIBILITY", visibility);
    }

    /**
     * This method takes {@code token} parameter and the regular expression
     * this parameter matches and explains the numbers in the appropriate way.
     *
     * @param token is the {@code String} to translate. It has to match provided
     *                {@code pattern} expression
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to print also the corresponding token name
     * @return {@code token} with added explanation.
     */
    static @NotNull String windDirSpd(@NotNull String token, String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);                              //normally the pattern is dddssUU(U) or dddssGssUU(U) where d -> direction, s -> speed and U -> unit char

        if (token.equalsIgnoreCase("00000KT"))
            return "%TOKEN: The wind is calm.".replace("%TOKEN", token);

        String spdUnit = token.contains("KT")
                ? metarDict.get("KT")
                : "meters per second";
        boolean conversionNeeded = "knots".equals(spdUnit);
        String  windDirection = token.startsWith("VRB")
                ? "is variable"
                : "blows from " + token.substring(0, 3) + " degrees";         // the substring is the direction 000 - 360
        String  windSpeed   = token.substring(3, 5),                          // the substring is the wind speed
                gusts = token.contains("G")
                        ? " with gusts of %GUSTS %UNIT%CONVERSION"
                          .replace("%GUSTS",token.substring(6, 8))
                          .replace("%UNIT", spdUnit)
                          .replace("%CONVERSION", Utilities.conversion(conversionNeeded,token.substring(6, 8), knotsToKmH, "km/h"))
                        : "";
        String conversion =
                conversionNeeded
                    ? Utilities.conversion(true,windSpeed, knotsToKmH, "km/h")
                    : "";

        return "%INITWind: The wind %DIRECTION at %SPEED %UNIT%CONVERSION%GUSTS."
                .replace("%INIT",  init)
                .replace("%DIRECTION", windDirection)
                .replace("%SPEED", windSpeed)
                .replace("%UNIT", spdUnit)
                .replace("%CONVERSION", conversion)
                .replace("%GUSTS", gusts);
    }

    /**
     * This method takes {@code token} parameter which represents the wind
     * variation and translates it accordingly.
     *
     * @param token is the {@code String} to be translated.
     * @param pattern regular expression to be matched with {@code token}
     * @param tokenPrint user's decision to print also the corresponding token name
     * @return the {@code String} which explains the wind variation
     */
    static @NotNull String windVariation(@NotNull String token, String pattern, boolean tokenPrint) {
        String init = initTokenDecoder(token, pattern, tokenPrint);

        int vPos = token.indexOf("V");
        String  firstWind   = token.substring(0, vPos),
                secondWind  = token.substring(vPos + 1);

        return "%INITVariable wind: The wind direction varies between %FWIND degrees and %SWIND degrees.\n"
                .replace("%INIT", init)
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

    /**
     * @param sectionName the argument to be put between the separator
     * @return the constant separator
     */
    static @NotNull String sectionSeparator(String sectionName) {
        return "----------------------------------------------- %s ------------------------------------------------"
               .replace("%s", sectionName);
    }

    /**
     * Method which initializes a token decoding session. It verifies that the
     * token matches the pattern and then prepares the first part of the resulting
     * token translation.
     * @param token a part of the METAR
     * @param pattern the pattern the token has to match
     * @param tokenPrint the flag which indicates the token highlighting
     * @return the "init" value for each token. It is either an empty string or
     *         a highlighted token.
     */
    static @NotNull String initTokenDecoder(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String result = "";
        assert token.matches(pattern);
        if (metarDict.isEmpty()) setMetarDict();
        if (tokenPrint) {
            return sectionSeparator(token) + "\n";
        }
        return result;
    }
}
