package cz.cuni.mff.java.flightplanner;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class treats the dialogs of the user with the program. The
 * DialogCenter class ensures the communication of the program and treats the user
 * inputs. Methods in this class are often invoked from other classes.
 */
public class DialogCenter {

    private static final String projectPackageName = "cz.cuni.mff.java.flightplanner";

    public static void main(String[] args) {

        List<Plugin> allPlugs, activePlugs;

        allPlugs = Plugin.loadAllPlugins(projectPackageName);
        showMainMenu();
        while (true) {
            activePlugs = choosePlugins(allPlugs);
            if (activePlugs.size() > 0) {
                checkActivePlugins(activePlugs);
                Plugin.startPlugins(activePlugs);
                activePlugs.clear();
            } else break;
        }
    }

    /**
     * This method prints initial text information about Flight Planner.
     */
    private static void showMainMenu() {
        System.out.println("Welcome to this Flight Planner.");
        System.out.println("You will be provided with several possibilities to show/construct your entire flight plan or its parts.\n");
    }

    /**
     * @param arg The optional argument to be printed with the prompted message.
     *
     * @param prompt {@code Boolean} flag which indicates whether the user's
     *               interaction is required in the
     *               DialogCenter.setFileOutputStream(boolean, String) method.
     *
     * @param fileName The name of a file used in case the output is to be
     *                 printed to file. If {@code null}, then a temporary file
     *                 is created.
     *
     * @return The value of output stream to be used. For more details, see
     *         DialogCenter.setFileOutputStream(boolean, String) method.
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
     * Sets the output stream chosen by user and eventually creates the file to
     * be written in. The file is by default created in current directory of the
     * project, or in the directory of user's choice if full path is explicitely
     * specified. The default value of output stream makes the output be printed
     * on {@code System.out}.
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

        OutputStream result = System.out; //the output is printed on stdout by default
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

        String completeFileName = "",
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
            } catch (IOException e) {
                System.err.println("An error during the file creation has occurred.");
            }
        }
        assert fileName != null; // if fileName is null then the result is already returned above

        synchronized (projectPackageName) {
            try {
                File output = FilesHandler.createNewFile(outPath, completeFileName);
                System.out.printf("%n%s%n%s",
                                  "The file creation was successful.",
                                  "Th file will be saved in: %FILE"
                                  .replace("%FILE",output.getAbsolutePath()));
                return new FileOutputStream(output);
            } catch (FileNotFoundException e) {
                System.err.println("The file could not be created in specified directory.");
                System.err.println("Therefore, screen output will be used.");
                return result;
            }
        }
    }

    /**
     * Launches an interactive mode which makes user choose from available methods.
     *
     * @param modes The list of all available modules.
     *
     * @return Returns the list of the modules chosen by the user.
     */
    static @NotNull List<Plugin> choosePlugins(@NotNull List<Plugin> modes){
        List<Plugin> activePlugins = new ArrayList<>();
        String[] options;
        String line;
        int longestKeyword = modes  .stream()
                                    .max(Comparator.comparingInt(p -> p.keyword().length()))
                                    .get()
                                    .keyword()
                                    .length();
        boolean repCond; //true, when choosing methods must be repeated, false if correct choice
        do {
            repCond = true;
            for (Plugin mode : modes) {
                System.out.printf("%d) \"%s\"%s : %s%n",
                        mode.pluginID(),
                        mode.keyword(),
                        " ".repeat(longestKeyword - mode.keyword().length()),
                        mode.description()
                );
            }
            System.out.print("Please type in all the numbers or parts of keywords of options you want to choose (separate them with commas): ");
            try {
                line = getInput(false, true);
                if (!line.isBlank()) {
                    options = line.split(",");
                    options = Arrays.stream(options)
                                    .filter(x -> !x.isBlank())
                                    .toArray(String[]::new); //removes empty fields of array
                    for (String opt : options) {
                        try {
                            int x = Integer.parseInt(opt.trim());
                            if (x <= 0 || x > modes.size())
                                throw new WrongInputException("Incorrect option number.\nPlease re-type all your options.");
                            if (!activePlugins.contains(modes.get(x - 1))) {
                                activePlugins.add(modes.get(x - 1));
                            }
                            repCond = false;

                        } catch (NumberFormatException e) {
                            List<Plugin> toBeAdded = new LinkedList<>();
                            for (Plugin mode : modes) {
                                if (mode.keyword().toLowerCase().contains(opt.toLowerCase())) {
                                  toBeAdded.add(mode);
                                }
                            }
                            switch (toBeAdded.size()) {
                                case 0:
                                    System.out.printf("You typed another character with the number.%nPlease re-type all your options.%n%n");
                                    throw new WrongInputException("Be sure you choose only correct options %s and separate them with commas.%n", listAllPlugins(modes));
                                case 1:
                                    activePlugins.addAll(toBeAdded);
                                    repCond = false;
                                    break;
                                default:
                                    System.out.println("Multiple possible options were found. Please, choose only among these now.");
                                    List<Plugin> secondChoice;
                                    do {
                                        secondChoice = choosePlugins(toBeAdded);
                                    } while (secondChoice.size() != 1);
                                    activePlugins.addAll(secondChoice);
                                    repCond = false;
                                    break;
                            }
                        }
                    }
                }
            } catch (WrongInputException e) {
                repCond = true;
            }
        } while (repCond);

        return activePlugins;
    }

    /**
     * Iterates through the list of modules to indicate the correct values to the
     * user.
     *
     * @param plugins The list of available modules
     *
     * @return Returns a string of all active plugins in format "{ 1, 2, ...,
     *         the number of last plugin }"
     */
    static @NotNull String listAllPlugins(@NotNull List<Plugin> plugins) {
        StringBuilder result = new StringBuilder("{ ");
        for (int i = 1; i < plugins.size(); i++) {
            result.append(i)
                  .append(", ");
        }
        result.append(plugins.size())
              .append(" }");

        return result.toString();
    }

    /**
     * This method goes through all of the modules and eliminates any incompatible
     * types, such as performs exit procedure if the user wants to exit the Flight
     * Planner or removes all modules if these are included in a more complex plugin.
     *
     * @param active The raw list of modules which have been activated. However,
     *               these need to be corrected for incompatible types.
     */
    static void checkActivePlugins(@NotNull List<Plugin> active) {
        ArrayList<Plugin> toRemove = new ArrayList<>();
        if (active.stream()
                  .anyMatch(x -> x.getClass().isAssignableFrom(ExitFlightPlannerPlugin.class))) {
            if (new ExitFlightPlannerPlugin().action() == 0)
                active.removeIf(x -> x.getClass().isAssignableFrom(ExitFlightPlannerPlugin.class));
        }

        if ((active.stream()
                   .anyMatch(x -> x.getClass().isAssignableFrom(CreateFlightPlanPlugin.class))) &&
             active.stream()
                   .anyMatch(x -> ( x.getClass().isAssignableFrom(AirportInfoPlugin.class) ||
                                            x.getClass().isAssignableFrom(WeatherInfoPlugin.class)))) {
            System.out.println("The most complex operation you want to perform is flight plan creation.\nTherefore, these operations will be suspended: ");
            active.stream()
                  .filter(x -> (x.pluginID() == 1 || x.pluginID() == 2))
                  .forEach(x -> {
                      toRemove.add(x);
                      System.out.printf("(%d) : %s%n", x.pluginID(), x.description());
                  });
            active.removeAll(toRemove);
            toRemove.clear();
        }

        if (active.size() > 0) {
            System.out.println("The list of operations which will be performed: ");
            for (Plugin mod : active) {
                System.out.printf("(%d) : %s%n", mod.pluginID(), mod.description());
            }
        }
        System.out.printf("%n");
    }

    /**
     * The method gets the input typed-in by the user.
     *
     * @param blankLineAllowed Flag to indicate whether the empty line will be
     *                         accepted as a correct input to be returned.
     * @param printNewLine  Flag which separates the following dialog.
     * @return Non-null {@code String} input given by a user.
     */
    static @NotNull String getInput(boolean blankLineAllowed, boolean printNewLine) {
        synchronized (new Object()) {
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
    }

    /**
     * Method prompts a message (usually a question). The user is expected to
     * provide the answer to the question which then is evaluated based on the
     * {@code trueResponse} parameter. If the beginnings of both start with the
     * same letter, then the answer to the message is evaluated as true.
     *
     * @param message Optional message to print before the question.
     *
     * @param question The message prompt/question for the user to be shown on stdout.
     *
     * @param trueResponse The required response to the {@code message} parameter.
     *
     * @return {@code True}, if the beginnings of both user answer and response
     *         expected in {@code trueResponse} parameter are equal.
     */
    static boolean getResponse(@Nullable String message, @NotNull String question,
                               @NotNull String trueResponse, boolean blankAllowed) {
        String optChoice = "(Y/n)";

        if (message != null) System.out.println(message);
        String autoDecline = "";
        if (blankAllowed)
            autoDecline = "Enter key press will decline automatically.\n";
        System.out.print(autoDecline + question.replace("%OPT", optChoice));
        String trueAnswer = getInput(blankAllowed, true).trim();
        return trueAnswer.startsWith(trueResponse.toUpperCase()) ||
                trueAnswer.startsWith(trueResponse.toLowerCase());
    }
}
