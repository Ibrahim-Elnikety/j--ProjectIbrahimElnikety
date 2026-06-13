// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

/**
 * An enum of token kinds. Each entry in this enum represents the kind of a token along with its
 * image (string representation).
 */
enum TokenKind {
    // End of file.
    EOF(""),

    // Reserved words.
    ABSTRACT("abstract"), BOOLEAN("boolean"), CHAR("char"), CLASS("class"), ELSE("else"),
    EXTENDS("extends"), IF("if"), IMPORT("import"), INSTANCEOF("instanceof"), INT("int"),
    NEW("new"), PACKAGE("package"), PRIVATE("private"), PROTECTED("protected"),
    PUBLIC("public"), RETURN("return"), STATIC("static"), SUPER("super"), THIS("this"),
    VOID("void"), WHILE("while"),
    
    // New Java reserved words
    BREAK("break"), BYTE("byte"), CASE("case"), CATCH("catch"), CONST("const"),
    CONTINUE("continue"), DEFAULT("default"), DO("do"), DOUBLE("double"), ENUM("enum"),
    FINAL("final"), FINALLY("finally"), FLOAT("float"), FOR("for"), IMPLEMENTS("implements"),
    INTERFACE("interface"), LONG("long"), NATIVE("native"), SHORT("short"), STRICTFP("strictfp"),
    SWITCH("switch"), SYNCHRONIZED("synchronized"), THROW("throw"), THROWS("throws"),
    TRANSIENT("transient"), TRY("try"), VOLATILE("volatile"), ASSERT("assert"), GOTO("goto"), UNDERSCORE("_"),


    // Operators.
    ASSIGN("="), DEC("--"), EQUAL("=="), GT(">"), INC("++"), LAND("&&"), LE("<="), LNOT("!"),
    MINUS("-"), PLUS("+"), PLUS_ASSIGN("+="), STAR("*"),
    
    //----------------------------------------------------------------------
    //TODO New operators
    
    MOD("%"), TILDE("~"), QUESTION("?"), MODASSIGN("%="), DIV("/"), NOTEQUAL("!="), LT("<"), COLON(":"), 
    ARROW("->"), GE(">="), BITOR("|"),  BITOR_ASSIGN("|="), LOR("||"), STARASSIGN("*="),
    DIVASSIGN("/="), BITAND("&"), BITANDASSIGN("&="), BITXOR("^"), BITXORASSIGN("^="), LSHIFT("<<"), 
    LSHIFTASSIGN("<<="), RSHIFT(">>"), RSHIFTASSIGN(">>="), RSHIFTUNSIGNED(">>>"), 
    RSHIFTUNSIGNEDASSIGN(">>>="), MINUS_ASSIGN("-="),
    
    //----------------------------------------------------------------------

    // Separators.
    COMMA(","), DOT("."), LBRACK("["), LCURLY("{"), LPAREN("("), RBRACK("]"), RCURLY("}"),
    RPAREN(")"), SEMI(";"),

    // Identifiers.
    IDENTIFIER("<IDENTIFIER>"),
    
    // New Literals.
    LONG_LITERAL("<LONG_LITERAL>"), FLOAT_LITERAL("<FLOAT_LITERAL>"), DOUBLE_LITERAL("<DOUBLE_LITERAL>"),

    // Literals.
    CHAR_LITERAL("<CHAR_LITERAL>"), FALSE("false"), INT_LITERAL("<INT_LITERAL>"), NULL("null"),
    STRING_LITERAL("<STRING_LITERAL>"), TRUE("true");
	
	

    // The token kind's string representation.
    private String image;

    /**
     * Constructs an instance of TokenKind given its string representation.
     *
     * @param image string representation of the token kind.
     */
    private TokenKind(String image) {
        this.image = image;
    }

    /**
     * Returns the token kind's string representation.
     *
     * @return the token kind's string representation.
     */
    public String tokenRep() {
        if (this == EOF) {
            return "<EOF>";
        }
        if (image.startsWith("<") && image.endsWith(">")) {
            return image;
        }
        return "\"" + image + "\"";
    }

    /**
     * Returns the token kind's image.
     *
     * @return the token kind's image.
     */
    public String image() {
        return image;
    }
}

/**
 * A representation of tokens returned by the Scanner method getNextToken(). A token has a kind
 * identifying what kind of token it is, an image for providing any semantic text, and the line in
 * which it occurred in the source file.
 */
public class TokenInfo {
    // Token kind.
    TokenKind kind;

    // Semantic text (if any). For example, the identifier name when the token kind is IDENTIFIER
    // . For tokens without a semantic text, it is simply its string representation. For example,
    // "+=" when the token kind is PLUS_ASSIGN.
    String image;

    // Line in which the token occurs in the source file.
    int line;

    /**
     * Constructs a TokenInfo object given its kind, the semantic text forming the token, and its
     * line number.
     *
     * @param kind  the token's kind.
     * @param image the semantic text forming the token.
     * @param line  the line in which the token occurs in the source file.
     */
    public TokenInfo(TokenKind kind, String image, int line) {
        this.kind = kind;
        this.image = image;
        this.line = line;
    }

    /**
     * Constructs a TokenInfo object given its kind and its line number. Its image is simply the
     * token kind's string representation.
     *
     * @param kind the token's identifying number.
     * @param line the line in which the token occurs in the source file.
     */
    public TokenInfo(TokenKind kind, int line) {
        this(kind, kind.image(), line);
    }

    /**
     * Returns the token's kind.
     *
     * @return the token's kind.
     */
    public TokenKind kind() {
        return kind;
    }

    /**
     * Returns the line number associated with the token.
     *
     * @return the line number associated with the token.
     */
    public int line() {
        return line;
    }

    /**
     * Returns the token's string representation.
     *
     * @return the token's string representation.
     */
    public String tokenRep() {
        return kind.tokenRep();
    }

    /**
     * Returns the token's image.
     *
     * @return the token's image.
     */
    public String image() {
        return image;
    }
}
