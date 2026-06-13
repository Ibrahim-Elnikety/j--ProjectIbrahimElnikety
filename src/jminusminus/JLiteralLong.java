package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a long literal.
 */
class JLiteralLong extends JExpression {
    // String representation of the literal.
    private String text;

    /**
     * Constructs an AST node for a long literal given its line number and string representation.
     *
     * @param line line in which the literal occurs in the source file.
     * @param text string representation of the literal.
     */
    public JLiteralLong(int line, String text) {
        super(line);
        this.text = text;
    }

    /**
     * Returns the literal as a long.
     *
     * @return the literal as a long.
     */
    public long toLong() {
        return Long.parseLong(text.substring(0, text.length() - 1));
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        type = Type.LONG;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        long v = toLong();
        if (v == 0L) {
            output.addNoArgInstruction(LCONST_0);
        } else if (v == 1L) {
            output.addNoArgInstruction(LCONST_1);
        } else {
            /*
             * Use the general LDC/LDC2 mechanism. The CLEmitter in this project supports
             * addLDCInstruction for constants (used for ints elsewhere as addLDCInstruction(i)).
             * Pass a java.lang.Long so the emitter emits the appropriate LDC2_W for long/double.
             */
            output.addLDCInstruction(Long.valueOf(v));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JLiteralLong:" + line, e);
        e.addAttribute("type", type == null ? "" : type.toString());
        e.addAttribute("value", text);
    }
}