package cool.compiler;

import cool.parser.ASTVisitor;
import cool.parser.nodes.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Map;

public class CodeGenVisitor implements ASTVisitor<ST> {
    static STGroupFile templates = new STGroupFile("cgen.stg");
    ST mainSection;
    ST dataSection;
    ST funcSection;

    Map<Integer, ST> intLiteralMemo;
    Map<String, ST> stringLiteralMemo;
    Map<Boolean, ST> booleanLiteralMemo;

    // Map each class name to the number of its instances
    Map<String, Integer> uniqueLabelCounter;

    @Override
    public ST visit(Program program) {
        templates.getInstanceOf("prologue");

        program.classes.forEach(x -> x.accept(this));

        return null;
    }

    @Override
    public ST visit(ClassMethodCall classMethodCall) {
        return null;
    }

    @Override
    public ST visit(Assign assign) {
        return null;
    }

    @Override
    public ST visit(UMinus uMinus) {
        return null;
    }

    @Override
    public ST visit(New neww) {
        return null;
    }

    @Override
    public ST visit(IsVoid isVoid) {
        return null;
    }

    @Override
    public ST visit(Not not) {
        return null;
    }

    @Override
    public ST visit(Mult mult) {
        return null;
    }

    @Override
    public ST visit(Div div) {
        return null;
    }

    @Override
    public ST visit(Plus plus) {
        return null;
    }

    @Override
    public ST visit(Minus minus) {
        return null;
    }

    @Override
    public ST visit(Relational relational) {
        return null;
    }

    @Override
    public ST visit(Id id) {
        return null;
    }

    @Override
    public ST visit(Int intt) {
        // .word 2              # type
        // .word 4              # size
        // .word Int_dispTab    # dispatch table
        // .word 0              # value

        return null;
    }

    @Override
    public ST visit(Stringg string) {
        // Each String generates an instance of Integer,
        // used to store the length of the String's content

        // .word 3
        // .word 6              # size in words
        // .word String_dispTab
        // .word int_const4
        // .asciiz "content"
        // .align 2             # 2^2 alignment = 32 bits

        return null;
    }

    @Override
    public ST visit(Bool bool) {
        return null;
    }

    @Override
    public ST visit(ClassMethodDef classMethodDef) {
        return null;
    }

    @Override
    public ST visit(ClassMemberDef classMemberDef) {
        return null;
    }

    @Override
    public ST visit(Formal formal) {
        return null;
    }

    @Override
    public ST visit(Type type) {
        return null;
    }

    @Override
    public ST visit(ClassDef classDef) {
        return null;
    }

    @Override
    public ST visit(If iff) {
        return null;
    }

    @Override
    public ST visit(While whilee) {
        return null;
    }

    @Override
    public ST visit(LetLocalVar letLocalVar) {
        return null;
    }

    @Override
    public ST visit(Let let) {
        return null;
    }

    @Override
    public ST visit(Case casee) {
        return null;
    }

    @Override
    public ST visit(CaseBranch caseBranch) {
        return null;
    }

    @Override
    public ST visit(Block block) {
        ST seq = templates.getInstanceOf("sequence");

        block.expressions
                .forEach(x -> seq.add("e", x.accept(this)));

        return seq;
    }
}
