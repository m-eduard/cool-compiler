package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

public class ClassDef extends Definition {
    public List<Type> baseClasses;

    public final String label = "class";
    public Id id;

    public List<Feature> features;

    public ClassDef(Id id, List<Type> baseClasses, List<Feature> features, Token token, ParserRuleContext context) {
        super(token, context);
        this.id = id;
        this.baseClasses = baseClasses;
        this.features = features;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
