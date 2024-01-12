package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class Mult extends Expression {
    public Expression left;
    public Expression right;

    public Mult(Expression left, Expression right, Token token, ParserRuleContext context) {
        super(token, context);
        this.left = left;
        this.right = right;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
