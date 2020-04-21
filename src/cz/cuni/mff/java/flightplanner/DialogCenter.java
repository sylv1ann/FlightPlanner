package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;

/**
 * This class treats the dialogs of the user with the program. The
 * DialogCenter class ensures the communication of the program and treats the user
 * inputs. Methods in this class are often invoked from other classes.
 */
public class DialogCenter {

    private static final String projectPackageName = "cz.cuni.mff.java.flightplanner";

    /**
     * The default value of filePath which indicates the current directory.
     */
    private static String filePath = ".";

    public static void main(String[] args) {

        List<Plugin> allPlugs, activePlugs;

        allPlugs = Plugin.loadAllPlugins(projectPackageName);
        Airport.setAirportsDatabase();
        showMainMenu();
        activePlugs = choosePlugins(allPlugs);
        if (activePlugs.size() > 0) {
            checkActivePlugins(activePlugs);
            Plugin.startPlugins(activePlugs);
            choosePlugins(allPlugs);
        }
    }

    /**
     * This method prints initial text information about Flight Planner.
     */
    private static void showMainMenu() {
        System.out.println("Welcome to this amazing Flight Planner.");
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
    public static OutputStream chooseOutputForm(@NotNull String arg, boolean prompt, @Nullable String fileName) {
        OutputStream result;

        System.out.printf("%nPlease type your choice of output form (screen / file / default)%s: ", arg);

        switch (getInput(false)) {
            default: //no break on purpose;
            case "screen":
                result = System.out;
                break;
            case "file":
                result = setFileOutputStream(prompt, fileName);
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
    public static OutputStream setFileOutputStream(boolean prompt, @Nullable String fileName) {

        OutputStream result = System.out; //the output is printed on stdout by default
        String dirSetting = ".".equals(filePath)
                ? "current"
                : "previously used";

        if (prompt && getResponse("If you want to choose the location for the file, please type \"Y\".",
                                  "If not, type anything else and  " + dirSetting + " directory will be used.",
                                  "Y", false)) {
            System.out.print("Please enter the absolute path of the destination directory: ");
            filePath = getInput(false);
        }

        String completeFileName = "";
        if (fileName != null)
            completeFileName = fileName + "_" + new SimpleDateFormat("yyyy-MM-ddHHmmss").format(new Date()) + ".txt";

        try {
            if (".".equals(filePath)) {//default setting
                if (fileName == null) result = new FileOutputStream(File.createTempFile("tmp", null, null));
                else result = new FileOutputStream(Downloader.fileFromPathCreator(completeFileName)); //
            }
            else result = new FileOutputStream(new File(filePath + File.separator + completeFileName));
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong. Please try again.");
            result = setFileOutputStream(true, fileName); //if this method needs to be repeated, user response must be given
        } catch (IOException ignored) { }

        return result;
    }

    /**
     * Launches an interactive mode which makes user choose from available methods.
     *
     * @param modes The list of all available modules.
     *
     * @return Returns the list of the modules chosen by the user.
     */
    public static @NotNull ArrayList<Plugin> choosePlugins(@NotNull List<Plugin> modes) {
        ArrayList<Plugin> activePlugins = new ArrayList<>();
        String line;
        String[] options;
        boolean repCond; //true, when choosing methods must be repeated, false if correct choice
        do {
            repCond = true;
            for (int i = 0; i < modes.size(); i++) {
                System.out.println("(" + (i + 1) + ") : " + modes.get(i).description());
            }
            System.out.print("Please type in all the numbers of options you want to choose (separate them with commas): ");
            try {
                line = getInput(false);
                System.out.print("\n"); //separates the dialog
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
                            Stream<Plugin> mods;            //this block of code ensures that the program won't fail in case the user writes the part of a plugin's keyword.
                            mods = modes.stream()
                                          .filter(x -> x.keyword().toLowerCase().contains(opt.toLowerCase()));
                            if (mods.count() == 1) {
                                mods.forEach(activePlugins::add); //however, the plugin has to be unambiguous
                            } else {
                                System.out.println("An error has occurred. You typed another character with the number.\nPlease re-type all your options.");
                                throw new WrongInputException();
                            }
                        }
                    }
                }
            } catch (WrongInputException e) {
                System.out.printf("Be sure you choose only correct options %s and separate them with commas.%n", listAllPlugins(modes));
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
     * @return Returns a string of all active plugins in format "{ 1, 2, ... }
     *         "the number of last plugin" }"
     */
    private static @NotNull String listAllPlugins(@NotNull List<Plugin> plugins) {
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
    private static void checkActivePlugins(@NotNull List<Plugin> active) {
        ArrayList<Plugin> toRemove = new ArrayList<>();
        if (active.stream()
                  .anyMatch(x -> x.getClass().getName().contains("ExitFlightPlannerModule"))) {

            try {
                if (getResponse(null, "Are you sure to exit the planner? (Y/n): ", "Y", true)) {
                    System.out.println("Goodbye. See you next time in Flight Planner.");
                    Thread.sleep(500);
                    System.exit(0);
                } else {
                    active.removeIf(x -> x.getClass().getName().contains("ExitFlightPlanner"));
                    System.out.print("\n");
                }
            } catch (InterruptedException ignored) {
            }
        }

        if ((active.stream()
                   .anyMatch(x -> x.pluginID() == 3)
            ) &&
             active.stream()
                   .anyMatch(x -> (x.pluginID() == 1 || x.pluginID() == 2))) {
            System.out.println("The most complex operation you want to perform is flight plan creation.\nTherefore, these operations will be suspended: ");
            active.stream()
                  .filter(x -> (x.pluginID() == 1 || x.pluginID() == 2))
                  .forEach(x -> {
                      toRemove.add(x);
                      System.out.println("(" + x.pluginID() + ") : " + x.description());
                  });
            active.removeAll(toRemove);
            toRemove.clear();
        }

        System.out.println("The list of operations which will be performed: ");
        for (Plugin mod : active) {
            System.out.println("(" + mod.pluginID() + ") : " + mod.description());
        }
        System.out.print("\n");
    }

    /**
     * The method gets the input typed-in by the user.
     *
     * @param blankLineAllowed Flag to indicate whether the empty line will be
     *                         accepted as a correct input to be returned.
     *
     * @return Non-null and non-empty {@code String} input given by a user.
     */
    public static @NotNull String getInput(boolean blankLineAllowed) {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            do {
                line = br.readLine();
            } while (line == null || (!blankLineAllowed && line.isBlank()));
        } catch (IOException e) {
            System.out.println("Something went wrong. Please try again.");
            line = getInput(blankLineAllowed);
        }

        return line + "\n";
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
    public static boolean getResponse(@Nullable String message, @NotNull String question, @NotNull String trueResponse, boolean blankAllowed) {
        if (message != null) System.out.println(message);
        String autoDecline = "";
        if (blankAllowed )
            autoDecline = "Enter key press will decline automatically.\n";
        System.out.print(autoDecline + question);
        return getInput(blankAllowed).trim().startsWith(trueResponse);
    }
}
