#Frege code generator for Java classes#
This project aims to reduce the manual effort of defining native bindings for Java classes in Frege. Given a Java class and it's purity whether
it is pure or mutable(`ST`) or doing IO(`IO`), this will generate corresponding Frege code for that class.
The generated code may still not compile due to other unknown types or you might want to wrap the return type in `Maybe`
if the method is known to return a possible `null`. So this is just an utility so that
we don't have to write down all the methods by looking at Java signatures.

**Example 1:** `java.awt.Point` in `ST`

1. All public members are resolved including public fields.
2. Overloaded constructors are resolved and seperated with `|` in the types.
3. Overloaded methods are identified and appended with repeated `'` for each variant in their name. The reason the representation
   differs from that of overloaded constructors is to allow different purity annotations among overloaded methods based on the purity
   of method parameters and return type.

```
data Point = native java.awt.Point where

  native x ".x" :: Mutable s Point -> ST s Int
  native y ".y" :: Mutable s Point -> ST s Int

  native new :: () -> STMutable s Point
              | Mutable s Point -> STMutable s Point
              | Int -> Int -> STMutable s Point

  native equals :: Mutable s Point -> Object -> ST s Bool
  native getLocation :: Mutable s Point -> STMutable s Point
  native getX :: Mutable s Point -> ST s Double
  native getY :: Mutable s Point -> ST s Double
  native move :: Mutable s Point -> Int -> Int -> ST s ()
  native setLocation :: Mutable s Point -> Mutable s Point -> ST s ()
  native setLocation' setLocation :: Mutable s Point -> Int -> Int -> ST s ()
  native setLocation'' setLocation :: Mutable s Point -> Double -> Double -> ST s ()
  native toString :: Mutable s Point -> ST s String
  native translate :: Mutable s Point -> Int -> Int -> ST s ()
```
**Example 2:** `java.util.HashMap` in `ST`

Type parameters for generic classes and methods are correctly resolved, so are nested classes (`java.util.Map.Entry`). The
naming convention for nested classes in the generated code is to prepend the class name with the parent class name and an underscore
but it can be overridden to have a different name (See `KnownTypes.properties` below).

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
  native entrySet :: Mutable s (HashMap k v) -> STMutable s (Set (Map_Entry k v))
  native get :: Mutable s (HashMap k v) -> Object -> ST s v
  native isEmpty :: Mutable s (HashMap k v) -> ST s Bool
  native keySet :: Mutable s (HashMap k v) -> STMutable s (Set k)
  native put :: Mutable s (HashMap k v) -> k -> v -> ST s v
  native putAll :: Mutable s (HashMap k v) -> Mutable s (Map k v) -> ST s ()
  native remove :: Mutable s (HashMap k v) -> Object -> ST s v
  native size :: Mutable s (HashMap k v) -> ST s Int
  native values :: Mutable s (HashMap k v) -> STMutable s (Collection v)

```
**Example 3:** `java.util.Locale` as `pure`

1. Public static fields are resolved and generated with all lowercase characters.
2. Though the class itself is pure, if any of the parameters for a method is impure such as arrays or other impure types
or if the method is returning `void` or doesn't take any parameters or throws any checked `Exception`,
the method will be considered as impure (here for example, `getAvailableLocales`, `getExtensionKeys`).

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
  native getAvailableLocales java.util.Locale.getAvailableLocales :: () -> STMutable s LocaleArr
  pure native getCountry :: Locale -> String
  pure native getDefault java.util.Locale.getDefault :: LocaleCategory -> Locale
  native getDefault' java.util.Locale.getDefault :: () -> ST s Locale
  pure native getDisplayCountry :: Locale -> String
  pure native getDisplayCountry' getDisplayCountry :: Locale -> Locale -> String
  pure native getDisplayLanguage :: Locale -> String
  pure native getDisplayLanguage' getDisplayLanguage :: Locale -> Locale -> String
  pure native getDisplayName :: Locale -> Locale -> String
  pure native getDisplayName' getDisplayName :: Locale -> String
  pure native getDisplayScript :: Locale -> String
  pure native getDisplayScript' getDisplayScript :: Locale -> Locale -> String
  pure native getDisplayVariant :: Locale -> Locale -> String
  pure native getDisplayVariant' getDisplayVariant :: Locale -> String
  pure native getExtension :: Locale -> Char -> String
  native getExtensionKeys :: Locale -> STMutable s (Set Character)
  pure native getISO3Country :: Locale -> String
  pure native getISO3Language :: Locale -> String
  native getISOCountries java.util.Locale.getISOCountries :: () -> STMutable s StringArr
  native getISOLanguages java.util.Locale.getISOLanguages :: () -> STMutable s StringArr
  pure native getLanguage :: Locale -> String
  pure native getScript :: Locale -> String
  native getUnicodeLocaleAttributes :: Locale -> STMutable s (Set String)
  native getUnicodeLocaleKeys :: Locale -> STMutable s (Set String)
  pure native getUnicodeLocaleType :: Locale -> String -> String
  pure native getVariant :: Locale -> String
  pure native hashCode :: Locale -> Int
  native setDefault java.util.Locale.setDefault :: Locale -> ST s ()
  native setDefault' java.util.Locale.setDefault :: LocaleCategory -> Locale -> ST s ()
  pure native toLanguageTag :: Locale -> String
  pure native toString :: Locale -> String
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
  native read' read :: MutableIO FileInputStream -> MutableIO ByteArr -> IO Int throws IOException
  native read'' read :: MutableIO FileInputStream -> MutableIO ByteArr -> Int -> Int -> IO Int throws IOException
  native skip :: MutableIO FileInputStream -> Long -> IO Long throws IOException
```

##KnownTypes.properties##

* All the examples above use a properties file which indicates all the Java classes, their purity and their optional new names in Frege code.
The name used here as the key is the class name returned by `java.lang.Class.getName()`
for that class. Nested classes can also be mentioned with their name as returned by `java.lang.Class.getName()`.
* If a class is missing in this file but being used in the class we are generating, the missing class is assumed to be pure and
the class's unqualified name will be used in the generated code.

```
int=pure,Int
boolean=pure,Bool
byte=pure,Byte
char=pure,Char
short=pure,Short
long=pure,Long
float=pure,Float
double=pure,Double
void=pure,()

java.lang.String=pure

java.math.BigInteger=pure,Integer

java.awt.Point=st

java.io.InputStream=io
java.io.PrintStream=io
java.io.FileInputStream=io
java.io.File=io

java.nio.channels.FileChannel=io

java.util.Collection=st
java.util.List=st
java.util.ArrayList=st
java.util.LinkedList=st
java.util.Iterator=st
java.util.ListIterator=st
java.util.Set=st
java.util.Map=st
java.util.HashMap=st
java.util.Scanner=st
java.util.Locale$Category=pure,LocaleCategory
java.util.Locale$Builder=st,LocaleBuilder
```

##How to run##

1. Download `native-gen-XX.jar` from [releases](https://github.com/Frege/native-gen/releases) where `XX` is the version
and `KnownTypes.properties` from [here](https://github.com/Frege/native-gen/blob/master/KnownTypes.properties).
2. Mention your class name along with it's purity in **KnownTypes.properties**.

   For example, to generate for `java.util.HashSet`, add `java.util.HashSet=st`
   or if you want to call it a different name in Frege, add `java.util.HashSet=st,JHashSet`
3. Run `java -jar native-gen-XX.jar java.util.HashSet KnownTypes.properties`

   If you want to generate for a third party class (In this example, a class from Guava library)
   Run `java -cp guava-15.0.jar:native-gen-XX.jar frege.nativegen.NativeGen com.google.common.collect.ImmutableCollection KnownTypes.properties`
