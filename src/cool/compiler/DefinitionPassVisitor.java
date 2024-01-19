package cool.compiler;

import cool.parser.ASTVisitor;
import cool.parser.nodes.*;
import cool.structures.*;

public class DefinitionPassVisitor implements ASTVisitor<Void> {
    Scope currentScope = null;

    @Override
    public Void visit(Program program) {
        currentScope = SymbolTable.globals;

        for (var stmt: program.classes)
            stmt.accept(this);

        return null;
    }

    @Override
    public Void visit(ClassMethodCall classMethodCall) {
        var id = classMethodCall.id;
        id.setScope(currentScope);

        if (classMethodCall.object != null)
            classMethodCall.object.accept(this);

        for (var param: classMethodCall.params)
            param.accept(this);
        return null;
    }

    @Override
    public Void visit(Assign assign) {
        assign.id.accept(this);
        assign.expr.accept(this);

        if (assign.id.token.getText().equals("self")) {
            SymbolTable.error(assign.context, assign.id.token, "Cannot assign to self");
        }

        return null;
    }

    @Override
    public Void visit(UMinus uMinus) {
        uMinus.expr.accept(this);
        return null;
    }

    @Override
    public Void visit(New neww) {
        neww.setScope(currentScope);
        return null;
    }

    @Override
    public Void visit(IsVoid isVoid) {
        isVoid.expr.accept(this);
        return null;
    }

    @Override
    public Void visit(Not not) {
        not.expr.accept(this);
        return null;
    }

    @Override
    public Void visit(Mult mult) {
        mult.left.accept(this);
        mult.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Div div) {
        div.left.accept(this);
        div.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Plus plus) {
        plus.left.accept(this);
        plus.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Minus minus) {
        minus.left.accept(this);
        minus.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Relational relational) {
        relational.left.accept(this);
        relational.right.accept(this);
        return null;
    }

    @Override
    public Void visit(Id id) {
        id.setScope(currentScope);
        return null;
    }

    @Override
    public Void visit(Int intt) {
        return null;
    }

    @Override
    public Void visit(Stringg string) {
        return null;
    }

    @Override
    public Void visit(Bool bool) {
        return null;
    }

    @Override
    public Void visit(ClassMethodDef classMethodDef) {
        var id = classMethodDef.id;

        var symbol = new MethodSymbol(classMethodDef.token.getText(), classMethodDef.formals.size(), currentScope);
        id.setScope(symbol);
        symbol.setType(new TypeSymbol(classMethodDef.returnType.token.getText()));

        if (!currentScope.add(symbol)) {
            SymbolTable.error(classMethodDef.context, classMethodDef.token, "Class "
                    + ((ClassSymbol) currentScope).getName()
                    + " redefines method "
                    + id.token.getText());
            return null;
        }
        id.setSymbol(symbol);

        // Enter the method inner scope
        currentScope = symbol;

        int formalIdx = 0;
        for (var formal: classMethodDef.formals) {
            formal.accept(this);

            formal.id.getSymbol().basePtr = "$fp";
            formal.id.getSymbol().offset = (3 + formalIdx++) * 4;
        }

        classMethodDef.body.accept(this);

        currentScope = currentScope.getParent();
        return null;
    }

    @Override
    public Void visit(ClassMemberDef classMemberDef) {
        var id = classMemberDef.id;
        id.setScope(currentScope);

        if (id.token.getText().equals("self")) {
            SymbolTable.error(classMemberDef.context, classMemberDef.token,"Class "
                    + ((ClassSymbol) currentScope).getName()
                    + " has attribute with illegal name self");
            return null;
        }

        var symbol = new IdSymbol(id.token.getText());
        if (!currentScope.add(symbol)) {
            SymbolTable.error(classMemberDef.context, classMemberDef.token,"Class "
                    + ((ClassSymbol) currentScope).getName()
                    + " redefines attribute "
                    + id.token.getText());
            return null;
        }

        id.setSymbol(symbol);

        // IMPORTANT: Temporary type annotation, errors regarding invalid
        //     types or incompatible init will be generated during the second pass
        symbol.setType(new TypeSymbol(classMemberDef.type.token.getText()));

        if (classMemberDef.initExpr != null)
            classMemberDef.initExpr.accept(this);

        return null;
    }

    @Override
    public Void visit(Formal formal) {
        var symbol = new IdSymbol(formal.token.getText());
        formal.id.setScope(currentScope);
        formal.id.setSymbol(symbol);

        // Add the temporary type, the type check will be done
        // during the second pass
        symbol.setType(new TypeSymbol(formal.type.token.getText()));

        if (!currentScope.add(symbol)) {
            SymbolTable.error(formal.context, formal.token,"Method "
                    + ((MethodSymbol) currentScope).getName()
                    + " of class "
                    + ((ClassSymbol) currentScope.getParent()).getName()
                    + " redefines formal parameter "
                    + formal.token.getText());
            return null;
        }

        if (formal.token.getText().equals("self")) {
            SymbolTable.error(formal.context, formal.token,"Method "
                    + ((MethodSymbol) currentScope).getName()
                    + " of class "
                    + ((ClassSymbol) currentScope.getParent()).getName()
                    + " has formal parameter with illegal name self");
            return null;
        }

        return null;
    }

    @Override
    public Void visit(Type type) {
        return null;
    }

    @Override
    public Void visit(ClassDef classDef) {
        var id = classDef.id;

        if (id.token.getText().equals(TypeSymbol.SELF_TYPE.getName())) {
            SymbolTable.error(classDef.context, classDef.token, "Class has illegal name " + id.token.getText());
            return null;
        }

        var symbol = classDef.baseClasses.isEmpty() ?
                new ClassSymbol(classDef.token.getText(), currentScope, classDef) :
                new ClassSymbol(classDef.token.getText(), currentScope, classDef.baseClasses.get(0).token.getText(), classDef);

        if (!currentScope.add(symbol)) {
            SymbolTable.error(classDef.context, classDef.token, "Class " + id.token.getText() + " is redefined");
            return null;
        }

        currentScope = symbol;
        id.setScope(currentScope);

        // Visit all the method and variable definitions from the current class
        for (var feature: classDef.features)
            feature.accept(this);

        currentScope = currentScope.getParent();
        return null;
    }

    @Override
    public Void visit(If iff) {
        iff.setScope(currentScope);

        iff.cond.accept(this);
        iff.thenBranch.accept(this);
        iff.elseBranch.accept(this);
        return null;
    }

    @Override
    public Void visit(While whilee) {
        whilee.cond.accept(this);
        whilee.body.accept(this);
        return null;
    }

    @Override
    public Void visit(LetLocalVar letLocalVar) {
        var id = letLocalVar.id;

        if (letLocalVar.initExpr != null)
            letLocalVar.initExpr.accept(this);

        // Change the scope only after the initialization
        // expression was visited and has the previous scope set
        currentScope = new DefaultScope(currentScope);
        id.setScope(currentScope);

        if (id.token.getText().equals("self")) {
            SymbolTable.error(letLocalVar.context, letLocalVar.token,
                    "Let variable has illegal name self");
            return null;
        }

        var symbol = new IdSymbol(letLocalVar.token.getText());
        currentScope.add(symbol);

        id.setSymbol(symbol);
        return null;
    }

    @Override
    public Void visit(Let let) {
        var initialScope = currentScope;

        for (LetLocalVar localVar : let.localVars) {
            localVar.accept(this);

            localVar.id.getSymbol().basePtr = "$fp";
            localVar.id.getSymbol().offset = localVar.id.getScope().nextUnusedLocalVarsOffset();
        }

        let.body.accept(this);

        currentScope = initialScope;
        return null;
    }

    @Override
    public Void visit(Case casee) {
        casee.setScope(currentScope);
        casee.expr.accept(this);

        for (var branch: casee.branches) {
            branch.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(CaseBranch caseBranch) {
        var id = caseBranch.id;

        currentScope = new DefaultScope(currentScope);
        id.setScope(currentScope);
        var symbol = new IdSymbol(id.token.getText());

        symbol.basePtr = "$fp";
        symbol.offset = currentScope.nextUnusedLocalVarsOffset();

        // The scope is empty, so it's safe not to check if symbol already exists
        currentScope.add(symbol);
        id.setSymbol(symbol);

        caseBranch.body.accept(this);

        currentScope = currentScope.getParent();
        return null;
    }

    @Override
    public Void visit(Block block) {
        currentScope = new DefaultScope(currentScope);;

        for (var expr : block.expressions)
            expr.accept(this);

        currentScope = currentScope.getParent();
        return null;
    }
}
