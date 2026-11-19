package unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JUnit6Test {

    @Test
    void testJUnit6() {
        int a = 10;
        int b = 20;

        assertEquals(30, a + b);
    }
}