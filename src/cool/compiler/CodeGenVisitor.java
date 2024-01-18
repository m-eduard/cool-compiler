package cool.compiler;

import cool.parser.ASTVisitor;
import cool.parser.CoolParser;
import cool.parser.nodes.*;
import cool.structures.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.util.Comparator;
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
    int currentCaseLabel;

    // <class>.<method> mapped to its offset
    Map<String, Integer> methodOffset;

    public int getLabelIndex(String name) {
        // Update the old counter
        uniqueLabelCounter.put(name, uniqueLabelCounter.getOrDefault(name, -1) + 1);
        return uniqueLabelCounter.get(name);
    }

    private String getIntLabel(Integer value) {
        if (intLiteralPool.containsKey(value))
            return intLiteralPool.get(value);

        String label = "int_const" + getLabelIndex("Int");
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

        // Each String generates an instance of Integer,
        // used to store the length of the String's content
        String lengthRef = getIntLabel(value.length());

        String label = "str_const" + getLabelIndex("String");
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

        String label = "bool_const" + getLabelIndex("Bool");
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
        // Get the class context, in order to extract filename
        ParserRuleContext context = classMethodCall.context;
        while (! (context.getParent() instanceof CoolParser.ProgramContext))
            context = context.getParent();

        String filename = getStringLabel(
                    new File(Compiler.fileNames.get(context)
                ).getName());

        // Create a unique identifier for the dispatch label
        int dispatchLabel = getLabelIndex("dispatch");

        ST template = templates.getInstanceOf("methodCall")
                .add("dispatchLabel", dispatchLabel)
                .add("filename", filename)
                .add("line", classMethodCall.token.getLine());
        ST dispatchTemplate = templates.getInstanceOf("dispatch")
                .add("methodOffset", ((MethodSymbol) classMethodCall.id.getSymbol()).offsetInDispTable);
        template.add("dispatch", dispatchTemplate);
        ST actualParams = templates.getInstanceOf("sequence");
        template.add("actualParams", actualParams);

        // Evaluate and load the actual parameters
        classMethodCall.params.reversed().forEach(x -> actualParams
                .add("e", templates.getInstanceOf("pushActualParam")
                        .add("expr", x.accept(this))));

        if (classMethodCall.implicitDispatch) {

        } else {
            // Will load in $a0 the reference to object on which dispatch is done
            template.add("dispatchExpr", classMethodCall.object.accept(this));

            if (classMethodCall.staticType != null) {
                dispatchTemplate.add("staticType",
                        classMethodCall.staticType.token.getText());
            }
        }

        return template;
    }

    @Override
    public ST visit(Assign assign) {
        ST expressionEval = assign.expr.accept(this);
        return templates.getInstanceOf("assign")
                .add("e", expressionEval)
                .add("basePtr", assign.id.getSymbol().basePtr)
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

        return templates.getInstanceOf("isvoid")
                .add("expr", isVoid.expr.accept(this))
                .add("trueLabel", trueLabel)
                .add("falseLabel", falseLabel)
                .add("label", getLabelIndex("isvoid"));
    }

    @Override
    public ST visit(Not not) {
        String trueLabel = getBoolLabel(true);
        String falseLabel = getBoolLabel(false);

        return templates.getInstanceOf("not")
                .add("expr", not.expr.accept(this))
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
                .add("op", "addu");
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
        String trueLabel = getBoolLabel(true);
        String falseLabel = getBoolLabel(false);

        if (relational.token.getText().equals("<")) {
            return templates.getInstanceOf("lessOrMaybeEqual")
                    .add("e1", relational.left.accept(this))
                    .add("e2", relational.right.accept(this))
                    .add("trueLabel", trueLabel)
                    .add("falseLabel", falseLabel)
                    .add("label", getLabelIndex("lessOrMaybeEqual"))
                    .add("op", "blt");
        }

        if (relational.token.getText().equals("<=")) {
            return templates.getInstanceOf("lessOrMaybeEqual")
                    .add("e1", relational.left.accept(this))
                    .add("e2", relational.right.accept(this))
                    .add("trueLabel", trueLabel)
                    .add("falseLabel", falseLabel)
                    .add("label", getLabelIndex("lessOrMaybeEqual"))
                    .add("op", "ble");
        }

        return templates.getInstanceOf("equal")
                .add("e1", relational.left.accept(this))
                .add("e2", relational.right.accept(this))
                .add("trueLabel", trueLabel)
                .add("falseLabel", falseLabel)
                .add("label", getLabelIndex("equal"));
    }

    @Override
    public ST visit(Id id) {
        return templates.getInstanceOf("id")
                .add("base", id.getSymbol().basePtr)
                .add("offset", id.getSymbol().offset)
                .add("isSelf", "self".equals(id.token.getText()));
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
        return templates.getInstanceOf("loadInstance")
                .add("e", getBoolLabel(bool.token
                .getText().equals("true")));
    }

    @Override
    public ST visit(ClassMethodDef classMethodDef) {
        ClassSymbol enclosingClass = (ClassSymbol)(classMethodDef.id.getScope().getParent());

        ST methodDef = templates.getInstanceOf("methodDefinition")
                .add("className", enclosingClass.getName())
                .add("methodName", classMethodDef.id.token.getText())
                .add("body", classMethodDef.body.accept(this))
                .add("freeStackSize", classMethodDef.formals.size() * 4 + 12);

        textSection.add("e", methodDef);

        return null;
    }

    @Override
    public ST visit(ClassMemberDef classMemberDef) {
        if (classMemberDef.initExpr == null)
            return null;

        return templates.getInstanceOf("classMemberInit")
                .add("e", classMemberDef.initExpr.accept(this))
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

    public String getDefaultInstance(String typeName) {
        if (typeName.equals("Int")) return getIntLabel(0);
        if (typeName.equals("String")) return getStringLabel("");
        if (typeName.equals("Bool")) return getBoolLabel(false);

        return "0";
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

        boolean ok = symbol.allMembers.isEmpty();

        for (IdSymbol member: symbol.allMembers) {
            protObj.add("members", getDefaultInstance(member.type.getName()));
        }

        for (Feature feature : classDef.features) {
            if (feature instanceof ClassMemberDef)
                initializations.add("e", feature.accept(this));
            else
                feature.accept(this);
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
        return templates.getInstanceOf("iff")
                .add("cond", iff.cond.accept(this))
                .add("thenBranch", iff.thenBranch.accept(this))
                .add("elseBranch", iff.elseBranch.accept(this))
                .add("label", getLabelIndex("if"));
    }

    @Override
    public ST visit(While whilee) {
        return templates.getInstanceOf("while")
                .add("cond", whilee.cond.accept(this))
                .add("body", whilee.body.accept(this))
                .add("label", getLabelIndex("while"));
    }

    @Override
    public ST visit(LetLocalVar letLocalVar) {
        ST initCode = null;

        if (letLocalVar.initExpr != null)
            initCode = letLocalVar.initExpr.accept(this);
        else
            initCode = templates.getInstanceOf("loadDefaultValue")
                            .add("instance", getDefaultInstance(letLocalVar.type.token.getText()));

        return templates.getInstanceOf("initializeLocalVar")
                .add("initCode", initCode)
                .add("basePtr", letLocalVar.id.getSymbol().basePtr)
                .add("offset", letLocalVar.id.getSymbol().offset);
    }

    @Override
    public ST visit(Let let) {
        ST template = templates.getInstanceOf("let")
                .add("freeStackSize", let.localVars.size() * 4);
        template.add("body", let.body.accept(this));

        let.localVars.forEach(x -> template.add(
                "localVars", x.accept(this)));

        return template;
    }

    @Override
    public ST visit(Case casee) {
        // Get the class context, in order to extract filename
        ParserRuleContext context = casee.context;
        while (! (context.getParent() instanceof CoolParser.ProgramContext))
            context = context.getParent();

        String filename = getStringLabel(
                new File(Compiler.fileNames.get(context)
                ).getName());

        // Sort branches based on the ordered ranges for types
        casee.branches.sort(Comparator.comparingInt(x -> CodeGenUtils.classesRanges.keySet().stream().toList().indexOf(x.type.token.getText())));

        ST template = templates.getInstanceOf("case")
                .add("expr", casee.expr.accept(this))
                .add("branches", casee.branches.stream()
                        .map(x -> x.accept(this)).toList())
                .add("label", currentCaseLabel)
                .add("filename", filename)
                .add("line", casee.token.getLine())
                // Each symbol corresponding to a case branch
                // has the same offset
                .add("localOffset", casee.branches.get(0).id.getSymbol().offset);

        currentCaseLabel++;
        return template;
    }

    @Override
    public ST visit(CaseBranch caseBranch) {
        Pair<Integer, Integer> range = CodeGenUtils.classesRanges.get(
                caseBranch.type.token.getText());

        return templates.getInstanceOf("caseBranch")
                .add("body", caseBranch.body.accept(this))
                .add("start", range.first)
                .add("end", range.second)
                .add("label", getLabelIndex("caseBranch"))
                .add("caseEndLabel", currentCaseLabel)
                .add("offset", caseBranch.id.getSymbol().offset);
    }

    @Override
    public ST visit(Block block) {
        ST seq = templates.getInstanceOf("sequence");

        block.expressions
                .forEach(x -> seq.add("e", x.accept(this)));

        return seq;
    }
}
