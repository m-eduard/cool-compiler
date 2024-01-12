package cool.parser.nodes;

import cool.parser.ASTVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class Program extends ASTNode {
    public final String label = "program";
    public List<ClassDef> classes;

    public Program(List<ClassDef> classes, Token token, ParserRuleContext context) {
        super(token, context);
        this.classes = classes;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
