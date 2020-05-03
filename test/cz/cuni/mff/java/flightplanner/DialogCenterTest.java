package cz.cuni.mff.java.flightplanner;

import org.junit.Test;

public class DialogCenterTest {

    @Test(expected = IllegalArgumentException.class)
    public void chooseOutputForm_NullArgTest() {
        DialogCenter.chooseOutputForm(null, false, "");
    }

    @Test
    public void listPluginsTest() {
    }
}
