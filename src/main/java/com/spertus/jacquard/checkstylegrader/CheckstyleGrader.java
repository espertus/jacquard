package com.spertus.jacquard.checkstylegrader;

import com.spertus.jacquard.common.*;
import com.spertus.jacquard.exceptions.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * A grader that runs checkstyle.
 *
 * @see <a href="https://checkstyle.sourceforge.io/cmdline.html">checkstyle</a>
 */
public class CheckstyleGrader extends Grader {
    private static final String GRADER_NAME = "Checkstyle";
    private static final String CHECKSTYLE_VERSION = "10.12.1";
    private static final String CHECKSTYLE_JAR = "checkstyle-" + CHECKSTYLE_VERSION + "-all.jar";
    private static final String CHECKSTYLE_URL = String.format(
            "https://github.com/checkstyle/checkstyle/releases/download/checkstyle-%1s/%2s",
            CHECKSTYLE_VERSION, CHECKSTYLE_JAR);
    private static final String CHECKSTYLE_SUBDIR = "lib";
    private static final String RESULT_FILE_NAME = "checkstyle-results.xml";
    private static final List<String> FIRST_COMMAND_PARTS = List.of(
            "java",
            "-cp",
            "lib/*",
            "com.puppycrawl.tools.checkstyle.Main",
            "-f=xml",
            "-o" + RESULT_FILE_NAME);
    private static final String CONFIG_TEMPLATE = "-c=%s";

    private final String ruleFile;
    private final double penalty;
    private final double maxPoints;

    /**
     * Creates a checkstyle grader.
     *
     * @param name      the name of the grader
     * @param ruleFile  the path to the rule file
     * @param penalty   the penalty per violation
     * @param maxPoints the maximum number of points if no violations occur
     */
    public CheckstyleGrader(
            final String name,
            final String ruleFile,
            final double penalty,
            final double maxPoints) {
        super(name);
        this.ruleFile = ruleFile;
        this.penalty = penalty;
        this.maxPoints = maxPoints;
    }

    /**
     * Creates a checkstyle grader.
     *
     * @param ruleFile  the path to the rule file
     * @param penalty   the penalty per violation
     * @param maxPoints the maximum number of points if no violations occur
     */
    public CheckstyleGrader(
            final String ruleFile,
            final double penalty,
            final double maxPoints) {
        this(GRADER_NAME, ruleFile, penalty, maxPoints);
    }

    // Taken from JGrade:CheckstyleGrader and modified
    private static String getAttributeValue(final String prefix, final Node attribute) {
        return attribute == null ? "" : String.format("%s%s", prefix, attribute.getNodeValue());
    }

    private static String getAttributeValue(final Node attribute) {
        return getAttributeValue("", attribute);
    }

    private String getOutputForErrorNode(final NamedNodeMap attributes) {
        if (attributes == null) {
            throw new InternalError();
        } else {
            final Node lineAttribute = attributes.getNamedItem("line");
            final Node columnAttribute = attributes.getNamedItem("column");
            final Node messageAttribute = attributes.getNamedItem("message");
            final String errorTypeAttribute = getAttributeValue(attributes.getNamedItem("source"));
            return String.format("\t%-20s - %s [%s]\n", getAttributeValue("line: ", lineAttribute) + getAttributeValue(", column", columnAttribute), getAttributeValue(messageAttribute), errorTypeAttribute);
        }
    }

    private int addOutputForFileNode(final StringBuilder sb, final Node elementNode) {
        // Extract information from elementNode.
        final String fullPath = elementNode.getAttributes().getNamedItem("name").toString();
        final String fileName = fullPath.substring(fullPath.lastIndexOf(System.getProperty("file.separator")) + 1, fullPath.length() - 1);
        final NodeList errorNodes = ((Element) elementNode).getElementsByTagName("error");

        // Format result.
        if (errorNodes.getLength() > 0) {
            sb.append(fileName).append(":\n");
        }
        for (int i = 0; i < errorNodes.getLength(); ++i) {
            sb.append(this.getOutputForErrorNode(errorNodes.item(i).getAttributes()));
        }

        // Return number of errors.
        return errorNodes.getLength();
    }

    private void downloadCheckstyleIfNeeded() throws InternalException {
        // It will already be downloaded if the recommended build.gradle file
        // or Dockerfile is used.
        try {
            final File file = new File(CHECKSTYLE_SUBDIR + "/" + CHECKSTYLE_JAR);
            if (!file.exists()) {
                Files.createDirectories(Paths.get(CHECKSTYLE_SUBDIR));
                // https://www.baeldung.com/java-download-file#using-nio
                try (ReadableByteChannel readableByteChannel =
                        Channels.newChannel(new URL(CHECKSTYLE_URL).openStream())) {
                    try (FileOutputStream fileOutputStream =
                                 new FileOutputStream(CHECKSTYLE_SUBDIR + "/" + CHECKSTYLE_JAR)) { // NOPMD
                        fileOutputStream.getChannel()
                                .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                    }
                }
            }
        } catch (IOException e) {
            throw new InternalException("Unable to download checkstyle jar", e);
        }
    }

    private void runCheckstyle(final Target... targets) throws InternalException {
        downloadCheckstyleIfNeeded();
        final List<String> arguments = new ArrayList<>(FIRST_COMMAND_PARTS);
        arguments.add(String.format(CONFIG_TEMPLATE, ruleFile));
        for (Target target : targets) {
            arguments.add(target.toPathString());
        }
        final ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            final Process p = pb.start(); // IOException
            final int result = p.waitFor(); // InterruptedException
            // Positive exit codes mean that checkstyle found problems, not that it failed.
            if (result < 0) {
                throw new InternalException("Exit code indicated checkstyle failure");
            }
        } catch (InterruptedException | IOException e) {
            throw new InternalException("Error running checkstyle ", e);
        }
    }

    private Result interpretOutput() throws
            IOException, SAXException, ParserConfigurationException {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final File file = new File(RESULT_FILE_NAME);
        final Document doc = builder.parse(file);
     //   new File(RESULT_FILE_NAME).delete(); // comment out when debugging
        final NodeList filesWithErrors = doc.getElementsByTagName("file");
        int numErrors = 0;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filesWithErrors.getLength(); i++) {
            final Node fileNode = filesWithErrors.item(i);
            numErrors += addOutputForFileNode(sb, fileNode);
        }
        if (numErrors == 0) {
            return makeSuccessResult(maxPoints, "No violations");
        } else {
            final double score = Math.max(0.0, maxPoints - numErrors * penalty);
            return makePartialCreditResult(score, maxPoints, sb.toString());
        }
    }

    @Override
    public Callable<List<Result>> getCallableSingleTarget(final Target target) {
        return () -> {
            // Either of the next two method calls could throw
            // InternalException. If so, it will be caught in
            // Grader.gradeTimed() or Grader.gradeUntimed().
            runCheckstyle(target);
            return List.of(interpretOutput());
        };
    }

    @Override
    public Callable<List<Result>> getCallableMultiTarget(final Target... targets) {
        return () -> {
            runCheckstyle(targets);
            return List.of(interpretOutput());
        };
    }
}
