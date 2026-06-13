// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a do-statement.
 */
public class JDoStatement extends JStatement {
	// Body.
	private JStatement body;

	// Test expression.
	private JExpression condition;

	/**
	 * Constructs an AST node for a do-statement.
	 *
	 * @param line      line in which the do-statement occurs in the source file.
	 * @param body      the body.
	 * @param condition test expression.
	 */
	public JDoStatement(int line, JStatement body, JExpression condition) {
		super(line);
		this.body = body;
		this.condition = condition;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JStatement analyze(Context context) {
		// Create a loop-local context so locals declared inside the body are scoped
		// properly
		Context loopContext = new LocalContext(context);

		// Analyze the body first (do-while executes body before condition)
		if (body != null) {
			body = (JStatement) body.analyze(loopContext);
		}

		// Analyze and type-check the condition
		if (condition != null) {
			condition = (JExpression) condition.analyze(loopContext);
			condition.type().mustMatchExpected(line(), Type.BOOLEAN);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void codegen(CLEmitter output) {
	    String bodyLabel = output.createLabel();
	    String endLabel  = output.createLabel();

	    // Entry falls into body
	    output.addLabel(bodyLabel);
	    if (body != null) {
	        body.codegen(output);
	    }

	    // Test the condition: if false, jump to end
	    condition.codegen(output, endLabel, false);

	    // Otherwise loop back to body
	    output.addBranchInstruction(GOTO, bodyLabel);

	    // Exit point
	    output.addLabel(endLabel);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void toJSON(JSONElement json) {
		JSONElement e = new JSONElement();
		json.addChild("JDoStatement:" + line, e);
		JSONElement e1 = new JSONElement();
		e.addChild("Body", e1);
		body.toJSON(e1);
		JSONElement e2 = new JSONElement();
		e.addChild("Condition", e2);
		condition.toJSON(e2);
	}
}
