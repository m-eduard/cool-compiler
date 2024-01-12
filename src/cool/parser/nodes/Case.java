package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class Case extends Expression {
    public Expression expr;
    public List<CaseBranch> branches;

    public Case(Expression expr, List<CaseBranch> branches,
                Token token, ParserRuleContext context) {
        super(token, context);
        this.expr = expr;
        this.branches = branches;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
