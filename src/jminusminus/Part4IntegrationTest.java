package jminusminus;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for Exercises 5.6–5.21.
 * These compile small j-- programs to .class files and run them on the JVM,
 * capturing their output.
 */
public class Part4IntegrationTest {

    // Utility: write source to a temp file
    private String writeTempSource(String baseName, String source) throws IOException {
        File tmp = File.createTempFile(baseName, ".java");
        tmp.deleteOnExit();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            w.write(source);
        }
        return tmp.getAbsolutePath();
    }

    // Utility: compile with j-- and emit .class file
    private void compileWithJMinusMinus(String filePath) throws Exception {
        LookaheadScanner scanner = new LookaheadScanner(filePath);
        Parser parser = new Parser(scanner);
        JCompilationUnit unit = parser.compilationUnit();
        unit.preAnalyze();
        unit.analyze(null);

        CLEmitter output = new CLEmitter(true); // true = add debug info
        unit.codegen(output);

        // Set destination directory to the source file’s parent folder
        output.destinationDir(new File(filePath).getParent());
        // Now write out the .class file(s)
        output.write();
    }

    // Utility: run a compiled class and capture stdout
    private String runClass(String className, File dir) throws Exception {
        Process proc = new ProcessBuilder("java", className)
                .directory(dir)
                .redirectErrorStream(true)
                .start();
        boolean finished = proc.waitFor(3, TimeUnit.SECONDS);
        assertTrue("Process timed out", finished);
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return output.trim().replace("\r\n", "\n").replace("\r", "");
    }

    // --- Exercise 5.6: do-while ---
    @Test
    public void testDoWhile_runtimeOutput() throws Exception {
        String src = "class TestDoWhile { "
                   + "public static void main(String[] args) { "
                   + "int i=0; do { System.out.println(i); i++; } while(i<3); } }";
        String path = writeTempSource("TestDoWhile", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestDoWhile", new File(path).getParentFile());
        assertEquals("0\n1\n2", output);
    }

    // --- Exercise 5.7: for ---
    @Test
    public void testFor_runtimeOutput() throws Exception {
        String src = "class TestFor { "
                   + "public static void main(String[] args) { "
                   + "for(int i=0;i<3;i++){ System.out.println(i); } } }";
        String path = writeTempSource("TestFor", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestFor", new File(path).getParentFile());
        assertEquals("0\n1\n2", output);
    }

    // --- Exercise 5.11: conditional expression ---
    @Test
    public void testConditionalExpression_trueBranch() throws Exception {
        String src = "class TestCondTrue { "
                   + "public static void main(String[] args) { "
                   + "boolean b=true; int x=b?1:2; System.out.println(x); } }";
        String path = writeTempSource("TestCondTrue", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestCondTrue", new File(path).getParentFile());
        assertEquals("1", output);
    }

    @Test
    public void testConditionalExpression_falseBranch() throws Exception {
        String src = "class TestCondFalse { "
                   + "public static void main(String[] args) { "
                   + "boolean b=false; int x=b?1:2; System.out.println(x); } }";
        String path = writeTempSource("TestCondFalse", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestCondFalse", new File(path).getParentFile());
        assertEquals("2", output);
    }

    // --- Exercise 5.12: logical-or ---
    @Test
    public void testLogicalOr_runtimeOutput() throws Exception {
        String src = "class TestOr { "
                   + "public static void main(String[] args) { "
                   + "System.out.println(true || false); } }";
        String path = writeTempSource("TestOr", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestOr", new File(path).getParentFile());
        assertEquals("true", output);
    }

    @Test
    public void testLogicalOr_shortCircuit() throws Exception {
        String src = "class TestOrShort { "
                   + "public static void main(String[] args) { "
                   + "System.out.println(true || (1/0==0)); } }";
        String path = writeTempSource("TestOrShort", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestOrShort", new File(path).getParentFile());
        assertEquals("true", output); // proves RHS never evaluated
    }
    
    @Test
    public void testThrow_runtimeOutput() throws Exception {
        String src = "class TestThrow { "
                   + "public static void main(String[] args) { "
                   + "throw new RuntimeException(\"Boom!\"); } }";
        String path = writeTempSource("TestThrow", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestThrow", new File(path).getParentFile());
        assertTrue(output.contains("RuntimeException"));
        assertTrue(output.contains("Boom!"));
    }
    
    @Test
    public void testLong_runtimeOutput() throws Exception {
        String src = "class TestLong { "
                   + "public static void main(String[] args) { "
                   + "long x=10000000000L; long y=2L; long z=x/y; System.out.println(z); } }";
        String path = writeTempSource("TestLong", src);
        compileWithJMinusMinus(path);
        String output = runClass("TestLong", new File(path).getParentFile());
        assertEquals("5000000000", output);
    }
}