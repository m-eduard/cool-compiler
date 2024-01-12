package cool.compiler;

import cool.parser.CoolParser;
import cool.parser.CoolParserBaseVisitor;
import cool.parser.nodes.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class ASTConstructionVisitor extends CoolParserBaseVisitor<ASTNode> {
    @Override public ASTNode visitProgram(CoolParser.ProgramContext ctx) {
        List<ClassDef> classes = new ArrayList<>();

        for (var classDef : ctx.classes) {
            classes.add((ClassDef) visit(classDef));
        }

        return new Program(classes, ctx.start, ctx.getParent());
    }

    @Override public ASTNode visitClass(CoolParser.ClassContext ctx) {
        List<Type> baseClasses = new ArrayList<>();
        List<Feature> features = new ArrayList<>();

        for (Token baseClass : ctx.base) {
            baseClasses.add(new Type(baseClass, ctx));
        }

        for (var feature : ctx.features) {
            features.add((Feature) visit(feature));
        }

        return new ClassDef(new Id(ctx.name, ctx), baseClasses, features, ctx.name, ctx);
    }

    @Override public ASTNode visitMethodDef(CoolParser.MethodDefContext ctx) {
        List<Formal> formals = new ArrayList<>();

        for (var formal : ctx.formals) {
            formals.add((Formal) visit(formal));
        }

        return new ClassMethodDef(new Type(ctx.type, ctx.getParent()), new Id(ctx.name, ctx.getParent()),
                formals, (Expression) visit(ctx.body), ctx.start, ctx.getParent());
    }

    @Override public ASTNode visitMemberDef(CoolParser.MemberDefContext ctx) {
        Expression initExpr = null;
        Token token = ctx.lval.name;

        if (ctx.init != null) {
            initExpr = (Expression) visit(ctx.init);
            token = ctx.ASSIGN().getSymbol();
        }

        return new ClassMemberDef(
                new Type(ctx.lval.type, ctx.lval),
                new Id(ctx.lval.name, ctx.lval),
                initExpr,
                token, ctx
        );
    }

    @Override public ASTNode visitImplicitDispatch(CoolParser.ImplicitDispatchContext ctx) {
        List<Expression> params = new ArrayList<>();

        for (var param : ctx.args) {
            params.add((Expression) visit(param));
        }

        return new ClassMethodCall(new Id(ctx.name, ctx.getParent()), params,
                ctx.start, ctx.getParent());
    }

    @Override public ASTNode visitExplicitDispatch(CoolParser.ExplicitDispatchContext ctx) {
        List<Expression> params = new ArrayList<>();

        for (var param : ctx.args) {
            params.add((Expression) visit(param));
        }

        Type dispatchStaticType = ctx.t == null ? null : new Type(ctx.t, ctx.getParent());

        return new ClassMethodCall(
                (Expression) visit(ctx.obj),
                dispatchStaticType,
                new Id(ctx.name, ctx.getParent()),
                params, ctx.obj.start, ctx.getParent());
    }

    @Override public ASTNode visitFormal(CoolParser.FormalContext ctx) {
        return new Formal(new Type(ctx.type, ctx.getParent()), new Id(ctx.name, ctx.getParent()),
                ctx.start, ctx.getParent());
    }

    @Override public ASTNode visitAssign(CoolParser.AssignContext ctx) {
        return new Assign(new Id(ctx.name, ctx.getParent()), (Expression) visit(ctx.e),
                ctx.ASSIGN().getSymbol(), ctx.getParent());
    }

    @Override public ASTNode visitUMinus(CoolParser.UMinusContext ctx) {
        return new UMinus((Expression) visit(ctx.e), ctx.UMINUS().getSymbol(), ctx.getParent());
    }

    @Override public ASTNode visitNew(CoolParser.NewContext ctx) {
        return new New(new Type(ctx.t, ctx.getParent()), ctx.NEW().getSymbol(), ctx.getParent());
    }

    @Override public ASTNode visitIsVoid(CoolParser.IsVoidContext ctx) {
        return new IsVoid((Expression) visit(ctx.e), ctx.ISVOID().getSymbol(), ctx.getParent());
    }

    @Override public ASTNode visitNot(CoolParser.NotContext ctx) {
        return new Not((Expression) visit(ctx.e), ctx.NOT().getSymbol(), ctx.getParent());
    }

    @Override public ASTNode visitMultDiv(CoolParser.MultDivContext ctx) {
        if (ctx.op.getText().equals("*")) {
            return new Mult((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                    ctx.op, ctx.getParent());
        } else if (ctx.op.getText().equals("/")) {
            return new Div((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                    ctx.op, ctx.getParent());
        }

        return null;
    }

    @Override public ASTNode visitPlusMinus(CoolParser.PlusMinusContext ctx) {
        if (ctx.op.getText().equals("+")) {
            return new Plus((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                    ctx.op, ctx.getParent());
        } else if (ctx.op.getText().equals("-")) {
            return new Minus((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                    ctx.op, ctx.getParent());
        }

        return null;
    }

    @Override public ASTNode visitRelational(CoolParser.RelationalContext ctx) {
        return new Relational((Expression) visit(ctx.left), (Expression) visit(ctx.right),
                ctx.op, ctx.getParent());
    }

    @Override public ASTNode visitNested(CoolParser.NestedContext ctx) {
        return visit(ctx.nested);
    }

    @Override public ASTNode visitId(CoolParser.IdContext ctx) {
        return new Id(ctx.ID().getSymbol(), (ParserRuleContext) ctx.getRuleContext());
    }

    @Override public ASTNode visitInt(CoolParser.IntContext ctx) {
        if (ctx.INT() != null) {
            return new Int(ctx.INT().getSymbol(), ctx.getParent());
        }

        return visitChildren(ctx);
    }

    @Override public ASTNode visitString(CoolParser.StringContext ctx) {
        if (ctx.STRING() != null) {
            return new Stringg(ctx.STRING().getSymbol(), ctx.getParent());
        }

        return visitChildren(ctx);
    }

    @Override public ASTNode visitBool(CoolParser.BoolContext ctx) {
        if (ctx.BOOL() != null) {
            return new Bool(ctx.BOOL().getSymbol(), ctx.getParent());
        }

        return visitChildren(ctx);
    }

    @Override public ASTNode visitIf(CoolParser.IfContext ctx) {
        return new If(
                (Expression) visit(ctx.cond),
                (Expression) visit(ctx.thenBranch),
                (Expression) visit(ctx.elseBranch),
                ctx.IF().getSymbol(), ctx.getParent());
    }

    @Override public ASTNode visitWhile(CoolParser.WhileContext ctx) {
        return new While(
                (Expression) visit(ctx.cond),
                (Expression) visit(ctx.body),
                ctx.WHILE().getSymbol(), ctx.getParent()
        );
    }

    @Override public ASTNode visitLocalVar(CoolParser.LocalVarContext ctx) {
        Expression initExpr = null;

        if (ctx.e != null) {
            initExpr = (Expression) visit(ctx.e);
        }

        return new LetLocalVar(
                new Id(ctx.lval.name, ctx.lval.getParent()),
                new Type(ctx.lval.type, ctx.lval.getParent()),
                initExpr,
                ctx.start, ctx.getParent()
        );
    }

    @Override public ASTNode visitLet(CoolParser.LetContext ctx) {
        List<LetLocalVar> localVars = new ArrayList<>();

        for (var localVar : ctx.localVars) {
            localVars.add((LetLocalVar) visit(localVar));
        }

        return new Let(localVars, (Expression) visit(ctx.body),
                ctx.LET().getSymbol(),
                ctx.getParent()
        );
    }

    @Override public ASTNode visitCase(CoolParser.CaseContext ctx) {
        // CaseBranch is just an ASTNode and does not have a direct corespondent
        // between the Context classes from the parse tree (a CaseBranch instance
        // will be created merging info from both a Formal and an Expression instance)
        List<CaseBranch> branches = new ArrayList<>();

        for (int i = 0; i < ctx.lval.size(); ++i) {
            Formal lval = (Formal) visit(ctx.lval.get(i));
            Expression rval = (Expression) visit(ctx.rval.get(i));

            branches.add(new CaseBranch(
                    lval.id,
                    lval.type,
                    rval,
                    ctx.start, ctx.getParent()
            ));
        }

        return new Case(
                (Expression) visit(ctx.e),
                branches,
                ctx.CASE().getSymbol(), ctx.getParent()
        );
    }

    @Override public ASTNode visitBlock(CoolParser.BlockContext ctx) {
        List<Expression> expressions = new ArrayList<>();

        for (var expr : ctx.expressions) {
            expressions.add((Expression) visit(expr));
        }

        return new Block(expressions, ctx.start, ctx.getParent());
    }
}

