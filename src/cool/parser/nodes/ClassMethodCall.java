package cool.parser.nodes;

import cool.parser.ASTVisitor;
import cool.structures.IdSymbol;
import cool.structures.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class ClassMethodCall extends Expression {
    public Boolean implicitDispatch;
    public Expression object;
    public Type staticType;
    public Id id;
    public List<Expression> params;

    public ClassMethodCall(Boolean implicitDispatch, Expression object,
                           Type staticType, Id id, List<Expression> params,
                           Token token, ParserRuleContext context) {
        super(token, context);
        this.implicitDispatch = implicitDispatch;
        this.object = object;
        this.staticType = staticType;
        this.id = id;
        this.params = params;
    }

    public ClassMethodCall(Expression object,
                           Type staticType, Id id, List<Expression> params,
                           Token token, ParserRuleContext context) {
        this(false, object, staticType, id, params, token, context);
    }

    // Constructor dedicated to implicit dispatch
    public ClassMethodCall(Id id, List<Expression> params,
                           Token token, ParserRuleContext context) {
        this(true, null, null, id, params, token, context);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
