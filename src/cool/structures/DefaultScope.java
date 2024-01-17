package cool.structures;

import java.util.*;

public class DefaultScope implements Scope {
    
    public Map<String, Symbol> symbols = new LinkedHashMap<>();
    
    private Scope parent;

    public int lastUnusedLocalVarsOffset = -4;
    
    public DefaultScope(Scope parent) {
        this.parent = parent;

        // Inherit the last unused offset from the parent scope
        // (the current scope will have some local variables, but their
        // offsets should not be propagated back to the parent scope, since
        // this scope and the next child of the parent scope will not live
        // during the same time)
        if (parent != null)
            this.lastUnusedLocalVarsOffset = parent.getLastUnusedLocalVarsOffset();
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
