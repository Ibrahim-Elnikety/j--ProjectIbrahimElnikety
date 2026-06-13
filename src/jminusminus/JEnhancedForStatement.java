package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for an enhanced for-statement (for (Type id : expression) body).
 */
class JEnhancedForStatement extends JStatement {
    private Type varType;
    private String varName;
    private JExpression iterable;
    private JStatement body;

    public JEnhancedForStatement(int line, Type varType, String varName,
                                 JExpression iterable, JStatement body) {
        super(line);
        this.varType = varType;
        this.varName = varName;
        this.iterable = iterable;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
        // Evaluate iterable in the surrounding context (side-effects happen before the loop).
        iterable = (JExpression) iterable.analyze(context);

        // Create a new local context for the loop so the loop variable is scoped to the body.
        LocalContext loopContext = new LocalContext(context);

        // Allocate an offset for the loop variable and register it in loopContext.
        int offset = loopContext.nextOffset();
        LocalVariableDefn defn = new LocalVariableDefn(varType, offset);
        loopContext.addEntry(line(), varName, defn);

        // Consider the loop variable initialized for use in the body.
        defn.initialize();

        // Analyze the body in the loop context so it can see the loop variable.
        body = (JStatement) body.analyze(loopContext);

        /*
         * TODO (future): check iterable.type() and set/validate the element type:
         * - if iterable.type() is an array type, ensure varType matches component type
         * - if iterable.type() is a reference type implementing Iterable, derive element type
         * - otherwise report a semantic error
         */

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        /*
         * Minimal placeholder codegen:
         * Emit the iterable expression so its side-effects run, then pop the result and emit the body.
         *
         * This keeps semantics (evaluation order) correct and the stack balanced. Replace with a
         * full implementation (array vs Iterable handling using an index or Iterator) when ready.
         */
        iterable.codegen(output);
        output.addNoArgInstruction(POP); // discard iterable value for now
        body.codegen(output);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JEnhancedForStatement:" + line, e);
        e.addAttribute("varType", varType == null ? "" : varType.toString());
        e.addAttribute("varName", varName);
        JSONElement it = new JSONElement();
        e.addChild("Iterable", it);
        iterable.toJSON(it);
        JSONElement b = new JSONElement();
        e.addChild("Body", b);
        body.toJSON(b);
    }
}