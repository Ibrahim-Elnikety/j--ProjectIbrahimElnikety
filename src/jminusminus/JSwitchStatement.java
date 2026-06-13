// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a switch-statement.
 */
public class JSwitchStatement extends JStatement {
    // Test expression.
    private JExpression condition;

    // List of switch-statement groups.
    private ArrayList<SwitchStatementGroup> stmtGroup;

    /**
     * Constructs an AST node for a switch-statement.
     *
     * @param line      line in which the switch-statement occurs in the source file.
     * @param condition test expression.
     * @param stmtGroup list of statement groups.
     */
    public JSwitchStatement(int line, JExpression condition,
                            ArrayList<SwitchStatementGroup> stmtGroup) {
        super(line);
        this.condition = condition;
        this.stmtGroup = stmtGroup;
    }

    // synthetic local offset to store evaluated selector for codegen
    private int selectorLocalOffset = -1;

    public JStatement analyze(Context context) {
        // Analyze selector expression
        condition = (JExpression) condition.analyze(context);

        // We only handle int selectors for now
        if (!condition.type().equals(Type.INT)) {
            JAST.compilationUnit.reportSemanticError(line(),
                    "switch selector must be of type int (currently supports only int)");
        }

        // Create a local context for case blocks so local variables in cases are scoped
        LocalContext switchContext = new LocalContext(context);

        // Allocate a synthetic local to hold the selector value at runtime
        selectorLocalOffset = switchContext.nextOffset();
        LocalVariableDefn selectorDefn = new LocalVariableDefn(condition.type(), selectorLocalOffset);
        switchContext.addEntry(line(), "_switch$" + line, selectorDefn);
        selectorDefn.initialize();

        // Analyze groups: labels and statements
        for (SwitchStatementGroup group : stmtGroup) {
            // analyze each case label (may be null for default)
            if (group.switchLabels != null) {
                ArrayList<JExpression> analyzedLabels = new ArrayList<JExpression>();
                for (JExpression lbl : group.switchLabels) {
                    if (lbl != null) {
                        lbl = (JExpression) lbl.analyze(context);
                        // require int literal or int-typed constant (conservative)
                        if (!lbl.type().equals(Type.INT)) {
                            JAST.compilationUnit.reportSemanticError(line(),
                                    "switch case label must be int literal");
                        }
                    }
                    analyzedLabels.add(lbl);
                }
                group.switchLabels = analyzedLabels;
            }

            // analyze statements in the switchContext so their locals are scoped here
            if (group.block != null) {
                ArrayList<JStatement> analyzedBlock = new ArrayList<JStatement>();
                for (JStatement s : group.block) {
                    analyzedBlock.add((JStatement) s.analyze(switchContext));
                }
                group.block = analyzedBlock;
            }
        }

        return this;
    }

    public void codegen(CLEmitter output) {
        // Create labels for each group and for end
        ArrayList<String> groupLabels = new ArrayList<String>();
        for (int i = 0; i < stmtGroup.size(); i++) {
            groupLabels.add(output.createLabel());
        }
        String endLabel = output.createLabel();

        // Evaluate selector once and store in synthetic local
        condition.codegen(output);
        // store int to local
        output.addOneArgInstruction(ISTORE, selectorLocalOffset);

        // For each group that has case labels, generate comparisons that jump to the group's label
        String defaultLabel = null;
        for (int i = 0; i < stmtGroup.size(); i++) {
            SwitchStatementGroup group = stmtGroup.get(i);
            ArrayList<JExpression> labels = group.switchLabels;
            if (labels == null || labels.isEmpty()) {
                // default case (use first null label encountered)
                defaultLabel = groupLabels.get(i);
                continue;
            }
            for (JExpression lbl : labels) {
                if (lbl == null) {
                    defaultLabel = groupLabels.get(i);
                    continue;
                }
                // load selector
                output.addOneArgInstruction(ILOAD, selectorLocalOffset);
                // push case constant
                if (lbl instanceof JLiteralInt) {
                    int v = ((JLiteralInt) lbl).toInt();
                    // use efficient push where possible
                    if (v >= -1 && v <= 5) {
                        switch (v) {
                            case -1: output.addNoArgInstruction(ICONST_M1); break;
                            case 0: output.addNoArgInstruction(ICONST_0); break;
                            case 1: output.addNoArgInstruction(ICONST_1); break;
                            case 2: output.addNoArgInstruction(ICONST_2); break;
                            case 3: output.addNoArgInstruction(ICONST_3); break;
                            case 4: output.addNoArgInstruction(ICONST_4); break;
                            case 5: output.addNoArgInstruction(ICONST_5); break;
                        }
                    } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
                        output.addOneArgInstruction(BIPUSH, v);
                    } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
                        output.addOneArgInstruction(SIPUSH, v);
                    } else {
                        output.addLDCInstruction(v);
                    }
                    // compare and jump if equal
                    output.addBranchInstruction(IF_ICMPEQ, groupLabels.get(i));
                } else {
                    // For non-literal labels, evaluate label expr and compare
                    lbl.codegen(output);
                    output.addBranchInstruction(IF_ICMPEQ, groupLabels.get(i));
                }
            }
        }

        // If no case matched, jump to default if present, otherwise to end
        if (defaultLabel != null) {
            output.addBranchInstruction(GOTO, defaultLabel);
        } else {
            output.addBranchInstruction(GOTO, endLabel);
        }

        // Emit code for each group (preserve fall-through)
        for (int i = 0; i < stmtGroup.size(); i++) {
            String lbl = groupLabels.get(i);
            output.addLabel(lbl);
            SwitchStatementGroup group = stmtGroup.get(i);
            if (group.block != null) {
                for (JStatement s : group.block) {
                    s.codegen(output);
                }
            }
            // fall through to next group by default
        }

        output.addLabel(endLabel);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JSwitchStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Condition", e1);
        condition.toJSON(e1);
        for (SwitchStatementGroup group : stmtGroup) {
            group.toJSON(e);
        }
    }
}

/**
 * A switch statement group consists of case labels and a block of statements.
 */
class SwitchStatementGroup {
    // Case labels.
    ArrayList<JExpression> switchLabels;

    // Block of statements.
    ArrayList<JStatement> block;

    /**
     * Constructs a switch-statement group.
     *
     * @param switchLabels case labels.
     * @param block        block of statements.
     */
    public SwitchStatementGroup(ArrayList<JExpression> switchLabels, ArrayList<JStatement> block) {
        this.switchLabels = switchLabels;
        this.block = block;
    }

    /**
     * Stores information about this switch statement group in JSON format.
     *
     * @param json the JSON emitter.
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("SwitchStatementGroup", e);
        for (JExpression label : switchLabels) {
            JSONElement e1 = new JSONElement();
            if (label != null) {
                e.addChild("Case", e1);
                label.toJSON(e1);
            } else {
                e.addChild("Default", e1);
            }
        }
        if (block != null) {
            for (JStatement stmt : block) {
                stmt.toJSON(e);
            }
        }
    }
}
