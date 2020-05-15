package cz.cuni.mff.java.flightplanner.util;

import java.text.DecimalFormat;

/**
 * The class which provides different useful methods used in the different parts
 * of the program.
 * These methods may be almost always called as they are, as they only provide
 * certain type of functionality, conversion etc...
 */
public class Utilities {

    /**
     * @param number target number
     * @param lowerBound the lower bound of the interval
     * @param upperBound the upper bound of the interval
     * @return the indication of whether the {@code number} is in the range
     *         between {@code lowerBound} and {@code upperBound}
     */
    static boolean isBetween(int number, int lowerBound, int upperBound) {
        return lowerBound <= number && number <= upperBound;
    }

    /**
     * @param conversionNeeded The flag indicating whether the conversion should
     *                         be actually performed. The conversion can be
     *                         explicitely defined as true, or expressed by any
     *                         boolean condition.
     * @param valueToConvert   The {@code String} value to be converted.This value
     *                         is parsed to the double value in the
     *                         {@link #unitsConverter(String, double)} method.
     * @param constant         The constant used for the conversion of the
     *                         {@code valueToConvert} parameter.
     * @param finalUnit        Optional argument specifying the unit for the result.
     * @return  The {@code String} representing result of the conversion of the
     *          {@code valueToConvert} parameter multiplied by the {@code constant}.
     */
    public static @NotNull String conversion(boolean conversionNeeded, String valueToConvert,
                                             double constant, String finalUnit) {
        if (conversionNeeded)
            return  " (%s %UNIT)"
                    .replace("%s", unitsConverter(valueToConvert, constant))
                    .replace("%UNIT", finalUnit);
        else return "";
    }

    /**
     * Fixed multiplication converter. Converts a value based on the constant.
     * @param arg       The value to be converted.
     * @param constant  The constant multiplied to the {@code arg}
     * @return Returns the {@code arg} parameter value after conversion.
     */
    public static double unitsConverter(double arg, double constant) {
        return arg * constant;
    }

    /**
     * An alternative for {@link #unitsConverter(double, double)} method. Casts
     * the {@code arg} String to double value and converts it using the method
     * mentioned in the link.
     * @param arg       Argument to be parsed and converted.
     * @param constant  The constant multiplied to the {@code arg}
     * @return The {@code arg} value converted to another units using the
     *         {@code constant}.
     * @see #unitsConverter(double, double)
     */
    static @NotNull String unitsConverter(String arg, double constant) {
        try {
            double argNum = unitsConverter(Double.parseDouble(arg), constant);
            return new DecimalFormat("#.##").format(argNum);
        } catch (NumberFormatException ignored) {
            return String.valueOf(Double.NaN);
        }
    }

    /**
     * Parses the {@code String} parameter supposed to be double number.
     *
     * @param strNum The string to be parsed.
     * @return Double number value of {@code strNum} parameter or NaN if something
     *         goes wrong.
     */
    public static double parseDouble(String strNum) {
        double num;
        try { num = Double.parseDouble(strNum); }
        catch (NumberFormatException e) { num = Double.NaN; }

        return num;
    }

    /**
     * The method provides the separator for different sections of the output text.
     *
     * @param sectionName The argument to be put between the separator.
     * @return The separator String.
     */
    public static @NotNull String sectionSeparator(String sectionName) {
        return "----------------------------------------------- %s ------------------------------------------------"
               .replace("%s", sectionName);
    }
}
