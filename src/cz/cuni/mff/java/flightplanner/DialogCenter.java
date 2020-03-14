package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

public class DialogCenter {

    public static void main(String[] args) {

        //Airport.setAirportsDatabase();
        //Airport.showAirportsList(null, "");
        //Airport.searchAirports(Airport.getAptDatabase(), false);

        /*ArrayList<Module> modules, activeModules;

        modules = Module.setAllModules(DialogCenter.class.getPackageName());
        Airport.setAirportsDatabase();
        showMainMenu();
        activeModules = chooseModules(modules);
        if (activeModules.size() > 0) {
            checkActiveModules(activeModules);
            Module.startModules(activeModules);
        }*/
    }

    public static OutputStream chooseOutputForm() {
        OutputStream result = System.out;

        System.out.println("You can choose whether your output will be shown on screen or written into separate file/files.");
        System.out.print("Please type your choice (screen / file / default): ");

        switch (getInput()) {
            default:
            case "default":
            case "screen":
                break;
            case "file":
                result = setOutputStream();
                break;
        }

        System.out.print("\n");
        return result;
    }

    private static OutputStream setOutputStream() {
        OutputStream result;
        System.out.print("Please enter your desired file name: ");
        String fileName = getInput();
        if (!fileName.contains(".txt")) {
            fileName = fileName + ".txt";
        }
        System.out.println("Do you want to choose the directory for your new file? (y/n)\nIf not, current directory will be used.");
        String filePath;
        if (getInput().toLowerCase().startsWith("y")) {
            System.out.print("Please enter the absolute path of the destination directory: ");
            filePath = getInput();
        } else {
            filePath = ".";
        }
        try {
            if (".".equals(filePath))
                result = new FileOutputStream(Downloader.fileFromPathCreator(fileName));
            else {
                result = new FileOutputStream(new File(filePath + File.pathSeparator + fileName));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Something went wrong. Please try again.");
            result = setOutputStream();
        }

        return result;
    }

    /**
     * This method only shows initial text information about Flight Planner.
     */
    private static void showMainMenu() {
        System.out.println("Welcome to this amazing Flight Planner.");
        System.out.println("You will be provided with several possibilities to show/construct your entire flight plan or its parts.\n");
    }

    private static @NotNull String getInput() {
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
     */
    @NotNull
    private static ArrayList<Module> chooseModules(@NotNull List<Module> modules) {
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
     * Only iterates through the list of modules to indicate the correct values to the user.
     *
     * @param modules = The list of available modules
     * @return = Returns a string of all active modules in format "{ 1, 2, ..., "the number of last module" }"
     */
    @NotNull
    private static String enumerateModules(@NotNull List<Module> modules) {
        StringBuilder result = new StringBuilder("{ ");
        for (int i = 1; i < modules.size(); i++) {
            result.append(i).append(", ");
        }
        result.append(modules.size()).append(" }");

        return result.toString();
    }

    /**
     * This method goes through all of the modules and eliminates any incompatible types, such as
     * performs exit procedure if the user wants to exit the Flight Planner or removes all modules if
     * these are included in a more complex module.
     *
     * @param active = The raw list of modules which have been activated. However, these need to be corrected for incompatible types.
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

    public static List<String> enterAirports(String initMsg) {
        List<String> result = new LinkedList<>();
        String[] fields;

        if (initMsg != null) {
            System.out.println(initMsg);
        }
        do {
            System.out.print("Please enter all the airports you wish to search and separate them with any non-letter character: ");
            fields = getInput().split("[^A-Za-z]+");
            result.addAll(Arrays.asList(fields));
        } while (getResponse("Do you wish to enter more airports? (Y/n): ", "Y"));
        return result;
    }

    public static boolean getResponse(String message, String trueResponse) {
        System.out.print(message);
        return getInput().trim().startsWith(trueResponse);
    }
}
