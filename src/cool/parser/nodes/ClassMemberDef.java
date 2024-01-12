package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class ClassMemberDef extends Feature {
    public final String label = "attribute";
    public Type type;
    public Id id;
    public Expression initExpr;

    public ClassMemberDef(Type type, Id id, Expression initExpr, Token token, ParserRuleContext context) {
        super(token, context);
        this.type = type;
        this.id = id;
        this.initExpr = initExpr;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
