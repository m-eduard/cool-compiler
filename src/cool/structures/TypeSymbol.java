package cool.structures;

import cool.parser.nodes.Formal;
import cool.parser.nodes.Id;
import cool.parser.nodes.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeSymbol extends Symbol {
    public ClassSymbol classSymbol;

    public TypeSymbol(String name) {
        super(name);
    }

    public TypeSymbol(String name, ClassSymbol classSymbol) {
        super(name);
        this.classSymbol = classSymbol;
    }

    public static final TypeSymbol INT   = new TypeSymbol("Int", new ClassSymbol("Int", SymbolTable.globals));
    public static final TypeSymbol STRING = new TypeSymbol("String", new ClassSymbol("String", SymbolTable.globals));
    public static final TypeSymbol BOOL = new TypeSymbol("Bool", new ClassSymbol("Bool", SymbolTable.globals));
    public static final TypeSymbol SELF_TYPE = new TypeSymbol("SELF_TYPE", new ClassSymbol("SELF_TYPE", SymbolTable.globals));
    public static final TypeSymbol OBJECT = new TypeSymbol("Object", new ClassSymbol("Object", SymbolTable.globals));
    public static final TypeSymbol IO = new TypeSymbol("IO", new ClassSymbol("IO", SymbolTable.globals));

    static {
        OBJECT.classSymbol.add(new MethodSymbol("abort", 0, OBJECT.classSymbol));
        OBJECT.classSymbol.lookupMethod("abort").setType(OBJECT);
        OBJECT.classSymbol.add(new MethodSymbol("type_name", 0, OBJECT.classSymbol));
        OBJECT.classSymbol.lookupMethod("type_name").setType(STRING);
        OBJECT.classSymbol.add(new MethodSymbol("copy", 0, OBJECT.classSymbol));
        OBJECT.classSymbol.lookupMethod("copy").setType(SELF_TYPE);


        IO.classSymbol.add(new MethodSymbol("out_string", 1, IO.classSymbol));
        IdSymbol param = new IdSymbol("x");
        param.setType(STRING);
        IO.classSymbol.lookupMethod("out_string").add(param);
        IO.classSymbol.lookupMethod("out_string").setType(SELF_TYPE);

        IO.classSymbol.add(new MethodSymbol("out_int", 1, IO.classSymbol));
        param = new IdSymbol("x");
        param.setType(INT);
        IO.classSymbol.lookupMethod("out_int").add(param);
        IO.classSymbol.lookupMethod("out_int").setType(SELF_TYPE);

        IO.classSymbol.add(new MethodSymbol("in_string", 0, IO.classSymbol));
        IO.classSymbol.lookupMethod("in_string").setType(STRING);
        IO.classSymbol.add(new MethodSymbol("in_int", 0, IO.classSymbol));
        IO.classSymbol.lookupMethod("in_int").setType(INT);


        STRING.classSymbol.add(new MethodSymbol("length", 0, STRING.classSymbol));
        STRING.classSymbol.lookupMethod("length").setType(INT);

        STRING.classSymbol.add(new MethodSymbol("concat", 1, STRING.classSymbol));
        param = new IdSymbol("s");
        param.setType(STRING);
        STRING.classSymbol.lookupMethod("concat").add(param);
        STRING.classSymbol.lookupMethod("concat").setType(STRING);

        STRING.classSymbol.add(new MethodSymbol("substr", 2, STRING.classSymbol));
        param = new IdSymbol("i");
        param.setType(INT);
        STRING.classSymbol.lookupMethod("substr").add(param);
        param = new IdSymbol("l");
        param.setType(INT);
        STRING.classSymbol.lookupMethod("substr").add(param);
        STRING.classSymbol.lookupMethod("substr").setType(STRING);

        INT.classSymbol.nestedScopes.add(TypeSymbol.OBJECT.classSymbol);
        STRING.classSymbol.nestedScopes.add(TypeSymbol.OBJECT.classSymbol);
        BOOL.classSymbol.nestedScopes.add(TypeSymbol.OBJECT.classSymbol);
        IO.classSymbol.nestedScopes.add(TypeSymbol.OBJECT.classSymbol);
    }

    public static final Set<TypeSymbol> defaultTypes = new HashSet<>(Arrays.asList(
            TypeSymbol.INT,
            TypeSymbol.STRING,
            TypeSymbol.BOOL,
            TypeSymbol.SELF_TYPE,
            TypeSymbol.OBJECT,
            TypeSymbol.IO
    ));

    public static final Set<String> illegalInheritance = new HashSet<>(Arrays.asList(
            TypeSymbol.INT,
            TypeSymbol.STRING,
            TypeSymbol.BOOL,
            TypeSymbol.SELF_TYPE,
            TypeSymbol.OBJECT
    )).stream().map(Symbol::getName).collect(Collectors.toSet());

    public static final Set<String> defaultClassesAsStrings = defaultTypes.stream()
            .filter(x -> !x.equals(TypeSymbol.SELF_TYPE))
            .map(Symbol::getName).collect(Collectors.toSet());
}
