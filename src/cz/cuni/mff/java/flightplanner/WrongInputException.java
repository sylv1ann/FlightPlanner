package cz.cuni.mff.java.flightplanner;

public class WrongInputException extends Throwable {

    public WrongInputException(String format, Object... args) {
        super("An error has occurred.");
        System.out.printf(format, args);
    }

    public WrongInputException() { }
}
