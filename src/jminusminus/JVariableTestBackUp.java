package jminusminus;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * These tests are incorrect
 * 
 * Extra parser semantic tests for Exercises 4.1 and 4.2.
 *
 * These tests reuse the project's LookaheadScanner(String fileName) constructor.
 */
public class JVariableTestBackUp {

    // Write source to a temp file and return the path
    private String writeTempSource(String baseName, String source) throws IOException {
        File tmp = File.createTempFile(baseName, ".java");
        tmp.deleteOnExit();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            w.write(source);
        }
        return tmp.getAbsolutePath();
    }

    // Parse a file, run preAnalyze/analyze, return captured System.err output (debug lines removed)
    private String analyzeFileAndCaptureErrors(String filePath) throws Exception {
        ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        System.setErr(new PrintStream(errBaos, true, "UTF-8"));

        try {
            LookaheadScanner scanner = new LookaheadScanner(filePath);
            Parser parser = new Parser(scanner);
            JCompilationUnit unit = parser.compilationUnit();
            unit.preAnalyze();
            // If your analyze signature requires a context, supply it here (e.g. unit.analyze(unitContext))
            unit.analyze(null);
        } finally {
            System.err.flush();
            System.setErr(oldErr);
        }

        String out = errBaos.toString("UTF-8");

        // Filter out debug lines that begin with "DEBUG:" (case-sensitive)
        StringBuilder filtered = new StringBuilder();
        String[] lines = out.split("\\R");
        for (String line : lines) {
            if (line.startsWith("DEBUG:")) {
                continue; // skip debug lines
            }
            filtered.append(line).append(System.lineSeparator());
        }

        return filtered.toString();
    }

    @Test
    public void testMultiplePublicTopLevelTypes_reportsError() throws Exception {
        String src = ""
                + "public class A { }\n"
                + "public class B { }\n";

        String path = writeTempSource("MultiplePublic", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);

        assertTrue("Expected an error about multiple public types; output:\n" + errors,
                lower.contains("multiple public") || lower.contains("only one public") || lower.contains("public type"));
    }

    @Test
    /**
     * broken.
     * @throws Exception
     */
    public void testOnlyFirstPublicKeepsPublic_secondLosesPublic() throws Exception {
        String src = ""
                + "public class First { }\n"
                + "public class Second { }\n"
                + "class Third { }\n";

        String path = writeTempSource("MultiPublicCheck", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);

        // Basic presence check
        assertTrue("Expected error about multiple public types; got:\n" + errors,
                lower.contains("multiple public") || lower.contains("only one public") || lower.contains("public type"));

     // Collect unique diagnostic lines that contain any of the candidate phrases.
        String[] stderrLines = lower.split("\\R");
        java.util.Set<String> matchingLines = new java.util.LinkedHashSet<>();
        for (String l : stderrLines) {
            if (l.contains("multiple public") || l.contains("only one public") || l.contains("public type")) {
                matchingLines.add(l);
            }
        }

        // 1) There is at least one candidate-phrase reported somewhere (presence)
        assertFalse("Expected at least one diagnostic line containing a candidate phrase; full stderr:\n" + errors,
                matchingLines.isEmpty());

        // 2) Exactly one unique diagnostic line mentions any of the candidate phrases
        assertEquals("Expected exactly one diagnostic line mentioning multiple-public; found:\n" + matchingLines + "\nFull stderr:\n" + errors,
                1, matchingLines.size());

        // 3) The single diagnostic line contains at least one of the candidate phrases (defensive)
        String theLine = matchingLines.iterator().next();
        assertTrue("Expected the diagnostic line to include a candidate phrase; line: " + theLine,
                theLine.contains("multiple public") || theLine.contains("only one public") || theLine.contains("public type"));
    }

    @Test
    public void testUndeclaredVariableReportedOnce_moreStress() throws Exception {
        String src = ""
                + "class A {\n"
                + "  int f() {\n"
                + "    x = 1;          // first use of x -> should be reported\n"
                + "    x = x + 2;      // second use of x -> should NOT be reported again\n"
                + "    y = 3;          // first use of y -> should be reported once\n"
                + "    x = x + y;      // uses both x and y again -> no additional undeclared errors\n"
                + "    return x + y;\n"
                + "  }\n"
                + "}\n";

        String path = writeTempSource("UndeclaredVarStress", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);

        // Ensure 'cannot find name' appears exactly twice (x and y)
        Map<Integer, LineMatch> cannotFindMatches = findMatchesByLine(lower, "cannot find name");
        if (cannotFindMatches.size() != 2) {
            StringBuilder debug = new StringBuilder("Expected two 'cannot find name' diagnostics but found " + cannotFindMatches.size() + ":\n");
            debug.append(cannotFindMatches).append("\nFull stderr:\n").append(errors);
            fail(debug.toString());
        }

        // Ensure there is at least one 'invalid operand types for +' diagnostic
        Map<Integer, LineMatch> invalidPlusMatches = findMatchesByLine(lower, "invalid operand types for +");
        if (invalidPlusMatches.size() < 1) {
            StringBuilder debug = new StringBuilder("Expected at least one 'invalid operand types for +' diagnostic but found none:\n");
            debug.append("Full stderr:\n").append(errors);
            fail(debug.toString());
        }
    }

    // Helper to count simple substring occurrences (non-overlapping)
    private static int countOccurrences(String haystack, String needle) {
        if (needle.length() == 0) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    // Return a map of lineNumber (1-based) -> LineMatch for lines that contain the needle
    private static Map<Integer, LineMatch> findMatchesByLine(String haystack, String needle) {
        Map<Integer, LineMatch> result = new LinkedHashMap<>();
        if (needle.length() == 0) return result;
        String[] lines = haystack.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int c = countOccurrences(lines[i], needle);
            if (c > 0) {
                result.put(i + 1, new LineMatch(lines[i], c));
            }
        }
        return result;
    }

    // Helper container for per-line matches
    private static class LineMatch {
        final String lineText;
        final int count;

        LineMatch(String lineText, int count) {
            this.lineText = lineText;
            this.count = count;
        }

        @Override
        public String toString() {
            return "{count=" + count + ", text=" + lineText + "}";
        }
    }
}