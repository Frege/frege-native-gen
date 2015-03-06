#Frege code generator for Java classes#
This project aims to reduce the manual effort for defining native bindings for Java classes in Frege. 

Given a Java class and it's purity (whether it is pure or mutable(`ST`) or doing IO(`IO`)), 
this will generate corresponding Frege code for that class.
The generated code may still not compile due to other unknown types or you might want to wrap the return type in `Maybe`
if the method is known to return a possible `null`. So this is just an utility so that
we don't have to look at every Java method signatures and write down the corresponding Frege type signatures 
which is both time consuming and more error-prone. 

Here are some examples on how this works:

**Example 1:** `java.awt.Point` in `ST`

1. All public members are resolved including public fields.
2. Overloaded constructors and methods are grouped and their types are seperated with `|`.
3. If the class implements `Serializable`, `derive Serializable` will be generated. In the same way, if the class
   is not `Serializable` but implements `Cloneable`, `derive Cloneable` will be generated and for subclasses of `Throwable`, `derive Exceptional` will be generated.

```
data Point = native java.awt.Point where

  native x ".x" :: Mutable s Point -> ST s Int
  native y ".y" :: Mutable s Point -> ST s Int

  native new :: Int -> Int -> STMutable s Point
              | Mutable s Point -> STMutable s Point
              | () -> STMutable s Point

  native equals :: Mutable s Point -> Object -> ST s Bool

  native getLocation :: Mutable s Point -> STMutable s Point

  native getX :: Mutable s Point -> ST s Double

  native getY :: Mutable s Point -> ST s Double

  native move :: Mutable s Point -> Int -> Int -> ST s ()

  native setLocation :: Mutable s Point -> Mutable s Point -> ST s ()
                      | Mutable s Point -> Int -> Int -> ST s ()
                      | Mutable s Point -> Double -> Double -> ST s ()

  native toString :: Mutable s Point -> ST s String

  native translate :: Mutable s Point -> Int -> Int -> ST s ()

derive Serializable Point

```
**Example 2:** `java.util.HashMap` in `ST`

Type parameters for generic classes and methods are resolved, so are nested classes (`java.util.Map.Entry`). The
naming convention for nested classes in the generated code is to prepend the class name with the parent class name and an underscore
but it can be overridden to have a different name (See `types.properties` below).

```
data HashMap k v = native java.util.HashMap where

  native new :: Mutable s (Map k v) -> STMutable s (HashMap k v)
              | () -> STMutable s (HashMap k v)
              | Int -> STMutable s (HashMap k v)
              | Int -> Float -> STMutable s (HashMap k v)

  native clear :: Mutable s (HashMap k v) -> ST s ()

  native clone :: Mutable s (HashMap k v) -> ST s Object

  native containsKey :: Mutable s (HashMap k v) -> Object -> ST s Bool

  native containsValue :: Mutable s (HashMap k v) -> Object -> ST s Bool

  native entrySet :: Mutable s (HashMap k v) -> STMutable s (Set (MapEntry k v))

  native get :: Mutable s (HashMap k v) -> Object -> ST s v

  native isEmpty :: Mutable s (HashMap k v) -> ST s Bool

  native keySet :: Mutable s (HashMap k v) -> STMutable s (Set k)

  native put :: Mutable s (HashMap k v) -> k -> v -> ST s v

  native putAll :: Mutable s (HashMap k v) -> Mutable s (Map k v) -> ST s ()

  native remove :: Mutable s (HashMap k v) -> Object -> ST s v

  native size :: Mutable s (HashMap k v) -> ST s Int

  native values :: Mutable s (HashMap k v) -> STMutable s (Collection v)

derive Serializable (HashMap k v)

```
**Example 3:** `java.util.Locale` as `pure`

1. Static constant fields are resolved and generated with all lowercase characters.
2. Though the class itself is pure, if any of the parameters for a method is impure such as arrays or other impure types
or if the method is returning `void` or doesn't take any parameters or throws any checked `Exception`,
the method will be considered as impure (here for example, `getAvailableLocales`, `getExtensionKeys`).
3. Due to grouping of overloaded methods, if any of the overloaded methods is pure but others are impure, the pure method will be considered as impure. For example, here, 
   `native getDefault java.util.Locale.getDefault :: LocaleCategory -> ST s Locale`
    is supposed to be pure because both `LocaleCategory` and `Locale` are pure but since the other overloaded version,
    `() -> ST s Locale` is impure, the first version is also modified to be impure (in `ST`).

```
data Locale = pure native java.util.Locale where

  pure native english java.util.Locale.ENGLISH :: Locale
  pure native french java.util.Locale.FRENCH :: Locale
  pure native german java.util.Locale.GERMAN :: Locale
  pure native italian java.util.Locale.ITALIAN :: Locale
  pure native japanese java.util.Locale.JAPANESE :: Locale
  pure native korean java.util.Locale.KOREAN :: Locale
  pure native chinese java.util.Locale.CHINESE :: Locale
  pure native simplified_chinese java.util.Locale.SIMPLIFIED_CHINESE :: Locale
  pure native traditional_chinese java.util.Locale.TRADITIONAL_CHINESE :: Locale
  pure native france java.util.Locale.FRANCE :: Locale
  pure native germany java.util.Locale.GERMANY :: Locale
  pure native italy java.util.Locale.ITALY :: Locale
  pure native japan java.util.Locale.JAPAN :: Locale
  pure native korea java.util.Locale.KOREA :: Locale
  pure native china java.util.Locale.CHINA :: Locale
  pure native prc java.util.Locale.PRC :: Locale
  pure native taiwan java.util.Locale.TAIWAN :: Locale
  pure native uk java.util.Locale.UK :: Locale
  pure native us java.util.Locale.US :: Locale
  pure native canada java.util.Locale.CANADA :: Locale
  pure native canada_french java.util.Locale.CANADA_FRENCH :: Locale
  pure native root java.util.Locale.ROOT :: Locale
  pure native private_use_extension java.util.Locale.PRIVATE_USE_EXTENSION :: Char
  pure native unicode_locale_extension java.util.Locale.UNICODE_LOCALE_EXTENSION :: Char

  native new :: String -> ST s Locale
              | String -> String -> ST s Locale
              | String -> String -> String -> ST s Locale

  pure native clone :: Locale -> Object

  pure native equals :: Locale -> Object -> Bool

  pure native forLanguageTag java.util.Locale.forLanguageTag :: String -> Locale

  native getAvailableLocales java.util.Locale.getAvailableLocales :: () -> STMutable s (JArray Locale)

  pure native getCountry :: Locale -> String

  native getDefault java.util.Locale.getDefault :: LocaleCategory -> ST s Locale
                                                 | () -> ST s Locale

  pure native getDisplayCountry :: Locale -> String
                                 | Locale -> Locale -> String

  pure native getDisplayLanguage :: Locale -> String
                                  | Locale -> Locale -> String

  pure native getDisplayName :: Locale -> Locale -> String
                              | Locale -> String

  pure native getDisplayScript :: Locale -> String
                                | Locale -> Locale -> String

  pure native getDisplayVariant :: Locale -> Locale -> String
                                 | Locale -> String

  pure native getExtension :: Locale -> Char -> String

  native getExtensionKeys :: Locale -> STMutable s (Set Character)

  pure native getISO3Country :: Locale -> String

  pure native getISO3Language :: Locale -> String

  native getISOCountries java.util.Locale.getISOCountries :: () -> STMutable s (JArray String)

  native getISOLanguages java.util.Locale.getISOLanguages :: () -> STMutable s (JArray String)

  pure native getLanguage :: Locale -> String

  pure native getScript :: Locale -> String

  native getUnicodeLocaleAttributes :: Locale -> STMutable s (Set String)

  native getUnicodeLocaleKeys :: Locale -> STMutable s (Set String)

  pure native getUnicodeLocaleType :: Locale -> String -> String

  pure native getVariant :: Locale -> String

  pure native hashCode :: Locale -> Int

  native setDefault java.util.Locale.setDefault :: Locale -> ST s ()
                                                 | LocaleCategory -> Locale -> ST s ()

  pure native toLanguageTag :: Locale -> String

  pure native toString :: Locale -> String

derive Serializable Locale
```

**Example 4:** `java.io.FileInputStream` in `IO`

Checked exceptions are identified and a `throws` is appended to the function type with all the checked exceptions.

```
data FileInputStream = native java.io.FileInputStream where

  native new :: MutableIO File -> IOMutable FileInputStream throws FileNotFoundException
              | String -> IOMutable FileInputStream throws FileNotFoundException
              | FileDescriptor -> IOMutable FileInputStream

  native available :: MutableIO FileInputStream -> IO Int throws IOException

  native close :: MutableIO FileInputStream -> IO () throws IOException

  native getChannel :: MutableIO FileInputStream -> IOMutable FileChannel

  native getFD :: MutableIO FileInputStream -> IO FileDescriptor throws IOException

  native read :: MutableIO FileInputStream -> IO Int throws IOException
               | MutableIO FileInputStream -> MutableIO (JArray Byte) -> IO Int throws IOException
               | MutableIO FileInputStream -> MutableIO (JArray Byte) -> Int -> Int -> IO Int throws IOException

  native skip :: MutableIO FileInputStream -> Long -> IO Long throws IOException
```

##types.properties##

* All the examples above use a properties file which indicates all the Java classes, their purity and their optional new names in Frege code.
The name used here as the key is the class name returned by `java.lang.Class.getName()`
for that class. Nested classes can also be mentioned with their name as returned by `java.lang.Class.getName()`.
* If a class is missing in this file but being used in the class we are generating, the missing class is assumed to be pure and
the unqualified name of the class will be used in the generated code.

```
boolean=pure,Bool
byte=pure,Byte
char=pure,Char
double=pure,Double
float=pure,Float
int=pure,Int

java.awt.Point=st

java.io.File=io
java.io.FileInputStream=io
java.io.InputStream=io
java.io.OutputStream=io
java.io.PrintStream=io
java.io.PrintWriter=io
java.io.Reader=io
java.io.Writer=io

java.lang.Appendable=st
java.lang.Comparable=pure
java.lang.Iterable=st
java.lang.Readable=st
java.lang.String=pure
java.lang.StringBuffer=st
java.lang.StringBuilder=st

java.math.BigDecimal=pure
java.math.BigInteger=pure,Integer

java.nio.ByteBuffer=st
java.nio.ByteOrder=pure
java.nio.CharBuffer=st
java.nio.DoubleBuffer=st
java.nio.FloatBuffer=st
java.nio.IntBuffer=st
java.nio.LongBuffer=st
java.nio.ShortBuffer=st

java.nio.channels.FileChannel=io
java.nio.channels.ReadableByteChannel=st

java.nio.file.Path=pure
java.nio.file.WatchService=st

java.security.Permission=pure
java.security.PermissionCollection=st

java.util.AbstractCollection=st
java.util.AbstractList=st
java.util.AbstractMap$SimpleEntry=st,AbstractMapSimpleEntry
java.util.AbstractMap$SimpleImmutableEntry=pure,AbstractMapSimpleImmutableEntry
java.util.AbstractMap=st
java.util.AbstractQueue=st
java.util.AbstractSequentialList=st
java.util.AbstractSet=st
java.util.ArrayDeque=st
java.util.ArrayList=st
java.util.Arrays=pure
java.util.BitSet=st
java.util.Calendar=st
java.util.Collection=st
java.util.Collections=pure
java.util.Comparator=pure
java.util.Currency=pure
java.util.Date=st
java.util.Deque=st
java.util.Dictionary=st
java.util.EnumMap=st
java.util.EnumSet=st
java.util.Enumeration=st
java.util.EventListener=st
java.util.EventListenerProxy=st
java.util.EventObject=st
java.util.Formattable=st
java.util.FormattableFlags=pure
java.util.Formatter$BigDecimalLayoutForm=pure,FormatterBigDecimalLayoutForm
java.util.Formatter=st
java.util.GregorianCalendar=st
java.util.HashMap=st
java.util.HashSet=st
java.util.Hashtable=st
java.util.IdentityHashMap=st
java.util.Iterator=st
java.util.LinkedHashMap=st
java.util.LinkedHashSet=st
java.util.LinkedList=st
java.util.List=st
java.util.ListIterator=st
java.util.Locale$Builder=st,LocaleBuilder
java.util.Locale$Category=pure,LocaleCategory
java.util.Locale=pure
java.util.Map$Entry=st,MapEntry
java.util.Map=st
java.util.NavigableMap=st
java.util.NavigableSet=st
java.util.Objects=pure
java.util.Observable=st
java.util.Observer=st
java.util.PriorityQueue=st
java.util.Properties=st
java.util.PropertyPermission=pure
java.util.Queue=st
java.util.Random=st
java.util.RandomAccess=st
java.util.Scanner=st
java.util.Set=st
java.util.SortedMap=st
java.util.SortedSet=st
java.util.Stack=st
java.util.TimeZone=pure
java.util.TreeMap=st
java.util.TreeSet=st
java.util.Vector=st
java.util.WeakHashMap=st

java.util.regex.MatchResult=pure
java.util.regex.Matcher=st,RegexMatcher
java.util.regex.Pattern=pure,RegexPattern

long=pure,Long
short=pure,Short
void=pure,()
```

##How to run##

1. Download and extract `frege-native-gen-XX.zip` from [releases](https://github.com/Frege/frege-native-gen/releases) where `XX` is the version. The `types.properties` is included in the zip.
2. Mention your class name along with it's purity in **types.properties**.

   For example, to generate for `java.util.HashSet`, add `java.util.HashSet=st`
   or if you want to call it a different name in Frege, add `java.util.HashSet=st,JHashSet`
3. Run `java -jar frege-native-gen-XX.jar java.util.HashSet`

   To generate for a third party class (In this example, a class from Guava library):
```
java -cp /path/to/guava-15.0.jar:lib/frege-YY.jar:frege-native-gen-XX.jar frege.nativegen.Main com.google.common.collect.ImmutableCollection
```
where `XX` and `YY` are the versions of the jar files in the downloaded zip.
