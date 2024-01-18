package cool.structures;

public class IdSymbol extends Symbol {
    public TypeSymbol type;
    public String basePtr;
    public int offset;

    public IdSymbol(String name) {
        super(name);
    }

    public void setType(TypeSymbol type) {
        this.type = type;
    }

    public TypeSymbol getType() {
        return type;
    }
}
