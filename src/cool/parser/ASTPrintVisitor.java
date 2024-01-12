package cool.parser;

import cool.parser.nodes.*;

public class ASTPrintVisitor implements ASTVisitor<Void> {
    int indent = 0;

    @Override
    public Void visit(Program program) {
        printIndent(program.label);
        indent++;

        for (var classDef : program.classes) {
            classDef.accept(this);
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(ClassMethodCall classMethodCall) {
        if (classMethodCall.implicitDispatch) {
            printIndent("implicit dispatch");
        } else {
            printIndent(classMethodCall.token.getText());
        }
        indent++;

        // Print the object on which dynamic dispatch is called
        // and also the static type, if it exists
        if (!classMethodCall.implicitDispatch) {
            classMethodCall.object.accept(this);

            if (classMethodCall.staticType.token != null) {
                classMethodCall.staticType.accept(this);
            }
        }

        // Print the name of the method
        classMethodCall.id.accept(this);

        for (var param : classMethodCall.params) {
            param.accept(this);
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        printIndent(assign.token.getText());
        indent++;

        assign.id.accept(this);
        assign.expr.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(UMinus uMinus) {
        printIndent(uMinus.token.getText());
        indent++;

        uMinus.expr.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(New neww) {
        printIndent(neww.token.getText());
        indent++;

        neww.type.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(IsVoid isVoid) {
        printIndent(isVoid.token.getText());
        indent++;

        isVoid.expr.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Not not) {
        printIndent(not.token.getText());
        indent++;

        not.expr.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Mult mult) {
        printIndent(mult.token.getText());
        indent++;

        mult.left.accept(this);
        mult.right.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Div div) {
        printIndent(div.token.getText());
        indent++;

        div.left.accept(this);
        div.right.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Plus plus) {
        printIndent(plus.token.getText());
        indent++;

        plus.left.accept(this);
        plus.right.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Minus minus) {
        printIndent(minus.token.getText());
        indent++;

        minus.left.accept(this);
        minus.right.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Relational relational) {
        printIndent(relational.token.getText());
        indent++;

        relational.left.accept(this);
        relational.right.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Id id) {
        printIndent(id.token.getText());

        return null;
    }

    @Override
    public Void visit(Int intt) {
        printIndent(intt.token.getText());

        return null;
    }

    @Override
    public Void visit(Stringg string) {
        printIndent(string.token.getText());

        return null;
    }

    @Override
    public Void visit(Bool bool) {
        printIndent(bool.token.getText());

        return null;
    }

    @Override
    public Void visit(ClassMethodDef classMethodDef) {
        printIndent(classMethodDef.label);
        indent++;

        printIndent(classMethodDef.id.token.getText());

        for (var formal : classMethodDef.formals) {
            formal.accept(this);
        }

        printIndent(classMethodDef.returnType.token.getText());
        classMethodDef.body.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(ClassMemberDef classMemberDef) {
        printIndent(classMemberDef.label);
        indent++;

        printIndent(classMemberDef.id.token.getText());
        printIndent(classMemberDef.type.token.getText());

        if (classMemberDef.initExpr != null) {
            classMemberDef.initExpr.accept(this);
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(Formal formal) {
        printIndent(formal.label);
        indent++;

        printIndent(formal.id.token.getText());
        printIndent(formal.type.token.getText());

        indent--;
        return null;
    }

    @Override
    public Void visit(Type type) {
        printIndent(type.token.getText());

        return null;
    }

    @Override
    public Void visit(ClassDef classDef) {
        printIndent(classDef.label);
        indent++;

        printIndent(classDef.token.getText());

        for (var baseClass : classDef.baseClasses) {
            baseClass.accept(this);
        }

        for (var feature : classDef.features) {
            feature.accept(this);
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(If iff) {
        printIndent(iff.token.getText());
        indent++;

        iff.cond.accept(this);
        iff.thenBranch.accept(this);
        iff.elseBranch.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(While whilee) {
        printIndent(whilee.token.getText());
        indent++;

        whilee.cond.accept(this);
        whilee.body.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(LetLocalVar letLocalVar) {
        printIndent(letLocalVar.label);
        indent++;

        letLocalVar.id.accept(this);
        letLocalVar.type.accept(this);

        if (letLocalVar.initExpr != null) {
            letLocalVar.initExpr.accept(this);
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(Let let) {
        printIndent(let.token.getText());
        indent++;

        for (var localVar : let.localVars) {
            localVar.accept(this);
        }
        let.body.accept(this);

        indent--;
        return null;
    }

    @Override
    public Void visit(Case casee) {
        printIndent(casee.token.getText());
        indent++;

        casee.expr.accept(this);

        for (var caseBranch : casee.branches) {
            caseBranch.accept(this);
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(CaseBranch caseBranch) {
        printIndent(caseBranch.label);
        indent++;

        caseBranch.id.accept(this);
        caseBranch.type.accept(this);
        caseBranch.body.accept(this);

        indent--;
        return null;
    }

    public Void visit(Block block) {
        printIndent(block.label);
        indent++;

        for (var expr : block.expressions) {
            expr.accept(this);
        }

        indent--;
        return null;
    }

    void printIndent(String str) {
        for (int i = 0; i < indent; i++)
            System.out.print("  ");
        System.out.println(str);
    }
}
