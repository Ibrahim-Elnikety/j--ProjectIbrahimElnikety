package jminusminus;

/**
 * A wrapper expression node that allows a TypeName to be used
 * as a JExpression in the AST (for static field/method access).
 */
class JTypeExpression extends JExpression {
    private TypeName typeName;

    public JTypeExpression(int line, TypeName typeName) {
        super(line);
        this.typeName = typeName;
    }

    @Override
    public JExpression analyze(Context context) {
        type = typeName.resolve(context);
        return this;
    }

    @Override
    public void codegen(CLEmitter output) {
        // Nothing to emit directly; used only as a target for field/method selections
    }

    @Override
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JTypeExpression:" + line, e);
        e.addAttribute("typeName", typeName.toString());
    }
}