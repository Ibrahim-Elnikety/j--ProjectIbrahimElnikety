// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for an assignment operation.
 */
abstract class JAssignment extends JBinaryExpression {
    public JAssignment(int line, String operator, JExpression lhs, JExpression rhs) {
        super(line, operator, lhs, rhs);
    }
}

/**
 * Assignment (=)
 */
class JAssignOp extends JAssignment {
    public JAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment");
        } else {
            lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        }
        rhs = (JExpression) rhs.analyze(context);
        rhs.type().mustMatchExpected(line(), lhs.type());
        type = rhs.type();
        if (lhs instanceof JVariable) {
            IDefn defn = ((JVariable) lhs).iDefn();
            if (defn != null) ((LocalVariableDefn) defn).initialize();
        }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        rhs.codegen(output);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

/**
 * Plus-assign (+=) (already handled specially for String)
 */
class JPlusAssignOp extends JAssignment {
    public JPlusAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "+=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment");
            return this;
        } else {
            lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        }
        rhs = (JExpression) rhs.analyze(context);
        if (lhs.type().equals(Type.INT)) {
            rhs.type().mustMatchExpected(line(), Type.INT);
            type = Type.INT;
        } else if (lhs.type().equals(Type.STRING)) {
            rhs = (new JStringConcatenationOp(line, lhs, rhs)).analyze(context);
            type = Type.STRING;
        } else {
            JAST.compilationUnit.reportSemanticError(line(), "Invalid lhs type for +=: " + lhs.type());
        }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        if (lhs.type().equals(Type.STRING)) {
            rhs.codegen(output);
        } else {
            ((JLhs) lhs).codegenLoadLhsRvalue(output);
            rhs.codegen(output);
            output.addNoArgInstruction(IADD);
        }
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

/* Int-only compound-assign implementations (analyze + codegen) */

class JMinusAssignOp extends JAssignment {
    public JMinusAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "-=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(ISUB);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JStarAssignOp extends JAssignment {
    public JStarAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "*=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IMUL);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JDivAssignOp extends JAssignment {
    public JDivAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "/=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IDIV);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JRemAssignOp extends JAssignment {
    public JRemAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "%=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IREM);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

/* OR/AND/XOR assign variants */

class JOrAssignOp extends JAssignment {
    public JOrAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "|=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IOR);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JAndAssignOp extends JAssignment {
    public JAndAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "&=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IAND);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JXorAssignOp extends JAssignment {
    public JXorAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "^=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IXOR);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

/* Shift-assign variants */

class JALeftShiftAssignOp extends JAssignment {
    public JALeftShiftAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "<<=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(ISHL);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JARightShiftAssignOp extends JAssignment {
    public JARightShiftAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, ">>=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(ISHR);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

class JLRightShiftAssignOp extends JAssignment {
    public JLRightShiftAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, ">>>=", lhs, rhs); }
    public JExpression analyze(Context context) {
        if (!(lhs instanceof JLhs)) { JAST.compilationUnit.reportSemanticError(line(), "Illegal lhs for assignment"); return this; }
        lhs = (JExpression) ((JLhs) lhs).analyzeLhs(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchExpected(line(), Type.INT); rhs.type().mustMatchExpected(line(), Type.INT);
        type = lhs.type();
        if (lhs instanceof JVariable) { IDefn defn = ((JVariable) lhs).iDefn(); if (defn != null) ((LocalVariableDefn) defn).initialize(); }
        return this;
    }
    public void codegen(CLEmitter output) {
        ((JLhs) lhs).codegenLoadLhsLvalue(output);
        ((JLhs) lhs).codegenLoadLhsRvalue(output);
        rhs.codegen(output);
        output.addNoArgInstruction(IUSHR);
        if (!isStatementExpression) ((JLhs) lhs).codegenDuplicateRvalue(output);
        ((JLhs) lhs).codegenStore(output);
    }
}

/* Parser-expected assign names (aliases) to match Parser instantiations */

class JRemainderAssignOp extends JAssignment {
    public JRemainderAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "%=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JRemAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JRemAssignOp(line, lhs, rhs).codegen(output); }
}

class JBitAndAssignOp extends JAssignment {
    public JBitAndAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "&=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JAndAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JAndAssignOp(line, lhs, rhs).codegen(output); }
}

class JBitOrAssignOp extends JAssignment {
    public JBitOrAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "|=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JOrAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JOrAssignOp(line, lhs, rhs).codegen(output); }
}

class JBitXorAssignOp extends JAssignment {
    public JBitXorAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "^=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JXorAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JXorAssignOp(line, lhs, rhs).codegen(output); }
}

class JLeftShiftAssignOp extends JAssignment {
    public JLeftShiftAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, "<<=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JALeftShiftAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JALeftShiftAssignOp(line, lhs, rhs).codegen(output); }
}

class JRightShiftAssignOp extends JAssignment {
    public JRightShiftAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, ">>=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JARightShiftAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JARightShiftAssignOp(line, lhs, rhs).codegen(output); }
}

class JUnsignedRightShiftAssignOp extends JAssignment {
    public JUnsignedRightShiftAssignOp(int line, JExpression lhs, JExpression rhs) { super(line, ">>>=", lhs, rhs); }
    public JExpression analyze(Context context) { return new JLRightShiftAssignOp(line, lhs, rhs).analyze(context); }
    public void codegen(CLEmitter output) { new JLRightShiftAssignOp(line, lhs, rhs).codegen(output); }
}