package cool.parser.nodes;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public abstract class Feature extends Definition {
    public Feature(Token token, ParserRuleContext context) {
        super(token, context);
    }
}
