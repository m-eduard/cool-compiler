package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class Assign extends Expression {
    public Id id;
    public Expression expr;

    public Assign(Id id, Expression expr, Token token, ParserRuleContext context) {
        super(token, context);
        this.id = id;
        this.expr = expr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
