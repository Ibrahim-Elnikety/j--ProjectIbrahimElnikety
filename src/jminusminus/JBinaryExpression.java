// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * This abstract base class is the AST node for a binary expression --- an expression with a binary
 * operator and two operands: lhs and rhs.
 */
abstract class JBinaryExpression extends JExpression {
    protected String operator;
    protected JExpression lhs;
    protected JExpression rhs;

    protected JBinaryExpression(int line, String operator, JExpression lhs, JExpression rhs) {
        super(line);
        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JBinaryExpression:" + line, e);
        e.addAttribute("operator", operator);
        e.addAttribute("type", type == null ? "" : type.toString());
        JSONElement e1 = new JSONElement();
        e.addChild("Operand1", e1);
        lhs.toJSON(e1);
        JSONElement e2 = new JSONElement();
        e.addChild("Operand2", e2);
        rhs.toJSON(e2);
    }
}

/* Binary operator classes with int/long support and JVM widening */

class JMultiplyOp extends JBinaryExpression {
    public JMultiplyOp(int line, JExpression lhs, JExpression rhs) { super(line, "*", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        // allow int or long; promote to long if either is long
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LMUL : IMUL);
    }
}

class JPlusOp extends JBinaryExpression {
    public JPlusOp(int line, JExpression lhs, JExpression rhs) { super(line, "+", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        if (lhs.type() == Type.STRING || rhs.type() == Type.STRING) {
            return (new JStringConcatenationOp(line, lhs, rhs)).analyze(context);
        }
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LADD : IADD);
    }
}

class JSubtractOp extends JBinaryExpression {
    public JSubtractOp(int line, JExpression lhs, JExpression rhs) { super(line, "-", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LSUB : ISUB);
    }
}

class JDivideOp extends JBinaryExpression {
    public JDivideOp(int line, JExpression lhs, JExpression rhs) { super(line, "/", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LDIV : IDIV);
    }
}

class JRemainderOp extends JBinaryExpression {
    public JRemainderOp(int line, JExpression lhs, JExpression rhs) { super(line, "%", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LREM : IREM);
    }
}

class JOrOp extends JBinaryExpression {
    public JOrOp(int line, JExpression lhs, JExpression rhs) { super(line, "|", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LOR : IOR);
    }
}

class JXorOp extends JBinaryExpression {
    public JXorOp(int line, JExpression lhs, JExpression rhs) { super(line, "^", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LXOR : IXOR);
    }
}

class JAndOp extends JBinaryExpression {
    public JAndOp(int line, JExpression lhs, JExpression rhs) { super(line, "&", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        type = (lhs.type() == Type.LONG || rhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        output.addNoArgInstruction(type == Type.LONG ? LAND : IAND);
    }
}

class JALeftShiftOp extends JBinaryExpression {
    public JALeftShiftOp(int line, JExpression lhs, JExpression rhs) { super(line, "<<", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        // Java: lhs can be int or long; rhs is int (shift count)
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchExpected(line(), Type.INT);
        type = (lhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output); // shift count must be int; if somehow long, convert
        if (rhs.type() == Type.LONG) output.addNoArgInstruction(L2I);
        output.addNoArgInstruction(type == Type.LONG ? LSHL : ISHL);
    }
}

class JARightShiftOp extends JBinaryExpression {
    public JARightShiftOp(int line, JExpression lhs, JExpression rhs) { super(line, ">>", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchExpected(line(), Type.INT);
        type = (lhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.LONG) output.addNoArgInstruction(L2I);
        output.addNoArgInstruction(type == Type.LONG ? LSHR : ISHR);
    }
}

class JLRightShiftOp extends JBinaryExpression {
    public JLRightShiftOp(int line, JExpression lhs, JExpression rhs) { super(line, ">>>", lhs, rhs); }
    public JExpression analyze(Context context) {
        lhs = (JExpression) lhs.analyze(context);
        rhs = (JExpression) rhs.analyze(context);
        lhs.type().mustMatchOneOf(line(), Type.INT, Type.LONG);
        rhs.type().mustMatchExpected(line(), Type.INT);
        type = (lhs.type() == Type.LONG) ? Type.LONG : Type.INT;
        return this;
    }
    public void codegen(CLEmitter output) {
        lhs.codegen(output);
        if (lhs.type() == Type.INT && type == Type.LONG) output.addNoArgInstruction(I2L);
        rhs.codegen(output);
        if (rhs.type() == Type.LONG) output.addNoArgInstruction(L2I);
        output.addNoArgInstruction(type == Type.LONG ? LUSHR : IUSHR);
    }
}