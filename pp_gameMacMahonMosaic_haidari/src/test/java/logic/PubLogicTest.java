package logic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PubLogicTest {

    @Test
    public void simpleTest() {
        Logic logic = new Logic();
        assertEquals(1, logic.getOne());
    }
}
