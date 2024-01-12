package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class Formal extends ASTNode {
    public final String label = "formal";
    public Type type;
    public Id id;

    public Formal(Type type, Id id, Token token, ParserRuleContext context) {
        super(token, context);
        this.type = type;
        this.id = id;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
