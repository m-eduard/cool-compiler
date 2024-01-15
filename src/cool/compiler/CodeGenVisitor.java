package cool.compiler;

import cool.parser.ASTVisitor;
import cool.parser.nodes.*;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.HashMap;
import java.util.Map;

public class CodeGenVisitor implements ASTVisitor<ST> {
    static STGroupFile templates = new STGroupFile("cool/compiler/cgen.stg");
    ST mainSection;
    ST dataSection;

    ST classNameTable;

    Map<Integer, String> intLiteralPool = new HashMap<>();
    Map<String, String> stringLiteralPool = new HashMap<>();
    Map<Boolean, String> booleanLiteralPool = new HashMap<>();

    // Map each class name to the number of its instances
    Map<String, Integer> uniqueLabelCounter = new HashMap<>();

    // <class>.<method> mapped to its offset
    Map<String, Integer> methodOffset;

    private String getIntLabel(Integer value) {
        if (intLiteralPool.containsKey(value))
            return intLiteralPool.get(value);

        // Update the old counter
        uniqueLabelCounter.put("Int", uniqueLabelCounter.getOrDefault("Int", -1) + 1);

        String label = "int_const" + uniqueLabelCounter.get("Int");
        ST intLiteral = templates.getInstanceOf("int_literal")
                .add("value", value)
                .add("label", label);

        this.dataSection.add("e", intLiteral);
        this.intLiteralPool.put(value, label);
        return label;
    }

    // Creates a new instance of String and adds it to .data
    private String getStringLabel(String value) {
        if (stringLiteralPool.containsKey(value))
            return stringLiteralPool.get(value);

        // Update the old counter
        uniqueLabelCounter.put("String", uniqueLabelCounter.getOrDefault("String", -1) + 1);

        // Each String generates an instance of Integer,
        // used to store the length of the String's content
        String lengthRef = getIntLabel(value.length());

        String label = "str_const" + uniqueLabelCounter.get("String");
        ST stringLiteral = templates.getInstanceOf("string_literal")
                .add("value", value)
                .add("numWords", (value.length() + 1) / 4)
                .add("lengthRef", lengthRef)
                .add("label", label);

        this.dataSection.add("e", stringLiteral);
        this.stringLiteralPool.put(value, label);
        return label;
    }

    private String getBoolLabel(Boolean value) {
        if (booleanLiteralPool.containsKey(value))
            return booleanLiteralPool.get(value);

        // Update the old counter
        uniqueLabelCounter.put("Bool", uniqueLabelCounter.getOrDefault("Bool", -1) + 1);

        String label = "bool_const" + uniqueLabelCounter.get("Bool");
        ST boolLiteral = templates.getInstanceOf("bool_literal")
                .add("value", value ? 1 : 0)
                .add("label", label);

        this.dataSection.add("e", boolLiteral);
        this.booleanLiteralPool.put(value, label);
        return label;
    }

    @Override
    public ST visit(Program program) {
        this.dataSection = templates.getInstanceOf("sequenceSpaced");
        this.classNameTable = templates.getInstanceOf("classNameTable");
        this.dataSection.add("e", this.classNameTable);

        templates.getInstanceOf("prologue");

        program.classes.forEach(x -> this.dataSection.add("e", x.accept(this)));

        ST programST = templates.getInstanceOf("program");
        programST.add("data", dataSection);

        // adaug numele tuturor fisierelor ca instante de string

        return programST;
    }

    @Override
    public ST visit(ClassMethodCall classMethodCall) {
        // First evaluate the actual parameters

        // Store in $a0 the address of self

        ST template = templates.getInstanceOf("method_call")
                .add("filename", "test")
                .add("line", classMethodCall.token.getLine());

        return template;
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
        getStringLabel(string.token.getText());
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
        System.out.println("this");
        return null;
    }

    @Override
    public ST visit(ClassDef classDef) {
        // Create a COOL String instance for class name
        classNameTable.add("e", getStringLabel(classDef.token.getText()));

        classDef.features.forEach(x -> x.accept(this));

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
