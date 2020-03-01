package cz.cuni.mff.java.flighplanner;

import java.io.*;
import java.util.*;
import java.util.jar.*;

public class DialogCenter {

    private static Scanner inputReader = new Scanner(System.in);
    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private static ArrayList<Module> modules = new ArrayList<>(), activeModules = new ArrayList<>();

    public static void main(String[] args) {

       List<Class<?>> cl = setAllModules("cz.cuni.mff.java.flighplanner");
       for (Class<?> classe : cl) {
           System.out.println(classe.getName());
       }


        /*setAllModules();
        showMainMenu();
        chooseModules();*/
    }

    private static List<Class<?>> setAllModules(String packageName) {
        String path = packageName.replaceAll("\\.", File.separator);
        List<Class<?>> classes = new ArrayList<>();
        String[] classPathEntries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));

        String name;
        for (String classpathEntry : classPathEntries) {
            if (classpathEntry.endsWith(".jar")) {
                File jar = new File(classpathEntry);
                try {
                    JarInputStream is = new JarInputStream(new FileInputStream(jar));
                    JarEntry entry;
                    while((entry = is.getNextJarEntry()) != null) {
                        name = entry.getName();
                        if (name.endsWith(".class")) {
                            if (name.contains(path) && name.endsWith(".class")) {
                                String classPath = name.substring(0, entry.getName().length() - 6);
                                classPath = classPath.replaceAll("[\\|/]", ".");
                                classes.add(Class.forName(classPath));
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Silence is gold
                }
            } else {
                try {
                    File base = new File(classpathEntry + File.separatorChar + path);
                    for (File file : base.listFiles()) {
                        name = file.getName();
                        if (name.endsWith(".class")) {
                            name = name.substring(0, name.length() - 6);
                            classes.add(Class.forName(packageName + "." + name));
                        }
                    }
                } catch (Exception ex) {
                    // Silence is gold
                }
            }
        }

        return classes;
        /*// TODO: 29/02/2020 Try to use reflection API to get all classes that extend Module class and create their instances
        modules.add(new GetWeatherInfoModule());
        modules.add(new GetAirportInfoModule());
        modules.add(new CreateFlightPlanModule());
        modules.add(new ExitFlightPlannerModule());*/
    }

    private static void showMainMenu() {
        System.out.println("Welcome to this amazing Flight Planner.");
        System.out.println("You will be provided with several possibilities to show/construct your entire flight plan or its parts.");
        pressAnyKeyToContinue();
    }

    private static void pressAnyKeyToContinue() {
        System.out.println("Press 'Enter' to continue...");
        try {
            System.in.read();
        }
        catch(Exception ignored) { }
    }

    private static void chooseModules() {
        String line;
        String[] options;
        boolean repCond;
        do {
            repCond = true;
            System.out.println("Please type in all the numbers of options you want to choose (separate the numbers with commas).");
            for (int i = 0; i < modules.size(); i++) {
                System.out.println("(" + (i + 1) + ") : " + modules.get(i).description);
            }
            try {
                do {
                    line = br.readLine();
                } while (line == null || line.isBlank());
                if (!line.isBlank()) {
                    options = line.split(",");
                    for (String opt : options) {
                        try {
                            if (!opt.isBlank()) {
                                int x = Integer.parseInt(opt.trim());
                                if (x <= 0 || x > modules.size())
                                    throw new WrongInputException("Incorrect option number.\n Please re-type all your options.");
                                if (!activeModules.contains(modules.get(x))) {
                                    activeModules.add(modules.get(x));
                                }
                                repCond = false;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("An error has occured.\n" +
                                    "You typed another character with the number.\n" +
                                    "Please re-type all your options.\n" +
                                    "Be sure you choose only correct options {1, 2, 3, 4} and separate them with commas.");
                            repCond = true;
                        } catch (WrongInputException ex) {
                            System.out.println("Be sure you choose only correct options {1, 2, 3, 4} and separate them with commas.");
                            repCond = true;
                        }
                    }
                }
            }
            catch (IOException ignored) { }
        } while(repCond);
    }

    /*

     prompt to invoke //remove(new File("C:\\Users\\vikto\\Downloads\\airports1.csv"));

    public static void remove(File file) {
        File outputFile = new File("C:\\Users\\vikto\\Downloads\\airports2.csv");
        try {
            if (outputFile.createNewFile()) {
                try (InputStreamReader is = new InputStreamReader(new FileInputStream(file));
                     BufferedReader br = new BufferedReader(is);
                     OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream(outputFile))) {
                    String line;
                    int lineNumber = 0;

                    while ((line = br.readLine()) != null) {
                        ++lineNumber;
                        String[] fields = line.split(",", 0);
                        System.out.println("Printing line: " + lineNumber);
                        for (String field : fields) {
                            wr.append(field).append(",");
                        }
                        wr.append("\n");
                    }
                } catch (IOException ignored) { }
            }
        } catch (IOException ignored) { }
    }*/
}
