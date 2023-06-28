package com.spertus.jacquard.junittester;

import com.spertus.jacquard.common.*;

import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * A tester that runs JUnit tests having the {@link GradedTest} annotation.
 */
public class JUnitTester extends Tester {
    private static final String TESTER_NAME = "JUnit Tester";
    private final List<? extends DiscoverySelector> selectors;
    private PrintStream stdoutCapture;

    /**
     * Constructs a JUnit tester that will run tests in the specified classes.
     *
     * @param classes the classes containing the tests
     */
    public JUnitTester(Class<?>... classes) {
        super(TESTER_NAME);
        selectors = Arrays.stream(classes)
                .map(DiscoverySelectors::selectClass)
                .toList();
    }

    /**
     * Constructs a JUnit tester that will run tests in the specified package.
     *
     * @param packageName the package containing the tests
     */
    public JUnitTester(String packageName) {
        super(TESTER_NAME);
        selectors = List.of(selectPackage(packageName));
    }

    @Override
    public List<Result> run() {
        Launcher launcher = LauncherFactory.create();
        JUnitTester.Listener listener = new Listener();
        launcher.registerTestExecutionListeners(listener);
        PrintStream originalOut = System.out;
        launcher.execute(request().selectors(selectors).build());
        System.setOut(originalOut);
        return listener.results;
    }

    private static class Listener implements TestExecutionListener {
        private final List<Result> results = new ArrayList<>();
        // These get set in executionStarted and used/closed in executionFinished.
        private PrintStream ps;
        private ByteArrayOutputStream baos;

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            baos = new ByteArrayOutputStream();
            if (ps != null) {
                ps.close();
            }
            ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
            System.setOut(ps);
        }

        private String makeOutput(TestExecutionResult teResult) {
            Optional<Throwable> throwable = teResult.getThrowable();
            String s = baos.toString();
            if (throwable.isEmpty()) {
                return s;
            }
            else if (s.isEmpty()) {
                return throwable.get().toString();
            }
            else {
                return s + "\n" + throwable.get();
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.getSource().isPresent()) {
                TestSource source = testIdentifier.getSource().get();
                if (source instanceof MethodSource methodSource) {
                    GradedTest gt = methodSource.getJavaMethod().getAnnotation(GradedTest.class);
                    if (gt != null) {
                        try {
                            results.add(switch (testExecutionResult.getStatus()) {
                                case SUCCESSFUL ->
                                        Result.makeSuccess(gt.name(), gt.points(), baos.toString());
                                case FAILED, ABORTED ->
                                        Result.makeTotalFailure(gt.name(), gt.points(), makeOutput(testExecutionResult));
                            });
                            ps.close();
                        } catch (NoSuchElementException e) { // if get() failed
                            results.add(Result.makeTotalFailure(gt.name(), gt.points(), "Test failed with no additional information"));
                        }
                    }
                }
            }
        }
    }
}