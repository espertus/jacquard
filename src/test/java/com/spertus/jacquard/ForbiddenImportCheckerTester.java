package com.spertus.jacquard;

import com.spertus.jacquard.common.*;
import com.spertus.jacquard.syntaxgrader.*;
import org.junit.jupiter.api.*;

import java.net.URISyntaxException;
import java.util.List;

public class ForbiddenImportCheckerTester {
    @BeforeAll
    public static void setup() {
        Autograder.initForTest();
    }

    @Test
    public void testNoForbiddenImports() throws URISyntaxException {
        ForbiddenImportChecker checker = new ForbiddenImportChecker(
                1.0, List.of("javax"));
        List<Result> results = checker.grade(TestUtilities.getTargetFromResource("good/Import.java"));
        TestUtilities.assertResultsMatch(results, 1, 1.0, 1.0);
    }

    @Test
    public void testSomeForbiddenImports() throws URISyntaxException {
        ForbiddenImportChecker checker = new ForbiddenImportChecker(
                1.0, List.of("java.util", "javax"));
        List<Result> results = checker.grade(TestUtilities.getTargetFromResource("good/Import.java"));
        TestUtilities.assertResultsMatch(results, 1, 0.0, 1.0);
    }

    @Test
    public void testAllForbiddenImports() throws URISyntaxException {
        ForbiddenImportChecker checker = new ForbiddenImportChecker(
                1.0, List.of("java.util", "javax"));
        List<Result> results = checker.grade(TestUtilities.getTargetFromResource("good/Import.java"));
        TestUtilities.assertResultsMatch(results, 1, 0.0, 1.0);
    }
}
