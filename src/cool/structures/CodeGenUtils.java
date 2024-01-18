package cool.structures;

import cool.compiler.ResolutionPassVisitor;

import java.util.*;
import java.util.stream.Collectors;

public class CodeGenUtils {
    public static Map<String, List<String>> inheritanceTree = new LinkedHashMap<>();
    public static Map<String, Integer> classesTags = new LinkedHashMap<>();
    public static Map<String, Pair<Integer, Integer>> classesRanges = new LinkedHashMap<>();

    public static Set<String> allClasses = new LinkedHashSet<>();

    public static List<String> orderedClasses = new ArrayList<>();
    public static int classesTagCounter = 0;

    public static void initializeInheritanceTree() {
        DefaultScope globalScope = null;
        try {
            globalScope = (DefaultScope) SymbolTable.globals;
        } catch(ClassCastException e) {
            e.printStackTrace();
        }

        for (String typeName : globalScope.symbols.keySet()) {
            Symbol sym = globalScope.lookup(typeName);

            if (sym instanceof TypeSymbol || sym instanceof ClassSymbol) {
                if (typeName.equals(TypeSymbol.SELF_TYPE.getName()))
                    continue;

                allClasses.add(typeName);

                if (typeName.equals(TypeSymbol.OBJECT.getName()))
                    continue;

                ClassSymbol cls = ResolutionPassVisitor.getClassSymbol(sym);
                String parentName = cls.getBaseClass();

                if (parentName == null || parentName.isEmpty()) {
                    parentName = TypeSymbol.OBJECT.name;
                }

                if (!inheritanceTree.containsKey(parentName)) {
                    inheritanceTree.put(parentName, new ArrayList<>());
                }

                inheritanceTree.get(parentName).add(typeName);
            }
        }
    }

    public static void initializeClassesTags() {
        String source = TypeSymbol.OBJECT.name;
        Map<String, Boolean> visited = new LinkedHashMap<>();

        allClasses.forEach(type -> visited.put(type, false));

        visited.put(source, true);

        depthFirstSearch(source, visited);

        orderedClasses.addAll(allClasses);

        orderedClasses.sort(Comparator.comparingInt(o -> classesTags.get(o)));

        List<String> classesRangesOrdered = classesRanges.entrySet().stream()
                .sorted((x, y) -> y.getValue().first - x.getValue().first)
                .map(Map.Entry::getKey).toList();

        Map<String, Pair<Integer, Integer>> tmp = new LinkedHashMap<>();
        for (String key : classesRangesOrdered) {
            tmp.put(key, classesRanges.get(key));
        }

        classesRanges = tmp;
    }

    public static void generateMethodsAndMembersOffsets() {
        Queue<String> q = new LinkedList<>();

        q.offer(TypeSymbol.OBJECT.getName());

        while (!q.isEmpty()) {
            String className = q.poll();

            Symbol sym = SymbolTable.globals.lookup(className);
            if (sym instanceof ClassSymbol || sym instanceof TypeSymbol) {
                ClassSymbol classSymbol = ResolutionPassVisitor.getClassSymbol(sym);
                String parentName = classSymbol.getBaseClass();
                ClassSymbol parentSymbol = ResolutionPassVisitor
                        .getClassSymbol(SymbolTable.globals.lookup(parentName));

                if (parentSymbol == null && !classSymbol.equals(TypeSymbol.OBJECT.classSymbol)) {
                    parentSymbol = TypeSymbol.OBJECT.classSymbol;
                }

                if (parentSymbol != null) {
                    classSymbol.allMethods.addAll(parentSymbol.allMethods);
                    classSymbol.allMembers.addAll(parentSymbol.allMembers);
                }

                for (MethodSymbol method : classSymbol.getMethods()) {
                    int index = classSymbol.allMethods.indexOf(method);
                    if (index == -1) {
                        method.offsetInDispTable = classSymbol.allMethods.size() * 4;
                        classSymbol.allMethods.add(method);
                    } else {
                        method.offsetInDispTable = 4 * index;
                        classSymbol.allMethods.set(index, method);
                    }
                }

                List<IdSymbol> newMembers = classSymbol.getMembers().stream().filter((x) -> !x
                        .getName().equals("self") && !x.getName().equals("_self")).toList();

                classSymbol.allMembers.addAll(newMembers);

                int index = classSymbol.allMembers.size() - newMembers.size();
                for (IdSymbol member : newMembers) {
                    member.basePtr = "$s0";
                    member.offset = (3 + index) * 4;
                    index++;
                }
            }

            inheritanceTree.getOrDefault(className, new ArrayList<>()).forEach(q::offer);
        }
    }

    public static int depthFirstSearch(String source, Map<String, Boolean> visited) {
        classesTags.put(source, classesTagCounter);
        classesRanges.put(source, new Pair<Integer, Integer>(classesTagCounter, classesTagCounter));
        classesTagCounter++;

        if (inheritanceTree.get(source) == null)
            return classesTagCounter - 1;

        int maxTagCounter = 0;
        for (String neighbour : inheritanceTree.get(source)) {
            if (!visited.get(neighbour)) {
                visited.put(neighbour, true);

                maxTagCounter = depthFirstSearch(neighbour, visited);
            }
        }

        classesRanges.get(source).second = maxTagCounter;
        return maxTagCounter;
    }
}
