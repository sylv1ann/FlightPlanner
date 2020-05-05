package cz.cuni.mff.java.flightplanner;

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

    static @NotNull String conversion(boolean conversionNeeded, String valueToConvert, double constant, String finalUnit) {
        if (conversionNeeded)
            return  " (%s %UNIT)"
                    .replace("%s", constantConverter(valueToConvert, constant))
                    .replace("%UNIT", finalUnit);
        else return "";
    }

    /**
     * Fixed multiplication converter. Converts a value based on the constant.
     * @param arg the value to be converted.
     * @return Returns the {@code arg} parameter value after conversion.
     */
    static double constantConverter(double arg, double constant) {
        return arg * constant;
    }

    /**
     * Wrapper around constantConverter(double, double) method. Casts the {@code arg}
     * string to double and converts it using {@code constantConverter(double, double)}.
     * @param arg Argument to be parsed and converted.
     * @param constant The constant multiplied to the {@code arg}
     * @return converted value from {@code arg}
     * @see #constantConverter(double, double)
     */
    static @NotNull String constantConverter(String arg, double constant) {
        try {
            double argNum = constantConverter(Double.parseDouble(arg), constant);
            return new DecimalFormat("#.##").format(argNum);
        } catch (NumberFormatException ignored) {
            return String.valueOf(Double.NaN);
        }
    }

    /**
     * Parses the {@code String} parameter supposed to be double number. Used
     * primarily in order to avoid code repetition. The value is expected to be
     * positive.
     * @param strNum The string to be parsed.
     * @return Double number value of {@code strNum} parameter or NaN if something
     *         goes wrong.
     */
    static double parseNum(String strNum) {
        double num;
        try { num = Double.parseDouble(strNum); }
        catch (NumberFormatException e) { num = Double.NaN; }

        return num;
    }

    /**
     * @param sectionName the argument to be put between the separator
     * @return the constant separator
     */
    static @NotNull String sectionSeparator(String sectionName) {
        return "----------------------------------------------- %s ------------------------------------------------"
               .replace("%s", sectionName);
    }
}
