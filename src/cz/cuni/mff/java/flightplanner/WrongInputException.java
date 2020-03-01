package cz.cuni.mff.java.flightplanner;

public class WrongInputException extends Throwable {

    public WrongInputException(String msg) {
        super("An error has occured.");
        System.out.println(msg);
    }

    public WrongInputException() { }
}
