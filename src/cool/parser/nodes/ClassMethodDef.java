package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class ClassMethodDef extends Feature {
    public final String label = "method";
    public Type returnType;
    public Id id;
    public List<Formal> formals;
    public Expression body;

    public ClassMethodDef(Type returnType, Id id, List<Formal> formals, Expression body,
                          Token token, ParserRuleContext context) {
        super(token, context);
        this.returnType = returnType;
        this.id = id;
        this.formals = formals;
        this.body = body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
