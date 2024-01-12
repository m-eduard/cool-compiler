package cool.parser;

import cool.parser.nodes.*;

public interface ASTVisitor<T> {
    T visit(Program program);
    T visit(ClassMethodCall classMethodCall);
    T visit(Assign assign);
    T visit(UMinus uMinus);
    T visit(New neww);
    T visit(IsVoid isVoid);
    T visit(Not not);
    T visit(Mult mult);
    T visit(Div div);
    T visit(Plus plus);
    T visit(Minus minus);
    T visit(Relational relational);
    T visit(Id id);
    T visit(Int intt);
    T visit(Stringg string);
    T visit(Bool bool);
    T visit(ClassMethodDef classMethodDef);
    T visit(ClassMemberDef classMemberDef);
    T visit(Formal formal);
    T visit(Type type);
    T visit(ClassDef classDef);
    T visit(If iff);
    T visit(While whilee);
    T visit(LetLocalVar letLocalVar);
    T visit(Let let);
    T visit(Case casee);
    T visit(CaseBranch caseBranch);
    T visit(Block block);
}
