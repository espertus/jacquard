package com.spertus.jacquard;

import com.spertus.jacquard.common.*;
import com.spertus.jacquard.exceptions.ClientException;
import com.spertus.jacquard.pmdgrader.PmdGrader;
import org.junit.jupiter.api.*;

import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PmdGraderTest {
    private static final double PENALTY_PER_VIOLATION = .5;
    private static final double MAX_PENALTY = 2.5;

    private Target missingCommentsTarget;
    private Target missingOverrideTarget;

    @BeforeAll()
    public static void init() {
        Autograder.initForTest();
    }

    @BeforeEach
    public void setup() throws URISyntaxException {
        missingCommentsTarget = TestUtilities.getTargetFromResource("good/MissingComments.java");
        missingOverrideTarget = TestUtilities.getTargetFromResource("good/FavoritesIterator.java");
    }

    @Test
    public void testRepeatability() {
        Grader grader =  PmdGrader.createFromRules(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml",
                "CommentRequired");
        TestUtilities.testRepeatability(grader, missingCommentsTarget);
    }

    @Test
    public void testSingleRule() throws ClientException {
        PmdGrader pmdGrader = PmdGrader.createFromRules(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml",
                "CommentRequired");
        List<Result> results = pmdGrader.grade(missingCommentsTarget);
        TestUtilities.assertResultsMatch(results, 1, MAX_PENALTY - 2 * PENALTY_PER_VIOLATION, MAX_PENALTY);
    }

    @Test
    public void testTwoRules() throws ClientException {
        PmdGrader pmdGrader = PmdGrader.createFromRules(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml",
                "CommentRequired", "UncommentedEmptyConstructor");
        List<Result> results = pmdGrader.grade(missingCommentsTarget);
        TestUtilities.assertResultsMatch(results, 1, MAX_PENALTY - 3 * PENALTY_PER_VIOLATION, MAX_PENALTY);
    }

    @Test
    public void testBadRuleset() {
        assertThrows(ClientException.class,
                () -> PmdGrader.createFromRuleSetPaths(
                        PENALTY_PER_VIOLATION,
                        MAX_PENALTY,
                        "category/java/documentation.xml",
                        "BADPATH/java/documentation.xml"));
    }

    @Test
    public void testBadRulename() {
        assertThrows(ClientException.class,
                () -> PmdGrader.createFromRules(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml",
                "CommentRequired", "NoSuchRule"));
    }

    @Test
    public void testSingleRuleSet() throws ClientException {
        PmdGrader pmdGrader = PmdGrader.createFromRuleSetPaths(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml");
        List<Result> results = pmdGrader.grade(missingCommentsTarget);
        assertEquals(1, results.size());
        assertEquals(MAX_PENALTY - 3 * PENALTY_PER_VIOLATION, TestUtilities.getTotalScore(results));
    }

    @Test
    public void testTwoRuleSets() throws ClientException {
        PmdGrader pmdGrader = PmdGrader.createFromRuleSetPaths(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml",
                "category/java/codestyle.xml");
        List<Result> results = pmdGrader.grade(missingCommentsTarget);
        TestUtilities.assertResultsMatch(results, 1, MAX_PENALTY - 5 * PENALTY_PER_VIOLATION, MAX_PENALTY);
    }

    @Test
    public void testDirectory() {
        PmdGrader pmdGrader = PmdGrader.createFromRuleSetPaths(
                PENALTY_PER_VIOLATION,
                MAX_PENALTY,
                "category/java/documentation.xml",
                "category/java/codestyle.xml");
        List<Target> targets = Target.fromDirectory("src/test/resources/good/");
        List<Result> results = pmdGrader.grade(targets); // lots of errors
        TestUtilities.assertResultsMatch(results, 1, 0, MAX_PENALTY);
    }

    @Test
    public void testFormatting() {
        PmdGrader pmdGrader = PmdGrader.createFromRules(
                1.0,
                5.0,
                "category/java/bestpractices.xml",
                "MissingOverride");
        List<Result> results = pmdGrader.grade(missingOverrideTarget);
        TestUtilities.assertResultsMatch(results, 1, 4.0, 5.0);
        assertTrue(results.get(0).getMessage().contains(
                "The method 'hasNext()' is missing an @Override annotation."));
    }

    @Test
    public void testRulesetIsFile() throws URISyntaxException {
        PmdGrader pmdGrader = PmdGrader.createFromRuleSetPaths(
                1.0,
                5.0,
                TestUtilities.getPath("pmd-ruleset.xml").toAbsolutePath().toString());
        List<Result> results = pmdGrader.grade(missingOverrideTarget);
        TestUtilities.assertResultsMatch(results, 1, 3.0, 5.0);
    }

    @Test
    public void testMultipleTargets() {
        PmdGrader pmdGrader = PmdGrader.createFromRuleSetPaths(
                1.0,
                5.0,
                "category/java/bestpractices.xml");
        List<Result> results = pmdGrader.grade(missingCommentsTarget, missingOverrideTarget);
        // 3 violations expected (1 missing override, 2 comment required)
        TestUtilities.assertResultsMatch(results, 1, 2.0, 5.0);
    }
}
