package cool.structures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodSymbol extends IdSymbol implements Scope {
    private Map<String, Symbol> symbols = new LinkedHashMap<>();
    private int numberOfFormals = 0;
    private Scope parent;

    public int lastUnusedLocalVarsOffset = -4;

    public int offsetInDispTable;
    
    public MethodSymbol(String name, int numberOfFormals, Scope parent) {
        super(name);
        this.numberOfFormals = numberOfFormals;
        this.parent = parent;
    }

    @Override
    public boolean add(Symbol sym) {
        if (symbols.containsKey(sym.getName()))
            return false;

        symbols.put(sym.getName(), sym);
        return true;
    }

    @Override
    public Symbol lookup(String name) {
        var sym = symbols.get(name);

        if (sym != null)
            return sym;

        if (parent != null)
            return parent.lookup(name);

        return null;
    }

    @Override
    public IdSymbol lookupMember(String name) {
        var sym = symbols.get(name);

        if (sym != null)
            return (IdSymbol) sym;

        if (parent != null)
            return parent.lookupMember(name);

        return null;
    }

    @Override
    public MethodSymbol lookupMethod(String name) {
        var sym = symbols.get(name);

        if (sym != null)
            return (MethodSymbol) sym;

        if (parent != null)
            return parent.lookupMethod(name);

        return null;
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    public Map<String, Symbol> getFormals() {
        return symbols.entrySet()
                .stream()
                .limit(numberOfFormals)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new
                ));
    }

    @Override
    public String toString() {
        return symbols.values().toString();
    }

    @Override
    public int getLastUnusedLocalVarsOffset() {
        return lastUnusedLocalVarsOffset;
    }

    @Override
    public int nextUnusedLocalVarsOffset() {
        this.lastUnusedLocalVarsOffset -= 4;
        return this.lastUnusedLocalVarsOffset + 4;
    }
}
