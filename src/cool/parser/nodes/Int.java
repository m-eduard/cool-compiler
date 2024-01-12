package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class Int extends Expression {
    public Int(Token token, ParserRuleContext context) {
        super(token, context);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
