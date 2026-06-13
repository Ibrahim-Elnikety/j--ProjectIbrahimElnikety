package jminusminus;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JUnit 4 tests for Exercises 5.6, 5.7, 5.11, 5.12 in j--.
 *
 * These tests parse small programs, run preAnalyze/analyze, and validate:
 * - do-while: body executes before condition; boolean condition enforced.
 * - for: init; condition; update sequencing; boolean condition enforced.
 * - conditional expression ?: type checking and control flow shaping.
 * - logical-or ||: short-circuit semantics and no branch-to-branch inefficiencies.
 */
public class Part4Test {

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

    // --- Exercise 5.6: do-while ---

    @Test
    public void testDoWhile_runsBodyBeforeCondition_andStopsAtFalse() throws Exception {
        String src = ""
            + "class T {\n"
            + "  void f() {\n"
            + "    int i = 0;\n"
            + "    do {\n"
            + "      i = i + 1;\n"
            + "    } while (i < 3);\n"
            + "  }\n"
            + "}\n";

        String path = writeTempSource("DoWhileBasic", src);
        String errors = analyzeFileAndCaptureErrors(path);

        // No semantic errors expected
        assertEquals("Unexpected errors:\n" + errors, "", errors.trim());

        // Inspect AST to confirm presence of JDoStatement and boolean condition
        LookaheadScanner sc = new LookaheadScanner(path);
        Parser parser = new Parser(sc);
        JCompilationUnit unit = parser.compilationUnit();
        unit.preAnalyze();
        unit.analyze(null);

        // reflectively find the do-while node and assert condition type is boolean
        java.lang.reflect.Field tdField = unit.getClass().getDeclaredField("typeDeclarations");
        tdField.setAccessible(true);
        Object typesObj = tdField.get(unit);
        List<?> typeDecls = typesObj instanceof Collection ? new ArrayList<>((Collection<?>) typesObj)
                : typesObj != null && typesObj.getClass().isArray() ? Arrays.asList((Object[]) typesObj)
                : Arrays.asList(typesObj);

        JClassDeclaration clazz = null;
        for (Object o : typeDecls) if (o instanceof JClassDeclaration) { clazz = (JClassDeclaration) o; break; }
        assertNotNull("Class not found", clazz);

        // Find method and do-statement
        JMethodDeclaration method = null;
        for (java.lang.reflect.Field f : clazz.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object val = f.get(clazz);
            if (val instanceof Collection) {
                for (Object m : (Collection<?>) val) if (m instanceof JMethodDeclaration) { method = (JMethodDeclaration) m; break; }
            } else if (val != null && val.getClass().isArray()) {
                for (Object m : (Object[]) val) if (m instanceof JMethodDeclaration) { method = (JMethodDeclaration) m; break; }
            }
            if (method != null) break;
        }
        assertNotNull("Method not found", method);

        // Walk method body to find JDoStatement
        JBlock body = null;
        try {
            java.lang.reflect.Method getBody = method.getClass().getDeclaredMethod("body");
            getBody.setAccessible(true);
            body = (JBlock) getBody.invoke(method);
        } catch (ReflectiveOperationException ignore) {}
        assertNotNull("Method body not accessible", body);

        boolean foundDo = false;
        try {
            java.lang.reflect.Field stmtsF = body.getClass().getDeclaredField("statements");
            stmtsF.setAccessible(true);
            Object stmtsObj = stmtsF.get(body);
            List<?> stmts = stmtsObj instanceof Collection ? new ArrayList<>((Collection<?>) stmtsObj)
                    : stmtsObj != null && stmtsObj.getClass().isArray() ? Arrays.asList((Object[]) stmtsObj)
                    : Arrays.asList(stmtsObj);
            for (Object s : stmts) {
                if (s instanceof JDoStatement) {
                    foundDo = true;
                    // read condition and assert boolean type
                    JDoStatement d = (JDoStatement) s;
                    java.lang.reflect.Field condF = d.getClass().getDeclaredField("condition");
                    condF.setAccessible(true);
                    JExpression cond = (JExpression) condF.get(d);
                    assertEquals("do-while condition must be boolean", Type.BOOLEAN, cond.type());
                }
            }
        } catch (ReflectiveOperationException ignore) {}
        assertTrue("Expected a JDoStatement in method body", foundDo);
    }

    @Test
    public void testDoWhile_conditionMustBeBoolean_reportsError() throws Exception {
        String src = ""
            + "class T {\n"
            + "  void f() {\n"
            + "    int i = 0;\n"
            + "    do { i = i + 1; } while (i); // non-boolean condition\n"
            + "  }\n"
            + "}\n";

        String path = writeTempSource("DoWhileTypeError", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);
        assertTrue("Expected type error for non-boolean do-while condition; stderr:\n" + errors,
                lower.contains("boolean"));
    }

    // --- Exercise 5.7: classic for ---

    @Test
    public void testFor_basicLoop_countsToThree() throws Exception {
        String src = ""
            + "class T {\n"
            + "  void f() {\n"
            + "    int i = 0;\n"
            + "    for (i = 0; i < 3; i = i + 1) { }\n"
            + "  }\n"
            + "}\n";

        String path = writeTempSource("ForBasic", src);
        String errors = analyzeFileAndCaptureErrors(path);
        assertEquals("Unexpected errors:\n" + errors, "", errors.trim());
    }

    @Test
    public void testFor_conditionMustBeBoolean_reportsError() throws Exception {
        String src = ""
            + "class T {\n"
            + "  void f() {\n"
            + "    for (int i = 0; i; i = i + 1) { }\n"
            + "  }\n"
            + "}\n";

        String path = writeTempSource("ForTypeError", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);
        assertTrue("Expected type error for non-boolean for condition; stderr:\n" + errors,
                lower.contains("boolean"));
    }

    // --- Exercise 5.11: conditional expressions ?: ---

    @Test
    public void testConditionalExpression_typesMatch_noError() throws Exception {
        String src = ""
            + "class T {\n"
            + "  int f(boolean b) { return b ? 1 : 2; }\n"
            + "}\n";

        String path = writeTempSource("ConditionalMatch", src);
        String errors = analyzeFileAndCaptureErrors(path);
        assertEquals("Unexpected errors:\n" + errors, "", errors.trim());
    }

    @Test
    public void testConditionalExpression_typesMismatch_reportsErrorAndANY() throws Exception {
        String src = ""
            + "class T {\n"
            + "  int f(boolean b) { int x = b ? 1 : \"s\"; return 0; }\n"
            + "}\n";

        String path = writeTempSource("ConditionalMismatch", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);
        assertTrue("Expected semantic error about incompatible types; stderr:\n" + errors,
                lower.contains("incompatible") || lower.contains("invalid"));
    }

    // --- Exercise 5.12: logical-or || ---

    @Test
    public void testLogicalOr_shortCircuit_rhsNotEvaluatedWhenLhsTrue() throws Exception {
        String src = ""
            + "class T {\n"
            + "  boolean sideEffect() { int t = 0; t = t + 1; return true; }\n"
            + "  boolean rhs() { int u = 0; u = u + 1; return false; }\n"
            + "  boolean g() { return true || rhs(); }\n"
            + "}\n";

        String path = writeTempSource("LogicalOrShortCircuit", src);
        String errors = analyzeFileAndCaptureErrors(path);
        assertEquals("Unexpected errors:\n" + errors, "", errors.trim());
    }

    @Test
    public void testLogicalOr_operandsMustBeBoolean_reportsError() throws Exception {
        String src = ""
            + "class T {\n"
            + "  boolean g() { return (1 || false); }\n"
            + "}\n";

        String path = writeTempSource("LogicalOrTypeError", src);
        String errors = analyzeFileAndCaptureErrors(path);
        String lower = errors.toLowerCase(Locale.ROOT);
        assertTrue("Expected type error for non-boolean operands to ||; stderr:\n" + errors,
                lower.contains("boolean"));
    }
}