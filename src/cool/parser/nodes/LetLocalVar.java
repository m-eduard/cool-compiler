package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class LetLocalVar extends Definition {
    public final String label = "local";
    public Id id;
    public Type type;
    public Expression initExpr;

    public LetLocalVar(Id id, Type type, Expression initExpr,
                       Token token, ParserRuleContext context) {
        super(token, context);
        this.id = id;
        this.type = type;
        this.initExpr = initExpr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
