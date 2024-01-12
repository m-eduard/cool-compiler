package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class CaseBranch extends ASTNode {
    public final String label = "case branch";
    public Id id;
    public Type type;
    public Expression body;

    public CaseBranch(Id id, Type type, Expression body,
                      Token token, ParserRuleContext context) {
        super(token, context);
        this.id = id;
        this.type = type;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
