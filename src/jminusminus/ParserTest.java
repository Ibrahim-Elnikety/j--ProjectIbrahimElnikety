package jminusminus;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * ParserTest — smoke tests for parser features added in exercises 3.22–3.29,
 * plus a token-dump debug test that prints the full token stream for an input.
 */
public class ParserTest {

    // Helper: create a Parser for given source by writing a temp .java file and giving its path
    // (LookaheadScanner in this project has a constructor that accepts a file path).
    private Parser parserFor(String src) throws Exception {
        Path p = Files.createTempFile("ParserTest", ".java");
        Files.write(p, src.getBytes(StandardCharsets.UTF_8));
        // Debug: print what we actually wrote so you can inspect the temp file if something goes wrong
        System.out.println("WROTE: " + p.toString());
        System.out.println(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        LookaheadScanner scanner = new LookaheadScanner(p.toString());
        return new Parser(scanner);
    }

    @Test
    public void parsesLongLiteral() throws Exception {
        String src = ""
                + "class A {\n"
                + "  void m() {\n"
                + "    long x = 1234567890123L;\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for long literal", p.errorHasOccurred());
    }

    @Test
    public void parsesAdditionalOperators() throws Exception {
        String src = ""
                + "class A {\n"
                + "  int m(int a, int b) {\n"
                + "    int r = (a ^ b) | (a << 2) >> 1 >>> 1;\n"
                + "    a ^= 5; a |= 3; a <<= 1; a >>= 1; a >>>= 1;\n"
                + "    return r;\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for added operators", p.errorHasOccurred());
    }

    @Test
    public void parsesConditionalExpression() throws Exception {
        String src = ""
                + "class A {\n"
                + "  int m(int a, int b) {\n"
                + "    int x = (a > b) ? a : b;\n"
                + "    return x;\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for conditional expression", p.errorHasOccurred());
    }

    @Test
    public void parsesBasicAndEnhancedForStatements() throws Exception {
        String src = ""
                + "class A {\n"
                + "  void basic() {\n"
                + "    for (int i = 0; i < 10; i = i + 1) { ; }\n"
                + "  }\n"
                + "  void enhanced(int[] arr) {\n"
                + "    for (int v : arr) { ; }\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for for-statements", p.errorHasOccurred());
    }

    @Test
    public void parsesSwitchStatement() throws Exception {
        String src = ""
                + "class A {\n"
                + "  void s(int x) {\n"
                + "    switch (x) {\n"
                + "      case 0: ; break;\n"
                + "      case 1: ; break;\n"
                + "      default: ;\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for switch statement", p.errorHasOccurred());
    }

    @Test
    public void parsesTryCatchFinallyAndThrow() throws Exception {
        String src = ""
                + "class A {\n"
                + "  void t() {\n"
                + "    try {\n"
                + "      if (true) throw new java.lang.Exception();\n"
                + "    } catch (java.lang.Exception e) {\n"
                + "      ;\n"
                + "    } finally {\n"
                + "      ;\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for try/catch/finally or throw", p.errorHasOccurred());
    }

    @Test
    public void parsesThrowsClauseInMethodDeclaration() throws Exception {
        String src = ""
                + "class A {\n"
                + "  void m() throws java.io.IOException, java.lang.InterruptedException {\n"
                + "    ;\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error for throws clause in method declaration", p.errorHasOccurred());
    }

    @Test
    public void dumpTokensForAllFailingSamples() throws Exception {
        String[] sources = new String[] {
            // long literal
            "class A {\n" +
            "  void m() {\n" +
            "    long x = 1234567890123L;\n" +
            "  }\n" +
            "}\n",
            // for (basic + enhanced)
            "class A {\n" +
            "  void basic() {\n" +
            "    for (int i = 0; i < 10; i = i + 1) { ; }\n" +
            "  }\n" +
            "  void enhanced(int[] arr) {\n" +
            "    for (int v : arr) { ; }\n" +
            "  }\n" +
            "}\n",
            // try-catch-finally + throw
            "class A {\n" +
            "  void t() {\n" +
            "    try {\n" +
            "      if (true) throw new java.lang.Exception();\n" +
            "    } catch (java.lang.Exception e) {\n" +
            "      ;\n" +
            "    } finally {\n" +
            "      ;\n" +
            "    }\n" +
            "  }\n" +
            "}\n",
            // combined file that failed
            "class A {\n" +
            "  long l = 1L;\n" +
            "  int f(int a, int[] arr) throws java.lang.Exception {\n" +
            "    int x = (a > 0) ? a : -a;\n" +
            "    for (int i = 0; i < arr.length; i++) { x = x + arr[i]; }\n" +
            "    for (int v : arr) { x = x + v; }\n" +
            "    switch (x) { case 0: break; default: break; }\n" +
            "    try { throw new java.lang.Exception(); } catch (java.lang.Exception e) { } finally { }\n" +
            "    return x;\n" +
            "  }\n" +
            "}\n"
        };

        for (int s = 0; s < sources.length; s++) {
            Path p = Files.createTempFile("ParserTestDump", ".java");
            Files.write(p, sources[s].getBytes(StandardCharsets.UTF_8));
            System.out.println("=== SAMPLE " + (s+1) + " FILE: " + p + " ===");
            System.out.println(sources[s]);
            System.out.println("=== TOKEN DUMP ===");
            Scanner sc = new Scanner(p.toString());
            TokenInfo t;
            int i = 0;
            do {
                t = sc.getNextToken();
                i++;
                String kind;
                String image;
                int line = -1;
                try {
                    kind = String.valueOf(t.kind);
                    image = String.valueOf(t.image);
                    line = t.line();
                } catch (Throwable ex1) {
                    try {
                        kind = String.valueOf(t.kind);
                        image = String.valueOf(t.image);
                        line = t.line;
                    } catch (Throwable ex2) {
                        kind = t.toString();
                        image = "";
                    }
                }
                System.out.printf("%2d: kind=%-20s image=\"%s\" line=%d%n", i, kind, image, line);
            } while (t != null && !"EOF".equals(String.valueOf(t.kind)));
            System.out.println("=== END SAMPLE " + (s+1) + " ===\n");
        }
    }
    
    @Test
    public void parsesAllFeaturesTogether() throws Exception {
        String src = ""
                + "class A {\n"
                + "  long l = 1L;\n"
                + "  int f(int a, int[] arr) throws java.lang.Exception {\n"
                + "    int x = (a > 0) ? a : -a;\n"
                + "    for (int i = 0; i < arr.length; i++) { x = x + arr[i]; }\n"
                + "    for (int v : arr) { x = x + v; }\n"
                + "    switch (x) { case 0: break; default: break; }\n"
                + "    try { throw new java.lang.Exception(); } catch (java.lang.Exception e) { } finally { }\n"
                + "    return x;\n"
                + "  }\n"
                + "}\n";
        Parser p = parserFor(src);
        p.compilationUnit();
        assertFalse("Parser reported an error when parsing all features together", p.errorHasOccurred());
    }

    /**
     * Debug test: write the switch test file and dump the raw tokens produced by Scanner.
     * Run this to reveal exactly what Scanner emits so we can spot mismatches.
     */
    @Test
    public void dumpTokensForSwitch() throws Exception {
        String src = ""
                + "class A {\n"
                + "  void s(int x) {\n"
                + "    switch (x) {\n"
                + "      case 0: ; break;\n"
                + "      case 1: ; break;\n"
                + "      default: ;\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        Path p = Files.createTempFile("ParserTestDump", ".java");
        Files.write(p, src.getBytes(StandardCharsets.UTF_8));
        System.out.println("WROTE: " + p);
        System.out.println("=== FILE CONTENT ===");
        System.out.println(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        System.out.println("=== TOKEN DUMP ===");

        Scanner sc = new Scanner(p.toString());
        TokenInfo t;
        int i = 0;
        do {
            t = sc.getNextToken();
            i++;
            String kind;
            String image;
            int line = -1;
            try {
                kind = String.valueOf(t.kind);
                image = String.valueOf(t.image);
                line = t.line();
            } catch (Throwable ex1) {
                try {
                    kind = String.valueOf(t.kind);
                    image = String.valueOf(t.image);
                    line = t.line;
                } catch (Throwable ex2) {
                    kind = t.toString();
                    image = "";
                }
            }
            System.out.printf("%2d: kind=%-20s image=\"%s\" line=%d%n", i, kind, image, line);
        } while (t != null && !"EOF".equals(String.valueOf(t.kind)));
        System.out.println("=== END TOKENS ===");
    }
}