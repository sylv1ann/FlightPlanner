package cz.cuni.mff.java.flightplanner.util;

import cz.cuni.mff.java.flightplanner.DialogCenter;
import cz.cuni.mff.java.flightplanner.dataobject.Airport;

import java.io.*;
import java.util.*;

/**
 * The METARProcessor is the class responsible for parsing, modifying
 * and processing the file(s) with METAR entries. This class processes and decodes
 * all the files into the comprehensible language.
 */
public class METARDecoder {

    private static final Map<String, String> metarDict = new HashMap<>();
    private static String fileOutputPath = null;
    /**
     * The constant pattern which makes part of a correct METAR.
     */
    private static final String windPttrn     = "((VRB)[0-9]{2}|[0-9]{5})(G[0-9]{2})?(KT|MPS)",
                                vartnPttrn    = "[0-9]{3,4}V(P)?[0-9]{3,4}",
                                vsbltyPttrn   = "[0-9]{4}|[0-9 /.]{1,5}SM",
                                rvrPttrn      = "R[0-9]{2}[LCR]?/([PM]?[0-9]+V)?[PM]?[0-9]+(FT)?(/[DNU])?",
                                weatherPttrn  = "(RE|[+-])?[a-zA-Z]{2,}",
                                tempPttrn     = "((M?[0-9]{2})|(//))/((M?[0-9]{2})|(//))",
                                cloudPttrn    = "(SKC|FEW|BKN|SCT|OVC|CLR)[0-9]{3}(CB|TCU|///)?",
                                vrtclVisPttrn = "VV[0-9]{3}",
                                pressurePttrn = "[AQ](([0-9]{4})|(////))",
                                windshrPttrn  = "WS (ALL RWY|(RWY[0-9]{2}[LCR]?))",
                                slpPttrn      = "SLP[0-9]{3}",
                                rsgPttrn      = "[0-9]{2}[0-9/]{6}";
    /**
     * The constants used in aviation for the units conversion.
     */
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
     * @param fileOutput    The flag indicating that the output is directed to a
     *                      file.
     * @return The exit code of the action. Any non-zero code means that an issue
     *         has occurred.
     */
    public int fileDecode(@NotNull File metarToDecode, @NotNull PrintStream printer, boolean fileOutput) {
        int exitCode;
        if ((exitCode = checkAndSetMetarDict()) != 0) return exitCode;

        List<String> allMETARs = readInReverseOrder(metarToDecode);
        if (allMETARs == null) return 1;
        int metarsAvailable = allMETARs.size(),
            metarsDecoded = 0;
        if (metarsAvailable == 0) {
            printer.println("No METAR was accessible for the specified period and airport.");
            return 0;
        }
        System.out.println(Utilities.sectionSeparator("METAR DECODING"));
        boolean tokenPrint =
                    DialogCenter.getResponse(
                            "The tokens may help you understand what token is being decoded.",
                            "Should the token names be printed too? %OPT: ",
                            "Y",
                            true),
                autoDecode =
                    DialogCenter.getResponse(
                            null,
                            "Do you want to decode all METARs automatically without any further asking? %OPT: ",
                            "Y",
                            true),
                noDecode = false;
        if (!autoDecode) {
            System.out.println("All available METAR = \"%DEFINITION\" are progressively decoded until you decide not to decode them anymore."
                    .replace("%DEFINITION", metarDict.get("METAR")));
        }

        for (String metarEntry : allMETARs) {
            if (noDecode) break;
            String[] tidyMETAR = csvMETARtidy(metarEntry.replace("=", "")
                                                                 .strip());
            if (tidyMETAR == null) return 1;
            String  initInfo    = tidyMETAR[0],
                    finalMetar  = tidyMETAR[1],
                    decision    = finalMetar.length() > 80
                            ? "METAR too long to display"
                            : finalMetar;
            if (finalMetar.endsWith("NIL")) continue;
            if (fileOutput) fileOutputPath = metarToDecode.getAbsolutePath();
            if (autoDecode && metarsDecoded != metarsAvailable - 1) fileOutputPath = null;
            if (autoDecode || DialogCenter.getResponse(
                        null,
                        "Do you want \"%DECISION\" to be decoded? %OPT: "
                                .replace("%DECISION", decision),
                        "Y",
                        false)
            ) {
                metarEntryDecode(finalMetar, printer,
                                 initInfo, tokenPrint);
                printer.printf("%n");
                metarsDecoded++;
            } else noDecode = true;
        }
        System.out.printf("METARs available: %d%nMETARs decoded  : %d%n",
                          metarsAvailable,          metarsDecoded);
        System.out.println(Utilities.sectionSeparator("END OF METAR DECODING"));
        return 0;
    }

    /**
     * Reads the file and adds each line to the end of the list. After each line
     * of the file is added to the list, it is reversed using
     * {@link Collections#reverse(List)} method. The file to be read in reverse
     * order is small enough. Therefore, performance should not be affected.
     * Reverse reading is done due to the fixed structure of the downloaded file.
     *
     * @param fileToRead The file to be read.
     * @return The list containing all lines from the file in the reverse order.
     */
    private static @Nullable
    List<String> readInReverseOrder(@NotNull File fileToRead) {
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileToRead))) {
            br.lines()
              .filter(line -> !line.isBlank())
              .forEach(result::add);
            Collections.reverse(result);
            return result;
        } catch (IOException e) {
            System.err.println("File reading failed. The METAR will not be decoded.");
            return null;
        }
    }

    /**
     * Processes the input METAR entry in .csv format and separates the metadata
     * from the actual METAR.
     * @param metarEntry a METAR unit and its metadata in .csv format
     * @return The String array of length 2, which contains the metadata on its
     *         first position and the METAR itself on the second position, or
     *         {@code null} if a database issue is detected.
     */
    private @Nullable String[] csvMETARtidy(@NotNull String metarEntry) {
        String auto = "", airport = "";
        boolean autoMetar = metarEntry.contains("AUTO");
        if (autoMetar) {
            metarEntry = metarEntry.replaceFirst("AUTO", "");
            auto = "automatically, with no human intervention or oversight ";
        }
        String[] fields = metarEntry.split(",");
        List<Airport> metarConcernedApts = Airport.searchAirports(null,
                                                                  List.of(fields[0]),
                                                                  false,
                                                                  false);
        if (metarConcernedApts == null) return null;
        for (Airport apt : metarConcernedApts) {
            airport = "Location: %ICAO\n\t%LOC\n\tLatitude: %LAT\n\tLongtitude: %LONG\n"
                      .replace("%ICAO", apt.getIcaoCode())
                      .replace("%LOC", apt.getName() + ", " + apt.getMunicipality() + ", " + apt.getCountryCode())
                      .replace("%LAT", String.valueOf(apt.getGeoLat()))
                      .replace("%LONG", String.valueOf(apt.getGeoLong()));
        }
        String initInfo = "%AIRPORTThe %TYPE was issued %AUTOthe %DAY-%MONTH-%YEAR at %HOUR:%MIN UTC time."
                          .replace("%AIRPORT", airport)
                          .replace("%AUTO",auto)
                          .replace("%DAY", fields[3])
                          .replace("%MONTH", fields[2])
                          .replace("%YEAR", fields[1])
                          .replace("%HOUR", fields[4])
                          .replace("%MIN", fields[5]),
               metar = fields[6];

        return new String[] {initInfo, metar};
    }

    /**
     * Decode a METAR unit specified in the {@code metarEntry}. The fixed-structured
     * METAR is divided into tokens. Each token is treated based on the category it
     * belongs to. METAR entry is being decoded using the
     * <a href="http://meteocentre.com/doc/metar.html">US/CAN METAR explanation</a>
     * and
     * <a href="https://www.skybrary.aero/index.php/Meteorological_Terminal_Air_Report_(METAR)">general METAR explanation</a>
     * websites.
     *
     * @param metarEntry The actual METAR unit which will be decoded.
     * @param printer    The printer used for printing.
     * @param initInfo   The String containing initial information to be printed
     *                   before each decoding.
     * @param tokenPrint The flag which indicates whether each token should be
     *                   highlighted before its translation.
     */
    private void metarEntryDecode(@NotNull String metarEntry, @NotNull PrintStream printer,
                                  String initInfo, boolean tokenPrint) {

        String[] tokens = metarEntry.split("\\s+");
        printer.println(Utilities.sectionSeparator(metarEntry));
        printer.println(initInfo.replace("%TYPE", tokens[0]));
        // 0 -> METAR/SPECI type, 1 -> airport ICAO, 2 -> day and time in zulu of metar publication, 3+ -> tokens to be decoded
        // for loop iterates through the tokens and for each determines what information the token represents
        for (int i = 3; i < tokens.length; i++) {
            if (tokens[i].matches(windPttrn)) {    //wind direction and speed information
                printer.println(windDirSpd(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(vartnPttrn)) {
                printer.println(windVariation(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(vsbltyPttrn)) {
                printer.println(visibility(tokens[i], tokenPrint));
                continue;
            }
            //the following if condition treats the "1 1/4SM" case
            if (tokens[i].matches("[0-9]+") &&
                (i + 1 < tokens.length)           &&
                tokens[i + 1].matches(vsbltyPttrn)
                ) {
                printer.println(visibility(tokens[i] + " " + tokens[i + 1], tokenPrint));
                i++;
                continue;
            }
            if (tokens[i].matches(rvrPttrn)) {
                printer.println(rvrVisibility(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(vrtclVisPttrn)) {
                printer.println(verticalVisibility(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(weatherPttrn)) {
                printer.println(weatherPhenomena(tokens[i], tokens[i].startsWith("RE"), tokenPrint));
                continue;
            }
            if (tokens[i].matches(cloudPttrn)) {
                printer.println(cloudLayer(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(tempPttrn)) {
                printer.println(temperature(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(pressurePttrn)) {
                printer.println(pressure(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(windshrPttrn)) {
                printer.println(windshearWarning(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(slpPttrn)) {
                printer.println(seaLvlPressure(tokens[i], tokenPrint));
                continue;
            }
            if (tokens[i].matches(rsgPttrn)) {
                printer.println(rwyStateGroup(tokens[i], rsgPttrn, tokenPrint));
            }
            if (tokens[i].matches("[a-zA-Z]+")) {
                String tokenMeaning = metarDict.get(tokens[i]);
                if (tokenMeaning != null) {
                    printer.printf("%s: %s.%n", tokens[i], tokenMeaning);
                }
            } else {
                printer.println(tokens[i] + ": Unknown token.");
            }
        }
        printer.println(Utilities.sectionSeparator("END OF METAR"));

        if (fileOutputPath != null) {
            System.out.printf("METAR decoding was successfully written to the %s file.%n%n",
                              fileOutputPath);
            fileOutputPath = null; // setting the value to null in order to avoid repeated messages for the same file
        }
    }

    /**
     * Decodes the "runway state group" part of a METAR. This part provides the
     * information about any runway contamination (e.g. snow, water, oil) which
     * may impact the braking action and aircraft safety during landing. The
     * METAR explanation sources are precised in
     * {@link #metarEntryDecode(String, PrintStream, String, boolean)} method.
     *
     * @param token      The raw RSG description.
     * @param rsgPttrn   The regex to ensure that the token is classified correctly.
     * @param tokenPrint The flag indicating whether to highlight the separation
     *                   between different sections.
     * @return The decoded result {@code String} based on the information in the
     *         raw RSG token.
     */
    private static @NotNull String rwyStateGroup(@NotNull String token, @NotNull String rsgPttrn, boolean tokenPrint) {
        // example of a RSG descriptor: 8849//91
        String init = initTokenDecoder(token,rsgPttrn,tokenPrint);
        // the token contains only numbers, "/" or substring "CLRD" on indexes 2 - 5

        boolean cleared  = token.contains("CLRD");
        String  rwyModif = getRSGRwyModificator(token.substring(0,2)); // rwy modificator is always present, however it's value has to be parsed
        String  brkAction = getRSGBrakingAction(token.substring(6));

        if (cleared) {
            return "%INITRunway state descriptor: Runway %RWY cleared, %BRKACT"
                    .replace("%INIT", init)
                    .replace("%RWY", rwyModif)
                    .replace("%BRKACT", brkAction);
        }

        char    contaminationType    = token.charAt(2),    // runway deposit position
                contaminationExtent  = token.charAt(3);    // runway contamination extent position

        String  contaminationDepth = getRSGContaminationDepth(token.substring(4, 6));

        return "%INITRunway state descriptor:\n\tRunway(s) concerned: %RWY\n\t %vCON: %CONTAM\n\t %vEXT: %EXTENT\n\t%vDEP: %DEPTH\n\t%vBR: %BRACT"
                .replace("%INIT", init)
                .replace("%RWY", rwyModif)
                .replace("%vCON", String.valueOf(contaminationType))
                .replace("%CONTAM", getRSGRwyContamination(contaminationType))
                .replace("%vEXT", String.valueOf(contaminationExtent))
                .replace("%EXTENT", getRSGContaminationExtent(contaminationExtent))
                .replace("%vDEP", token.substring(4,6))
                .replace("%DEPTH", contaminationDepth)
                .replace("%vBR", token.substring(6))
                .replace("%BRACT", brkAction);
    }

    /**
     * The method used for {@link #rwyStateGroup(String, String, boolean)}.
     * This method decodes a part of the RSG descriptor based on the descriptor
     * explanation at the METAR explanation websites mentioned in
     * {@link #metarEntryDecode(String, PrintStream, String, boolean)} method
     * documentation.
     * @param arg The character whose meaning is searched for.
     * @return    The meaning of the {@code arg} character.
     */
    private static @NotNull String getRSGRwyContamination(char arg) {
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
     * The method used for {@link #rwyStateGroup(String, String, boolean)}.
     * This method decodes a part of the RSG descriptor based on the descriptor
     * explanation at the METAR explanation websites mentioned in
     * {@link #metarEntryDecode(String, PrintStream, String, boolean)} method
     * documentation.
     * @param arg The character whose meaning is searched for.
     * @return    The meaning of the {@code arg} character.
     */
    private static @NotNull String getRSGContaminationExtent(char arg) {
        Map<Character, String> rwyContamiDict = new HashMap<>();
        rwyContamiDict.put('1', "Less than 10%");
        rwyContamiDict.put('2', "11% to 25%");
        rwyContamiDict.put('5', "26% to 50%");
        rwyContamiDict.put('9', "51% to 100%");
        rwyContamiDict.put('/', "Not reported");

        return rwyContamiDict.get(arg);
    }

    /**
     * The method used for {@link #rwyStateGroup(String, String, boolean)}.
     * This method decodes a part of the RSG descriptor based on the descriptor
     * explanation at the METAR explanation websites mentioned in
     * {@link #metarEntryDecode(String, PrintStream, String, boolean)} method
     * documentation.
     * @param arg The String whose meaning is searched for.
     * @return    The meaning of the {@code arg} String.
     */
    private static @NotNull String getRSGContaminationDepth(String arg) {
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
     * The method used for {@link #rwyStateGroup(String, String, boolean)}.
     * This method decodes a part of the RSG descriptor based on the descriptor
     * explanation at the METAR explanation websites mentioned in
     * {@link #metarEntryDecode(String, PrintStream, String, boolean)} method
     * documentation.
     * @param rwy The String runway representation whose meaning is searched for.
     * @return    The meaning of the {@code arg} parameter.
     */
    private static @NotNull String getRSGRwyModificator(String rwy) {
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
     * The method used for {@link #rwyStateGroup(String, String, boolean)}.
     * This method decodes a part of the RSG descriptor based on the descriptor
     * explanation at the METAR explanation websites mentioned in
     * {@link #metarEntryDecode(String, PrintStream, String, boolean)} method
     * documentation.
     * @param arg The String whose meaning is searched for.
     * @return    The meaning of the {@code arg} parameter.
     */
    private static @NotNull String getRSGBrakingAction(String arg) {
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
     *
     * @param token      The SLP to be translated.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           The information about sea-level pressure.
     */
    private static @NotNull String seaLvlPressure(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.slpPttrn, tokenPrint);

        String  seaLevelPressure = "",
                conversion       = "";
        try {
            double slPressure = Double.parseDouble(token.substring(3)) / 10;
            if (slPressure >= 50.0)
                seaLevelPressure = "9%s".replace("%s", String.valueOf(slPressure));
            else {
                if (slPressure < 10) {
                    seaLevelPressure = "100%s".replace("%s",String.valueOf(slPressure));
                } else seaLevelPressure = "10%s".replace("%s", String.valueOf(slPressure));
            }
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
    * This method explains the warning of possible wind-shear existence in the
    * vicinity of the airport.
    *
    * @param token      Wind-shear token to be translated.
    * @param tokenPrint The flag which indicates whether the token should be
     *                  highlighted before its translation.
    * @return           Wind shear warning explanation.
    */
    private static @NotNull String windshearWarning(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.windshrPttrn, tokenPrint);

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
     *
     * @param token      Encoded pressure information to be translated
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           The information about pressure using correct units.
     */
    private static @NotNull String pressure(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.pressurePttrn, tokenPrint);

        String  value = token.substring(1),
                unit  = token.startsWith("Q") ? "hPa" : "inches";
        if ("////".equals(value)) {
            return "%INITSea-level pressure (QNH): Pressure not available."
                    .replace("%INIT",init);
        }

        String  conversion = "hPa".equals(unit)
                                ? Utilities.conversion(true,value,hPaToInch,"inches")
                                : Utilities.conversion(true,value,inchTohPa,"hPa");

        if ("inches".equals(unit)) {
            value = "%fP.%sP"
                   .replace("%fP", value.substring(0,2))
                   .replace("%sP", value.substring(2));
        }

        return "%INITSea-level pressure (QNH): %VALUE %UNIT%CONVERSION."
                .replace("%INIT", init)
                .replace("%VALUE", value)
                .replace("%UNIT", unit)
                .replace("%CONVERSION", conversion);
    }

    /**
     * This method takes {@code token} parameter which represents the vertical
     * visibility and translates it accordingly.
     *
     * @param token      The vertical visibility representation to be translated.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           The information about vertical visibility using correct
     *                   units.
     */
    private static @NotNull String verticalVisibility(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.vrtclVisPttrn, tokenPrint);

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
     *
     * @param token      The clound representation to be translated.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           The cloud information explanation.
     */
    private static @NotNull String cloudLayer(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.cloudPttrn, tokenPrint);

        assert token.length() >= 6;
        String layerType = metarDict.get(token.substring(0, 3));
        if (layerType == null) layerType = "unknown layer type";
        String  height      = token.substring(3,6),
                layerHeight = Utilities.unitsConverter(height,100), //i.e. BKN030 means broken at 3000 feet
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
     *
     * @param token      The temperature to be translated.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           Decoded temperature.
     */
    private static @NotNull String temperature(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.tempPttrn, tokenPrint);

        if (token.equals("/////"))
            return "%INITTemperature: Temperature is not available."
                   .replace("%INIT",init);

        String[] temps   = token.split("/",-1);
        String   temp    = temps[0].startsWith("M")
                                ? "%TEMP %UNIT."
                                  .replace("%TEMP",temps[0].replace("M", "-"))
                                : temps[0].isBlank()
                                    ? "Temperature not available."
                                    : "%TEMP %UNIT.".replace("%TEMP", temps[0]),
                dewPoint = temps[1].startsWith("M")
                                ? "%TEMP %UNIT."
                                  .replace("%TEMP",temps[1].replace("M","-"))
                                : temps[1].isBlank()
                                        ? "Dewpoint not available."
                                        : "%DEW %UNIT.".replace("%DEW", temps[1]);
        String  unit = "degrees";
        return "%INITTemperature: %TEMP\nDewpoint   : %DEWPOINT"
                .replace("%INIT", init)
                .replace("%TEMP", temp)
                .replace("%DEWPOINT", dewPoint)
                .replaceAll("%UNIT", unit);
    }

    /**
     * This method takes {@code token} parameter which represents the weather
     * phenomenon abbreviation and translates it accordingly.
     *
     * @param token      The weather phenomenon token to be decoded.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @param recentWeather Indicates whether the token represents the "Recent
     *                      weather" part of a METAR.
     * @return           The wind variation explanation.
     */
    private static @NotNull String weatherPhenomena(@NotNull String token, boolean recentWeather, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.weatherPttrn, tokenPrint);

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
                recent     = recentWeather ? "Recent " : "",
                weatherKey = "Weather: ";
        StringBuilder sb = new StringBuilder();
        boolean isUnknown = false;

        if (phenomenon == null) {
            for (int i = 0; i < token.length() / 2; i++) {
                String  phenomenKey   = token.substring(2*i, 2*(i+1)),
                        phenomenValue = metarDict.get(phenomenKey);
                if (phenomenValue != null) {
                    sb.append(phenomenValue).append(" ");
                } else isUnknown = true;
            }
            if (isUnknown) {
                sb.append("Unknown token");
                weatherKey = "";
            }
            phenomenon = sb.toString().strip();
        }
        return "%INIT%RECENT%WEATHER%TOKENB = %MODIFIER %PHENOMENON."
                .replace("%WEATHER",weatherKey)
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
     * @param token      The runway visibility string to be translated
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           The translation of at-runway level visibility information.
     */
    private static @NotNull String rvrVisibility(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.rvrPttrn, tokenPrint);

        int    slash      = token.indexOf('/');
        String rwyID      = token.substring(1, slash),
               units      = token.contains("FT") ? "feet" : "meters";
        String modifier   = "";

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
                    String modifier1 = "";
                    if (token.charAt(_V + 1) == 'P') {
                        modifier1 = "more than ";
                        token = token.replace("VP","V");
                    }
                    if (token.charAt(_V + 1) == 'M') {
                        modifier1 = "less than ";
                        token = token.replace("VM","V");
                    }
                    String fVis = token.substring(slash + 1, _V),    // the variable visibility value is expected
                           sVis = token.substring(_V + 1, _V + 5);   // to have format nnnnVnnnn where n = [0-9]

                    String varConversion = "feet".equals(units)
                            ? "%FCONV and%SCONV"
                              .replace("%FCONV",Utilities.conversion(true,
                                                                                     fVis,
                                                                                     ftToM,
                                                                                     "meters"))
                              .replace("%SCONV",Utilities.conversion(true,
                                                                                      sVis,
                                                                                      ftToM,
                                                                                      "meters"))
                            : "";

                    modifier = "variable between %FVIS %UNIT and %MOD1%SVIS %UNIT%VARCONV"
                                .replace("%MOD1",modifier1)
                                .replace("%FVIS", fVis)
                                .replace("%SVIS", sVis)
                                .replace("%VARCONV", varConversion);
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
                ? Utilities.conversion(!value.isBlank(), value, ftToM, "meters")
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
     * @param token      The visibility string to be translated.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           The information about visibility using correct units.
     */
    private static @NotNull String visibility(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.vsbltyPttrn, tokenPrint);

        if (token.equals("9999"))
            return "%INITVisibility: The visibility is 10 km or more."
                    .replace("%INIT",init);
        if (token.equals("0000"))
            return "%INITVisibility: The visibility is 50 meters or less."
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
     * @param token   The {@code String} to translate. It has to match provided
     *                {@code pattern} expression.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           {@code token} with added explanation.
     */
    private static @NotNull String windDirSpd(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.windPttrn, tokenPrint);  //normally the pattern is dddssUU(U) or dddssGssUU(U) where d -> direction, s -> speed and U -> unit char

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
     * @param token      The {@code String} to be translated.
     * @param tokenPrint The flag which indicates whether the token should be
     *                   highlighted before its translation.
     * @return           {@code String} which explains the wind variation.
     */
    private static @NotNull String windVariation(@NotNull String token, boolean tokenPrint) {
        String init = initTokenDecoder(token, METARDecoder.vartnPttrn, tokenPrint);

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
     * Checks whether the dictionary with METAR terminology already exists.
     * If it does not, then reads the .txt file containing the dictionary with
     * specified terminology and creates {@code Map<String,String> metarDict} map.
     *
     * @see #metarDict
     *
     * @return The exit code of the action. Any non-zero code means that an issue
     *         has occurred.
     */
    private static int checkAndSetMetarDict() {
        if (metarDict.isEmpty()) {
            File metarDictLocation = FilesHandler.findResource("metarDictionary.txt");
            if (metarDictLocation == null) {
                System.err.println("The resource was not found.\n");
                return 1;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(metarDictLocation))) {
                String dictEntry;
                while ((dictEntry = br.readLine()) != null) {
                    String[] tokens = dictEntry.split("=");
                    metarDict.put(tokens[0], tokens[1]);
                }
            } catch (IOException ex) {
                System.err.println("Something went wrong while reading the METAR dictionary file.");
                return 1;
            }
        }
        return 0;
    }

    /**
     * Method which initializes a token decoding session. It verifies that the
     * token matches the pattern and then prepares the first part of the resulting
     * token translation.
     * @param token      A token: part of the METAR.
     * @param pattern    The pattern the token has to match.
     * @param tokenPrint The flag which indicates the token highlighting.
     * @return           The "init" value for each token. It is either an empty
     *                   string or a highlighted token.
     */
    private static @NotNull String initTokenDecoder(@NotNull String token, @NotNull String pattern, boolean tokenPrint) {
        String result = "";
        assert token.matches(pattern);
        checkAndSetMetarDict();
        if (tokenPrint) {
            return Utilities.sectionSeparator(token) + "\n";
        }
        return result;
    }
}
