package cz.cuni.mff.java.flightplanner;

import org.jetbrains.annotations.NotNull;
import java.io.*;

public interface MyPrinterI {

    void setWriter(OutputStream writer);
    OutputStream getWriter();
    void append(byte[] message) throws IOException;
}

class MyPrinter implements MyPrinterI {

    @NotNull
    private OutputStream writer = System.out;

    public void setWriter(@NotNull OutputStream writer) {
        this.writer = writer;
    }

    @NotNull
    @Override
    public OutputStream getWriter() {
        return writer;
    }

    @Override
    public void append(byte[] message) throws IOException {
        this.writer.write(message);
    }
}