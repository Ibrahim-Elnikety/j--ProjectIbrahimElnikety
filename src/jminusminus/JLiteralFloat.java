package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a float literal.
 */
class JLiteralFloat extends JExpression {
    // String representation of the literal.
    private String text;

    /**
     * Constructs an AST node for a float literal given its line number and string representation.
     *
     * @param line line in which the literal occurs in the source file.
     * @param text string representation of the literal.
     */
    public JLiteralFloat(int line, String text) {
        super(line);
        this.text = text;
    }

    /**
     * Returns the literal as a float.
     *
     * @return the literal as a float.
     */
    public float toFloat() {
        // Allow literals like "1.23f" or "1.23F"
        String s = text;
        char last = s.charAt(s.length() - 1);
        if (last == 'f' || last == 'F') {
            s = s.substring(0, s.length() - 1);
        }
        return Float.parseFloat(s);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        type = Type.FLOAT;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        float v = toFloat();
        if (v == 0.0f) {
            output.addNoArgInstruction(FCONST_0);
        } else if (v == 1.0f) {
            output.addNoArgInstruction(FCONST_1);
        } else if (v == 2.0f) {
            output.addNoArgInstruction(FCONST_2);
        } else {
            /*
             * Use the general LDC mechanism. Pass a java.lang.Float so the emitter emits
             * the appropriate LDC for float constants.
             */
            output.addLDCInstruction(Float.valueOf(v));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JLiteralFloat:" + line, e);
        e.addAttribute("type", type == null ? "" : type.toString());
        e.addAttribute("value", text);
    }
}