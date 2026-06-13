// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * An AST node for a break-statement.
 */
public class JBreakStatement extends JStatement {
    /**
     * Constructs an AST node for a break-statement.
     *
     * @param line line in which the break-statement occurs in the source file.
     */
    public JBreakStatement(int line) {
        super(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JStatement analyze(Context context) {
        // Nothing to resolve here at this stage; codegen will expect the enclosing
        // loop/switch to provide a break target label. Keep node intact.
        return this;
    }

    @Override
    public void codegen(CLEmitter output) {
        output.addBranchInstruction(GOTO, "BREAK_PLACEHOLDER");
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JBreakStatement:" + line, e);
    }
}
