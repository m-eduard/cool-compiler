package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class While extends Expression {
    public Expression cond;
    public Expression body;

    public While(Expression cond, Expression body, Token token, ParserRuleContext context) {
        super(token, context);
        this.cond = cond;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
