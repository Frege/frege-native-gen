package frege.nativegen;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static frege.nativegen.NativeGen.FregeType;
import static frege.nativegen.NativeGen.Purity;
import static junit.framework.Assert.assertEquals;

public class NativeGenTest {
    private static Map<String, FregeType> knownTypes;

    @BeforeClass
    public static void setup() {
        knownTypes = new HashMap<>();
        knownTypes.put("int", new FregeType("Int", int.class, Purity.PURE));
        knownTypes.put("boolean", new FregeType("Bool", boolean.class, Purity.PURE));
        knownTypes.put("byte", new FregeType("Byte", byte.class, Purity.PURE));
        knownTypes.put("char", new FregeType("Char", char.class, Purity.PURE));
        knownTypes.put("short", new FregeType("Short", short.class, Purity.PURE));
        knownTypes.put("long", new FregeType("Long", long.class, Purity.PURE));
        knownTypes.put("float", new FregeType("Float", float.class, Purity.PURE));
        knownTypes.put("double", new FregeType("Double", double.class, Purity.PURE));
        knownTypes.put("void", new FregeType("()", void.class, Purity.PURE));
        knownTypes.put("java.lang.String", new FregeType("String", java.lang.String.class, Purity.PURE));
        knownTypes.put("java.math.BigInteger", new FregeType("Integer", java.math.BigInteger.class, Purity.PURE));
        knownTypes.put("java.lang.Math", new FregeType("Math", java.lang.Math.class, Purity.PURE));
        knownTypes.put("java.awt.Point", new FregeType("Point", java.awt.Point.class, Purity.ST));
        knownTypes.put("java.io.InputStream", new FregeType("InputStream", java.io.InputStream.class, Purity.IO));
        knownTypes.put("java.io.PrintStream", new FregeType("PrintStream", java.io.InputStream.class, Purity.IO));
        knownTypes.put("java.io.FileInputStream", new FregeType("FileInputStream", java.io.FileInputStream.class, Purity.IO));
        knownTypes.put("java.nio.channels.FileChannel", new FregeType("FileChannel", java.nio.channels.FileChannel.class, Purity.IO));
        knownTypes.put("java.io.File", new FregeType("File", java.io.File.class, Purity.IO));
        knownTypes.put("java.util.Collection", new FregeType("Collection", java.util.Collection.class, Purity.ST));
        knownTypes.put("java.util.ArrayList", new FregeType("ArrayList", java.util.ArrayList.class, Purity.ST));
        knownTypes.put("java.util.LinkedList", new FregeType("LinkedList", java.util.LinkedList.class, Purity.ST));
        knownTypes.put("java.util.Iterator", new FregeType("Iterator", java.util.Iterator.class, Purity.ST));
        knownTypes.put("java.util.Locale", new FregeType("Locale", java.util.Locale.class, Purity.PURE));
        knownTypes.put("java.util.Locale$Builder", new FregeType("LocaleBuilder", java.util.Locale.Builder.class, Purity.ST));
        knownTypes.put("java.util.Set", new FregeType("Set", java.util.Set.class, Purity.ST));
        knownTypes.put("java.util.Map", new FregeType("Map", java.util.Map.class, Purity.ST));
        knownTypes.put("java.util.Scanner", new FregeType("Scanner", java.util.Scanner.class, Purity.ST));
        knownTypes.put("java.util.HashMap", new FregeType("HashMap", java.util.HashMap.class, Purity.ST));

    }

    @AfterClass
    public static void teardown() {
        knownTypes = new HashMap<>();
    }

    @Test
    // new java.util.ArrayList()
    public void testNoArgConstructor() throws NoSuchMethodException {
        final Class<java.util.ArrayList> clazz = java.util.ArrayList.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Constructor constructor = clazz.getDeclaredConstructor();
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(constructor)));
        final String expected = "native new :: () -> STMutable s (ArrayList e)";
        assertEquals(expected, actual);
    }

    @Test
    // java.lang.System.out
    public void testStaticField() throws NoSuchFieldException {
        final Class<System> clazz = System.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Field field = clazz.getDeclaredField("out");
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(field)));
        final String expected = "native out java.lang.System.out :: IOMutable PrintStream";
        assertEquals(expected, actual);
    }

    @Test
    // p.x where p :: Point
    public void testInstanceField() throws NoSuchFieldException {
        final Class<?> clazz = java.awt.Point.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Field field = clazz.getDeclaredField("x");
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(field)));
        final String expected = "native x \".x\" :: Mutable s Point -> ST s Int";
        assertEquals(expected, actual);
    }

    @Test
    // java.lang.Math.floor(double) :: double
    public void testPureStaticMethod() throws NoSuchMethodException {
        final Class<Math> clazz = Math.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Method method = clazz.getDeclaredMethod("floor", double.class);
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(method)));
        final String expected = "pure native floor java.lang.Math.floor :: Double -> Double";
        assertEquals(expected, actual);
    }

    @Test
    // b.add(BigInteger) :: BigInteger where b :: BigInteger
    public void testPureInstanceMethod() throws NoSuchMethodException {
        final Class<java.math.BigInteger> clazz = java.math.BigInteger.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Method method = clazz.getDeclaredMethod("add", java.math.BigInteger.class);
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(method)));
        final String expected = "pure native add :: Integer -> Integer -> Integer";
        assertEquals(expected, actual);
    }

    @Test
    // arrayList.add(Object) :: boolean where arrayList :: java.util.ArrayList<E>
    public void testImpureInstanceMethod() throws NoSuchMethodException {
        final Class<java.util.ArrayList> clazz = java.util.ArrayList.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Method method = clazz.getDeclaredMethod("add", java.lang.Object.class);
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(method)));
        final String expected = "native add :: Mutable s (ArrayList e) -> e -> ST s Bool";
        assertEquals(expected, actual);
    }

    @Test
    // java.lang.System.currentTimeMillis() :: long
    public void testNoArg() throws NoSuchMethodException {
        final Class<System> clazz = System.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Method method = clazz.getDeclaredMethod("currentTimeMillis");
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(method)));
        final String expected = "native currentTimeMillis java.lang.System.currentTimeMillis :: () -> ST s Long";
        assertEquals(expected, actual);
    }

    @Test
    public void testVoidReturn() throws NoSuchMethodException {
        final Class<System> clazz = System.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        final Method method = clazz.getDeclaredMethod("exit", int.class);
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(method)));
        final String expected = "native exit java.lang.System.exit :: Int -> ST s ()";
        assertEquals(expected, actual);
    }

    @Test
    public void testGenericArrayReturn() throws NoSuchMethodException {
        final Class<Class> clazz = Class.class;
        final NativeGen gen = new NativeGen(clazz, knownTypes);
        // TypeVariable<Class<T>>[]	java.lang.Class.getTypeParameters()
        final Method method = clazz.getDeclaredMethod("getTypeParameters");
        final String actual = gen.functionToSrc(gen.withPurity(gen.toFregeType(method)));
        final String expected = "native getTypeParameters :: Class t -> STMutable s (TypeVariableArr (Class t))";
        assertEquals(expected, actual);
    }

}

