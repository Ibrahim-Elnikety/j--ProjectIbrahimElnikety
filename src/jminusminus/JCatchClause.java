package jminusminus;

/**
 * A holder for a catch clause used by JTryStatement and the parser.
 * Minimal implementation sufficient for parsing tests; extend with analyze/codegen as needed.
 */
public class JCatchClause {
    /** Exception type as written (resolution happens during analysis). */
    public final TypeName exceptionType;

    /** Exception variable name. */
    public final String exceptionName;

    /** Catch block body. */
    public final JBlock block;

    public JCatchClause(TypeName exceptionType, String exceptionName, JBlock block) {
        this.exceptionType = exceptionType;
        this.exceptionName = exceptionName;
        this.block = block;
    }

    /** Optional hook for later analysis; keep for parity with other AST pieces. */
    public JCatchClause analyze(Context context) {
        // Real implementation should open a new scope, bind the exceptionName to exceptionType, analyze block.
        return this;
    }

    public void toJSON(JSONElement json) {
        // Create a node for this catch clause and add the block as its child.
        JSONElement e = new JSONElement();
        json.addChild("JCatchClause:" + block.line(), e);

        // Add the catch block (use an element child since addChild expects JSONElement)
        JSONElement b = new JSONElement();
        e.addChild("block", b);
        block.toJSON(b);

    }
}