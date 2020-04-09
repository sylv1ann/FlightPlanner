package cz.cuni.mff.java.flightplanner;

import java.io.*;
import java.util.List;

public class GetWeatherInfoPlugin implements Plugin {

    OutputStream outStream;

    @Override
    public String name() { return this.getClass().getName(); }

    @Override
    public String description() { return "Write information about the weather at chosen airports."; }

    @Override
    public String keyword() { return "weather info"; }

    @Override
    public Integer pluginID() { return 1; }

    @Override
    public void action() {
        boolean auto = DialogCenter.getResponse(null,
                "Do you want the " + this.keyword() + " output to be managed automatically? (Y/n): ",
                "Y",
                true
        );
        if (DialogCenter.getResponse(null,
                                     "Do you want to precise the date and time for the output? (Y/n)",
                                     "Y",
                                     true)
           ) {
            DialogCenter.getInput(false); //this block of code will invoke the method for time and date precision

        }
        List<Airport> foundAirports = Airport.searchAirports(null, false);
        PrintStream pr;
        if (auto)
            outStream = DialogCenter.chooseOutputForm("", false, null);

        for (Airport apt : foundAirports) {
            if (!auto)
                outStream = DialogCenter.chooseOutputForm(" for " + apt.icaoCode + " airport", true, apt.icaoCode);
            else {
                if (outStream instanceof FileOutputStream)
                    outStream = DialogCenter.setFileOutputStream(false, apt.icaoCode);
            }
            //tu niekde by sa malo začať odohrávať sťahovanie ktoré vráti súbor s daným metarom z Downloader.downloadMETAR()
        }

        // TODO: 06/03/2020 na ziskaných letiskách potom zavolám Downloader a už začnem samotné sťahovanie dát a následne parsovanie
    }

}
