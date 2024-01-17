package cool.structures;

public interface Scope {
    public boolean add(Symbol sym);

    public Symbol lookup(String str);

    public IdSymbol lookupMember(String name);

    public MethodSymbol lookupMethod(String name);

    public Scope getParent();

    public default int getLastUnusedLocalVarsOffset() {
        return -4;
    }

    public default int nextUnusedLocalVarsOffset() {
        return -4;
    }
}
