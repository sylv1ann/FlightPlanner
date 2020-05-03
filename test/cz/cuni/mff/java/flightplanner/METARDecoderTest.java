package cz.cuni.mff.java.flightplanner;

import org.junit.Test;

import static java.lang.System.out;

public class METARDecoderTest {

    private static final String wndPattern      = "((VRB)[0-9]{2}|[0-9]{5})(G[0-9]{2})?(KT|MPS)",
                                vartnPattern    = "[0-9]{3,4}V[0-9]{3,4}",
                                vsbltyPattern   = "[0-9 /]{1,5}(SM)?",
                                rvrPattern      = "R[0-9]{2}[LCR]?/(P|M|[0-9]+V)?[0-9]+(FT/)?[DNU]?",
                                weatherPattern  = "[+-]?[a-zA-Z]{2,}",
                                tempPattern     = "M?[0-9]{2}/M?[0-9]{2}";

    @Test
    public void weatherPhenomenaTest() {
        String result ="Weather: +SHRA = heavy showers rain.";
        assert result.equals(METARDecoder.weatherPhenomena("+SHRA",weatherPattern, false, true));
    }

    @Test
    public void weatherPhenomenaTest1() {
        String result = "Weather: BLSN = moderate blowing snow.";
        assert result.equals(METARDecoder.weatherPhenomena("BLSN",weatherPattern, false, true));
    }

    @Test
    public void weatherPhenomenaTest2() {
        String result = "Weather: -RA = light rain.";
        assert result.equals(METARDecoder.weatherPhenomena("-RA",weatherPattern, false, true));
    }

    @Test
    public void rvrVisibilityTest() {
        String result = "----------------------------------------------- R04R/P1500N ------------------------------------------------\n" +
                        "Runway 04R, touchdown zone visual range is more than 1500 meters and no change is expected.";
        String actResult = METARDecoder.rvrVisibility("R04R/P1500N", rvrPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void rvrVisibilityTest1() {
        String result = "----------------------------------------------- R22/P1500FT/U ------------------------------------------------\n" +
                        "Runway 22, touchdown zone visual range is more than 1500 feet (457.2 meters) and rising is expected.";
        String actResult = METARDecoder.rvrVisibility("R22/P1500FT/U", rvrPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
       assert result.equals(actResult);
    }

    @Test
    public void visibilityTest() {
        String result = "----------------------------------------------- 1400 ------------------------------------------------\n" +
                        "Maximum horizontal visibility: 1400 meters.";
        String actResult = METARDecoder.visibility("1400",vsbltyPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void visibilityTest1() {
        String result = "----------------------------------------------- 1 1/2SM ------------------------------------------------\n" +
                        "Maximum horizontal visibility: 1 1/2 statute miles.";
        String actResult = METARDecoder.visibility("1 1/2SM",vsbltyPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void windDirSpd() {
        String result = "----------------------------------------------- 04011KT ------------------------------------------------\n" +
                        "Wind: The wind blows from 040 degrees at 11 knots (20.37 km/h).";
        String actResult = METARDecoder.windDirSpd("04011KT", wndPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void windDirSpd1() {
        String result = "----------------------------------------------- 27036G43KT ------------------------------------------------\n" +
                        "Wind: The wind blows from 270 degrees at 36 knots (66.67 km/h) with gusts of 43 knots (79.64 km/h).";
        String actResult = METARDecoder.windDirSpd("27036G43KT", wndPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void windVariation() {
        String result = "----------------------------------------------- 180V240 ------------------------------------------------\n" +
                        "Variable wind: The wind direction varies between 180 degrees and 240 degrees.\n" +
                        "The wind direction has varied by 60 degrees or more in last 10 minutes with the mean speed exceeding 3 knots.";
        String actResult = METARDecoder.windVariation("180V240", vartnPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void temperatureTest() {
        String result = "----------------------------------------------- M05/M02 ------------------------------------------------\n" +
                "Temperature: -05 degrees.\n" +
                "Dewpoint   : -02 degrees.";
        String actResult = METARDecoder.temperature("M05/M02", tempPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void temperatureTest1() {
        String result = "----------------------------------------------- 05/02 ------------------------------------------------\n" +
                        "Temperature: 05 degrees.\n" +
                        "Dewpoint   : 02 degrees.";

        String actResult = METARDecoder.temperature("05/02", tempPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void temperatureTest2() {
        String result = "----------------------------------------------- 05/M02 ------------------------------------------------\n" +
                        "Temperature: 05 degrees.\n" +
                        "Dewpoint   : -02 degrees.";
        String actResult = METARDecoder.temperature("05/M02", tempPattern, true);
        if (!result.equals(actResult)) {
            out.println(actResult);
        }
        assert result.equals(actResult);
    }

    @Test
    public void rsgTest() {
        out.println(METARDecoder.rwyStateGroup("8849//91", "[0-9]{2}[0-9/]{6}", false));
    }

    @Test
    public void wholeMETARDecode() {
        //new METARDecoder().decode("202004251330 METAR CYYQ 171100Z 30024KT 1SM R33/3500FT +SN BLSN BKN006 OVC022 M19/M23 A2978 RMK SF4NS4 VSBY 1/2?11/2 SLP134", out);
        //new METARDecoder().decode("202004251330 METAR LZKZ 251330Z 34015KT CAVOK 13/06 Q1002 NOSIG", out);
        //new METARDecoder().decode("202004251300 METAR LZKZ 251300Z 36015KT 9999 -SHRA FEW024 BKN080 12/04 Q1002 NOSIG", out);
    }
}
