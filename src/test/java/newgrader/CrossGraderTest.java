package newgrader;

import newgrader.crossgrader.*;
import newgrader.exceptions.ClientException;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrossGraderTest {
    private static final String CSV_FILE = """
            , newgrader.crossgrader.CorrectAdder, newgrader.crossgrader.BuggyAdder
            addZero, 2, -1
            """;

    private static final String CSV_FILE_WITH_INT_PARAMS = String.format("""
            , %1$s#0, %1$s#1, %1$s#2, %1$s#3,
            addZero, -1, -2, -3, -4
            """, "newgrader.crossgrader.ParameterizedBuggyAdder");

    private List<Result> getResults(String csv) throws ClientException {
        CrossGrader grader = new CrossGrader(GeneralizedAdderTest.class,
                new ByteArrayInputStream(csv.getBytes()));
        return grader.gradeAll();
    }

    @Test
    public void testClassesWithoutIntParameters() throws ClientException {
        List<Result> results = getResults(CSV_FILE);
        assertEquals(2, results.size());
        testResult(results.get(0), 2, 2);
        testResult(results.get(1), 1, 1);
        assertEquals(3, TestUtilities.getTotalScore(results));
    }

    private void testResult(Result result, double score, double maxScore) {
        assertEquals(score, result.score());
        assertEquals(maxScore, result.maxScore());
    }

    @Test
    public void testClassesWithIntParameters() throws ClientException {
        CrossGrader grader = new CrossGrader(GeneralizedAdderTest.class,
                new ByteArrayInputStream(CSV_FILE_WITH_INT_PARAMS.getBytes()));
        List<Result> results = grader.gradeAll();
        assertEquals(4, results.size());
        // Tests 0, 1, and 2 will fail. Test 3 will succeed.
        testResult(results.get(0), 1, 1);
        testResult(results.get(1), 2, 2);
        testResult(results.get(2), 3, 3);
        testResult(results.get(3), 0, 4);
    }

}
