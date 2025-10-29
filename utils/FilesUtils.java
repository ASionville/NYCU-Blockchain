package p2pblockchain.utils;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for file operations in the P2PBlockchain project.
 */
public class FilesUtils {
    
    /**
     * Read the entire content of a file into a byte array.
     *
     * @param filePath Path to the file
     * @return byte array containing the file's content
     */
    public static byte[] readFileToBytes(String filePath) {
        try {
            if (Files.exists(Path.of(filePath))) {
                return Files.readAllBytes(Path.of(filePath));
            } else {
                Logger.error("File not found: " + filePath);
                return new byte[0];
            }
        } catch (Exception e) {
            Logger.error("Error reading file: " + filePath);
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Write a byte array to a file.
     *
     * @param filePath Path to the file
     * @param data Byte array to write
     * @param writeMode Write mode ("c" for create, "a" for append, "o" for overwrite)
     * @return true if write was successful, false otherwise
     */
    public static boolean writeFile(String filePath, byte[] data, String writeMode) {
        try {
            switch (writeMode) {
                case "c":
                    if (Files.exists(Path.of(filePath))) {
                        Logger.error("File already exists: " + filePath);
                        return false;
                    } else {
                        Files.write(Path.of(filePath), data, StandardOpenOption.CREATE_NEW);
                        return true;
                    }
                case "a":
                    if (Files.exists(Path.of(filePath))) {
                        Files.write(Path.of(filePath), data, StandardOpenOption.APPEND);
                    } else {
                        Files.write(Path.of(filePath), data, StandardOpenOption.CREATE_NEW);
                    }
                    return true;
                case "o":
                    if (Files.exists(Path.of(filePath))) {
                        Logger.warn("Overwriting existing file: " + filePath);
                        Files.deleteIfExists(Path.of(filePath));
                    }
                    Files.write(Path.of(filePath), data, StandardOpenOption.CREATE_NEW);
                    return true;
                default:
                    Logger.error("Unsupported write mode: " + writeMode);
                    return false;
            }
        } catch (Exception e) {
            Logger.error("Error writing file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a file.
     *
     * @param filePath Path to the file
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
            return true;
        } catch (Exception e) {
            Logger.error("Error deleting file: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * List all files in a directory.
     *
     * @param directoryPath Path to the directory
     * @return Array of file paths in the directory
     */
    public static String[] listFilesInDirectory(String directoryPath) {
        if (!Files.isDirectory(Path.of(directoryPath))) {
            Logger.error("Provided path is not a directory: " + directoryPath);
            return new String[0];
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(directoryPath))) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .map(Path::toString)
                    .toArray(String[]::new);
        } catch (Exception e) {
            Logger.error("Error listing files in directory: " + directoryPath);
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Create a directory (including any necessary but nonexistent parent directories).
     *
     * @param directoryPath Path to the directory to create
     * @return true if the directory was created or already exists, false otherwise
     */
    public static boolean createDirectory(String directoryPath) {
        try {
            Files.createDirectories(Path.of(directoryPath));
            return true;
        } catch (Exception e) {
            Logger.error("Error creating directory: " + directoryPath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a directory and all its contents.
     *
     * @param directoryPath Path to the directory to delete
     * @return true if the directory was deleted, false otherwise
     */
    public static boolean deleteDirectory(String directoryPath) {
        Path rootPath = Path.of(directoryPath);
        try {
            try (Stream<Path> walk = Files.walk(rootPath)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                return true;
            } catch (Exception e) {
                Logger.error("Error deleting directory: " + directoryPath);
                return false;
            }
        } catch (Exception e) {
            Logger.error("Error processing directory: " + directoryPath);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if a file or directory exists at the given path.
     *
     * @param path Path to check
     * @return true if the file or directory exists, false otherwise
     */
    public static boolean fileExist(String path) {
        return Files.exists(Path.of(path));
    }
}