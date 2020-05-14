package cz.cuni.mff.java.flightplanner;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The DialogCenter class provides the communication interface between the program
 * and its user. This class represents includes also basic methods and functions
 * to handle the initial setting of the program.
 */
public class DialogCenter {

    /**
     * The method prints initial welcoming text.
     */
    static void welcomeMenu() {
        System.out.println("Welcome to the Flight Planner.");
        System.out.println("You will be provided with several possibilities of constructing the parts of the flight plan.");
        System.out.printf("%n");
    }

    /**
     * The method which asks the user to precise the way how the output of the
     * currently performed action should be managed.
     *
     * @param arg    The optional argument to be printed in the prompted message.
     *
     * @param prompt {@code Boolean} flag which indicates whether the user's
     *               interaction is required in the
     *               setFileOutputStream(boolean, String) method.
     *
     * @param fileName The name of a file used in case the output is to be
     *                 printed to file. If {@code null}, then a temporary file
     *                 is created.
     *
     * @return The value of output stream to be used. For more details, see
     *         setFileOutputStream(boolean, String) method.
     *
     * @see #setFileOutputStream(boolean, String)
     */
    static OutputStream chooseOutputForm(@NotNull String arg, boolean prompt, @Nullable String fileName) {
        OutputStream result;

        arg = (!arg.isEmpty() && !arg.startsWith(" "))
                ? " " + arg
                : arg;

        System.out.printf("Please type your choice of output form (screen/file)%s: ", arg);

        switch (getInput(false, false)) {
            case "file":
                result = setFileOutputStream(prompt, fileName);
                break;
            case "screen": //no break on purpose because default output stream is System.out
            default:
                result = System.out;
                break;
        }

        System.out.printf("%n");
        return result;
    }

    /**
     * Sets the output stream chosen by the user and eventually creates the file to
     * be written in. The output file is by default created in the project output
     * directory. However, the user is given the possibility to specify either the
     * absolute or the relative path to the output file. The path and file creation
     * are handled by {@link FilesHandler} class.
     *
     * @param prompt {@code Boolean} flag which indicates whether user's
     *               interaction is required.
     *
     * @param fileName The name of a file used in case the output is to be
     *                 printed to file. If {@code null}, then a temporary file
     *                 is created.
     *
     * @return The {@code output stream} type to be used when printing gathered
     *         data.
     */
    static OutputStream setFileOutputStream(boolean prompt, @Nullable String fileName) {

        String curDir = System.getProperty("user.dir"),
               outPath = null;

        if (prompt) {
            System.out.printf("%n%s%n%s%n%s%n%s%n%s",
                    "You can now select the destination directory for the file.",
                    "If you want to do so, please enter the (absolute/relative) path.",
                    "If you want to get the current directory for relative path usage, enter \"pwd\".",
                    "Any other choice will result in using the default \"output\" directory for the file(s).",
                    "Enter your choice: ");
            outPath = getInput(true, true);
            if ("pwd".equalsIgnoreCase(outPath)) {
                System.out.println(FilesHandler.pwd());
                System.out.print("Now please enter the path: ");
                outPath = getInput(true, true);
            }
        }

        String completeFileName,
               dateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        if (fileName != null) {
            completeFileName = "%FILENAME_%DATETIME.%EXTENSION"
                               .replace("%FILENAME", fileName)
                               .replace("%DATETIME", dateTime)
                               .replace("%EXTENSION", "txt");
        } else {
            try {
                File temporary = File.createTempFile("temporary_file_%DATETIME"
                                                           .replace("%DATETIME", dateTime),
                                                     null, null);
                temporary.deleteOnExit();
                return new FileOutputStream(temporary);
            } catch (FileNotFoundException e) {
                System.err.println("The file was not found or does not exist.");
                return System.out;
            } catch (IOException e) {
                System.err.println("An error during the file creation has occurred.");
                return System.out;
            }
        }
        // the fileName will not be null in the following piece of code as in
        // such a case, the result is already returned above
        try {
            File output = FilesHandler.createNewFile(outPath, completeFileName);
            System.out.printf("%n%s%n%s%n",
                    "The file creation was successful.",
                    "The file will be saved in: %FILE_PATH"
                    .replace("%FILE_PATH", output.getAbsolutePath()));
            return new FileOutputStream(output);
        } catch (FileNotFoundException e) {
            System.err.println("The file could not be created in specified directory.");
            System.err.println("Therefore, screen output will be used.");
            return System.out;
        }
    }

    /**
     * Launches an interactive mode which makes user choose from available options.
     * These options are printed and then matched using the unique ID numbers.
     *
     * @param modes The list of all available plugins.
     *
     * @return Returns the list of the plugins chosen by the user.
     */
    static @NotNull List<Plugin> choosePlugins(@NotNull List<Plugin> modes){
        List<Plugin> activePlugins = new ArrayList<>();
        String[] options;
        String line;
        // only to make the indentation look better
        int longestKeyword = modes  .stream()
                                    .max(Comparator.comparingInt(p -> p.keyword().length()))
                                    .get()
                                    .keyword()
                                    .length();
        for (Plugin mode : modes) {
            System.out.printf("%d) \"%s\"%s : %s%n",
                    mode.pluginID(),
                    mode.keyword(),
                    " ".repeat(longestKeyword - mode.keyword().length()),
                    mode.description()
            );
        }
        System.out.print("Please enter all the numbers or parts of keywords of options you want to choose (separate them with commas): ");
        line = getInput(false, true);
        options = Arrays.stream (line.split(",")) // in the stream, the array is corrected for the blank fields
                        .filter (x -> !x.isBlank())
                        .map    (String::strip)
                        .toArray(String[]::new);
        for (String option : options) {
            try {
                int optionID = Integer.parseInt(option.trim());
                if (modes.stream()
                         .anyMatch(pl -> pl.pluginID() == optionID)) {
                    List<Plugin> matchingList =
                            modes.stream ()
                                 .filter (pl -> pl.pluginID() == optionID)
                                 .collect(Collectors.toUnmodifiableList());
                    if (matchingList.size() == 1) {
                        if (!activePlugins.containsAll(matchingList)) {
                             activePlugins.addAll(matchingList);
                             modes.removeAll(matchingList);
                        }
                        continue;
                    }
                }
                throw new NumberFormatException();
            } catch (NumberFormatException e) {
                List<Plugin> toBeAdded = new LinkedList<>(
                    modes.stream()
                            .filter(pl -> pl.keyword()
                                                    .toLowerCase()
                                                    .contains(option.toLowerCase()))
                            .collect(Collectors.toUnmodifiableList())
                );
                switch (toBeAdded.size()) {
                    case 0:
                        System.out.printf ("No option for \"%s\" is available.%nPlease re-enter the incorrect option.%n", option);
                        System.out.println("Be sure you choose only correct options %MODES and separate them with commas."
                                           .replace("%MODES", listAllPlugins(modes)));
                        activePlugins.addAll(choosePlugins(modes));
                    case 1:
                        activePlugins.addAll(toBeAdded);
                        modes.removeAll(toBeAdded);
                        continue;
                    default:
                        System.out.println("Multiple possible options were found. Please, choose only among these now.");
                        activePlugins.addAll(choosePlugins(toBeAdded));
                }
            }
        }
        return activePlugins;
    }

    /**
     * Iterates through the list of plugins to help the user identify them
     * by their ID number.
     *
     * @param plugins The list of available plugins.
     *
     * @return Returns a string of all provided plugin IDs.
     */
    static @NotNull String listAllPlugins(@NotNull List<Plugin> plugins) {
        StringBuilder result = new StringBuilder("{ ");
        plugins.forEach(pl -> result.append(pl.pluginID())
                                          .append(" "));
        result.append("}");
        return result.toString();
    }

    /**
     * The method gets the input typed-in by the user.
     *
     * @param blankLineAllowed  Flag to indicate whether the empty line will be
     *                          accepted as a correct input.
     * @param printNewLine      Flag which separates the dialog that follows.
     *
     * @return Non-null {@code String} input given by a user.
     */
    static @NotNull String getInput(boolean blankLineAllowed, boolean printNewLine) {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            do {
                line = br.readLine();
            } while (line == null || (!blankLineAllowed && line.isBlank()));
        } catch (IOException e) {
            System.out.println("Something went wrong. Please try again.");
            line = getInput(blankLineAllowed, printNewLine);
        }

        if (printNewLine) System.out.printf("%n");
        return line;
    }

    /**
     * Method prompts a question preceded by an optional message. The user is
     * expected to provide the answer to the question which is then evaluated
     * based on the {@code trueAnswer} parameter. In order to evaluate the
     * method as true, both the trueAnswer and the user's actual response have
     * to start with the same letter (comparison is not case-sensitive).
     *
     * @param message Optional message to be printed before the question.
     *
     * @param question The question for the user to be shown on stdout.
     *
     * @param trueAnswer The required beginning of the response in order to
     *                     evalute as true.
     *
     * @return {@code True}, if the beginnings of both user's answer and
     *         {@code trueAnswer} parameter are equal.
     */
    static boolean getResponse(@Nullable String message, @NotNull String question,
                               @NotNull  String trueAnswer, boolean blankAllowed) {
        String  optChoice = "(Y/n)";
        if (blankAllowed)
            System.out.println("\"Enter\" key press will decline automatically.");
        if (message != null)
            System.out.println(message);
        System.out.print(question.replace("%OPT", optChoice));
        String answer = getInput(blankAllowed, true).trim();
        return answer.startsWith(trueAnswer.toUpperCase()) ||
               answer.startsWith(trueAnswer.toLowerCase());
    }
}
