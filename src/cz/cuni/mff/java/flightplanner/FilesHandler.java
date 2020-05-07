package cz.cuni.mff.java.flightplanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FilesHandler {

    private static final Path defaultResourceDirPath = Path.of("resources"),
                              defaultOutputDirPath = Path.of("output"),
                              projectParentRootPath = Path.of(System.getProperty("user.dir"));

    /**
     * The method which searches for the resource file needed for the program.
     * The search starts in the default "resources" directory. If the resource is
     * not found, then it searches in the project root and eventually in the home
     * directory. If the resource is still not found, then the exception is thrown.
     *
     * @param fileName The name of the resource to be searched for.
     * @return Returns the resource file, or null if it is not found.
     */
    public static @Nullable File findResource(@NotNull String fileName) {
        File result = null;

        try {
            File foundResource;
            foundResource = walkThrough(defaultOutputDirPath, fileName);
            if (foundResource != null) {
                return foundResource;
            } else {
                foundResource = walkThrough(projectParentRootPath,fileName);
                if (foundResource != null) {
                    return foundResource;
                } else {
                    Path homeRoot = Path.of(System.getProperty("user.home"));
                    foundResource = walkThrough(homeRoot, fileName);
                    if (foundResource != null) {
                        return foundResource;
                    } else throw new IOException();
                }
            }
        } catch (IOException ignored) {
            System.err.println("Could not walk or find the file.");
            return null;
        }
    }

    /**
     * Wrapper around file walking used several times in findResource(String) method.
     *
     * @param path Represents the path where the {@code fileName} should be searched
     *             for.
     *
     * @param fileName The fileName to be searched for while walking through the
     *                 {@code path}.
     *
     * @return The file representing the found resource file, or {@code null}, when
     *         such a file could not be found or an error occurrs.
     */
    private static @Nullable File walkThrough(Path path, String fileName) {
        List<Path> foundPaths;

        try {
            if (Files.walk(defaultResourceDirPath)
                    .anyMatch(x -> x.getFileName()
                                            .toString()
                                            .equals(fileName))) {
                foundPaths = Files.walk(defaultResourceDirPath)
                                  .filter(x -> x.getFileName()
                                                        .toString()
                                                        .equals(fileName))
                                  .collect(Collectors.toUnmodifiableList());
                return new File(foundPaths.get(0).toUri());
            } else return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Creates a new file in the user defined directory or in the default output
     * directory.
     * @param pathName Represents a desired path to store the file. This
     *                 parameter may represent an absolute path as well as a
     *                 relative path to the current project directory.
     *                 If null or if the path is invalid, the default "output"
     *                 directory of the project is used.
     *
     * @param fileName Name of the new file.
     * @return New file either in the specified directory or in the default
     *         project output directory.
     */
    static @NotNull File createNewFile(@Nullable String pathName, @NotNull String fileName) {
        Path outPath;
        try {
            outPath = Path.of(pathName)
                          .toAbsolutePath()
                          .normalize();
            if (!Files.exists(outPath) || pathName.isBlank()) {
                throw new NullPointerException();
            }
        } catch (NullPointerException | InvalidPathException invalidPath) {
            outPath = defaultOutputDirPath.toAbsolutePath()
                                          .normalize();
            if (pathName != null ) {
                System.out.println("Specified path is invalid.");
                System.out.println("Therefore, %DEFAULT_PATH will be used."
                                   .replace("%DEFAULT_PATH",outPath.toString()));
            }
        }

        return new File(outPath + File.separator + fileName);
    }

    /**
     * @return The {@code String} representing the current directory.
     */
    static String pwd() {
        return System.getProperty("user.dir");
    }
}
