package cool.parser.nodes;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public abstract class Definition extends ASTNode {
    public Definition(Token token, ParserRuleContext context) {
        super(token, context);
    }
}
