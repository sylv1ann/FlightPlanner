package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.*;

/**
 * This class should treat all the dialogs of the user with the program. The
 * DialogCenter class ensures the communication of the program and treats the user
 * inputs. Methods in this class may be invoked from other classes.
 */
public class DialogCenter {

    /**
     * The default value of filePath which indicates the current directory.
     */
    private static String filePath = ".";

    public static void main(String[] args) {

        //Airport.setAirportsDatabase();
        //Airport.showAirportsList(null, "", false);
        //Airport.searchAirports(null, false);

        ArrayList<Module> modules, activeModules;

        modules = Module.setAllModules(DialogCenter.class.getPackageName());
        Airport.setAirportsDatabase();
        showMainMenu();
        activeModules = chooseModules(modules);
        if (activeModules.size() > 0) {
            checkActiveModules(activeModules);
            Module.startModules(activeModules);
        }
    }

    /**
     * @param arg The argument to be added to the prompted message.
     *
     * @param prompt {@code Boolean} flag which indicates whether the user
     *               interaction is required passed to
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
    public static OutputStream chooseOutputForm(String arg, boolean prompt, String fileName) {
        OutputStream result;

        System.out.print("\nPlease type your choice of output form (screen / file / default)" + arg + ": ");

        switch (getInput()) {
            default:
            case "default":
            case "screen":
                result = System.out;
                break;
            case "file":
                result = setFileOutputStream(prompt, fileName);
                break;
        }

        System.out.print("\n");
        return result;
    }

    /**
     * Sets the output stream chosen by user and eventually creates the file to
     * be written in. The file is by default created in current directory of the
     * project, or in the directory of user's choice if full path is explicitely
     * specified. The default value of output stream makes the output be printed
     * on {@code System.out}.
     *
     * @param prompt {@code Boolean} flag which indicates whether the user
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
        String dirSetting = ".".equals(filePath) ? "current" : "previously used";


        if (prompt && getResponse("If you want to choose the location for the file, please type \"Y\".",
                                  "If not, type anything else and  " + dirSetting + " directory will be used.",
                                  "Y")) {
            System.out.print("Please enter the absolute path of the destination directory: ");
            filePath = getInput();
        }

        String completeFileName = "";
        if (fileName != null)
            completeFileName = fileName + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt";

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
     * This method prints initial text information about Flight Planner.
     */
    private static void showMainMenu() {
        System.out.println("Welcome to this amazing Flight Planner.");
        System.out.println("You will be provided with several possibilities to show/construct your entire flight plan or its parts.\n");
    }

    /**
     * The method gets the input typed-in by the user.
     *
     * @return Non-null and non-empty {@code String} input given by a user.
     */
    public static @NotNull String getInput() {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            do {
                line = br.readLine();
            } while (line == null || line.isBlank());
        } catch (IOException e) {
            System.out.println("Something went wrong. Please try again.");
            line = getInput();
        }

        return line;
    }

    /**
     * Invokes an interactive mode which makes user choose from available methods.
     *
     * @param modules The list of all available modules.
     *
     * @return Returns the list of the modules chosen by the user.
     */
    private static @NotNull ArrayList<Module> chooseModules(@NotNull List<Module> modules) {
        ArrayList<Module> activeModules = new ArrayList<>();
        String line;
        String[] options;
        boolean repCond;
        do {
            repCond = true;
            for (int i = 0; i < modules.size(); i++) {
                System.out.println("(" + (i + 1) + ") : " + modules.get(i).description);
            }
            System.out.print("Please type in all the numbers of options you want to choose (separate the numbers with commas): ");
            try {
                line = getInput();
                System.out.print("\n"); //separates the dialog
                if (!line.isBlank()) {
                    options = line.split(",");
                    options = Arrays.stream(options).filter(x -> !x.isBlank()).toArray(String[]::new);
                    for (String opt : options) {
                        try {
                            int x = Integer.parseInt(opt.trim());
                            if (x <= 0 || x > modules.size())
                                throw new WrongInputException("Incorrect option number.\nPlease re-type all your options.");
                            if (!activeModules.contains(modules.get(x - 1))) {
                                activeModules.add(modules.get(x - 1));
                            }
                            repCond = false;

                        } catch (NumberFormatException e) {
                            Stream<Module> mods;            //this block of code ensures that the program won't fail in case the user types in the part of a module's keyword.
                            mods = modules.stream().filter(x -> x.keyword.toLowerCase().contains(opt.toLowerCase()));
                            if (mods.count() == 1) {
                                mods.forEach(activeModules::add); //however, the module has to be unambiguous
                            } else {
                                System.out.println("An error has occurred. You typed another character with the number.\nPlease re-type all your options.");
                                throw new WrongInputException();
                            }
                        }
                    }
                }
            } catch (WrongInputException e) {
                System.out.println("Be sure you choose only correct options " + enumerateModules(modules) + " and separate them with commas.");
                repCond = true;
            }
        } while (repCond);

        return activeModules;
    }

    /**
     * Iterates through the list of modules to indicate the correct values to the
     * user.
     *
     * @param modules The list of available modules
     *
     * @return Returns a string of all active modules in format "{ 1, 2, ... }
     *         "the number of last module" }"
     */
    private static @NotNull String enumerateModules(@NotNull List<Module> modules) {
        StringBuilder result = new StringBuilder("{ ");
        for (int i = 1; i < modules.size(); i++) {
            result.append(i).append(", ");
        }
        result.append(modules.size()).append(" }");

        return result.toString();
    }

    /**
     * This method goes through all of the modules and eliminates any incompatible
     * types, such as performs exit procedure if the user wants to exit the Flight
     * Planner or removes all modules if these are included in a more complex module.
     *
     * @param active The raw list of modules which have been activated. However,
     *               these need to be corrected for incompatible types.
     */
    private static void checkActiveModules(@NotNull ArrayList<Module> active) {
        ArrayList<Module> toRemove = new ArrayList<>();
        if (active.stream().anyMatch(x -> x.getClass().getName().contains("ExitFlightPlannerModule"))) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                System.out.print("Are you sure to exit the planner?\nConfirm by typing \"y\": ");
                String confirm = br.readLine();
                if (confirm.toLowerCase().startsWith("y")) {
                    System.out.println("Goodbye. See you next time in Flight Planner.");
                    Thread.sleep(500);
                    System.exit(10);
                } else {
                    active.stream().filter(x -> x.getClass().getName().contains("Exit")).forEach(toRemove::add);
                    active.removeAll(toRemove);
                    toRemove.clear();
                    System.out.print("\n");
                }
            } catch (InterruptedException | IOException ignored) {
            }
        }

        if ((active.stream().anyMatch(x -> x.processNum == 3)) &&
                active.stream().anyMatch(x -> (x.processNum == 1 || x.processNum == 2))) {
            System.out.println("The most complex operation you want to perform is flight plan creation.\nTherefore, these operations will be suspended: ");
            active.stream().filter(x -> (x.processNum == 1 || x.processNum == 2)).forEach(x -> {
                toRemove.add(x);
                System.out.println("(" + x.processNum + ") : " + x.description);
            });
            active.removeAll(toRemove);
            toRemove.clear();
        }

        System.out.println("The list of operations which will be performed: ");
        for (Module mod : active) {
            System.out.println("(" + mod.processNum + ") : " + mod.description);
        }
        System.out.print("\n");
    }

    /**
     * The method prompts the user to enter all the airports to be searched for
     * and creates a list of provided strings which are separated by any non-letter
     * character.
     *
     * @param initMsg The message to be printed prior to the prompt. If null,
     *                nothing is printed.
     *
     * @return Returns the {@code non-null} list of strings supposed to be airports'
     *         ICAO codes, municipalities or airports names, which will be searched
     *         for in the database.
     */
    public static @NotNull List<String> enterAirports(@Nullable String initMsg) {
        List<String> result = new LinkedList<>();
        String[] fields;

        if (initMsg != null) {
            System.out.println(initMsg);
        }
        do {
            System.out.print("Please enter all the airports you wish to search and separate them with any non-letter character: ");
            fields = getInput().split("[^A-Za-z]+");
            result.addAll(Arrays.asList(fields));
        } while (getResponse(null,"Do you wish to enter more airports? (Y/n): ", "Y"));
        return result;
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
    public static boolean getResponse(@Nullable String message, @NotNull String question, @NotNull String trueResponse) {
        if (message != null) System.out.println(message);
        System.out.print(question);
        return getInput().trim().startsWith(trueResponse);
    }
}
