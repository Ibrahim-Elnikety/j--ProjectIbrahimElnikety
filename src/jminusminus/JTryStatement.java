// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a try-catch-finally statement.
 */
class JTryStatement extends JStatement {
    // The try block.
    private JBlock tryBlock;

    // The catch parameters.
    private ArrayList<JFormalParameter> parameters;

    // The catch blocks.
    private ArrayList<JBlock> catchBlocks;

    // The finally block.
    private JBlock finallyBlock;

    // synthetic locals allocated for catch parameters (parallel to parameters list)
    private ArrayList<Integer> catchParamOffsets;

    // synthetic local used to stash exception for finally handling (or -1 if unused)
    private int finallyTempOffset = -1;

    /**
     * Constructs an AST node for a try-statement.
     *
     * @param line         line in which the try-statement occurs in the source file.
     * @param tryBlock     the try block.
     * @param parameters   the catch parameters.
     * @param catchBlocks  the catch blocks.
     * @param finallyBlock the finally block.
     */
    public JTryStatement(int line, JBlock tryBlock, ArrayList<JFormalParameter> parameters,
                         ArrayList<JBlock> catchBlocks, JBlock finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.parameters = parameters;
        this.catchBlocks = catchBlocks;
        this.finallyBlock = finallyBlock;
        this.catchParamOffsets = (parameters != null) ? new ArrayList<Integer>(parameters.size()) : null;
    }

    /**
     * {@inheritDoc}
     */
    public JTryStatement analyze(Context context) {
        // Analyze try block in its own local context so locals declared inside are scoped there.
        LocalContext tryContext = new LocalContext(context);
        if (tryBlock != null) {
            tryBlock = (JBlock) tryBlock.analyze(tryContext);
        }

        // Sanity check: parameters and catchBlocks must align
        if ((parameters == null && catchBlocks != null) || (parameters != null && catchBlocks == null)
                || (parameters != null && catchBlocks != null && parameters.size() != catchBlocks.size())) {
            JAST.compilationUnit.reportSemanticError(line(),
                    "mismatched number of catch parameters and catch blocks");
        }

        // Analyze each catch clause in its own local context.
        if (parameters != null) {
            catchParamOffsets = new ArrayList<Integer>(parameters.size());
            for (int i = 0; i < parameters.size(); i++) {
                JFormalParameter param = parameters.get(i);
                JBlock catchBlock = catchBlocks.get(i);

                // The exception parameter type must be a reference type (not primitive).
                if (param.type().isPrimitive()) {
                    JAST.compilationUnit.reportSemanticError(param.line(),
                            "catch parameter must be a reference type");
                }

                // Create a local context for the catch block where the exception param is visible.
                LocalContext catchContext = new LocalContext(context);

                // Allocate a local slot for the exception parameter and register it.
                int offset = catchContext.nextOffset();
                LocalVariableDefn defn = new LocalVariableDefn(param.type(), offset);
                catchContext.addEntry(param.line(), param.name(), defn);
                defn.initialize();

                // Remember offset for codegen
                catchParamOffsets.add(offset);

                // Analyze the catch block in this catchContext.
                if (catchBlock != null) {
                    catchBlocks.set(i, (JBlock) catchBlock.analyze(catchContext));
                }
            }
        }

        // Allocate finally temp local (if there is a finally) in the surrounding method context
        if (finallyBlock != null) {
            MethodContext mctx = context.methodContext();
            if (mctx == null) {
                JAST.compilationUnit.reportSemanticError(line(), "try/finally must be inside a method");
                finallyTempOffset = -1;
            } else {
                finallyTempOffset = mctx.nextOffset();
            }
        }

        // Analyze finally block (if any) in its own local context.
        if (finallyBlock != null) {
            LocalContext finContext = new LocalContext(context);
            finallyBlock = (JBlock) finallyBlock.analyze(finContext);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     *
     * Important: this codegen emits the try region, handler entry code blocks and finally body,
     * but it does NOT register exception-table entries with CLEmitter because the project's
     * CLEmitter may not expose that API. For real exception dispatch to work you must have the
     * CLEmitter record try->handler table entries (see CLEmitter extension notes).
     */
    public void codegen(CLEmitter output) {
        // Labels:
        String tryStart = output.createLabel();
        String tryEnd = output.createLabel();
        String afterHandlers = output.createLabel();

        // Labels for handlers (one per catch)
        ArrayList<String> handlerLabels = new ArrayList<String>();
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                handlerLabels.add(output.createLabel());
            }
        }

        // Label for finally handler (used if finally exists to ensure it can run on exceptions)
        String finallyHandler = (finallyBlock != null) ? output.createLabel() : null;

        // Mark try start
        output.addLabel(tryStart);

        // Emit try block
        if (tryBlock != null) {
            tryBlock.codegen(output);
        }

        // Mark try end
        output.addLabel(tryEnd);

        // On normal flow, jump over handlers to afterHandlers (handlers follow)
        output.addBranchInstruction(GOTO, afterHandlers);

        // Emit handlers. NOTE: Without exception-table entries these blocks will never be reached
        // by the JVM exception dispatch; they are emitted so code appears in the class file.
        if (parameters != null) {
        	for (int i = 0; i < parameters.size(); i++) {
        	    JBlock handlerBlock = catchBlocks.get(i);
        	    String hLabel = handlerLabels.get(i);

        	    // Mark handler label entry point
        	    output.addLabel(hLabel);


                // The JVM leaves the exception object on the stack at handler entry.
                // Store it into the catch parameter local (we allocated offsets during analyze)
                int offset = (catchParamOffsets != null && i < catchParamOffsets.size())
                        ? catchParamOffsets.get(i)
                        : -1;
                if (offset >= 0) {
                    output.addOneArgInstruction(ASTORE, offset);
                } else {
                    output.addNoArgInstruction(POP);
                }

                // Execute catch block
                if (handlerBlock != null) {
                    handlerBlock.codegen(output);
                }

                // After catch block, jump to afterHandlers so we don't fall into next handler
                output.addBranchInstruction(GOTO, afterHandlers);
            }
        }

        // Optional finally handling for exception paths:
        if (finallyBlock != null) {
            // Emit finally handler entry
            output.addLabel(finallyHandler);

            // Throwable on stack at handler entry. Save to temp local if available.
            if (finallyTempOffset >= 0) {
                output.addOneArgInstruction(ASTORE, finallyTempOffset);
            } else {
                output.addNoArgInstruction(POP);
            }

            // Execute finally block
            finallyBlock.codegen(output);

            // Rethrow saved exception if possible
            if (finallyTempOffset >= 0) {
                output.addOneArgInstruction(ALOAD, finallyTempOffset);
                output.addNoArgInstruction(ATHROW);
            }
        }

        // After handlers label
        output.addLabel(afterHandlers);

        // On normal completion, run finally if present
        if (finallyBlock != null) {
            finallyBlock.codegen(output);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JTryStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("TryBlock", e1);
        if (tryBlock != null) tryBlock.toJSON(e1);
        if (catchBlocks != null) {
            for (int i = 0; i < catchBlocks.size(); i++) {
                JFormalParameter param = parameters.get(i);
                JBlock catchBlock = catchBlocks.get(i);
                JSONElement e2 = new JSONElement();
                e.addChild("CatchBlock", e2);
                String s = String.format("[\"%s\", \"%s\"]", param.name(), param.type() == null ?
                        "" : param.type().toString());
                e2.addAttribute("parameter", s);
                if (catchBlock != null) catchBlock.toJSON(e2);
            }
        }
        if (finallyBlock != null) {
            JSONElement e2 = new JSONElement();
            e.addChild("FinallyBlock", e2);
            finallyBlock.toJSON(e2);
        }
    }
}