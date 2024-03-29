package com.spertus.jacquard.junittester.group;

import com.spertus.jacquard.junittester.GradedTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.fail;

// These 2 tests should get merged together.
@Tag("IndirectTest")
public class GroupTest1 {
    @Test
    @GradedTest(name = "Bus constructor",
            description = "Verifies exception is thrown for null argument",
            points = 2.0)
    public void testBusConstructor1() {
        fail();
    }

    @Test
    @GradedTest(name = "Bus constructor",
            description = "Verifies exception is thrown for bad route",
            points = 3.0)
    public void testBusConstructor2() {
        // pass
    }
}
