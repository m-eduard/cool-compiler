package cool.compiler;

import cool.parser.ASTVisitor;
import cool.parser.nodes.*;
import cool.structures.ClassSymbol;
import cool.structures.CodeGenUtils;
import cool.structures.SymbolTable;
import cool.structures.TypeSymbol;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CodeGenVisitor implements ASTVisitor<ST> {
    static STGroupFile templates = new STGroupFile("cool/compiler/cgen.stg");
    ST dataSection;

    ST textSection;

    Map<Integer, String> intLiteralPool = new HashMap<>();
    Map<String, String> stringLiteralPool = new HashMap<>();
    Map<Boolean, String> booleanLiteralPool = new HashMap<>();

    // Map each class name to the number of its instances
    Map<String, Integer> uniqueLabelCounter = new HashMap<>();

    // <class>.<method> mapped to its offset
    Map<String, Integer> methodOffset;

    public int getLabelIndex(String name) {
        uniqueLabelCounter.put(name, uniqueLabelCounter.getOrDefault(name, -1) + 1);
        return uniqueLabelCounter.get(name);
    }

    private String getIntLabel(Integer value) {
        if (intLiteralPool.containsKey(value))
            return intLiteralPool.get(value);

        // Update the old counter
        uniqueLabelCounter.put("Int", uniqueLabelCounter.getOrDefault("Int", -1) + 1);

        String label = "int_const" + uniqueLabelCounter.get("Int");
        ST intLiteral = templates.getInstanceOf("int_literal")
                .add("value", value)
                .add("label", label)
                .add("tag", CodeGenUtils.classesTags.get("Int"));

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
                .add("numWords", (value.length() / 4 + 1) + 4)
                .add("lengthRef", lengthRef)
                .add("label", label)
                .add("tag", CodeGenUtils.classesTags.get("String"));

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
                .add("label", label)
                .add("tag", CodeGenUtils.classesTags.get("Bool"));

        this.dataSection.add("e", boolLiteral);
        this.booleanLiteralPool.put(value, label);
        return label;
    }

    @Override
    public ST visit(Program program) {
        // initialize inheritance tree and tags
        CodeGenUtils.initializeInheritanceTree();
        CodeGenUtils.initializeClassesTags();
        CodeGenUtils.generateMethodsAndMembersOffsets();

        this.dataSection = templates.getInstanceOf("sequenceSpaced");
        this.textSection = templates.getInstanceOf("sequenceSpaced");

        ST classNameTable = templates.getInstanceOf("classNameTable");

        CodeGenUtils.orderedClasses.forEach(name -> classNameTable.add("e", getStringLabel(name)));

        this.dataSection.add("e", classNameTable);
        TypeSymbol.defaultClassesAsStrings.forEach(name -> this.textSection
                .add("e", templates.getInstanceOf("instanceInit")
                        .add("class", name)
                        .add("baseClass", "Object".equals(name) ? null : "Object")));

        this.dataSection.add("e", templates
                .getInstanceOf("genericProtObj")
                .add("name", "Object")
                .add("tag", CodeGenUtils.classesTags.get("Object")));

        this.dataSection.add("e", templates
                .getInstanceOf("genericProtObj")
                .add("name", "IO")
                .add("tag", CodeGenUtils.classesTags.get("IO")));

        this.dataSection.add("e", templates
                .getInstanceOf("intAndBoolProtObj")
                .add("name", "Int")
                .add("tag", CodeGenUtils.classesTags.get("Int")));

        this.dataSection.add("e", templates
                .getInstanceOf("intAndBoolProtObj")
                .add("name", "Bool")
                .add("tag", CodeGenUtils.classesTags.get("Bool")));

        this.dataSection.add("e", templates
                .getInstanceOf("stringProtObj")
                .add("tag", CodeGenUtils.classesTags.get("String"))
                .add("lengthTag", getIntLabel(0)));

        ST objectTable = templates.getInstanceOf("objectTable");
        CodeGenUtils.orderedClasses.forEach(name -> objectTable.add("content", templates
                .getInstanceOf("objectTableEntry")
                .add("className", name)));

        this.dataSection.add("e", objectTable);

        program.classes.forEach(x -> this.dataSection.add("e", x.accept(this)));

        ST programST = templates.getInstanceOf("program");
        programST.add("classes", CodeGenUtils.orderedClasses)
                .add("intTag", CodeGenUtils.classesTags.get("Int"))
                .add("boolTag", CodeGenUtils.classesTags.get("Bool"))
                .add("stringTag", CodeGenUtils.classesTags.get("String"));

        programST.add("data", dataSection);
        programST.add("text", textSection);

        for (String className : CodeGenUtils.orderedClasses) {
            ClassSymbol sym = ResolutionPassVisitor.getClassSymbol(SymbolTable.globals.lookup(className));

            dataSection.add("e", templates.getInstanceOf("dispTable")
                    .add("className", sym.getName())
                    .add("methods", sym.allMethods
                            .stream().map(x -> ((ClassSymbol)(x.getParent())).getName() + "." + x.getName()).toList()));
        }

        return programST;
    }

    @Override
    public ST visit(ClassMethodCall classMethodCall) {
        // First evaluate the actual parameters

        // Store in $a0 the address of self

        String filename = getStringLabel(new File(Compiler.fileNames.get(classMethodCall.context)).getName());

        ST template = templates.getInstanceOf("method_call")
                .add("filename", filename)
                .add("line", classMethodCall.token.getLine());

        return template;
    }

    @Override
    public ST visit(Assign assign) {
        ST expressionEval = assign.expr.accept(this);
        return templates.getInstanceOf("assign")
                .add("e", expressionEval)
                .add("offset", assign.id.getSymbol().offset);
    }

    @Override
    public ST visit(UMinus uMinus) {
        return templates.getInstanceOf("uminus").add("e", uMinus.expr.accept(this));
    }

    @Override
    public ST visit(New neww) {
        String className = neww.type.token.getText();
        if (!"SELF_TYPE".equals(className)) {
            return templates.getInstanceOf("newObject").add("type", className);
        }

        return templates.getInstanceOf("newSelfType");
    }

    @Override
    public ST visit(IsVoid isVoid) {
        String trueLabel = getBoolLabel(true);
        String falseLabel = getBoolLabel(false);

        return templates.getInstanceOf("isvoid").add("trueLabel", trueLabel)
                .add("falseLabel", falseLabel).add("label", getLabelIndex("isvoid"));
    }

    @Override
    public ST visit(Not not) {
        String trueLabel = getBoolLabel(true);
        String falseLabel = getBoolLabel(false);

        return templates.getInstanceOf("not")
                .add("trueLabel", trueLabel)
                .add("falseLabel", falseLabel)
                .add("notLabel", getLabelIndex("not"));
    }

    @Override
    public ST visit(Mult mult) {
        return templates.getInstanceOf("arithm")
                .add("e1", mult.left.accept(this))
                .add("e2", mult.right.accept(this))
                .add("op", "mul");
    }

    @Override
    public ST visit(Div div) {
        return templates.getInstanceOf("arithm")
                .add("e1", div.left.accept(this))
                .add("e2", div.right.accept(this))
                .add("op", "div");
    }

    @Override
    public ST visit(Plus plus) {
        return templates.getInstanceOf("arithm")
                .add("e1", plus.left.accept(this))
                .add("e2", plus.right.accept(this))
                .add("op", "add");
    }

    @Override
    public ST visit(Minus minus) {
        return templates.getInstanceOf("arithm")
                .add("e1", minus.left.accept(this))
                .add("e2", minus.right.accept(this))
                .add("op", "sub");
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
        return templates.getInstanceOf("loadInstance").add("e", getIntLabel(Integer
                .parseInt(intt.token.getText())));
    }

    @Override
    public ST visit(Stringg string) {
        return templates.getInstanceOf("loadInstance").add("e", getStringLabel(string.token.getText()));
    }

    @Override
    public ST visit(Bool bool) {
        return templates.getInstanceOf("loadInstance").add("e", getBoolLabel(bool.token
                .getText().equals("True")));
    }

    @Override
    public ST visit(ClassMethodDef classMethodDef) {
        ClassSymbol enclosingClass = (ClassSymbol)(classMethodDef.id.getScope().getParent());

        ST methodDef = templates.getInstanceOf("methodDefinition")
                .add("className", enclosingClass.getName())
                .add("methodName", classMethodDef.id.token.getText())
                .add("body", classMethodDef.body.accept(this));

        textSection.add("e", methodDef);

        return null;
    }

    @Override
    public ST visit(ClassMemberDef classMemberDef) {
        ST expressionEval = null;

        if (classMemberDef.initExpr != null)
            expressionEval = classMemberDef.initExpr.accept(this);
        return templates.getInstanceOf("assign")
                .add("e", expressionEval)
                .add("offset", classMemberDef.id.getSymbol().offset);
    }

    @Override
    public ST visit(Formal formal) {
        return null;
    }

    @Override
    public ST visit(Type type) {
        ST defaultLabel = templates.getInstanceOf("sequence");
        String name = type.token.getText();

        if (name.equals("Int")) {
            defaultLabel.add("e", getIntLabel(0));
        } else if (name.equals("String")) {
            defaultLabel.add("e", getStringLabel(""));
        } else if (name.equals("Bool")) {
            defaultLabel.add("e", getBoolLabel(false));
        } else {
            defaultLabel.add("e", 0);
        }

        return defaultLabel;
    }

    @Override
    public ST visit(ClassDef classDef) {
        // Create a COOL String instance for class name
//        classDef.features.forEach(x -> x.accept(this));
        String className = classDef.id.token.getText();
        ClassSymbol symbol = ResolutionPassVisitor.getClassSymbol(SymbolTable.globals.lookup(className));

        ST initializations = templates.getInstanceOf("sequence");
        ST protObj = templates.getInstanceOf("protObj")
                .add("className", className)
                .add("tag", CodeGenUtils.classesTags.get(className))
                .add("size", symbol.allMembers.size() + 3);

        boolean ok = true;

        for (Feature feature : classDef.features) {
            if (feature instanceof ClassMemberDef) {
                initializations.add("e", feature.accept(this));
                protObj.add("members", ((ClassMemberDef) feature).type.accept(this));
                ok = false;
            } else {
                feature.accept(this);
            }
        }

        protObj.add("isEmpty", ok);

        textSection.add("e", templates.getInstanceOf("instanceInit")
                .add("class", className)
                .add("baseClass", symbol.getBaseClass())
                .add("initializations", initializations));


        return protObj;
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
