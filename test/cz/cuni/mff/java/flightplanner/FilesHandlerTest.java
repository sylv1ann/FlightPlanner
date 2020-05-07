package cz.cuni.mff.java.flightplanner;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FilesHandlerTest {

    @Test
    public void fileCreationTest() {
        File newFile = FilesHandler.createNewFile(null, "test1.txt");
        System.out.println(newFile.getName());
        System.out.println(newFile.getAbsolutePath());
        try (FileWriter wr = new FileWriter(newFile)) {
            wr.write("Hello world");
            System.out.println("Writing to %FILE successful.".replace("%FILE", newFile.getName()));
            newFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println("File %FILE not found"
                    .replace("%FILE", newFile.getName()));
            assert false;
        }
    }

    @Test
    public void secondFile() {
        File newFile = FilesHandler.createNewFile("C:\\Users\\vikto\\Downloads", "test2.txt");
        //newFile.deleteOnExit();
        System.out.println(newFile.getName());
        System.out.println(newFile.getAbsolutePath());
        try (FileWriter wr = new FileWriter(newFile)) {
            wr.write("Hello world");
            System.out.println("Writing to %FILE successful.".replace("%FILE", newFile.getName()));
            newFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println("File %FILE not found"
                    .replace("%FILE", newFile.getName()));
            assert false;
        }
    }

    @Test
    public void thirdFile() {
        File newFile = FilesHandler.createNewFile("../../../", "test3.txt");
        //newFile.deleteOnExit();
        System.out.println(newFile.getName());
        System.out.println(newFile.getAbsolutePath());
        try (FileWriter wr = new FileWriter(newFile)) {
            wr.write("Hello world");
            System.out.println("Writing to %FILE successful.".replace("%FILE", newFile.getName()));
            newFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println("File %FILE not found"
            .replace("%FILE", newFile.getName()));
            assert false;
        }
    }

    @Test
    public void fourthFile() {
        File newFile = FilesHandler.createNewFile("jk", "test4.txt");
        //newFile.deleteOnExit();
        System.out.println(newFile.getName());
        System.out.println(newFile.getAbsolutePath());
        try (FileWriter wr = new FileWriter(newFile)) {
            wr.write("Hello world");
            System.out.println("Writing to %FILE successful.".replace("%FILE", newFile.getName()));
            newFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println("File %FILE not found"
                    .replace("%FILE", newFile.getName()));
            assert false;
        }
    }

    @Test
    public void pwd() {
        System.out.println(FilesHandler.pwd());
    }
}
