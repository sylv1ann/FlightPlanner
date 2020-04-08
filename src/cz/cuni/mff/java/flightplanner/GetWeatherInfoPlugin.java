package cz.cuni.mff.java.flightplanner;

import java.io.*;

public class GetWeatherInfoPlugin implements Plugin {

    @Override
    public String name() { return "GetWeatherInfoModule"; }

    @Override
    public String description() { return "Write information about the weather at chosen airports."; }

    @Override
    public String keyword() { return "weather"; }

    @Override
    public Integer pluginID() { return 1; }

    @Override
    public void action() {
        OutputStream outStream;
        outStream = DialogCenter.chooseOutputForm("", true, ""); // TODO: 15/03/2020 remember to check method arguments. Maybe shouhld be default.
        try {
            outStream.write(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DialogCenter.enterAirports(null); //vloží letiská na ktorých by chcel získať počasie
        // TODO: 06/03/2020 na ziskaných letiskách potom zavolám Downloader a už začnem samotné sťahovanie dát a následne parsovanie
    }

}

