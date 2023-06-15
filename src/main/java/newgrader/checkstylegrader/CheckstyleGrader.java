package newgrader.checkstylegrader;

import newgrader.*;
import newgrader.exceptions.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class CheckstyleGrader {
    private static final String RESULT_FILE_NAME = "checkstyle-results.xml";
    private static final List<String> FIRST_COMMAND_PARTS = List.of(
            "java",
            "-cp",
            "\"lib/*\"",
            "com.puppycrawl.tools.checkstyle.Main",
            "-f=xml",
            "-o",
            RESULT_FILE_NAME);
    private static final String CONFIG_TEMPLATE = "-c=%s";

    private final String ruleFile;
    private final String pathToCheck;
    private final double penalty;
    private final double maxPoints;

    public CheckstyleGrader(String ruleFile, String pathToCheck, double penalty, double maxPoints) {
        this.ruleFile = ruleFile;
        this.pathToCheck = pathToCheck;
        this.penalty = penalty;
        this.maxPoints = maxPoints;
    }

    // Taken from JGrade:CheckstyleGrader and modified
    private static String getAttributeValue(String prefix, Node attribute) {
        return attribute == null ? "" : String.format("%s%s", prefix, attribute.getNodeValue());
    }

    private static String getAttributeValue(Node attribute) {
        return getAttributeValue("", attribute);
    }

    private String getOutputForErrorNode(NamedNodeMap attributes) {
        if (attributes == null) {
            throw new InternalError();
        } else {
            Node lineAttribute = attributes.getNamedItem("line");
            Node columnAttribute = attributes.getNamedItem("column");
            Node messageAttribute = attributes.getNamedItem("message");
            String errorTypeAttribute = getAttributeValue(attributes.getNamedItem("source"));
            return String.format("\t%-20s - %s [%s]\n", getAttributeValue("line: ", lineAttribute) + getAttributeValue(", column", columnAttribute), getAttributeValue(messageAttribute), errorTypeAttribute);
        }
    }

    private int addOutputForFileNode(StringBuilder sb, Node elementNode) {
        String fullPath = elementNode.getAttributes().getNamedItem("name").toString();
        String fileName = fullPath.substring(fullPath.lastIndexOf(System.getProperty("file.separator")) + 1, fullPath.length() - 1);
        NodeList errorNodes = ((Element) elementNode).getElementsByTagName("error");
        if (errorNodes.getLength() > 0) {
            sb.append(fileName).append(":\n");
        }

        for (int i = 0; i < errorNodes.getLength(); ++i) {
            sb.append(this.getOutputForErrorNode(errorNodes.item(i).getAttributes()));
        }

        return errorNodes.getLength();
    }

    private void runCheckstyle() {
        List<String> arguments = new ArrayList<>(FIRST_COMMAND_PARTS);
        arguments.add(String.format(CONFIG_TEMPLATE, ruleFile));
        arguments.add(pathToCheck);
        ProcessBuilder pb = new ProcessBuilder(arguments);
        try {
            Process p = pb.start();
            int result = p.waitFor();
            // Positive exit codes mean that checkstyle found problems, not that it failed.
            if (result < 0) {
                throw new DependencyException("Exit code indicated checkstyle failure");
            }
        } catch (InterruptedException | IOException e) {
            throw new DependencyException("Error running checkstyle ", e);
        }
    }

    private Result interpretOutput() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        File file = new File(RESULT_FILE_NAME);
        Document doc = builder.parse(file);
        NodeList filesWithErrors = doc.getElementsByTagName("file");
        int numErrors = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filesWithErrors.getLength(); i++) {
            Node fileNode = filesWithErrors.item(i);
            numErrors += addOutputForFileNode(sb, fileNode);
        }
        if (numErrors == 0) {
            return Result.makeSuccess("Checkstyle", maxPoints, "No violations");
        } else {
            double score = Math.min(0.0, maxPoints - numErrors * penalty);
            return Result.makeResult("Checkstyle", score, maxPoints, sb.toString());
        }
    }

    public Result grade() {
        try {
            runCheckstyle();
            return interpretOutput();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            return Result.makeError("Internal error when running Checkstyle", e);
        }
    }

    public static void main(String[] args) {
        Result result = new CheckstyleGrader("sun_checks.xml", "foo.java", 0, 5).grade();
        System.out.println(result);
    }
}
