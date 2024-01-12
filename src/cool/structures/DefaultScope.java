package cool.structures;

import java.util.*;

public class DefaultScope implements Scope {
    
    private Map<String, Symbol> symbols = new LinkedHashMap<>();
    
    private Scope parent;
    
    public DefaultScope(Scope parent) {
        this.parent = parent;
    }

    @Override
    public boolean add(Symbol sym) {
        // Reject duplicates in the same scope.
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
    
    @Override
    public String toString() {
        return symbols.values().toString();
    }

}
