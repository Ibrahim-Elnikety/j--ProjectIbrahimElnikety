// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a for-statement.
 */
class JForStatement extends JStatement {
    // Initialization.
    private ArrayList<JStatement> init;

    // Test expression
    private JExpression condition;

    // Update.
    private ArrayList<JStatement> update;

    // The body.
    private JStatement body;

    /**
     * Constructs an AST node for a for-statement.
     *
     * @param line      line in which the for-statement occurs in the source file.
     * @param init      the initialization.
     * @param condition the test expression.
     * @param update    the update.
     * @param body      the body.
     */
    public JForStatement(int line, ArrayList<JStatement> init, JExpression condition,
                         ArrayList<JStatement> update, JStatement body) {
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    /**
     * {@inheritDoc}
     */
    public JForStatement analyze(Context context) {
        // Create a nested local context for the loop body and any init-declared locals.
        Context loopContext = new LocalContext(context);

        // Analyze init statements (if any) in loopContext
        if (init != null) {
            ArrayList<JStatement> analyzedInit = new ArrayList<JStatement>();
            for (JStatement stmt : init) {
                analyzedInit.add((JStatement) stmt.analyze(loopContext));
            }
            init = analyzedInit;
        }

        // Analyze condition (if present) and require boolean
        if (condition != null) {
            condition = (JExpression) condition.analyze(loopContext);
            condition.type().mustMatchExpected(line(), Type.BOOLEAN);
        }

        // Analyze body in the same loopContext
        if (body != null) {
            body = (JStatement) body.analyze(loopContext);
        }

        // Analyze update statements (if any) in loopContext
        if (update != null) {
            ArrayList<JStatement> analyzedUpdate = new ArrayList<JStatement>();
            for (JStatement stmt : update) {
                analyzedUpdate.add((JStatement) stmt.analyze(loopContext));
            }
            update = analyzedUpdate;
        }

        return this;
    }

    public void codegen(CLEmitter output) {
        String testLabel = output.createLabel();
        String bodyLabel = output.createLabel();
        String endLabel = output.createLabel();

        // Emit init
        if (init != null) {
            for (JStatement stmt : init) {
                stmt.codegen(output);
            }
        }

        // Jump to test
        output.addBranchInstruction(GOTO, testLabel);

        // Body
        output.addLabel(bodyLabel);
        if (body != null) {
            body.codegen(output);
        }

        // Updates
        if (update != null) {
            for (JStatement stmt : update) {
                stmt.codegen(output);
            }
        }

        // Test
        output.addLabel(testLabel);
        if (condition != null) {
            condition.codegen(output);
            // if condition is false (0) jump to end
            output.addBranchInstruction(IFEQ, endLabel);
            // otherwise fall through to body
            output.addBranchInstruction(GOTO, bodyLabel);
        } else {
            // No condition means infinite loop: jump to body
            output.addBranchInstruction(GOTO, bodyLabel);
        }

        output.addLabel(endLabel);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JForStatement:" + line, e);
        if (init != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Init", e1);
            for (JStatement stmt : init) {
                stmt.toJSON(e1);
            }
        }
        if (condition != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Condition", e1);
            condition.toJSON(e1);
        }
        if (update != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Update", e1);
            for (JStatement stmt : update) {
                stmt.toJSON(e1);
            }
        }
        if (body != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Body", e1);
            body.toJSON(e1);
        }
    }
}
