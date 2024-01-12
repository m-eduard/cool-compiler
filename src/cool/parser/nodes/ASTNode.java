package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public abstract class ASTNode {
    public Token token;
    public ParserRuleContext context;

    public ASTNode(Token token, ParserRuleContext context) {
        this.token = token;
        this.context = context;
    }

    public abstract <T> T accept(ASTVisitor<T> visitor);
}
