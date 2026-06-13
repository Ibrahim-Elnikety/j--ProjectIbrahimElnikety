package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for an identifier used as a primary expression.
 */
class JVariable extends JExpression implements JLhs {
	// The variable's name.
	private String name;

	// The variable's definition.
	private IDefn iDefn;

	// Was analyzeLhs() done?
	private boolean analyzeLhs;

	/**
	 * Constructs the AST node for a variable.
	 *
	 * @param line line in which the variable occurs in the source file.
	 * @param name the name.
	 */
	public JVariable(int line, String name) {
		super(line);
		this.name = name;
	}

	/**
	 * Returns the identifier name.
	 *
	 * @return the identifier name.
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the identifier's definition.
	 *
	 * @return the identifier's definition.
	 */
	public IDefn iDefn() {
		return iDefn;
	}

	@Override
	public JExpression analyze(Context context) {
		iDefn = context.lookup(name);
		if (iDefn == null) {
			// Not a local, but is it a field?
			Type definingType = context.definingType();
			Field field = definingType.fieldFor(name);
			if (field == null) {
				// Report the undeclared-variable error once.
				JAST.compilationUnit.reportSemanticError(line(), "Cannot find name: %s", name);

				// Try to synthesize a placeholder local variable in the current method context
				// so that subsequent uses of the same name don't produce repeated errors.
				MethodContext mc = context.methodContext(); // may be null outside methods
				if (mc != null) {
					// Only insert if not already present in this (or surrounding) context.
					if (mc.lookup(name) == null) {
						// Allocate an offset for the synthetic local variable.
						int offset = mc.nextOffset();

						// Create a synthetic LocalVariableDefn using the available constructor.
						LocalVariableDefn synth = new LocalVariableDefn(Type.ANY, offset);

						// Mark it initialized so we don't trigger "might not have been initialized".
						synth.initialize();

						// Enter the synthesized definition into the current method/local context.
						mc.addEntry(line(), name, synth);

						// Point this node at the synthesized defn so later logic uses it.
						iDefn = synth;
					} else {
						// If lookup finds something, bind to it.
						iDefn = mc.lookup(name);
					}
				}

				// Preserve previous behavior: mark this node's type as ANY so analysis can
				// continue.
				type = Type.ANY;
			} else {
				// Rewrite a variable denoting a field as an explicit field selection.
				type = field.type();
				// If static, use a variable node containing the type name as the qualifier.
				// Otherwise use 'this' for instance fields.
				JExpression qualifier = field.isStatic()
						|| (context.methodContext() != null && context.methodContext().isStatic())
								? new JVariable(line(), definingType.toString())
								: new JThis(line());
				JExpression newTree = new JFieldSelection(line(), qualifier, name);
				return (JExpression) newTree.analyze(context);
			}
		} else {
			if (!analyzeLhs && iDefn instanceof LocalVariableDefn && !((LocalVariableDefn) iDefn).isInitialized()) {
				JAST.compilationUnit.reportSemanticError(line(), "Variable %s might not have been initialized", name);
			}
			type = iDefn.type();
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public JExpression analyzeLhs(Context context) {
		analyzeLhs = true;
		JExpression newTree = analyze(context);
		if (newTree instanceof JVariable) {
			// Could (now) be a JFieldSelection, but if it's (still) a JVariable...
			if (iDefn != null && !(iDefn instanceof LocalVariableDefn)) {
				JAST.compilationUnit.reportSemanticError(line(), "%s is a bad LHS to a =", name);
			}
		}
		return newTree;
	}

	/**
	 * {@inheritDoc}
	 */
	public void codegen(CLEmitter output) {
    if (iDefn instanceof LocalVariableDefn) {
        int offset = ((LocalVariableDefn) iDefn).offset();
        if (type.isReference()) {
            switch (offset) {
                case 0: output.addNoArgInstruction(ALOAD_0); break;
                case 1: output.addNoArgInstruction(ALOAD_1); break;
                case 2: output.addNoArgInstruction(ALOAD_2); break;
                case 3: output.addNoArgInstruction(ALOAD_3); break;
                default: output.addOneArgInstruction(ALOAD, offset); break;
            }
        } else {
            if (type == Type.INT || type == Type.BOOLEAN || type == Type.CHAR) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(ILOAD_0); break;
                    case 1: output.addNoArgInstruction(ILOAD_1); break;
                    case 2: output.addNoArgInstruction(ILOAD_2); break;
                    case 3: output.addNoArgInstruction(ILOAD_3); break;
                    default: output.addOneArgInstruction(ILOAD, offset); break;
                }
            } else if (type == Type.LONG) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(LLOAD_0); break;
                    case 1: output.addNoArgInstruction(LLOAD_1); break;
                    case 2: output.addNoArgInstruction(LLOAD_2); break;
                    case 3: output.addNoArgInstruction(LLOAD_3); break;
                    default: output.addOneArgInstruction(LLOAD, offset); break;
                }
            } else if (type == Type.FLOAT) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(FLOAD_0); break;
                    case 1: output.addNoArgInstruction(FLOAD_1); break;
                    case 2: output.addNoArgInstruction(FLOAD_2); break;
                    case 3: output.addNoArgInstruction(FLOAD_3); break;
                    default: output.addOneArgInstruction(FLOAD, offset); break;
                }
            } else if (type == Type.DOUBLE) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(DLOAD_0); break;
                    case 1: output.addNoArgInstruction(DLOAD_1); break;
                    case 2: output.addNoArgInstruction(DLOAD_2); break;
                    case 3: output.addNoArgInstruction(DLOAD_3); break;
                    default: output.addOneArgInstruction(DLOAD, offset); break;
                }
            }
        }
    }
}

	/**
	 * {@inheritDoc}
	 */
	public void codegen(CLEmitter output, String targetLabel, boolean onTrue) {
	    if (iDefn instanceof LocalVariableDefn) {
	        codegen(output); // push value

	        // For non-int primitives, convert to int by comparing to zero.
	        if (type == Type.LONG) {
	            output.addNoArgInstruction(LCONST_0);
	            output.addNoArgInstruction(LCMP); // leaves int on stack
	        } else if (type == Type.DOUBLE) {
	            output.addNoArgInstruction(DCONST_0);
	            output.addNoArgInstruction(DCMPG); // or DCMPL; choose one consistently
	        } else if (type == Type.FLOAT) {
	            output.addNoArgInstruction(FCONST_0);
	            output.addNoArgInstruction(FCMPG); // or FCMPL
	        }
	        output.addBranchInstruction(onTrue ? IFNE : IFEQ, targetLabel);
	    }
	}

	/**
	 * {@inheritDoc}
	 */
	public void codegenLoadLhsLvalue(CLEmitter output) {
		// Nothing here.
	}

	/**
	 * {@inheritDoc}
	 */
	public void codegenLoadLhsRvalue(CLEmitter output) {
		codegen(output);
	}

	/**
	 * {@inheritDoc}
	 */
	public void codegenDuplicateRvalue(CLEmitter output) {
	    if (iDefn instanceof LocalVariableDefn) {
	        output.addNoArgInstruction((type == Type.LONG || type == Type.DOUBLE) ? DUP2 : DUP);
	    }
	}

	/**
	 * {@inheritDoc}
	 */
	public void codegenStore(CLEmitter output) {
    if (iDefn instanceof LocalVariableDefn) {
        int offset = ((LocalVariableDefn) iDefn).offset();
        if (type.isReference()) {
            switch (offset) {
                case 0: output.addNoArgInstruction(ASTORE_0); break;
                case 1: output.addNoArgInstruction(ASTORE_1); break;
                case 2: output.addNoArgInstruction(ASTORE_2); break;
                case 3: output.addNoArgInstruction(ASTORE_3); break;
                default: output.addOneArgInstruction(ASTORE, offset); break;
            }
        } else {
            if (type == Type.INT || type == Type.BOOLEAN || type == Type.CHAR) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(ISTORE_0); break;
                    case 1: output.addNoArgInstruction(ISTORE_1); break;
                    case 2: output.addNoArgInstruction(ISTORE_2); break;
                    case 3: output.addNoArgInstruction(ISTORE_3); break;
                    default: output.addOneArgInstruction(ISTORE, offset); break;
                }
            } else if (type == Type.LONG) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(LSTORE_0); break;
                    case 1: output.addNoArgInstruction(LSTORE_1); break;
                    case 2: output.addNoArgInstruction(LSTORE_2); break;
                    case 3: output.addNoArgInstruction(LSTORE_3); break;
                    default: output.addOneArgInstruction(LSTORE, offset); break;
                }
            } else if (type == Type.FLOAT) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(FSTORE_0); break;
                    case 1: output.addNoArgInstruction(FSTORE_1); break;
                    case 2: output.addNoArgInstruction(FSTORE_2); break;
                    case 3: output.addNoArgInstruction(FSTORE_3); break;
                    default: output.addOneArgInstruction(FSTORE, offset); break;
                }
            } else if (type == Type.DOUBLE) {
                switch (offset) {
                    case 0: output.addNoArgInstruction(DSTORE_0); break;
                    case 1: output.addNoArgInstruction(DSTORE_1); break;
                    case 2: output.addNoArgInstruction(DSTORE_2); break;
                    case 3: output.addNoArgInstruction(DSTORE_3); break;
                    default: output.addOneArgInstruction(DSTORE, offset); break;
                }
            }
        }
    }
}

	/**
	 * {@inheritDoc}
	 */
	public void toJSON(JSONElement json) {
		JSONElement e = new JSONElement();
		json.addChild("JVariable:" + line, e);
		e.addAttribute("name", name());
	}
}