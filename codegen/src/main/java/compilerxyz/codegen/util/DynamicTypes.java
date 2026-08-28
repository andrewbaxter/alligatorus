package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import compilerxyz.util.TMap;
import com.squareup.javapoet.ParameterizedTypeName;
import compilerxyz.util.IMap;

/**
 * Runtime {@code Type}-descriptor lookup for codegen. The <em>descriptor
 * expression</em> for any type discovered by {@link
 * compilerxyz.codegen.experimental.DynamicTypeScanner} — every {@code @Builtin}
 * type (routed to its generated {@code GenX.dynamicType()}) and any type that
 * hand-declares a nullary {@code dynamicType()} — comes from that scan; this
 * class is not a second registry of them.
 *
 * <p>What lives here is only what the scan can't derive: the primitive/leaf
 * <em>value classes</em> (the JVM class a {@code TypeXxxDynamic} represents,
 * e.g. {@code String} for {@code TypeStringDynamic}), built with {@code new
 * TypeXxxDynamic()}, and the composite wrappers ({@code TList}/{@code
 * Opt}/{@code EResult}/{@code TMap}).
 */
public final class DynamicTypes {
  private DynamicTypes() {}

  private record CompositeEntry(int arity, TypeName valueClass) {}

  /** Value types built with {@code new X()}, mapped to the JVM class they represent. */
  private static final IMap<ClassName, TypeName> NULLARY;
  private static final IMap<ClassName, CompositeEntry> COMPOSITES;

  private static void nullary(
      TMap<ClassName, TypeName> nullary, String pkg, String cls, TypeName valueClass) {
    nullary.put(ClassName.get(pkg, cls), valueClass);
  }

  private static void nullary(TMap<ClassName, TypeName> nullary, String pkg, String cls) {
    var cn = ClassName.get(pkg, cls);
    nullary.put(cn, cn);
  }

  private static void composite(
      TMap<ClassName, CompositeEntry> composites,
      String pkg,
      String cls,
      int arity,
      TypeName valueClass) {
    composites.put(ClassName.get(pkg, cls), new CompositeEntry(arity, valueClass));
  }

  static {
    var nullary = new TMap<ClassName, TypeName>();
    var composites = new TMap<ClassName, CompositeEntry>();
    // Nullary value types — value classes are unboxed / distinct where appropriate.
    nullary(nullary, "compilerxyz.eval.value.string", "TypeStringDynamic", ClassName.get(String.class));
    nullary(nullary, "compilerxyz.eval.value.bool_", "TypeBoolDynamic", TypeName.BOOLEAN);
    nullary(nullary, "compilerxyz.eval.value.int_", "TypeIntDynamic", TypeName.INT);
    nullary(nullary, "compilerxyz.eval.value.short_", "TypeShortDynamic", TypeName.SHORT);
    nullary(nullary, "compilerxyz.eval.value.long_", "TypeLongDynamic", TypeName.LONG);
    nullary(nullary, "compilerxyz.eval.value.float_", "TypeFloatDynamic", TypeName.FLOAT);
    nullary(nullary, "compilerxyz.eval.value.double_", "TypeDoubleDynamic", TypeName.DOUBLE);
    nullary(nullary, "compilerxyz.eval.value.ubyte_", "TypeUbyteDynamic", TypeName.BYTE);
    nullary(nullary, "compilerxyz.eval.value.ushort_", "TypeUshortDynamic", TypeName.SHORT);
    nullary(nullary, "compilerxyz.eval.value.uint_", "TypeUintDynamic", TypeName.INT);
    nullary(nullary, "compilerxyz.eval.value.ulong_", "TypeUlongDynamic", TypeName.LONG);
    nullary(nullary, "compilerxyz.eval.value.byte_", "TypeByteDynamic", TypeName.BYTE);
    nullary(nullary, "compilerxyz.eval.value.null_", "TypeNullDynamic", ClassName.OBJECT);
    nullary(
        nullary,
        "compilerxyz.eval.value.unique",
        "TypeUniqueDynamic",
        ClassName.get("compilerxyz.eval", "Unique"));
    nullary(
        nullary,
        "compilerxyz.eval.value.unique",
        "TypeUniqueConst",
        ClassName.get("compilerxyz.eval", "Unique"));
    nullary(
        nullary,
        "compilerxyz.eval.value.constkey",
        "TypeConstkeyDynamic",
        ClassName.get("compilerxyz.eval", "ConstKey"));
    nullary(
        nullary,
        "compilerxyz.eval.value.expr",
        "TypeExpr",
        ClassName.get("compilerxyz.eval.expr", "StagedExprCell"));
    nullary(nullary, "compilerxyz.eval.value.throw_", "TypeThrowDynamic");
    nullary(nullary, "compilerxyz.eval.value.panic", "TypePanicDynamic");
    nullary(nullary, "compilerxyz.eval.value.function", "TypeFunctionAspectDynamic");
    nullary(nullary, "compilerxyz.eval.value.function", "TypeFunctionCastValueDynamic");
    nullary(nullary, "compilerxyz.eval.value.function", "TypeFunctionMustReinterpretConst");
    nullary(nullary, "compilerxyz.eval.value.list", "TypeListConstructorConst");
    nullary(nullary, "compilerxyz.eval.value.map", "TypeMapConstructorConst");
    nullary(nullary, "compilerxyz.eval.value.option", "TypeOptionConstructorConst");
    nullary(nullary, "compilerxyz.eval.value.eresult", "TypeEResultConstructorConst");
    nullary(nullary, "compilerxyz.eval.value.zbase32", "TypeZbase32Decode");
    nullary(nullary, "compilerxyz.eval.value.source", "TypeImport");
    nullary(nullary, "compilerxyz.eval.value.source", "TypeSourceVirtual");
    nullary(nullary, "compilerxyz.eval.value.source", "TypeSourceLocal");
    nullary(nullary, "compilerxyz.eval.value.source", "TypeSourceRemote");
    nullary(nullary, "compilerxyz.eval.value.source", "TypeSourceSha256");

    // Composite descriptors.
    composite(
        composites,
        "compilerxyz.eval.value.list",
        "TypeListBoxedDynamic",
        1,
        ClassName.get("compilerxyz.util", "TList"));
    composite(
        composites,
        "compilerxyz.eval.value.option",
        "TypeOptionDynamic",
        1,
        ClassName.get("compilerxyz.util", "Opt"));
    composite(
        composites,
        "compilerxyz.eval.value.eresult",
        "TypeEResultDynamic",
        1,
        ClassName.get("compilerxyz.eval", "EResult"));
    composite(
        composites,
        "compilerxyz.eval.value.map",
        "TypeMapDynamic",
        2,
        ClassName.get("compilerxyz.util", "TOrderedMap"));
    NULLARY = IMap.from(nullary);
    COMPOSITES = IMap.from(composites);
  }

  public static CodeBlock leaf(ClassName descriptor, TMap<ClassName, CodeBlock> scannedLeaves) {
    if (NULLARY.containsKey(descriptor)) {
      return CodeBlock.of("new $T()", descriptor);
    }
    var scanned = scannedLeaves.getOrNull(descriptor);
    if (scanned != null) return scanned;
    if (descriptor.packageName().equals("compilerxyz.gen")) {
      // A gen-package companion is its own descriptor source.
      return CodeBlock.of("$T.dynamicType()", descriptor);
    }
    throw new IllegalArgumentException(
        "DynamicTypes has no descriptor for " + descriptor.canonicalName());
  }

  public static TypeName leafValueClass(ClassName descriptor) {
    var value = NULLARY.getOrNull(descriptor);
    // Every other type is its own value class (the JVM class is the type).
    return value != null ? value : descriptor;
  }

  public static CodeBlock composite(ClassName wrapper, CodeBlock... params) {
    var spec = compositeEntry(wrapper);
    if (spec.arity() != params.length) {
      throw new IllegalArgumentException(
          "DynamicTypes composite "
              + wrapper.canonicalName()
              + " expects "
              + spec.arity()
              + " params, got "
              + params.length);
    }
    var joined = CodeBlock.builder();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) joined.add(", ");
      joined.add(params[i]);
    }
    return CodeBlock.of("new $T($L)", wrapper, joined.build());
  }

  public static TypeName compositeValueClass(ClassName wrapper, TypeName... params) {
    var spec = compositeEntry(wrapper);
    if (spec.arity() != params.length) {
      throw new IllegalArgumentException(
          "DynamicTypes composite "
              + wrapper.canonicalName()
              + " expects "
              + spec.arity()
              + " params, got "
              + params.length);
    }
    return ParameterizedTypeName.get((ClassName) spec.valueClass(), params);
  }

  private static CompositeEntry compositeEntry(ClassName wrapper) {
    var spec = COMPOSITES.getOrNull(wrapper);
    if (spec == null) {
      throw new IllegalArgumentException(
          "DynamicTypes has no composite entry for " + wrapper.canonicalName());
    }
    return spec;
  }
}
