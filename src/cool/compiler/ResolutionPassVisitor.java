package cool.compiler;

import cool.parser.ASTVisitor;
import cool.parser.nodes.*;
import cool.structures.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResolutionPassVisitor implements ASTVisitor<TypeSymbol> {
    @Override
    public TypeSymbol visit(Program program) {
        for (var stmt: program.classes)
            stmt.accept(this);

//        ClassSymbol mainClass = (ClassSymbol) SymbolTable.globals.lookup("Main");

//        if (mainClass == null || mainClass.lookupMethod("main") == null) {
//            SymbolTable.error(program.context, program.token, "No method main in class Main");
//        }

        return null;
    }

    MethodSymbol solveMethodCall(ClassSymbol classScope, ClassMethodCall classMethodCall) {
        var id = classMethodCall.id;

        MethodSymbol symbol = classScope.lookupMethod(id.token.getText());

        if (symbol == null) {
            // It is possible that the current class doesn't yet have all the inheritance hierarchy
            // formed, so we have to first make sure that it inherited everything, and then recheck
            classScope.classDef.accept(this);
            symbol = classScope.lookupMethod(id.token.getText());

            if (symbol == null)
                SymbolTable.error(classMethodCall.context, id.token, "Undefined method "
                        + id.token.getText()
                        + " in class "
                        + classScope.getName());
        }

        return symbol;
    }

    private TypeSymbol getClosestClass(Scope scope) {
        // TRICK: Search for "_self" in the current scope, in order to find
        //     the name of the current class where the method is located
        return scope.lookupMember("_self").getType();
    }

    @Override
    public TypeSymbol visit(ClassMethodCall classMethodCall) {
        var id = classMethodCall.id;
        TypeSymbol returnType = null;

        MethodSymbol symbol = null;
        ClassSymbol classScope = null;

        if (classMethodCall.implicitDispatch) {
            symbol = id.getScope().lookupMethod(id.token.getText());
            classScope = (ClassSymbol) id.getScope().lookup(getClosestClass(id.getScope()).getName());

            if (symbol == null) {
                SymbolTable.error(classMethodCall.context, classMethodCall.token, "Undefined method "
                        + id.token.getText()
                        + " in class "
                        + classScope.getName());
                return null;
            }

            returnType = symbol.getType();
        } else {
            // We need to find the type of the dispatch expression
            TypeSymbol exprType = classMethodCall.object.accept(this);

            if (exprType.equals(TypeSymbol.SELF_TYPE))
                exprType = getClosestClass(id.getScope());

            if (classMethodCall.staticType == null) {
                // Dedicated to default classes, which are TypeSymbol s
                try {
                    classScope = (ClassSymbol) SymbolTable.globals.lookup(exprType.getName());
                } catch (Exception e) {
                    classScope = ((TypeSymbol) SymbolTable.globals.lookup(exprType.getName())).classSymbol;
                }

                symbol = solveMethodCall(classScope, classMethodCall);
                if (symbol == null)
                    return null;

                returnType = symbol.getType();
            } else {
                TypeSymbol staticType = classMethodCall.staticType.accept(this);

                if (staticType == null) {
                    SymbolTable.error(classMethodCall.context, classMethodCall.staticType.token, "Type "
                            + classMethodCall.staticType.token.getText()
                            + " of static dispatch is undefined");
                    return null;
                } else if (staticType == TypeSymbol.SELF_TYPE) {
                    SymbolTable.error(classMethodCall.context, classMethodCall.staticType.token,
                            "Type of static dispatch cannot be SELF_TYPE");
                    return null;
                } else if (!checkIfSubtype(exprType, staticType)) {
                    SymbolTable.error(classMethodCall.context, classMethodCall.staticType.token, "Type "
                            + staticType
                            + " of static dispatch is not a superclass of type "
                            + exprType);
                    return null;
                }

                classScope = getClassSymbol(SymbolTable.globals.lookup(staticType.getName()));
                symbol = solveMethodCall(classScope, classMethodCall);

                if (symbol == null)
                    return null;
                returnType = symbol.getType();
            }
        }

        classMethodCall.id.setSymbol(symbol);

        // Check the number and type of actual arguments
        var formals = symbol.getFormals();

        if (classMethodCall.params.size() != formals.size()) {
            SymbolTable.error(classMethodCall.context, id.token, "Method "
                    + id.token.getText()
                    + " of class "
                    + classScope.getName()
                    + " is applied to wrong number of arguments");
        } else {
            int i = 0;
            for (Map.Entry<String, Symbol> entry: formals.entrySet()) {
                TypeSymbol actualType = classMethodCall.params.get(i).accept(this);
                TypeSymbol formalType = ((IdSymbol) entry.getValue()).getType();

                if (actualType.equals(TypeSymbol.SELF_TYPE))
                    actualType = getClosestClass(id.getScope());

                if (!checkIfSubtype(actualType, formalType)) {
                    SymbolTable.error(classMethodCall.context, classMethodCall.params.get(i).token, "In call to method "
                            + id.token.getText()
                            + " of class "
                            + classScope.getName()
                            + ", actual type "
                            + actualType
                            + " of formal parameter "
                            + entry.getKey()
                            + " is incompatible with declared type "
                            + formalType);
                }

                i += 1;
            }
        }

        if (returnType.equals(TypeSymbol.SELF_TYPE))
            if (!classMethodCall.implicitDispatch)
                // It's the same type as the dispatch expression
                returnType = classMethodCall.object.accept(this);

        return returnType;
    }

    // Look on the inheritance chain and decide if A <= B
    private boolean checkIfSubtype(TypeSymbol A, TypeSymbol B) {
        if (A.getName().equals(B.getName()))
            return true;

        // Create a link between the TypeSymbol, which doesn't store any info
        // about the class hierarchy, and the ClassSymbol
        Symbol actualSymbolA = SymbolTable.globals.lookup(A.getName());
        Symbol actualSymbolB = SymbolTable.globals.lookup(B.getName());

        if (actualSymbolA instanceof ClassSymbol) {
//            ClassSymbol baseClass = (ClassSymbol) SymbolTable.globals.lookup(
//                    ((ClassSymbol) actualSymbolA).getBaseClass());

//            while (baseClass != null && !baseClass.equals(actualSymbolA)) {
//                if (baseClass.equals(actualSymbolB))
//                    return true;
//
//                baseClass = getClassSymbol(SymbolTable.globals.lookup(baseClass.getBaseClass()));
//            }
            for (Symbol sym : getClassSymbol(actualSymbolA).nestedScopes) {
                ClassSymbol classSymbol = getClassSymbol(sym);
                if (classSymbol.equals(getClassSymbol(actualSymbolB))) {
                    return true;
                }
            }

        } else {
            return B.equals(TypeSymbol.OBJECT);
        }

        return false;
    }

    // Wrapper over the original checkIfSubtype, used to solve
    // references to SELF_TYPE with respect to the current scope
    private boolean checkIfSubtype(TypeSymbol A, TypeSymbol B, Scope scope) {
        if (A.equals(B) || B.equals(TypeSymbol.OBJECT))
            return true;

        if (A.equals(TypeSymbol.SELF_TYPE))
            A = getClosestClass(scope);
        if (B.equals(TypeSymbol.SELF_TYPE))
            return false;

        return checkIfSubtype(A, B);
    }

    @Override
    public TypeSymbol visit(Assign assign) {
        TypeSymbol requiredType = assign.id.accept(this);
        TypeSymbol expressionType = assign.expr.accept(this);

        // Don't generate redundant errors
        if (requiredType == null || expressionType == null)
            return null;

        if (!checkIfSubtype(expressionType, requiredType, assign.id.getScope())) {
            SymbolTable.error(assign.context, assign.expr.token, "Type "
                    + expressionType
                    + " of assigned expression is incompatible with declared type "
                    + requiredType
                    + " of identifier "
                    + assign.id.token.getText());
        }

        return expressionType;
    }

    @Override
    public TypeSymbol visit(UMinus uMinus) {
        var typeSymbol = uMinus.expr.accept(this);

        if (!typeSymbol.equals(TypeSymbol.INT)) {
            SymbolTable.error(uMinus.context, uMinus.expr.token,"Operand of "
                + uMinus.token.getText()
                + " has type "
                + typeSymbol
                + " instead of Int");
        }

        return TypeSymbol.INT;
    }

    @Override
    public TypeSymbol visit(New neww) {
        var typeSymbol = neww.type.accept(this);

        if (typeSymbol == null) {
            SymbolTable.error(neww.context, neww.type.token,"new is used with undefined type "
                    + neww.type.token.getText());
        }

        return typeSymbol;
    }

    @Override
    public TypeSymbol visit(IsVoid isVoid) {
        isVoid.expr.accept(this);
        return TypeSymbol.BOOL;
    }

    @Override
    public TypeSymbol visit(Not not) {
        var typeSymbol = not.expr.accept(this);

        if (typeSymbol != TypeSymbol.BOOL && typeSymbol != null) {
            SymbolTable.error(not.context, not.expr.token,"Operand of not has type "
                    + typeSymbol
                    + " instead of Bool");
        }

        return TypeSymbol.BOOL;
    }

    private TypeSymbol arithmeticHelper(Expression expr, Expression opLeft, Expression opRight) {
        var leftType = opLeft.accept(this);
        var rightType = opRight.accept(this);

        if (leftType == null || rightType == null) {
            // Don't return null, in order to allow continuing the analysis
        } else if (!leftType.equals(rightType) || !leftType.equals(TypeSymbol.INT)) {
            var wrongType = (leftType != TypeSymbol.INT ? leftType : rightType);
            var wrongTypeSideToken = (leftType != TypeSymbol.INT ? opLeft : opRight).token;

            SymbolTable.error(expr.context, wrongTypeSideToken,"Operand of "
                    + expr.token.getText()
                    + " has type "
                    + wrongType
                    + " instead of Int");
        }

        String operand = expr.token.getText();
        if (operand.equals("~") || operand.equals("<") || operand.equals("<="))
            return TypeSymbol.BOOL;
        return TypeSymbol.INT;
    }

    @Override
    public TypeSymbol visit(Mult mult) {
        return arithmeticHelper(mult, mult.left, mult.right);
    }

    @Override
    public TypeSymbol visit(Div div) {
        return arithmeticHelper(div, div.left, div.right);
    }

    @Override
    public TypeSymbol visit(Plus plus) {
        return arithmeticHelper(plus, plus.left, plus.right);
    }

    @Override
    public TypeSymbol visit(Minus minus) {
        return arithmeticHelper(minus, minus.left, minus.right);
    }

    @Override
    public TypeSymbol visit(Relational relational) {
        if (!relational.token.getText().equals("=")) {
            return arithmeticHelper(relational, relational.left, relational.right);
        } else {
            var leftType = relational.left.accept(this);
            var rightType = relational.right.accept(this);

            if (leftType == TypeSymbol.INT || leftType == TypeSymbol.STRING || leftType == TypeSymbol.BOOL
            || rightType == TypeSymbol.INT || rightType == TypeSymbol.STRING || rightType == TypeSymbol.BOOL)
            {
                if (leftType != rightType) {
                    SymbolTable.error(relational.context, relational.token,"Cannot compare "
                    + leftType
                    + " with "
                    + rightType);
                }
            }
        }

        return TypeSymbol.BOOL;
    }

    @Override
    public TypeSymbol visit(Id id) {
        var symbol = (IdSymbol) id.getScope().lookupMember(id.token.getText());

        if (symbol == null) {
            SymbolTable.error(id.context, id.token,"Undefined identifier " + id.token.getText());
            return null;
        }
        id.setSymbol(symbol);

        return symbol.getType();
    }

    @Override
    public TypeSymbol visit(Int intt) {
        return TypeSymbol.INT;
    }

    @Override
    public TypeSymbol visit(Stringg string) {
        return TypeSymbol.STRING;
    }

    @Override
    public TypeSymbol visit(Bool bool) {
        return TypeSymbol.BOOL;
    }

    @Override
    public TypeSymbol visit(ClassMethodDef classMethodDef) {
        var id = classMethodDef.id;

        for (var formal: classMethodDef.formals) {
            TypeSymbol formalType = formal.accept(this);

            if (formalType == null) {
                SymbolTable.error(formal.context, formal.type.token,"Method "
                        + id.token.getText()
                        + " of class "
                        + ((ClassSymbol) id.getScope().getParent()).getName()
                        + " has formal parameter "
                        + formal.token.getText()+
                        " with undefined type "
                        + formal.type.token.getText());
            }

            if (formalType == TypeSymbol.SELF_TYPE) {
                SymbolTable.error(formal.context, formal.type.token,"Method "
                        + id.token.getText()
                        + " of class "
                        + ((ClassSymbol) id.getScope().getParent()).getName()
                        + " has formal parameter "
                        + formal.token.getText()+
                        " with illegal type SELF_TYPE");
            }
        }

        var returnType = classMethodDef.returnType.accept(this);
        var bodyType = classMethodDef.body.accept(this);

        // Annotate the return type
        if (id.getSymbol() != null && returnType != null) {
            id.getSymbol().setType(returnType);
        }

        if (returnType != null && bodyType != null && !checkIfSubtype(bodyType, returnType, id.getScope())) {
            SymbolTable.error(classMethodDef.context, classMethodDef.body.token,"Type "
                    + bodyType
                    + " of the body of method "
                    + ((MethodSymbol) classMethodDef.id.getScope()).getName()
                    + " is incompatible with declared return type "
                    + returnType);
        }

        ClassSymbol baseClass = getClassSymbol(SymbolTable.globals.lookup(
                ((ClassSymbol) id.getScope().getParent()).getBaseClass()));

        while (baseClass != null && !baseClass.getName().equals(id.token.getText())) {
            MethodSymbol baseMethod = baseClass.lookupMethod(id.token.getText());

            if (baseMethod != null) {
                // Check the number of parameters
                if (baseMethod.getFormals().size() != classMethodDef.formals.size()) {
                    SymbolTable.error(classMethodDef.context, classMethodDef.token, "Class "
                            + ((ClassSymbol) id.getScope().getParent()).getName()
                            + " overrides method "
                            + id.token.getText()
                            + " with different number of formal parameters");
                } else {
                    int cnt = 0;

                    // Check the type of the parameters
                    for (var formal: baseMethod.getFormals().entrySet()) {
                        if (((IdSymbol) formal.getValue()).getType() != classMethodDef.formals.get(cnt).type.accept(this)) {
                            SymbolTable.error(classMethodDef.context, classMethodDef.formals.get(cnt).type.token, "Class "
                                    + ((ClassSymbol) id.getScope().getParent()).getName()
                                    + " overrides method "
                                    + id.token.getText()
                                    + " but changes type of formal parameter "
                                    + classMethodDef.formals.get(cnt).token.getText()
                                    + " from "
                                    + ((IdSymbol) formal.getValue()).getType()
                                    + " to "
                                    + classMethodDef.formals.get(cnt).type.accept(this));
                        }

                        cnt += 1;
                    }

                    // Check the return type
                    if (returnType != baseMethod.getType()) {
                        SymbolTable.error(classMethodDef.context, classMethodDef.returnType.token, "Class "
                                + ((ClassSymbol) id.getScope().getParent()).getName()
                                + " overrides method "
                                + id.token.getText()
                                + " but changes return type from "
                                + baseMethod.getType()
                                + " to "
                                + returnType);
                    }
                }
            }

            baseClass = getClassSymbol(SymbolTable.globals.lookup(baseClass.getBaseClass()));
        }

        return returnType;
    }

    @Override
    public TypeSymbol visit(ClassMemberDef classMemberDef) {
        var id = classMemberDef.id;
        var type = classMemberDef.type;

        // Check if the variable is already defined in a base class
        String currentClassName = ((ClassSymbol) classMemberDef.id.getScope()).getName();
        String baseClassName = ((ClassSymbol) classMemberDef.id.getScope()).getBaseClass();


        while (baseClassName != null && SymbolTable.globals.lookup(baseClassName) instanceof ClassSymbol baseClassSymbol) {

            if (baseClassSymbol.lookupMember(id.token.getText()) != null) {
                SymbolTable.error(classMemberDef.context, classMemberDef.token,"Class "
                        + currentClassName
                        + " redefines inherited attribute "
                        + id.token.getText());
                return null;
            }

            baseClassName = baseClassSymbol.getBaseClass();
        }

        // Check if the type of this class member is valid
        var typeSymbol = type.accept(this);
        if (typeSymbol == null) {
            SymbolTable.error(classMemberDef.context, type.token,"Class "
                    + currentClassName
                    + " has attribute "
                    + id.token.getText()
                    + " with undefined type "
                    + type.token.getText());
            return null;
        }

        // Annotate the type
        var symbol = id.getSymbol();
        if (symbol != null)
            symbol.setType(typeSymbol);

        // Propagate the analysis to the initializing expression
        if (classMemberDef.initExpr != null) {
            TypeSymbol exprTypeSymbol = classMemberDef.initExpr.accept(this);

            if (exprTypeSymbol == null)
                return typeSymbol;

            if (!checkIfSubtype(exprTypeSymbol, typeSymbol, classMemberDef.id.getScope())) {
                SymbolTable.error(classMemberDef.context, classMemberDef.initExpr.token, "Type "
                        + exprTypeSymbol
                        + " of initialization expression of attribute "
                        + id.token.getText()
                        + " is incompatible with declared type "
                        + typeSymbol);
                return typeSymbol;
            }
        }

        return typeSymbol;
    }

    @Override
    public TypeSymbol visit(Formal formal) {
        TypeSymbol typeSymbol = formal.type.accept(this);

        if (typeSymbol != null)
            formal.id.getSymbol().setType(typeSymbol);
        return typeSymbol;
    }

    @Override
    public TypeSymbol visit(Type type) {
        TypeSymbol typeSymbol = null;

        if (type.token.getText().equals("Int")) {
            typeSymbol = TypeSymbol.INT;
        } else if (type.token.getText().equals("String")) {
            typeSymbol = TypeSymbol.STRING;
        } else if (type.token.getText().equals("Bool")) {
            typeSymbol = TypeSymbol.BOOL;
        } else if (type.token.getText().equals("SELF_TYPE")) {
            typeSymbol = TypeSymbol.SELF_TYPE;
        } else if (type.token.getText().equals("Object")) {
            typeSymbol = TypeSymbol.OBJECT;
        } else if (SymbolTable.globals.lookup(type.token.getText()) != null) {
            typeSymbol = new TypeSymbol(type.token.getText());
        }

        return typeSymbol;
    }

    @Override
    public TypeSymbol visit(ClassDef classDef) {
        for (Type baseClass: classDef.baseClasses) {
            if (baseClass.accept(this) == null) {
                SymbolTable.error(baseClass.context, baseClass.token, "Class "
                        + classDef.token.getText()
                        + " has undefined parent "
                        + baseClass.token.getText()
                );
                return null;
            }

            if (TypeSymbol.illegalInheritance.contains(baseClass.token.getText())) {
                SymbolTable.error(baseClass.context, baseClass.token, "Class "
                        + classDef.token.getText()
                        + " has illegal parent "
                        + baseClass.token.getText()
                );
                return null;
            }
        }

        // Check cyclic inheritance and add all the parent classes scopes
        // to create a list of nested scopes
        for (Type baseClass: classDef.baseClasses) {
            String currentClass = baseClass.token.getText();

            while (true) {
                Symbol currentClassSymbol = SymbolTable.globals.lookup(currentClass);

                if (currentClassSymbol instanceof ClassSymbol) {
                    // Nest all the scopes from the inheritance chain
                    ((ClassSymbol) classDef.id.getScope()).nestedScopes.add((ClassSymbol) currentClassSymbol);

                    currentClass = ((ClassSymbol) currentClassSymbol).getBaseClass();
                } else if (currentClassSymbol instanceof TypeSymbol) {
                    ((ClassSymbol) classDef.id.getScope()).nestedScopes
                            .add(((TypeSymbol) currentClassSymbol).classSymbol);

                    currentClass = ((TypeSymbol) currentClassSymbol).classSymbol.getBaseClass();
                } else {
                    System.out.println("Unknown inheritance");
                    return null;
                }

                if (currentClass == null)
                    break;

                if (currentClass.equals(classDef.token.getText())) {
                    SymbolTable.error(classDef.context, classDef.token, "Inheritance cycle for class "
                            + classDef.token.getText()
                    );
                    return null;
                }
            }
        }

        // Set used to avoid generating redundant errors for the same wrong types
        Set<String> inexistentTypes = new HashSet<>();

        for (var feature : classDef.features) {
            if (feature instanceof ClassMemberDef) {
                String maybeType = ((ClassMemberDef) feature).type.token.getText();

                if (inexistentTypes.contains(maybeType)) {
                    continue;
                }
            }

            TypeSymbol type = feature.accept(this);

            if (type == null && feature instanceof ClassMemberDef)
                // Make sure that expression type doesn't exist (visitor might
                // also return null if semantic analysis failed)
                if (SymbolTable.globals.lookup(((ClassMemberDef) feature).type.token.getText()) == null)
                    inexistentTypes.add(((ClassMemberDef) feature).type.token.getText());
        }

        return null;
    }

    private TypeSymbol getTypesLCAHelper(TypeSymbol root, ClassSymbol A, ClassSymbol B, Scope currentScope) {
        // Apply the LCA algo
        ClassSymbol baseClassA = (ClassSymbol) SymbolTable.globals.lookup(
                A.getBaseClass());

        // Go up on one branch until you find the other type between the
        // children of the other branch, or you've reached Object (also avoid cycles)
        while (baseClassA != null && !baseClassA.equals(A)) {
            // Check the inheritance chain for B and try to find the current ancestor of A
            ClassSymbol baseClassB = (ClassSymbol) SymbolTable.globals.lookup(
                    A.getBaseClass());
            while (baseClassB != null && !baseClassB.equals(B)) {
                if (baseClassB.equals(baseClassA))
                    return new TypeSymbol(baseClassA.getName());

                baseClassB = (ClassSymbol) SymbolTable.globals.lookup(baseClassA.getBaseClass());
            }

            baseClassA = (ClassSymbol) SymbolTable.globals.lookup(baseClassA.getBaseClass());
        }

        return root;
    }

    // Find the Lowest Common Ancestor between types A and B
    private TypeSymbol getTypesLCA(TypeSymbol A, TypeSymbol B, Scope currentScope) {
        if (A == null || B == null)
            return null;

        if (A.equals(B))
            return A;

        // A is subtype of B
        if (checkIfSubtype(A, B, currentScope))
            return B;

        // B is subtype of A
        if (checkIfSubtype(B, A, currentScope))
            return A;

        Symbol actualSymbolA = SymbolTable.globals.lookup(A.getName());
        Symbol actualSymbolB = SymbolTable.globals.lookup(B.getName());

        if (actualSymbolA instanceof ClassSymbol && actualSymbolB instanceof ClassSymbol) {
            return getTypesLCAHelper(TypeSymbol.OBJECT, (ClassSymbol) actualSymbolA, (ClassSymbol) actualSymbolB, currentScope);
        }

        return TypeSymbol.OBJECT;
    }

    @Override
    public TypeSymbol visit(If iff) {
        TypeSymbol conditionType = iff.cond.accept(this);

        if (conditionType != TypeSymbol.BOOL) {
            SymbolTable.error(iff.context, iff.cond.token,"If condition has type "
                    + conditionType
                    + " instead of Bool");
        }

        TypeSymbol thenType = iff.thenBranch.accept(this);
        TypeSymbol elseType = iff.elseBranch.accept(this);

        return getTypesLCA(thenType, elseType, iff.getScope());
    }

    @Override
    public TypeSymbol visit(While whilee) {
        TypeSymbol conditionType = whilee.cond.accept(this);

        if (conditionType != TypeSymbol.BOOL) {
            SymbolTable.error(whilee.context, whilee.cond.token,"While condition has type "
                    + conditionType
                    + " instead of Bool");
        }

        // Propagate the visitor to while's body
        whilee.body.accept(this);

        return TypeSymbol.OBJECT;
    }

    @Override
    public TypeSymbol visit(LetLocalVar letLocalVar) {
        var id = letLocalVar.id;
        TypeSymbol typeSymbol = letLocalVar.type.accept(this);

        if (typeSymbol != null) {
            ((IdSymbol) letLocalVar.id.getScope().lookup(letLocalVar.id.token.getText())).setType(typeSymbol);
        } else {
            SymbolTable.error(letLocalVar.context, letLocalVar.type.token,"Let variable "
                    + id.token.getText()
                    + " has undefined type "
                    + letLocalVar.type.token.getText());
            return null;
        }

        if (letLocalVar.initExpr != null) {
            TypeSymbol exprTypeSymbol = letLocalVar.initExpr.accept(this);

            if (!checkIfSubtype(exprTypeSymbol, typeSymbol)) {
                SymbolTable.error(letLocalVar.context, letLocalVar.initExpr.token, "Type "
                        + exprTypeSymbol
                        + " of initialization expression of identifier "
                        + id.token.getText()
                        + " is incompatible with declared type "
                        + typeSymbol);
                return null;
            }
        }

        return typeSymbol;
    }

    @Override
    public TypeSymbol visit(Let let) {
        Scope prevScope = let.getScope();

        // TRICK: In order to avoid not knowing which is the
        // type of let's body, because an inner scope uses
        // a wrong type for a local variable, we replace the
        // scope with the most recent valid scope of a let local var
        for (LetLocalVar localVar : let.localVars) {
            TypeSymbol localVarType = localVar.accept(this);

            // Replace its scope with the previous one, which is valid
            if (localVarType == null) {
                localVar.id.setScope(prevScope);
            } else {
                prevScope = localVar.id.getScope();
            }
        }

        let.body.setScope(prevScope);
        return let.body.accept(this);
    }

    @Override
    public TypeSymbol visit(Case casee) {
        TypeSymbol branchesTypesLCA = casee.branches.get(0).accept(this);

        for (var branch: casee.branches) {
            TypeSymbol branchTypeSymbol = branch.accept(this);

            branchesTypesLCA = getTypesLCA(branchTypeSymbol, branchesTypesLCA, casee.getScope());
        }

        casee.expr.accept(this);

        return branchesTypesLCA;
    }

    @Override
    public TypeSymbol visit(CaseBranch caseBranch) {
        var id = caseBranch.id;

        if (id.token.getText().equals("self")) {
            SymbolTable.error(caseBranch.context, id.token,"Case variable has illegal name self");
        }

        var typeSymbol = caseBranch.type.accept(this);

        if (typeSymbol == null) {
            SymbolTable.error(caseBranch.context, caseBranch.type.token,"Case variable "
                    + id.token.getText()
                    + " has undefined type "
                    + caseBranch.type.token.getText());
        } else if (typeSymbol == TypeSymbol.SELF_TYPE) {
            SymbolTable.error(caseBranch.context, caseBranch.type.token,"Case variable "
                    + id.token.getText()
                    + " has illegal type "
                    + caseBranch.type.token.getText());
        } else {
            id.getSymbol().setType(typeSymbol);
        }

        return caseBranch.body.accept(this);
    }

    @Override
    public TypeSymbol visit(Block block) {
        TypeSymbol lastType = null;

        for (var expr: block.expressions)
            lastType = expr.accept(this);

        return lastType;
    }

    public static ClassSymbol getClassSymbol(Symbol s) {
        ClassSymbol result = null;

        try {
            result = (ClassSymbol) s;
        } catch(Exception e) {
            result = ((TypeSymbol) s).classSymbol;
        }

        return result;
    }
}
