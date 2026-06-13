// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * An AST node for a throw-statement.
 */
class JThrowStatement extends JStatement {
    // The thrown exception.
    private JExpression expr;

    /**
     * Constructs an AST node for a throw-statement.
     *
     * @param line line in which the throw-statement appears in the source file.
     * @param expr the returned expression.
     */
    public JThrowStatement(int line, JExpression expr) {
        super(line);
        this.expr = expr;
    }

    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
        // Analyze the thrown expression first.
        expr = (JExpression) expr.analyze(context);

        // The thrown expression must be a reference type (subclass of Throwable in Java).
        if (expr.type().isPrimitive()) {
            JAST.compilationUnit.reportSemanticError(line(),
                    "cannot throw a primitive type");
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // Evaluate the exception object and throw it.
        expr.codegen(output);
        output.addNoArgInstruction(ATHROW);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JThrowStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Expression", e1);
        expr.toJSON(e1);
    }
}
