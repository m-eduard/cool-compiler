package cool.structures;

import cool.parser.nodes.ClassDef;
import cool.parser.nodes.Type;

import java.sql.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassSymbol extends IdSymbol implements Scope {
    private Map<String, Symbol> memberSymbols = new LinkedHashMap<>();
    private Map<String, Symbol> methodSymbols = new LinkedHashMap<>();

    public List<MethodSymbol> allMethods = new ArrayList<>();
    public List<IdSymbol> allMembers = new ArrayList<>();

    {
        memberSymbols.put("self", new IdSymbol("self"));

        // Dummy symbol used to retrieve the name of the closest class
        memberSymbols.put("_self", new IdSymbol("_self"));
    }

    private Scope parent;
    private String baseClass;

    // Member which has to be set to either "method" or "member", used
    // by the lookup method in order to decide in which namespace to search
    public String lookupType = "member";

    public List<ClassSymbol> nestedScopes = new ArrayList<>();

    // Decided to store this classDef in order to easily apply the Resolution
    // visitor on it, when a class have to inherit all the members and methods
    // needed to solve a method call referenced from other class defined before
    // e.g.: B.f(), but B is defined under the call, and it inherits f from other class
    public ClassDef classDef;

    public ClassSymbol(String name, Scope parent) {
        super(name);
        this.parent = parent;

        ((IdSymbol) memberSymbols.get("self")).setType(TypeSymbol.SELF_TYPE);
        ((IdSymbol) memberSymbols.get("_self")).setType(new TypeSymbol(name));
    }

    public ClassSymbol(String name, Scope parent, ClassDef classDef) {
        this(name, parent);
        this.classDef = classDef;

        // Add only the root scope (which also contain some default methods)
        nestedScopes.add(TypeSymbol.OBJECT.classSymbol);
    }

    public ClassSymbol(String name, Scope parent, String baseClass, ClassDef classDef) {
        this(name, parent, classDef);
        this.baseClass = baseClass;
    }

    @Override
    public boolean add(Symbol sym) {
        if (sym instanceof MethodSymbol) {
            if (methodSymbols.containsKey(sym.getName()))
                return false;

            methodSymbols.put(sym.getName(), sym);
            return true;
        }

        if (memberSymbols.containsKey(sym.getName()))
            return false;

        memberSymbols.put(sym.getName(), sym);
        return true;
    }

    @Override
    public Symbol lookup(String name) {
        var sym = (lookupType.equals("member") ? memberSymbols : methodSymbols)
                .get(name);

        if (sym != null)
            return sym;

        if (parent != null)
            return parent.lookup(name);

        return null;
    }

    @Override
    public IdSymbol lookupMember(String name) {
        lookupType = "member";
        IdSymbol sym = (IdSymbol) lookup(name);

        // Search the member in inherited classes also
        for (ClassSymbol parentScope : nestedScopes) {
            if (sym == null) {
                sym = parentScope.lookupMember(name);
            }
        }

        return sym;
    }

    @Override
    public MethodSymbol lookupMethod(String name) {
        lookupType = "method";
        MethodSymbol sym = (MethodSymbol) lookup(name);

        // Search the method in inherited classes also
        for (ClassSymbol parentScope : nestedScopes) {
            if (sym == null) {
                sym = parentScope.lookupMethod(name);
            }
        }

        lookupType = "member";
        return sym;
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    public String getBaseClass() {
        if (baseClass == null && !name.equals(TypeSymbol.OBJECT.name)) {
            return TypeSymbol.OBJECT.name;
        }

        return baseClass;
    }

    @Override
    public String toString() {
        return memberSymbols.values().toString() + methodSymbols.values().toString();
    }

    public List<MethodSymbol> getMethods() {
        return new ArrayList<>(methodSymbols.values()
                .stream()
                .filter(sym -> sym instanceof MethodSymbol)
                .map(sym -> (MethodSymbol)sym)
                .toList());
    }

    public List<IdSymbol> getMembers() {
        return new ArrayList<>(memberSymbols.values()
                .stream()
                .filter(sym -> sym instanceof IdSymbol)
                .map(sym -> (IdSymbol)sym)
                .toList());
    }
}
