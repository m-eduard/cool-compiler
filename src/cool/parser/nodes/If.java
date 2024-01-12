package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class If extends Expression {
    public Expression cond;
    public Expression thenBranch;
    public Expression elseBranch;

    public If(Expression cond, Expression thenBranch, Expression elseBranch,
              Token token, ParserRuleContext context) {
        super(token, context);
        this.cond = cond;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
