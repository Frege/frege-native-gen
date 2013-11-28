package frege.nativegen;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static java.util.Arrays.asList;

public final class NativeGen {

    private final Class<?> clazz;
    private final Map<String, FregeType> knownTypes;

    public NativeGen(final Class<?> clazz) {
        this(clazz, new HashMap<String, FregeType>());
    }

    public NativeGen(final Class<?> clazz, final Map<String, FregeType> knownTypes) {
        this.clazz = clazz;
        this.knownTypes = knownTypes;
    }

    public FregeType fregeType(final Type type) {
        final FregeType fregeType;
        if (type instanceof TypeVariable<?>) {
            fregeType = fregeType((TypeVariable<?>) type);

        } else if (type instanceof WildcardType) {
            fregeType = fregeType((WildcardType) type);

        } else if (type instanceof GenericArrayType) {
            fregeType = fregeType((GenericArrayType) type);

        } else if (type instanceof ParameterizedType) {
            fregeType = fregeType((ParameterizedType) type);

        } else if (type instanceof Class<?>) {
            fregeType = fregeType((Class<?>) type);

        } else {
            fregeType = new FregeType("Object", Object.class);
        }
        return fregeType;
    }

    public FregeType fregeType(Class<?> cls) {
        final FregeType fregeType;
        final FregeType knownType = knownTypes.get(cls.getName());
        if (knownType != null) {
            fregeType = new FregeType(knownType.type, cls, cls.isArray() ? Purity.ST : knownType.purity);
        } else {
            fregeType = new FregeType(fregeName(cls), cls, cls.isArray() ? Purity.ST : Purity.PURE);
        }
        return fregeType;
    }

    public FregeType fregeType(ParameterizedType ptype) {
        final FregeType fregeType;
        final FregeType pFregeType = fregeType(ptype.getRawType());
        final List<FregeType> ts = fregeTypes(asList(ptype.getActualTypeArguments()));
        final String typeParams;
        if (ts.isEmpty()) {
            typeParams = "";
        } else {
            final StringBuilder builder = new StringBuilder();
            final Iterator<FregeType> itr = ts.iterator();
            builder.append(addParenthesis(itr.next().type));
            while (itr.hasNext()) {
                builder.append(" ").append(addParenthesis(itr.next().type));
            }
            typeParams = builder.toString();
        }
        fregeType = pFregeType.withType(pFregeType.type + " " + typeParams);
        return fregeType;
    }

    private FregeType fregeType(GenericArrayType arrayType) {
        final Type compType = arrayType.getGenericComponentType();
        final FregeType compFregeType = fregeType(compType);
        final String typeName;
        if (compType instanceof TypeVariable<?>) {
            typeName = "ObjectArr";
        } else {
            typeName = compFregeType.type.replaceFirst("(\\w+)", "$1Arr");
        }
        return new FregeType(typeName, Object[].class, Purity.ST); //Generic Arrays are not supported in Frege
    }

    public FregeType fregeType(final TypeVariable<?> typeVar) {
        return new FregeType(typeVar.getName().toLowerCase());
    }

    public FregeType fregeType(WildcardType wildcard) {
        return fregeType(wildcard.getUpperBounds()[0]);
    }

    private String fregeName(Class<?> cls) {
        final String ftype;

        if (cls.isArray()) {
            ftype = fregeType(cls.getComponentType()).type + "Arr";
        } else if (cls.isMemberClass()) {
            ftype = unqualifiedName(cls).replace("$", "_");
        } else {
            ftype = unqualifiedName(cls);
        }
        return ftype;
    }

    public List<FregeType> fregeTypes(final List<Type> types) {
        final List<FregeType> fregeTypes = new ArrayList<>(types.size());
        for (final Type t: types) {
            fregeTypes.add(fregeType(t));
        }
        return fregeTypes;
    }

    private FregeType fregeTypeWithTypeParams(final Class<?> clazz) {
        final FregeType fregeType;
        if (clazz.getTypeParameters().length != 0) {
            final FregeType ftype = fregeType((Type) clazz);
            final String typeParams =
                    makeString(fregeTypes(Arrays.<Type>asList(clazz.getTypeParameters())), " ");
            fregeType = new FregeType(ftype.type + " " + typeParams, ftype.clazz, ftype.purity);
        } else {
            fregeType = fregeType((Type) clazz);
        }
        return fregeType;
    }

    private static String capitalize(final String name) {
        final char head = name.charAt(0);
        return Character.toUpperCase(head) + name.substring(1);
    }

    private String uncapitalize(String name) {
        final char head = name.charAt(0);
        return Character.toLowerCase(head) + name.substring(1);
    }

    private static String unqualifiedName(final Class<?> cls) {
        final String clsName = cls.getName();
        final String unqName;
        if (clsName.indexOf('.') != -1) {
            final int lastDotPos = clsName.lastIndexOf('.');
            unqName = clsName.substring(lastDotPos + 1);
        } else {
            unqName = clsName;
        }
        return unqName;
    }

    public List<FunctionType> constructors() {
        final Constructor<?>[] constructors = clazz.getConstructors();
        final List<FunctionType> functions = new ArrayList<>(constructors.length);
        for (final Constructor<?> con: constructors) {
            if (!con.isSynthetic() && Modifier.isPublic(con.getModifiers())) {
                final FunctionType ctype = toFregeType(con);
                functions.add(ctype);
            }
        }
        return functions;
    }

    public FunctionType toFregeType(Constructor<?> con) {
        final Type[] params = con.getGenericParameterTypes();
        final Type[] exceptions = con.getGenericExceptionTypes();
        final FregeType thisType = fregeTypeWithTypeParams(clazz);
        final List<FregeType> exceptionSigs =
                fregeTypes(filterCheckedExceptions(Arrays.asList(exceptions)));
        final List<FregeType> paramTypes;
        if (params.length == 0) {
            final FregeType fregeType = new FregeType("()", void.class, Purity.PURE);
            paramTypes = Arrays.asList(fregeType);
        } else {
            paramTypes = fregeTypes(asList(params));
        }
        return new FunctionType(paramTypes, thisType, exceptionSigs);
    }

    public List<FunctionType> methods() {
        final Method[] methods = clazz.getDeclaredMethods();

        final List<FunctionType> functions = new ArrayList<>(methods.length);
        for (final Method m: methods) {
            if (!m.isSynthetic() && !m.isBridge() && Modifier.isPublic(m.getModifiers())) {
                functions.add(toFregeType(m));
            }
        }
        return functions;
    }

    public FunctionType toFregeType(final Method m) {
        final boolean isStatic = Modifier.isStatic(m.getModifiers());
        final FregeType thisType = fregeTypeWithTypeParams(clazz);
        final Type[] params = m.getGenericParameterTypes();
        final Type returnType = m.getGenericReturnType();
        final LinkedList<FregeType> sig = new LinkedList<>(fregeTypes(asList(params)));
        if (!isStatic) {
            sig.addFirst(thisType);
        } else {
            if (params.length == 0) {
                sig.addFirst(new FregeType("()", void.class, Purity.PURE));
            }
        }
        final Type[] exceptions = m.getGenericExceptionTypes();
        final List<FregeType> exceptionSigs =
                fregeTypes(filterCheckedExceptions(Arrays.asList(exceptions)));
        return new FunctionType(sig, fregeType(returnType), exceptionSigs, uncapitalize(m.getName()),
                isStatic ? clazz.getCanonicalName() + "." + m.getName() : m.getName());
    }

    public List<FunctionType> fields() {
        final Field[] fields = clazz.getDeclaredFields();
        final List<FunctionType> functions = new ArrayList<>(fields.length);
        for (final Field f: fields) {
            if (!f.isSynthetic() && Modifier.isPublic(f.getModifiers())) {
                final FunctionType fieldType = toFregeType(f);
                functions.add(fieldType);
            }
        }
        return functions;
    }

    public FunctionType toFregeType(Field f) {
        final FregeType thisType = fregeTypeWithTypeParams(clazz);
        final boolean isStatic = Modifier.isStatic(f.getModifiers());
        final Type returnType = f.getGenericType();
        final LinkedList<FregeType> sig = new LinkedList<>();
        if (!isStatic) {
            sig.addFirst(thisType);
        }
        return new FunctionType(
                sig,
                fregeType(returnType),
                Collections.<FregeType>emptyList(),
                f.getName().toLowerCase(),
                isStatic ? clazz.getCanonicalName() + "." + f.getName() : "\"." + f.getName() + "\"");
    }

    public static List<Type> filterCheckedExceptions(final List<Type> types) {
        final List<Type> checkedExceptionTypes = new ArrayList<>(types.size());
        for (final Type t: types) {
            if (t instanceof Class<?>) {
                final Class<?> exc = (Class<?>) t;
                if (Exception.class.isAssignableFrom(exc) &&
                        !RuntimeException.class.isAssignableFrom(exc)) {
                    checkedExceptionTypes.add(t);
                }
            }
        }
        return checkedExceptionTypes;
    }

    private static String makeString(final List<FregeType> ts, final String sep) {
        final String joined;
        if (ts.isEmpty()) {
            joined = "";
        } else {
            final StringBuilder builder = new StringBuilder();
            final Iterator<FregeType> itr = ts.iterator();
            builder.append(itr.next().type);
            while (itr.hasNext()) {
                builder.append(sep).append(itr.next().type);
            }
            joined = builder.toString();
        }
        return joined;
    }

    public static void main(final String[] args) throws Exception {
        final Map<String, FregeType> knownTypes = new HashMap<>();
        if (args.length >= 2) {
            try (final InputStream knownTypesFile = new FileInputStream(args[1])) {
                knownTypes.putAll(parseKnownTypes(knownTypesFile));
            }
            final NativeGen gen = new NativeGen(Class.forName(args[0]), knownTypes);
            System.out.println(gen.toFrege());
        } else if (args.length == 1) {
            final NativeGen gen = new NativeGen(Class.forName(args[0]), knownTypes);
            System.out.println(gen.toFrege());
        } else {
            System.err.println("Error: Missing Java class name and an optional properties file name!");
        }

    }

    private static Map<String,? extends FregeType> parseKnownTypes(InputStream knownTypesInput)
            throws IOException, ClassNotFoundException {
        final Properties props = new Properties();
        props.load(knownTypesInput);
        final Map<String, FregeType> knownTypes = new HashMap<>();
        for (String className: props.stringPropertyNames()) {
            final Class<?> clazz = classForName(className);
            final String value = props.getProperty(className, "");
            final String[] parts = value.split(",");
            final FregeType fregeType;
            if (parts.length >= 2) {
                final String purityStr = parts[0].trim();
                final String fregeName = parts[1].trim();
                fregeType = new FregeType(
                        fregeName, clazz,
                        Purity.fromString(purityStr, Purity.PURE));
                knownTypes.put(fregeType.clazz.getName(), fregeType);
            } else if (parts.length == 1) {
                final String purityStr = parts[0].trim();
                final String ftype;
                if (clazz.isPrimitive()) {
                    ftype = capitalize(clazz.getName());
                } else if (clazz.isArray()) {
                    ftype = clazz.getComponentType().getCanonicalName() + "Arr";
                } else if (clazz.isMemberClass()) {
                    ftype = unqualifiedName(clazz).replace("$", "_");
                } else {
                    ftype = unqualifiedName(clazz);
                }
                fregeType = new FregeType(ftype, clazz,
                        Purity.fromString(purityStr, Purity.PURE));
                knownTypes.put(fregeType.clazz.getName(), fregeType);
            }

        }
        return  knownTypes;
    }

    private static Class<?> classForName(String className) throws ClassNotFoundException {
        final Class<?> clazz;
        switch (className) {
            case "int":
                clazz = int.class;
                break;
            case "boolean":
                clazz = boolean.class;
                break;
            case "byte":
                clazz = byte.class;
                break;
            case "char":
                clazz = char.class;
                break;
            case "short":
                clazz = short.class;
                break;
            case "long":
                clazz = long.class;
                break;
            case "float":
                clazz = float.class;
                break;
            case "double":
                clazz = double.class;
                break;
            case "void":
                clazz = void.class;
                break;
            default:
                clazz = Class.forName(className);
                break;
        }
        return clazz;
    }

    public String toFrege() {
        final StringWriter swriter = new StringWriter();
        final PrintWriter out = new PrintWriter(swriter);

        final FregeType thisType = fregeTypeWithTypeParams(clazz);
        out.println(String.format("data %s = %snative %s where",
                thisType.type,
                thisType.purity.isPure() ? "pure " : "" , clazz.getCanonicalName()));
        out.println();

        final List<FunctionType> fields = fields();
        for (final FunctionType type: withPurity(fields)) {
            out.print("  ");
            out.println(functionToSrc(type));
        }
        if (!fields.isEmpty()) out.println();

        final List<FunctionType> constructors = withPurity(constructors());
        out.print(srcWithGroup(constructors));

        final Map<String, List<FunctionType>> methodMap = groupByName(withPurity(methods()));
        for (final Map.Entry<String, List<FunctionType>> entry: methodMap.entrySet()) {
            final List<FunctionType> functions = entry.getValue();
            out.print(srcWithGroup(functions));

        }
        out.flush();
        return swriter.toString();
    }

    private String srcWithGroup(final List<FunctionType> functions) {
        final String src;
        if (!functions.isEmpty()) {
            final StringWriter swriter = new StringWriter();
            final PrintWriter out = new PrintWriter(swriter);
            final ListIterator<FunctionType> fitr = functions.listIterator();
            final FunctionType firstFunction = fitr.next();
            final String functionName = firstFunction.name +
                    (firstFunction.name.equals(firstFunction.nativeName) ? "" : " " + firstFunction.nativeName);
            final boolean isConstructor = firstFunction.name.equals("new");
            final boolean allPure = !isConstructor && allWithPurity(functions, Purity.PURE);
            final FunctionType newFirstFunction;
            // Since other overloaded versions are impure, change purity for this function also
            if (!allPure && firstFunction.returnType.purity == Purity.PURE) {
                newFirstFunction = firstFunction.withReturnType(returnTypeWithPurity(firstFunction, Purity.ST));
            } else {
                newFirstFunction = firstFunction;
            }
            final String startingLine = String.format(
                    "  %snative %s :: %s", allPure ? "pure " : "", functionName,
                    functionTypeToSrc(newFirstFunction));
            out.println(startingLine);

            while (fitr.hasNext()) {
                out.print(repeatStr(' ', startingLine.indexOf("::")) + " | ");
                final FunctionType function = fitr.next();
                final FunctionType newFunction;
                // Since other overloaded versions are impure, change purity for this function also
                if (!allPure && function.returnType.purity == Purity.PURE ) {
                    newFunction = function.withReturnType(returnTypeWithPurity(function, Purity.ST));
                } else {
                    newFunction = function;
                }
                out.println(functionTypeToSrc(newFunction));
            }
            out.println();
            src = swriter.toString();
        } else {
            src = "";
        }
        return src;
    }

    private boolean allWithPurity(final List<FunctionType> functions, final Purity purity) {
        boolean match = true;
        for (final FunctionType function: functions) {
            if (function.returnType.purity != purity) {
                match = false;
                break;
            }
        }
        return match;
    }

    private String repeatStr(char c, int count) {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(c);
        }
        return builder.toString();
    }

    public String functionTypeToSrc(final FunctionType f) {
        final StringBuilder builder = new StringBuilder()
                .append(NativeGen.makeString(f.paramTypes, " -> "))
                .append(f.paramTypes.isEmpty() ? "" : " -> ")
                .append(f.returnType.type);
        if (!f.exceptionTypes.isEmpty()) {
            builder.append(" throws ");
            builder.append(NativeGen.makeString(f.exceptionTypes, ", "));
        }
        return builder.toString();
    }

    public String functionToSrc(final FunctionType f) {
        final StringBuilder builder = new StringBuilder()
                .append(f.returnType.purity == Purity.PURE ? "pure native " : "native ")
                .append(f.name)
                .append(f.name.equals(f.nativeName) ? "" : " " + f.nativeName)
                .append(" :: ")
                .append(NativeGen.makeString(f.paramTypes, " -> "))
                .append(f.paramTypes.isEmpty() ? "" : " -> ")
                .append(f.returnType.type);
        if (!f.exceptionTypes.isEmpty()) {
            builder.append(" throws ");
            builder.append(NativeGen.makeString(f.exceptionTypes, ", "));
        }
        return builder.toString();
    }

    public final Map<String, List<FunctionType>> groupByName(final List<FunctionType> functions) {
        final Map<String, List<FunctionType>> grouped = new TreeMap<>();
        for (FunctionType function: functions) {
            final List<FunctionType> group;
            if (grouped.containsKey(function.name)) {
                group = grouped.get(function.name);
            } else {
                group = new ArrayList<>();
                grouped.put(function.name, group);
            }
            group.add(function);
        }
        return grouped;
    }

    public List<FunctionType> withPurity(final List<FunctionType> functionTypes) {
        final List<FunctionType> functions = new ArrayList<>();
        for (final FunctionType function: functionTypes) {
            final FunctionType newFunction;
            newFunction = withPurity(function);
            functions.add(newFunction);
        }
        return functions;
    }

    public FunctionType withPurity(FunctionType function) {
        FunctionType newFunction;
        final List<FregeType> paramTypes = function.paramTypes;
        if (anyMatchingPurity(paramTypes, function.returnType, Purity.IO)) {
            newFunction = withPurity(function, Purity.IO);

        } else if (function.returnType.clazz.equals(void.class) ||
                function.name.equals("new") || !function.exceptionTypes.isEmpty() ||
                anyMatchingPurity(paramTypes, function.returnType, Purity.ST)) {
            newFunction = withPurity(function, Purity.ST);
        } else {
            newFunction = function;
        }
        return newFunction;
    }

    private FunctionType withPurity(
            final FunctionType function,
            final Purity purity) {
        if (purity == Purity.PURE) return function;
        final List<FregeType> newParamTypes = paramTypesWithPurity(function, purity);
        final FregeType newReturnType = returnTypeWithPurity(function, purity);
        return function.withParamTypes(newParamTypes)
                .withReturnType(newReturnType);
    }

    private List<FregeType> paramTypesWithPurity(FunctionType function, Purity purity) {
        final String paramMonad = purity == Purity.ST ? "Mutable s" : "MutableIO";
        final List<FregeType> newParamTypes = new ArrayList<>();
        for (final FregeType paramType: function.paramTypes) {
            if (!paramType.purity.isPure()) {
                final String newType = paramMonad + " " + addParenthesis(paramType.type);
                newParamTypes.add(new FregeType(newType, paramType.clazz, purity));
            } else {
                newParamTypes.add(paramType);
            }
        }
        return newParamTypes;
    }

    private FregeType returnTypeWithPurity(FunctionType function, Purity purity) {
        final String returnMonad = purity == Purity.ST ? "STMutable s" : "IOMutable";
        final String returnPureMonad = purity == Purity.ST ? "ST s" : "IO";
        final FregeType newReturnType;

        if (!function.returnType.purity.isPure()) {
            newReturnType = new FregeType(returnMonad + " " + addParenthesis(function.returnType.type),
                    function.returnType.clazz, purity);
        } else {
            newReturnType = new FregeType(
                    returnPureMonad + " " + addParenthesis(function.returnType.type), function.returnType.clazz, purity);
        }
        return newReturnType;
    }

    public static String addParenthesis(final String str) {
        return !str.startsWith("(") && (str.indexOf(' ') != -1) ? "(" + str + ")" : str;
    }

    public boolean anyMatchingPurity(final List<FregeType> types,
                                     final FregeType returnType,
                                     final Purity expected) {
        if (returnType.purity == expected) {
            return true;
        }
        for (final FregeType type: types) {
            if (type.purity == expected ||
                    (expected == Purity.ST && type.clazz.equals(void.class))) return true;
        }
        return false;
    }

    public static final class FunctionType {
        final List<FregeType> paramTypes;
        final FregeType returnType;
        final List<FregeType> exceptionTypes;
        final String name;
        final String nativeName;

        public FunctionType(final List<FregeType> paramTypes,
                            final FregeType returnType) {
            this(paramTypes, returnType, Collections.<FregeType>emptyList());
        }

        public FunctionType(final List<FregeType> paramTypes,
                            final FregeType returnType,
                            final List<FregeType> exceptionTypes) {
            this(paramTypes, returnType, exceptionTypes, "new");
        }

        public FunctionType(final List<FregeType> paramTypes,
                            final FregeType returnType,
                            final List<FregeType> exceptionTypes,
                            final String name) {
            this(paramTypes, returnType, exceptionTypes, name, name);
        }

        public FunctionType(final List<FregeType> paramTypes,
                            final FregeType returnType,
                            final List<FregeType> exceptionTypes,
                            final String name,
                            final String nativeName) {
            this.paramTypes = paramTypes;
            this.returnType = returnType;
            this.exceptionTypes = exceptionTypes;
            this.name = name;
            this.nativeName = nativeName;
        }

        public FunctionType withParamTypes(final List<FregeType> newParamTypes) {
            return new FunctionType(newParamTypes, returnType, exceptionTypes, name, nativeName);
        }

        public FunctionType withReturnType(final FregeType newReturnType) {
            return new FunctionType(paramTypes, newReturnType, exceptionTypes, name, nativeName);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder()
                    .append(returnType.purity == Purity.PURE ? "pure native " : "native ")
                    .append(name)
                    .append(name.equals(nativeName) ? "" : " " + nativeName)
                    .append(" :: ")
                    .append(NativeGen.makeString(paramTypes, " -> "))
                    .append(paramTypes.isEmpty() ? "" : " -> ")
                    .append(returnType.type);
            if (!exceptionTypes.isEmpty()) {
                builder.append(" throws ");
                builder.append(NativeGen.makeString(exceptionTypes, ", "));
            }
            return builder.toString();
        }

    }

    public static enum Purity {
        PURE, ST, IO;

        public boolean isPure() {
            return this == PURE;
        }

        public static Purity fromString(String value, Purity deflt) {
            final String lowervalue = value.toLowerCase();
            final Purity res;
            switch(lowervalue) {
                case "pure":
                    res = PURE;
                    break;
                case "st":
                    res = ST;
                    break;
                case "io":
                    res = IO;
                    break;
                default:
                    res = deflt;
                    break;
            }
            return res;
        }
    }

    public static final class FregeType {
        final String type;
        final Class<?> clazz;
        final Purity purity;

        public FregeType(final String type, final Class<?> clazz, final Purity purity) {
            this.type = type;
            this.clazz = clazz;
            this.purity = purity;
        }

        public FregeType(final String type, final Class<?> clazz) {
            this(type, clazz, Purity.PURE);
        }

        public FregeType(final String type) {
            this(type, Object.class, Purity.PURE);
        }

        public FregeType withType(String type) {
            return new FregeType(type, clazz, purity);
        }
    }


}

