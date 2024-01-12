package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class Let extends Expression {
    public List<LetLocalVar> localVars;
    public Expression body;

    public Let(List<LetLocalVar> localVars, Expression body,
               Token token, ParserRuleContext context) {
        super(token, context);
        this.localVars = localVars;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
