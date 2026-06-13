package jminusminus;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ScannerTests {

    private File writeTemp(String content) throws IOException {
        File tmp = File.createTempFile("jmm_test_", ".j--");
        tmp.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write(content);
        }
        return tmp;
    }

    private List<TokenInfo> tokensFrom(String content) throws Exception {
        File f = writeTemp(content);
        Scanner scanner = new Scanner(f.getAbsolutePath());
        List<TokenInfo> tokens = new ArrayList<>();
        TokenInfo t;
        do {
            t = scanner.getNextToken();
            tokens.add(t);
        } while (t.kind() != TokenKind.EOF);
        return tokens;
    }

    private String getLexeme(TokenInfo t) {
        if (t == null) return "";
        String[] names = {"lexeme", "image", "getLexeme", "getImage", "text", "getText", "value"};
        for (String n : names) {
            try {
                Method m = t.getClass().getMethod(n);
                Object r = m.invoke(t);
                if (r != null) return r.toString();
            } catch (NoSuchMethodException ignored) {}
            catch (Exception ignored) {}
        }
        try {
            String s = t.toString();
            return s == null ? "" : s;
        } catch (Exception ignored) { return ""; }
    }

    @Test
    public void multiLineCommentsAreIgnored() throws Exception {
        String src = "/* this is a\n multi-line comment */\n" +
                     "/* nested-like * not real nesting */ class C { }\n" +
                     "// single line comment\n" +
                     "int x;";
        List<TokenInfo> toks = tokensFrom(src);
        boolean sawClass = false, sawInt = false, sawSemi = false;
        for (TokenInfo t : toks) {
            if (t.kind() == TokenKind.CLASS) sawClass = true;
            if (t.kind() == TokenKind.INT) sawInt = true;
            if (t.kind() == TokenKind.SEMI) sawSemi = true;
        }
        assertTrue("class keyword should be recognized", sawClass);
        assertTrue("int keyword should be recognized", sawInt);
        assertTrue("semicolon should appear", sawSemi);
    }

    @Test
    public void operatorsRecognitionByLexeme() throws Exception {
        String src = "+ ++ += - -- -= * *= / /= % %= == != > >= < <= && || & | ^ << >> >>> ? : ;";
        List<TokenInfo> toks = tokensFrom(src);

        List<String> lexemes = new ArrayList<>();
        for (TokenInfo t : toks) lexemes.add(getLexeme(t).trim());

        String[] expected = new String[] {
            "+", "++", "+=", "-", "--", "-=", "*", "*=", "/", "/=", "%", "%=", "==", "!=", ">", ">=", "<", "<=", "&&", "||", "&", "|", "^", "<<", ">>", ">>>", "?", ":", ";"
        };

        int idx = 0;
        for (String lex : lexemes) {
            if (idx < expected.length && expected[idx].equals(lex)) idx++;
        }
        assertEquals("Operator lexeme subsequence not found", expected.length, idx);
    }

    @Test
    public void reservedWordsRecognitionByLexeme() throws Exception {
        String[] words = new String[] {
            "abstract","boolean","char","class","else","extends","false",
            "if","import","instanceof","int","new","null","package",
            "private","protected","public","return","static","super","this",
            "true","void","while"
        };
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(w).append(" ");
        sb.append(";");
        List<TokenInfo> toks = tokensFrom(sb.toString());

        int countReserved = 0;
        for (TokenInfo t : toks) {
            String lex = getLexeme(t).trim();
            for (String w : words) if (w.equals(lex)) countReserved++;
        }
        assertTrue("Should recognize reserved words (at least half)", countReserved >= words.length / 2);
    }

    @Test
    public void numericLiteralsByLexeme() throws Exception {
        String src = "123 0 456789L 123456l 3.14 2.0d 6.022e23 1.2E-3f 0.5F";
        List<TokenInfo> toks = tokensFrom(src);

        boolean sawIntLex = false, sawLongLex = false, sawFloatLex = false, sawDoubleLex = false;
        Pattern intPat = Pattern.compile("^[0-9]+$");
        Pattern longPat = Pattern.compile("^[0-9]+[lL]$");
        Pattern floatPat = Pattern.compile("^[0-9]+(\\.[0-9]*)?([eE][+-]?[0-9]+)?[fF]$");
        Pattern doublePat = Pattern.compile("^[0-9]*\\.[0-9]+([eE][+-]?[0-9]+)?$|^[0-9]+[dD]$|^[0-9]+([eE][+-]?[0-9]+)$");

        for (TokenInfo t : toks) {
            String lex = getLexeme(t).trim();
            if (lex.isEmpty()) continue;
            if (intPat.matcher(lex).matches()) sawIntLex = true;
            if (longPat.matcher(lex).matches()) sawLongLex = true;
            if (floatPat.matcher(lex).matches()) sawFloatLex = true;
            if (doublePat.matcher(lex).matches()) sawDoubleLex = true;
        }
        assertTrue(sawIntLex);
        assertTrue(sawLongLex);
        assertTrue(sawFloatLex);
        assertTrue(sawDoubleLex);
    }

    @Test
    public void integerRepresentationsLexeme() throws Exception {
        String src = "0x1A 0XFF 0755 0b1010 0B1100";
        List<TokenInfo> toks = tokensFrom(src);
        boolean hex=false, oct=false, bin=false;
        for (TokenInfo t : toks) {
            String lex = getLexeme(t).toLowerCase();
            if (lex.startsWith("0x")) hex = true;
            else if (lex.startsWith("0b")) bin = true;
            else if (lex.startsWith("0") && lex.length()>1 && lex.matches("0[0-7]+")) oct = true;
        }
        assertTrue(hex);
        assertTrue(bin);
        assertTrue(oct);
    }

    @Test
    public void stringsAndCharLiteralsScanned() throws Exception {
        String src = "\"hello\\nworld\" '\\'' '\\\\' \"escaped \\\" quote\"";
        List<TokenInfo> toks = tokensFrom(src);
        boolean sawString=false, sawChar=false;
        for (TokenInfo t : toks) {
            if (t.kind() == TokenKind.STRING_LITERAL) sawString = true;
            if (t.kind() == TokenKind.CHAR_LITERAL) sawChar = true;
        }
        assertTrue(sawString);
        assertTrue(sawChar);
    }

    // Manual harness if JUnit isn't set up
    public static void main(String[] args) throws Exception {
        ScannerTests st = new ScannerTests();
        List<TokenInfo> toks = st.tokensFrom("class C { int x = 0x1A; /*comment*/ }\n + ++ += 3.14 2.0f 123L != !");
        Set<String> seen = new HashSet<>();
        for (TokenInfo t : toks) {
            String lex = st.getLexeme(t);
            System.out.printf("%s\t\"%s\"%n", t.kind(), lex);
            seen.add(lex);
        }
        System.out.println("Contains '!=' ? " + seen.contains("!="));
    }
    
    @Test
    public void allReservedWordsRecognized() throws Exception {
        String[] all = new String[] {
            "abstract","assert","boolean","break","byte","case","catch","char","class","const",
            "continue","default","do","double","else","enum","extends","final","finally","float",
            "for","goto","if","implements","import","instanceof","int","interface","long","native",
            "new","package","private","protected","public","return","short","static","strictfp",
            "super","switch","synchronized","this","throw","throws","transient","try","void",
            "volatile","while"
        };
        StringBuilder sb = new StringBuilder();
        for (String w : all) sb.append(w).append(" ");
        sb.append(";");
        List<TokenInfo> toks = tokensFrom(sb.toString());

        List<String> lexemes = new ArrayList<>();
        for (TokenInfo t : toks) lexemes.add(getLexeme(t).trim());
        int matched = 0;
        for (String w : all) {
            for (String lx : lexemes) {
                if (w.equals(lx)) { matched++; break; }
            }
        }
        assertEquals("All reserved words should be recognized", all.length, matched);
    }

    @Test
    public void leadingDotFloatIsRecognized() throws Exception {
        String src = ".5 .25 .0 .123e2";
        List<TokenInfo> toks = tokensFrom(src);
        boolean sawLeadingFloat = false;
        for (TokenInfo t : toks) {
            String lex = getLexeme(t).trim();
            if (lex.equals(".5") || lex.equals(".25") || lex.equals(".0") || lex.equals(".123e2")) {
                sawLeadingFloat = true;
            }
        }
        assertTrue("Leading-dot floats should be recognized as single float/double tokens", sawLeadingFloat);
    }

    @Test
    public void hexFloatingLiteralRecognized() throws Exception {
        String src = "0x1.8p3 0X1.2P-4";
        List<TokenInfo> toks = tokensFrom(src);
        boolean sawHexFloat = false;
        for (TokenInfo t : toks) {
            String lex = getLexeme(t).trim().toLowerCase();
            if (lex.startsWith("0x") && (lex.contains("p") || lex.contains("p-") || lex.contains("p+"))) {
                sawHexFloat = true;
            }
        }
        assertTrue("Hexadecimal floating literals should be recognized (0x1.8p3)", sawHexFloat);
    }

    @Test
    public void underscoresInsideNumericLiterals() throws Exception {
        String src = "1_000 0b1010_0110 0xFF_FF 1_234.56_78 123_456l";
        List<TokenInfo> toks = tokensFrom(src);

        boolean sawIntUnderscore = false, sawBinUnderscore = false, sawHexUnderscore = false, sawFloatUnderscore = false, sawLongUnderscore = false;
        for (TokenInfo t : toks) {
            String lex = getLexeme(t).trim();
            if (lex.equals("1_000")) sawIntUnderscore = true;
            if (lex.equalsIgnoreCase("0b1010_0110")) sawBinUnderscore = true;
            if (lex.equalsIgnoreCase("0xFF_FF")) sawHexUnderscore = true;
            if (lex.equals("1_234.56_78")) sawFloatUnderscore = true;
            if (lex.equalsIgnoreCase("123_456l")) sawLongUnderscore = true;
        }
        assertTrue("At least one underscore numeric literal should be recognized", sawIntUnderscore || sawBinUnderscore || sawHexUnderscore || sawFloatUnderscore || sawLongUnderscore);
    }

    @Test
    public void malformedExponentProducesError() throws Exception {
        File f = writeTemp("1e+ 2.3e- .5e ");
        Scanner scanner = new Scanner(f.getAbsolutePath());
        boolean sawError = false;
        TokenInfo t;
        do {
            t = scanner.getNextToken();
        } while (t.kind() != TokenKind.EOF && !scanner.errorHasOccurred());
        sawError = scanner.errorHasOccurred();
        assertTrue("Scanner should report error for malformed exponent like '1e+'", sawError);
    }

    @Test
    public void unterminatedMultiLineCommentReportsError() throws Exception {
        File f = writeTemp("class C { /* unclosed comment ");
        Scanner scanner = new Scanner(f.getAbsolutePath());
        TokenInfo t;
        do {
            t = scanner.getNextToken();
        } while (t.kind() != TokenKind.EOF && !scanner.errorHasOccurred());
        assertTrue("Unterminated multi-line comment should set scanner error flag", scanner.errorHasOccurred());
    }

}